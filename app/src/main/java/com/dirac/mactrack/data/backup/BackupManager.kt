package com.dirac.mactrack.data.backup

import com.dirac.mactrack.data.AppDatabase
import com.dirac.mactrack.data.entity.FoodItem
import com.dirac.mactrack.data.entity.Goal
import com.dirac.mactrack.data.entity.MealEntry
import com.dirac.mactrack.data.entity.MealTemplate
import com.dirac.mactrack.data.entity.MealTemplateItem
import com.dirac.mactrack.data.entity.Recipe
import com.dirac.mactrack.data.entity.RecipeIngredient
import com.dirac.mactrack.data.entity.UserProfile
import com.dirac.mactrack.data.entity.WeightEntry
import org.json.JSONArray
import org.json.JSONObject

// A count of what an import wrote, for a confirmation message.
data class ImportSummary(
    val foods: Int, val entries: Int, val weights: Int,
    val templates: Int, val recipes: Int, val hadProfile: Boolean
)

// Whole-database JSON backup/restore using org.json (no extra dependency). Import upserts by id,
// so restoring into an empty DB recreates the data and restoring into a populated one merges.
class BackupManager(private val db: AppDatabase) {

    suspend fun exportJson(): String {
        val root = JSONObject()
        root.put("app", "MacTrack")
        root.put("schemaVersion", 8)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("foods", JSONArray().apply { db.foodItemDao().getAllOnce().forEach { put(it.toJson()) } })
        root.put("goals", JSONArray().apply { db.goalDao().getAllOnce().forEach { put(it.toJson()) } })
        root.put("entries", JSONArray().apply { db.mealEntryDao().getAllOnce().forEach { put(it.toJson()) } })
        root.put("weights", JSONArray().apply { db.weightEntryDao().getAllOnce().forEach { put(it.toJson()) } })
        db.userProfileDao().getProfileOnce()?.let { root.put("profile", it.toJson()) }
        root.put("mealTemplates", JSONArray().apply { db.mealTemplateDao().getAllTemplatesOnce().forEach { put(it.toJson()) } })
        root.put("mealTemplateItems", JSONArray().apply { db.mealTemplateDao().getAllItemsOnce().forEach { put(it.toJson()) } })
        root.put("recipes", JSONArray().apply { db.recipeDao().getAllRecipesOnce().forEach { put(it.toJson()) } })
        root.put("recipeIngredients", JSONArray().apply { db.recipeDao().getAllIngredientsOnce().forEach { put(it.toJson()) } })
        return root.toString(2)
    }

    // Throws org.json.JSONException on malformed input, or IllegalArgumentException if it isn't a
    // MacTrack backup. Callers should catch and surface a friendly message.
    suspend fun importJson(text: String): ImportSummary {
        val root = JSONObject(text)
        require(root.optString("app") == "MacTrack") { "This file isn't a MacTrack backup." }

        var foods = 0; var entries = 0; var weights = 0; var templates = 0; var recipes = 0; var hadProfile = false

        root.optJSONArray("foods")?.let { a -> for (i in 0 until a.length()) { db.foodItemDao().upsert(foodFromJson(a.getJSONObject(i))); foods++ } }
        root.optJSONArray("goals")?.let { a -> for (i in 0 until a.length()) db.goalDao().upsert(goalFromJson(a.getJSONObject(i))) }
        root.optJSONArray("entries")?.let { a -> for (i in 0 until a.length()) { db.mealEntryDao().insert(entryFromJson(a.getJSONObject(i))); entries++ } }
        root.optJSONArray("weights")?.let { a -> for (i in 0 until a.length()) { db.weightEntryDao().insert(weightFromJson(a.getJSONObject(i))); weights++ } }
        if (root.has("profile") && !root.isNull("profile")) { db.userProfileDao().upsert(profileFromJson(root.getJSONObject("profile"))); hadProfile = true }
        root.optJSONArray("mealTemplates")?.let { a -> for (i in 0 until a.length()) { db.mealTemplateDao().insertTemplate(templateFromJson(a.getJSONObject(i))); templates++ } }
        root.optJSONArray("mealTemplateItems")?.let { a -> for (i in 0 until a.length()) db.mealTemplateDao().insertItem(templateItemFromJson(a.getJSONObject(i))) }
        root.optJSONArray("recipes")?.let { a -> for (i in 0 until a.length()) { db.recipeDao().insertRecipe(recipeFromJson(a.getJSONObject(i))); recipes++ } }
        root.optJSONArray("recipeIngredients")?.let { a -> for (i in 0 until a.length()) db.recipeDao().insertIngredient(recipeIngredientFromJson(a.getJSONObject(i))) }

        return ImportSummary(foods, entries, weights, templates, recipes, hadProfile)
    }
}

private fun JSONObject.strOrNull(k: String): String? = if (!has(k) || isNull(k)) null else getString(k)
private fun JSONObject.dblOrNull(k: String): Double? = if (!has(k) || isNull(k)) null else getDouble(k)

private fun FoodItem.toJson() = JSONObject().apply {
    put("id", id); put("name", name); brand?.let { put("brand", it) }
    put("calories", calories); put("proteinG", proteinG); put("carbG", carbG); put("fatG", fatG)
    put("fiberG", fiberG); put("sugarG", sugarG); put("satFatG", satFatG)
    put("sodiumMg", sodiumMg); put("potassiumMg", potassiumMg); put("cholesterolMg", cholesterolMg)
    put("caffeineMg", caffeineMg); put("servingSize", servingSize); put("servingUnit", servingUnit)
    put("favorite", favorite); emoji?.let { put("emoji", it) }; barcode?.let { put("barcode", it) }
}

private fun foodFromJson(o: JSONObject) = FoodItem(
    id = o.getString("id"), name = o.getString("name"), brand = o.strOrNull("brand"),
    calories = o.getDouble("calories"), proteinG = o.getDouble("proteinG"), carbG = o.getDouble("carbG"), fatG = o.getDouble("fatG"),
    fiberG = o.optDouble("fiberG", 0.0), sugarG = o.optDouble("sugarG", 0.0), satFatG = o.optDouble("satFatG", 0.0),
    sodiumMg = o.optDouble("sodiumMg", 0.0), potassiumMg = o.optDouble("potassiumMg", 0.0), cholesterolMg = o.optDouble("cholesterolMg", 0.0),
    caffeineMg = o.optDouble("caffeineMg", 0.0), servingSize = o.optDouble("servingSize", 1.0), servingUnit = o.optString("servingUnit", "serving"),
    favorite = o.optBoolean("favorite", false), emoji = o.strOrNull("emoji"), barcode = o.strOrNull("barcode")
)

private fun Goal.toJson() = JSONObject().apply {
    put("id", id); put("calorieGoal", calorieGoal); put("proteinGoalG", proteinGoalG)
    put("carbGoalG", carbGoalG); put("fatGoalG", fatGoalG); put("source", source); put("createdAt", createdAt)
}

private fun goalFromJson(o: JSONObject) = Goal(
    id = o.getString("id"), calorieGoal = o.getDouble("calorieGoal"), proteinGoalG = o.getDouble("proteinGoalG"),
    carbGoalG = o.getDouble("carbGoalG"), fatGoalG = o.getDouble("fatGoalG"), source = o.optString("source", "custom"),
    createdAt = o.optLong("createdAt", System.currentTimeMillis())
)

private fun MealEntry.toJson() = JSONObject().apply {
    put("id", id); put("date", date); put("timeMinutes", timeMinutes); put("foodName", foodName)
    put("amount", amount); put("quantity", quantity); put("unit", unit)
    put("calories", calories); put("proteinG", proteinG); put("carbG", carbG); put("fatG", fatG)
    put("fiberG", fiberG); put("sugarG", sugarG); put("satFatG", satFatG)
    put("sodiumMg", sodiumMg); put("potassiumMg", potassiumMg); put("cholesterolMg", cholesterolMg); put("caffeineMg", caffeineMg)
    put("createdAt", createdAt); put("sourceType", sourceType); sourceId?.let { put("sourceId", it) }
    unitLabel?.let { put("unitLabel", it) }; put("updatedAt", updatedAt)
}

private fun entryFromJson(o: JSONObject) = MealEntry(
    id = o.getString("id"), date = o.getString("date"), timeMinutes = o.getInt("timeMinutes"), foodName = o.getString("foodName"),
    amount = o.getDouble("amount"), quantity = o.getDouble("quantity"), unit = o.optString("unit", "serving"),
    calories = o.getDouble("calories"), proteinG = o.getDouble("proteinG"), carbG = o.getDouble("carbG"), fatG = o.getDouble("fatG"),
    fiberG = o.optDouble("fiberG", 0.0), sugarG = o.optDouble("sugarG", 0.0), satFatG = o.optDouble("satFatG", 0.0),
    sodiumMg = o.optDouble("sodiumMg", 0.0), potassiumMg = o.optDouble("potassiumMg", 0.0), cholesterolMg = o.optDouble("cholesterolMg", 0.0),
    caffeineMg = o.optDouble("caffeineMg", 0.0), createdAt = o.optLong("createdAt", System.currentTimeMillis()),
    sourceType = o.optString("sourceType", "unknown"), sourceId = o.strOrNull("sourceId"), unitLabel = o.strOrNull("unitLabel"),
    updatedAt = o.optLong("updatedAt", 0L)
)

private fun WeightEntry.toJson() = JSONObject().apply {
    put("id", id); put("date", date); put("weightKg", weightKg); put("createdAt", createdAt)
}

private fun weightFromJson(o: JSONObject) = WeightEntry(
    id = o.getString("id"), date = o.getString("date"), weightKg = o.getDouble("weightKg"),
    createdAt = o.optLong("createdAt", System.currentTimeMillis())
)

private fun UserProfile.toJson() = JSONObject().apply {
    put("id", id); put("sex", sex); put("age", age); put("weightKg", weightKg); put("heightCm", heightCm)
    put("activityLevel", activityLevel); put("goalType", goalType); put("proteinLevel", proteinLevel)
    put("fatLevel", fatLevel); put("updatedAt", updatedAt); bodyFatPct?.let { put("bodyFatPct", it) }
}

private fun profileFromJson(o: JSONObject) = UserProfile(
    id = o.optInt("id", 0), sex = o.getString("sex"), age = o.getInt("age"), weightKg = o.getDouble("weightKg"),
    heightCm = o.getDouble("heightCm"), activityLevel = o.getString("activityLevel"), goalType = o.getString("goalType"),
    proteinLevel = o.getString("proteinLevel"), fatLevel = o.getString("fatLevel"),
    updatedAt = o.optLong("updatedAt", System.currentTimeMillis()), bodyFatPct = o.dblOrNull("bodyFatPct")
)

private fun MealTemplate.toJson() = JSONObject().apply {
    put("id", id); put("name", name); put("createdAt", createdAt); mealType?.let { put("mealType", it) }
}

private fun templateFromJson(o: JSONObject) = MealTemplate(
    id = o.getString("id"), name = o.getString("name"),
    createdAt = o.optLong("createdAt", System.currentTimeMillis()), mealType = o.strOrNull("mealType")
)

private fun MealTemplateItem.toJson() = JSONObject().apply {
    put("id", id); put("templateId", templateId); put("foodId", foodId); put("amount", amount)
}

private fun templateItemFromJson(o: JSONObject) = MealTemplateItem(
    id = o.getString("id"), templateId = o.getString("templateId"), foodId = o.getString("foodId"), amount = o.getDouble("amount")
)

private fun Recipe.toJson() = JSONObject().apply {
    put("id", id); put("name", name); put("makesServings", makesServings)
    cookedWeightG?.let { put("cookedWeightG", it) }; emoji?.let { put("emoji", it) }; put("createdAt", createdAt)
}

private fun recipeFromJson(o: JSONObject) = Recipe(
    id = o.getString("id"), name = o.getString("name"), makesServings = o.optDouble("makesServings", 1.0),
    cookedWeightG = o.dblOrNull("cookedWeightG"), emoji = o.strOrNull("emoji"),
    createdAt = o.optLong("createdAt", System.currentTimeMillis())
)

private fun RecipeIngredient.toJson() = JSONObject().apply {
    put("id", id); put("recipeId", recipeId); put("foodId", foodId); put("amount", amount)
}

private fun recipeIngredientFromJson(o: JSONObject) = RecipeIngredient(
    id = o.getString("id"), recipeId = o.getString("recipeId"), foodId = o.getString("foodId"), amount = o.getDouble("amount")
)
