package com.dirac.mactrack

import android.app.Application
import androidx.room.Room
import com.dirac.mactrack.data.AppDatabase
import com.dirac.mactrack.data.repository.FoodRepository
import com.dirac.mactrack.data.repository.GoalRepository
import com.dirac.mactrack.data.repository.MealEntryRepository
import com.dirac.mactrack.data.repository.WeightRepository
import com.dirac.mactrack.data.repository.UserProfileRepository
import com.dirac.mactrack.data.repository.MealTemplateRepository
import com.dirac.mactrack.data.repository.ThemeRepository

class MacTrackApplication : Application() {
    val database: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "mactrack.db")
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
    val weightRepository: WeightRepository by lazy {
        WeightRepository(database.weightEntryDao())
    }
    val userProfileRepository: UserProfileRepository by lazy {
        UserProfileRepository(database.userProfileDao())
    }
    val mealTemplateRepository: MealTemplateRepository by lazy {
        MealTemplateRepository(database.mealTemplateDao())
    }
    val themeRepository: ThemeRepository by lazy {
        ThemeRepository(this)
    }
}