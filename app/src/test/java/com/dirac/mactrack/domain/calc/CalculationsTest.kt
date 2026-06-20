package com.dirac.mactrack.domain.calc

import org.junit.Assert.assertEquals
import org.junit.Test

class CalculationsTest {

    @Test
    fun mifflinStJeor_male_isCorrect() {
        // 800 + 1125 - 150 + 5 = 1780
        val bmr = mifflinStJeorBmr(Sex.MALE, weightKg = 80.0, heightCm = 180.0, age = 30)
        assertEquals(1780.0, bmr, 0.001)
    }

    @Test
    fun mifflinStJeor_female_isCorrect() {
        // 650 + 1031.25 - 140 - 161 = 1380.25
        val bmr = mifflinStJeorBmr(Sex.FEMALE, weightKg = 65.0, heightCm = 165.0, age = 28)
        assertEquals(1380.25, bmr, 0.001)
    }

    @Test
    fun tdee_appliesMultiplier() {
        val result = tdee(bmr = 1780.0, activityLevel = ActivityLevel.MODERATE)
        assertEquals(2759.0, result, 0.001)
    }

    @Test
    fun macroTargets_normalCase() {
        val r = macroTargets(
            targetCalories = 2200.0,
            bodyWeightLb = 180.0,
            proteinPerLb = 1.0,
            fatFraction = 0.275
        )
        assertEquals(180.0, r.proteinG, 0.001)
        assertEquals(605.0 / 9.0, r.fatG, 0.001)
        assertEquals(218.75, r.carbG, 0.001)
    }

    @Test
    fun macroTargets_clampsCarbsToZeroWhenOverbudget() {
        val r = macroTargets(
            targetCalories = 800.0,
            bodyWeightLb = 120.0,
            proteinPerLb = 1.2,
            fatFraction = 0.35
        )
        assertEquals(0.0, r.carbG, 0.001)
    }
}