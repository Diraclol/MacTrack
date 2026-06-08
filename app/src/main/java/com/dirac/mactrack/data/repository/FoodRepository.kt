package com.dirac.mactrack.data.repository

import com.dirac.mactrack.data.dao.FoodItemDao
import com.dirac.mactrack.data.entity.FoodItem
import kotlinx.coroutines.flow.Flow

class FoodRepository(private val foodItemDao: FoodItemDao) {
    fun getAllFoods(): Flow<List<FoodItem>> = foodItemDao.getAll()
    suspend fun addFood(foodItem: FoodItem) = foodItemDao.upsert(foodItem)
    suspend fun deleteFood(foodItem: FoodItem) = foodItemDao.delete(foodItem)
}