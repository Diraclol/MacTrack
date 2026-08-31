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
import com.dirac.mactrack.data.cnf.CnfFood
import com.dirac.mactrack.data.cnf.CnfRepository
import com.dirac.mactrack.data.entity.FoodItem
import com.dirac.mactrack.data.entity.MealEntry
import com.dirac.mactrack.data.food.cnfFoodDetail
import com.dirac.mactrack.data.food.foodItemDetail
import com.dirac.mactrack.data.food.stagePortion
import com.dirac.mactrack.data.repository.FoodRepository
import com.dirac.mactrack.data.repository.MealEntryRepository
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
    private val mealEntryRepository: MealEntryRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val allFoods: StateFlow<List<FoodItem>> = foodRepository.getAllFoods()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val custom: StateFlow<List<FoodItem>> = combine(_query, allFoods) { q, foods ->
        if (q.isBlank()) emptyList() else foods.filter { it.name.contains(q, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _common = MutableStateFlow<List<CnfFood>>(emptyList())
    val common: StateFlow<List<CnfFood>> = _common.asStateFlow()

    val cartCount: StateFlow<Int> = cartRepository.items
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun onQueryChange(q: String) {
        _query.value = q
        viewModelScope.launch {
            val r = withContext(Dispatchers.IO) { cnfRepository.search(q) }
            if (_query.value == q) _common.value = r
        }
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

    fun logCart(onDone: () -> Unit) {
        val items = cartRepository.items.value
        if (items.isEmpty()) return
        viewModelScope.launch {
            val now = LocalTime.now()
            val tm = now.hour * 60 + now.minute
            val today = LocalDate.now().toString()
            items.forEach { ci ->
                mealEntryRepository.logEntry(
                    MealEntry(
                        date = today, timeMinutes = tm, foodName = ci.name,
                        amount = ci.amount, quantity = ci.quantity, unit = ci.unit,
                        calories = ci.nutrients.kcal, proteinG = ci.nutrients.protein, carbG = ci.nutrients.carb, fatG = ci.nutrients.fat,
                        fiberG = ci.nutrients.fiber, sugarG = ci.nutrients.sugar, satFatG = ci.nutrients.satFat,
                        sodiumMg = ci.nutrients.sodium, potassiumMg = ci.nutrients.potassium, cholesterolMg = ci.nutrients.cholesterol,
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
                UnifiedSearchViewModel(app.foodRepository, app.cnfRepository, app.cartRepository, app.mealEntryRepository)
            }
        }
    }
    fun discardCart() {
        cartRepository.clear()
    }
}