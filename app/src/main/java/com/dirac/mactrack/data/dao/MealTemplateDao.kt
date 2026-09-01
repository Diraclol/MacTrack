package com.dirac.mactrack.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dirac.mactrack.data.entity.MealTemplate
import com.dirac.mactrack.data.entity.MealTemplateItem
import kotlinx.coroutines.flow.Flow

@Dao
interface MealTemplateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: MealTemplate)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: MealTemplateItem)

    @Query("SELECT * FROM meal_templates ORDER BY name")
    fun getTemplates(): Flow<List<MealTemplate>>

    @Query("SELECT * FROM meal_template_items WHERE templateId = :templateId")
    suspend fun getItems(templateId: String): List<MealTemplateItem>

    @Query("SELECT * FROM meal_templates")
    suspend fun getAllTemplatesOnce(): List<MealTemplate>

    @Query("SELECT * FROM meal_template_items")
    suspend fun getAllItemsOnce(): List<MealTemplateItem>

    @Delete
    suspend fun deleteTemplate(template: MealTemplate)

    @Query("DELETE FROM meal_template_items WHERE templateId = :templateId")
    suspend fun deleteItemsFor(templateId: String)
}