package com.dirac.mactrack.data.ai.recipe

import com.dirac.mactrack.data.food.Nutrients

// Domain model for AI-4: turning a list of ingredients into a saved Recipe or Meal.
// See docs/AI4_PLAN.md for the architecture. These types are the contract between the parsing step
// (the model returns JSON) and the deterministic resolve + save step (plain app code).

// What the user asked the AI to create.
enum class BuildTarget { RECIPE, MEAL }

// One ingredient as parsed from the user's request, BEFORE it is matched to a food in a database.
// `unit` is a free string: "g", "ml", "serving", or something like "cup" / "egg" / "slice".
data class ParsedIngredient(
    val name: String,
    val quantity: Double,
    val unit: String
)

// The whole request the model extracted from the user's message.
data class RecipeBuildRequest(
    val target: BuildTarget,
    val name: String,
    val ingredients: List<ParsedIngredient>
)

// One ingredient AFTER trying to resolve it to a saved food (a food_items row). When `foodId` is
// null the ingredient could not be matched (or its unit could not be converted) and is reported to
// the user rather than saved. `servings` is the amount expressed in servings of the resolved food,
// which is exactly what RecipeIngredient.amount / MealTemplateItem.amount store.
data class ResolvedIngredient(
    val parsed: ParsedIngredient,
    val foodId: String?,
    val servings: Double,
    val source: String,          // "custom" | "cnf" | "off" | "unresolved"
    val perServing: Nutrients?   // nutrients in ONE serving of the resolved food, for the summary
) {
    val resolved: Boolean get() = foodId != null

    // Nutrients this ingredient contributes to the recipe/meal total (per serving * servings).
    fun contribution(): Nutrients = perServing?.let { it * servings } ?: Nutrients.ZERO
}
