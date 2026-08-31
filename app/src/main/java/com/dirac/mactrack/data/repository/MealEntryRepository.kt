package com.dirac.mactrack.data.repository

import com.dirac.mactrack.data.dao.MealEntryDao
import com.dirac.mactrack.data.entity.MealEntry
import kotlinx.coroutines.flow.Flow

class MealEntryRepository(private val mealEntryDao: MealEntryDao) {
    fun getEntriesForDate(date: String): Flow<List<MealEntry>> = mealEntryDao.getForDate(date)
    fun getLoggedDates(since: String): Flow<List<String>> = mealEntryDao.getLoggedDates(since)
    suspend fun logEntry(entry: MealEntry) = mealEntryDao.insert(entry)
    suspend fun getEntry(id: String): MealEntry? = mealEntryDao.getById(id)
    fun getRecentDistinct(limit: Int): Flow<List<MealEntry>> = mealEntryDao.getRecentDistinct(limit)
    suspend fun deleteEntry(entry: MealEntry) = mealEntryDao.delete(entry)
}