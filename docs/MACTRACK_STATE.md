# MacTrack — state and roadmap

Accurate as of 2026-08-31.

## Where we are

MVP is done and working on a real Pixel. Recent work, newest first — all built and verified
on device:

- `Food detail: Log and Done return to the food log` (Add still returns to search)
- `Reopen a logged entry in the food detail screen` — queue tasks 1 and 2, done
- `Write source provenance at every log site (2b)` — 2b, done
- `Cart: carry source provenance on CartItem`

Schema is v2 (real `Migration(1,2)`), provenance is written at every log site, and a logged
entry can be reopened in the full detail screen. Current focus is the MacroFactor UI/UX pass
Dirac is speccing (next section), then the remaining queue.

## Current UI/UX focus (MacroFactor polish)

A visual pass to look and feel more like MacroFactor; `MACROFACTOR_REFERENCE.md` has the
target screens. Requested by Dirac, grouped. One buildable step at a time; he reviews each by
screenshot. We are NOT copying MacroFactor's whole feature set — only what's listed.

**Search overhaul (this is queue task 4, expanded).**
- Category tabs: All / Foods / Recipes / Meals.
- Empty query shows, in order: Recent (recently logged, from `meal_entries`), then Saved
  (all foods, meals, recipes), then Common (CNF).
- Results appear in rounded boxes when typing.
- A Quick Add entry point on this screen.
- Top-right control to commit staged foods to the log, with calories-added-of-budget beside
  it and a staged-items chip (MacroFactor's `X / 2500` + the two mini icons).
- Tapping the top-right commit opens a **"Review Foods"** screen: the staged foods with their
  combined **impact on the daily goal** (per-macro remaining bars + a nutrition-detail ring),
  and "Add More Foods" / "Log N Foods" actions. This is our cart upgraded into the
  review-before-save flow.
- Search field docked above the keyboard.

**Food log.**
- Day navigation: arrows on each end of the header to switch days. Surfaces the "goal as of
  date" known issue below — fix `getLatestGoal` when this lands.
- Each hour block shows its total macros (P/C/F plus cal), not just calories; brighter and
  more readable.
- Calorie numbers on the right want a "cal" label to read right.

**Food detail.** Shift the content down slightly.

**Navigation / More screen.** Target (per MacroFactor's More): a profile header on top -- name,
"member since", avatar, and streak stats (active / longest / total-tracked days) -- then card
rows with icons. Local-first, so no Friends/Subscription rows.
- (done) One combined "Saved Foods, Meals & Recipes" screen (`ui/feature/library/LibraryScreen`,
  tabs Foods/Recipes/Meals) replaces the three separate entries; the standalone Search-foods
  entry is retired (search lives on the food log).
- Still to do: move the profile block to the top with streak stats and restyle rows as cards;
  move Quick Add out of More onto the search screen (part of the search overhaul).

**Barcode scanning (queue task 6, wanted sooner).** ML Kit; needs the physical device and a
new dependency — confirm before adding it. Later, Open Food Facts as a "Branded" source
mapped into `FoodDetail` with the barcode as `sourceId`.

**Goals.** A "reassess" action on the Goals screen re-runs onboarding, but only for activity
level, goal type, protein level, and fat level; everything else (sex, age, height, weight)
stays as-is. It recomputes the `Goal` (Mifflin-St Jeor) and writes a NEW `Goal` row so goal
history is preserved for the "goal as of date" fix. To enable this, split onboarding into
separate per-step pages (activity, goal, protein, fat, ...) as reusable composables, so the
reassess flow can present just the four relevant steps instead of the whole thing.

**Global.** More color across the app (specifics pending from Dirac).

## What is built and working

Onboarding with static Mifflin-St Jeor producing a `Goal` and `UserProfile`. Light, dark,
and system theming via SharedPreferences. Floating bottom nav on the three tab routes.
Dashboard with calorie ring, macro bars, and a 30-day logging streak grid. Time-based food
log (date header, Cal/P/F/C "left" strip, hour pills with subtotals, food cards). Tap a
logged entry to rescale it in a bottom-sheet number pad, or open Details to reopen it in the
full detail screen. Unified food search across custom foods and CNF, into a shared food
detail screen. Cart staging (`+` on a search row, "Log Foods" to commit). Saved meals,
recipes (one-way into a `FoodItem`), weight logging, Quick Add.

The food detail screen opens with the number pad up over a dim scrim; the first key press
replaces the prefilled amount; tapping the scrim drops the pad to a docked bar. It shows
calorie and macro rings, a contribution card, and a micronutrient list. For a food it offers
Log and Add; reopened as an entry (`source == "entry"`) it offers a single Done, reloads the
real food's full portion list from `sourceId` (snapshot fallback for quick/unknown rows), and
measures the card against the full daily goal.

Nutrients tracked: 4 macros plus 6 micros (fiber g, sugar g, saturated fat g, sodium mg,
potassium mg, cholesterol mg). CNF has fiber, sodium and potassium but never caffeine.

## Data model, and why

`meal_entries` stores a frozen nutrient snapshot plus provenance. This was a deliberate
decision; do not "normalize" it later without re-reading this.

Why snapshots rather than references to the food:

- `cnf.db` is opened outside Room as a separate SQLite file, so `meal_entries` cannot be
  joined to `cnf_food` at all. The largest food source cannot be a foreign key.
- History must not mutate. Fixing a typo in a custom food would otherwise silently rewrite
  last month's logs, and the planned adaptive expenditure engine reads historical intake.
- Deleting a custom food must leave its logged days intact.

Why provenance was added on top (schema v2):

- A snapshot alone cannot rebuild the food's portion list, so the detail screen could only
  offer one unit chip when reopening an entry.
- "Recent foods" in the search overhaul needs to re-log a previous food at a *new* amount,
  which needs the food's identity, not just its frozen numbers.

Columns added in v2: `sourceType` (`cnf` | `custom` | `quick` | `recipe` | `branded` |
`unknown`), `sourceId` (nullable), `unitLabel` (nullable), `updatedAt`. Rows logged before
v2 carry `sourceType = 'unknown'` with nulls, permanently — every read path needs a
snapshot-derived fallback for them.

## 2b (done): provenance at every log site

Every write path sets `sourceType`/`sourceId`/`unitLabel`/`updatedAt`: food detail
`log`/`addToCart`, unified search `addToCart`/`logCart`, quick add (`quick`), meal templates
(`custom`), and the rescale path (`updatedAt`); `CartItem` carries the fields through the
cart. Pre-v2 rows stay `sourceType = 'unknown'`. Verified in the Database Inspector.

## Queue

1. **(done) Reopen entries / `mealEntryDetail`.** Reopening a logged food reloads the real
   food from `sourceId` for the full portion list, preselecting the logged unit; snapshot
   fallback for quick/unknown rows or a deleted food. `mealEntryDetail` lives in
   `FoodModels.kt`.

2. **(done) Log-entry edit sheet: `Details` and `Done`.** Details opens the entry in the
   detail screen (entry mode, single Done, card measured against the full goal); Done
   rescales in place. Reuses `food_detail/entry/{id}`.

3. **A Room migration test.** There is a migration chain and no test for it. Use
   `MigrationTestHelper` (instrumented) to create a v1 database with a row, run
   `MIGRATION_1_2`, and assert the row survives with `sourceType = 'unknown'`. This is the
   safety net for every future schema change.

4. **Search overhaul.** See "Current UI/UX focus" above for the current, expanded spec
   (tabs All/Foods/Recipes/Meals; Recent/Saved/Common; Quick Add; top-right log + cals;
   rounded boxes; docked search field). Recent is now derivable from `meal_entries` thanks
   to provenance. Meals and recipes become searchable result types.

5. **Food log header.** Swipe to toggle each total between "left" and "X / goal". Make the
   four header nutrients interchangeable with any micro (pairs with dashboard customization).
   Pairs with the day-navigation and hour-block work in the current focus.

6. **Barcode scanning.** First true post-MVP feature. ML Kit, needs the physical device.
   Later, Open Food Facts as a "Branded" source mapped into `FoodDetail` with the barcode
   as `sourceId`.

7. **Later, larger.** Dashboard customization (show/hide/reorder widgets, weight rejoins).
   Nutrition overview screen (Today/1W/1M/3M/1Y averages, full micros, floor/target/ceiling)
   — use SQL aggregate queries, not in-memory sums over every entry. Log-by-calories
   back-calculation. Custom font wired into `ui/theme/Type.kt`. Accounts and sync (lean
   Supabase; `updatedAt` already exists for this, tombstones will be needed for deletes).
   Adaptive expenditure engine. A cleanup pass to extract shared composables (the ring
   composable is duplicated between the dashboard and the food detail screen). KMP.
   JSON backup and export.

## Captured ideas (from MacroFactor screenshots, not yet scheduled)

- **Global "+" FAB** (bottom-right) opening a sheet: food search, Quick Track/Quick Add,
  barcode, and Create Food / Create Meal / Create Recipe (no alcoholic-bev), plus Log weight /
  Log water.
- **Dashboard redesign**: a weekly macro bar chart with a day selector; stat tiles showing
  "X of goal" plus a period average; Weigh-in / Food Log (heat-map dots) / Weight / Steps
  widgets; a docked food-search bar.
- **Food icons**: an emoji per food (bundled emoji, keyword-matched on the food name; store an
  optional `iconEmoji` on `FoodItem`, fall back to a category default). No network, no assets.
- **Export logs as JSON** (and maybe `.csv`/`.xlsx`) -- part of import/export. JSON first:
  serialize `meal_entries` (+ goals/profile) with kotlinx.serialization, write via the Storage
  Access Framework so the user picks the destination. Import is the harder half (do later).
- **Nutrition Report screen** (per-macro progress + carb detail: fiber, sugars, added sugars,
  sugar alcohols, starch -- note we only track a subset without a `cnf.db` rebuild) and a meal
  "..." menu (Copy Day, Reorder Meals, Day Notes, Log Water, Export Meal History, Delete
  Today's Foods).
- **Google Fit / Health Connect** steps integration (tentative yes; Health Connect is the
  modern path on Android).
- **AI parsing (admin-only, much later)**: BYO Gemini API key -- the maintainer runs it; other
  users supply their own key. Flow: write out an ingredient list; each item matches a stated
  brand, or the CNF "Common" DB if no brand is given; the matches mass-add to the cart for the
  user to edit/remove; then log as a meal or save as a recipe. The DB is the source of truth;
  AI never auto-saves. Also possible: Firebase auth for accounts. All of this needs the
  INTERNET permission (added now for barcode).
- A **README** with app screenshots, and a **wiki / knowledge base** -- both later.

## Longer-term vision (from the foundation roadmap, `Desktop/MacTrack.txt`)

Beyond the queue, the original plan calls for, roughly in order: a review-before-save flow
(the cart is a first step); AI text parsing ("2 eggs, protein shake, 20 grapes" -> match
against the local DB -> review -> confirm; the DB is the source of truth, AI never
auto-saves); AI photo estimation (vision -> match -> review, never auto-save); import/export
logs (`.csv`/`.xlsx`); accounts + sync; more micronutrients (vitamins, minerals, aminos,
soluble/insoluble fiber, additives -- requires rebuilding `cnf.db`, a separate project);
workout integration; and a heat-map calendar (the dashboard streak grid is a start). CI is
also wanted (there is a `.github/` dir). None of these are scheduled yet -- the near-term
focus is the UI/UX pass above.

## Work session notes (2026-08-31)

Working the queue one buildable + verified + committed step at a time; Dirac builds/tests on
return. Verification is done with review.

- **Gemini key**: Dirac has a Gemini API key (currently powering his homelab) that can be
  shared for the app's AI features and exposes many models. Confirms the AI plan (admin runs
  it / BYO key). Still much later.

Decisions to confirm when back (I did NOT guess these):
- **Search "Recipes" vs "Foods" tabs**: recipes currently become plain `FoodItem`s with no
  flag, so search can't tell a recipe-derived food from a normal one. Splitting those tabs
  needs a marker on `FoodItem` (a real `Migration`) or a different model. Parked.
- **"Recent" re-log**: `quick`/`unknown` recent entries have no `sourceId`, so they can't
  reopen their source food -- they'd need a snapshot re-log path. Design TBD.

## Known issues worth fixing when nearby

- **Goals are read as "latest", not "as of that date."** `GoalRepository.getLatestGoal()`
  is used everywhere, so once date navigation exists (see current focus), historical days
  will be measured against today's goal. The fix is a query-only change:
  `SELECT * FROM goals WHERE createdAt <= :endOfDay ORDER BY createdAt DESC LIMIT 1`.
- **The edit sheet's pad appends to the prefilled amount** instead of replacing on the
  first key press, unlike the food detail screen, which does replace. They should match.
- **`FoodDetailScreen` and `DashboardScreen` each define their own ring composable** and
  their own macro colour constants. Extract when doing the cleanup pass (pairs with the
  "more color" global request — a shared macro palette).

## Gotchas that have actually cost time

- New files must be `.kt`. A `.java` file containing Kotlin fails silently and confusingly.
- Duplicate `composable("route")` entries in `MacTrackApp.kt` have happened.
- Warnings (yellow) do not block builds; only errors (red) do.
- "Uninstall the app to fix a DB crash" was a one-time move during an early flatten. It is
  not a debugging step any more — a schema crash names the offending column in logcat, and
  uninstalling destroys the evidence and the data.
