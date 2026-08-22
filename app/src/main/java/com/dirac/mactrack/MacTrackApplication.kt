package com.dirac.mactrack

import android.app.Application
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dirac.mactrack.data.AppDatabase
import com.dirac.mactrack.data.repository.FoodRepository
import com.dirac.mactrack.data.repository.GoalRepository
import com.dirac.mactrack.data.repository.MealEntryRepository
import com.dirac.mactrack.data.repository.WeightRepository
import com.dirac.mactrack.data.repository.UserProfileRepository
import com.dirac.mactrack.data.repository.MealTemplateRepository
import com.dirac.mactrack.data.repository.ThemeRepository

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

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `weight_entries` (`id` TEXT NOT NULL, `date` TEXT NOT NULL, `weightKg` REAL NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `user_profile` (`id` INTEGER NOT NULL, `sex` TEXT NOT NULL, `age` INTEGER NOT NULL, `weightKg` REAL NOT NULL, `heightCm` REAL NOT NULL, `activityLevel` TEXT NOT NULL, `goalType` TEXT NOT NULL, `proteinLevel` TEXT NOT NULL, `fatLevel` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
    }
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `meal_templates` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `meal_template_items` (`id` TEXT NOT NULL, `templateId` TEXT NOT NULL, `foodId` TEXT NOT NULL, `amount` REAL NOT NULL, PRIMARY KEY(`id`))")
    }
}
class MacTrackApplication : Application() {
    val database: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "mactrack.db")
            .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
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