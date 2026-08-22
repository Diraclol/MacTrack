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

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE food_items ADD COLUMN fiberG REAL NOT NULL DEFAULT 0.0")
        db.execSQL("ALTER TABLE food_items ADD COLUMN sugarG REAL NOT NULL DEFAULT 0.0")
        db.execSQL("ALTER TABLE food_items ADD COLUMN satFatG REAL NOT NULL DEFAULT 0.0")
        db.execSQL("ALTER TABLE food_items ADD COLUMN sodiumMg REAL NOT NULL DEFAULT 0.0")
        db.execSQL("ALTER TABLE food_items ADD COLUMN potassiumMg REAL NOT NULL DEFAULT 0.0")
        db.execSQL("ALTER TABLE food_items ADD COLUMN cholesterolMg REAL NOT NULL DEFAULT 0.0")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE meal_entries ADD COLUMN fiberG REAL NOT NULL DEFAULT 0.0")
        db.execSQL("ALTER TABLE meal_entries ADD COLUMN sugarG REAL NOT NULL DEFAULT 0.0")
        db.execSQL("ALTER TABLE meal_entries ADD COLUMN satFatG REAL NOT NULL DEFAULT 0.0")
        db.execSQL("ALTER TABLE meal_entries ADD COLUMN sodiumMg REAL NOT NULL DEFAULT 0.0")
        db.execSQL("ALTER TABLE meal_entries ADD COLUMN potassiumMg REAL NOT NULL DEFAULT 0.0")
        db.execSQL("ALTER TABLE meal_entries ADD COLUMN cholesterolMg REAL NOT NULL DEFAULT 0.0")
    }
}

class MacTrackApplication : Application() {
    val database: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "mactrack.db")
            .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
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