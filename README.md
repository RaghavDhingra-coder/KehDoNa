# KehDoNa

> **Dil mein hai, toh KehDoNa.**

KehDoNa is a campus social and dating platform for discovering compatible people, sharing anonymous
confessions, and finding mutual secret crushes.

**Phase 2 (current):** Firebase accounts (phone OTP, email + verification, Google) and the
"Discover your vibe" AI personality profile in the Android app. Confessions, matching, Secret Crush
and messaging are **not** implemented yet.

## Android app (`app/`)

The native KehDoNa app: Kotlin, Jetpack Compose, Material 3, type-safe Navigation Compose,
DataStore. Package: `com.codecrafters.kehdona`. Open the project root in Android Studio, or:

```bash
./gradlew assembleDebug          # build app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest      # ViewModel tests + Compose UI/navigation tests (Robolectric, no emulator)
./gradlew installDebug           # install on a connected device/emulator
```

Source layout (`app/src/main/java/com/codecrafters/kehdona/`):

| Package | Contents |
| --- | --- |
| `ui/theme` | Brand colours, Fraunces + Plus Jakarta Sans typography (bundled in `res/font`), shapes. Dynamic colour is off. |
| `ui/components` | Logo, buttons, badges, background, `LoadingState` / `ErrorState` / `EmptyState`, illustrative preview cards |
| `ui/screens` | `start` (launch state), `welcome`, `onboarding`, `auth` — screens + ViewModels |
| `ui/screens/auth` | Auth entry, sign-up / log-in form with OTP step, `AuthViewModel` |
| `ui/screens/account` | Email verification and profile-name setup |
| `ui/screens/vibe` | "Let's discover your vibe ✨", ChatGPT handoff, paste screen (auto-saves a valid reply) |
| `ui/screens/profile` | My Profile: the finalized personality profile, read-only |
| `ui/screens/home` | Signed-in landing: private profile and log-out |
| `navigation` | `@Serializable` routes and `KehDoNaNavHost` |
| `domain` | Validation, `SessionResolver` (which screen a member resumes at), the personality prompt and `PersonalityParser` |
| `data/auth`, `data/profile` | Firebase Auth, Credential Manager (Google), Firestore profiles, each behind an interface |
| `data/onboarding`, `model`, `di` | `OnboardingRepository` (DataStore), models, `AppContainer` (manual DI) |

The web `client/` and `server/` below are a separate, earlier prototype and are not used by the app.

## Phase 2: accounts and personality

### Account flow

1. **Sign up**: name + phone (SMS OTP), name + email + password (verification email), or Google.
2. Email accounts must click the verification link before continuing.
3. Accounts without a profile (e.g. first Google log-in) are asked for their name.
4. **Let's discover your vibe ✨**: *Discover My Personality* copies a fixed prompt to the clipboard
   and opens the ChatGPT app (or chatgpt.com). Pasting ChatGPT's reply (or *Paste from clipboard*)
   saves it automatically, locks it, and goes straight to Home, with no confirmation step. Pastes
   that aren't a reply to the prompt (including the prompt itself) are rejected. If saving fails,
   nothing is locked and the reply stays on screen (and survives process death) for a retry.
5. **The personality profile is final.** There is no edit, replace, re-import or regenerate. The
   signup name is never changed by anything ChatGPT writes.
6. Home shows a personal welcome, *Open My Profile* and *Log out*. **My Profile** shows the sections
   the reply actually supports (with Observed / Inferred labels) and the full original reply. It is
   loaded from Firestore, so it's there after log-out/log-in, restarts and reinstalls.

Passwords and OTP codes go straight to Firebase and are never stored by the app. Profiles live in
Firestore at `users/{uid}`, readable and writable only by their owner (`firestore.rules`). The rules
also require server timestamps (`createdAt` can never change), require `email` / `phoneNumber` to
match the caller's Firebase Auth ID token, and check every field's type and length. Creating a
profile runs in a transaction, so a retry only updates the name and never overwrites `createdAt`.

The personality profile lives in its own document, `personalityProfiles/{uid}`: the reply verbatim
(`response`), `promptVersion` and a server `importedAt`. The rules let the owner **create it once**
(only after `users/{uid}` exists) and read it; **update and delete are denied for every client**,
and nobody else can read or list it. Saving runs in a transaction, so an offline save fails instead
of queueing, and a retry after an ambiguous failure returns the already-locked profile.

Firebase has no built-in email OTP, so email accounts are verified with Firebase's verification
link. A 6-digit email code would need a backend (e.g. a Cloud Function issuing custom tokens).

### Firebase setup (required — sign-in is disabled until this is done)

Without `app/google-services.json` the app still builds, but every sign-in screen says Firebase
isn't configured and no sign-in is attempted.

1. Create a project at <https://console.firebase.google.com> and add an **Android app** with package
   `com.codecrafters.kehdona`.
2. Add your signing certificate fingerprints (**SHA-1 and SHA-256**) under *Project settings › Your
   apps*. Get them with `./gradlew signingReport`. Add the debug key, your release/upload key and,
   if you use Play App Signing, the Play signing key. Phone auth and Google sign-in fail without them.
3. Download **`google-services.json`** into `app/` (it's git-ignored). The Google Services Gradle
   plugin is applied automatically when the file exists.
4. *Authentication › Sign-in method*: enable **Phone**, **Email/Password** and **Google** (set a
   support email). Enabling Google creates the web OAuth client whose ID the app reads automatically.
5. Phone auth: Firebase uses Play Integrity (reCAPTCHA as fallback). Enable the **Play Integrity
   API** in Google Cloud for the project. Phone auth needs the **Blaze** plan for real SMS beyond
   the free quota; for testing, add numbers and codes under *Phone numbers for testing*.
6. *Firestore Database*: create a database (production mode), then publish `firestore.rules`:
   paste it into Console › Firestore › Rules, or run `firebase use --add` (pick the project) and
   `firebase deploy --only firestore:rules`. `firebase.json` is already configured; don't run
   `firebase init`, which would overwrite it. Run the rules tests below first.
7. Optional: customise the verification and password-reset email templates under
   *Authentication › Templates*.

### Verifying Phase 2

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease   # Android: tests, lint, builds

npm --prefix firestore-tests install   # once
npm run test:rules                     # Firestore security rules on the local emulator
```

The rules tests need **JDK 21+** on `PATH` (Android Studio's bundled JBR works:
`export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`). They run against
the `demo-kehdona` emulator project, which can never touch a real Firebase project, and replay the
exact writes the app makes for phone, email and Google accounts, plus the access that must be denied.

Android tests use fake repositories, so they cover the flows and ViewModels but not the live
Firebase services. After connecting a Firebase project, check on a real device:

1. Phone sign-up with a test number from *Phone numbers for testing* → code screen → vibe screen.
2. Email sign-up → verification email arrives → "I've verified my email" moves on.
3. Google sign-in → new account asks for a name; returning account opens home.
4. Discover My Personality → ChatGPT opens with the prompt copied → paste the reply → lands on Home
   → Open My Profile shows the sections and the full reply; there is no way to change it.
5. Log out → auth entry; relaunch → the right screen resumes.
6. In the Firestore console, `users/{uid}` and `personalityProfiles/{uid}` contain only the fields
   listed in `firestore.rules`.

### Sign-in troubleshooting

Errors end with the real Firebase code, e.g. "(ERROR_OPERATION_NOT_ALLOWED)". Logcat tags
`KehDoNaAuth` / `KehDoNaProfile` log the code only (never messages, numbers, codes or tokens).

| Shown code | Cause | Fix (Firebase console) |
| --- | --- | --- |
| `MISSING_DEFAULT_WEB_CLIENT_ID` | Google isn't enabled, so `google-services.json` has no web OAuth client | Authentication › Sign-in method › enable **Google**, then download `google-services.json` again |
| `ERROR_OPERATION_NOT_ALLOWED` | That provider is switched off | Authentication › Sign-in method › enable it (Phone / Email/Password / Google) |
| `ERROR_APP_NOT_AUTHORIZED`, `ERROR_MISSING_CLIENT_IDENTIFIER`, `ERROR_INVALID_APP_CREDENTIAL` | This build's SHA fingerprints aren't registered, or Play Integrity / reCAPTCHA failed | Project settings › Your apps › add SHA-1 and SHA-256 (`./gradlew signingReport`); enable the Play Integrity API |
| `BILLING_NOT_ENABLED` | Phone auth needs a billing-enabled project | Upgrade to Blaze, or test with *Phone numbers for testing* |
| `Firestore: PERMISSION_DENIED` | The project's Firestore rules aren't KehDoNa's (the console default denies everything) | Publish `firestore.rules` (Firestore › Rules) |
| `Firestore: NOT_FOUND` / `FAILED_PRECONDITION` | No Firestore database | Firestore Database › Create database |

On an emulator, Play Integrity can't pass, so phone sign-in falls back to a reCAPTCHA page; test
numbers from *Phone numbers for testing* skip SMS entirely.

## Repository layout

```
KehDoNa/
├── client/                 React + Vite + TypeScript + Tailwind CSS (web app)
│   ├── src/
│   │   ├── components/
│   │   │   ├── ui/          Design-system primitives (Button, Badge, Container, …)
│   │   │   ├── layout/      Navbar, Footer
│   │   │   └── landing/     Landing-page sections and preview cards
│   │   ├── hooks/           useInView, useScrolled, useApiHealth, useDocumentTitle
│   │   ├── layouts/         RootLayout (nav + footer + hash scrolling)
│   │   ├── lib/             API client, button styles, helpers
│   │   ├── pages/           LandingPage, PlaceholderPage
│   │   ├── styles/          index.css — design tokens (colours, type, radii, shadows)
│   │   ├── App.tsx          Routes
│   │   └── main.tsx
│   └── .env.example
├── server/                 Express 5 + TypeScript + Prisma 7 (API)
│   ├── src/
│   │   ├── routes/          health.ts
│   │   ├── middleware/      errorHandler.ts
│   │   ├── lib/             env, prisma, database check, HttpError
│   │   ├── app.ts           Express app factory (testable)
│   │   └── index.ts         Entry point, graceful shutdown
│   ├── prisma/
│   │   ├── schema.prisma
│   │   └── migrations/
│   ├── prisma.config.ts
│   ├── tests/               Vitest + Supertest
│   └── .env.example
├── app/, gradle/, …        Android Studio project (the KehDoNa app)
├── firestore.rules         Firestore security rules for user profiles
├── firestore-tests/        Emulator tests for firestore.rules (`npm run test:rules`)
├── firebase.json           Firebase CLI config (rules path, emulator port); no project is linked
├── docker-compose.yml      Optional local PostgreSQL
├── .env.example            Variables for docker-compose
└── package.json            Convenience scripts that run both apps
```

## Prerequisites

- **Node.js 20.19+** (22 LTS or newer recommended) and **npm 10+**
- **PostgreSQL 14+**, either a local install, Docker, or a hosted database (Neon, Supabase,
  Railway, …)
- Git

## Quick start

```bash
# 1. Install dependencies (the server's postinstall also runs `prisma generate`)
npm run install:all

# 2. Create local env files from the templates
cp client/.env.example client/.env
cp server/.env.example server/.env
#    Then edit server/.env and set DATABASE_URL (see "Database" below).

# 3. Apply database migrations
npm --prefix server run db:migrate

# 4. Start the API (http://localhost:4000) and the web app (http://localhost:5173)
npm run dev:server      # terminal 1
npm run dev:client      # terminal 2
```

Open http://localhost:5173. In development the footer shows a small status line, for example
**"Dev · API online · database up"**. This confirms the browser can reach the API.

## Environment variables

### `server/.env` (server-only, never sent to the browser)

| Variable       | Default                 | Description                                          |
| -------------- | ----------------------- | ---------------------------------------------------- |
| `NODE_ENV`     | `development`           | `development`, `test` or `production`                |
| `PORT`         | `4000`                  | Port the API listens on                              |
| `CORS_ORIGINS` | `http://localhost:5173` | Comma-separated browser origins allowed to call the API |
| `DATABASE_URL` | none                    | PostgreSQL connection string used by Prisma          |

Variables are validated at startup with Zod. Invalid values stop the server with a clear message.
If `DATABASE_URL` is empty, the API still starts and reports `database.status: "not_configured"`.

### `client/.env`

| Variable       | Default                 | Description                     |
| -------------- | ----------------------- | ------------------------------- |
| `VITE_API_URL` | `http://localhost:4000` | Base URL of the KehDoNa API     |

Only `VITE_`-prefixed variables are bundled into the browser, so never put secrets here.

### Root `.env` (optional)

Only used by `docker-compose.yml`: `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_DB`,
`POSTGRES_PORT`.

## Database (PostgreSQL + Prisma)

The Phase 1 schema contains a single `users` table (`id`, `email`, `created_at`, `updated_at`).
Phase 2 will build on it. Pick **one** way to run PostgreSQL:

**A. Docker**

```bash
cp .env.example .env            # optional: change the password
docker compose up -d
# server/.env:
# DATABASE_URL=postgresql://kehdona:<password>@localhost:5432/kehdona?schema=public
```

**B. Existing local PostgreSQL (Homebrew, Postgres.app, installer)**

```bash
createdb kehdona                # or: psql -c 'CREATE DATABASE kehdona;'
# server/.env:
# DATABASE_URL=postgresql://<user>:<password>@localhost:5432/kehdona?schema=public
```

**C. Hosted PostgreSQL**: paste the provider's connection string into `DATABASE_URL`. Most hosts
need `?sslmode=require` at the end.

### Prisma commands (run inside `server/`)

| Command                     | What it does                                                      |
| --------------------------- | ----------------------------------------------------------------- |
| `npm run prisma:generate`   | Regenerates the typed client into `src/generated/prisma`           |
| `npm run db:migrate`        | **Development:** creates/applies migrations (`prisma migrate dev`) |
| `npm run db:deploy`         | **Production/CI:** applies pending migrations only                 |
| `npm run db:status`         | Shows which migrations have been applied                          |
| `npm run db:studio`         | Opens Prisma Studio to browse data                                |

`prisma migrate dev` may offer to **reset** the database when it detects drift. Only use it on a
development database, and use `db:deploy` everywhere else.

## API

### `GET /api/health`

Responds `200` whenever the API process is up. Database availability is reported separately, so a
database outage doesn't make the API itself look down.

```json
{
  "status": "ok",
  "service": "kehdona-api",
  "version": "0.1.0",
  "uptimeSeconds": 42,
  "timestamp": "2026-10-22T11:48:10.711Z",
  "database": { "status": "up", "latencyMs": 4 }
}
```

`database.status` is one of `up` (includes `latencyMs`), `down`, or `not_configured`.

### Errors

Every error uses the same shape:

```json
{ "error": { "code": "not_found", "message": "Route GET /api/nope does not exist" } }
```

Codes: `not_found` (404), `invalid_json` (400), `payload_too_large` (413), `cors_forbidden` (403),
`internal_error` (500).

## Scripts

From the repository root:

| Command              | Description                                       |
| -------------------- | ------------------------------------------------- |
| `npm run install:all`| Install client and server dependencies            |
| `npm run dev:client` | Vite dev server on :5173                          |
| `npm run dev:server` | API with auto-reload (tsx watch) on :4000         |
| `npm run build`      | Production builds → `client/dist`, `server/dist`  |
| `npm run typecheck`  | TypeScript checks for both apps                   |
| `npm run lint`       | ESLint for both apps                              |
| `npm test`           | Server tests (Vitest + Supertest)                 |
| `npm run check`      | All of the above                                  |
| `npm run test:rules` | Firestore rules tests on the local emulator (needs `npm --prefix firestore-tests install` and JDK 21+) |

Each app also has `npm run format` / `format:check` (Prettier). The server has `npm start` to run
the compiled build, and the client has `npm run preview` to serve its build.

## Design system

All tokens live in `client/src/styles/index.css` (Tailwind v4 `@theme`). Tailwind's default palette
is cleared, so only brand colours can be used:

- **night** (near-black aubergine surfaces), **plum**, **violet**
- **rose / blush** (primary actions, accents), **lavender / lilac** (secondary accents, focus)
- **cream** (occasional light sections), **ink** (text) and **line** (borders)

Typography: **Fraunces** (display) and **Plus Jakarta Sans** (body). Both are self-hosted via
Fontsource with system fallbacks, so there are no external font requests. Motion respects
`prefers-reduced-motion`.

## Troubleshooting

- **Footer says "API offline"**: the server isn't running, or `VITE_API_URL` doesn't match its
  port. Restart Vite after editing `client/.env`.
- **CORS error in the browser console**: add the web app's exact origin (scheme + host + port) to
  `CORS_ORIGINS` in `server/.env` and restart the server.
- **`database.status` is `down`**: check that PostgreSQL is running (`pg_isready`) and that the
  user, password, port and database name in `DATABASE_URL` are correct.
- **`P1000: Authentication failed`** from Prisma: wrong username or password in `DATABASE_URL`.
- **`P1003: Database does not exist`**: create it (`createdb kehdona`) or fix the name in the URL.
- **`Cannot find module '../generated/prisma/client.js'`**: run `npm --prefix server run prisma:generate`.
- **Port already in use**: change `PORT` in `server/.env` (and `VITE_API_URL` to match), or stop
  the other process.
