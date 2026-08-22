package com.dirac.mactrack.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "weight_entries")
data class WeightEntry(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val date: String,          // ISO date, e.g. "2026-08-22"
    val weightKg: Double,
    val createdAt: Long = System.currentTimeMillis()
)