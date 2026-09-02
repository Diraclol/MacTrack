package com.dirac.mactrack.ui.feature.dashboard

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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

// Rolling 7-day daily average of cals/macros, plus how many of those days had any log.
data class WeeklyAvg(val cal: Double, val p: Double, val c: Double, val f: Double, val days: Int)

// Rolling 7-day daily average of the tracked micronutrients (same "days with a log" denominator).
data class NutrientAvg(
    val sodiumMg: Double,
    val potassiumMg: Double,
    val fiberG: Double,
    val caffeineMg: Double,
    val days: Int
)

class DashboardViewModel(
    goalRepository: GoalRepository,
    mealEntryRepository: MealEntryRepository
) : ViewModel() {

    private val today: String = LocalDate.now().toString()
    private val since: String = LocalDate.now().minusDays(29).toString()

    val goal: StateFlow<Goal?> = goalRepository.getLatestGoal()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val todayEntries: StateFlow<List<MealEntry>> = mealEntryRepository.getEntriesForDate(today)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val loggedDates: StateFlow<List<String>> = mealEntryRepository.getLoggedDates(since)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Average over the days in the last 7 that actually have a log (not diluted by empty days).
    val weeklyAvg: StateFlow<WeeklyAvg> =
        mealEntryRepository.getDailyTotals(LocalDate.now().minusDays(6).toString())
            .map { daily ->
                val n = daily.size.coerceAtLeast(1)
                WeeklyAvg(
                    cal = daily.sumOf { it.calories } / n,
                    p = daily.sumOf { it.proteinG } / n,
                    c = daily.sumOf { it.carbG } / n,
                    f = daily.sumOf { it.fatG } / n,
                    days = daily.size
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WeeklyAvg(0.0, 0.0, 0.0, 0.0, 0))

    // Micronutrient 7-day average. getDailyTotals only sums cals/macros, so this reads the raw
    // entries since 6 days ago and averages per logged day (a day is a query, not a stored total).
    val weeklyNutrientAvg: StateFlow<NutrientAvg> =
        mealEntryRepository.getEntriesSince(LocalDate.now().minusDays(6).toString())
            .map { entries ->
                val byDay = entries.groupBy { it.date }
                val n = byDay.size.coerceAtLeast(1)
                NutrientAvg(
                    sodiumMg = entries.sumOf { it.sodiumMg } / n,
                    potassiumMg = entries.sumOf { it.potassiumMg } / n,
                    fiberG = entries.sumOf { it.fiberG } / n,
                    caffeineMg = entries.sumOf { it.caffeineMg } / n,
                    days = byDay.size
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NutrientAvg(0.0, 0.0, 0.0, 0.0, 0))

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MacTrackApplication
                DashboardViewModel(app.goalRepository, app.mealEntryRepository)
            }
        }
    }
}