package com.dirac.mactrack.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dirac.mactrack.data.dao.FoodItemDao
import com.dirac.mactrack.data.dao.GoalDao
import com.dirac.mactrack.data.dao.MealEntryDao
import com.dirac.mactrack.data.entity.Goal
import com.dirac.mactrack.data.entity.FoodItem
import com.dirac.mactrack.data.entity.MealEntry


@Database(entities = [FoodItem::class, Goal::class, MealEntry::class], version = 6, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodItemDao(): FoodItemDao
    abstract fun goalDao(): GoalDao
    abstract fun mealEntryDao(): MealEntryDao
}
