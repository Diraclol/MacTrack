package com.dirac.mactrack.data.food

import com.dirac.mactrack.data.cnf.CnfFood
import com.dirac.mactrack.data.cnf.CnfMeasure
import com.dirac.mactrack.data.entity.FoodItem
import com.dirac.mactrack.data.entity.MealEntry
import com.dirac.mactrack.data.entity.Recipe
import com.dirac.mactrack.data.entity.RecipeIngredient

data class Nutrients(
    val kcal: Double, val protein: Double, val carb: Double, val fat: Double,
    val fiber: Double, val sugar: Double, val satFat: Double,
    val sodium: Double, val potassium: Double, val cholesterol: Double,
    // Defaulted so sources without caffeine data (CNF, Open Food Facts) need no change.
    val caffeine: Double = 0.0
) {
    operator fun times(m: Double) = Nutrients(
        kcal * m, protein * m, carb * m, fat * m, fiber * m, sugar * m,
        satFat * m, sodium * m, potassium * m, cholesterol * m, caffeine * m
    )

    operator fun plus(o: Nutrients) = Nutrients(
        kcal + o.kcal, protein + o.protein, carb + o.carb, fat + o.fat,
        fiber + o.fiber, sugar + o.sugar, satFat + o.satFat,
        sodium + o.sodium, potassium + o.potassium, cholesterol + o.cholesterol,
        caffeine + o.caffeine
    )

    companion object {
        val ZERO = Nutrients(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
    }
}

// A selectable portion (e.g. "g", "1 large egg", "serving") and the nutrients in ONE of it.
data class PortionUnit(
    val label: String,
    val per: Nutrients,
    val grams: Double?   // grams in one unit, if known; null when unknown
)

// The common shape every food source maps into, so one detail screen renders all of them.
data class FoodDetail(
    val name: String,
    val units: List<PortionUnit>,
    val defaultUnitLabel: String,
    val defaultAmount: Double
)

private fun isJunkMeasure(desc: String): Boolean {
    val d = desc.lowercase()
    return d.contains("refuse") || d.contains("as purchased") || d.isBlank()
}

fun cnfFoodDetail(food: CnfFood, measures: List<CnfMeasure>): FoodDetail {
    val perGram = Nutrients(
        food.kcal, food.protein, food.carb, food.fat, food.fiber, food.sugar,
        food.satFat, food.sodium, food.potassium, food.cholesterol
    ) * (1.0 / 100.0)

    val goodMeasures = measures.filter { it.grams > 0.0 && !isJunkMeasure(it.description) }

    val units = buildList {
        add(PortionUnit("g", perGram, 1.0))
        add(PortionUnit("oz", perGram * 28.3495, 28.3495))
        goodMeasures.forEach { m -> add(PortionUnit(m.description, perGram * m.grams, m.grams)) }
    }

    // Prefer a single natural unit ("1 slice", "1 large egg", "1 cup"); else any real
    // measure; else fall back to 100 g.
    val preferred = goodMeasures.firstOrNull { it.description.trim().startsWith("1 ") }
        ?: goodMeasures.firstOrNull()

    return if (preferred != null) {
        FoodDetail(food.name, units, defaultUnitLabel = preferred.description, defaultAmount = 1.0)
    } else {
        FoodDetail(food.name, units, defaultUnitLabel = "g", defaultAmount = 100.0)
    }
}

// Import a Common (CNF) food into the saved-foods table as a 100 g serving. The id is
// deterministic (cnf_<code>) so re-adding the same common food upserts instead of duplicating.
fun CnfFood.asFoodItem(): FoodItem = FoodItem(
    id = "cnf_$code",
    name = name,
    calories = kcal, proteinG = protein, carbG = carb, fatG = fat,
    fiberG = fiber, sugarG = sugar, satFatG = satFat,
    sodiumMg = sodium, potassiumMg = potassium, cholesterolMg = cholesterol,
    servingSize = 100.0, servingUnit = "g"
)

fun foodItemDetail(food: FoodItem): FoodDetail {
    val perServing = Nutrients(
        food.calories, food.proteinG, food.carbG, food.fatG, food.fiberG, food.sugarG,
        food.satFatG, food.sodiumMg, food.potassiumMg, food.cholesterolMg, food.caffeineMg
    )
    val gramsPerServing = if (food.servingUnit == "g" || food.servingUnit == "ml") food.servingSize else null
    val units = buildList {
        add(PortionUnit("serving", perServing, gramsPerServing))
        if (gramsPerServing != null && gramsPerServing > 0) {
            add(PortionUnit(food.servingUnit, perServing * (1.0 / gramsPerServing), 1.0))
        }
    }
    return FoodDetail(food.name, units, defaultUnitLabel = "serving", defaultAmount = 1.0)
}

// Reopen a logged entry when we cannot (or need not) reload its source food: rebuild a
// single-unit FoodDetail from the frozen snapshot. Per-unit nutrients = snapshot / amount.
fun mealEntryDetail(entry: MealEntry): FoodDetail {
    val amt = if (entry.amount > 0.0) entry.amount else 1.0
    val perUnit = Nutrients(
        entry.calories, entry.proteinG, entry.carbG, entry.fatG, entry.fiberG, entry.sugarG,
        entry.satFatG, entry.sodiumMg, entry.potassiumMg, entry.cholesterolMg, entry.caffeineMg
    ) * (1.0 / amt)
    val label = entry.unitLabel ?: entry.unit
    val units = listOf(PortionUnit(label, perUnit, grams = null))
    return FoodDetail(entry.foodName, units, defaultUnitLabel = label, defaultAmount = amt)
}

// A recipe maps to a per-serving FoodDetail so it flows through the same detail -> cart -> log
// path as any other food. Total = sum over ingredients of (that food's per-serving nutrients *
// servings used); per-serving = total / makesServings. When a cooked (finished) weight is known,
// a "g" unit additionally lets you log by grams of the batch.
fun recipeDetail(
    recipe: Recipe,
    ingredients: List<RecipeIngredient>,
    foodsById: Map<String, FoodItem>
): FoodDetail {
    val total = ingredients.fold(Nutrients.ZERO) { acc, ing ->
        val food = foodsById[ing.foodId] ?: return@fold acc
        val perServing = Nutrients(
            food.calories, food.proteinG, food.carbG, food.fatG, food.fiberG, food.sugarG,
            food.satFatG, food.sodiumMg, food.potassiumMg, food.cholesterolMg, food.caffeineMg
        )
        acc + perServing * ing.amount
    }
    val n = if (recipe.makesServings > 0.0) recipe.makesServings else 1.0
    val perServing = total * (1.0 / n)
    val cooked = recipe.cookedWeightG
    val gramsPerServing = if (cooked != null && cooked > 0.0) cooked / n else null
    val units = buildList {
        add(PortionUnit("serving", perServing, gramsPerServing))
        if (cooked != null && cooked > 0.0) {
            add(PortionUnit("g", total * (1.0 / cooked), 1.0))
        }
    }
    return FoodDetail(recipe.name, units, defaultUnitLabel = "serving", defaultAmount = 1.0)
}

data class Staged(val quantity: Double, val unit: String, val nutrients: Nutrients)

private fun round2(x: Double): Double = kotlin.math.round(x * 100.0) / 100.0

fun stagePortion(amount: Double, unit: PortionUnit): Staged {
    val n = unit.per * amount
    val g = unit.grams
    return if (g != null) Staged(round2(amount * g), "g", n) else Staged(round2(amount), unit.label, n)
}

// ---- Favorite serving units (UI-15) -----------------------------------------------------------
// A generic weight/volume unit the user can pin to the FRONT of every food's serving picker.
// `grams` is the weight of ONE of this unit. For VOLUME units it is really the millilitre size under
// a standard ~1 g/ml assumption (MacTrack stores no per-food density), so those gram figures are
// estimates. `isVolume` is only used by the settings screen, to label the volume ones as estimates.
data class GenericServingUnit(
    val key: String,
    val label: String,
    val grams: Double,
    val isVolume: Boolean
)

// The catalog offered on the "Favorite Serving Units" screen: exact-conversion weight units first,
// then volume units (estimated). Keys double as the stored preference values and deliberately match
// the labels foods already use ("g", "oz", "ml"), so favouriting one of those reuses the food's own
// unit instead of synthesising a duplicate.
val FAVORITE_UNIT_CATALOG: List<GenericServingUnit> = listOf(
    GenericServingUnit("g", "g", 1.0, isVolume = false),
    GenericServingUnit("oz", "oz", 28.3495, isVolume = false),
    GenericServingUnit("lb", "lb", 453.592, isVolume = false),
    GenericServingUnit("ml", "ml", 1.0, isVolume = true),
    GenericServingUnit("tsp", "tsp", 4.92892, isVolume = true),
    GenericServingUnit("tbsp", "tbsp", 14.7868, isVolume = true),
    GenericServingUnit("fl oz", "fl oz", 29.5735, isVolume = true),
    GenericServingUnit("cup", "cup", 240.0, isVolume = true)
)

// Return a copy of this FoodDetail with the user's favourite units pulled to the FRONT of the unit
// list (so they are the first serving chips). A favourite the food already lists is just moved up; a
// favourite it lacks is synthesised from the food's per-gram nutrients, but only when a gram basis
// exists (some existing unit whose gram weight is known). A food with no gram basis (e.g. a frozen
// log snapshot logged in an unknown unit) keeps only the favourites it already lists. The default
// selected unit is left untouched -- favourites change chip order, not which unit is preselected.
fun FoodDetail.withFavoriteUnits(favoriteKeys: List<String>): FoodDetail {
    if (favoriteKeys.isEmpty()) return this
    val basis = units.firstOrNull { it.grams != null && it.grams > 0.0 }
    val perGram: Nutrients? = basis?.let { u -> u.grams?.let { g -> u.per * (1.0 / g) } }
    val ordered = ArrayList(units)
    // Prepend in reverse so favoriteKeys[0] ends up leftmost.
    for (key in favoriteKeys.asReversed()) {
        val existing = ordered.indexOfFirst { it.label == key }
        if (existing >= 0) {
            ordered.add(0, ordered.removeAt(existing))
        } else {
            val generic = FAVORITE_UNIT_CATALOG.firstOrNull { it.key == key } ?: continue
            if (perGram == null) continue
            ordered.add(0, PortionUnit(generic.label, perGram * generic.grams, generic.grams))
        }
    }
    return copy(units = ordered)
}