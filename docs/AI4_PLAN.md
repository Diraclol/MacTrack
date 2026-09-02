# AI-4 — Ingredient list -> macros -> save as Recipe or Meal

The goal: when someone gives the AI a list of ingredients, resolve each ingredient's macros
(branded -> use the branded macros; else the Canadian Nutrient File; else Open Food Facts), do the
reasonable calculations, and save the result as a **new Recipe or a new Meal** depending on what the
user asked for.

This doc records the chosen architecture and the staged build. The deterministic core is built and the
flow is wired into the AI tab; what remains is on-device testing with a real Gemini key.

## Architecture decision: AI parses -> app resolves + saves (NOT model function-calling)

The map of the AI layer (see below) showed the client is an OpenAI-compatible **text/streaming**
call (`AiClient.stream()` returns `Flow<String>`), with no tool/function-calling plumbing and no
channel to surface a tool call. Real function-calling would mean reassembling fragmented SSE
`tool_calls`, a multi-turn tool loop, and a richer message model — a lot of surface that can't be
tested here.

Instead:

1. **The model does only parsing.** Given the user's message, it returns strict JSON:
   `{ "target": "recipe" | "meal", "name": "...", "ingredients": [ { "name": "...",
   "quantity": <number>, "unit": "g|ml|serving|cup|..." } ] }`. It expresses quantities in grams
   where it reasonably can (it knows a large egg is ~50 g), because gram amounts resolve cleanly.
2. **The app does resolution + math + persistence, deterministically, against the real sources** —
   which keeps the numbers honest (the model never invents macros here).
   For each ingredient: resolve saved food -> CNF -> Open Food Facts, materialise a `food_items`
   row (recipe/meal ingredients must reference a saved FoodItem), compute the amount in *servings*
   of that food, then save via `RecipeRepository.saveRecipe(...)` or
   `MealTemplateRepository.saveTemplate(...)`.

This reuses the existing text client (no new SSE machinery) and keeps every hard-to-test decision in
plain, unit-testable Kotlin.

## Data-model facts this relies on (from the code map)

- `Recipe(id, name, makesServings, cookedWeightG?, emoji?, createdAt)` +
  `RecipeIngredient(id, recipeId, foodId, amount)`. `amount` = number of **servings** of the food.
- `MealTemplate(id, name, createdAt, mealType?=null)` +
  `MealTemplateItem(id, templateId, foodId, amount)`. Same `(foodId, amount)` shape.
- **`foodId` must reference a real `food_items` row** — nutrients are looked up live from the
  FoodItem, never stored inline. So resolving an ingredient means creating/upserting a FoodItem.
- CNF: `CnfRepository.search(name)` (ORDER BY length(name)); `CnfFood.asFoodItem()` -> `cnf_<code>`,
  100 g serving. OFF: `searchByName(name)` then `lookup(code)`; needs a hand-built FoodItem. Saved
  foods: matched in memory (no name-query DAO).
- Save paths: `RecipeRepository.saveRecipe(name, makesServings, cookedWeightG, emoji,
  ingredients: List<Pair<foodId, servings>>): String` and
  `MealTemplateRepository.saveTemplate(name, items: List<Pair<foodId, servings>>)`.

## Staged build (each stage is its own commit; CI compiles + runs unit tests)

- **[DONE] Stage 1 — domain model + serving math.** `ParsedIngredient`, `RecipeBuildRequest`,
  `ResolvedIngredient`, `BuildTarget` (`RecipeBuildModels.kt`) and a pure `servingsFor(...)`
  (`ServingMath.kt`) that converts a requested amount into servings of a food (mass/volume via the
  same factors as UI-15; honest `null` when units can't be reconciled). Unit-tested
  (`ServingMathTest`).
- **[DONE] Stage 2 — `IngredientResolver`.** `suspend resolve(ParsedIngredient) ->
  ResolvedIngredient`, priority saved -> CNF -> OFF, upserting a FoodItem for CNF/OFF hits
  (`cnf_<code>` / `off_<code>`, idempotent), computing `servings` via `servingsFor`, persisting only
  when the unit converts. Device-tested.
- **[DONE] Stage 3 — `RecipeRequestParser` + `RecipeMealBuilder`.** Parser lifts the JSON object out
  of the model reply and validates it; builder resolves every ingredient, sums macros, and saves a
  new Recipe or MealTemplate, skipping+reporting anything unresolved (saves nothing if none resolve).
  Device-tested.

- **[BUILT — needs on-device test] Stage 4 — wired into `AiViewModel`.** Chosen behaviour:
  **preview first, save on confirm**. Flow, as shipped:
  1. `SYSTEM_PROMPT` now tells the model to answer a "make a recipe/meal from these" request with ONLY
     the JSON object `{ "target": "recipe"|"meal", "name", "ingredients": [ {name, quantity, unit} ] }`
     (grams where reasonable; "serving" for countable items). Ambiguous -> "meal".
  2. `send()` accumulates the reply, hides raw JSON behind a "Putting that together..." placeholder
     while it streams, then `RecipeRequestParser.parse(reply)`. If it's a build request, it calls
     `RecipeMealBuilder.preview(request)` (resolves + totals, **does not save**) and shows a summary
     bubble ("Recipe 'X': 1200 cal, 90P/100C/40F, from N ingredients. Couldn't match: ...").
  3. `AiScreen` renders a **Save as recipe/meal** button under that bubble; tapping it calls
     `AiViewModel.commitBuild(...)` -> `RecipeMealBuilder.commit(preview)` (saves the Recipe/MealTemplate)
     and turns the bubble into "Saved ... Find it in your Kitchen."
  4. Non-build replies behave exactly as before (the "Log this" estimate button still works).
  **What to test on device (needs a Gemini key):** ask e.g. "make a recipe from 2 eggs, 100 g chicken,
  1 cup rice" -> confirm a clean preview (no raw JSON flash), sensible macros, unmatched items listed,
  and that Save actually creates the recipe/meal in the Kitchen. Tune `SYSTEM_PROMPT` if the model is
  inconsistent about emitting bare JSON. Defaults chosen for the open questions below: recipe-vs-meal
  from wording (else meal); `makesServings = 1.0`; unmatched ingredients skipped + reported.

## Design decisions (the earlier open questions, now resolved)

1. **Recipe vs meal default** when the user is ambiguous ("save these as ...")? Current plan: the
   model picks from the wording; default to **meal** if unclear (a meal is the lighter construct).
2. **`makesServings` for a generated recipe** — default to `1.0` (the whole thing is one batch), or
   ask the model to estimate servings? Current plan: `1.0`, user edits later in the recipe editor.
3. **Unresolvable ingredients** (a name nothing matches, or a piece unit like "2 eggs" against a
   gram-only food): current plan is to **skip and report** them, saving the rest. OK, or should it
   refuse and ask?
4. **Auto-save vs confirm**: RESOLVED — **preview first, save on confirm** (Stage 4). The build shows a
   preview bubble with totals and unmatched items; a Save button commits it. Safer than auto-save.
