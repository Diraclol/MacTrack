package com.dirac.mactrack.data.ai.recipe

import com.dirac.mactrack.data.food.Nutrients
import com.dirac.mactrack.data.repository.MealTemplateRepository
import com.dirac.mactrack.data.repository.RecipeRepository

// The outcome of building a recipe/meal from an ingredient list: what was saved, its total macros,
// and which ingredients could not be matched (so the caller can tell the user).
data class BuildResult(
    val target: BuildTarget,
    val name: String,
    val savedId: String?,                    // the new recipe id; null for meals (saveTemplate has no id) or if nothing was saved
    val saved: Boolean,
    val total: Nutrients,
    val resolved: List<ResolvedIngredient>,  // the ingredients that were saved
    val skipped: List<ResolvedIngredient>    // the ingredients that could not be matched/converted
)

// AI-4 Stage 3. Resolves every parsed ingredient (saved -> CNF -> OFF) and saves the result as a new
// Recipe or MealTemplate. Ingredients that don't resolve are skipped and reported rather than
// blocking the save; if none resolve, nothing is saved. Thin orchestration over IngredientResolver +
// the repositories; verified on device.
class RecipeMealBuilder(
    private val resolver: IngredientResolver,
    private val recipeRepository: RecipeRepository,
    private val mealTemplateRepository: MealTemplateRepository
) {

    suspend fun build(request: RecipeBuildRequest): BuildResult {
        val resolvedAll = request.ingredients.map { resolver.resolve(it) }
        val usable = resolvedAll.filter { it.resolved }
        val skipped = resolvedAll.filterNot { it.resolved }
        val total = usable.fold(Nutrients.ZERO) { acc, r -> acc + r.contribution() }

        if (usable.isEmpty()) {
            return BuildResult(
                target = request.target, name = request.name, savedId = null, saved = false,
                total = Nutrients.ZERO, resolved = emptyList(), skipped = skipped
            )
        }

        // RecipeIngredient / MealTemplateItem store (foodId -> servings of that food).
        val pairs = usable.map { it.foodId!! to it.servings }
        val savedId = when (request.target) {
            BuildTarget.RECIPE -> recipeRepository.saveRecipe(
                name = request.name, makesServings = 1.0, cookedWeightG = null, emoji = null, ingredients = pairs
            )
            BuildTarget.MEAL -> {
                mealTemplateRepository.saveTemplate(request.name, pairs)
                null
            }
        }

        return BuildResult(
            target = request.target, name = request.name, savedId = savedId, saved = true,
            total = total, resolved = usable, skipped = skipped
        )
    }
}
