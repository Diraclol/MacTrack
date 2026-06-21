package com.dirac.mactrack.ui.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dirac.mactrack.MacTrackApplication
import com.dirac.mactrack.data.entity.FoodItem
import com.dirac.mactrack.data.entity.Goal
import com.dirac.mactrack.data.entity.MealEntry
import com.dirac.mactrack.data.repository.FoodRepository
import com.dirac.mactrack.data.repository.GoalRepository
import com.dirac.mactrack.data.repository.MealEntryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class MealLogViewModel(
    private val foodRepository: FoodRepository,
    private val mealEntryRepository: MealEntryRepository,
    private val goalRepository: GoalRepository
) : ViewModel() {

    private val today: String = LocalDate.now().toString()

    val foods: StateFlow<List<FoodItem>> = foodRepository.getAllFoods()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayEntries: StateFlow<List<MealEntry>> = mealEntryRepository.getEntriesForDate(today)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val goal: StateFlow<Goal?> = goalRepository.getLatestGoal()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun logFood(food: FoodItem, mealLabel: String, amount: Double) {
        viewModelScope.launch {
            mealEntryRepository.logEntry(
                MealEntry(
                    date = today,
                    mealLabel = mealLabel,
                    foodName = food.name,
                    amount = amount,
                    calories = food.calories * amount,
                    proteinG = food.proteinG * amount,
                    carbG = food.carbG * amount,
                    fatG = food.fatG * amount
                )
            )
        }
    }

    fun deleteEntry(entry: MealEntry) {
        viewModelScope.launch {
            mealEntryRepository.deleteEntry(entry)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MacTrackApplication
                MealLogViewModel(app.foodRepository, app.mealEntryRepository, app.goalRepository)
            }
        }
    }
}