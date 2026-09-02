# AI-4 — Ingredient list -> macros -> save as Recipe or Meal

Dirac's ask: when someone gives the AI a list of ingredients, resolve each ingredient's macros
(branded -> use the branded macros; else the Canadian Nutrient File; else Open Food Facts), do the
reasonable calculations, and save the result as a **new Recipe or a new Meal** depending on what the
user asked for.

This doc records the chosen architecture and the staged build, so it can be reviewed/redirected. It
was drafted during the focused session; nothing here is wired into a screen yet.

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
   which is what Dirac asked for and keeps the numbers honest (the model never invents macros here).
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

- **Stage 1 (this commit): domain model + serving math.** `ParsedIngredient`, `RecipeBuildRequest`,
  `ResolvedIngredient`, `BuildTarget`, and a pure `servingsFor(quantity, unit, servingSize,
  servingUnit)` that converts a requested amount into servings of a food (mass/volume via the same
  factors as the Favorite Serving Units feature; honest `null` when units can't be reconciled).
  Unit-tested. Inert (nothing calls it yet).
- **Stage 2: `IngredientResolver`.** `suspend resolve(ParsedIngredient) -> ResolvedIngredient`,
  priority saved -> CNF -> OFF, upserting a FoodItem for CNF/OFF hits (idempotent ids), computing
  `servings` via `servingsFor`.
- **Stage 3: `RecipeMealBuilder` + the AI JSON contract/parser.** Turn a `RecipeBuildRequest` +
  resolved ingredients into a saved Recipe or MealTemplate; a robust parser to pull the JSON out of
  a model reply (like `MacroParser`, but for this structure). Unit-tested.
- **Stage 4 (NEEDS DIRAC / live-API testing): wire into `AiViewModel`.** Detect the "make a
  recipe/meal from these" intent, send the JSON-extraction prompt, parse, run resolver + builder,
  and post a result bubble ("Saved recipe 'X' — 1200 cal, 90 P / 100 C / 40 F; 2 ingredients
  couldn't be matched: ..."). This is the only stage whose behaviour can't be verified without the
  live model + a device, so it is left for Dirac to finish and test.

## Open questions for Dirac (please weigh in)

1. **Recipe vs meal default** when the user is ambiguous ("save these as ...")? Current plan: the
   model picks from the wording; default to **meal** if unclear (a meal is the lighter construct).
2. **`makesServings` for a generated recipe** — default to `1.0` (the whole thing is one batch), or
   ask the model to estimate servings? Current plan: `1.0`, user edits later in the recipe editor.
3. **Unresolvable ingredients** (a name nothing matches, or a piece unit like "2 eggs" against a
   gram-only food): current plan is to **skip and report** them, saving the rest. OK, or should it
   refuse and ask?
4. **Auto-save vs confirm**: save immediately, or show a preview the user confirms first? Current
   plan: save, then tell the user (they can edit/delete). A confirm step is safer but needs UI.
