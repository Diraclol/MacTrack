package com.dirac.mactrack.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "food_items")
data class FoodItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val brand: String? = null,
    // All nutrient values below are for ONE serving,
    // where one serving is `servingSize servingUnit` (e.g. 38 g, 1 cup).
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
    val servingSize: Double = 1.0,
    val servingUnit: String = "serving"
)