package com.dirac.mactrack.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID
import androidx.room.ColumnInfo

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
    @ColumnInfo(defaultValue = "0") val caffeineMg: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    // Where this logged food came from, so it can be reopened, re-logged, and analysed later.
    @ColumnInfo(defaultValue = "unknown")
    val sourceType: String = "unknown",   // cnf | custom | quick | recipe | branded | unknown
    val sourceId: String? = null,         // CNF code, FoodItem id, or barcode
    val unitLabel: String? = null,        // portion the user picked, e.g. "1 slice"
    @ColumnInfo(defaultValue = "0")
    val updatedAt: Long = 0L
)