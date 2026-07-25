package com.dirac.mactrack.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "meal_entries")
data class MealEntry(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val date: String,
    val mealLabel: String,        // "M1", "M2", "M3", "Supplements"
    val foodName: String,
    val amount: Double,           // number of servings
    val quantity: Double = 0.0,
    val unit: String = "serving",
    val calories: Double,         // already scaled by amount
    val proteinG: Double,
    val carbG: Double,
    val fatG: Double,
    val createdAt: Long = System.currentTimeMillis()
)