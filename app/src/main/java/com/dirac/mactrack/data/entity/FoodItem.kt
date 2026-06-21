package com.dirac.mactrack.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "food_items")
data class FoodItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val brand: String? = null,
    // The macros below are the values for ONE serving,
    // where one serving is `servingSize servingUnit` (e.g. 38 g, 1 cup).
    val calories: Double,
    val proteinG: Double,
    val carbG: Double,
    val fatG: Double,
    val servingSize: Double = 1.0,
    val servingUnit: String = "serving"
)