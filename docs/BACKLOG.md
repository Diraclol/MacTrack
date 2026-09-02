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
      **Follow-up SHIPPED:** the TDEE engine now uses **Katch-McArdle** (lean-mass BMR) when a body fat %
      is set, else Mifflin-St Jeor — matching tdeecalculator.net (verified: their 119 lb / 16% sample
      gives BMR 1349). `domain/calc/Calculations.kt` (`katchMcArdleBmr`, `harrisBenedictBmr` for
      reference, `basalMetabolicRate` selector); reassess shows which formula it used; unit tests added.

_(Recipe "preparation instructions" — the notes box in the Create Recipe reference — was considered
and dropped at Dirac's call: not needed. The redesigned Create Recipe screen intentionally omits it.)_

---

## Follow-ups not blocked by hardware (UI / no schema)

- [x] **UI-1: Export / Import data (JSON).** SHIPPED. `data/backup/BackupManager` serializes every
      table to JSON (org.json, no new dep) and restores by upsert; Export/Import buttons in More via the
      Storage Access Framework. Round-trips export → wipe → import.
- [x] **UI-2: Macro rings on the food log (swipe view).** SHIPPED. The food-log totals row is now a
      3-way swipe cycle: "remaining" → "eaten / goal" → **rings** (a `TotalRing` per macro — a Canvas
      progress ring toward the day's goal, over-goal turning red, value in the center). Not the
      dashboard (stays as-is). Follow-up if wanted: a persisted default view and a Consumed/Remaining
      label on the rings. **Update:** the whole totals row (all three views) is now wrapped in one grey
      filled box, matching the nutrient cards.
- [x] **UI-3: Trends screen.** SHIPPED. TrendsScreen (metric + period selectors, daily-average card,
      Canvas daily bar chart with goal line); `DailyTotals` aggregation via `GROUP BY date`; the
      dashboard "Cals + Macros" card shows the rolling 7-day average and taps through to Trends.
- [x] **UI-4: Weight redesign.** SHIPPED. Dedicated Weight screen (nav card in More): current weight +
      change, Log-weight dialog, 1M/3M/1Y/All range chips, a Canvas trend graph, history with delete.
      **Update SHIPPED:** the graph now has a real weight axis (max/mid/min kg labels + gridlines, via
      `TextMeasurer`/`drawText`, adapting to the range) and start/end date labels; the Log-weight dialog
      has a **date picker** so past weigh-ins can be backfilled, and logging is now one-per-day (a
      backfilled date replaces via `WeightEntryDao.replaceForDate`).
- [x] **UI-5: Log-reminder notifications.** SHIPPED (off by default). A "Log reminder" switch in More →
      Preferences schedules a daily 8 PM nudge via `AlarmManager` + a `ReminderReceiver` (no WorkManager
      dep needed). Requests `POST_NOTIFICATIONS` at toggle time on Android 13+. Fixed 8 PM time for now
      (a time picker is a follow-up). NOTE: the actual alarm/notification firing is device-behavior
      dependent — confirm on the Pixel.
- [x] **UI-6: Drag log items between time blocks.** SHIPPED. Long-press and hold a logged food row,
      then drag up/down; on release it snaps into whichever hour block it was dropped in. Data decision
      (Dirac): no minute precision — the row's `timeMinutes` is set to the target hour × 60 (top of the
      hour), since the log only ever groups by hour. `MealLogViewModel.moveEntryToHour`; the drop is
      resolved against hour-header positions captured via `onGloballyPositioned`. Coexists with the
      horizontal swipe-to-delete on the same row. Follow-up if the feel is off: tune the hold-vs-swipe
      gesture split, and lift the dragged card above neighbours (cross-lazy-item zIndex).
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
      four box nutrients (sodium/potassium/fiber/caffeine). **Update SHIPPED:** the dashboard now also
      carries a **Nutrients** card (7-day average of the four, `DashboardViewModel.weeklyNutrientAvg`)
      below Cals+Macros, tapping through to the nutrient detail; both dashboard cards' links were
      relabeled from "7-day avg" to **See more**.
- [x] **UI-9: Barcode camera scanning.** SHIPPED. On-device, offline camera scanner
      (`ui/feature/scanner/BarcodeScannerScreen`) using CameraX 1.6.2 (`LifecycleCameraController`) +
      bundled ML Kit `barcode-scanning:17.3.0` (no API key, no Play Services) via `MlKitAnalyzer`
      (EAN-13/8, UPC-A/E). The barcode dialog now offers **Scan with camera**; the first hit is looked
      up in Open Food Facts through the existing `branded` path. New CAMERA permission + `uses-feature
      camera.any required=false`; APK grows ~2.4 MB for the bundled model. NOTE: 17.3.0 specifically is
      the version with 16 KB native-lib alignment that AGP 9 enforces — earlier versions fail the build.
      Deps added to the version catalog (camerax, cameraMlkit, mlkitBarcode, lifecycleRuntimeCompose).
      Confirmed working on the Pixel (scanned a real snack). **Follow-up SHIPPED:** the barcode icons on
      both the food log and food search now open the camera directly (manual-entry dialog removed); an
      unrecognized code shows a dialog to Scan again or Create food (prefilled with the barcode, via a
      `create_food?barcode=` arg); the scanner has a framed viewfinder + sweeping red line overlay.
      **Follow-up SHIPPED (polish):** a flashlight/torch toggle (bottom-right, `enableTorch`) and a
      gallery-import icon (bottom-left, PickVisualMedia -> `InputImage.fromFilePath` -> same ML Kit
      scanner) so a barcode can be read from an existing photo without the camera.
      **Still to do (offline):** a scan -> saved-food offline match (`food_items.barcode` before OFF).
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
- [x] **UI-12: Food-detail duplicate & edit.** SHIPPED (food part). A 3-dot overflow menu (top-right of
      the food detail screen, via a new optional `actions` slot on `BackBar`) offers **Duplicate & edit**
      for any non-recipe food (Common/CNF, branded/scanned, a logged entry incl. an AI estimate, or an
      existing custom food): `FoodDetailViewModel.duplicateAsFood` writes a NEW custom `FoodItem` from the
      shown food's default serving (grams when known; a scanned food carries its barcode onto the copy)
      and opens it in the food editor (`edit_food/{id}`) to tweak name/macros/icon. The user's own custom
      food also gets a plain **Edit**. Covers near-match nutrition labels and editing AI-logged items.
      **Follow-up (not started): recipe duplicate.** Viewing a recipe should offer "Duplicate recipe"
      that clones the `Recipe` + its `RecipeIngredient`s into a new recipe and opens the recipe editor
      (`edit_recipe/{id}`) -- the overflow menu is intentionally hidden for `source == "recipe"` until
      then. Also optional: carry the source food's chosen emoji onto the copy (today it name-derives).
- [x] **UI-13: Food-logging month calendar.** SHIPPED (pending device build). The dashboard Food Logging
      card is now tappable ("See more") -> `ui/feature/streak/FoodLogCalendarScreen` (+ VM): a scrollable
      12-month calendar where a filled blue day = at least one entry logged that day and a plain day =
      missed; Sunday-start grid, a streak/days-tracked/longest header, and a legend. Route `food_calendar`.
- [x] **UI-14: Online Open Food Facts NAME search.** SHIPPED. Text search now queries Open Food Facts by
      product name (`/cgi/search.pl`, `search_terms=`) alongside the offline CNF + custom results, so
      branded items (Cheerios etc.) surface when typed. Results appear in a new **Branded** section after
      Common, debounced ~400 ms (min 3 chars), offline-safe (no network -> section stays empty, CNF/custom
      unaffected), "Searching online..." while loading. `OffProduct` carries per-100g macros; tapping opens
      the existing `food_detail/branded/<code>` path (same as a scan), and quick-add resolves via
      `openFoodFactsRepository.lookup(code)`. Hidden in ingredient-picker mode. Implemented in
      `data/off/OpenFoodFactsRepository.kt` (`OffProduct` + `searchByName`/`fetchSearch`),
      `UnifiedSearchViewModel.kt` (branded/searchingBranded flows + debounced job), `UnifiedSearchScreen.kt`
      (Branded section). NOTE the CNF query is still exact-substring (`name LIKE %term%`), so a typo
      ("cherrios") returns nothing -> an optional fuzzy/typo pass is a separate follow-up.
- [x] **UI-15: Favorite Serving Units (full, incl. volume).** SHIPPED. A More screen "Favorite Serving
      Units" pins up to 2 units to the FRONT of every food's serving picker. Catalog: WEIGHT (g/oz/lb,
      exact conversions) + VOLUME (ml/tsp/tbsp/fl oz/cup) with a standard ~1 g/ml assumption -- volume
      gram figures are labelled as estimates on the screen. Pref stored as CSV keys, capped at 2 (a 3rd
      drops the oldest). `FoodDetail.withFavoriteUnits(keys)` (data/food/FoodModels.kt) moves an
      already-present favourite to the front, or synthesises a missing one from the food's per-gram basis
      (skipped when the food has no gram basis, e.g. a frozen log snapshot); the default selected unit is
      unchanged. Applied at both serving selectors -- FoodDetailScreen and the TodayScreen edit sheet.
      Files: FoodModels.kt (`GenericServingUnit`, `FAVORITE_UNIT_CATALOG`, `withFavoriteUnits`),
      ThemeRepository/ThemeViewModel (`favoriteUnits` CSV pref), FavoriteUnitsScreen.kt (new), MoreScreen +
      MacTrackApp wiring. NOTE: because `stagePortion` normalises any gram-bearing unit to grams, logging a
      volume favourite stores the gram-equivalent (same as oz today); the serving chip still shows e.g.
      "cup (240 g)". A follow-up could preserve the volume label in history.
- [ ] **UI-16: Number-pad coloured macro pills.** Render the macro summary (e.g. 18P / 0C / 4F / 109 cal)
      as coloured pills (like the food-log rows) on the logging surface. **PENDING:** confirm which surface
      -- the compact bottom-sheet log editor in Dirac's screenshot differs from the current full-screen
      FoodDetail (which uses macro rings); locate/point-to it before building.
- [ ] **UI-17: Nutrient-detail pill polish.** The chip rows on the nutrient detail screen (nutrient
      switcher + period pills, UI-8/period-pills) "look weird" (Dirac). **PENDING a screenshot** to fix
      precisely -- likely the two stacked FilterChip scroll-rows read cluttered; consider consolidating or
      spacing them.
- [ ] **UI-18: Cals + Macros outline on the food log (TENTATIVE).** Dirac "might" want an outline/border
      on the food-log totals box for visibility (like the nutrient boxes). One-line border change; do on
      confirm.
- [x] **RESEARCH-1: PWA / iOS feasibility spike.** DONE — see [PWA_IOS_SPIKE.md](PWA_IOS_SPIKE.md).
      Recommendation: iOS via **Compose Multiplatform** (share domain/data/most-UI, ship native iOS);
      web via a **thin PWA over the Supabase backend** later (not CMP-wasm); no Flutter/RN rewrite.
      The one discipline to hold now: keep `domain`/`data`/`calc` free of `android.*` imports (they are).
- [ ] **I18N-1: French localization (fr-CA).** Add a French language option in More. **Parked by Dirac
      2026-09-01** — captured with a plan, not started, because it is a large multi-step job that must be
      built in small increments on-device. **Timing (Dirac): do it as a feature branch once the full
      public release is out** — not before, so it doesn't churn the pre-release work. **Scope:** there is *no* i18n infrastructure today — no
      `strings.xml`, zero `stringResource`; every user-facing string is a hardcoded literal (~300-400
      strings: 191 `Text("…")` + 26 `contentDescription` + ~39 titles/labels/placeholders + dialogs/
      Toasts/BackBar titles, across 24 screens). **Mechanism (decided):** Compose-level switch, no new
      deps and no base-class change — `MainActivity` is a `ComponentActivity` (not AppCompat), so instead
      of `AppCompatDelegate.setApplicationLocales`, store the language in `ThemeRepository` (mirroring the
      theme / dashboard-graph prefs) and provide an overridden `LocalConfiguration`/`LocalContext` at the
      top of `MacTrackApp` so `stringResource` resolves French. **Phased plan:** (1) build the Language
      toggle (English/French) + the switch mechanism, and translate the bottom nav + More as a proof it
      builds and switches on device; (2) fan out screen-by-screen, one buildable commit per few screens,
      pulling literals into `res/values/strings.xml` + `res/values-fr/strings.xml`. **Out of scope:** food
      *data* names (CNF, Open Food Facts) are data, not UI — they stay in their source language (CNF does
      ship French names; wiring that is a separate future item).

---

## Accounts, roles & AI (needs a backend + product decisions)

Gated on choosing a backend and on the security rules in `docs/SECURITY.md`. Roles must be enforced
server-side, never client-trusted. This whole track is scheduled **last**. **Backend decision: Supabase**
(Postgres + Auth + Row-Level Security + auto API + a Kotlin SDK) — it supplies the server-side role
enforcement the security model requires with minimal backend to build; Neon was evaluated and rejected
(DB-only, would force building auth+API+roles ourselves), and **Convex** was evaluated and rejected too
(first-party Android SDK but no durable offline cache / no optimistic updates on Android — worse for a
local-first Room app, and a document model vs the relational shared food DB). Rationale in
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
      **Slice 2 SHIPPED:** attach a food photo (PickVisualMedia → downscale ~1024 px → base64 data URL,
      `ImageEncoder`/`DataUrlImage`), sent as an OpenAI vision content part; photo-only asks for a macro
      estimate. **Slice 3 SHIPPED:** a "Log this" button under assistant estimates → best-effort
      `MacroParser` → an editable review dialog → logs a `meal_entry` (sourceType "ai") to the current
      day. The AI feature is now end-to-end. **Later (optional):** chat persistence (Room), a "local
      server" preset button, structured-output extraction instead of regex parsing.
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
- [x] **AI-2: Background system prompt.** SHIPPED. The chat is given a richer `system` turn on every
      request (`AiViewModel.SYSTEM_PROMPT`) explaining what MacTrack is, how the user feeds it info
      (a plain question, a food photo, a photo + weight, or a pasted item list), that an estimate should
      read as name + calories + protein/carbs/fat for a stated serving (the app shows a review dialog
      before logging), and to keep estimates **consistent with** Open Food Facts (branded/barcoded) and
      the Canadian Nutrient File (common whole foods) plus typical Canadian serving sizes — deferring to
      the app's own barcode scan for exact label values. Kept honest: this chat has **no live access** to
      CNF/OFF and is told so, rather than pretending it can query them.
- [ ] **AI-3: Real tool / function-calling for the assistant.** Give the model actual access instead of
      just prompt guidance: function-calling (or a retrieval step) so it can look a food up in the CNF
      asset and Open Food Facts, and read the user's own context (profile, goals, today's remaining
      calories/macros) to answer "what should I eat to hit my protein." Gated on structured-output /
      tool-call support on the chosen endpoint; bigger than AI-2 (a string) — it's plumbing.
- [ ] **AI-4: Ingredient list -> macros -> save as recipe/meal.** CONFIRMED (Dirac). When the user gives
      the AI a list of ingredients, resolve each (**branded -> use branded macros; else look it up in the
      CNF database; else Open Food Facts**), do the reasonable per-ingredient calculation, sum, and let the
      user log/save the result. If they asked for it as a **recipe** or a **meal**, create a new saved
      `Recipe` or `MealTemplate` accordingly (not just a one-off log). Depends on real CNF/OFF access for
      the model (AI-3) and on wiring the AI's structured result into the existing Recipe/Meal create path.
      Big; overlaps UI-14 (OFF name lookup).

---

## Profile & accounts front-door (some local, some gated on the backend)

- [ ] **PROF-1: Profile name.** A name on the local profile/account. Schema-gated: `user_profile.name`
      (nullable) via a `Migration(8,9)` + DB v9 + a device build so KSP regenerates `9.json` (commit it).
      Shown on Profile (editable) and asked for in onboarding. Local — no backend needed.
- [x] **PROF-2: Photo profile icon.** SHIPPED. The avatar can be a photo from the gallery, not just an
      emoji. `data/profile/AvatarStore` copies the picked photo into app-internal storage (downscaled
      ~256 px JPEG, a fresh timestamped filename each pick so the path string changes and Compose
      re-decodes, old files cleaned up). `ThemeRepository` stores the path in prefs (`avatar_photo_path`)
      as `avatarPhotoPath: StateFlow<String?>`; picking an emoji clears it (mutually exclusive). A shared
      `ui/common/ProfileAvatar` composable renders the photo if set else the emoji, used in all three
      avatar spots (dashboard header, More header, Profile). On Profile, tapping the avatar opens a
      chooser: Choose a photo / Choose an emoji / Remove photo. Uses the same PickVisualMedia photo
      picker as the AI attach (no new permission), decodes with BitmapFactory (no Coil/Glide dep). No
      schema change.
- [ ] **PROF-3: Onboarding account choice.** First onboarding screen asks "Create an account" vs
      "Continue without an account". "Continue without" is the current flow. Real sign-up is ACCT-1
      (needs the backend) — until then show it as coming-soon. Leans on the Cloud-Free-then-self-host
      plan in [BACKEND_RESEARCH.md](BACKEND_RESEARCH.md).

## Release prep (pre-public — repo stays PRIVATE until Dirac says otherwise)

- [ ] **REL-1: README** in Dirac's format: name, use cases, dependencies/tools used, written in,
      formatted for, Android dependencies, Gemini-API-key dependency, screenshots (Dirac provides).
      Recommended additions: a `LICENSE`, a short privacy note (BYO-key, local-first, no tracking), a
      `SECURITY.md` pointer, and a one-paragraph architecture blurb. Build in a small worktree.
- [~] **REL-2: Logo** — IN PROGRESS. Final monogram (faceted M/T ligature, brand blue #0861AB) delivered
      by Dirac as `MacTrack Monogram Logo.zip`. **SHIPPED:** the app **launcher icon** (adaptive, white
      mark on brand-blue, authored as hand-written VectorDrawables — `drawable/ic_launcher_foreground.xml`
      + `ic_launcher_background.xml` + `@color/mactrack_blue`; the mark also serves the `<monochrome>`
      themed-icon layer) and a reusable in-app mark (`drawable/ic_mactrack_logo.xml`), used on the new
      **About** screen (More → About). **SECURITY NOTE:** every supplied file (SVGs *and* PNGs) carried an
      embedded embedded "metadata" block — AI-tool provenance metadata marking the file as
      third-party. To honour the no-pre-public cleanup rule, **nothing was copied in verbatim**:
      the icons are re-authored from the six polygon coordinates as clean VectorDrawables (zero metadata),
      and no raster from the zip is in the repo. **Splash SHIPPED:** a branded launch screen (blue +
      centered white monogram) via the androidx `core-splashscreen` dep (Dirac's call), `Theme.MacTrack.Starting`
      + `installSplashScreen()` in MainActivity — backported to all supported devices (minSdk 26). **Still
      to do:** the app **header** placement (a small mark in a top bar — a visual/taste call to make with
      Dirac on device; the app uses per-screen headers, not one global app bar, so where it goes is a
      design choice). **iOS icon set: N/A** until an iOS/Compose-Multiplatform
      target exists (RESEARCH-1); the metadata-stripped 1024 master can be regenerated then. The stale green
      density `mipmap-*dpi/*.webp` fallbacks are unused at minSdk 26 and were left in place (removable later).
- [ ] **REL-3: CircleCI** — CI to auto-run `:app:testDebugUnitTest` and build a release APK; wire at
      release time for a full release. (Dirac: "wire it when the product is done".)
- [ ] **REL-4: Knowledge base / wiki** — seed from the existing docs (ENGINEERING_SUMMARY, BACKLOG,
      SECURITY, BACKEND_RESEARCH, PWA_IOS_SPIKE) as the backbone.
- [ ] **REL-5: Pre-public "pre-public cleanup."** Right before the repo goes public, sweep out every trace
      that the app was built with AI/the tool assistance. **This is a LATER task (do it as the last step
      before flipping the repo public), not now.** Current inventory (verified 2026-09-01):
      - **Commit history: CLEAN.** All commits are authored `Diraclol <junguy256@gmail.com>`; no commit
        message mentions AI/an external tool/co-written/"produced with". Nothing to rewrite. (Re-verify
        at wipe time in case anything slipped in.)
      - **`NOTES.md` and `.local/`: not published.** `NOTES.md` is gitignored and `.local/` is
        untracked, so neither ships. BUT tracked files *reference* the `NOTES.md` filename — those
        references both dangle (the file won't be there) and reveal the tell. Fix each:
        `MoreStatsViewModel.kt:20` (comment "per NOTES.md"), `docs/BACKEND_RESEARCH.md` ("the NOTES.md …
        rule"), `docs/ENGINEERING_SUMMARY.md` (two `../NOTES.md` links). **Decision to make:** either
        drop the references, or rename the working-rules content to a neutral committed doc (e.g.
        `CONTRIBUTING.md` / `docs/CONVENTIONS.md`) and point links there.
      - **Docs tone pass.** Re-read `docs/*.md` so they read as solo authorship — no "the assistant",
        "pair-programmed", etc. (none found today; recheck). Also genericise any remaining literal
        "third-party"/provenance phrasing that names a tool.
      - **Asset metadata.** In-repo icons are clean hand-authored VectorDrawables (no metadata). Any
        RASTER uploaded later (Play Store 512 icon, iOS icon set from the 1024 master) must have its embedded
        content-credentials stripped first — those came with the "third-party" manifest embedded.
      - **Out-of-repo, keep out:** `~/.local/…` memory and the `Downloads/MacTrack Monogram Logo.zip`
        are outside the repo; just never commit them.
      Whole thing is a find-replace sweep + one doc-rename decision — low effort, high stakes; do it in a
      small worktree and diff before publishing. Note the app's own **Gemini AI feature** is a legitimate
      product feature and stays — the wipe targets *build-time AI-assistance evidence*, not the product.

## Parked

- **Social feed** — a low-priority "probably not". Do not build toward it unless revived.

---

## v1 done (shipped)

- [x] Onboarding + TDEE goal engine (Mifflin–St Jeor BMR, activity, goal adjustment, macro split)
- [x] Advanced goal types (recomp / maingain / lean bulk) behind a toggle
- [x] Common food search over the bundled CNF asset (`cnf.db`, read-only, outside Room)
- [x] Custom foods (Create Food) + Kitchen browse of foods/meals/recipes
- [x] Unified food search (All / Foods / Meals / Quick add) + in-memory cart + quick add
- [x] Barcode lookup via Open Food Facts (manual entry + on-device camera scan, CameraX + ML Kit)
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
