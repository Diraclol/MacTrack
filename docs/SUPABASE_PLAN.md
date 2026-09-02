# Supabase backend + sync — design plan (ACCT-1/ACCT-2)

Concrete plan for adding a Supabase backend to MacTrack: accounts, per-user cloud sync/backup, and a
shared food database. This builds on the already-locked decisions in
[BACKEND_RESEARCH.md](BACKEND_RESEARCH.md) and the threat model in [SECURITY.md](SECURITY.md); it does
not re-open the stack choice. Nothing here is built yet — it is a plan to react to, and it ends with
the decisions still needed.

## 0. Principles (unchanged, load-bearing)

- **Room stays the source of truth. Offline-first is non-negotiable.** The backend is a sync + backup
  + sharing layer bolted onto a Room-first app, never its foundation. Every online feature degrades to
  the current offline behaviour (CNF + saved foods). This is what keeps the whole thing reversible.
- **Accounts are OPTIONAL.** "Continue without an account" stays the default flow (PROF-3). Signing in
  turns on sync/backup and (for some roles) the shared food DB; signing out just stops syncing and
  leaves local data intact.
- **Authorization is server-side only.** Roles live in the database (a JWT claim + Row-Level Security),
  never trusted from the client. No admin credential is ever hardcoded or committed.
- **The `domain`/`data`/`calc` layers stay Android-free** (for the later Compose-Multiplatform port).
  The sync engine is plain Kotlin/coroutines over a thin client interface.
- **Secrets never sync and never ship in the client.** The BYO Gemini key stays in Keystore-encrypted
  local prefs; it is never uploaded. The Supabase anon key is publishable (RLS is the real gate); the
  service-role key is never in the app.

## 1. What syncs vs what stays local

**Syncs (per-user, the 9 Room tables — same set the backup already serialises):** `food_items`,
`meal_entries`, `recipes`, `recipe_ingredients`, `meal_templates`, `meal_template_items`, `goals`,
`weight_entries`, `user_profile`.

**Stays device-local (never leaves the phone):** everything not in Room — theme/UI prefs, favourite
units, nutrient order, reminder/AI toggles (`ThemeRepository`), the **Gemini API key + AI settings**
(`AiSettingsStore` — secret), the in-memory cart + ingredient builder, the viewed-day state, and the
avatar JPEG file. These are device preferences, not user data.

## 2. Postgres schema

Two groups of tables.

### 2a. Per-user data (one Postgres table per Room table)

Each mirrors its Room table's columns **plus three sync columns**, and keeps the Room UUID as the key:

- `id uuid` (or `text`) primary key — the same client-generated UUID Room already uses (no new ids).
- `user_id uuid not null references auth.users(id)` — the owner. Stamped by the client on push;
  enforced by RLS (see §3). **Not added to Room** — a local DB belongs to whoever is signed in, so the
  owner is known at push time; keeping Room `user_id`-free avoids a schema change to every table and
  keeps single-user local logic simple.
- `updated_at timestamptz not null default now()` — drives last-write-wins.
- `deleted boolean not null default false` — tombstone, so deletions propagate (see §5).

`user_profile` is the one shape change: today it is a **device singleton** (`id = 0`). In Postgres it
becomes **one row per user keyed by `user_id`** (drop the fixed `0` PK; `user_id` is the identity). The
child tables (`recipe_ingredients`, `meal_template_items`) keep their plain-string parent links
(`recipe_id`, `template_id`) and `food_id`; model them as real FKs **within the same user's rows**,
`on delete cascade` for the true ownership links (ingredient→recipe, item→template) but **not** for
`food_id` (a custom food can be deleted without touching historical logs). `meal_entries.source_id`
stays a plain nullable string — it is polymorphic (CNF asset code / food id / barcode) and must not be
a foreign key; `meal_entries` rows are frozen snapshots, never recomputed.

### 2b. Shared/community food database (ACCT-2)

One table, e.g. `shared_foods`: the `food_items` columns + `contributed_by uuid`, `status text`
(`pending` / `approved` / `quarantined`), `created_at`, `updated_at`. Access pattern: **everyone reads
`approved`; Btesters insert (as `pending`); admin moderates.** Server-side validation (name
length/charset, macros finite/non-negative/bounded, serving > 0) + per-user write rate-limits enforced
by RLS + a Postgres trigger/policy, so a bad contributor can't poison everyone's search. This is a
Phase-4 item; it does not block personal sync.

### 2c. Roles

A `role` value per user — `admin` | `btester` | `regular` — carried as a custom JWT claim
(`app_metadata.role`, set server-side), read by RLS. Admin is created once in the console and gated by
its user id / claim; the password is never in the repo. (This maps SECURITY.md's Firebase-era
"custom claims" onto Supabase — see §8.)

## 3. Row-Level Security (the real access gate)

- **Per-user tables:** `enable row level security`; policy `user_id = auth.uid()` for select/insert/
  update/delete. A signed-in user can only ever see and write their own rows, even with a tampered
  client. Deny-by-default (no policy = no access).
- **`shared_foods`:** `select` where `status = 'approved'` for everyone (incl. anon read if we want);
  `insert` only where `auth.jwt()->>'role' in ('btester','admin')` and `status = 'pending'`; `update`
  (moderation) only for `admin`. Rate-limit inserts via a policy/trigger counting recent rows per user.
- **Discipline:** every policy is tested with a *non-owner* account before it is trusted — a wrong RLS
  policy either leaks or locks data. This is the single highest-risk area.

## 4. Auth

- **SDK:** `supabase-kt` (GoTrue + Postgrest, later Realtime), added under the ask-before-adding-a-dep
  rule when Phase 1 starts. Pin the version; confirm the current 3.x at add time.
- **Methods:** email/password + Google sign-in (both native in Supabase Auth).
- **PROF-3 onboarding:** first screen offers "Create an account" vs "Continue without an account".
  Until Phase 1 ships, "Create an account" shows as coming-soon; after, it runs the real flow. "Continue
  without" remains the default and fully-functional path forever.
- **Session:** the GoTrue session (refresh token) is persisted in Keystore-encrypted storage, same
  norm as the Gemini key. Sign-out clears the session and stops sync; local data stays.

## 5. Sync engine (the hard part — this is the real work of ACCT-1)

Supabase gives the pipes (auth, RLS, REST/Realtime); the Room↔Postgres reconciliation is ours.

- **Model: per-row last-write-wins by `updated_at`, with tombstones for deletes.** Simple, robust
  enough for a single user across a few devices; no CRDTs.
- **Push:** on a trigger (app foreground, after a local write settles, and/or periodic), send local rows
  whose `updated_at` > `last_pushed_at` to Postgres (upsert on `id`), stamping `user_id`. Order parents
  before children (recipes before recipe_ingredients; foods before entries that reference them).
- **Pull:** fetch rows where `updated_at` > `last_pulled_at` (a per-table or global cursor). For each,
  compare to the local row: **the higher `updated_at` wins**; apply upserts, and apply `deleted = true`
  as a local delete (or soft-delete — see below).
- **Deletes / tombstones:** two viable approaches — a **decision** (§10):
  1. **Soft-delete column** (`deleted`) on every table: deletes set `deleted = true, updated_at = now`;
     all reads filter `where deleted = false`; a periodic purge drops old tombstones. Most robust
     (handles delete-on-A vs edit-on-B via LWW) but touches every query.
  2. **Tombstone table** (`sync_tombstones(table, row_id, deleted_at)`): each DAO delete also records a
     tombstone; reads are unchanged; push sends tombstones as cloud `deleted = true`. Less invasive to
     existing queries; slightly more moving parts.
- **Cursor + engine:** a small `SyncEngine` (plain Kotlin, coroutine `Flow`s) + a `sync_state` local
  table holding `last_pulled_at`/`last_pushed_at`. Triggered on app open and after writes; **not**
  Realtime at first (periodic + on-open pull is simpler and easier on battery; add Realtime later if
  live multi-device is wanted).
- **Two fixes needed before sync is clean:**
  - `weight_entries` currently deletes+reinserts on a same-day edit (id churn). Change
    `replaceForDate` to update the existing row in place (stable id) so an edit is an update, not a
    delete+insert.
  - `food_items` has no `updated_at` at all — it must get one (see §6) or its edits can't win LWW.

## 6. Room migration 8 -> 9 (add the sync columns)

One additive migration, following the project's migration discipline (add `Migration(8,9)`, register
it, rebuild so the exported schema JSON is regenerated and committed, matching `@ColumnInfo` defaults on
both sides, never destructive):

- Add `updated_at INTEGER NOT NULL DEFAULT 0` (ms) to the seven tables that lack it: `food_items`,
  `recipes`, `recipe_ingredients`, `meal_templates`, `meal_template_items`, `goals`, `weight_entries`.
  (`meal_entries` and `user_profile` already have `updatedAt`.) Stamp it in every write path.
- If soft-delete is chosen (§5): add `deleted INTEGER NOT NULL DEFAULT 0` to all nine tables and filter
  reads. If tombstone-table is chosen: add a `sync_tombstones` table instead and no per-table `deleted`.
- No `user_id` in Room (owner is stamped at push).

This migration is invisible to the offline app (all defaults), so it can land **before** any backend
code — it just starts stamping `updated_at`, which is harmless until sync exists.

## 7. Staged rollout (each phase shippable + reversible)

- **Phase 0 — project + safety net.** Create the Supabase Cloud (Free) project; a daily keep-alive
  ping (GitHub Action hitting the REST API) so the free tier doesn't pause; write the SQL schema + RLS;
  test every policy with a non-owner account. No app code yet.
- **Phase 1 — auth + accounts (ACCT-1, part 1).** Add `supabase-kt`; PROF-3 account-choice onboarding;
  email/password + Google sign-in; a `profiles` row + role claim; admin created in console. Sync still
  off — signing in does nothing visible yet. This alone is a real, demoable "accounts" milestone.
- **Phase 2 — one-way backup (push only).** The migration (§6) + a `SyncEngine` that pushes local rows
  to Postgres. Gives cloud backup + "restore on a fresh install" without conflict complexity.
- **Phase 3 — two-way sync.** Add pull + LWW + tombstones. Now multi-device works. This is the hard,
  well-tested phase (conflict cases, ordering, delete propagation).
- **Phase 4 — shared food DB (ACCT-2).** `shared_foods` + moderation RLS; Btester contribute, admin
  moderate, everyone search. Independent of personal sync.
- **Phase 5 (later) — self-host.** Move to the home-lab box (Envoy + Supavisor behind a Cloudflare
  Tunnel, API gateway only, Postgres never exposed, Studio LAN/Tailscale-only). RLS/Auth/JWTs are
  identical, so roles behave the same — "shared ops, not extra ops."

## 8. Security notes / SECURITY.md mapping

SECURITY.md predates this decision and still names **Firebase**; its **threat model and principles stay
authoritative**, but the technology names map onto Supabase:

- Firebase Auth + custom claims -> **GoTrue auth + a `role` claim in `app_metadata`**.
- Firestore/RTDB security rules -> **Postgres Row-Level Security policies**.
- Firebase App Check -> **Supabase App Check / Play Integrity** (so only our app can call the backend).

Unchanged rules: never trust the client for authz; roles server-side only; **no hardcoded admin
password ever** (it lands in the APK + git history forever); a shared/admin key, if ever needed, sits
behind a server-side proxy (an Edge Function), never in the client; HTTPS only; deny-by-default. When
this design is folded into the codebase, SECURITY.md should be updated to the Supabase terms.

## 9. Risks (going in with eyes open)

- **RLS is the sharp edge** — a wrong policy leaks or locks data. Test with a non-owner every time.
- **Sync is genuinely hard** — Supabase is not a turnkey offline-sync engine. LWW + tombstones is the
  chosen, deliberately-simple strategy; edge cases (delete-vs-edit, clock skew) need real tests.
- **Free-tier pause** after ~7 days idle — solved by the daily keep-alive; paused projects don't count
  against the 2-project free cap.
- **New dependency** (`supabase-kt`) — a deliberate, pinned addition at Phase 1.
- **Clock skew** in LWW — stamp `updated_at` from a monotonic-ish source and tolerate small skew;
  server `now()` on push can be the tiebreaker if needed.

## 10. Decisions

**Locked (Dirac, 2026-09-02):**

- **Order vs the public release: POST-v1.** Accounts/sync land *after* the Android public release, as a
  distinct milestone (and resume showcase). So this whole plan is parked until the local app has
  shipped publicly — it stays off the release critical path. Do NOT start building it before then.
- **Deletes: the `sync_tombstones` table** (not per-table soft-delete columns) — keeps existing
  reads/queries unchanged. So migration 8->9 adds only `updated_at` to the seven tables, plus the new
  `sync_tombstones` table; no per-table `deleted` flag.
- **Sync cadence: periodic + on-app-open + after-write.** No Realtime at first (can add later if live
  multi-device is wanted).

**Still open (defaults fine; confirm when Phase 0 actually starts, post-v1):**

- **Sync free for everyone or tiered?** Default: free for any signed-in user; roles only gate the
  shared food DB + new-feature flags.
- **Sign-out behaviour:** default keep local data (it's the user's; app is local-first); on account
  *switch* on one device, clear + re-pull.
- **Cloud-Free-first vs Pro:** default Cloud Free + daily keep-alive; self-host later (existing plan).

When the public release is out, Phase 0 (schema + RLS SQL, no app code) is the first concrete,
reversible step.
