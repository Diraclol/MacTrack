package com.dirac.mactrack.data.repository

import com.dirac.mactrack.data.dao.FoodItemDao
import com.dirac.mactrack.data.entity.FoodItem
import kotlinx.coroutines.flow.Flow

class FoodRepository(private val foodItemDao: FoodItemDao) {
    fun getAllFoods(): Flow<List<FoodItem>> = foodItemDao.getAll()
    fun getFavorites(): Flow<List<FoodItem>> = foodItemDao.getFavorites()
    suspend fun getFood(id: String): FoodItem? = foodItemDao.getById(id)
    suspend fun findByBarcode(barcode: String): FoodItem? = foodItemDao.findByBarcode(barcode)
    suspend fun addFood(foodItem: FoodItem) = foodItemDao.upsert(foodItem)
    suspend fun setFavorite(id: String, favorite: Boolean) = foodItemDao.setFavorite(id, favorite)
    suspend fun setEmoji(id: String, emoji: String?) = foodItemDao.setEmoji(id, emoji)
    suspend fun deleteFood(foodItem: FoodItem) = foodItemDao.delete(foodItem)
}