package com.dirac.mactrack.data.ai.recipe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// servingsFor converts an AI-parsed ingredient amount into "servings of a food", which is what
// RecipeIngredient / MealTemplateItem store. These guard the conversion rules and the honest nulls.
class ServingMathTest {

    private val d = 0.0001

    // A CNF/OFF food imports as a 100 g serving; grams convert by ratio.
    @Test fun gramsAgainst100gServing() {
        assertEquals(1.5, servingsFor(150.0, "g", 100.0, "g")!!, d)
        assertEquals(1.0, servingsFor(100.0, "g", 100.0, "g")!!, d)
        assertEquals(0.5, servingsFor(50.0, "g", 100.0, "g")!!, d)
    }

    // "N servings" is N of the food's own servings, whatever a serving is defined as.
    @Test fun servingWordIsCountDirectly() {
        assertEquals(2.0, servingsFor(2.0, "serving", 100.0, "g")!!, d)
        assertEquals(3.0, servingsFor(3.0, "servings", 1.0, "serving")!!, d)
        // Empty unit is treated as "servings".
        assertEquals(1.0, servingsFor(1.0, "", 250.0, "g")!!, d)
    }

    // Volume against a gram serving uses the ~1 g/ml estimate (cup = 240 ml ~ 240 g).
    @Test fun volumeAgainstGramServing() {
        assertEquals(2.4, servingsFor(1.0, "cup", 100.0, "g")!!, d)       // 240 g / 100 g
        assertEquals(0.147868, servingsFor(1.0, "tbsp", 100.0, "g")!!, d) // 14.7868 g / 100 g
    }

    // Other weight units convert by their gram factors.
    @Test fun weightUnitsConvert() {
        assertEquals(0.283495, servingsFor(1.0, "oz", 100.0, "g")!!, d)
        assertEquals(10.0, servingsFor(1.0, "kg", 100.0, "g")!!, d)       // 1000 g / 100 g
        assertEquals(4.53592, servingsFor(1.0, "lb", 100.0, "g")!!, d)    // 453.592 g / 100 g
    }

    // ml against an ml-based serving.
    @Test fun volumeAgainstVolumeServing() {
        assertEquals(2.5, servingsFor(250.0, "ml", 100.0, "ml")!!, d)
    }

    // Matching piece units divide by pieces-per-serving.
    @Test fun matchingPieceUnits() {
        assertEquals(3.0, servingsFor(3.0, "slice", 1.0, "slice")!!, d)
        assertEquals(1.5, servingsFor(3.0, "slice", 2.0, "slice")!!, d) // serving = 2 slices
    }

    // A weight/volume request against a piece serving (or vice versa) can't be reconciled.
    @Test fun mixedUnitsAreUnresolved() {
        assertNull(servingsFor(2.0, "egg", 100.0, "g"))   // pieces vs grams
        assertNull(servingsFor(1.0, "cup", 1.0, "slice")) // volume vs pieces
        assertNull(servingsFor(2.0, "clove", 1.0, "slice")) // different piece names
    }

    // Guards on non-positive inputs.
    @Test fun nonPositiveInputsAreNull() {
        assertNull(servingsFor(0.0, "g", 100.0, "g"))
        assertNull(servingsFor(-5.0, "g", 100.0, "g"))
        assertNull(servingsFor(100.0, "g", 0.0, "g"))
    }

    // Unit matching is case- and whitespace-insensitive.
    @Test fun unitsAreNormalised() {
        assertEquals(1.5, servingsFor(150.0, " G ", 100.0, "g")!!, d)
        assertEquals(2.0, servingsFor(2.0, "Servings", 100.0, "g")!!, d)
    }
}
