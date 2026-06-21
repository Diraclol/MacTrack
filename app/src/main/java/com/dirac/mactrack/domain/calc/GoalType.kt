package com.dirac.mactrack.domain.calc

enum class GoalType(val label: String, val defaultAdjustment: Double) {
    LOSE("Lose weight", -400.0),
    MAINTAIN("Maintain", 0.0),
    GAIN("Gain weight", 400.0)
}