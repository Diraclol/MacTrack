package com.dirac.mactrack.ui.common

// Soft daily reference intakes for the micronutrient bars — a display scale, not a user goal.
// Shared by the food log (Today), the dashboard nutrient tiles, and the nutrient detail screen so
// every bar fills against the same reference. Common adult daily values (mg, except fibre in g).
object NutrientTargets {
    const val SodiumMg = 2300.0
    const val PotassiumMg = 3400.0
    const val FiberG = 28.0
    const val CaffeineMg = 400.0
}
