package com.dirac.mactrack.data.repository

import com.dirac.mactrack.data.dao.WeightEntryDao
import com.dirac.mactrack.data.entity.WeightEntry
import kotlinx.coroutines.flow.Flow

class WeightRepository(private val weightEntryDao: WeightEntryDao) {
    fun getAllWeights(): Flow<List<WeightEntry>> = weightEntryDao.getAll()
    // Replace-by-date: one weigh-in per day, so backfilling a past date overwrites cleanly.
    suspend fun logWeight(entry: WeightEntry) = weightEntryDao.replaceForDate(entry)
    suspend fun deleteWeight(entry: WeightEntry) = weightEntryDao.delete(entry)
}