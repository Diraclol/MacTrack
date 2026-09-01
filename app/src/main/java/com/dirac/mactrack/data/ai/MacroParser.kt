package com.dirac.mactrack.data.ai

// Best-effort extraction of a nutrition estimate from an assistant reply, so a "Log this" action can
// prefill a review form. The model almost always lists one macro per line ("* **Protein:** 12 g"), so
// this reads line by line and grabs the first number on each labelled line. Imperfect by design -- the
// review dialog lets the user correct anything before it's logged.
object MacroParser {

    data class Estimate(
        val calories: Double,
        val protein: Double,
        val carb: Double,
        val fat: Double
    ) {
        val hasAny: Boolean get() = calories > 0.0 || protein > 0.0 || carb > 0.0 || fat > 0.0
    }

    private val NUMBER = Regex("(\\d+(?:\\.\\d+)?)")

    fun parse(text: String): Estimate {
        var cal = 0.0; var p = 0.0; var c = 0.0; var f = 0.0
        text.lineSequence().forEach { raw ->
            val line = raw.lowercase()
            val num = NUMBER.find(line)?.value?.toDoubleOrNull() ?: return@forEach
            when {
                (line.contains("calorie") || line.contains("kcal")) && cal == 0.0 -> cal = num
                line.contains("protein") && p == 0.0 -> p = num
                line.contains("carb") && c == 0.0 -> c = num
                line.contains("saturated") -> Unit // skip "saturated fat" so it doesn't take the fat slot
                line.contains("fat") && f == 0.0 -> f = num
            }
        }
        // If the model gave macros but no explicit calorie line, derive it (4/4/9).
        if (cal == 0.0 && (p > 0.0 || c > 0.0 || f > 0.0)) cal = p * 4 + c * 4 + f * 9
        return Estimate(cal, p, c, f)
    }
}
