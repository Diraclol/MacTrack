package com.dirac.mactrack.ui.feature.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dirac.mactrack.MacTrackApplication
import com.dirac.mactrack.data.entity.FoodItem
import com.dirac.mactrack.data.repository.FoodRepository
import com.dirac.mactrack.data.repository.RecipeRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecipesViewModel(
    private val foodRepository: FoodRepository,
    private val recipeRepository: RecipeRepository
) : ViewModel() {

    // The saved foods that can be used as recipe ingredients.
    val foods: StateFlow<List<FoodItem>> = foodRepository.getAllFoods()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
                RecipesViewModel(app.foodRepository, app.recipeRepository)
            }
        }
    }
}