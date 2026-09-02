# MacTrack — Engineering Summary

*Last updated: 2026-09-01*
*Engineering status plus the rules and decisions that shape the code — architecture and "why", not step-by-step how-to. Current status lives in [MACTRACK_STATE.md](MACTRACK_STATE.md); deferred work in [BACKLOG.md](BACKLOG.md); the threat model in [SECURITY.md](SECURITY.md).*

---

## AT A GLANCE

| | |
|---|---|
| **App** | Local-first Android calorie + macro tracker. Offline core, single user, no backend yet. |
| **Core (calc engine)** | Done, unit-tested. BMR (Katch-McArdle when body fat is set, else Mifflin–St Jeor) → TDEE → macro split; matches tdeecalculator.net. |
| **Food log + search + dashboard + Kitchen** | Built and running on device. Reviewed by screenshot. |
| **Data** | Room (`mactrack.db`, **schema v8**) + bundled read-only CNF asset (`cnf.db`). |
| **AI assistant** | SHIPPED — an opt-in chat tab (BYO-key, OpenAI-compatible, default Gemini): text + food-photo (vision) → macro estimate → review → log. Key encrypted in the Keystore. `data/ai/`, `ui/feature/ai/`. |
| **Accounts / roles / shared DB** | Not built (scheduled last). Backend decided: **Supabase** (Neon and Convex both evaluated and rejected; see BACKEND_RESEARCH.md). |
| **Barcode scanning** | Manual entry + **on-device camera scan** (CameraX 1.6.2 + bundled ML Kit `barcode-scanning:17.3.0`, offline, no key). `ui/feature/scanner/`. |
| **Next up** | UI-10 instrumented tests (needs the emulator, now available) or ACCT-1 (Supabase). Barcode (UI-9), drag-between-blocks (UI-6), and Katch-McArdle TDEE all shipped this round. |

**Build gate:** schema changes can't be finished from the editor — they need a device build so KSP regenerates `app/schemas/.../N.json`. Do them one migration at a time. (All of SCHEMA-1..6 shipped; DB is at v8.)

---

## THE STACK

| | |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose (Material3 1.4.0, Compose BOM 2026.02.01) |
| Persistence | Room (KSP), schema version **8**, `exportSchema = true` |
| DI | Manual — `MacTrackApplication` holds every repository as a `lazy val`. No Hilt/Koin. |
| Package root | `com.dirac.mactrack` |
| Bundled data | Canadian Nutrient File — `app/src/main/assets/cnf.db` (~2 MB, read-only) |
| Network | Open Food Facts (barcode lookup) + OpenAI-compatible AI (default Gemini). Both `HttpURLConnection` + `org.json`, no networking dependency. |
| Camera | CameraX 1.6.2 + bundled ML Kit `barcode-scanning:17.3.0` (on-device, offline, no API key) for the barcode scanner. 17.3.0 is the 16 KB-aligned build AGP 9 requires. |
| Secrets | AI API key encrypted with a hardware-backed Android **Keystore** AES-GCM key; only ciphertext in prefs (`data/ai/AiSettingsStore`). No secret is ever hardcoded or committed. |
| Test devices | Pixel 8a (real) + emulator |
| Source count | ~95 `.kt` files |

```bash
./gradlew :app:assembleDebug        # build
./gradlew :app:testDebugUnitTest    # unit tests — calc engine MUST stay green
./gradlew :app:installDebug         # install over existing app (never uninstall to fix a DB error)
adb logcat                          # device logs
```

---

## ARCHITECTURE — how it fits together

```
                         Android app process
                                 │
        MacTrackApplication  (manual DI: every repository is a lazy val; builds the Room DB)
                                 │
   ┌──────────── Jetpack Compose UI — single NavHost in ui/navigation/MacTrackApp.kt ───────────┐
   │   screens take nav lambdas only (onBack/onOpenX/onLogged); no NavController leaks           │
   │   collectAsState()                                                                          │
   │        ▲                                                                                    │
   │   ViewModels ──── StateFlow (stateIn, WhileSubscribed(5000)) ────────────────────────────┐  │
   └────────┼─────────────────────────────────────────────────────────────────────────────────┼──┘
            │ repository Flows                                                                  │
            ▼                                                                                   ▼
      Repositories ───────────────┬────────────────┬────────────────┬──────────── in-memory / prefs
            │                      │                │                │        (Cart, LogDateStore,
            ▼                      ▼                ▼                ▼         "settings" prefs)
   Room (mactrack.db)      CNF asset (cnf.db,   Open Food Facts   SharedPreferences
   entities + DAOs         read-only, opened    barcode lookup    (theme, start screen, avatar)
   (schema v8)             OUTSIDE Room)        (network)
```

### File / package layout

| Path | Responsibility |
|------|----------------|
| [MacTrackApplication.kt](../app/src/main/java/com/dirac/mactrack/MacTrackApplication.kt) | Manual DI container; builds the Room DB + registers migrations |
| [MainActivity.kt](../app/src/main/java/com/dirac/mactrack/MainActivity.kt) | Single activity; sets Compose content + theme |
| `ui/navigation/` | `MacTrackApp.kt` — the one `NavHost`, floating bottom nav, all route wiring |
| `ui/feature/` | One package per screen: `today`, `dashboard`, `foodsearch`, `food`, `library`, `meals`, `recipes`, `goals`, `profile`, `more`, `onboarding` — each a Composable + its ViewModel |
| `ui/common/` | Shared UI + helpers — `BackBar`, the stateless `NumberPad`, `EmptyHint`/`CreateMenuItem`, `NutrientTargets`, and the `oneDecimal` formatter |
| `ui/theme/` | `Color`/`Theme`/`Type`, the shared `MacroColors` palette, `StartScreen`, `ThemeViewModel` ← `ThemeRepository` (prefs) |
| `data/entity/` | Room `@Entity`: `FoodItem`, `Goal`, `MealEntry`, `MealTemplate`(+`MealTemplateItem`), `UserProfile`, `WeightEntry` |
| `data/dao/` | Room DAOs — all queries return `Flow` |
| `data/repository/` | Repositories wrapping DAOs; expose `Flow`s |
| `data/food/` | `FoodModels.kt` — `Nutrients`/`PortionUnit`/`FoodDetail` + mappers `cnfFoodDetail`/`foodItemDetail`/`mealEntryDetail`/`stagePortion`. New source = new mapper, not a new screen |
| `data/cnf/` | Canadian Nutrient File — opens `cnf.db` outside Room; parameterized `LIKE` search |
| `data/off/` | Open Food Facts barcode lookup (network; code filtered to digits) |
| `data/cart/` | In-memory `Cart` — never persisted |
| `data/session/` | `LogDateStore` — selected/log date as a shared `StateFlow` |
| `domain/calc/` | Pure calc engine (BMR/TDEE/macros) + `ActivityLevel`/`FatLevel`/`ProteinLevel`/`Sex`/`GoalType`. No Android deps, unit-tested |

---

## HARD RULES — never violate

These lose user data or break the build. No "just this once".

```
RULE: NEVER add fallbackToDestructiveMigration(). It silently wipes the food log.
      A schema mismatch means entity vs migration SQL disagree — fix the disagreement.
RULE: NEVER bump @Database(version=N) without a real Migration(N-1,N) in data/Migrations.kt,
      registered in MacTrackApplication, then a build so KSP writes app/schemas/.../N.json,
      and that JSON committed. Migrations are cumulative; never rewrite an old one.
RULE: A NOT NULL column added with a SQL DEFAULT needs a matching
      @ColumnInfo(defaultValue="...") on the entity field, or Room throws at startup.
RULE: NEVER regenerate, edit, or delete app/src/main/assets/cnf.db. It is a pre-built asset.
RULE: cnf.db is opened OUTSIDE Room — you CANNOT JOIN meal_entries to CNF tables.
      Never design a schema that assumes you can.
RULE: Exactly ONE composable("route") per route in MacTrackApp.kt. Duplicates crash routing.
RULE: New files are .kt. A .java file with Kotlin in it fails confusingly.
RULE: Do not upgrade Kotlin/AGP/Compose/Room/KSP/Gradle without asking. Version drift has cost days.
```

---

## DECISIONS — why things are the way they are

**Manual DI, not Hilt/Koin.** One `Application` with `lazy val` repositories is enough for a single-module app; no annotation processor, no graph to reason about. ViewModels use `viewModelFactory { initializer { ... } }` + `APPLICATION_KEY`, exposed as `companion object { val Factory }`.

**`meal_entries` rows are snapshots + provenance.** Nutrient columns are scaled and **frozen at log time**; editing a custom food must never change past logs. `sourceType`/`sourceId`/`unitLabel` say where the food came from so an entry can be reopened and re-logged. Both halves are load-bearing.

**A day is a query, not a record.** Totals come from `GROUP BY date`. No daily-totals table, ever — it would drift from the entries.

**CNF lives outside Room.** The bundled nutrient DB is read-only and huge relative to user data; opening it as a Room entity set would couple migrations to it. It's queried directly with parameterized `LIKE`; logged CNF foods are reopened via provenance, not a join.

**Dynamic color is OFF.** Material You pulled wallpaper colors and made the whole app read as one hue. The app ships its own neutral-dark scheme with a blue accent.

**Log date is shared session state.** `data/session/LogDateStore` holds the selected day as a `StateFlow`, so logging from search or the detail screen targets the *viewed* day without threading a date arg through every route.

**Screens never see a NavController.** They take `onBack`/`onOpenX`/`onLogged` lambdas; all wiring is in the single `NavHost`. Keeps screens testable and routes in one place.

---

## GOTCHAS — symptom → cause → rule

```
SYMPTOM: App crashes on launch after a schema change, logcat names a column.
CAUSE:   The migration SQL and the @Entity disagree (type, nullability, or DEFAULT).
FIX:     Make the ALTER TABLE match the entity exactly; NOT NULL DEFAULT needs
         @ColumnInfo(defaultValue=...). Rebuild so schemas/N.json regenerates.
RULE:    A schema crash is the debugging ENTRY POINT (it names the diff), never a
         reason to wipe data or add fallbackToDestructiveMigration().

SYMPTOM: A ViewModel Flow that depends on the selected date doesn't recompile / warns.
CAUSE:   flatMapLatest / mapLatest are experimental coroutine APIs.
RULE:    Annotate with @OptIn(ExperimentalCoroutinesApi::class).

SYMPTOM: A logged food's numbers look wrong after editing that custom food.
CAUSE:   Expecting historical rows to recompute from the live food.
RULE:    They never do. meal_entries are frozen snapshots; only a re-log changes them.
```

---

## KNOWN NON-PROBLEMS — do not chase

```
Caffeine shows 0 in the food-log nutrient box   → no column yet; SCHEMA-2 in BACKLOG.
CRLF/LF warnings on git add (Windows)            → cosmetic autocrlf; harmless.
"Loading…" flash on the food detail screen       → detail loads async; expected.
No instrumented UI tests                          → known gap; UI-10 in BACKLOG.
```

---

## COMPANION FILES

| File | What it is |
|------|------------|
| [BACKLOG.md](BACKLOG.md) | Deferred work, bucketed by what blocks it. The task queue. |
| [MACTRACK_STATE.md](MACTRACK_STATE.md) | Project status: what is built, what is in flight, and what is next. |
| [SECURITY.md](SECURITY.md) | Threat model + current posture; the plan for accounts/roles/AI. |
