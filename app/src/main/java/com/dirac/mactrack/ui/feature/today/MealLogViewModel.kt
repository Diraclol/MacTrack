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
import com.dirac.mactrack.data.repository.GoalRepository
import com.dirac.mactrack.data.repository.MealEntryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class MealLogViewModel(
    private val mealEntryRepository: MealEntryRepository,
    private val goalRepository: GoalRepository
) : ViewModel() {

    private val today: String = LocalDate.now().toString()

    val todayEntries: StateFlow<List<MealEntry>> = mealEntryRepository.getEntriesForDate(today)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val goal: StateFlow<Goal?> = goalRepository.getLatestGoal()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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
                MealLogViewModel(app.mealEntryRepository, app.goalRepository)
            }
        }
    }
}