package com.dirac.mactrack.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dirac.mactrack.data.dao.FoodItemDao
import com.dirac.mactrack.data.dao.GoalDao
import com.dirac.mactrack.data.dao.MealEntryDao
import com.dirac.mactrack.data.dao.WeightEntryDao
import com.dirac.mactrack.data.dao.UserProfileDao
import com.dirac.mactrack.data.entity.Goal
import com.dirac.mactrack.data.entity.FoodItem
import com.dirac.mactrack.data.entity.MealEntry
import com.dirac.mactrack.data.entity.WeightEntry
import com.dirac.mactrack.data.entity.UserProfile
import com.dirac.mactrack.data.entity.MealTemplate
import com.dirac.mactrack.data.entity.MealTemplateItem
import com.dirac.mactrack.data.dao.MealTemplateDao
import com.dirac.mactrack.data.entity.Recipe
import com.dirac.mactrack.data.entity.RecipeIngredient
import com.dirac.mactrack.data.dao.RecipeDao

@Database(entities = [FoodItem::class, Goal::class, MealEntry::class, WeightEntry::class, UserProfile::class, MealTemplate::class, MealTemplateItem::class, Recipe::class, RecipeIngredient::class], version = 8, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodItemDao(): FoodItemDao
    abstract fun goalDao(): GoalDao
    abstract fun mealEntryDao(): MealEntryDao
    abstract fun weightEntryDao(): WeightEntryDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun mealTemplateDao(): MealTemplateDao
    abstract fun recipeDao(): RecipeDao
}