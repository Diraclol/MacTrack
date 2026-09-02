package com.dirac.mactrack.data.ai.recipe

import org.json.JSONObject

// AI-4 Stage 3. Turns the model's reply into a RecipeBuildRequest. The model is prompted to answer
// with a single JSON object; this pulls that object out (even if it is wrapped in prose or a ```json
// fence) and validates it. Returns null when the reply is not a usable build request, so the caller
// can fall back to treating the reply as an ordinary chat message.
//
// Uses org.json (like OpenFoodFactsRepository), so it is exercised on-device, not in JVM unit tests.
object RecipeRequestParser {

    fun parse(reply: String): RecipeBuildRequest? {
        val jsonText = extractJsonObject(reply) ?: return null
        val obj = runCatching { JSONObject(jsonText) }.getOrNull() ?: return null

        val target = when (obj.optString("target").trim().lowercase()) {
            "recipe" -> BuildTarget.RECIPE
            "meal" -> BuildTarget.MEAL
            else -> return null
        }
        val name = obj.optString("name").trim().ifBlank { return null }

        val arr = obj.optJSONArray("ingredients") ?: return null
        val ingredients = buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val n = o.optString("name").trim()
                if (n.isBlank()) continue
                // quantity may arrive as a number or a numeric string.
                val qty = o.optDouble("quantity", Double.NaN)
                    .let { if (it.isNaN()) o.optString("quantity").trim().toDoubleOrNull() ?: 0.0 else it }
                if (qty <= 0.0) continue
                val unit = o.optString("unit").trim().ifBlank { "serving" }
                add(ParsedIngredient(name = n, quantity = qty, unit = unit))
            }
        }
        if (ingredients.isEmpty()) return null

        return RecipeBuildRequest(target = target, name = name, ingredients = ingredients)
    }

    // The first '{' to the last '}' — enough to lift the object out of a fenced or prose-wrapped reply.
    private fun extractJsonObject(reply: String): String? {
        val start = reply.indexOf('{')
        val end = reply.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return reply.substring(start, end + 1)
    }
}
