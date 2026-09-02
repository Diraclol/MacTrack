package com.dirac.mactrack.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
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

    @Query("DELETE FROM weight_entries WHERE date = :date")
    suspend fun deleteByDate(date: String)

    // One weigh-in per calendar day: clear any existing entry for that date, then insert. Backfilling
    // a past date overwrites rather than stacking a second point on the same day.
    @Transaction
    suspend fun replaceForDate(entry: WeightEntry) {
        deleteByDate(entry.date)
        insert(entry)
    }

    @Delete
    suspend fun delete(entry: WeightEntry)
}