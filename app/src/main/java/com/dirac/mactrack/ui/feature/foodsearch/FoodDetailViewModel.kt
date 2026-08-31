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
import com.dirac.mactrack.data.entity.Goal
import com.dirac.mactrack.data.entity.MealEntry
import com.dirac.mactrack.data.food.FoodDetail
import com.dirac.mactrack.data.food.PortionUnit
import com.dirac.mactrack.data.food.cnfFoodDetail
import com.dirac.mactrack.data.food.foodItemDetail
import com.dirac.mactrack.data.food.stagePortion
import com.dirac.mactrack.data.repository.FoodRepository
import com.dirac.mactrack.data.repository.GoalRepository
import com.dirac.mactrack.data.repository.MealEntryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class FoodDetailViewModel(
    private val foodRepository: FoodRepository,
    private val cnfRepository: CnfRepository,
    private val cartRepository: CartRepository,
    private val mealEntryRepository: MealEntryRepository,
    goalRepository: GoalRepository
) : ViewModel() {

    private val today: String = LocalDate.now().toString()

    private val _detail = MutableStateFlow<FoodDetail?>(null)
    val detail: StateFlow<FoodDetail?> = _detail.asStateFlow()

    val goal: StateFlow<Goal?> = goalRepository.getLatestGoal()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val todayEntries: StateFlow<List<MealEntry>> = mealEntryRepository.getEntriesForDate(today)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun load(source: String, id: String) {
        viewModelScope.launch {
            val d = withContext(Dispatchers.IO) {
                when (source) {
                    "cnf" -> cnfRepository.getFood(id.toIntOrNull() ?: -1)?.let { f ->
                        cnfFoodDetail(f, cnfRepository.measures(f.code))
                    }
                    "custom" -> foodRepository.getFood(id)?.let { foodItemDetail(it) }
                    else -> null
                }
            }
            _detail.value = d
        }
    }

    fun addToCart(amount: Double, unit: PortionUnit) {
        val d = _detail.value ?: return
        val staged = stagePortion(amount, unit)
        cartRepository.add(CartItem(name = d.name, quantity = staged.quantity, unit = staged.unit, nutrients = staged.nutrients))
    }

    fun log(amount: Double, unit: PortionUnit, timeMinutes: Int, onDone: () -> Unit) {
        val d = _detail.value ?: return
        val staged = stagePortion(amount, unit)
        val s = staged.nutrients
        viewModelScope.launch {
            mealEntryRepository.logEntry(
                MealEntry(
                    date = today, timeMinutes = timeMinutes, foodName = d.name,
                    amount = amount, quantity = staged.quantity, unit = staged.unit,
                    calories = s.kcal, proteinG = s.protein, carbG = s.carb, fatG = s.fat,
                    fiberG = s.fiber, sugarG = s.sugar, satFatG = s.satFat,
                    sodiumMg = s.sodium, potassiumMg = s.potassium, cholesterolMg = s.cholesterol
                )
            )
            onDone()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MacTrackApplication
                FoodDetailViewModel(app.foodRepository, app.cnfRepository, app.cartRepository, app.mealEntryRepository, app.goalRepository)
            }
        }
    }
}