# MacTrack — Engineering Summary

A local-first Android calorie and macro tracker. Kotlin, Jetpack Compose, Room. Offline, single
user, no backend, no account (yet). Ships the Canadian Nutrient File as a bundled read-only SQLite
asset (the "Common" food source). Visual and interaction target is MacroFactor. Package root
`com.dirac.mactrack`.

Status: v1 in active development, running on a Pixel and an emulator; UI reviewed by screenshot,
the calc engine covered by unit tests. Room database at **schema version 2**.

> This is the architectural context — read it before touching the data model or navigation. The
> running work log lives in [docs/MACTRACK_STATE.md](docs/MACTRACK_STATE.md); the deferred work in
> [BACKLOG.md](BACKLOG.md); the threat model in [docs/SECURITY.md](docs/SECURITY.md); the day-to-day
> rules and hard constraints in [NOTES.md](NOTES.md).

---

## What the app does, and where its data comes from

MacTrack logs food against a daily calorie + macro goal. Four food sources feed one detail screen:

- **Common** — the bundled Canadian Nutrient File (`cnf.db`), a ~2 MB read-only SQLite asset opened
  **outside Room**. Searched with parameterized `LIKE`.
- **Custom foods** — user-created `FoodItem` rows in the Room database.
- **Meals** — repeatable sets of foods (`MealTemplate` + items).
- **Barcode** — Open Food Facts lookup over the network (manual entry today; camera scanning is
  backlogged).

Goals come from an onboarding flow that runs a TDEE calculation (Mifflin–St Jeor BMR → activity
multiplier → goal adjustment → macro split). The same engine backs "Reassess goals".

---

## Architecture

```
                         Android app process
                                 │
        MacTrackApplication  (manual DI: every repository is a lazy val; builds the Room DB)
                                 │
   ┌──────────── Jetpack Compose UI — single NavHost in ui/navigation/MacTrackApp.kt ───────────┐
   │   screens take nav lambdas only (onBack/onOpenX/onLogged); no NavController leaks           │
   │                                                                                             │
   │   collectAsState()                                                                          │
   │        ▲                                                                                    │
   │   ViewModels ──── StateFlow (stateIn, WhileSubscribed(5000)) ────────────────────────────┐  │
   └────────┼─────────────────────────────────────────────────────────────────────────────────┼──┘
            │ repository Flows                                                                  │
            ▼                                                                                   ▼
      Repositories ───────────────┬────────────────┬────────────────┬──────────── in-memory / prefs
            │                      │                │                │        (Cart, LogDateStore,
            ▼                      ▼                ▼                ▼         "settings" prefs)
   Room (app database)     CNF asset (cnf.db,   Open Food Facts   SharedPreferences
   entities + DAOs         read-only, opened    barcode lookup    (theme, start screen, avatar)
   (schema v2)             OUTSIDE Room)        (network)
```

### Package / file layout

| Path | Responsibility |
|------|----------------|
| [MacTrackApplication.kt](app/src/main/java/com/dirac/mactrack/MacTrackApplication.kt) | Manual DI container — repositories as `lazy val`s; builds the Room database + registers migrations |
| [MainActivity.kt](app/src/main/java/com/dirac/mactrack/MainActivity.kt) | Single activity; sets the Compose content and theme |
| `ui/navigation/` | `MacTrackApp.kt` — the one `NavHost`, the floating bottom nav, all route wiring |
| `ui/feature/` | One package per screen: `today` (food log), `dashboard`, `foodsearch`, `food` (create), `library` (kitchen), `meals`, `recipes`, `goals`, `profile`, `more`, `onboarding` — each a Composable + its ViewModel |
| `ui/common/` | Shared UI — `BackBar`, `NumberPad` (stateless shared pad) |
| `ui/theme/` | `Color`/`Theme`/`Type`, `StartScreen`, and `ThemeViewModel` backed by `ThemeRepository` (SharedPreferences) |
| `data/entity/` | Room `@Entity` classes: `FoodItem`, `Goal`, `MealEntry`, `MealTemplate`, `UserProfile`, `WeightEntry` |
| `data/dao/` | Room DAOs — all queries return `Flow` |
| `data/repository/` | Repositories wrapping DAOs; expose `Flow`s upward |
| `data/food/` | `FoodModels.kt` — `Nutrients`/`PortionUnit`/`FoodDetail` + the source mappers `cnfFoodDetail`, `foodItemDetail`, `mealEntryDetail`, `stagePortion`. A new food source is a new mapper, not a new detail screen |
| `data/cnf/` | Canadian Nutrient File access — opens the bundled `cnf.db` asset outside Room; parameterized `LIKE` search |
| `data/off/` | Open Food Facts barcode lookup (network; filters the code to digits) |
| `data/cart/` | In-memory `Cart` — never persisted |
| `data/session/` | `LogDateStore` — the selected/log date as a shared `StateFlow` |
| `domain/calc/` | Pure calc engine: `Calculations` (Mifflin–St Jeor BMR, TDEE, macro targets) + `ActivityLevel`/`FatLevel`/`ProteinLevel`/`Sex`/`GoalType`. Unit-tested, no Android deps |

### Conventions (imitate the existing code)

- **Manual DI** only — no Hilt/Koin. ViewModels are built with `viewModelFactory { initializer { ... } }`
  using `APPLICATION_KEY`, exposed as a `companion object { val Factory }`.
- **Data flow** — repositories expose Room `Flow`s; ViewModels expose `StateFlow` via
  `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initial)`; screens `collectAsState()`.
- **Navigation** — exactly one `composable("route")` per route in `MacTrackApp.kt`; screens never take
  a `NavController`. Pushed screens show `ui/common/BackBar`; the bottom nav shows only on the three
  tab routes.

---

## Capabilities (v1)

- Onboarding → TDEE goal engine (+ advanced goal types: recomp / maingain / lean bulk behind a toggle).
- Food search across Common (CNF) / saved Foods / Meals / Quick add, with an in-memory cart and
  barcode lookup (Open Food Facts).
- Food detail with full portion-unit selection, a contribution-to-remaining card, and macro share rings.
- Food log: day navigation (Sunday-start week strip + date arrows) logging to the viewed day, hour
  blocks with macro pills, a micronutrient box, one-line swipeable totals with a goal tick, and
  swipe-to-reveal delete.
- Kitchen (browse saved foods/meals/recipes) + Create Food.
- Dashboard (Cals + Macros card, 30-day logging heatmap), Profile (changeable avatar), Reassess goals.
- Neutral-dark theme with a blue accent; default-landing-screen setting; weight logging.

---

## Data model, and why

`meal_entries` rows are **snapshots plus provenance**, and both halves matter:

- Nutrient columns are already scaled and **frozen at log time**. Never recompute a historical row
  from its food; editing a custom food must not change past logs.
- `sourceType` / `sourceId` / `unitLabel` record where the food came from so an entry can be reopened
  and re-logged. Every new row must set them. `sourceId` holds the exact id
  `FoodDetailViewModel.load(source, id)` accepts.
- `cnf.db` is opened outside Room, so you **cannot** join `meal_entries` to CNF tables. Any
  "re-open this logged food" path goes through the provenance fields, not a SQL join.
- A day is a **query**, not a record — aggregate with `GROUP BY date`; never add a daily-totals table.

### Migrations (hard rules — violating these loses the food log)

- Never add `fallbackToDestructiveMigration()`. A schema mismatch means the entity and the migration
  SQL disagree — fix the disagreement.
- Never bump `@Database(version = N)` without a real `Migration(N-1, N)` in `data/Migrations.kt`,
  registered in `MacTrackApplication`; build so KSP regenerates `app/schemas/.../N.json`, and commit
  that JSON. Migrations are cumulative and never rewritten.
- A `NOT NULL` column added with a SQL `DEFAULT` needs a matching `@ColumnInfo(defaultValue = "...")`
  or Room's startup validation throws.
- Never regenerate or edit `app/src/main/assets/cnf.db` — it is a pre-built asset.

---

## Testing

The calc engine is the tested core: `domain/calc/CalculationsTest.kt` must stay green (BMR/TDEE/macro
math). UI is verified on device by screenshot review — there are no instrumented Compose tests yet
(tracked in [BACKLOG.md](BACKLOG.md)).

```bash
./gradlew :app:testDebugUnitTest    # calc engine — keep green
```

---

## Real-world findings worth knowing

1. **`cnf.db` lives outside Room.** No `JOIN` from `meal_entries` to CNF is possible; the schema must
   never assume one. Logged CNF foods are reopened via `sourceType="cnf"` + `sourceId`.
2. **Logged rows are immutable snapshots.** The nutrient columns are scaled at log time; re-scaling an
   entry rewrites the same row id and stamps `updatedAt`, but the numbers never trace back to a live food.
3. **Room validates the schema at startup.** A migration/entity disagreement crashes on launch and
   names the offending column in logcat — that is the debugging entry point, not a reason to wipe data.
4. **Material You was the "everything is one color" bug.** `dynamicColor` pulled wallpaper colors; it
   is off so the app renders its own neutral-dark scheme with a blue accent.
5. **`flatMapLatest` / `mapLatest` need `@OptIn(ExperimentalCoroutinesApi::class)`** — used where a
   ViewModel switches its Flow on the selected date.
6. **The selected log date is shared session state** (`data/session/LogDateStore`), not a nav
   argument — so logging from search or the detail screen targets the *viewed* day without threading
   a date through every route.

---

## Operations

```bash
./gradlew :app:assembleDebug        # build
./gradlew :app:testDebugUnitTest    # unit tests (calc engine must stay green)
./gradlew :app:installDebug         # install over the existing app (never uninstall to fix a DB error)
adb logcat                          # device logs; the Pixel is the real test device
```

Build after every step and commit at each working checkpoint. Schema changes require a device build
so KSP regenerates `app/schemas/.../N.json` — do them one migration at a time.

---

## Outstanding work

See [BACKLOG.md](BACKLOG.md). It is bucketed by *what blocks each item* — schema-migration-gated
work (needs a build to regenerate the schema JSON), UI follow-ups that are safe from a dev machine,
and the accounts/roles/AI track that needs a backend and product decisions.
