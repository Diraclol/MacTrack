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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Browse-side ViewModel for the Kitchen: saved foods and saved meals, filtered by a query.
class KitchenViewModel(
    private val foodRepository: FoodRepository,
    mealTemplateRepository: MealTemplateRepository
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

    val meals: StateFlow<List<MealTemplate>> = combine(_query, allMeals) { q, meals ->
        if (q.isBlank()) meals else meals.filter { it.name.contains(q, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
