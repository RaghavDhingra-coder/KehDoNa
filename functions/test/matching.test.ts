import { strict as assert } from 'node:assert';
import { test } from 'node:test';
import { accepts, compatibility, mutuallyEligible, pairId, sections, type Gender, type Member, type Preference } from '../src/matching';

const member = (uid: string, gender: Gender, preference: Preference, personality = ''): Member =>
  ({ uid, firstName: uid, gender, preference, personality });

test('preferences accept only the stated genders', () => {
  assert.equal(accepts('women', 'woman'), true);
  assert.equal(accepts('women', 'man'), false);
  assert.equal(accepts('men', 'non_binary'), false);
  assert.equal(accepts('all', 'prefer_not_to_say'), true);
});

test('default opposite-gender pairs are mutually eligible; same-direction ones are not', () => {
  assert.equal(mutuallyEligible(member('a', 'man', 'women'), member('b', 'woman', 'men')), true);
  assert.equal(mutuallyEligible(member('a', 'man', 'women'), member('b', 'man', 'women')), false);
  assert.equal(mutuallyEligible(member('a', 'man', 'women'), member('b', 'woman', 'women')), false);
});

test('both people must accept each other (one-sided interest is not enough)', () => {
  assert.equal(mutuallyEligible(member('a', 'man', 'all'), member('b', 'woman', 'women')), false);
  assert.equal(mutuallyEligible(member('a', 'woman', 'all'), member('b', 'woman', 'women')), true);
});

test('non-binary, self-describing and undisclosed genders only match people open to all genders', () => {
  for (const g of ['non_binary', 'self_describe', 'prefer_not_to_say'] as Gender[]) {
    assert.equal(mutuallyEligible(member('a', g, 'all'), member('b', 'man', 'women')), false, g);
    assert.equal(mutuallyEligible(member('a', g, 'all'), member('b', 'man', 'all')), true, g);
    assert.equal(mutuallyEligible(member('a', g, 'men'), member('b', 'man', 'all')), true, g);
  }
});

test('never matched with yourself', () => {
  assert.equal(mutuallyEligible(member('a', 'man', 'all'), member('a', 'man', 'all')), false);
});

const RIYA = `Communication style: [Observed] Warm, playful and curious.
Interests:
- [Observed] Indie music and late-night playlists
- [Inferred] Travel and street food
Values: [Observed] Honesty and loyalty
Relationship preferences: [Inferred] Steady, thoughtful partners
Compatibility traits: People who love music and honest conversations
Confidence note: Based on months of chats.`;
const ARJUN = `Communication style: [Observed] Thoughtful and curious, a little playful.
Interests: [Observed] Music festivals, travel, cooking
Values: [Observed] Honesty, family
Relationship preferences: Someone steady and thoughtful
Compatibility traits: Warm, curious people`;
const KABIR = `Communication style: [Observed] Blunt and brief.
Interests: [Observed] Cricket statistics, motorbikes
Values: Ambition, winning
Relationship preferences: Not enough context`;

test('sections are read from the prompt headings, including bullets', () => {
  const s = sections(RIYA);
  assert.ok(s.get('interests')!.has('music'));
  assert.ok(s.get('interests')!.has('travel'));
  assert.ok(!s.get('interests')!.has('observed'));
});

test('more similar profiles score higher, and shared themes come from both sides', () => {
  const close = compatibility(RIYA, ARJUN);
  const far = compatibility(RIYA, KABIR);
  assert.ok(close.score > far.score, `${close.score} > ${far.score}`);
  assert.ok(close.sharedThemes.includes('music'));
  assert.ok(close.sharedThemes.includes('honesty'));
  for (const theme of close.sharedThemes) {
    assert.ok(RIYA.toLowerCase().includes(theme) && ARJUN.toLowerCase().includes(theme), theme);
  }
  assert.ok(close.sharedThemes.length <= 5);
});

test('compatibility is symmetric and bounded', () => {
  assert.equal(compatibility(RIYA, ARJUN).score, compatibility(ARJUN, RIYA).score);
  const self = compatibility(RIYA, RIYA).score;
  assert.ok(self > 0.5 && self <= 1);
});

test('pair ids are order-independent', () => {
  assert.equal(pairId('b', 'a'), pairId('a', 'b'));
  assert.equal(pairId('a', 'b'), 'a_b');
});
