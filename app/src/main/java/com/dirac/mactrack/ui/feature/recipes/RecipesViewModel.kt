package com.dirac.mactrack.ui.feature.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dirac.mactrack.MacTrackApplication
import com.dirac.mactrack.data.builder.DraftIngredient
import com.dirac.mactrack.data.builder.IngredientBuilderRepository
import com.dirac.mactrack.data.entity.FoodItem
import com.dirac.mactrack.data.repository.FoodRepository
import com.dirac.mactrack.data.repository.RecipeRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Backs the redesigned Create Recipe screen. Ingredients are picked on the reused food-search screen
// (which appends to the shared IngredientBuilder); this ViewModel reads that draft, lets the screen
// adjust servings/remove, and saves a Recipe (name, makesServings, optional cooked weight). Macros
// are computed from the ingredients at display/log time (recipeDetail), not frozen here. Cleared on
// open (init) so each new recipe starts empty.
class RecipesViewModel(
    private val foodRepository: FoodRepository,
    private val recipeRepository: RecipeRepository,
    private val ingredientBuilder: IngredientBuilderRepository
) : ViewModel() {

    init {
        ingredientBuilder.clear()
    }

    val foods: StateFlow<List<FoodItem>> = foodRepository.getAllFoods()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ingredients: StateFlow<List<DraftIngredient>> = ingredientBuilder.items
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setServings(foodId: String, servings: Double) = ingredientBuilder.setServings(foodId, servings)

    fun removeIngredient(foodId: String) = ingredientBuilder.remove(foodId)

    // Persist a real recipe: name, how many servings it makes, an optional cooked/finished weight,
    // and the ingredient list (foodId -> servings of that food used in the whole recipe), then clear
    // the draft.
    fun saveRecipe(
        name: String,
        makesServings: Double,
        cookedWeightG: Double?,
        emoji: String?,
        ingredients: List<Pair<String, Double>>,
        onDone: () -> Unit
    ) {
        if (name.isBlank() || makesServings <= 0.0 || ingredients.isEmpty()) return
        viewModelScope.launch {
            recipeRepository.saveRecipe(
                name = name.trim(),
                makesServings = makesServings,
                cookedWeightG = cookedWeightG,
                emoji = emoji,
                ingredients = ingredients
            )
            ingredientBuilder.clear()
            onDone()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MacTrackApplication
                RecipesViewModel(app.foodRepository, app.recipeRepository, app.ingredientBuilder)
            }
        }
    }
}
