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
import com.dirac.mactrack.data.cnf.CnfRepository
import com.dirac.mactrack.data.cart.CartRepository
import com.dirac.mactrack.data.off.OpenFoodFactsRepository
import com.dirac.mactrack.data.session.LogDateStore
import com.dirac.mactrack.data.MIGRATION_1_2
import com.dirac.mactrack.data.MIGRATION_2_3
import com.dirac.mactrack.data.MIGRATION_3_4

class MacTrackApplication : Application() {
    val database: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "mactrack.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
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
    val cnfRepository: CnfRepository by lazy {
        CnfRepository(this)
    }

    val cartRepository: CartRepository by lazy {
        CartRepository()
    }

    val openFoodFactsRepository: OpenFoodFactsRepository by lazy {
        OpenFoodFactsRepository()
    }

    val logDateStore: LogDateStore by lazy {
        LogDateStore()
    }

}