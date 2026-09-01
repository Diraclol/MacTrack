package com.dirac.mactrack.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dirac.mactrack.data.entity.WeightEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: WeightEntry)

    @Query("SELECT * FROM weight_entries ORDER BY date")
    fun getAll(): Flow<List<WeightEntry>>

    @Query("SELECT * FROM weight_entries")
    suspend fun getAllOnce(): List<WeightEntry>

    @Delete
    suspend fun delete(entry: WeightEntry)
}