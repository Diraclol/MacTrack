package com.dirac.mactrack.domain.calc

enum class ProteinLevel(val gramsPerLb: Double) {
    MIN(0.7),
    MODERATE(1.0),
    HIGH(1.2)
}