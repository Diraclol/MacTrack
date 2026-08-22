package com.dirac.mactrack.data.repository

import com.dirac.mactrack.data.dao.GoalDao
import com.dirac.mactrack.data.entity.Goal
import kotlinx.coroutines.flow.Flow

class GoalRepository(private val goalDao: GoalDao) {
    fun getLatestGoal(): Flow<Goal?> = goalDao.getLatest()
    suspend fun saveGoal(goal: Goal) = goalDao.upsert(goal)
}