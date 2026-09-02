# MacTrack — Backlog

Deferred work, organized by what blocks it: **schema-migration-gated** (needs a Room migration + a
build so KSP regenerates the schema JSON), **follow-ups** (UI/feature work with no schema), and
**accounts, roles & AI** (needs a backend and product decisions). Shipped work is checked off at the
bottom. Item codes (SCHEMA-n, UI-n, ACCT-n, AI-n, REL-n) cross-reference commits.

Current status is in [MACTRACK_STATE.md](MACTRACK_STATE.md); the architecture in
[ENGINEERING_SUMMARY.md](ENGINEERING_SUMMARY.md); the threat model in [SECURITY.md](SECURITY.md).

---

## Open

### Schema-migration-gated

Each needs a `Migration(N-1, N)`, a database version bump, and a device build so KSP regenerates
`app/schemas/.../N.json` (currently v8) — done one migration at a time, never destructive.

- [ ] **PROF-1: Profile name.** A nullable `user_profile.name` via `Migration(8,9)` (DB v9); shown on
      Profile (editable) and asked for in onboarding. Local, no backend needed.

### Follow-ups (no schema)

- [ ] **RESEARCH-2: Other barcode nutrition databases.** Open Food Facts stays primary (free, keyless,
      global, Canada-friendly). USDA FoodData Central "Branded" is a possible optional fallback when OFF
      misses a code (has GTIN/UPC + extra micronutrients, but needs a key and is US-centric).
      Nutritionix / Barcode Lookup / Edamam are commercial. No change needed now.
- [ ] **UI-10: Instrumented tests.** Unit tests exist (calc engine, nutrient arithmetic, food mappers).
      Remaining: instrumented Compose tests (day strip, swipe-to-delete, totals toggle) — needs the
      emulator/device.
- [ ] **I18N-1: French localisation (fr-CA).** A large, incremental job and a **later feature branch**,
      after the public release. No i18n infrastructure exists yet (~300-400 hardcoded strings across 24
      screens). Planned mechanism: a Compose-level locale switch stored in `ThemeRepository` (no new
      deps, no base-class change), then pull strings into `res/values/` + `res/values-fr/` screen by
      screen. Food *data* names (CNF, Open Food Facts) stay in their source language.

### Accounts, roles & AI (needs a backend + product decisions)

Backend decided: **Supabase** (Postgres + Auth + Row-Level Security + a Kotlin SDK) — it supplies the
server-side role enforcement the security model requires with minimal backend to build. Neon and Convex
were both evaluated and rejected ([BACKEND_RESEARCH.md](BACKEND_RESEARCH.md)). This whole track is
scheduled after the public release.

- [ ] **ACCT-1: Auth + accounts.** Google + email/password sign-in; three roles (admin / Btester /
      regular) enforced server-side via a JWT claim + RLS; no admin credential in the repo. Full design
      — schema, RLS, optional-auth, last-write-wins + tombstone sync, Room migration 8->9, and a phased
      rollout — is in [SUPABASE_PLAN.md](SUPABASE_PLAN.md). Parked post-release.
- [ ] **ACCT-2: Shared food database.** A moderated `shared_foods` table Btesters can grow, with
      server-side validation and rate-limiting so it can't be poisoned. SUPABASE_PLAN §2b/§3; Phase 4.
- [ ] **AI-1: Vision extras (opt-in, bring-your-own Gemini key).** Photo + a weight for a more accurate
      estimate; a menu photo -> cutting/bulking suggestion. Provider locked to Gemini (free keyless AI
      Studio key, structured JSON output). Slices 1-3 (chat, vision, log-this) already shipped; these
      are further use cases. The key stays in Keystore-encrypted prefs, never embedded.
- [ ] **AI-3: Real tool / function-calling.** Give the model actual lookups (the CNF asset + Open Food
      Facts) and the user's own context (goals, remaining macros), instead of prompt guidance. Gated on
      structured-output / tool-call support on the endpoint; larger than the current prompt approach.
- [~] **AI-4: Ingredient list -> recipe/meal.** IN PROGRESS. The model returns structured JSON; the app
      resolves each ingredient deterministically (saved food -> CNF -> Open Food Facts) and saves a new
      Recipe or MealTemplate. The deterministic core and the preview/save flow are built; **still needs
      on-device testing with a Gemini key**. Design in [AI4_PLAN.md](AI4_PLAN.md).

### Profile front-door

- [ ] **PROF-3: Onboarding account choice.** First onboarding screen offers "Create an account" vs
      "Continue without an account" (the current flow). Real sign-up is ACCT-1; until then it shows as
      coming-soon.

### Release prep

- [~] **REL-2: Branding.** The launcher icon (adaptive), the splash screen, and an in-app mark on the
      About screen are shipped. Remaining: placing a small mark in the per-screen headers (a taste call
      to make on device). An iOS icon set is N/A until an iOS target exists.

## Parked

- **Social feed** — a low-priority "probably not". Not built toward unless revived.

---

## Shipped

### Schema (DB v1 -> v8)

- [x] **SCHEMA-1: Favourites** (v3) — heart custom foods; a Favorites section in search.
- [x] **SCHEMA-2: Caffeine** (v4) — `caffeineMg` on entries/foods, through totals and the food log.
- [x] **SCHEMA-3: Barcode + emoji/icon** (v5) — nullable `barcode`/`emoji` on foods; an emoji picker.
- [x] **SCHEMA-4: Recipe model** (v6) — `Recipe` + `RecipeIngredient`, per-serving mapping, logging.
- [x] **SCHEMA-5: Meal-type column** (v7) — shipped, but the UI was reverted; the column is dormant.
- [x] **SCHEMA-6: Body fat** (v8) — nullable `bodyFatPct`; enables Katch-McArdle TDEE.

### Features / UI

- [x] **UI-1: JSON export / import** — every table to a user-picked file, restored by upsert.
- [x] **UI-2: Macro rings** on the food-log totals (a swipe view), all wrapped in one boxed row.
- [x] **UI-3: Trends screen** — metric/period selectors, a daily-average card, a Canvas bar chart.
- [x] **UI-4: Weight screen** — current + change, a log dialog with a date picker, a ranged trend graph.
- [x] **UI-5: Log reminder** — an opt-in daily 8 PM notification (AlarmManager), off by default.
- [x] **UI-6: Drag log items** between hour blocks (long-press + drag).
- [x] **UI-7: Ingredient picker** for Create Recipe / Create Meal (the food search in picker mode).
- [x] **UI-8: Nutrient detail screens** — per-nutrient trend + daily contributors; a dashboard card.
- [x] **UI-9: Barcode camera scanning** — CameraX + bundled ML Kit, offline; torch, gallery import, and
      a viewfinder; a scanned code checks saved foods first, then Open Food Facts.
- [x] **UI-11: Onboarding overhaul** — a stepped wizard with a TDEE reveal and goal adjustment.
- [x] **UI-12: Duplicate & edit** a food from the detail screen's overflow menu.
- [x] **UI-13: Food-log calendar** — a 12-month logged/missed calendar with streak stats.
- [x] **UI-14: Open Food Facts name search** — branded results alongside CNF + custom, debounced.
- [x] **UI-15: Favourite serving units** — pin up to 2 units to the front of every food's picker.
- [x] **UI-16: Coloured macro pills** on the food-log edit-sheet number pad.
- [x] **UI-17: Compact nutrient chips** on the nutrient-detail selector (one row, no wrap).
- [x] **UI-18: Outlined totals box** on the food log.
- [x] **UI-19: Scan button** in the food editor's barcode field (round-trips the scanned code back).
- [x] **SEARCH-2: CNF ranking + plurals** — starts-with ranking and simple singular/plural stemming.
- [x] **SCAN-1: Scan stabilisation** — require the same code across a few frames before accepting.
- [x] **PROF-2: Photo avatar** — pick a gallery photo (downscaled, app-internal) or an emoji.
- [x] **RESEARCH-1: PWA / iOS spike** — iOS via Compose Multiplatform, web via a thin PWA later
      ([PWA_IOS_SPIKE.md](PWA_IOS_SPIKE.md)). The domain/data/calc layers are kept Android-free.

### Release prep

- [x] **REL-1: README** — root README + MIT LICENSE (copyright Daniel Nguyen) + a screenshot gallery.
- [x] **REL-3: CI** — GitHub Actions runs unit tests + a debug build on every push/PR to `main`.
- [x] **REL-4: Docs index** — a navigable knowledge base over all project docs.

### v1 core

- [x] Onboarding + TDEE goal engine (Mifflin-St Jeor / Katch-McArdle, activity, goal, macro split).
- [x] Advanced goal types (recomp / maingain / lean bulk) behind a toggle.
- [x] Common-food search over the bundled CNF asset (read-only, opened outside Room).
- [x] Custom foods + a Kitchen browse of foods/meals/recipes; edit in place, swipe-to-delete.
- [x] Unified food search (All / Foods / Meals / Recipes / Quick add) + an in-memory cart.
- [x] Food detail with full portion units, a contribution-to-remaining card, and macro rings.
- [x] Food log: week strip + date navigation, hour blocks, macro pills, a micronutrient box, swipeable
      totals with a goal tick, swipe-to-delete.
- [x] Dashboard (Cals + Macros + nutrients cards, a logging heatmap) + a Profile with a changeable
      avatar.
- [x] Goals folded with Reassess (TDEE recalc or custom); a default-landing-screen setting.
- [x] Weight logging; a neutral-dark theme + a full blue light theme; a compact bottom-nav pill.
- [x] Security assessment ([SECURITY.md](SECURITY.md)).
