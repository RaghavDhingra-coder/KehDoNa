// Callable Cloud Functions for matching. Every call is tied to the signed-in Firebase user;
// the uid comes from the verified auth token, never from the request body.

import { initializeApp } from 'firebase-admin/app';
import { getFirestore } from 'firebase-admin/firestore';
import { setGlobalOptions } from 'firebase-functions/v2';
import { HttpsError, onCall, type CallableRequest } from 'firebase-functions/v2/https';
import { MatchError, MatchService } from './service';

initializeApp();
setGlobalOptions({ region: 'us-central1', maxInstances: 10 });

const service = new MatchService(getFirestore());

function requireUid(request: CallableRequest): string {
  if (!request.auth) throw new HttpsError('unauthenticated', 'Sign in to find a match.');
  return request.auth.uid;
}

async function run<T>(work: () => Promise<T>): Promise<T> {
  try {
    return await work();
  } catch (e) {
    if (e instanceof MatchError) throw new HttpsError('failed-precondition', e.message, { reason: e.reason });
    throw e;
  }
}

/** Returns { status: 'matched', matchId } | { status: 'waiting' }. */
export const requestMatch = onCall((request) => run(() => service.requestMatch(requireUid(request))));

/** Returns { status: 'idle' } or, if already matched, { status: 'matched', matchId }. */
export const cancelMatchRequest = onCall((request) => run(() => service.cancelRequest(requireUid(request))));
