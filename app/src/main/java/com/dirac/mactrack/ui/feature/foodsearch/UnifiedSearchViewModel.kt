package com.dirac.mactrack.ui.feature.foodsearch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dirac.mactrack.MacTrackApplication
import com.dirac.mactrack.data.builder.IngredientBuilderRepository
import com.dirac.mactrack.data.cart.CartItem
import com.dirac.mactrack.data.cart.CartRepository
import com.dirac.mactrack.data.cnf.CnfFood
import com.dirac.mactrack.data.cnf.CnfRepository
import com.dirac.mactrack.data.entity.FoodItem
import com.dirac.mactrack.data.entity.MealEntry
import com.dirac.mactrack.data.entity.MealTemplate
import com.dirac.mactrack.data.entity.Recipe
import com.dirac.mactrack.data.food.asFoodItem
import com.dirac.mactrack.data.food.cnfFoodDetail
import com.dirac.mactrack.data.food.foodItemDetail
import com.dirac.mactrack.data.food.stagePortion
import com.dirac.mactrack.data.repository.FoodRepository
import com.dirac.mactrack.data.repository.MealEntryRepository
import com.dirac.mactrack.data.repository.MealTemplateRepository
import com.dirac.mactrack.data.repository.RecipeRepository
import com.dirac.mactrack.data.session.LogDateStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime

class UnifiedSearchViewModel(
    private val foodRepository: FoodRepository,
    private val cnfRepository: CnfRepository,
    private val cartRepository: CartRepository,
    private val mealEntryRepository: MealEntryRepository,
    private val mealTemplateRepository: MealTemplateRepository,
    private val recipeRepository: RecipeRepository,
    private val ingredientBuilder: IngredientBuilderRepository,
    private val logDateStore: LogDateStore
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val allFoods: StateFlow<List<FoodItem>> = foodRepository.getAllFoods()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val custom: StateFlow<List<FoodItem>> = combine(_query, allFoods) { q, foods ->
        if (q.isBlank()) emptyList() else foods.filter { it.name.contains(q, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // The Foods tab browses every saved food (all when the query is blank, filtered when typing),
    // unlike `custom` above which stays empty until you type (it feeds the All tab).
    val savedFoods: StateFlow<List<FoodItem>> = combine(_query, allFoods) { q, foods ->
        if (q.isBlank()) foods else foods.filter { it.name.contains(q, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Favorited (hearted) saved foods, for the Favorites section in the All tab.
    val favorites: StateFlow<List<FoodItem>> = foodRepository.getFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Saved meals (repeatable sets of foods) for the Meals tab.
    val templates: StateFlow<List<MealTemplate>> = mealTemplateRepository.getTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Saved recipes for the Recipes tab; tapping one opens its detail to log by serving.
    val recipes: StateFlow<List<Recipe>> = recipeRepository.getRecipes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _common = MutableStateFlow<List<CnfFood>>(emptyList())
    val common: StateFlow<List<CnfFood>> = _common.asStateFlow()

    val cartCount: StateFlow<Int> = cartRepository.items
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // How many ingredients are in the shared draft, for the picker-mode "Done (N)" button.
    val builderCount: StateFlow<Int> = ingredientBuilder.items
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Recently logged foods that can be reopened (have provenance), for the empty-query view.
    val recent: StateFlow<List<MealEntry>> = mealEntryRepository.getRecentDistinct(20)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onQueryChange(q: String) {
        _query.value = q
        viewModelScope.launch {
            val r = withContext(Dispatchers.IO) { cnfRepository.search(q) }
            if (_query.value == q) _common.value = r
        }
    }

    // Toggle a saved food's heart. Custom foods only (favorite is a food_items column).
    fun toggleFavorite(food: FoodItem) {
        viewModelScope.launch { foodRepository.setFavorite(food.id, !food.favorite) }
    }

    fun addToCart(source: String, id: String) {
        viewModelScope.launch {
            val detail = withContext(Dispatchers.IO) {
                when (source) {
                    "cnf" -> cnfRepository.getFood(id.toIntOrNull() ?: -1)?.let { cnfFoodDetail(it, cnfRepository.measures(it.code)) }
                    "custom" -> foodRepository.getFood(id)?.let { foodItemDetail(it) }
                    else -> null
                }
            } ?: return@launch
            val unit = detail.units.find { it.label == detail.defaultUnitLabel }
                ?: detail.units.firstOrNull() ?: return@launch
            val staged = stagePortion(detail.defaultAmount, unit)
            cartRepository.add(CartItem(name = detail.name, quantity = staged.quantity, amount = detail.defaultAmount, unit = staged.unit, nutrients = staged.nutrients, sourceType = source, sourceId = id, unitLabel = unit.label))
        }
    }

    // Picker mode: add a searched food to the shared meal/recipe draft as one serving. Meal and
    // recipe ingredients reference food_items rows, so a Common (CNF) food is first imported into
    // food_items (idempotent upsert keyed by its deterministic "cnf_<code>" id, same as food search
    // does). Branded/other sources aren't supported as ingredients yet and are ignored.
    fun addIngredient(source: String, id: String, name: String) {
        viewModelScope.launch {
            val foodId = when (source) {
                "custom" -> id
                "cnf" -> {
                    val cnf = withContext(Dispatchers.IO) { cnfRepository.getFood(id.toIntOrNull() ?: -1) }
                        ?: return@launch
                    foodRepository.addFood(cnf.asFoodItem())
                    "cnf_${cnf.code}"
                }
                else -> return@launch
            }
            ingredientBuilder.add(foodId, name)
        }
    }

    // Add every food in a saved meal to the cart (each staged at the meal's recorded servings),
    // so meals flow through the same cart -> Log path as individual foods.
    fun addTemplateToCart(templateId: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val items = mealTemplateRepository.getItems(templateId)
                items.forEach { item ->
                    val food = foodRepository.getFood(item.foodId) ?: return@forEach
                    val detail = foodItemDetail(food)
                    val unit = detail.units.find { it.label == detail.defaultUnitLabel }
                        ?: detail.units.firstOrNull() ?: return@forEach
                    val staged = stagePortion(item.amount, unit)
                    cartRepository.add(
                        CartItem(
                            name = detail.name, quantity = staged.quantity, amount = item.amount,
                            unit = staged.unit, nutrients = staged.nutrients,
                            sourceType = "custom", sourceId = food.id, unitLabel = unit.label
                        )
                    )
                }
            }
        }
    }

    // One-off macro entry logged straight to today (not saved as a reusable food).
    fun quickAdd(name: String, calories: Double, protein: Double, carb: Double, fat: Double, onDone: () -> Unit) {
        viewModelScope.launch {
            val now = LocalTime.now()
            mealEntryRepository.logEntry(
                MealEntry(
                    date = logDateStore.current().toString(), timeMinutes = now.hour * 60 + now.minute,
                    foodName = name.ifBlank { "Quick add" }, amount = 1.0, quantity = 1.0, unit = "serving",
                    calories = calories, proteinG = protein, carbG = carb, fatG = fat,
                    sourceType = "quick", unitLabel = "serving", updatedAt = System.currentTimeMillis()
                )
            )
            onDone()
        }
    }

    fun logCart(onDone: () -> Unit) {
        val items = cartRepository.items.value
        if (items.isEmpty()) return
        viewModelScope.launch {
            val now = LocalTime.now()
            val tm = now.hour * 60 + now.minute
            val date = logDateStore.current().toString()
            items.forEach { ci ->
                mealEntryRepository.logEntry(
                    MealEntry(
                        date = date, timeMinutes = tm, foodName = ci.name,
                        amount = ci.amount, quantity = ci.quantity, unit = ci.unit,
                        calories = ci.nutrients.kcal, proteinG = ci.nutrients.protein, carbG = ci.nutrients.carb, fatG = ci.nutrients.fat,
                        fiberG = ci.nutrients.fiber, sugarG = ci.nutrients.sugar, satFatG = ci.nutrients.satFat,
                        sodiumMg = ci.nutrients.sodium, potassiumMg = ci.nutrients.potassium, cholesterolMg = ci.nutrients.cholesterol,
                        caffeineMg = ci.nutrients.caffeine,
                        sourceType = ci.sourceType, sourceId = ci.sourceId, unitLabel = ci.unitLabel,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
            cartRepository.clear()
            onDone()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MacTrackApplication
                UnifiedSearchViewModel(app.foodRepository, app.cnfRepository, app.cartRepository, app.mealEntryRepository, app.mealTemplateRepository, app.recipeRepository, app.ingredientBuilder, app.logDateStore)
            }
        }
    }
    fun discardCart() {
        cartRepository.clear()
    }
}