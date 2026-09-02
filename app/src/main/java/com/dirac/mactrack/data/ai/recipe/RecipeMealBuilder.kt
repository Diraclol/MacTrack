package com.dirac.mactrack.data.ai.recipe

import com.dirac.mactrack.data.food.Nutrients
import com.dirac.mactrack.data.repository.MealTemplateRepository
import com.dirac.mactrack.data.repository.RecipeRepository

// A resolved-but-not-yet-saved recipe/meal: the totals and per-ingredient results, ready to show the
// user for confirmation. Nothing is written to the recipes/meals tables until commit() is called.
// (Resolving does upsert a food_items row for each CNF/OFF hit, the same as picking an ingredient in
// search does -- those are harmless saved foods even if the user cancels the recipe.)
data class BuildPreview(
    val target: BuildTarget,
    val name: String,
    val total: Nutrients,
    val resolved: List<ResolvedIngredient>,  // matched ingredients that would be saved
    val skipped: List<ResolvedIngredient>    // ingredients that couldn't be matched/converted
) {
    val canSave: Boolean get() = resolved.isNotEmpty()
}

// AI-4 Stage 3/4. Resolves every parsed ingredient (saved -> CNF -> OFF) into a preview the user can
// review, then, on confirmation, saves it as a new Recipe or MealTemplate. Thin orchestration over
// IngredientResolver + the repositories; verified on device.
class RecipeMealBuilder(
    private val resolver: IngredientResolver,
    private val recipeRepository: RecipeRepository,
    private val mealTemplateRepository: MealTemplateRepository
) {

    // Resolve without saving the recipe/meal, so the caller can show the user what will be created.
    suspend fun preview(request: RecipeBuildRequest): BuildPreview {
        val resolvedAll = request.ingredients.map { resolver.resolve(it) }
        val usable = resolvedAll.filter { it.resolved }
        val skipped = resolvedAll.filterNot { it.resolved }
        val total = usable.fold(Nutrients.ZERO) { acc, r -> acc + r.contribution() }
        return BuildPreview(
            target = request.target, name = request.name, total = total,
            resolved = usable, skipped = skipped
        )
    }

    // Persist a previewed recipe/meal. Returns the new recipe id (RECIPE) or null (MEAL, whose save has
    // no id / nothing resolved). RecipeIngredient / MealTemplateItem store (foodId -> servings).
    suspend fun commit(preview: BuildPreview): String? {
        if (preview.resolved.isEmpty()) return null
        val pairs = preview.resolved.map { it.foodId!! to it.servings }
        return when (preview.target) {
            BuildTarget.RECIPE -> recipeRepository.saveRecipe(
                name = preview.name, makesServings = 1.0, cookedWeightG = null, emoji = null, ingredients = pairs
            )
            BuildTarget.MEAL -> {
                mealTemplateRepository.saveTemplate(preview.name, pairs)
                null
            }
        }
    }
}
