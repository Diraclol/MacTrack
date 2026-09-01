package com.dirac.mactrack.ui.feature.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dirac.mactrack.MacTrackApplication
import com.dirac.mactrack.data.builder.DraftIngredient
import com.dirac.mactrack.data.builder.IngredientBuilderRepository
import com.dirac.mactrack.data.entity.FoodItem
import com.dirac.mactrack.data.entity.Recipe
import com.dirac.mactrack.data.repository.FoodRepository
import com.dirac.mactrack.data.repository.RecipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Backs both Create Recipe and Edit Recipe. Ingredients are picked on the reused food-search screen
// (which appends to the shared IngredientBuilder); this ViewModel reads that draft, lets the screen
// adjust servings/remove, and saves a Recipe. When `editId` is set (opened from the Kitchen), it loads
// the existing recipe into the draft and saves as an update. Macros are computed from the ingredients
// at display/log time (recipeDetail), not frozen here.
class RecipesViewModel(
    private val foodRepository: FoodRepository,
    private val recipeRepository: RecipeRepository,
    private val ingredientBuilder: IngredientBuilderRepository,
    private val editId: String?
) : ViewModel() {

    val isEditing: Boolean = editId != null

    // The existing recipe's fields, for the screen to seed its inputs once. Null in create mode.
    private val _initial = MutableStateFlow<Recipe?>(null)
    val initial: StateFlow<Recipe?> = _initial.asStateFlow()

    init {
        ingredientBuilder.clear()
        if (editId != null) loadForEdit(editId)
    }

    private fun loadForEdit(id: String) {
        viewModelScope.launch {
            val recipe = recipeRepository.getRecipe(id) ?: return@launch
            _initial.value = recipe
            recipeRepository.getIngredients(id).forEach { ing ->
                val name = foodRepository.getFood(ing.foodId)?.name ?: "Food"
                ingredientBuilder.set(ing.foodId, name, ing.amount)
            }
        }
    }

    val foods: StateFlow<List<FoodItem>> = foodRepository.getAllFoods()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ingredients: StateFlow<List<DraftIngredient>> = ingredientBuilder.items
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setServings(foodId: String, servings: Double) = ingredientBuilder.setServings(foodId, servings)

    fun removeIngredient(foodId: String) = ingredientBuilder.remove(foodId)

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
            if (editId != null) {
                recipeRepository.updateRecipe(editId, name.trim(), makesServings, cookedWeightG, emoji, ingredients)
            } else {
                recipeRepository.saveRecipe(name.trim(), makesServings, cookedWeightG, emoji, ingredients)
            }
            ingredientBuilder.clear()
            onDone()
        }
    }

    fun deleteRecipe(onDone: () -> Unit) {
        val id = editId ?: return
        viewModelScope.launch {
            recipeRepository.getRecipe(id)?.let { recipeRepository.deleteRecipe(it) }
            ingredientBuilder.clear()
            onDone()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MacTrackApplication
                val editId: String? = createSavedStateHandle()["id"]
                RecipesViewModel(app.foodRepository, app.recipeRepository, app.ingredientBuilder, editId)
            }
        }
    }
}
