package com.dirac.mactrack.ui.feature.trends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dirac.mactrack.MacTrackApplication
import com.dirac.mactrack.data.dao.DailyTotals
import com.dirac.mactrack.data.entity.Goal
import com.dirac.mactrack.data.repository.GoalRepository
import com.dirac.mactrack.data.repository.MealEntryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

enum class TrendPeriod(val label: String, val days: Long?) {
    W1("1W", 7), M1("1M", 30), M3("3M", 90), M6("6M", 180), Y1("1Y", 365), ALL("All", null)
}

enum class TrendMetric(val label: String, val unit: String) {
    CALORIES("Calories", "cal"), PROTEIN("Protein", "g"), CARBS("Carbs", "g"), FAT("Fat", "g")
}

class TrendsViewModel(
    private val mealEntryRepository: MealEntryRepository,
    goalRepository: GoalRepository
) : ViewModel() {

    private val _period = MutableStateFlow(TrendPeriod.M1)
    val period: StateFlow<TrendPeriod> = _period.asStateFlow()
    fun setPeriod(p: TrendPeriod) { _period.value = p }

    private val _metric = MutableStateFlow(TrendMetric.CALORIES)
    val metric: StateFlow<TrendMetric> = _metric.asStateFlow()
    fun setMetric(m: TrendMetric) { _metric.value = m }

    // Daily totals for the selected period, aggregated with GROUP BY date (no totals table).
    @OptIn(ExperimentalCoroutinesApi::class)
    val daily: StateFlow<List<DailyTotals>> = _period.flatMapLatest { p ->
        val since = p.days?.let { LocalDate.now().minusDays(it - 1).toString() } ?: "0000-01-01"
        mealEntryRepository.getDailyTotals(since)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val goal: StateFlow<Goal?> = goalRepository.getLatestGoal()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MacTrackApplication
                TrendsViewModel(app.mealEntryRepository, app.goalRepository)
            }
        }
    }
}
