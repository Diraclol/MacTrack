package com.dirac.mactrack.data.repository

import com.dirac.mactrack.data.dao.WeightEntryDao
import com.dirac.mactrack.data.entity.WeightEntry
import kotlinx.coroutines.flow.Flow

class WeightRepository(private val weightEntryDao: WeightEntryDao) {
    fun getAllWeights(): Flow<List<WeightEntry>> = weightEntryDao.getAll()
    suspend fun logWeight(entry: WeightEntry) = weightEntryDao.insert(entry)
    suspend fun deleteWeight(entry: WeightEntry) = weightEntryDao.delete(entry)
}