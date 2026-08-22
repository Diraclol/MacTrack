package com.dirac.mactrack.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "meal_entries")
data class MealEntry(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val date: String,             // ISO day, e.g. "2026-08-22"
    val timeMinutes: Int,         // minutes since midnight (0..1439); hour = timeMinutes / 60
    val foodName: String,
    val amount: Double,
    val quantity: Double = 0.0,
    val unit: String = "serving",
    // All values below are already scaled by amount (a snapshot at log time).
    val calories: Double,
    val proteinG: Double,
    val carbG: Double,
    val fatG: Double,
    val fiberG: Double = 0.0,
    val sugarG: Double = 0.0,
    val satFatG: Double = 0.0,
    val sodiumMg: Double = 0.0,
    val potassiumMg: Double = 0.0,
    val cholesterolMg: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)