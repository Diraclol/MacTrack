package com.dirac.mactrack.data.ai.recipe

// Convert a requested amount into the number of SERVINGS of a food, so an AI-parsed ingredient can
// be stored as a RecipeIngredient / MealTemplateItem (whose `amount` is "servings of that food").
//
// A food's one serving is `servingSize servingUnit` (e.g. 100 g, or 1 "serving", or 1 "slice").
// Weight/volume factors match the Favorite Serving Units feature (UI-15); volume uses the standard
// ~1 g/ml assumption, so a volume-based conversion is an estimate.

// Grams (or millilitres, treated as grams under ~1 g/ml) in ONE of `unit`, or null if `unit` is not
// a recognised weight/volume unit (e.g. "serving", "egg", "slice").
private fun gramsPerUnit(unit: String): Double? = when (unit.trim().lowercase()) {
    "g", "gram", "grams" -> 1.0
    "kg", "kilogram", "kilograms" -> 1000.0
    "mg", "milligram", "milligrams" -> 0.001
    "oz", "ounce", "ounces" -> 28.3495
    "lb", "lbs", "pound", "pounds" -> 453.592
    "ml", "milliliter", "millilitre", "milliliters", "millilitres", "cc" -> 1.0
    "l", "liter", "litre", "liters", "litres" -> 1000.0
    "tsp", "teaspoon", "teaspoons" -> 4.92892
    "tbsp", "tablespoon", "tablespoons" -> 14.7868
    "fl oz", "floz", "fluid ounce", "fluid ounces" -> 29.5735
    "cup", "cups" -> 240.0
    else -> null
}

private fun isServingWord(unit: String): Boolean =
    unit.trim().lowercase().let { it.isEmpty() || it == "serving" || it == "servings" }

/**
 * How many servings of a food (one serving = `servingSize` `servingUnit`) equal `quantity` `unit`.
 * Returns null when the amount is non-positive or the units cannot be reconciled (e.g. "2 cups" of a
 * food whose serving is "1 slice") — the caller then reports that ingredient as unresolved rather
 * than guessing.
 */
fun servingsFor(
    quantity: Double,
    unit: String,
    servingSize: Double,
    servingUnit: String
): Double? {
    if (quantity <= 0.0 || servingSize <= 0.0) return null

    // "2 servings" means two of the food's own servings, regardless of how a serving is defined.
    if (isServingWord(unit)) return quantity

    val reqGrams = gramsPerUnit(unit)
    val servGrams = gramsPerUnit(servingUnit)

    // Both sides are weight/volume: exact (or ~1 g/ml estimated) gram ratio.
    if (reqGrams != null && servGrams != null) {
        return (quantity * reqGrams) / (servingSize * servGrams)
    }

    // Neither side is weight/volume, but they name the same piece unit (e.g. "slice" and "slice"):
    // count ratio against how many of that piece make one serving.
    if (reqGrams == null && servGrams == null &&
        unit.trim().lowercase() == servingUnit.trim().lowercase()
    ) {
        return quantity / servingSize
    }

    // One side is weight/volume and the other is a piece/serving: not reconcilable without a
    // per-piece weight we do not have.
    return null
}
