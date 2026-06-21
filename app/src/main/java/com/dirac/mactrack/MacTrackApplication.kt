package com.dirac.mactrack

import android.app.Application
import androidx.room.Room
import com.dirac.mactrack.data.AppDatabase
import com.dirac.mactrack.data.repository.FoodRepository
import com.dirac.mactrack.data.repository.GoalRepository
import com.dirac.mactrack.data.repository.MealEntryRepository

class MacTrackApplication : Application() {
    val database: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "mactrack.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }
    val foodRepository: FoodRepository by lazy {
        FoodRepository(database.foodItemDao())
    }

    val goalRepository: GoalRepository by lazy {
        GoalRepository(database.goalDao())
    }

    val mealEntryRepository: MealEntryRepository by lazy {
        MealEntryRepository(database.mealEntryDao())
    }

}