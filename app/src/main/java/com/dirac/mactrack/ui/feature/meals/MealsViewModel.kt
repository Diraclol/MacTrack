package com.dirac.mactrack.ui.feature.meals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dirac.mactrack.MacTrackApplication
import com.dirac.mactrack.data.entity.FoodItem
import com.dirac.mactrack.data.entity.MealEntry
import com.dirac.mactrack.data.entity.MealTemplate
import com.dirac.mactrack.data.repository.FoodRepository
import com.dirac.mactrack.data.repository.MealEntryRepository
import com.dirac.mactrack.data.repository.MealTemplateRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

class MealsViewModel(
    private val foodRepository: FoodRepository,
    private val mealTemplateRepository: MealTemplateRepository,
    private val mealEntryRepository: MealEntryRepository
) : ViewModel() {

    private val today: String = LocalDate.now().toString()

    val foods: StateFlow<List<FoodItem>> = foodRepository.getAllFoods()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val templates: StateFlow<List<MealTemplate>> = mealTemplateRepository.getTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveTemplate(name: String, mealType: String?, items: List<Pair<String, Double>>) {
        if (name.isBlank() || items.isEmpty()) return
        viewModelScope.launch { mealTemplateRepository.saveTemplate(name.trim(), mealType, items) }
    }

    fun deleteTemplate(template: MealTemplate) {
        viewModelScope.launch { mealTemplateRepository.deleteTemplate(template) }
    }

    // Log every food in the saved meal at the current time.
    fun logTemplate(template: MealTemplate) {
        viewModelScope.launch {
            val now = LocalTime.now()
            val timeMinutes = now.hour * 60 + now.minute
            val items = mealTemplateRepository.getItems(template.id)
            val foodsById = foods.value.associateBy { it.id }
            items.forEach { item ->
                val food = foodsById[item.foodId] ?: return@forEach
                val a = item.amount
                mealEntryRepository.logEntry(
                    MealEntry(
                        date = today,
                        timeMinutes = timeMinutes,
                        foodName = food.name,
                        amount = a,
                        quantity = a * food.servingSize,
                        unit = food.servingUnit,
                        calories = food.calories * a,
                        proteinG = food.proteinG * a,
                        carbG = food.carbG * a,
                        fatG = food.fatG * a,
                        fiberG = food.fiberG * a,
                        sugarG = food.sugarG * a,
                        satFatG = food.satFatG * a,
                        sodiumMg = food.sodiumMg * a,
                        potassiumMg = food.potassiumMg * a,
                        cholesterolMg = food.cholesterolMg * a,
                        sourceType = "custom",
                        sourceId = food.id,
                        unitLabel = "serving",
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MacTrackApplication
                MealsViewModel(app.foodRepository, app.mealTemplateRepository, app.mealEntryRepository)
            }
        }
    }
}