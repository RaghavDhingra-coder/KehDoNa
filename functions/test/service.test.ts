// Runs against the Firestore emulator (npm test starts it). Exercises the real transactions.
import { strict as assert } from 'node:assert';
import { after, beforeEach, test } from 'node:test';
import { initializeApp } from 'firebase-admin/app';
import { getFirestore } from 'firebase-admin/firestore';
import { MatchError, MatchService } from '../src/service';

assert.ok(process.env.FIRESTORE_EMULATOR_HOST, 'Run via `npm test` so the Firestore emulator is used.');
const app = initializeApp({ projectId: 'demo-kehdona' }, 'service-test');
const db = getFirestore(app);
const service = new MatchService(db);

const MUSIC = `Communication style: Warm and curious\nInterests: Indie music, travel, street food\nValues: Honesty, loyalty\nRelationship preferences: Steady and thoughtful`;
const MUSIC2 = `Communication style: Curious and warm\nInterests: Music festivals, travel\nValues: Honesty, family\nRelationship preferences: Thoughtful`;
const CRICKET = `Communication style: Blunt\nInterests: Cricket statistics, motorbikes\nValues: Ambition`;

async function member(uid: string, name: string, gender: string, matchPreference: string, personality?: string) {
  const data: Record<string, string> = { name, email: `${uid}@example.com`, phoneNumber: '+919876543210' };
  if (gender) data.gender = gender;
  if (matchPreference) data.matchPreference = matchPreference;
  if (gender === 'self_describe') data.genderSelfDescription = 'private words';
  await db.doc(`users/${uid}`).set(data);
  if (personality) await db.doc(`personalityProfiles/${uid}`).set({ response: personality, promptVersion: 1 });
}

async function clear() {
  for (const c of ['users', 'personalityProfiles', 'matchQueue', 'matchState', 'matches']) {
    const docs = await db.collection(c).listDocuments();
    await Promise.all(docs.map((d) => d.delete()));
  }
}

beforeEach(clear);
after(async () => { await clear(); });

test('first member waits; the next compatible member is matched with them, and both see the same match', async () => {
  await member('riya', 'Riya Sharma', 'woman', 'men', MUSIC);
  await member('arjun', 'Arjun Mehta', 'man', 'women', MUSIC2);

  assert.deepEqual(await service.requestMatch('riya'), { status: 'waiting' });
  const result = await service.requestMatch('arjun');

  assert.deepEqual(result, { status: 'matched', matchId: 'arjun_riya' });
  const match = (await db.doc('matches/arjun_riya').get()).data()!;
  assert.deepEqual(match.userIds, ['arjun', 'riya']);
  assert.deepEqual(match.participants, { arjun: { firstName: 'Arjun' }, riya: { firstName: 'Riya' } });
  assert.equal((await db.doc('matchState/riya').get()).get('matchId'), 'arjun_riya');
  assert.equal((await db.doc('matchState/arjun').get()).get('matchId'), 'arjun_riya');
  assert.equal((await db.collection('matchQueue').get()).size, 0);
  // Asking again returns the same match instead of creating another.
  assert.deepEqual(await service.requestMatch('riya'), { status: 'matched', matchId: 'arjun_riya' });
});

test('match documents carry no private data', async () => {
  await member('riya', 'Riya Sharma', 'woman', 'all', MUSIC);
  await member('sam', 'Sam Lee', 'self_describe', 'all', MUSIC2);
  await service.requestMatch('riya');
  await service.requestMatch('sam');

  const json = JSON.stringify((await db.doc('matches/riya_sam').get()).data());
  for (const secret of ['@example.com', '+91', 'private words', 'self_describe', 'Sharma', 'Lee', 'Warm and curious']) {
    assert.ok(!json.includes(secret), `match doc leaks ${secret}`);
  }
});

test('picks the most compatible eligible candidate, not just the first in line', async () => {
  await member('kabir', 'Kabir', 'man', 'women', CRICKET);
  await member('arjun', 'Arjun', 'man', 'women', MUSIC2);
  await service.requestMatch('kabir'); // waited longer
  await service.requestMatch('arjun');
  await member('riya', 'Riya', 'woman', 'men', MUSIC);

  assert.deepEqual(await service.requestMatch('riya'), { status: 'matched', matchId: 'arjun_riya' });
  assert.equal((await db.doc('matchState/kabir').get()).get('status'), 'waiting');
});

test('respects both preferences: no match when either side is not open to the other', async () => {
  await member('riya', 'Riya', 'woman', 'women', MUSIC);
  await member('arjun', 'Arjun', 'man', 'women', MUSIC2);
  await service.requestMatch('riya');

  assert.deepEqual(await service.requestMatch('arjun'), { status: 'waiting' });
});

test('non-binary members are matched with people open to all genders', async () => {
  await member('alex', 'Alex', 'non_binary', 'all', MUSIC);
  // Waiting first and more similar to Alex, but only open to men: must be skipped.
  await member('arjun', 'Arjun', 'man', 'men', MUSIC);
  await member('meera', 'Meera', 'woman', 'all', CRICKET);
  await service.requestMatch('arjun');
  await service.requestMatch('meera');

  assert.deepEqual(await service.requestMatch('alex'), { status: 'matched', matchId: 'alex_meera' });
  assert.equal((await db.doc('matchState/arjun').get()).get('status'), 'waiting');
});

test('simultaneous requests produce exactly one consistent match', async () => {
  await member('riya', 'Riya', 'woman', 'men', MUSIC);
  await member('arjun', 'Arjun', 'man', 'women', MUSIC2);
  await member('dev', 'Dev', 'man', 'women', MUSIC2);

  const results = await Promise.all([service.requestMatch('riya'), service.requestMatch('arjun'), service.requestMatch('dev')]);

  const matches = await db.collection('matches').get();
  assert.equal(matches.size, 1, 'exactly one match');
  const [a, b] = matches.docs[0].get('userIds') as string[];
  assert.ok(a === 'riya' || b === 'riya');
  // Everyone's state agrees with the single match; nobody is in two matches.
  for (const uid of ['riya', 'arjun', 'dev']) {
    const state = (await db.doc(`matchState/${uid}`).get()).data()!;
    if ([a, b].includes(uid)) assert.equal(state.matchId, matches.docs[0].id);
    else assert.equal(state.status, 'waiting');
  }
  assert.equal(results.filter((r) => r.status === 'matched').length >= 1, true);
});

test('a pair is never matched twice', async () => {
  await member('riya', 'Riya', 'woman', 'men', MUSIC);
  await member('arjun', 'Arjun', 'man', 'women', MUSIC2);
  await db.doc('matches/arjun_riya').set({ userIds: ['arjun', 'riya'] }); // an earlier match

  await service.requestMatch('riya');
  assert.deepEqual(await service.requestMatch('arjun'), { status: 'waiting' });
});

test('cancelling leaves the queue; a cancelled member is not matched', async () => {
  await member('riya', 'Riya', 'woman', 'men', MUSIC);
  await member('arjun', 'Arjun', 'man', 'women', MUSIC2);
  await service.requestMatch('riya');

  assert.deepEqual(await service.cancelRequest('riya'), { status: 'idle' });
  assert.deepEqual(await service.requestMatch('arjun'), { status: 'waiting' });
  assert.equal((await db.doc('matchQueue/riya').get()).exists, false);
});

test('cancelling after a confirmed match keeps the match', async () => {
  await member('riya', 'Riya', 'woman', 'men', MUSIC);
  await member('arjun', 'Arjun', 'man', 'women', MUSIC2);
  await service.requestMatch('riya');
  await service.requestMatch('arjun');

  assert.deepEqual(await service.cancelRequest('riya'), { status: 'matched', matchId: 'arjun_riya' });
});

test('incomplete profiles are refused with a specific reason, never guessed', async () => {
  await member('noDetails', 'A', undefined as never, undefined as never, MUSIC);
  await member('noVibe', 'B', 'woman', 'men');

  await assert.rejects(service.requestMatch('noDetails'), (e) => e instanceof MatchError && e.reason === 'MATCHING_DETAILS_MISSING');
  await assert.rejects(service.requestMatch('noVibe'), (e) => e instanceof MatchError && e.reason === 'PERSONALITY_MISSING');
  await assert.rejects(service.requestMatch('ghost'), (e) => e instanceof MatchError && e.reason === 'PROFILE_NOT_FOUND');
  assert.equal((await db.collection('matchQueue').get()).size, 0);
});

test('waiting members with incomplete profiles are skipped as candidates', async () => {
  await member('arjun', 'Arjun', 'man', 'women', MUSIC2);
  await service.requestMatch('arjun');
  await db.doc('personalityProfiles/arjun').delete(); // e.g. data removed by an admin
  await member('riya', 'Riya', 'woman', 'men', MUSIC);

  assert.deepEqual(await service.requestMatch('riya'), { status: 'waiting' });
});
