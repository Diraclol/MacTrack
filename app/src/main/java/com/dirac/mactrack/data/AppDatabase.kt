package com.dirac.mactrack.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dirac.mactrack.data.dao.FoodItemDao
import com.dirac.mactrack.data.entity.FoodItem

@Database(entities = [FoodItem::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodItemDao(): FoodItemDao
}