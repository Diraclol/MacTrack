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
device build so KSP regenerates `app/schemas/.../N.json` (currently at **v8**) — which must be
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

_(Recipe "preparation instructions" — the notes box in the Create Recipe reference — was considered
and dropped at Dirac's call: not needed. The redesigned Create Recipe screen intentionally omits it.)_

---

## Follow-ups not blocked by hardware (UI / no schema)

- [x] **UI-1: Export / Import data (JSON).** SHIPPED. `data/backup/BackupManager` serializes every
      table to JSON (org.json, no new dep) and restores by upsert; Export/Import buttons in More via the
      Storage Access Framework. Round-trips export → wipe → import.
- [ ] **UI-2: Macro rings on the food log (swipe view).** NOT the dashboard — Dirac confirmed the
      dashboard stays as-is. On the food log's swipeable totals row, add a **rings** view as another
      swipe state (same gesture as the existing totals / goal-tick swipe): one circular progress ring per
      macro (calories / protein / carbs / fat) filling toward the day's goal, MacroFactor-style, with a
      Consumed/Remaining sense. Reuses the day's existing numbers — just a new way to draw them.
- [x] **UI-3: Trends screen.** SHIPPED. TrendsScreen (metric + period selectors, daily-average card,
      Canvas daily bar chart with goal line); `DailyTotals` aggregation via `GROUP BY date`; the
      dashboard "Cals + Macros" card shows the rolling 7-day average and taps through to Trends.
- [x] **UI-4: Weight redesign.** SHIPPED. Dedicated Weight screen (nav card in More): current weight +
      change, Log-weight dialog, 1M/3M/1Y/All range chips, a Canvas trend graph, history with delete.
- [x] **UI-5: Log-reminder notifications.** SHIPPED (off by default). A "Log reminder" switch in More →
      Preferences schedules a daily 8 PM nudge via `AlarmManager` + a `ReminderReceiver` (no WorkManager
      dep needed). Requests `POST_NOTIFICATIONS` at toggle time on Android 13+. Fixed 8 PM time for now
      (a time picker is a follow-up). NOTE: the actual alarm/notification firing is device-behavior
      dependent — confirm on the Pixel.
- [ ] **UI-6: Drag-to-reorder log items between time blocks.** Hold + drag a logged row up/down to
      move it to another hour block. Gesture-heavy (device testing) AND moving between blocks means
      rewriting the row's `timeMinutes` — decide that data behavior before building.
- [x] **UI-7: Ingredient picker for Create Recipe / Create Meal.** SHIPPED and reworked to match the
      MacroFactor reference. Create Meal and Create Recipe are now pure creation forms; their "+" / Add
      buttons open the existing food-search screen in a **picker mode** (`UnifiedSearchScreen(picker=…)`)
      that searches the whole catalog (custom + Common/CNF). Picks land in a shared in-memory
      `IngredientBuilderRepository` that survives the navigation round-trip (same idea as the Cart), and
      CNF foods are imported into `food_items` by their `cnf_<code>` id so ingredients stay food_items-
      backed. Since Create Meal became a pure form, meal delete moved to the Kitchen (long-press).
      Follow-ups: storing CNF/branded ingredients "properly" (a source ref on the item rows, a schema
      decision) instead of the food_items import; a recipe **icon picker**.
- [x] **UI-8: Nutrient detail screens.** SHIPPED. Tapping a micronutrient card on the food log opens
      `ui/feature/nutrient/NutrientDetailScreen`: today's total vs a reference target, a 30-day Canvas
      bar chart with a target line, and today's contributors (foods summed by that nutrient). Covers the
      four box nutrients (sodium/potassium/fiber/caffeine).
- [ ] **UI-9: Barcode camera scanning.** Manual barcode lookup works; camera scanning needs new deps
      (CameraX + ML Kit barcode) + the CAMERA permission + a scanner screen.
- [~] **UI-10: Automated tests.** JVM unit tests added (JUnit4, no new deps): the calc engine
      (pre-existing), plus `IngredientBuilderRepository`, `Nutrients` arithmetic, and the FoodModels
      mappers (`asFoodItem` / `foodItemDetail` / `mealEntryDetail` / `recipeDetail` / `stagePortion`).
      Run with `./gradlew :app:testDebugUnitTest`. **Remaining:** instrumented Compose tests (day strip,
      swipe-to-delete, swipe-toggle totals) — need a working emulator/device.
- [x] **UI-11: Onboarding overhaul.** SHIPPED. Rebuilt into a stepped, MacroFactor-style wizard with a
      progress bar: Sex → Age → Height → Weight → Activity (the app's **5** levels) → **TDEE reveal** →
      Goal (Lose/Maintain/Gain, which shows the adjusted daily target) → Fat preference → Protein
      preference → summary → saves profile + goal. Follow-ups: fancier pickers (wheel/ruler like the
      reference) and an optional body-fat step (SCHEMA-6 column exists).
- [x] **RESEARCH-1: PWA / iOS feasibility spike.** DONE — see [PWA_IOS_SPIKE.md](PWA_IOS_SPIKE.md).
      Recommendation: iOS via **Compose Multiplatform** (share domain/data/most-UI, ship native iOS);
      web via a **thin PWA over the Supabase backend** later (not CMP-wasm); no Flutter/RN rewrite.
      The one discipline to hold now: keep `domain`/`data`/`calc` free of `android.*` imports (they are).

---

## Accounts, roles & AI (needs a backend + product decisions)

Gated on choosing a backend and on the security rules in `docs/SECURITY.md`. Roles must be enforced
server-side, never client-trusted. This whole track is scheduled **last**. **Backend decision: Supabase**
(Postgres + Auth + Row-Level Security + auto API + a Kotlin SDK) — it supplies the server-side role
enforcement the security model requires with minimal backend to build; Neon was evaluated and rejected
(DB-only, would force building auth+API+roles ourselves). Rationale in
[BACKEND_RESEARCH.md](BACKEND_RESEARCH.md).

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
      **STATUS — Slice 1 SHIPPED.** New "AI" nav tab with an OpenWebUI-style streaming chat
      (`ui/feature/ai/`), plus AI settings (base URL / model / key / Test connection). Built on an
      OpenAI-compatible client (`data/ai/AiClient`) over HttpURLConnection + SSE, so **base URL + key
      + model are config, not constants**: defaults to Gemini's OpenAI endpoint
      (`gemini-3.5-flash-lite`), and a homelab Ollama/Open WebUI URL drops in as a later toggle with no
      code change (a homelab consult confirmed Gemini is the right default — the local GTX-1050 box has
      no vision model and ~30-90 s latency). The key is encrypted with a hardware-backed Android
      Keystore AES-GCM key (no third-party dep); only ciphertext is stored. Conversation is in-memory.
      **Remaining:** Slice 2 = image attach (downscale ~1024 px → base64 data URL) for photo → macros;
      Slice 3 = a "Log this" action turning an estimate into a review-before-save food/log entry.
      Later: chat persistence (Room), a "local server" preset button.
      **Model designation (dev note, corrected by homelab 2026-09-01).** Main = `gemini-3.5-flash-lite`
      (app default; fastest/cheapest multimodal, verified ~15 RPM / 500 RPD). Backup =
      **`gemini-3.1-flash-lite`** — same tier, SEPARATE per-model daily bucket, so main+backup ≈ 1,000
      req/day of headroom. NOTE: the earlier pick `gemini-2.5-flash` is **retired for new accounts (404,
      verified 2026-08-29)** — do not use it. The Model field is an editable dropdown; the client retries
      429/503 with exponential backoff (1/2/4 s). We ride the **OpenAI-compat endpoint**
      (`/v1beta/openai/…`), which sits on the now-"legacy"-but-supported `generateContent` API — fine for
      now; if retired we'd move to the Interactions API.
      **Quota decision (LOCKED): separate Google Cloud project.** Free-tier daily quotas pool at the
      PROJECT level, so MacTrack gets its OWN project (AI Studio → Create API key → "in new project",
      name it `mactrack`) → fully separate, independently-revocable quota. Leave **billing disabled** on
      that project so the free tier is a hard circuit breaker (a retry-loop bug hits 429, not a card).
      Model separation was rejected (it's forgettable and would push Dirac's own study chats to a 20/day
      tier). BYO-key means this generalizes: every MacTrack user brings their own project + quota.

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
- [x] Neutral-dark theme (blue accent) + a full blue light theme; compact centered bottom-nav pill
- [x] Kitchen edit flow: tap a food/meal/recipe to edit in place, swipe-to-delete rows; clustered
      ingredient icons for meals/recipes; recipes loggable from the Add-food search (Recipes tab)
- [x] Security assessment (`docs/SECURITY.md`)
