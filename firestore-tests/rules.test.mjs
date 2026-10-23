// Security-rule tests for ../firestore.rules. Each write mirrors FirestoreProfileRepository.kt exactly.
// Runs only against the local emulator ("demo-" project IDs can never reach a real Firebase project).
import { initializeTestEnvironment, assertSucceeds, assertFails } from '@firebase/rules-unit-testing';
import {
  doc, getDoc, setDoc, updateDoc, deleteDoc, collection, getDocs, runTransaction, serverTimestamp, Timestamp,
} from 'firebase/firestore';
import { readFileSync } from 'node:fs';

const env = await initializeTestEnvironment({
  projectId: 'demo-kehdona',
  firestore: {
    rules: readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8'),
    host: '127.0.0.1',
    port: 8085,
  },
});

const ts = () => serverTimestamp();
// ID-token claims as Firebase Auth issues them for each sign-in method.
const phoneUser = () => env.authenticatedContext('phone1', { phone_number: '+919876543210' }).firestore();
const emailUser = () => env.authenticatedContext('email1', { email: 'riya@college.edu', email_verified: false }).firestore();
const googleUser = () => env.authenticatedContext('g1', { email: 'riya@gmail.com', email_verified: true }).firestore();
const anon = () => env.unauthenticatedContext().firestore();

// --- FirestoreProfileRepository equivalents ---
function createProfile(db, uid, name, contact = {}) {
  const ref = doc(db, 'users', uid);
  return runTransaction(db, async (tx) => {
    if ((await tx.get(ref)).exists()) {
      tx.update(ref, { name, updatedAt: ts() });
    } else {
      tx.set(ref, { name, ...contact, createdAt: ts(), updatedAt: ts() });
    }
  });
}
const updateName = (db, uid, name) => updateDoc(doc(db, 'users', uid), { name, updatedAt: ts() });
const savePersonality = (db, uid, text) => updateDoc(doc(db, 'users', uid),
  { personalityProfile: text, personalityUpdatedAt: ts(), personalitySkipped: false, updatedAt: ts() });
const skipPersonality = (db, uid) => updateDoc(doc(db, 'users', uid), { personalitySkipped: true, updatedAt: ts() });
const read = async (uid) => {
  let data;
  await env.withSecurityRulesDisabled(async (ctx) => { data = (await getDoc(doc(ctx.firestore(), 'users', uid))).data(); });
  return data;
};

let pass = 0;
let fail = 0;
async function check(label, expect, fn) {
  try {
    await (expect === 'allow' ? assertSucceeds(fn()) : assertFails(fn()));
    pass++;
    console.log(`  ok   ${expect.padEnd(5)} ${label}`);
  } catch (e) {
    fail++;
    console.log(`  FAIL ${expect.padEnd(5)} ${label} :: ${String(e.message).split('\n')[0]}`);
  }
}
function expectTrue(label, condition) {
  if (condition) { pass++; console.log(`  ok   check ${label}`); } else { fail++; console.log(`  FAIL check ${label}`); }
}

console.log('App operations, every sign-in method (must be allowed)');
await check('phone sign-up creates own profile', 'allow', () => createProfile(phoneUser(), 'phone1', 'Riya Sharma', { phoneNumber: '+919876543210' }));
await check('email sign-up (not yet verified) creates own profile', 'allow', () => createProfile(emailUser(), 'email1', 'Riya', { email: 'riya@college.edu' }));
await check('Google sign-in creates own profile', 'allow', () => createProfile(googleUser(), 'g1', 'Riya G', { email: 'riya@gmail.com' }));
await check('owner reads own profile', 'allow', () => getDoc(doc(phoneUser(), 'users', 'phone1')));
await check('save personality profile', 'allow', () => savePersonality(phoneUser(), 'phone1', 'Communication style: [Observed] warm.'));
await check('skip personality', 'allow', () => skipPersonality(emailUser(), 'email1'));
await check('explicitly confirmed name change', 'allow', () => updateName(phoneUser(), 'phone1', 'Priya'));
await check('Unicode name (Devanagari)', 'allow', () => updateName(googleUser(), 'g1', 'राघव ढींगरा'));
await check('12,000-char personality profile', 'allow', () => savePersonality(phoneUser(), 'phone1', 'x'.repeat(12000)));

console.log('createProfile retry on an existing profile (transaction)');
const before = await read('phone1');
await new Promise((r) => setTimeout(r, 20));
await check('retry is allowed and only renames', 'allow', () => createProfile(phoneUser(), 'phone1', 'Riya Sharma', { phoneNumber: '+919876543210' }));
const after = await read('phone1');
expectTrue('createdAt unchanged after retry', after.createdAt.isEqual(before.createdAt));
expectTrue('updatedAt advanced after retry', after.updatedAt.toMillis() > before.updatedAt.toMillis());
expectTrue('personality profile kept after retry', after.personalityProfile === before.personalityProfile);
expectTrue('name applied by retry', after.name === 'Riya Sharma');
await check('old set+merge on existing profile (would rewrite createdAt)', 'deny', () =>
  setDoc(doc(phoneUser(), 'users', 'phone1'), { name: 'Riya', createdAt: ts(), updatedAt: ts() }, { merge: true }));

console.log('Unauthenticated / other UID (must be denied)');
await check('unauthenticated read', 'deny', () => getDoc(doc(anon(), 'users', 'phone1')));
await check('unauthenticated create', 'deny', () => setDoc(doc(anon(), 'users', 'x'), { name: 'Riya', createdAt: ts(), updatedAt: ts() }));
await check("read another UID's profile", 'deny', () => getDoc(doc(emailUser(), 'users', 'phone1')));
await check("update another UID's profile", 'deny', () => updateName(emailUser(), 'phone1', 'Hacked'));
await check("create a profile at another UID", 'deny', () => createProfile(emailUser(), 'someone-else', 'Riya'));
await check('list the users collection', 'deny', () => getDocs(collection(phoneUser(), 'users')));
await check('delete own profile', 'deny', () => deleteDoc(doc(phoneUser(), 'users', 'phone1')));
await check('write a subcollection under own profile', 'deny', () => setDoc(doc(phoneUser(), 'users', 'phone1', 'private', 'x'), { a: 1 }));

console.log('Firebase Auth contact details (must be denied)');
await check('email not matching token', 'deny', () => createProfile(googleUser(), 'g1b', 'Riya', { email: 'someone@else.com' }));
await check('email on a phone-only account', 'deny', () => createProfile(phoneUser(), 'phone2', 'Riya', { email: 'riya@gmail.com' }));
await check('phone not matching token', 'deny', () => createProfile(phoneUser(), 'phone1b', 'Riya', { phoneNumber: '+911111111111' }));
await check('rewrite email on update', 'deny', () => updateDoc(doc(googleUser(), 'users', 'g1'), { email: 'x@y.com', updatedAt: ts() }));
await check('remove phoneNumber via full overwrite', 'deny', () =>
  setDoc(doc(phoneUser(), 'users', 'phone1'), { name: 'Priya', createdAt: Timestamp.fromMillis(0), updatedAt: ts() }));

console.log('Timestamps (must be denied)');
await check('client-clock createdAt on create', 'deny', () =>
  setDoc(doc(phoneUser(), 'users', 'phone1c'), { name: 'Riya', phoneNumber: '+919876543210', createdAt: Timestamp.now(), updatedAt: ts() }));
await check('missing timestamps on create', 'deny', () => setDoc(doc(phoneUser(), 'users', 'phone1d'), { name: 'Riya' }));
await check('update without server updatedAt', 'deny', () => updateDoc(doc(phoneUser(), 'users', 'phone1'), { name: 'Riya' }));
await check('client-clock updatedAt', 'deny', () => updateDoc(doc(phoneUser(), 'users', 'phone1'), { name: 'Riya', updatedAt: Timestamp.now() }));
await check('change createdAt on update', 'deny', () => updateDoc(doc(phoneUser(), 'users', 'phone1'), { createdAt: ts(), updatedAt: ts() }));
await check('client-clock personalityUpdatedAt', 'deny', () =>
  updateDoc(doc(phoneUser(), 'users', 'phone1'), { personalityProfile: 'hi', personalityUpdatedAt: Timestamp.now(), updatedAt: ts() }));

console.log('Types and lengths (must be denied)');
await check('name too short', 'deny', () => updateName(phoneUser(), 'phone1', 'R'));
await check('name too long', 'deny', () => updateName(phoneUser(), 'phone1', 'a'.repeat(51)));
await check('name with digits/markup', 'deny', () => updateName(phoneUser(), 'phone1', '<b>Riya1</b>'));
await check('name with outer spaces', 'deny', () => updateName(phoneUser(), 'phone1', ' Riya '));
await check('name not a string', 'deny', () => updateDoc(doc(phoneUser(), 'users', 'phone1'), { name: 42, updatedAt: ts() }));
await check('personality over 12,000 chars', 'deny', () => savePersonality(phoneUser(), 'phone1', 'x'.repeat(12001)));
await check('personalitySkipped not a bool', 'deny', () => updateDoc(doc(phoneUser(), 'users', 'phone1'), { personalitySkipped: 'yes', updatedAt: ts() }));
await check('unknown field', 'deny', () => updateDoc(doc(phoneUser(), 'users', 'phone1'), { isAdmin: true, updatedAt: ts() }));

console.log(`\n${pass} passed, ${fail} failed`);
await env.cleanup();
process.exit(fail ? 1 : 0);
