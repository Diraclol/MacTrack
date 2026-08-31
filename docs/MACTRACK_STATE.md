# MacTrack — state and roadmap

Accurate as of 2026-08-31.

## Where we are

MVP is done and working on a real Pixel. Recent commits, newest first:

- `DB v2: meal_entries gains source provenance` — schema v2 with a real `Migration(1,2)`
- `Food detail: contribution card, pad open on entry`
- `Add inline quantity edit on the Food Log`

The next task (2b below) is specified in full and has not been started.

## What is built and working

Onboarding with static Mifflin-St Jeor producing a `Goal` and `UserProfile`. Light, dark,
and system theming via SharedPreferences. Floating bottom nav on the three tab routes.
Dashboard with calorie ring, macro bars, and a 30-day logging streak grid. Time-based food
log (date header, Cal/P/F/C "left" strip, hour pills with subtotals, food cards with a
calories number, bottom search bar). Tap a logged entry to rescale its amount in a
bottom-sheet number pad. Unified food search across custom foods and CNF, into a shared
food detail screen. Cart staging (`+` on a search row, "Log Foods" to commit). Saved meals,
recipes (one-way into a `FoodItem`), weight logging, Quick Add.

The food detail screen currently: opens with the number pad up over a dim scrim, first key
press replaces the prefilled amount, tapping the scrim drops the pad to a docked bar with
the amount box plus Log and Add, and shows calorie and macro rings, a "Contribution to
Remaining Daily Macros" card, and a micronutrient list.

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

## NEXT TASK — 2b: write provenance at every log site

Schema v2 exists but nothing populates the new columns yet. Five write sites plus the cart.
All edits are additive. Nothing should change visually.

**`data/cart/Cart.kt`** — the cart sits between the food and the log, so it has to carry
the fields. Add to `CartItem`, after `nutrients`:

```kotlin
    val sourceType: String = "unknown",
    val sourceId: String? = null,
    val unitLabel: String? = null
```

**`ui/feature/foodsearch/FoodDetailViewModel.kt`** — remember what the screen was opened
with. Add fields above `_detail`:

```kotlin
    private var loadedSourceType: String = "unknown"
    private var loadedSourceId: String? = null
```

Record them at the top of `load(source, id)`, before `viewModelScope.launch`. Then in
`addToCart`, pass `sourceType = loadedSourceType, sourceId = loadedSourceId,
unitLabel = unit.label` into `CartItem`. And in `log`, after `cholesterolMg = s.cholesterol`,
add `sourceType = loadedSourceType, sourceId = loadedSourceId, unitLabel = unit.label,
updatedAt = System.currentTimeMillis()`.

**`ui/feature/foodsearch/UnifiedSearchViewModel.kt`** — `addToCart(source, id)` already has
both values, so pass `sourceType = source, sourceId = id, unitLabel = unit.label` into
`CartItem`. In `logCart`, pass them through from the cart item:
`sourceType = ci.sourceType, sourceId = ci.sourceId, unitLabel = ci.unitLabel,
updatedAt = System.currentTimeMillis()`.

**`ui/feature/foodsearch/QuickAddViewModel.kt`** — `sourceType = "quick"`,
`unitLabel = "serving"`, `updatedAt = System.currentTimeMillis()`. No `sourceId`; a quick
add has no food behind it and null is the correct answer.

**`ui/feature/meals/MealsViewModel.kt`** — in `logTemplate`, each row is a `FoodItem`, so
`sourceType = "custom"`, `sourceId = food.id`, `unitLabel = "serving"`,
`updatedAt = System.currentTimeMillis()`. ("Logged as part of meal X" would be a separate
`groupId` column later, not this field.)

**`ui/feature/today/MealLogViewModel.kt`** — in `updateEntryQuantity`'s `entry.copy(...)`,
add `updatedAt = System.currentTimeMillis()`. `sourceType` and `sourceId` carry over
through `copy` automatically.

Verify with App Inspection's Database Inspector: a newly logged CNF food should show
`sourceType` `cnf`, a numeric `sourceId`, and a `unitLabel` like `30 g`. Cart-logged and
Quick Add rows should show `cnf`/`custom` and `quick` respectively. Pre-v2 rows stay
`unknown`.

## Then, in order

1. **Upgrade `mealEntryDetail`.** When `sourceId` is present, reload the real food (CNF or
   custom) and preselect the logged `unitLabel`, giving the full portion list and exact
   per-unit values. Keep the current snapshot-derived single unit as the fallback for
   `unknown` rows.

2. **Log-entry edit sheet: `Details` and `Done`.** Replace the sheet pad's single `Save`
   with two actions. `Done` saves the amount as it does today. `Details` opens the food
   detail screen for that entry, which needs:
   - `MealEntryDao`: `@Query("SELECT * FROM meal_entries WHERE id = :id") suspend fun getById(id: String): MealEntry?` (a query only, no schema change)
   - `MealEntryRepository.getEntry(id)`
   - `FoodDetailViewModel`: an `"entry"` branch in `load` that fetches the entry and maps
     it with `mealEntryDetail`, plus `updateEntry(amount, unit, onDone)` which rewrites the
     same row id (`e.copy(...)` through `logEntry`, since insert is `OnConflictStrategy.REPLACE`)
   - `FoodDetailScreen`: an entry mode (`source == "entry"`) showing a single `Done` action
     instead of Log and Add, and titling the card "Contribution to Daily Goal" measured
     against the full goal rather than the remainder — MacroFactor does exactly this,
     because the entry is already counted in today's totals
   - `TodayScreen`: an `onOpenEntry: (String) -> Unit` parameter
   - `MacTrackApp`: no new route needed — reuse `food_detail/{source}/{id}` with
     `navController.navigate("food_detail/entry/$entryId")`. The bottom bar already hides there.

3. **A Room migration test.** There is a migration chain and no test for it. Use
   `MigrationTestHelper` (instrumented) to create a v1 database with a row, run
   `MIGRATION_1_2`, and assert the row survives with `sourceType = 'unknown'`. This is the
   safety net for every future schema change.

4. **Search overhaul.** Category tabs (All / Recipes / Meals / Foods). With an empty query,
   show Recent (derived from `meal_entries`, now possible because of provenance) and Saved
   (all foods, meals and recipes, alphabetical). Move the search field to a docked bar above
   the keyboard that rises with it. Meals and recipes become searchable result types.

5. **Food log header.** Swipe to toggle each total between "left" and "X / goal". Make the
   four header nutrients interchangeable with any micro (pairs with dashboard customization).

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

## Known issues worth fixing when nearby

- **Goals are read as "latest", not "as of that date."** `GoalRepository.getLatestGoal()`
  is used everywhere, so once date navigation exists, historical days will be measured
  against today's goal. The fix is a query-only change:
  `SELECT * FROM goals WHERE createdAt <= :endOfDay ORDER BY createdAt DESC LIMIT 1`.
- **The edit sheet's pad appends to the prefilled amount** instead of replacing on the
  first key press, unlike the food detail screen, which does replace. They should match.
- **`FoodDetailScreen` and `DashboardScreen` each define their own ring composable** and
  their own macro colour constants. Extract when doing the cleanup pass.

## Gotchas that have actually cost time

- New files must be `.kt`. A `.java` file containing Kotlin fails silently and confusingly.
- Duplicate `composable("route")` entries in `MacTrackApp.kt` have happened.
- Warnings (yellow) do not block builds; only errors (red) do.
- "Uninstall the app to fix a DB crash" was a one-time move during an early flatten. It is
  not a debugging step any more — a schema crash names the offending column in logcat, and
  uninstalling destroys the evidence and the data.
