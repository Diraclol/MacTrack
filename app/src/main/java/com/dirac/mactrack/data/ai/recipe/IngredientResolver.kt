package com.dirac.mactrack.data.ai.recipe

import com.dirac.mactrack.data.cnf.CnfRepository
import com.dirac.mactrack.data.entity.FoodItem
import com.dirac.mactrack.data.food.Nutrients
import com.dirac.mactrack.data.food.asFoodItem
import com.dirac.mactrack.data.off.OffProduct
import com.dirac.mactrack.data.off.OpenFoodFactsRepository
import com.dirac.mactrack.data.repository.FoodRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

// AI-4 Stage 2. Resolves one AI-parsed ingredient to a saved food (a food_items row), so it can be
// stored as a RecipeIngredient / MealTemplateItem (which reference food_items by id). Priority:
//   1. the user's saved foods (their own data; may include saved branded items)
//   2. the Canadian Nutrient File (offline common foods)
//   3. Open Food Facts (online branded fallback)
// CNF/OFF hits are upserted into food_items with deterministic ids (cnf_<code> / off_<code>) so a
// re-run reuses the same row instead of duplicating. The order is a product choice and easy to change.
//
// This is thin orchestration over the repositories; the load-bearing arithmetic (amount -> servings)
// lives in servingsFor(), which is unit-tested separately. Verified end-to-end on device.
class IngredientResolver(
    private val foodRepository: FoodRepository,
    private val cnfRepository: CnfRepository,
    private val offRepository: OpenFoodFactsRepository
) {

    suspend fun resolve(ingredient: ParsedIngredient): ResolvedIngredient {
        val name = ingredient.name.trim()
        if (name.isBlank()) return unresolved(ingredient)

        // 1. Saved foods (no name-query DAO, so match against a snapshot in memory).
        val saved = foodRepository.getAllFoods().first().let { foods ->
            foods.firstOrNull { it.name.equals(name, ignoreCase = true) }
                ?: foods.firstOrNull { it.name.contains(name, ignoreCase = true) }
        }
        if (saved != null) return finalize(ingredient, saved, source = "custom", persist = false)

        // 2. CNF (offline). search() is ORDER BY length(name), so the first hit is the closest match.
        val cnf = withContext(Dispatchers.IO) { cnfRepository.search(name) }.firstOrNull()
        if (cnf != null) return finalize(ingredient, cnf.asFoodItem(), source = "cnf", persist = true)

        // 3. Open Food Facts (online). Empty on no network -> falls through to unresolved.
        val off = offRepository.searchByName(name).firstOrNull()
        if (off != null) return finalize(ingredient, offFoodItem(off), source = "off", persist = true)

        return unresolved(ingredient)
    }

    // Compute the servings amount first; only persist a synthesised FoodItem once we know the
    // ingredient is usable, so an unconvertible unit never leaves an orphan food_items row.
    private suspend fun finalize(
        ingredient: ParsedIngredient,
        item: FoodItem,
        source: String,
        persist: Boolean
    ): ResolvedIngredient {
        val servings = servingsFor(ingredient.quantity, ingredient.unit, item.servingSize, item.servingUnit)
            ?: return unresolved(ingredient)
        if (persist) foodRepository.addFood(item)
        return ResolvedIngredient(
            parsed = ingredient,
            foodId = item.id,
            servings = servings,
            source = source,
            perServing = item.perServing()
        )
    }

    private fun unresolved(ingredient: ParsedIngredient) =
        ResolvedIngredient(parsed = ingredient, foodId = null, servings = 0.0, source = "unresolved", perServing = null)

    // Build a saved food from an Open Food Facts name-search hit. OffProduct carries only the four
    // macros per 100 g (search rows are macro-only), so micros are left 0; a later pass could enrich
    // via offRepository.lookup(code). Deterministic id keeps re-runs idempotent.
    private fun offFoodItem(off: OffProduct): FoodItem = FoodItem(
        id = "off_${off.code}",
        name = off.name,
        calories = off.kcalPer100,
        proteinG = off.proteinPer100,
        carbG = off.carbPer100,
        fatG = off.fatPer100,
        servingSize = 100.0,
        servingUnit = "g",
        barcode = off.code
    )
}

// Nutrients in ONE serving of a saved food (all FoodItem nutrient columns are per serving).
private fun FoodItem.perServing(): Nutrients = Nutrients(
    kcal = calories, protein = proteinG, carb = carbG, fat = fatG,
    fiber = fiberG, sugar = sugarG, satFat = satFatG,
    sodium = sodiumMg, potassium = potassiumMg, cholesterol = cholesterolMg, caffeine = caffeineMg
)
