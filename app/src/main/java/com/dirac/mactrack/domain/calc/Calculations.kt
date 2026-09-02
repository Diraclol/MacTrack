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

// Katch-McArdle: the most accurate BMR estimate when body fat is known, because it works off lean
// body mass alone (fat is roughly metabolically inert). bodyFatPct is a percentage, e.g. 16.0 = 16%.
// LBM = weight * (1 - bodyFat/100). This is what tdeecalculator.net switches to once you fill in body
// fat (their sample 119 lb / 16% gives 370 + 21.6 * 45.34 kg ~= 1349, matching their page).
fun katchMcArdleBmr(weightKg: Double, bodyFatPct: Double): Double {
    val leanMassKg = weightKg * (1.0 - bodyFatPct / 100.0)
    return 370.0 + 21.6 * leanMassKg
}

// Revised Harris-Benedict (1984). Calculated for reference / for people who prefer it; not the app's
// default. Kept here so the numbers exist if a screen ever wants to show them.
fun harrisBenedictBmr(
    sex: Sex,
    weightKg: Double,
    heightCm: Double,
    age: Int
): Double = if (sex == Sex.MALE) {
    (13.397 * weightKg + 4.799 * heightCm - 5.677 * age) + 88.362
} else {
    (9.247 * weightKg + 3.098 * heightCm - 4.330 * age) + 447.593
}

// Which BMR the app actually uses: Katch-McArdle when a valid body fat % is known (most accurate),
// otherwise Mifflin-St Jeor. Same rule as tdeecalculator.net.
fun basalMetabolicRate(
    sex: Sex,
    weightKg: Double,
    heightCm: Double,
    age: Int,
    bodyFatPct: Double?
): Double =
    if (bodyFatPct != null && bodyFatPct > 0.0) katchMcArdleBmr(weightKg, bodyFatPct)
    else mifflinStJeorBmr(sex, weightKg, heightCm, age)

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
