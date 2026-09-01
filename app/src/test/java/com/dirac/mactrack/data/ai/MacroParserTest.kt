package com.dirac.mactrack.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// The best-effort macro extractor behind the AI "Log this" action. These lock down the common reply
// shapes; the review dialog handles anything it misses, so exactness isn't required, only the usual cases.
class MacroParserTest {

    @Test
    fun parsesStandardBulletList() {
        val reply = """
            Two large whole eggs have approximately:
            * **Calories:** 140 kcal
            * **Protein:** 12 g
            * **Carbs:** 1 g
            * **Fat:** 10 g
        """.trimIndent()
        val e = MacroParser.parse(reply)
        assertEquals(140.0, e.calories, 0.0)
        assertEquals(12.0, e.protein, 0.0)
        assertEquals(1.0, e.carb, 0.0)
        assertEquals(10.0, e.fat, 0.0)
        assertTrue(e.hasAny)
    }

    @Test
    fun skipsSaturatedFatSoItDoesNotTakeTheFatSlot() {
        val reply = """
            * **Calories:** 200 kcal
            * **Protein:** 20 g
            * **Carbs:** 5 g
            * **Saturated fat:** 3 g
            * **Fat:** 12 g
        """.trimIndent()
        val e = MacroParser.parse(reply)
        assertEquals(200.0, e.calories, 0.0)
        assertEquals(20.0, e.protein, 0.0)
        assertEquals(5.0, e.carb, 0.0)
        // The Fat line wins; the earlier "Saturated fat" line is skipped.
        assertEquals(12.0, e.fat, 0.0)
    }

    @Test
    fun derivesCaloriesFromMacrosWhenNoCalorieLine() {
        val reply = """
            Protein: 12 g
            Carbs: 1 g
            Fat: 10 g
        """.trimIndent()
        val e = MacroParser.parse(reply)
        // 12*4 + 1*4 + 10*9 = 142
        assertEquals(142.0, e.calories, 0.0)
        assertEquals(12.0, e.protein, 0.0)
        assertEquals(1.0, e.carb, 0.0)
        assertEquals(10.0, e.fat, 0.0)
    }

    @Test
    fun handlesDecimalsAndFirstNumberPerLine() {
        val reply = "Calories: 72.5 kcal for 1 large egg\nProtein: 6.3 g"
        val e = MacroParser.parse(reply)
        assertEquals(72.5, e.calories, 0.0001)
        assertEquals(6.3, e.protein, 0.0001)
    }

    @Test
    fun nonNutritionTextParsesToEmpty() {
        val e = MacroParser.parse("I'm not sure what food that is — could you describe it?")
        assertEquals(0.0, e.calories, 0.0)
        assertEquals(0.0, e.protein, 0.0)
        assertFalse(e.hasAny)
    }
}
