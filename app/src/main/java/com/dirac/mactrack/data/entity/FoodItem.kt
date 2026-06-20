package com.dirac.mactrack.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "food_items")
data class FoodItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val brand: String? = null,
    val calories: Double,
    val proteinG: Double,
    val carbG: Double,
    val fatG: Double,
    val baseServingAmount: Double = 100.0,
    val baseServingUnit: String = "g"
)