package com.dirac.mactrack

import android.app.Application
import androidx.room.Room
import com.dirac.mactrack.data.AppDatabase
import com.dirac.mactrack.data.repository.FoodRepository

class MacTrackApplication : Application() {
    val database: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "mactrack.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }
    val foodRepository: FoodRepository by lazy {
        FoodRepository(database.foodItemDao())
    }
}