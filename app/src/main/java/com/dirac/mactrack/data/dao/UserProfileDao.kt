package com.dirac.mactrack.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dirac.mactrack.data.entity.UserProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: UserProfile)

    @Query("SELECT * FROM user_profile WHERE id = 0")
    fun getProfile(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 0")
    suspend fun getProfileOnce(): UserProfile?

    @Query("UPDATE user_profile SET bodyFatPct = :pct WHERE id = 0")
    suspend fun setBodyFat(pct: Double?)
}