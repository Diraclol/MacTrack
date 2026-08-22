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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecipesViewModel(private val foodRepository: FoodRepository) : ViewModel() {

    val foods: StateFlow<List<FoodItem>> = foodRepository.getAllFoods()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ingredients: foodId -> servings of that food used in the whole recipe
    fun saveRecipe(name: String, makesServings: Double, ingredients: Map<String, Double>) {
        if (name.isBlank() || makesServings <= 0.0 || ingredients.isEmpty()) return
        val foodsById = foods.value.associateBy { it.id }

        var cal = 0.0; var p = 0.0; var c = 0.0; var f = 0.0
        var fiber = 0.0; var sugar = 0.0; var sat = 0.0
        var sodium = 0.0; var potassium = 0.0; var chol = 0.0

        ingredients.forEach { (foodId, servings) ->
            val food = foodsById[foodId] ?: return@forEach
            cal += food.calories * servings
            p += food.proteinG * servings
            c += food.carbG * servings
            f += food.fatG * servings
            fiber += food.fiberG * servings
            sugar += food.sugarG * servings
            sat += food.satFatG * servings
            sodium += food.sodiumMg * servings
            potassium += food.potassiumMg * servings
            chol += food.cholesterolMg * servings
        }

        val n = makesServings
        viewModelScope.launch {
            foodRepository.addFood(
                FoodItem(
                    name = name.trim(),
                    calories = cal / n,
                    proteinG = p / n,
                    carbG = c / n,
                    fatG = f / n,
                    fiberG = fiber / n,
                    sugarG = sugar / n,
                    satFatG = sat / n,
                    sodiumMg = sodium / n,
                    potassiumMg = potassium / n,
                    cholesterolMg = chol / n,
                    servingSize = 1.0,
                    servingUnit = "serving"
                )
            )
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MacTrackApplication
                RecipesViewModel(app.foodRepository)
            }
        }
    }
}