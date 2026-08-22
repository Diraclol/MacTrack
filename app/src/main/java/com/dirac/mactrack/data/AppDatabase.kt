package com.dirac.mactrack.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dirac.mactrack.data.dao.FoodItemDao
import com.dirac.mactrack.data.dao.GoalDao
import com.dirac.mactrack.data.dao.MealEntryDao
import com.dirac.mactrack.data.dao.WeightEntryDao
import com.dirac.mactrack.data.entity.Goal
import com.dirac.mactrack.data.entity.FoodItem
import com.dirac.mactrack.data.entity.MealEntry
import com.dirac.mactrack.data.entity.WeightEntry

@Database(entities = [FoodItem::class, Goal::class, MealEntry::class, WeightEntry::class], version = 9, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodItemDao(): FoodItemDao
    abstract fun goalDao(): GoalDao
    abstract fun mealEntryDao(): MealEntryDao
    abstract fun weightEntryDao(): WeightEntryDao
}