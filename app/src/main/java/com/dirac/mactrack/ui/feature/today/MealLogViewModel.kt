package com.dirac.mactrack.ui.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dirac.mactrack.MacTrackApplication
import com.dirac.mactrack.data.entity.Goal
import com.dirac.mactrack.data.entity.MealEntry
import com.dirac.mactrack.data.cnf.CnfRepository
import com.dirac.mactrack.data.food.FoodDetail
import com.dirac.mactrack.data.food.PortionUnit
import com.dirac.mactrack.data.food.entryFoodDetail
import com.dirac.mactrack.data.food.stagePortion
import com.dirac.mactrack.data.repository.FoodRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import com.dirac.mactrack.data.repository.GoalRepository
import com.dirac.mactrack.data.repository.MealEntryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class MealLogViewModel(
    private val mealEntryRepository: MealEntryRepository,
    private val goalRepository: GoalRepository,
    private val cnfRepository: CnfRepository,
    private val foodRepository: FoodRepository
) : ViewModel() {

    private val today: String = LocalDate.now().toString()

    val todayEntries: StateFlow<List<MealEntry>> = mealEntryRepository.getEntriesForDate(today)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val goal: StateFlow<Goal?> = goalRepository.getLatestGoal()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // The full portion list for the entry being edited in the bottom sheet, loaded lazily so
    // the pad can offer every unit, not just the one that was logged. Null until it loads; the
    // sheet uses a synchronous snapshot fallback meanwhile.
    private val _editDetail = MutableStateFlow<FoodDetail?>(null)
    val editDetail: StateFlow<FoodDetail?> = _editDetail.asStateFlow()

    fun loadEditDetail(entry: MealEntry) {
        _editDetail.value = null
        viewModelScope.launch {
            _editDetail.value = withContext(Dispatchers.IO) {
                entryFoodDetail(entry, cnfRepository, foodRepository)
            }
        }
    }

    fun clearEditDetail() {
        _editDetail.value = null
    }

    // Rewrite an entry to a new amount + unit using that unit's per-unit values (same row id).
    fun updateEntry(entry: MealEntry, amount: Double, unit: PortionUnit) {
        if (amount <= 0.0) return
        val staged = stagePortion(amount, unit)
        val s = staged.nutrients
        viewModelScope.launch {
            mealEntryRepository.logEntry(
                entry.copy(
                    amount = amount, quantity = staged.quantity, unit = staged.unit,
                    calories = s.kcal, proteinG = s.protein, carbG = s.carb, fatG = s.fat,
                    fiberG = s.fiber, sugarG = s.sugar, satFatG = s.satFat,
                    sodiumMg = s.sodium, potassiumMg = s.potassium, cholesterolMg = s.cholesterol,
                    unitLabel = unit.label,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteEntry(entry: MealEntry) {
        viewModelScope.launch { mealEntryRepository.deleteEntry(entry) }
    }

    // Rescale an already-logged entry to a new amount (same id = update in place).
    fun updateEntryQuantity(entry: MealEntry, newQuantity: Double) {
        if (entry.quantity <= 0.0 || newQuantity <= 0.0) return
        val factor = newQuantity / entry.quantity
        viewModelScope.launch {
            mealEntryRepository.logEntry(
                entry.copy(
                    amount = entry.amount * factor,
                    quantity = newQuantity,
                    calories = entry.calories * factor,
                    proteinG = entry.proteinG * factor,
                    carbG = entry.carbG * factor,
                    fatG = entry.fatG * factor,
                    fiberG = entry.fiberG * factor,
                    sugarG = entry.sugarG * factor,
                    satFatG = entry.satFatG * factor,
                    sodiumMg = entry.sodiumMg * factor,
                    potassiumMg = entry.potassiumMg * factor,
                    cholesterolMg = entry.cholesterolMg * factor,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MacTrackApplication
                MealLogViewModel(app.mealEntryRepository, app.goalRepository, app.cnfRepository, app.foodRepository)
            }
        }
    }
}