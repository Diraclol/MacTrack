package com.dirac.mactrack.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dirac.mactrack.data.entity.Goal
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(goal: Goal)

    @Query("SELECT * FROM goals ORDER BY createdAt DESC LIMIT 1")
    fun getLatest(): Flow<Goal?>

    @Query("SELECT * FROM goals")
    suspend fun getAllOnce(): List<Goal>
}