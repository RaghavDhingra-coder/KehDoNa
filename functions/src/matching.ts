// Pure matching rules: who may be matched with whom, and how compatible two profiles are.
// No I/O here, so it is fully unit-tested.

export const GENDERS = ['man', 'woman', 'non_binary', 'self_describe', 'prefer_not_to_say'] as const;
export const PREFERENCES = ['men', 'women', 'all'] as const;
export type Gender = (typeof GENDERS)[number];
export type Preference = (typeof PREFERENCES)[number];

/** What the matcher knows about a member. Private fields (email, phone, self-description) never enter it. */
export interface Member {
  uid: string;
  firstName: string;
  gender: Gender;
  preference: Preference;
  personality: string;
}

/** True when someone who wants [preference] is open to a person of [gender]. */
export function accepts(preference: Preference, gender: Gender): boolean {
  switch (preference) {
    case 'all':
      return true;
    case 'men':
      return gender === 'man';
    case 'women':
      return gender === 'woman';
  }
}

/**
 * Both people's stated preferences must include each other. Members who are non-binary,
 * self-describe or prefer not to say are only matched with people open to all genders,
 * because nobody's gender is ever assumed.
 */
export function mutuallyEligible(a: Member, b: Member): boolean {
  return a.uid !== b.uid && accepts(a.preference, b.gender) && accepts(b.preference, a.gender);
}

// --- Compatibility -------------------------------------------------------------------------

type SectionKey =
  | 'communication' | 'interests' | 'values' | 'emotional' | 'humour'
  | 'social' | 'relationship' | 'strengths' | 'compatibility' | 'confidence';

const HEADINGS: Array<[SectionKey, string]> = [
  ['communication', 'communication style'],
  ['interests', 'interests'],
  ['values', 'values'],
  ['emotional', 'emotional tendencies'],
  ['humour', 'sense of humou?r'],
  ['social', 'social energy'],
  ['relationship', 'relationship preferences'],
  ['strengths', 'strengths'],
  ['compatibility', 'compatibility traits'],
  ['confidence', 'confidence note'],
];

/** Same-section overlap weights. "compatibility" is scored separately against the other profile. */
const WEIGHTS: Partial<Record<SectionKey, number>> = {
  interests: 3, values: 3, relationship: 2, communication: 2, humour: 1, social: 1, emotional: 1, strengths: 1,
};
const CROSS_WEIGHT = 2;

const STOP_WORDS = new Set((
  'the and for you your yours with that this are was were have has had not but they them their there ' +
  'from into about what when where which while who whom how why can could would should will just also ' +
  'very really more most much many some any all each other than then too our out own same such only ' +
  'like likely seem seems seemed tend tends often usually sometimes enough context observed inferred ' +
  'based chats chat conversations conversation people person things thing someone something way ways ' +
  'being been both its it\'s you\'re you\'ve don\'t doesn\'t lot lots kind sort may might perhaps'
).split(' '));

function tokens(text: string): Set<string> {
  const out = new Set<string>();
  for (const raw of text.toLowerCase().split(/[^\p{L}']+/u)) {
    const word = raw.replace(/^'+|'+$/g, '');
    if (word.length < 3 || STOP_WORDS.has(word)) continue;
    out.add(singular(word));
  }
  return out;
}

/** Light plural folding so "movies"/"movie" and "books"/"book" meet; "class" stays "class". */
function singular(word: string): string {
  if (word.length > 4 && word.endsWith('ies')) return `${word.slice(0, -3)}y`;
  if (word.length > 3 && word.endsWith('s') && !word.endsWith('ss')) return word.slice(0, -1);
  return word;
}

/** Splits a reply into the prompt's sections; text outside any heading is ignored. */
export function sections(response: string): Map<SectionKey, Set<string>> {
  const result = new Map<SectionKey, Set<string>>();
  let current: SectionKey | null = null;
  for (const rawLine of response.split(/\r?\n/)) {
    const line = rawLine.trim();
    let matched = false;
    for (const [key, label] of HEADINGS) {
      const m = new RegExp(`^[\\s>#*_\\-•\\d.)]*${label}(\\s*\\([^)\\n]*\\))?[\\s*_]*(:|$)(.*)$`, 'i').exec(line);
      if (m) {
        current = key;
        matched = true;
        const words = tokens(m[3] ?? '');
        result.set(key, new Set([...(result.get(key) ?? []), ...words]));
        break;
      }
    }
    if (!matched && current) {
      result.set(current, new Set([...(result.get(current) ?? []), ...tokens(line)]));
    }
  }
  return result;
}

function jaccard(a: Set<string>, b: Set<string>): number {
  if (a.size === 0 || b.size === 0) return 0;
  let shared = 0;
  for (const word of a) if (b.has(word)) shared++;
  return shared / (a.size + b.size - shared);
}

function union(map: Map<SectionKey, Set<string>>, skip: SectionKey[] = []): Set<string> {
  const all = new Set<string>();
  for (const [key, words] of map) if (!skip.includes(key)) words.forEach((w) => all.add(w));
  return all;
}

export interface Compatibility {
  /** 0..1, higher is more compatible. */
  score: number;
  /** Up to five words both profiles share (interests and values first). Nothing private to one side. */
  sharedThemes: string[];
}

export function compatibility(aText: string, bText: string): Compatibility {
  const a = sections(aText);
  const b = sections(bText);
  let total = 0;
  let weight = 0;
  for (const [key, w] of Object.entries(WEIGHTS) as Array<[SectionKey, number]>) {
    const aw = a.get(key);
    const bw = b.get(key);
    if (aw?.size && bw?.size) {
      total += w * jaccard(aw, bw);
      weight += w;
    }
  }
  // What each person says they click with, against what the other person is like.
  const aAll = union(a, ['compatibility', 'confidence']);
  const bAll = union(b, ['compatibility', 'confidence']);
  const crossParts = [
    a.get('compatibility')?.size ? jaccard(a.get('compatibility')!, bAll) : null,
    b.get('compatibility')?.size ? jaccard(b.get('compatibility')!, aAll) : null,
  ].filter((v): v is number => v !== null);
  if (crossParts.length) {
    total += CROSS_WEIGHT * (crossParts.reduce((x, y) => x + y, 0) / crossParts.length);
    weight += CROSS_WEIGHT;
  }
  // Free-form replies without headings still get a whole-text comparison.
  const score = weight > 0 ? total / weight : jaccard(tokens(aText), tokens(bText));

  const themes: string[] = [];
  for (const key of ['interests', 'values', 'relationship', 'communication'] as SectionKey[]) {
    const aw = a.get(key);
    const bw = b.get(key);
    if (!aw || !bw) continue;
    for (const word of [...aw].sort()) if (bw.has(word) && !themes.includes(word)) themes.push(word);
  }
  return { score: Math.round(score * 1000) / 1000, sharedThemes: themes.slice(0, 5) };
}

/** Stable id for a pair, so the same two people can never get two match documents. */
export function pairId(a: string, b: string): string {
  return a < b ? `${a}_${b}` : `${b}_${a}`;
}
