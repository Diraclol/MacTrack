package com.dirac.mactrack.ui.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dirac.mactrack.MacTrackApplication
import com.dirac.mactrack.data.entity.FoodItem
import com.dirac.mactrack.data.entity.MealTemplate
import com.dirac.mactrack.data.repository.FoodRepository
import com.dirac.mactrack.data.repository.MealTemplateRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// A saved meal plus the macro totals of its foods (one serving of each, times its amount).
data class MealSummary(
    val template: MealTemplate,
    val calories: Double,
    val proteinG: Double,
    val carbG: Double,
    val fatG: Double
)

// Browse-side ViewModel for the Kitchen: saved foods and saved meals, filtered by a query.
class KitchenViewModel(
    private val foodRepository: FoodRepository,
    private val mealTemplateRepository: MealTemplateRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val allFoods: StateFlow<List<FoodItem>> = foodRepository.getAllFoods()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val foods: StateFlow<List<FoodItem>> = combine(_query, allFoods) { q, foods ->
        if (q.isBlank()) foods else foods.filter { it.name.contains(q, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allMeals: StateFlow<List<MealTemplate>> = mealTemplateRepository.getTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Meals with their foods summed. Recomputes when the query, meals, or foods change; loads
    // each meal's items off the main thread via mapLatest (cancels a stale recompute).
    @OptIn(ExperimentalCoroutinesApi::class)
    val meals: StateFlow<List<MealSummary>> =
        combine(_query, allMeals, allFoods) { q, meals, foods -> Triple(q, meals, foods) }
            .mapLatest { (q, meals, foods) ->
                val byId = foods.associateBy { it.id }
                val filtered = if (q.isBlank()) meals else meals.filter { it.name.contains(q, ignoreCase = true) }
                filtered.map { tpl ->
                    var kcal = 0.0; var p = 0.0; var c = 0.0; var f = 0.0
                    mealTemplateRepository.getItems(tpl.id).forEach { item ->
                        val food = byId[item.foodId] ?: return@forEach
                        kcal += food.calories * item.amount
                        p += food.proteinG * item.amount
                        c += food.carbG * item.amount
                        f += food.fatG * item.amount
                    }
                    MealSummary(tpl, kcal, p, c, f)
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onQueryChange(q: String) { _query.value = q }

    fun deleteFood(food: FoodItem) {
        viewModelScope.launch { foodRepository.deleteFood(food) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MacTrackApplication
                KitchenViewModel(app.foodRepository, app.mealTemplateRepository)
            }
        }
    }
}
