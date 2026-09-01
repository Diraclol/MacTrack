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
import com.dirac.mactrack.data.entity.Recipe
import com.dirac.mactrack.data.food.foodIcon
import com.dirac.mactrack.data.food.recipeDetail
import com.dirac.mactrack.data.repository.FoodRepository
import com.dirac.mactrack.data.repository.MealTemplateRepository
import com.dirac.mactrack.data.repository.RecipeRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// A saved meal plus the macro totals of its foods (one serving of each, times its amount) and the
// icons of those foods (for the clustered meal icon).
data class MealSummary(
    val template: MealTemplate,
    val calories: Double,
    val proteinG: Double,
    val carbG: Double,
    val fatG: Double,
    val icons: List<String>
)

// A saved recipe plus its PER-SERVING macros (total ingredients / makesServings) and the icons of its
// ingredient foods (for the clustered recipe icon).
data class RecipeSummary(
    val recipe: Recipe,
    val calories: Double,
    val proteinG: Double,
    val carbG: Double,
    val fatG: Double,
    val icons: List<String>
)

// Browse-side ViewModel for the Kitchen: saved foods, meals, and recipes, filtered by a query.
class KitchenViewModel(
    private val foodRepository: FoodRepository,
    private val mealTemplateRepository: MealTemplateRepository,
    private val recipeRepository: RecipeRepository
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
                    val icons = mutableListOf<String>()
                    mealTemplateRepository.getItems(tpl.id).forEach { item ->
                        val food = byId[item.foodId] ?: return@forEach
                        kcal += food.calories * item.amount
                        p += food.proteinG * item.amount
                        c += food.carbG * item.amount
                        f += food.fatG * item.amount
                        icons.add(foodIcon(food.emoji, food.name))
                    }
                    MealSummary(tpl, kcal, p, c, f, icons)
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allRecipes: StateFlow<List<Recipe>> = recipeRepository.getRecipes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Recipes with their PER-SERVING macros, computed from the current ingredient foods via
    // recipeDetail (so editing an ingredient food is reflected). Recomputes off the main thread.
    @OptIn(ExperimentalCoroutinesApi::class)
    val recipes: StateFlow<List<RecipeSummary>> =
        combine(_query, allRecipes, allFoods) { q, recipes, foods -> Triple(q, recipes, foods) }
            .mapLatest { (q, recipes, foods) ->
                val byId = foods.associateBy { it.id }
                val filtered = if (q.isBlank()) recipes else recipes.filter { it.name.contains(q, ignoreCase = true) }
                filtered.map { r ->
                    val ings = recipeRepository.getIngredients(r.id)
                    val per = recipeDetail(r, ings, byId).units.firstOrNull()?.per
                    val icons = ings.mapNotNull { byId[it.foodId] }.map { foodIcon(it.emoji, it.name) }
                    RecipeSummary(r, per?.kcal ?: 0.0, per?.protein ?: 0.0, per?.carb ?: 0.0, per?.fat ?: 0.0, icons)
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onQueryChange(q: String) { _query.value = q }

    fun deleteFood(food: FoodItem) {
        viewModelScope.launch { foodRepository.deleteFood(food) }
    }

    fun deleteRecipe(recipe: Recipe) {
        viewModelScope.launch { recipeRepository.deleteRecipe(recipe) }
    }

    fun deleteMeal(template: MealTemplate) {
        viewModelScope.launch { mealTemplateRepository.deleteTemplate(template) }
    }

    fun setEmoji(id: String, emoji: String?) {
        viewModelScope.launch { foodRepository.setEmoji(id, emoji) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MacTrackApplication
                KitchenViewModel(app.foodRepository, app.mealTemplateRepository, app.recipeRepository)
            }
        }
    }
}
