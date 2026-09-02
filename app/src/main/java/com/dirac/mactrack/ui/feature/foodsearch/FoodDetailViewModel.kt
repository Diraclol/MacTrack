package com.dirac.mactrack.ui.feature.foodsearch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dirac.mactrack.MacTrackApplication
import com.dirac.mactrack.data.cart.CartItem
import com.dirac.mactrack.data.cart.CartRepository
import com.dirac.mactrack.data.cnf.CnfRepository
import com.dirac.mactrack.data.entity.FoodItem
import com.dirac.mactrack.data.entity.Goal
import com.dirac.mactrack.data.entity.MealEntry
import com.dirac.mactrack.data.food.FoodDetail
import com.dirac.mactrack.data.food.PortionUnit
import com.dirac.mactrack.data.food.cnfFoodDetail
import com.dirac.mactrack.data.food.foodItemDetail
import com.dirac.mactrack.data.food.entryFoodDetail
import com.dirac.mactrack.data.food.mealEntryDetail
import com.dirac.mactrack.data.food.recipeDetail
import com.dirac.mactrack.data.food.stagePortion
import com.dirac.mactrack.data.off.OpenFoodFactsRepository
import com.dirac.mactrack.data.session.LogDateStore
import com.dirac.mactrack.data.repository.FoodRepository
import com.dirac.mactrack.data.repository.GoalRepository
import com.dirac.mactrack.data.repository.MealEntryRepository
import com.dirac.mactrack.data.repository.RecipeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.UUID

class FoodDetailViewModel(
    private val foodRepository: FoodRepository,
    private val cnfRepository: CnfRepository,
    private val openFoodFactsRepository: OpenFoodFactsRepository,
    private val cartRepository: CartRepository,
    private val mealEntryRepository: MealEntryRepository,
    private val recipeRepository: RecipeRepository,
    private val logDateStore: LogDateStore,
    goalRepository: GoalRepository
) : ViewModel() {

    private val today: String = LocalDate.now().toString()

    private var loadedSourceType: String = "unknown"
    private var loadedSourceId: String? = null
    private var loadedEntry: MealEntry? = null

    private val _detail = MutableStateFlow<FoodDetail?>(null)
    val detail: StateFlow<FoodDetail?> = _detail.asStateFlow()

    // False while a load is in flight; true once it finishes (so the screen can tell "loading"
    // apart from "loaded but nothing found" -- e.g. an offline or unknown barcode).
    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    val goal: StateFlow<Goal?> = goalRepository.getLatestGoal()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val todayEntries: StateFlow<List<MealEntry>> = mealEntryRepository.getEntriesForDate(today)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun load(source: String, id: String) {
        loadedSourceType = source
        loadedSourceId = id
        _loaded.value = false
        viewModelScope.launch {
            val d = withContext(Dispatchers.IO) {
                when (source) {
                    "cnf" -> cnfRepository.getFood(id.toIntOrNull() ?: -1)?.let { f ->
                        cnfFoodDetail(f, cnfRepository.measures(f.code))
                    }
                    "custom" -> foodRepository.getFood(id)?.let { foodItemDetail(it) }
                    "recipe" -> recipeRepository.getRecipe(id)?.let { r ->
                        val ings = recipeRepository.getIngredients(r.id)
                        val foodsById = ings.mapNotNull { foodRepository.getFood(it.foodId) }.associateBy { it.id }
                        recipeDetail(r, ings, foodsById)
                    }
                    "branded" -> {
                        // Offline-first: a saved food carrying this barcode wins over an online lookup,
                        // and is logged with correct provenance (custom, not branded).
                        val saved = foodRepository.findByBarcode(id)
                        if (saved != null) {
                            loadedSourceType = "custom"
                            loadedSourceId = saved.id
                            foodItemDetail(saved)
                        } else {
                            openFoodFactsRepository.lookup(id)
                        }
                    }
                    "entry" -> {
                        val e = mealEntryRepository.getEntry(id)
                        loadedEntry = e
                        e?.let { entryDetail(it) }
                    }
                    else -> null
                }
            }
            _detail.value = d
            _loaded.value = true
        }
    }

    fun addToCart(amount: Double, unit: PortionUnit) {
        val d = _detail.value ?: return
        val staged = stagePortion(amount, unit)
        cartRepository.add(CartItem(name = d.name, quantity = staged.quantity, amount = amount, unit = staged.unit, nutrients = staged.nutrients, sourceType = loadedSourceType, sourceId = loadedSourceId, unitLabel = unit.label))
    }

    fun log(amount: Double, unit: PortionUnit, timeMinutes: Int, onDone: () -> Unit) {
        val d = _detail.value ?: return
        val staged = stagePortion(amount, unit)
        val s = staged.nutrients
        viewModelScope.launch {
            mealEntryRepository.logEntry(
                MealEntry(
                    date = logDateStore.current().toString(), timeMinutes = timeMinutes, foodName = d.name,
                    amount = amount, quantity = staged.quantity, unit = staged.unit,
                    calories = s.kcal, proteinG = s.protein, carbG = s.carb, fatG = s.fat,
                    fiberG = s.fiber, sugarG = s.sugar, satFatG = s.satFat,
                    sodiumMg = s.sodium, potassiumMg = s.potassium, cholesterolMg = s.cholesterol,
                    caffeineMg = s.caffeine,
                    sourceType = loadedSourceType, sourceId = loadedSourceId, unitLabel = unit.label,
                    updatedAt = System.currentTimeMillis()
                )
            )
            onDone()
        }
    }

    // Reopen a logged entry: reload its real food (full portion list) when provenance lets
    // us, preselecting the logged unit; otherwise fall back to the frozen snapshot.
    private suspend fun entryDetail(e: MealEntry): FoodDetail =
        entryFoodDetail(e, cnfRepository, foodRepository)

    // Rewrite the same entry row (id preserved) with a new amount/unit. sourceType/sourceId
    // carry over through copy; insert is REPLACE, so this updates in place.
    fun updateEntry(amount: Double, unit: PortionUnit, onDone: () -> Unit) {
        val e = loadedEntry ?: return
        val staged = stagePortion(amount, unit)
        val s = staged.nutrients
        viewModelScope.launch {
            mealEntryRepository.logEntry(
                e.copy(
                    amount = amount, quantity = staged.quantity, unit = staged.unit,
                    calories = s.kcal, proteinG = s.protein, carbG = s.carb, fatG = s.fat,
                    fiberG = s.fiber, sugarG = s.sugar, satFatG = s.satFat,
                    sodiumMg = s.sodium, potassiumMg = s.potassium, cholesterolMg = s.cholesterol,
                    caffeineMg = s.caffeine,
                    unitLabel = unit.label,
                    updatedAt = System.currentTimeMillis()
                )
            )
            onDone()
        }
    }

    // Duplicate the currently shown food into a NEW editable custom food, and hand its id to the
    // caller so it can open the food editor. Any non-recipe source works (Common/CNF, branded, a logged
    // entry incl. an AI estimate, or an existing custom food). The copy captures one default serving of
    // the food (grams when known). A scanned (branded) food carries its barcode onto the copy, so a
    // corrected label can still match future scans.
    fun duplicateAsFood(onReady: (String) -> Unit) {
        val d = _detail.value ?: return
        val u = d.units.find { it.label == d.defaultUnitLabel } ?: d.units.firstOrNull() ?: return
        val amt = if (d.defaultAmount > 0.0) d.defaultAmount else 1.0
        val per = u.per * amt
        val grams = u.grams
        val size = if (grams != null && grams > 0.0) grams * amt else amt
        val unitLabel = if (grams != null && grams > 0.0) "g" else u.label
        val newId = UUID.randomUUID().toString()
        val barcode = if (loadedSourceType == "branded") loadedSourceId else null
        viewModelScope.launch {
            foodRepository.addFood(
                FoodItem(
                    id = newId,
                    name = "${d.name} copy",
                    calories = per.kcal, proteinG = per.protein, carbG = per.carb, fatG = per.fat,
                    fiberG = per.fiber, sugarG = per.sugar, satFatG = per.satFat,
                    sodiumMg = per.sodium, potassiumMg = per.potassium, cholesterolMg = per.cholesterol,
                    caffeineMg = per.caffeine,
                    servingSize = size, servingUnit = unitLabel,
                    barcode = barcode
                )
            )
            onReady(newId)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MacTrackApplication
                FoodDetailViewModel(app.foodRepository, app.cnfRepository, app.openFoodFactsRepository, app.cartRepository, app.mealEntryRepository, app.recipeRepository, app.logDateStore, app.goalRepository)
            }
        }
    }
}