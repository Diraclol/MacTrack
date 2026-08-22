package com.dirac.mactrack.ui.feature.foodsearch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dirac.mactrack.MacTrackApplication
import com.dirac.mactrack.data.entity.MealEntry
import com.dirac.mactrack.data.repository.MealEntryRepository
import kotlinx.coroutines.launch
import java.time.LocalDate

class QuickAddViewModel(private val mealEntryRepository: MealEntryRepository) : ViewModel() {

    private val today: String = LocalDate.now().toString()

    fun quickAdd(
        name: String,
        calories: Double,
        protein: Double,
        carb: Double,
        fat: Double,
        timeMinutes: Int,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            mealEntryRepository.logEntry(
                MealEntry(
                    date = today,
                    timeMinutes = timeMinutes,
                    foodName = name.ifBlank { "Quick add" },
                    amount = 1.0,
                    quantity = 1.0,
                    unit = "serving",
                    calories = calories,
                    proteinG = protein,
                    carbG = carb,
                    fatG = fat
                )
            )
            onDone()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MacTrackApplication
                QuickAddViewModel(app.mealEntryRepository)
            }
        }
    }
}