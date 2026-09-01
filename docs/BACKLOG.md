# MacTrack — Backlog

Deferred work, organized by **what blocks it**, not by priority or effort. Three buckets:
**Schema-migration-gated** (needs a Room migration + a build so KSP regenerates the schema JSON),
**Follow-ups not blocked** (UI/feature work safe from a dev machine, no schema), and **Accounts,
roles & AI** (needs a backend and product decisions). A checked **v1 done** list sits at the bottom
so scope is visible at a glance. Item codes (SCHEMA-n, UI-n, ACCT-n, AI-n) cross-reference commits.

The running narrative log is [MACTRACK_STATE.md](MACTRACK_STATE.md); the architecture is
[ENGINEERING_SUMMARY.md](ENGINEERING_SUMMARY.md); the threat model is
[SECURITY.md](SECURITY.md).

---

## Schema-migration-gated

Each item needs a `Migration(N-1, N)` in `data/Migrations.kt`, a version bump on the database, and a
device build so KSP regenerates `app/schemas/.../N.json` (currently at **v2**) — which must be
committed. Do these **one migration at a time**, building after each, per the migration rules in
ENGINEERING_SUMMARY.md. Never `fallbackToDestructiveMigration()`.

- [x] **SCHEMA-1: Favorites / heart foods.** SHIPPED (DB v3). `FoodItem.favorite` boolean +
      `MIGRATION_2_3`; heart toggle on saved-food rows and a **Favorites** section in the unified
      search list after Recent (`ui/feature/foodsearch/UnifiedSearchScreen.kt`, `AllTab`). Note:
      only custom foods are favoritable (the column is on `food_items`); favoriting a CNF/branded
      food would need a separate provenance-keyed table.
- [x] **SCHEMA-2: Caffeine tracking.** SHIPPED (DB v4). `caffeineMg` on `MealEntry` and `FoodItem`
      via `MIGRATION_3_4`, flowing through `Nutrients` (defaulted, so CNF/OFF sources need no change),
      the food-log Caffeine total, a Create Food "Caffeine (mg)" input, and the food-detail Caffeine
      row. CNF/branded foods carry no caffeine data, so custom foods are the source.
- [x] **SCHEMA-3: Custom-food barcode + emoji/icon.** SHIPPED (DB v5). Nullable `FoodItem.emoji`
      and `FoodItem.barcode` via `MIGRATION_4_5`. A shared scrollable `EmojiPickerDialog`
      (`ui/common`) powers a tap-to-choose icon in Create Food (plus a barcode field) and a
      tap-to-change icon on Kitchen rows; `foodIcon(emoji, name)` renders the chosen icon (falling
      back to the name-derived one) in the Kitchen and unified search. Barcode camera scanning is
      still UI-9.
- [x] **SCHEMA-4: Recipe model.** SHIPPED (DB v6 + full wiring). `Recipe` (name, makesServings,
      nullable cookedWeightG, emoji) + `RecipeIngredient` entities, `RecipeDao`, `RecipeRepository`,
      `MIGRATION_5_6`. `RecipesViewModel`/`RecipesScreen` now persist real recipes (with a
      cooked-weight field), off the old "collapse into a FoodItem" hack. `recipeDetail` maps a recipe
      to a per-serving `FoodDetail` (total ÷ makesServings; a "g" unit appears when a cooked weight is
      set), so recipes flow through the existing FoodDetail → cart → log path — logging writes ONE
      `meal_entry` with `sourceType="recipe"`. Kitchen "Recipes" tab lists them with per-serving
      macros (tap to open/log, long-press to delete). Follow-ups: a recipe-ingredient *picker* (today
      you set servings against the full food list) and a recipe icon picker live under UI-7.
- [x] **SCHEMA-5: Meal type on templates — migration shipped, feature REVERTED.** The migration
      shipped (DB v7, `MIGRATION_6_7` added `MealTemplate.mealType`), but the meal-type UI was
      **removed** at Dirac's call: a meal is just a *labeled batch of foods* (log supplements etc. as
      one meal in a tap), so the meal name is the label — no breakfast/lunch/dinner/snack type. The
      `mealType` column is left **DORMANT** (always null) so the entity still matches the shipped
      schema; dropping it would need a separate migration. Don't reuse it without a plan.
- [x] **SCHEMA-6: Bodyfat on the profile.** SHIPPED (DB v8). Nullable `UserProfile.bodyFatPct` via
      `MIGRATION_7_8`; a tap-to-edit "Body fat" box on Profile (0–100, blank clears). Preserved across
      `saveProfile()` — onboarding/reassess don't wipe it — via a merge in the repository plus a
      dedicated `setBodyFat()`. **This closes the schema-migration-gated backlog** (all of SCHEMA-1..6).

---

## Follow-ups not blocked by hardware (UI / no schema)

- [ ] **UI-1: Export / Import data (JSON).** No way to back up or move data. Add a More-screen action
      that serializes entities to JSON and re-imports with schema/bounds validation (see the
      import/export notes in `docs/SECURITY.md`). Done = round-trip export → wipe → import restores the log.
- [ ] **UI-2: Dashboard rings redesign.** The dashboard macro card is bars today. Rebuild as
      MacroFirst-style weekly nutrition rings with a Consumed/Remaining toggle and small insight graphs.
- [ ] **UI-3: Trends screen.** Tapping the "Cals + Macros" card should open a Trends screen with a
      period selector (1W / 1M / 3M / 6M / 1Y / All) and a daily line/bar; the card itself should show
      the WEEKLY AVERAGE of cals/P/C/F. Aggregate with `GROUP BY date` — no totals table.
- [ ] **UI-4: Weight redesign.** Drop steps and "weigh-in"; collapse weight logging to a button; keep
      only bodyweight + date; add a trend graph with 1M / 3M / 1Y / All ranges (`WeightEntry` already exists).
- [ ] **UI-5: Log-reminder notifications.** Opt-in reminder to log food, **off by default**, toggle in
      More. Needs `POST_NOTIFICATIONS` (Android 13+) + a WorkManager scheduler. A pref, no schema.
- [ ] **UI-6: Drag-to-reorder log items between time blocks.** Hold + drag a logged row up/down to
      move it to another hour block. Gesture-heavy (device testing) AND moving between blocks means
      rewriting the row's `timeMinutes` — decide that data behavior before building.
- [ ] **UI-7: Rebuild Create Recipe / Create Meal with a proper ingredient picker.** Today Create
      Recipe just lists your saved foods with a servings box each — so with few/no saved foods there's
      **nowhere to add ingredients**, and there's no search/add flow. Rebuild both around an "add
      ingredient" search (over CNF + saved foods), a chosen-ingredient list, and per-screen fields:
      **Recipe** = named, each ingredient has a **servings** amount, plus recipe-level **cooked weight**
      and an icon; logs as ONE per-serving entry (SCHEMA-4 model). **Meal** = the same food set but
      **no per-item servings/label and no weight** — it's a batch that logs each of its foods to the
      food log at once (the current MealTemplate "add to cart → log" flow). Also a recipe **icon
      picker** (reuse `EmojiPickerDialog` + `FOOD_EMOJIS`). Pairs with SCHEMA-5.
- [ ] **UI-8: Nutrient detail screens.** Per-nutrient trend with Floor / Target / Ceiling and an
      all-contributors list (MacroFactor-style), reachable from the micronutrient box.
- [ ] **UI-9: Barcode camera scanning.** Manual barcode lookup works; camera scanning needs new deps
      (CameraX + ML Kit barcode) + the CAMERA permission + a scanner screen.
- [ ] **UI-10: Instrumented Compose tests.** No UI tests exist; the day strip, swipe-to-delete, and
      swipe-toggle totals are only verified by screenshot. Add a first `androidTest` pass for the food log.
- [ ] **UI-11: Onboarding overhaul.** The onboarding "Set up your profile" flow is a plain single
      scroll of fields (Sex/Age/Weight/Height/Activity/Goal). Redesign into a stepped, friendlier flow
      matching the app's polish; consider an optional body-fat step (the column exists now, SCHEMA-6).
- [ ] **RESEARCH-1: PWA / iOS feasibility spike.** MacTrack is Android-native today (Kotlin + Jetpack
      Compose + Room, with a bundled SQLite `cnf.db` asset). Investigate reaching the web (as a PWA)
      and iOS. Sketch the options and their cost: a Compose Multiplatform / KMP share of the domain +
      data layer (Room has KMP support; the CNF asset and cnf.db access would need a cross-platform
      story), vs. a separate web/PWA client over an eventual sync backend (ties into ACCT-1), vs. a
      fresh cross-platform rewrite. Done = a short written recommendation (what's realistic, what it
      costs, what it forces on the current architecture) in `docs/` — not an implementation.

---

## Accounts, roles & AI (needs a backend + product decisions)

Gated on choosing a backend (Firebase Auth/Firestore vs a Postgres-backed service like Supabase) and
on the security rules in `docs/SECURITY.md`. Roles must be enforced server-side, never client-trusted.
This whole track is scheduled **last**.

- [ ] **ACCT-1: Auth + accounts.** Google sign-in and email/password. Three roles — **admin** (newest
      features), **Btester** (can add foods to a shared database), **regular** (no extras). Enforce
      roles with server-side claims / row-level security. Never hardcode the admin credential anywhere.
- [ ] **ACCT-2: Shared food database.** The database Btesters grow. Needs validation, moderation, and
      rate-limiting so shared entries can't poison everyone's search.
- [ ] **AI-1: Vision features (separate, opt-in, BYO-key). Gemini only.** Photo → estimate macros;
      photo + a weight → more accurate; paste an item list (brands optional) → match/calculate against
      CNF + saved foods; menu photo → cutting/bulking recommendation. All go through review-before-save.
      **Provider decision (locked): Gemini only — no OpenAI, no NVIDIA NIM, no backup provider.** For a
      bring-your-own-key app the deciding factor is friction, and Gemini's AI Studio key is free, needs
      no card, has usable free RPM/RPD, native structured (JSON schema) output, and a range of models
      to pick from (Flash for speed/cost, Pro for hard cases). Let the user choose the Gemini **model**
      in-app; default to a Flash-class model. Key lives in EncryptedSharedPreferences / Keystore, never
      embedded in the app or committed. Still keep the call site behind a thin interface for
      testability, but only one implementation is planned.

---

## Parked

- **Social feed** — a low-priority "probably not". Do not build toward it unless revived.

---

## v1 done (shipped)

- [x] Onboarding + TDEE goal engine (Mifflin–St Jeor BMR, activity, goal adjustment, macro split)
- [x] Advanced goal types (recomp / maingain / lean bulk) behind a toggle
- [x] Common food search over the bundled CNF asset (`cnf.db`, read-only, outside Room)
- [x] Custom foods (Create Food) + Kitchen browse of foods/meals/recipes
- [x] Unified food search (All / Foods / Meals / Quick add) + in-memory cart + quick add
- [x] Barcode lookup via Open Food Facts (manual entry)
- [x] Food detail with full portion units, contribution-to-remaining card, macro share rings
- [x] Food log: Sunday-start week strip + date arrows logging to the viewed day; hour blocks with
      macro pills; micronutrient box; one-line swipeable totals with a goal tick; swipe-to-reveal delete
- [x] Dashboard (Cals + Macros card, 30-day logging heatmap) + Profile (changeable avatar)
- [x] Goals folded with Reassess (TDEE recalc or custom); default-landing-screen setting
- [x] Weight logging (`WeightEntry`)
- [x] Neutral-dark theme (blue accent), centered bottom nav
- [x] Security assessment (`docs/SECURITY.md`)
