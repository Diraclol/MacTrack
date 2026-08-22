package com.dirac.mactrack.data.cnf

data class CnfFood(
    val code: Int,
    val name: String,
    // all per 100 g
    val kcal: Double,
    val protein: Double,
    val carb: Double,
    val fat: Double,
    val fiber: Double,
    val sugar: Double,
    val satFat: Double,
    val sodium: Double,
    val potassium: Double,
    val cholesterol: Double
)