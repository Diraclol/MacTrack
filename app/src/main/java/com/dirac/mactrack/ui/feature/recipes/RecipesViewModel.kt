package com.dirac.mactrack.ui.feature.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dirac.mactrack.MacTrackApplication
import com.dirac.mactrack.data.cnf.CnfFood
import com.dirac.mactrack.data.entity.FoodItem
import com.dirac.mactrack.data.food.asFoodItem
import com.dirac.mactrack.data.cnf.CnfRepository
import com.dirac.mactrack.data.repository.FoodRepository
import com.dirac.mactrack.data.repository.RecipeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RecipesViewModel(
    private val foodRepository: FoodRepository,
    private val recipeRepository: RecipeRepository,
    private val cnfRepository: CnfRepository
) : ViewModel() {

    // The saved foods that can be used as recipe ingredients.
    val foods: StateFlow<List<FoodItem>> = foodRepository.getAllFoods()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Ingredient search over the whole catalog: custom foods filtered in the picker; Common (CNF)
    // foods searched here. Adding a CNF food imports it into food_items (deduped by code).
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _cnfMatches = MutableStateFlow<List<CnfFood>>(emptyList())
    val cnfMatches: StateFlow<List<CnfFood>> = _cnfMatches.asStateFlow()

    fun onQueryChange(q: String) {
        _query.value = q
        viewModelScope.launch {
            val r = if (q.isBlank()) emptyList() else withContext(Dispatchers.IO) { cnfRepository.search(q) }
            if (_query.value == q) _cnfMatches.value = r
        }
    }

    fun importCnf(cnf: CnfFood) {
        viewModelScope.launch { foodRepository.addFood(cnf.asFoodItem()) }
    }

    // Persist a real recipe: name, how many servings it makes, an optional cooked/finished
    // weight, an optional icon, and the ingredient list (foodId -> servings of that food used
    // in the whole recipe). Macros are computed from the ingredients at display/log time
    // (recipeDetail), so editing an ingredient food is reflected -- no frozen copy here.
    fun saveRecipe(
        name: String,
        makesServings: Double,
        cookedWeightG: Double?,
        emoji: String?,
        ingredients: Map<String, Double>
    ) {
        if (name.isBlank() || makesServings <= 0.0 || ingredients.isEmpty()) return
        viewModelScope.launch {
            recipeRepository.saveRecipe(
                name = name.trim(),
                makesServings = makesServings,
                cookedWeightG = cookedWeightG,
                emoji = emoji,
                ingredients = ingredients.toList()
            )
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MacTrackApplication
                RecipesViewModel(app.foodRepository, app.recipeRepository, app.cnfRepository)
            }
        }
    }
}