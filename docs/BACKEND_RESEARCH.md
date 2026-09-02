# Backend research — Supabase vs Neon vs Convex (for ACCT-1)

**Question:** MacTrack will eventually need accounts, roles (admin / Btester / regular), a shared food
database, and cloud sync. Is **Neon** or **Convex** a better backend than **Supabase** for that?

**Short answer: Supabase** (Neon evaluated below; Convex evaluated in its own section at the end —
still Supabase). For MacTrack's shape — a local-first Android app, ≤5 users, that needs
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
  — a deliberate addition; I confirm any new dependency before adding it.
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

---

## Also considered: Convex (researched 2026-09)

**Verdict: still Supabase.** Convex is a genuinely strong, modern reactive backend (its online real-time
DX is excellent, and unlike `supabase-kt` it publishes a *first-party* Android SDK). But it doesn't beat
Supabase on any axis that matters for MacTrack, and loses on the ones that do:

| Factor (most important first) | Convex | Supabase | Better here |
|---|---|---|---|
| **Offline / local-first fit** | Connection-oriented (WebSocket); no durable on-device cache, no offline write queue across restarts, and — unlike its React client — **no optimistic updates on Android**. True offline needs PowerSync (experimental, Android support unconfirmed). | Also not turnkey offline, but stateless REST bridges to Room naturally; existing Kotlin+Room references exist. | **Supabase** |
| Android/Kotlin SDK | First-party `dev.convex:android-convexmobile`, but pre-1.0 (~v0.8, wraps a Rust client, occasional breaking changes). | `supabase-kt` community-maintained but mature 3.x, broad, lots of Android material. | Supabase (mature); Convex only "officially published" |
| Accounts + roles | Auth in beta; polished mobile path wants 3rd-party Clerk/Auth0. Roles = hand-written TS authz in every function. | Auth built in, free; roles = declarative Postgres RLS. | **Supabase** |
| Shared writable food DB | Document model + manual joins; every read/write via TS functions you write. | Relational Postgres (natural for the tabular CNF/food data); "all read / admins write" = a couple of RLS policies + auto REST API. | **Supabase** |
| Free tier | ~1M calls, 0.5 GB DB; no documented inactivity pause. | 500 MB DB, 50k MAU; **pauses after 7 days idle** (data kept). | ~tie (Convex avoids the pause; Supabase has more headroom) |
| Lock-in | Proprietary reactive/document model; FSL-1.1 (source-available, Apache after 2 yrs); self-host is Docker-based. | "Just Postgres" — pg dump/restore, self-hostable, lowest lock-in. | **Supabase** |
| Beginner learning curve | Adds a TypeScript server + Convex model + Node tooling + wiring 3rd-party mobile auth. | Stays in SQL (kin to the SQLite already shipped) + the Kotlin SDK. | **Supabase** |

**Why, in plain terms:** the app already lives in Room and that doesn't change — whichever backend we
pick, we hand-write the "pull server → Room, push local edits → server" sync layer ourselves. Convex's
headline feature (live reactive queries over a socket) never reaches Room automatically, so we'd pay its
complexity for a benefit an offline-first app can't use — and its Android client is *actively worse*
offline. Meanwhile the shared food DB is relational (a fit for Postgres), and "all read, admins write"
is nearly free on Supabase's RLS versus imperative role checks in every Convex function.

**Convex would only pull ahead** if MacTrack pivoted to live online collaboration as the core and you
were willing to learn TypeScript — not where it is today.

*Unconfirmed by the research (don't treat as settled): the exact current `supabase-kt` version; whether
Convex formally guarantees no free-tier pause; whether the experimental PowerSync–Convex connector
supports Android at all.*

---

## Self-host on the home lab vs Supabase Cloud (researched 2026-09)

I run a home lab, so the question was whether to self-host the Supabase Docker stack there instead of
using the cloud (prompted by the free tier pausing after 7 days idle).

**Decision: Cloud Free + a daily keep-alive ping for the beta; self-host later, co-located with the AI
box.** The deciding factors:

- **Home uptime becomes every user's login/sync uptime.** A power blip, ISP outage, or router reboot
  logs everyone out until it's back. The app is offline-first, so *local* use keeps working — only sync
  and fresh login block — but that's still worse than the cloud's zero-ops uptime.
- **Mobile reachability is the real work.** A phone on cellular can't hit `192.168.x.x`; you must expose
  the lab publicly over HTTPS (auth tokens require TLS). The safe path is a **Cloudflare Tunnel**
  (outbound-only, no port-forward, hides the home IP, free edge TLS), exposing **only** the API gateway
  and **never** Postgres:5432, with Studio kept LAN/Tailscale-only and all demo secrets rotated.
- **RLS + Auth are identical self-hosted.** Row-Level Security is native Postgres and self-hosted GoTrue
  issues the same JWTs — so the admin/beta-tester/regular roles behave the same either way. Self-hosting
  later loses nothing on the security model (the reason we picked Supabase).
- **The free-tier pause is cheap to defeat.** A daily cron/Action hitting the REST API keeps the project
  awake (widely done; not officially blessed, so treat as "very likely fine"). Paused projects don't
  count against the 2-active-project free cap.
- **Cost.** Self-host = $0 on the invoice but paid in ops time + outage risk. Cloud Free = $0 with the
  pause (solved). Cloud Pro ($25/mo) mainly buys away the pause + managed backups — nice-to-haves, not
  needs at ≤5 users.

**The natural Phase 2:** when the home lab box is stood up for the AI service, co-locate self-hosted
Supabase on it — then it's *shared* ops, not *extra* ops, and self-host's $0 + full RLS/auth parity
becomes genuinely attractive. Self-hosted stack note: the current Docker compose uses **Envoy** (gateway)
and **Supavisor** (pooler); older tutorials saying **Kong** are dated — follow the official docker guide.
