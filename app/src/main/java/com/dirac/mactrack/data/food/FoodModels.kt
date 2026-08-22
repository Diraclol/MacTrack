package com.dirac.mactrack.data.food

import com.dirac.mactrack.data.cnf.CnfFood
import com.dirac.mactrack.data.cnf.CnfMeasure
import com.dirac.mactrack.data.entity.FoodItem

data class Nutrients(
    val kcal: Double, val protein: Double, val carb: Double, val fat: Double,
    val fiber: Double, val sugar: Double, val satFat: Double,
    val sodium: Double, val potassium: Double, val cholesterol: Double
) {
    operator fun times(m: Double) = Nutrients(
        kcal * m, protein * m, carb * m, fat * m, fiber * m, sugar * m,
        satFat * m, sodium * m, potassium * m, cholesterol * m
    )
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
    val defaultAmount: Double
)

fun cnfFoodDetail(food: CnfFood, measures: List<CnfMeasure>): FoodDetail {
    val perGram = Nutrients(
        food.kcal, food.protein, food.carb, food.fat, food.fiber, food.sugar,
        food.satFat, food.sodium, food.potassium, food.cholesterol
    ) * (1.0 / 100.0)
    val units = buildList {
        add(PortionUnit("g", perGram, 1.0))
        add(PortionUnit("oz", perGram * 28.3495, 28.3495))
        measures.forEach { m -> add(PortionUnit(m.description, perGram * m.grams, m.grams)) }
    }
    return FoodDetail(food.name, units, defaultAmount = 100.0)
}

fun foodItemDetail(food: FoodItem): FoodDetail {
    val perServing = Nutrients(
        food.calories, food.proteinG, food.carbG, food.fatG, food.fiberG, food.sugarG,
        food.satFatG, food.sodiumMg, food.potassiumMg, food.cholesterolMg
    )
    val gramsPerServing = if (food.servingUnit == "g" || food.servingUnit == "ml") food.servingSize else null
    val units = buildList {
        add(PortionUnit("serving", perServing, gramsPerServing))
        if (gramsPerServing != null && gramsPerServing > 0) {
            add(PortionUnit(food.servingUnit, perServing * (1.0 / gramsPerServing), 1.0))
        }
    }
    return FoodDetail(food.name, units, defaultAmount = 1.0)
}

data class Staged(val quantity: Double, val unit: String, val nutrients: Nutrients)

fun stagePortion(amount: Double, unit: PortionUnit): Staged {
    val n = unit.per * amount
    val g = unit.grams
    return if (g != null) Staged(amount * g, "g", n) else Staged(amount, unit.label, n)
}