package com.dirac.mactrack.data.food

import com.dirac.mactrack.data.cnf.CnfFood
import com.dirac.mactrack.data.entity.FoodItem
import com.dirac.mactrack.data.entity.MealEntry
import com.dirac.mactrack.data.entity.Recipe
import com.dirac.mactrack.data.entity.RecipeIngredient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// The source->FoodDetail mappers and portion staging carry every food (Common/CNF, custom, recipe)
// into the one shape the detail/cart/log path consumes. These lock down that math.
class FoodMappersTest {

    private fun food(
        id: String,
        cal: Double,
        protein: Double = 0.0,
        carb: Double = 0.0,
        fat: Double = 0.0,
        size: Double = 1.0,
        unit: String = "serving"
    ) = FoodItem(
        id = id, name = id, calories = cal, proteinG = protein, carbG = carb, fatG = fat,
        servingSize = size, servingUnit = unit
    )

    private fun nutrients(kcal: Double) =
        Nutrients(kcal, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)

    // --- asFoodItem (CNF import) ---

    @Test
    fun asFoodItem_usesDeterministicIdAnd100gServing() {
        val cnf = CnfFood(
            code = 4321, name = "Rolled oats",
            kcal = 389.0, protein = 16.9, carb = 66.3, fat = 6.9,
            fiber = 10.6, sugar = 0.99, satFat = 1.2,
            sodium = 2.0, potassium = 429.0, cholesterol = 0.0
        )
        val item = cnf.asFoodItem()
        assertEquals("cnf_4321", item.id)
        assertEquals("Rolled oats", item.name)
        assertEquals(389.0, item.calories, 0.0)
        assertEquals(16.9, item.proteinG, 0.0)
        assertEquals(100.0, item.servingSize, 0.0)
        assertEquals("g", item.servingUnit)
    }

    // --- foodItemDetail ---

    @Test
    fun foodItemDetail_gramFood_addsPerGramUnit() {
        val detail = foodItemDetail(food("rice", cal = 130.0, size = 100.0, unit = "g"))
        assertEquals("serving", detail.defaultUnitLabel)
        assertEquals(1.0, detail.defaultAmount, 0.0)
        assertEquals(2, detail.units.size)
        val serving = detail.units[0]
        assertEquals("serving", serving.label)
        assertEquals(130.0, serving.per.kcal, 0.0001)
        assertEquals(100.0, serving.grams!!, 0.0)
        val perGram = detail.units[1]
        assertEquals("g", perGram.label)
        assertEquals(1.3, perGram.per.kcal, 0.0001)
        assertEquals(1.0, perGram.grams!!, 0.0)
    }

    @Test
    fun foodItemDetail_nonGramFood_hasSingleServingUnit() {
        val detail = foodItemDetail(food("egg", cal = 72.0, unit = "serving", size = 1.0))
        assertEquals(1, detail.units.size)
        assertEquals("serving", detail.units[0].label)
        assertNull(detail.units[0].grams)
    }

    // --- mealEntryDetail ---

    @Test
    fun mealEntryDetail_dividesSnapshotByAmount() {
        val entry = MealEntry(
            date = "2026-01-01", timeMinutes = 480, foodName = "Toast", amount = 2.0,
            calories = 200.0, proteinG = 8.0, carbG = 40.0, fatG = 2.0,
            unit = "serving", unitLabel = "slice"
        )
        val detail = mealEntryDetail(entry)
        assertEquals("slice", detail.defaultUnitLabel)
        assertEquals(2.0, detail.defaultAmount, 0.0)
        assertEquals(1, detail.units.size)
        // 200 kcal logged over 2 slices = 100 kcal per slice.
        assertEquals(100.0, detail.units[0].per.kcal, 0.0001)
        assertEquals(4.0, detail.units[0].per.protein, 0.0001)
    }

    @Test
    fun mealEntryDetail_guardsAgainstZeroAmount() {
        val entry = MealEntry(
            date = "2026-01-01", timeMinutes = 0, foodName = "Odd", amount = 0.0,
            calories = 50.0, proteinG = 1.0, carbG = 2.0, fatG = 3.0
        )
        val detail = mealEntryDetail(entry)
        // amount 0 is treated as 1, so per-unit == the snapshot itself.
        assertEquals(50.0, detail.units[0].per.kcal, 0.0001)
        assertEquals(1.0, detail.defaultAmount, 0.0)
    }

    // --- recipeDetail ---

    @Test
    fun recipeDetail_perServingIsTotalOverServings() {
        val foods = mapOf(
            "a" to food("a", cal = 100.0, protein = 10.0),
            "b" to food("b", cal = 50.0, protein = 5.0)
        )
        val recipe = Recipe(id = "r", name = "Bowl", makesServings = 2.0)
        val ingredients = listOf(
            RecipeIngredient(recipeId = "r", foodId = "a", amount = 2.0),
            RecipeIngredient(recipeId = "r", foodId = "b", amount = 1.0)
        )
        val detail = recipeDetail(recipe, ingredients, foods)
        // total = 100*2 + 50*1 = 250 kcal, 25 g protein; per serving (÷2) = 125 kcal, 12.5 g.
        assertEquals(1, detail.units.size)
        assertEquals("serving", detail.units[0].label)
        assertEquals(125.0, detail.units[0].per.kcal, 0.0001)
        assertEquals(12.5, detail.units[0].per.protein, 0.0001)
        assertNull(detail.units[0].grams)
    }

    @Test
    fun recipeDetail_withCookedWeightAddsGramUnit() {
        val foods = mapOf("a" to food("a", cal = 100.0))
        val recipe = Recipe(id = "r", name = "Loaf", makesServings = 2.0, cookedWeightG = 500.0)
        val ingredients = listOf(RecipeIngredient(recipeId = "r", foodId = "a", amount = 5.0))
        val detail = recipeDetail(recipe, ingredients, foods)
        // total = 500 kcal; per serving = 250 kcal over 250 g; per gram = 500/500 = 1 kcal/g.
        assertEquals(2, detail.units.size)
        assertEquals(250.0, detail.units[0].per.kcal, 0.0001)
        assertEquals(250.0, detail.units[0].grams!!, 0.0)
        assertEquals("g", detail.units[1].label)
        assertEquals(1.0, detail.units[1].per.kcal, 0.0001)
        assertEquals(1.0, detail.units[1].grams!!, 0.0)
    }

    @Test
    fun recipeDetail_skipsIngredientsWhoseFoodIsMissing() {
        val foods = mapOf("a" to food("a", cal = 100.0))
        val recipe = Recipe(id = "r", name = "Partial", makesServings = 1.0)
        val ingredients = listOf(
            RecipeIngredient(recipeId = "r", foodId = "a", amount = 1.0),
            RecipeIngredient(recipeId = "r", foodId = "gone", amount = 3.0)
        )
        val detail = recipeDetail(recipe, ingredients, foods)
        // Only "a" contributes; the missing food is skipped, not counted as zero-and-crash.
        assertEquals(100.0, detail.units[0].per.kcal, 0.0001)
    }

    // --- stagePortion ---

    @Test
    fun stagePortion_gramBackedUnit_reportsGramQuantity() {
        val unit = PortionUnit("15 ml", nutrients(10.0), grams = 15.0)
        val staged = stagePortion(amount = 2.0, unit = unit)
        // 2 x 15 ml -> 30 g, nutrients scaled by 2.
        assertEquals(30.0, staged.quantity, 0.0)
        assertEquals("g", staged.unit)
        assertEquals(20.0, staged.nutrients.kcal, 0.0001)
    }

    @Test
    fun stagePortion_unitlessUnit_keepsItsLabel() {
        val unit = PortionUnit("scoop", nutrients(120.0), grams = null)
        val staged = stagePortion(amount = 1.5, unit = unit)
        assertEquals(1.5, staged.quantity, 0.0)
        assertEquals("scoop", staged.unit)
        assertEquals(180.0, staged.nutrients.kcal, 0.0001)
    }

    @Test
    fun stagePortion_roundsGramQuantityToTwoDecimals() {
        val unit = PortionUnit("1 unit", nutrients(1.0), grams = 33.333)
        val staged = stagePortion(amount = 1.0, unit = unit)
        assertEquals(33.33, staged.quantity, 0.0)
        assertTrue(staged.quantity <= 33.34)
    }
}
