package com.dirac.mactrack.ui.feature.nutrient

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dirac.mactrack.MacTrackApplication
import com.dirac.mactrack.data.entity.MealEntry
import com.dirac.mactrack.data.repository.MealEntryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

// The last year of logged entries; the screen slices them per selected nutrient and time period.
class NutrientDetailViewModel(mealEntryRepository: MealEntryRepository) : ViewModel() {

    val entries: StateFlow<List<MealEntry>> =
        mealEntryRepository.getEntriesSince(LocalDate.now().minusDays(364).toString())
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MacTrackApplication
                NutrientDetailViewModel(app.mealEntryRepository)
            }
        }
    }
}
