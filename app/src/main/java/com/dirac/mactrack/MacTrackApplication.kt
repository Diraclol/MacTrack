package com.dirac.mactrack

import android.app.Application
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dirac.mactrack.data.AppDatabase
import com.dirac.mactrack.data.repository.FoodRepository
import com.dirac.mactrack.data.repository.GoalRepository
import com.dirac.mactrack.data.repository.MealEntryRepository

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE meal_entries ADD COLUMN quantity REAL NOT NULL DEFAULT 0.0")
        db.execSQL("ALTER TABLE meal_entries ADD COLUMN unit TEXT NOT NULL DEFAULT 'serving'")
    }
}

class MacTrackApplication : Application() {
    val database: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "mactrack.db")
            .addMigrations(MIGRATION_5_6)
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