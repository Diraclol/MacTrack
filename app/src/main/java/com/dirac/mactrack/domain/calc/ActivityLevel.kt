package com.dirac.mactrack.domain.calc

enum class ActivityLevel(val multiplier: Double) {
    SEDENTARY(1.2),
    LIGHT(1.375),
    MODERATE(1.55),
    HEAVY(1.725),
    ATHLETE(1.9)
}