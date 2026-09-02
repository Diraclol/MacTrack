# MacroFactor reference

MacTrack's visual and interaction target is MacroFactor. This file records what MacroFactor
does, screen by screen, so the UI-mimicking tasks in `MACTRACK_STATE.md` have a concrete
spec to build against. Descriptions are from MacroFactor app screenshots and public docs;
this is reference for imitation, not code to copy. Paraphrased throughout to avoid copying
MacroFactor's own text.

Sources: https://macrofactor.com/ , help center
https://help.macrofactorapp.com/en/collections/18-macrofactor-nutrition ,
roadmap https://feedback.macrofactorapp.com/roadmap .

## Screens observed

### Journal / food log (their main "today" screen)
- Header: `< Today >` date stepper, plus a horizontal week strip (S M T W T F S) where each
  day is a small calorie ring showing that day's progress; the selected day is outlined.
- A program summary card (named, e.g. "Rev") with four rings — Protein, Carbs, Fat, kCal —
  each shown as "X of GOAL" (e.g. `47 of 106`). The card is swipeable (page dots) between
  views.
- A row of micronutrient tiles (Sodium, Potassium, Dietary Fiber, Caffeine) each with a
  floor/target/ceiling slider (a bar with a target tick), not just a number.
- Food grouped into meal sections (M1, M2, M3, Supplements). Each section has a P/C/F/Cal
  pill summary, a collapse chevron, a `...` menu, and a `+`. NOTE: MacTrack is deliberately
  time-based (hour pills), NOT meal-slot based — imitate the look, keep our time model.
- Each food row: emoji/icon, name, "amount unit > P C F kcal" with color-coded macros.
- Bottom: a docked Food Search bar with a barcode icon and an AI-camera icon.
- Bottom nav: Dashboard / Journal / Goals / More, plus a floating `+`. (MacTrack has 3 tabs.)

### Food search screen
- Header: back arrow, a meal target label (e.g. `M1`), a calorie progress bar `0 / 2500`,
  a staged-count chip, and a commit/log check button top-right.
- Category tabs: All / Recipes / Meals / Foods.
- Empty query shows Recent (with a "Most Recent" sort control) and Saved sections. Each row
  has a `+` to stage at the food's default serving.
- Docked search bar at the bottom that rises with the keyboard, with barcode and Quick-Add
  (`+`) affordances beside it.

### Food edit / add number pad (bottom sheet)
- Opens over the log. Shows the food name, a large editable amount with a unit, unit/macro
  chips (fork = unit, flame = calories, P, C, F) so you can enter by amount OR by a macro/
  calorie value (the other fields back-calculate).
- Number pad with `.` `0` and backspace.
- Bottom row actions: **Details** (opens the food detail screen for that entry) and **Done**
  (saves the amount). This is the exact model for `MACTRACK_STATE.md` "Then, in order" task 2.

### Philosophy / numbers, in short
- Nothing turns red when you exceed a target; over/under is shown as neutral information.
- Targets are derived from the user's own weight + intake data; no shame UI.
- Expenditure is solved from weight trend + intake over ~14-30 days, then calorie/macro
  targets follow. This is the "adaptive expenditure engine" in state doc task 7. (Deferred.)

## Activity correction factors (for onboarding / expenditure)

MacTrack onboarding currently uses static Mifflin-St Jeor. For reference:

Standard activity multipliers:
| Category | Description | Factor |
|---|---|---|
| Sedentary | Inactive job + very rare/minimal exercise | 1.2 |
| Lightly Active | Light exercise 1-3 days/week | 1.375 |
| Moderately Active | Moderate exercise 3-5 days/week | 1.55 |
| Very Active | Hard exercise 6-7 days/week | 1.725 |
| Extremely Active | Hard daily exercise + physically demanding job | 1.9 |

MacroFactor splits general activity from exercise (its stated improvement over the above):
| General activity | Factor | Exercise | Add |
|---|---|---|---|
| Low | 1.2 | 0 sessions/week | +0 |
| Moderate | 1.4 | 1-3 sessions/week | +0.1 |
| High | 1.6 | 4-6 sessions/week | +0.2 |
| | | 7+ sessions/week | +0.3 |

Total multiplier = general factor + exercise add (e.g. sedentary job + lifting 5x/week =
1.2 + 0.2 = 1.4). Consider adopting this split if/when onboarding is revisited.

## Requested refinements to the search overhaul (state doc task 4)

To fold in when the search overhaul (task 4) is built:
- Top-right: a control to commit staged foods to the food log (their check button).
- Beside it: calories added-so-far out of the day's remaining/total budget (their `X / 2500`).
- Quick Add reachable from this screen.
- The full tab set: All / Foods / Recipes / Meals.

## What MacroFactor lists as out of scope (so we don't chase it)
From their roadmap "Won't Do": wearable/Fitbit/Garmin/Oura integrations (they route through
Health Connect / Apple Health instead), activity calories added to the target, vacation mode,
reverse-diet goal, family plan, food cost tracking. Useful to know what the target app
deliberately omits.
