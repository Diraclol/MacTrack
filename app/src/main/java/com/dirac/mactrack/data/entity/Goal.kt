package com.dirac.mactrack.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val calorieGoal: Double,
    val proteinGoalG: Double,
    val carbGoalG: Double,
    val fatGoalG: Double,
    val source: String = "CALCULATED",
    val createdAt: Long = System.currentTimeMillis()
)