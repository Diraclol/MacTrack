package com.dirac.mactrack.ui.feature.more

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

data class LogStats(val activeStreak: Int, val longestStreak: Int, val totalTracked: Int)

// Food-logging streak stats for the More profile header, derived from the distinct logged
// dates. A day is a query, not a record (per NOTES.md) -- this computes over the dates flow.
class MoreStatsViewModel(mealEntryRepository: MealEntryRepository) : ViewModel() {

    val stats: StateFlow<LogStats> = mealEntryRepository.getLoggedDates("0000-01-01")
        .map { computeStats(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LogStats(0, 0, 0))

    private fun computeStats(dateStrings: List<String>): LogStats {
        val dates = dateStrings
            .mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
            .toSortedSet()
        if (dates.isEmpty()) return LogStats(0, 0, 0)

        var longest = 1
        var run = 1
        var prev: LocalDate? = null
        for (d in dates) {
            if (prev != null) {
                run = if (d == prev.plusDays(1)) run + 1 else 1
            }
            if (run > longest) longest = run
            prev = d
        }

        val today = LocalDate.now()
        var cursor: LocalDate? = when {
            dates.contains(today) -> today
            dates.contains(today.minusDays(1)) -> today.minusDays(1)
            else -> null
        }
        var active = 0
        while (cursor != null && dates.contains(cursor)) {
            active++
            cursor = cursor.minusDays(1)
        }

        return LogStats(active, longest, dates.size)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MacTrackApplication
                MoreStatsViewModel(app.mealEntryRepository)
            }
        }
    }
}
