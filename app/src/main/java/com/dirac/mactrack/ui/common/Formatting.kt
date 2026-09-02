package com.dirac.mactrack.ui.common

import java.util.Locale

// Formats a number to exactly one decimal place (e.g. 2.0, 70.3), independent of device locale.
fun oneDecimal(x: Double): String = String.format(Locale.US, "%.1f", x)
