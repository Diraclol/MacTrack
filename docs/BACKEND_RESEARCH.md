# Backend research — Supabase vs Neon (for ACCT-1)

**Question:** MacTrack will eventually need accounts, roles (admin / Btester / regular), a shared food
database, and cloud sync. Is **Neon** a better backend than **Supabase** for that?

**Short answer: Supabase.** For MacTrack's shape — a local-first Android app, ≤5 users, that needs
auth + **server-side role enforcement** + an API with almost no backend of its own — Supabase gives you
all of that out of the box. Neon is an excellent *serverless Postgres*, but it is only the database; it
would force you to build and host the auth layer, the API layer, and the role-enforcement layer
yourself. That's the opposite of what a solo beginner-Kotlin dev shipping a 5-user app wants.

Consider Neon later only if you outgrow Supabase's opinionated model, or you specifically want its
database branching / true scale-to-zero for a heavier dev workflow.

---

## What each one actually is

| | **Supabase** | **Neon** |
|---|---|---|
| Category | Backend-as-a-Service (BaaS) on Postgres | Serverless Postgres database, only |
| Auth | Built in (email/password, Google, etc.) | **None** — bring your own (Clerk, Auth.js, Firebase Auth…) |
| Role enforcement | **Row-Level Security (RLS)** in the DB, enforced server-side | RLS exists in Postgres, but you must build the API + auth that sets the DB role/claims |
| API to the app | Auto REST + GraphQL (PostgREST); realtime; storage | **None** — you write your own server / serverless functions |
| Android client | `supabase-kt` SDK (auth + Postgrest + realtime modules) | No client SDK — the app hits *your* backend |
| DB branching | No | **Yes** (its headline feature) |
| Scale-to-zero | Free project pauses after ~7 days idle | Real serverless scale-to-zero, fast cold start |
| Free tier (plenty for 5 users) | 500 MB DB, ~50k MAU auth, 1 GB storage | ~0.5 GB storage, generous compute hours |

The decisive row is **role enforcement**. `docs/SECURITY.md` requires that roles are enforced
server-side and never client-trusted. Supabase's RLS is exactly that: policies live in the database, so
even a tampered client cannot read/write rows it isn't allowed to. With Neon you'd re-create that whole
apparatus (an auth provider that mints claims + a backend that trusts them + SQL policies) before you
could enforce a single role.

---

## Mapping MacTrack's needs

- **Accounts (ACCT-1)** — Supabase Auth does Google + email/password directly, with a Kotlin SDK. Neon:
  add a separate auth provider and wire it up.
- **Roles (admin / Btester / regular)** — a `role` claim on the user + RLS policies (`Btester` can
  `INSERT` into the shared foods table; everyone can `SELECT`; only `admin` sees new features via a
  flag). Server-enforced, forgeable by no one. Neon: you build this.
- **Shared food database (ACCT-2)** — one Postgres table + RLS + a `status` column for
  moderation/rate-limiting. Natural in Supabase.
- **Sync** — the app stays **Room-first / offline-first**; the backend is a sync + backup + sharing
  layer, not the source of truth. Reconciliation (per-row `updatedAt`, last-write-wins to start) is a
  real design task either way — the backend choice doesn't change that.
- **Cost** — both free tiers comfortably cover 5 users; cost is not the tiebreaker.

Nothing about MacTrack today (barcode/OFF lookup, the CNF asset, the AI BYO-key feature) is affected by
this choice — they stay as-is.

---

## Caveats to go in with (Supabase)

- **RLS has a learning curve.** Policies must be written carefully — a wrong policy leaks or locks data.
  Test every policy with a non-owner account before trusting it.
- **New dependency set.** `supabase-kt` (GoTrue / Postgrest / Realtime modules) lands when ACCT-1 starts
  — a deliberate addition, gated behind the NOTES.md "ask before adding deps" rule.
- **Free-tier pause.** A Supabase project pauses after ~7 days of zero activity; daily use keeps it
  awake, and one dashboard click un-pauses it. Not a concern for an active user.
- **Sync is still the hard part.** Supabase gives you the pipes (auth, RLS, API, realtime), not a
  turnkey offline-sync engine. The Room↔Postgres reconciliation is ours to design and is the real work
  of ACCT-1 — pick a strategy (per-row timestamps, tombstones for deletes) up front.

## When Neon would win (not now)

- You already have (or want) a custom backend and just need the best serverless Postgres under it.
- You want **database branching** for preview environments / heavy schema iteration.
- You expect spiky, near-zero-baseline traffic where scale-to-zero economics matter — irrelevant at 5
  users.

---

## Recommendation

**Use Supabase for ACCT-1/ACCT-2.** It supplies the server-side role enforcement the security model
demands, plus auth and an auto API with a Kotlin SDK, so the amount of backend you build and secure
stays small — the right trade for a solo dev and ≤5 users. Revisit Neon only if the app later outgrows
Supabase's model or needs DB branching. The BYO-key/offline-first architecture we're already committed
to keeps this reversible: the backend is a sync layer bolted onto a Room-first app, not its foundation.
