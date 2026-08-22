package com.dirac.mactrack.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 0,   // single row, always 0
    val sex: String,
    val age: Int,
    val weightKg: Double,
    val heightCm: Double,
    val activityLevel: String,
    val goalType: String,
    val proteinLevel: String,
    val fatLevel: String,
    val updatedAt: Long = System.currentTimeMillis()
)