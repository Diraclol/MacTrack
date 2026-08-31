package com.dirac.mactrack.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dirac.mactrack.data.entity.MealEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface MealEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: MealEntry)

    @Query("SELECT * FROM meal_entries WHERE date = :date ORDER BY timeMinutes, createdAt")
    fun getForDate(date: String): Flow<List<MealEntry>>

    @Query("SELECT DISTINCT date FROM meal_entries WHERE date >= :since")
    fun getLoggedDates(since: String): Flow<List<String>>

    @Query("SELECT * FROM meal_entries WHERE id = :id")
    suspend fun getById(id: String): MealEntry?

    // The most-recent log of each distinct re-openable food (has provenance), newest first.
    // SQLite returns the MAX(createdAt) row's columns for each group.
    @Query(
        "SELECT * FROM meal_entries WHERE sourceId IS NOT NULL " +
            "GROUP BY sourceType, sourceId ORDER BY MAX(createdAt) DESC LIMIT :limit"
    )
    fun getRecentDistinct(limit: Int): Flow<List<MealEntry>>

    @Delete
    suspend fun delete(entry: MealEntry)
}