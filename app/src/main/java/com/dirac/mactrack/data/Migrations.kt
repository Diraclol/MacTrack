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