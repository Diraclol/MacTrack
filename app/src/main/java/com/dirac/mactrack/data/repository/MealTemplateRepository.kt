package com.dirac.mactrack.data.repository

import com.dirac.mactrack.data.dao.MealTemplateDao
import com.dirac.mactrack.data.entity.MealTemplate
import com.dirac.mactrack.data.entity.MealTemplateItem
import kotlinx.coroutines.flow.Flow

class MealTemplateRepository(private val dao: MealTemplateDao) {
    fun getTemplates(): Flow<List<MealTemplate>> = dao.getTemplates()
    suspend fun getItems(templateId: String): List<MealTemplateItem> = dao.getItems(templateId)

    suspend fun saveTemplate(name: String, items: List<Pair<String, Double>>) {
        val template = MealTemplate(name = name)
        dao.insertTemplate(template)
        items.forEach { (foodId, amount) ->
            dao.insertItem(MealTemplateItem(templateId = template.id, foodId = foodId, amount = amount))
        }
    }

    suspend fun deleteTemplate(template: MealTemplate) {
        dao.deleteItemsFor(template.id)
        dao.deleteTemplate(template)
    }
}