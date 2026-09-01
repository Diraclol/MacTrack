package com.dirac.mactrack.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

// A saved recipe: a named set of foods that makes N servings, optionally with a known
// finished ("cooked") weight so it can be logged by grams of the batch as well as by serving.
// Logging a recipe writes ONE meal_entry with its per-serving macros (sourceType = "recipe").
@Entity(tableName = "recipes")
data class Recipe(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val makesServings: Double = 1.0,
    val cookedWeightG: Double? = null,
    val emoji: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

// One food inside a recipe, with how many servings of that food the whole recipe uses.
@Entity(tableName = "recipe_ingredients")
data class RecipeIngredient(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val recipeId: String,   // which Recipe this belongs to
    val foodId: String,     // which FoodItem
    val amount: Double      // number of servings of that food
)
