package com.dirac.mactrack.domain.calc

fun mifflinStJeorBmr(
    sex: Sex,
    weightKg: Double,
    heightCm: Double,
    age: Int
): Double {
    val base = (10 * weightKg) + (6.25 * heightCm) - (5 * age)
    val sexAdjustment = if (sex == Sex.MALE) 5 else -161
    return base + sexAdjustment
}

fun tdee(bmr: Double, activityLevel: ActivityLevel): Double {
    return bmr * activityLevel.multiplier
}
data class MacroTargets(
    val calories: Double,
    val proteinG: Double,
    val carbG: Double,
    val fatG: Double
)

fun macroTargets(
    targetCalories: Double,
    bodyWeightLb: Double,
    proteinPerLb: Double,
    fatFraction: Double
): MacroTargets {
    val proteinG = bodyWeightLb * proteinPerLb
    val proteinCalories = proteinG * 4
    val fatCalories = targetCalories * fatFraction
    val fatG = fatCalories / 9
    val remaining = targetCalories - proteinCalories - fatCalories
    val carbCalories = if (remaining > 0) remaining else 0.0
    val carbG = carbCalories / 4
    return MacroTargets(
        calories = targetCalories,
        proteinG = proteinG,
        carbG = carbG,
        fatG = fatG
    )
}