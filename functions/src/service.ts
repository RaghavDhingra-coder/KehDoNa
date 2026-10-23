// Server-side matching. Runs only inside Cloud Functions with the Admin SDK; clients can't
// read the queue, other members' profiles, or write matches (see firestore.rules).

import { FieldValue, Firestore, Timestamp, type DocumentSnapshot } from 'firebase-admin/firestore';
import { compatibility, GENDERS, mutuallyEligible, pairId, PREFERENCES, type Gender, type Member, type Preference } from './matching';

export type MatchErrorReason = 'PROFILE_NOT_FOUND' | 'MATCHING_DETAILS_MISSING' | 'PERSONALITY_MISSING';

/** A request that can't be served until the member completes their profile. */
export class MatchError extends Error {
  constructor(readonly reason: MatchErrorReason, message: string) {
    super(message);
  }
}

export type MatchStatus =
  | { status: 'matched'; matchId: string }
  | { status: 'waiting' }
  | { status: 'idle' };

/** How many waiting members one request considers. */
const CANDIDATE_LIMIT = 200;

export class MatchService {
  constructor(private readonly db: Firestore) {}

  private users = () => this.db.collection('users');
  private personalities = () => this.db.collection('personalityProfiles');
  private queue = () => this.db.collection('matchQueue');
  private states = () => this.db.collection('matchState');
  private matches = () => this.db.collection('matches');

  /**
   * Returns the member's existing match, or matches them with the most compatible eligible
   * member who is waiting, or puts them in the waiting queue. Safe to call repeatedly.
   */
  async requestMatch(uid: string): Promise<MatchStatus> {
    const me = await this.loadMember(uid);

    // Join the queue first, so two members asking at the same moment always see each other.
    const joined = await this.db.runTransaction(async (tx) => {
      const state = await tx.get(this.states().doc(uid));
      if (state.get('status') === 'matched') return { status: 'matched', matchId: state.get('matchId') } as MatchStatus;
      const queued = await tx.get(this.queue().doc(uid));
      if (!queued.exists) tx.set(queued.ref, { enqueuedAt: FieldValue.serverTimestamp() });
      tx.set(state.ref, { status: 'waiting', updatedAt: FieldValue.serverTimestamp() });
      return null;
    });
    if (joined) return joined;

    return (await this.matchFromQueue(me)) ?? { status: 'waiting' };
  }

  /** Leaves the waiting queue. A confirmed match can't be cancelled this way and is returned as is. */
  async cancelRequest(uid: string): Promise<MatchStatus> {
    return this.db.runTransaction(async (tx) => {
      const state = await tx.get(this.states().doc(uid));
      if (state.get('status') === 'matched') return { status: 'matched', matchId: state.get('matchId') };
      tx.delete(this.queue().doc(uid));
      tx.set(state.ref, { status: 'idle', updatedAt: FieldValue.serverTimestamp() });
      return { status: 'idle' };
    });
  }

  private async matchFromQueue(me: Member): Promise<MatchStatus | null> {
    const waiting = await this.queue().orderBy('enqueuedAt').limit(CANDIDATE_LIMIT).get();
    const others = waiting.docs.filter((d) => d.id !== me.uid);
    if (others.length === 0) return null;
    const enqueuedAt = new Map(others.map((d) => [d.id, (d.get('enqueuedAt') as Timestamp | undefined)?.toMillis() ?? 0]));

    const members = await this.loadMembers(others.map((d) => d.id));
    const eligible = members.filter((m) => mutuallyEligible(me, m));
    if (eligible.length === 0) return null;

    // Never pair the same two people twice.
    const pairDocs = await this.db.getAll(...eligible.map((m) => this.matches().doc(pairId(me.uid, m.uid))));
    const fresh = eligible.filter((_, i) => !pairDocs[i].exists);

    const ranked = fresh
      .map((m) => ({ member: m, ...compatibility(me.personality, m.personality) }))
      .sort((x, y) => y.score - x.score || enqueuedAt.get(x.member.uid)! - enqueuedAt.get(y.member.uid)!);

    for (const candidate of ranked) {
      const outcome = await this.db.runTransaction(async (tx) => {
        const id = pairId(me.uid, candidate.member.uid);
        const [myState, theirState, myQueue, theirQueue, pair] = await tx.getAll(
          this.states().doc(me.uid),
          this.states().doc(candidate.member.uid),
          this.queue().doc(me.uid),
          this.queue().doc(candidate.member.uid),
          this.matches().doc(id),
        );
        // Someone else matched me meanwhile: report that match.
        if (myState.get('status') === 'matched') return { status: 'matched', matchId: myState.get('matchId') } as MatchStatus;
        // I cancelled meanwhile: stop.
        if (!myQueue.exists) return { status: 'idle' } as MatchStatus;
        // They were matched, cancelled, or already paired with me: try the next candidate.
        if (theirState.get('status') !== 'waiting' || !theirQueue.exists || pair.exists) return null;

        const [first, second] = [me, candidate.member].sort((x, y) => (x.uid < y.uid ? -1 : 1));
        tx.create(pair.ref, {
          userIds: [first.uid, second.uid],
          // Only what each person may see about the other: first name and shared themes.
          participants: { [first.uid]: { firstName: first.firstName }, [second.uid]: { firstName: second.firstName } },
          score: candidate.score,
          sharedThemes: candidate.sharedThemes,
          createdAt: FieldValue.serverTimestamp(),
        });
        for (const uid of [me.uid, candidate.member.uid]) {
          tx.set(this.states().doc(uid), { status: 'matched', matchId: id, updatedAt: FieldValue.serverTimestamp() });
          tx.delete(this.queue().doc(uid));
        }
        return { status: 'matched', matchId: id } as MatchStatus;
      });
      if (outcome) return outcome;
    }
    return null;
  }

  private async loadMember(uid: string): Promise<Member> {
    const [user, personality] = await this.db.getAll(this.users().doc(uid), this.personalities().doc(uid));
    if (!user.exists) throw new MatchError('PROFILE_NOT_FOUND', 'Create your profile before matching.');
    const member = toMember(user, personality);
    if (member === 'details') {
      throw new MatchError('MATCHING_DETAILS_MISSING', 'Add your gender and who you\'d like to match with first.');
    }
    if (member === 'personality') {
      throw new MatchError('PERSONALITY_MISSING', 'Discover your vibe first: matching uses your personality profile.');
    }
    return member;
  }

  private async loadMembers(uids: string[]): Promise<Member[]> {
    if (uids.length === 0) return [];
    const docs = await this.db.getAll(
      ...uids.flatMap((uid) => [this.users().doc(uid), this.personalities().doc(uid)]),
    );
    const members: Member[] = [];
    for (let i = 0; i < docs.length; i += 2) {
      const member = docs[i].exists ? toMember(docs[i], docs[i + 1]) : null;
      if (member && typeof member === 'object') members.push(member);
    }
    return members;
  }
}

function toMember(user: DocumentSnapshot, personality: DocumentSnapshot): Member | 'details' | 'personality' {
  const gender = user.get('gender');
  const preference = user.get('matchPreference');
  if (!GENDERS.includes(gender) || !PREFERENCES.includes(preference)) return 'details';
  const response = personality.exists ? personality.get('response') : null;
  if (typeof response !== 'string' || response.length === 0) return 'personality';
  const name = String(user.get('name') ?? '').trim();
  return {
    uid: user.id,
    firstName: name.split(/\s+/)[0] || 'Someone',
    gender: gender as Gender,
    preference: preference as Preference,
    personality: response,
  };
}
