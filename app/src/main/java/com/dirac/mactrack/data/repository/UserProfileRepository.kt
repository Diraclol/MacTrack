package com.dirac.mactrack.data.repository

import com.dirac.mactrack.data.dao.UserProfileDao
import com.dirac.mactrack.data.entity.UserProfile
import kotlinx.coroutines.flow.Flow

class UserProfileRepository(private val dao: UserProfileDao) {
    fun getProfile(): Flow<UserProfile?> = dao.getProfile()

    // Save the profile, preserving an existing body-fat value when the caller (onboarding /
    // reassess) didn't set one — so recalculating goals never wipes body fat.
    suspend fun saveProfile(profile: UserProfile) {
        val existingBodyFat = dao.getProfileOnce()?.bodyFatPct
        dao.upsert(profile.copy(bodyFatPct = profile.bodyFatPct ?: existingBodyFat))
    }

    suspend fun setBodyFat(pct: Double?) = dao.setBodyFat(pct)
}