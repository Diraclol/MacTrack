# MacTrack — security assessment

Scope: the current local-first app, plus the threat model for the planned features (accounts +
roles, a shared food database, import/export, and AI). No emojis, per house style.

## 1. Current posture — small and solid

The app today is local-only: no backend, no accounts, one on-device Room database plus a bundled
read-only SQLite asset. That keeps the attack surface tiny, and the code that touches untrusted
input is written defensively.

- **SQL injection: not present.**
  - Room DAOs use `@Query` with bound `:params` — parameterized and verified at compile time.
  - The raw-SQLite CNF search (`data/repository/CnfRepository.kt`) builds `name LIKE ?` clauses and
    passes the user's terms as **bind arguments**, never string-concatenated into SQL. The column
    list and `LIMIT` are code constants, not input. `getFood`/`measures` bind the code with `?`.
    The CNF DB is opened `OPEN_READONLY`.
- **Network: safe.** The only network call is the Open Food Facts barcode lookup
  (`data/off/OpenFoodFactsRepository.kt`). It **filters the barcode to digits only** before putting
  it in the URL (no path/URL injection), talks to a single fixed HTTPS host (no SSRF), sets
  timeouts, catches all exceptions to null, and parses the response with `org.json` (a safe parser).
- **Android surface: minimal.** Manifest declares only `INTERNET`. The single `exported` component
  is `MainActivity` (the required LAUNCHER entry). No content providers, services, or receivers are
  exported. No other app can reach the data.
- **Data at rest.** The Room DB lives in the app's private sandbox. For a single-user local tracker
  with no PII, that is acceptable.

Minor hardening to keep in mind even now:
- `android:allowBackup="true"` lets device backups include the DB. Fine while there is no sensitive
  data; revisit (or scope `backup_rules`) once accounts/PII exist.
- Make sure release builds are `minifyEnabled` (R8) and never `debuggable`. Keep cleartext traffic
  off (`usesCleartextTraffic=false`, the default on modern targetSdk).

## 2. Planned features — where the real risk is

### 2.1 Accounts and roles (admin / Btester / regular) — "auth jumping" / privilege escalation
This is the highest-risk area. The rule: **never trust the client for authorization.**

- A role stored on the client (a boolean, a prefs value, a field in the local DB) can be flipped by
  anyone with a rooted device or a repackaged APK. Gating a feature only in the UI is not security —
  it is a suggestion.
- Enforce roles **server-side**: Firebase Auth **custom claims** for the role, and **Firestore/RTDB
  security rules** that check the claim on every read/write. The client UI hides admin/Btester
  features for convenience; the backend rules are what actually stop a regular user from writing
  admin/Btester data.
- **Do not ship a hardcoded admin password.** A literal password in the source is in the APK and in
  git history forever (and this repo is meant to read as a solo/private project). Create the admin
  account once in the Firebase console and grant it the `admin` claim there (console or a Cloud
  Function); the password lives in Firebase, never in code. Same for the Btester role.
- Prefer Google Sign-In or Firebase email/password. If email/password, enforce verification and a
  sane password policy, and rely on Firebase's built-in rate limiting against credential stuffing.

### 2.2 Shared food database (Btesters add foods everyone sees) — poisoning / abuse
Untrusted writes into shared, globally-visible data.

- **Validate every field server-side** (a Cloud Function or strict Firestore rules): name length and
  charset, macros as finite non-negative numbers within sane bounds (reject negative/absurd
  calories), serving size > 0. Do not rely on client validation.
- **Moderation + rate limits.** A malicious Btester can spam entries or inject offensive/garbage
  names that all users then see. Add per-user write rate limits, a soft review/flag queue, and the
  ability to quarantine or roll back a contributor's entries.
- **Output safety.** Food names are inert in Compose `Text`, but the moment they are put into a
  WebView, an HTML export, or a CSV opened in a spreadsheet (formula injection via a leading `=`,
  `+`, `-`, `@`), they become dangerous. Sanitize on export (prefix risky cells with `'`).

### 2.3 Import / export (JSON) — untrusted parsing
Export is low-risk. Import parses a file the user (or someone) hands the app.

- Validate against an expected schema; reject unknown shapes. Clamp/validate values (dates parseable,
  macros finite and bounded) before writing to the DB.
- Cap file size to avoid a memory/DoS blow-up. Stream if large.
- If import can overwrite existing logs, **confirm with the user** and prefer merge-with-preview over
  blind replace. Never delete on import without confirmation.
- Use `org.json`/kotlinx.serialization with explicit types. Avoid reflection-based deserializers that
  can instantiate arbitrary classes.

### 2.4 AI / API keys (Gemini) — key exposure
`MacTrack.txt` already lists "putting API keys directly in the app" as a top mistake. Hold that line.

- **The admin's shared Gemini key must never be embedded in the client** (APKs decompile trivially).
  Put it behind a server-side proxy (a Cloud Function) that holds the key and enforces the caller's
  role/quota; the app calls the proxy, not Gemini directly.
- **BYO-key users**: store their key in `EncryptedSharedPreferences` (Android Keystore-backed), never
  in plaintext prefs, never in logs, never committed. Let them clear it. Send it only over HTTPS to
  the model endpoint.
- Follow the existing product rule: AI output is a **suggestion that the user reviews** before it is
  saved — this also limits the blast radius of a bad/prompt-injected AI response.

### 2.5 Transport and platform
- HTTPS everywhere (OFF and Firebase already are). Keep cleartext disabled.
- Least privilege on permissions: `INTERNET` now; add `CAMERA` only when barcode scanning ships;
  never microphone. Request camera at runtime, at point of use.
- Release hardening: R8/minify + resource shrinking, no debug builds shipped, consider Play
  Integrity / App Check (Firebase App Check) so only genuine app instances can hit the backend.

## 3. Prioritized checklist (when the backend lands)
1. Firebase Auth + custom claims for admin/Btester/regular; **no client-trusted role**.
2. Firestore/RTDB security rules enforcing per-role read/write; deny by default.
3. Firebase App Check so only your app can call the backend.
4. Server-side validation + rate limiting on shared-food writes; a moderation/rollback path.
5. Gemini key behind a server proxy; BYO keys in EncryptedSharedPreferences.
6. Import validation (schema, bounds, size, confirm overwrite) + CSV formula-injection escaping on export.
7. Release build hardening (R8, no debuggable, allowBackup review once PII exists).

Nothing in the current codebase is exploitable that I can see; this document is the guardrail for the
next phase, where the interesting attacks (privilege escalation and shared-data poisoning) become
possible.
