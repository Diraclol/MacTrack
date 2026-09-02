package com.dirac.mactrack.ui.feature.streak

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dirac.mactrack.MacTrackApplication
import com.dirac.mactrack.data.repository.MealEntryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

// Dates that have at least one logged entry, over the last year, as a set for O(1) day lookups on the
// calendar. A day is a query (GROUP BY date) -- there is no per-day record.
class FoodLogCalendarViewModel(mealEntryRepository: MealEntryRepository) : ViewModel() {

    val loggedDates: StateFlow<Set<String>> =
        mealEntryRepository.getLoggedDates(LocalDate.now().minusMonths(12).toString())
            .map { it.toSet() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MacTrackApplication
                FoodLogCalendarViewModel(app.mealEntryRepository)
            }
        }
    }
}
