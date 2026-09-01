package com.dirac.mactrack.ui.feature.meals

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
import com.dirac.mactrack.data.repository.FoodRepository
import com.dirac.mactrack.data.repository.MealTemplateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Backs both Create Meal and Edit Meal. A meal is a labeled batch of foods; ingredients are picked on
// the reused food-search screen (which appends to the shared IngredientBuilder). This ViewModel reads
// that draft, lets the screen adjust servings/remove, and saves it as a MealTemplate. When `editId` is
// set (opened from the Kitchen), it loads that meal into the draft and saves as an update.
class MealsViewModel(
    private val foodRepository: FoodRepository,
    private val mealTemplateRepository: MealTemplateRepository,
    private val ingredientBuilder: IngredientBuilderRepository,
    private val editId: String?
) : ViewModel() {

    val isEditing: Boolean = editId != null

    // The name of the meal being edited, for the screen to seed its field once. Null in create mode.
    private val _initialName = MutableStateFlow<String?>(null)
    val initialName: StateFlow<String?> = _initialName.asStateFlow()

    init {
        // Fresh draft when creating; load the existing meal's foods when editing. Returning from the
        // picker keeps the same back-stack entry (and this ViewModel), so this runs only once.
        ingredientBuilder.clear()
        if (editId != null) loadForEdit(editId)
    }

    private fun loadForEdit(id: String) {
        viewModelScope.launch {
            val template = mealTemplateRepository.getTemplate(id) ?: return@launch
            _initialName.value = template.name
            mealTemplateRepository.getItems(id).forEach { item ->
                val name = foodRepository.getFood(item.foodId)?.name ?: "Food"
                ingredientBuilder.set(item.foodId, name, item.amount)
            }
        }
    }

    val foods: StateFlow<List<FoodItem>> = foodRepository.getAllFoods()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ingredients: StateFlow<List<DraftIngredient>> = ingredientBuilder.items
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setServings(foodId: String, servings: Double) = ingredientBuilder.setServings(foodId, servings)

    fun removeIngredient(foodId: String) = ingredientBuilder.remove(foodId)

    fun saveMeal(name: String, items: List<Pair<String, Double>>, onDone: () -> Unit) {
        if (name.isBlank() || items.isEmpty()) return
        viewModelScope.launch {
            if (editId != null) {
                mealTemplateRepository.updateTemplate(editId, name.trim(), items)
            } else {
                mealTemplateRepository.saveTemplate(name.trim(), items)
            }
            ingredientBuilder.clear()
            onDone()
        }
    }

    fun deleteMeal(onDone: () -> Unit) {
        val id = editId ?: return
        viewModelScope.launch {
            mealTemplateRepository.getTemplate(id)?.let { mealTemplateRepository.deleteTemplate(it) }
            ingredientBuilder.clear()
            onDone()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MacTrackApplication
                val editId: String? = createSavedStateHandle()["id"]
                MealsViewModel(app.foodRepository, app.mealTemplateRepository, app.ingredientBuilder, editId)
            }
        }
    }
}
