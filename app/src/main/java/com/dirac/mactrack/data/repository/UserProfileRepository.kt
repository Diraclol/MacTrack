package com.dirac.mactrack.data.repository

import com.dirac.mactrack.data.dao.UserProfileDao
import com.dirac.mactrack.data.entity.UserProfile
import kotlinx.coroutines.flow.Flow

class UserProfileRepository(private val dao: UserProfileDao) {
    fun getProfile(): Flow<UserProfile?> = dao.getProfile()
    suspend fun saveProfile(profile: UserProfile) = dao.upsert(profile)
}