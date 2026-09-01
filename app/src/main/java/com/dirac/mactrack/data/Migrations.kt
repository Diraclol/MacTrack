package com.dirac.mactrack.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// The migration chain. Never bump @Database(version) without adding a link here.
// 1 -> 2: meal_entries learns where each logged food came from.
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE meal_entries ADD COLUMN sourceType TEXT NOT NULL DEFAULT 'unknown'")
        db.execSQL("ALTER TABLE meal_entries ADD COLUMN sourceId TEXT")
        db.execSQL("ALTER TABLE meal_entries ADD COLUMN unitLabel TEXT")
        db.execSQL("ALTER TABLE meal_entries ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
    }
}

// 2 -> 3: food_items can be favorited (hearted). DEFAULT 0 must match FoodItem's
// @ColumnInfo(defaultValue = "0") on the `favorite` field.
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE food_items ADD COLUMN favorite INTEGER NOT NULL DEFAULT 0")
    }
}

// 3 -> 4: caffeine tracking. caffeineMg on both saved foods and logged entries.
// REAL NOT NULL DEFAULT 0 must match the @ColumnInfo(defaultValue = "0") on each field.
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE food_items ADD COLUMN caffeineMg REAL NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE meal_entries ADD COLUMN caffeineMg REAL NOT NULL DEFAULT 0")
    }
}

// 4 -> 5: custom foods can carry a chosen icon and a barcode. Both nullable TEXT, so no
// DEFAULT (and no @ColumnInfo(defaultValue) on FoodItem.emoji / FoodItem.barcode).
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE food_items ADD COLUMN emoji TEXT")
        db.execSQL("ALTER TABLE food_items ADD COLUMN barcode TEXT")
    }
}

// 5 -> 6: recipes get real storage (two new tables). Purely additive — no existing table
// changes. Column types/NOT NULL must match the Recipe / RecipeIngredient entities exactly:
// makesServings and createdAt are non-null with no SQL DEFAULT (Kotlin defaults only);
// cookedWeightG and emoji are nullable.
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `recipes` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                "`makesServings` REAL NOT NULL, `cookedWeightG` REAL, `emoji` TEXT, " +
                "`createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `recipe_ingredients` (`id` TEXT NOT NULL, " +
                "`recipeId` TEXT NOT NULL, `foodId` TEXT NOT NULL, `amount` REAL NOT NULL, " +
                "PRIMARY KEY(`id`))"
        )
    }
}

// 6 -> 7: saved meals can carry a meal type (breakfast/lunch/…). Nullable TEXT, so no DEFAULT
// and no @ColumnInfo(defaultValue) on MealTemplate.mealType.
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE meal_templates ADD COLUMN mealType TEXT")
    }
}

// 7 -> 8: the profile can store an optional body-fat percentage. Nullable REAL, so no DEFAULT
// and no @ColumnInfo(defaultValue) on UserProfile.bodyFatPct.
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE user_profile ADD COLUMN bodyFatPct REAL")
    }
}