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
    fun katchMcArdle_isCorrect() {
        // 20% body fat on 80 kg -> 64 kg lean -> 370 + 21.6*64 = 1752.4
        val bmr = katchMcArdleBmr(weightKg = 80.0, bodyFatPct = 20.0)
        assertEquals(1752.4, bmr, 0.001)
    }

    @Test
    fun katchMcArdle_matchesTdeeCalculatorSample() {
        // tdeecalculator.net's own sample: 119 lb (53.977 kg) at 16% body fat shows BMR 1349.
        val bmr = katchMcArdleBmr(weightKg = 119.0 / 2.20462, bodyFatPct = 16.0)
        assertEquals(1349.0, bmr, 1.0)
    }

    @Test
    fun harrisBenedict_male_isCorrect() {
        // 13.397*80 + 4.799*180 - 5.677*30 + 88.362 = 1853.632
        val bmr = harrisBenedictBmr(Sex.MALE, weightKg = 80.0, heightCm = 180.0, age = 30)
        assertEquals(1853.632, bmr, 0.001)
    }

    @Test
    fun harrisBenedict_female_isCorrect() {
        // 9.247*65 + 3.098*165 - 4.330*28 + 447.593 = 1438.578
        val bmr = harrisBenedictBmr(Sex.FEMALE, weightKg = 65.0, heightCm = 165.0, age = 28)
        assertEquals(1438.578, bmr, 0.001)
    }

    @Test
    fun basalMetabolicRate_usesKatchWhenBodyFatKnown() {
        // With body fat, it must equal Katch-McArdle, NOT Mifflin.
        val bmr = basalMetabolicRate(Sex.MALE, weightKg = 80.0, heightCm = 180.0, age = 30, bodyFatPct = 20.0)
        assertEquals(katchMcArdleBmr(80.0, 20.0), bmr, 0.001)
    }

    @Test
    fun basalMetabolicRate_fallsBackToMifflinWithoutBodyFat() {
        val bmr = basalMetabolicRate(Sex.MALE, weightKg = 80.0, heightCm = 180.0, age = 30, bodyFatPct = null)
        assertEquals(mifflinStJeorBmr(Sex.MALE, 80.0, 180.0, 30), bmr, 0.001)
    }

    @Test
    fun basalMetabolicRate_treatsZeroBodyFatAsUnknown() {
        // 0% is not a real reading; fall back to Mifflin rather than pretending 100% lean.
        val bmr = basalMetabolicRate(Sex.FEMALE, weightKg = 65.0, heightCm = 165.0, age = 28, bodyFatPct = 0.0)
        assertEquals(mifflinStJeorBmr(Sex.FEMALE, 65.0, 165.0, 28), bmr, 0.001)
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