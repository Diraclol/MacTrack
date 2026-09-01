package com.dirac.mactrack.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dirac.mactrack.data.entity.FoodItem
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(foodItem: FoodItem)

    @Query("SELECT * FROM food_items ORDER BY name")
    fun getAll(): Flow<List<FoodItem>>

    @Query("SELECT * FROM food_items")
    suspend fun getAllOnce(): List<FoodItem>

    @Query("SELECT * FROM food_items WHERE id = :id")
    suspend fun getById(id: String): FoodItem?

    @Query("SELECT * FROM food_items WHERE favorite = 1 ORDER BY name")
    fun getFavorites(): Flow<List<FoodItem>>

    @Query("UPDATE food_items SET favorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: String, favorite: Boolean)

    @Query("UPDATE food_items SET emoji = :emoji WHERE id = :id")
    suspend fun setEmoji(id: String, emoji: String?)

    @Delete
    suspend fun delete(foodItem: FoodItem)
}