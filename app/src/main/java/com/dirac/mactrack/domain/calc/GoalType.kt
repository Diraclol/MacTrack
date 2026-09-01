package com.dirac.mactrack.domain.calc

enum class GoalType(val label: String, val defaultAdjustment: Double, val advanced: Boolean = false) {
    LOSE("Lose weight", -400.0),
    MAINTAIN("Maintain", 0.0),
    GAIN("Gain weight", 400.0),
    // Advanced options (revealed behind an "Advanced" toggle). Recomp runs a slight deficit;
    // maingain a ~100 cal surplus; lean bulk a ~250 cal surplus.
    RECOMP("Recomp", -250.0, advanced = true),
    MAINGAIN("Maingain", 100.0, advanced = true),
    LEAN_BULK("Lean bulk", 250.0, advanced = true)
}