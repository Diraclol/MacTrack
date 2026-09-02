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

- **[DONE] Stage 1 — domain model + serving math.** `ParsedIngredient`, `RecipeBuildRequest`,
  `ResolvedIngredient`, `BuildTarget` (`RecipeBuildModels.kt`) and a pure `servingsFor(...)`
  (`ServingMath.kt`) that converts a requested amount into servings of a food (mass/volume via the
  same factors as UI-15; honest `null` when units can't be reconciled). Unit-tested
  (`ServingMathTest`).
- **[DONE] Stage 2 — `IngredientResolver`.** `suspend resolve(ParsedIngredient) ->
  ResolvedIngredient`, priority saved -> CNF -> OFF, upserting a FoodItem for CNF/OFF hits
  (`cnf_<code>` / `off_<code>`, idempotent), computing `servings` via `servingsFor`, persisting only
  when the unit converts. Reviewed; device-tested.
- **[DONE] Stage 3 — `RecipeRequestParser` + `RecipeMealBuilder`.** Parser lifts the JSON object out
  of the model reply and validates it; builder resolves every ingredient, sums macros, and saves a
  new Recipe or MealTemplate, skipping+reporting anything unresolved (saves nothing if none resolve).
  Reviewed; device-tested.

- **[TODO — DIRAC] Stage 4 — wire into `AiViewModel`.** This is deliberately left for you: it changes
  the AI chat's behaviour (a design-sensitive surface) and its result can't be verified without the
  live model + a device, and it depends on the open questions below. Concrete spec:
  1. **Construction:** give `AiViewModel` an `IngredientResolver` + `RecipeMealBuilder` (add
     `foodRepository`, `cnfRepository`, `openFoodFactsRepository`, `recipeRepository`,
     `mealTemplateRepository` — all already `lazy val`s on `MacTrackApplication` — through
     `AiViewModel.Factory`, and build the two collaborators there).
  2. **Prompt:** extend `SYSTEM_PROMPT` so that WHEN (and only when) the user asks to save an
     ingredient list as a recipe or meal, the model replies with ONLY the JSON object
     `{ "target": "recipe"|"meal", "name": "...", "ingredients": [ {"name","quantity","unit"} ] }`,
     quantities in grams where reasonable, `unit` = "serving" for countable items it can't gram.
  3. **Dispatch:** in `send()`, after the reply completes, `RecipeRequestParser.parse(reply)`. If
     non-null, call `RecipeMealBuilder.build(request)` and replace/append an assistant bubble with a
     summary: "Saved {recipe|meal} 'X' — {kcal} cal, {P}/{C}/{F} g. Couldn't match: {names}." If
     null, treat the reply as an ordinary chat message (today's behaviour).
  4. **`UiMessage`/history:** the build JSON turn is machine-facing; consider hiding the raw JSON from
     the chat (show only the friendly summary).
  Everything Stage 4 calls is already on `main` and compile-clean.

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
