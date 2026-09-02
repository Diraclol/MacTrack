# MacTrack — project status

Accurate as of 2026-09-02. The short "where things stand" snapshot: what is built, what is in flight,
and what is next. The full task list is in [BACKLOG.md](BACKLOG.md); the architecture and the reasoning
behind the design are in [ENGINEERING_SUMMARY.md](ENGINEERING_SUMMARY.md).

## Where it stands

The app is built and running on a real Pixel — a complete offline-first tracker:

- Onboarding + TDEE goal engine, goals with a reassess flow.
- Food logging: day view with hour blocks, macro pills and rings, a micronutrient box, a week strip
  with date navigation, drag-to-reorder between blocks, swipe-to-delete.
- Three-source food search: the bundled Canadian Nutrient File (offline), custom foods, and Open Food
  Facts (branded, by name or barcode) with an offline fallback.
- Custom foods, recipes, and meals with an ingredient picker.
- On-device barcode scanning (CameraX + ML Kit, offline; torch, gallery import).
- Trends and weight tracking (custom Canvas charts), a food-log calendar with a streak.
- JSON backup + import, light/dark themes, and a profile with a photo or emoji avatar.

The Room database is at **schema v8** (SCHEMA-1..6 shipped: favourites, caffeine, barcode/emoji,
recipes, a dormant meal-type column, body fat). The TDEE engine uses Katch-McArdle when a body fat %
is set and Mifflin-St Jeor otherwise, and is unit-tested against a reference calculator.

The optional AI assistant is built: an opt-in chat tab (bring-your-own Gemini key, OpenAI-compatible
streaming client, key encrypted in the Android Keystore) that estimates macros from text or a food
photo and logs the result after review.

## In flight

- **AI-4 (ingredient list -> recipe/meal).** The deterministic core and the preview/save flow are
  built; it still needs on-device testing with a real Gemini key. See
  [AI4_PLAN.md](AI4_PLAN.md).
- **UI-10 (automated tests).** JVM unit tests cover the calc engine, nutrient arithmetic, and the food
  mappers. Instrumented Compose tests are the remaining gap (need the emulator/device).

## What is next

A matter of sequencing (see the backlog): instrumented UI tests, and — scheduled after the public
release — accounts, roles, a shared food database, and cloud sync on Supabase
([SUPABASE_PLAN.md](SUPABASE_PLAN.md)). iOS (Compose Multiplatform) and French localisation are later
feature branches.

## Data model (the one load-bearing decision)

`meal_entries` rows are frozen nutrient snapshots plus provenance (source type / id / unit). The
snapshot keeps history immutable — editing or deleting a food never rewrites past logs — while the
provenance lets an entry be reopened, rescaled, and re-logged. A day is a query (`GROUP BY date`),
never a stored total. The full rationale is in [ENGINEERING_SUMMARY.md](ENGINEERING_SUMMARY.md).

## Known issues worth fixing when nearby

- **Goals are read as "latest", not "as of that date."** `GoalRepository.getLatestGoal()` is used
  everywhere, so historical days are measured against today's goal. The fix is query-only:
  `SELECT * FROM goals WHERE createdAt <= :endOfDay ORDER BY createdAt DESC LIMIT 1`.
- **The edit-sheet number pad appends to the prefilled amount** on the first key press instead of
  replacing it (the food-detail pad replaces). They should match.
- **The ring composable is still duplicated** between the dashboard and the food-detail screen. The
  macro colour palette has already been extracted to `ui/theme/MacroColors`; fold the ring in next.

## Gotchas that have cost time

- New files must be `.kt`; a `.java` file containing Kotlin fails confusingly.
- Exactly one `composable("route")` per route in `MacTrackApp.kt` — duplicates have crept in before.
- Warnings (yellow) never block a build; only errors (red) do.
- A schema-mismatch crash names the offending column in logcat — that is the debugging entry point,
  never a reason to uninstall the app or add a destructive migration (either destroys the data).
