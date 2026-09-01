package com.dirac.mactrack.data.food

import org.junit.Assert.assertEquals
import org.junit.Test

// The Nutrients value type is the unit of all macro/micro math (scaling a portion, summing a
// recipe). These guard its arithmetic, including the defaulted caffeine field.
class NutrientsTest {

    private fun sample(scale: Double = 1.0) = Nutrients(
        kcal = 100.0 * scale, protein = 10.0 * scale, carb = 20.0 * scale, fat = 5.0 * scale,
        fiber = 3.0 * scale, sugar = 8.0 * scale, satFat = 2.0 * scale,
        sodium = 150.0 * scale, potassium = 200.0 * scale, cholesterol = 30.0 * scale,
        caffeine = 40.0 * scale
    )

    @Test
    fun zeroIsAllZeros() {
        val z = Nutrients.ZERO
        assertEquals(0.0, z.kcal, 0.0)
        assertEquals(0.0, z.protein, 0.0)
        assertEquals(0.0, z.carb, 0.0)
        assertEquals(0.0, z.fat, 0.0)
        assertEquals(0.0, z.caffeine, 0.0)
    }

    @Test
    fun timesScalesEveryField() {
        val scaled = sample() * 2.0
        assertEquals(200.0, scaled.kcal, 0.0001)
        assertEquals(20.0, scaled.protein, 0.0001)
        assertEquals(40.0, scaled.carb, 0.0001)
        assertEquals(10.0, scaled.fat, 0.0001)
        assertEquals(6.0, scaled.fiber, 0.0001)
        assertEquals(16.0, scaled.sugar, 0.0001)
        assertEquals(4.0, scaled.satFat, 0.0001)
        assertEquals(300.0, scaled.sodium, 0.0001)
        assertEquals(400.0, scaled.potassium, 0.0001)
        assertEquals(60.0, scaled.cholesterol, 0.0001)
        assertEquals(80.0, scaled.caffeine, 0.0001)
    }

    @Test
    fun timesByZeroGivesZero() {
        val scaled = sample() * 0.0
        assertEquals(0.0, scaled.kcal, 0.0)
        assertEquals(0.0, scaled.caffeine, 0.0)
    }

    @Test
    fun plusAddsFieldwise() {
        val sum = sample() + sample(2.0)
        // 1x + 2x = 3x of each field.
        assertEquals(300.0, sum.kcal, 0.0001)
        assertEquals(30.0, sum.protein, 0.0001)
        assertEquals(60.0, sum.carb, 0.0001)
        assertEquals(15.0, sum.fat, 0.0001)
        assertEquals(120.0, sum.caffeine, 0.0001)
    }

    @Test
    fun plusZeroIsIdentity() {
        val n = sample()
        val sum = n + Nutrients.ZERO
        assertEquals(n, sum)
    }
}
