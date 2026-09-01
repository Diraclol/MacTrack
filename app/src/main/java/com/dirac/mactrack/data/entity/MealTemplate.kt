package com.dirac.mactrack.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

// A saved, reusable group of foods (e.g. "Breakfast shake").
@Entity(tableName = "meal_templates")
data class MealTemplate(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    // Optional meal type (Breakfast / Lunch / Dinner / Snack). Nullable, so the migration adds it
    // without a DEFAULT and no @ColumnInfo is needed.
    val mealType: String? = null
)

// One food inside a saved meal, with how much of it.
@Entity(tableName = "meal_template_items")
data class MealTemplateItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val templateId: String,   // which MealTemplate this belongs to
    val foodId: String,       // which FoodItem
    val amount: Double        // number of servings
)