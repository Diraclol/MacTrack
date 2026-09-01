package com.dirac.mactrack.ui.feature.meals

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
import com.dirac.mactrack.data.repository.MealTemplateRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Backs the redesigned Create Meal screen. A meal is a labeled batch of foods logged together in one
// tap; ingredients are picked on the reused food-search screen, which appends to the shared
// IngredientBuilder. This ViewModel just reads that draft, lets the screen adjust servings/remove,
// and saves it as a MealTemplate. Cleared on open (init) so each new meal starts empty.
class MealsViewModel(
    private val foodRepository: FoodRepository,
    private val mealTemplateRepository: MealTemplateRepository,
    private val ingredientBuilder: IngredientBuilderRepository
) : ViewModel() {

    init {
        // Fresh draft every time the Create Meal screen is opened. Returning from the picker keeps
        // the same back-stack entry (and this ViewModel), so this does not wipe picked foods.
        ingredientBuilder.clear()
    }

    // All saved foods, so the screen can resolve each draft ingredient's macros for the live totals.
    val foods: StateFlow<List<FoodItem>> = foodRepository.getAllFoods()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // The ingredients picked so far (from the shared builder).
    val ingredients: StateFlow<List<DraftIngredient>> = ingredientBuilder.items
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setServings(foodId: String, servings: Double) = ingredientBuilder.setServings(foodId, servings)

    fun removeIngredient(foodId: String) = ingredientBuilder.remove(foodId)

    // Persist the meal as a template (name + foodId->servings pairs), then clear the draft.
    fun saveMeal(name: String, items: List<Pair<String, Double>>, onDone: () -> Unit) {
        if (name.isBlank() || items.isEmpty()) return
        viewModelScope.launch {
            mealTemplateRepository.saveTemplate(name.trim(), items)
            ingredientBuilder.clear()
            onDone()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MacTrackApplication
                MealsViewModel(app.foodRepository, app.mealTemplateRepository, app.ingredientBuilder)
            }
        }
    }
}
