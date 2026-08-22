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
import com.dirac.mactrack.data.entity.UserProfile
import com.dirac.mactrack.data.entity.WeightEntry
import com.dirac.mactrack.data.repository.GoalRepository
import com.dirac.mactrack.data.repository.MealEntryRepository
import com.dirac.mactrack.data.repository.UserProfileRepository
import com.dirac.mactrack.data.repository.WeightRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

class DashboardViewModel(
    goalRepository: GoalRepository,
    mealEntryRepository: MealEntryRepository,
    weightRepository: WeightRepository,
    userProfileRepository: UserProfileRepository
) : ViewModel() {

    private val today: String = LocalDate.now().toString()

    val goal: StateFlow<Goal?> = goalRepository.getLatestGoal()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val todayEntries: StateFlow<List<MealEntry>> = mealEntryRepository.getEntriesForDate(today)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weights: StateFlow<List<WeightEntry>> = weightRepository.getAllWeights()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val profile: StateFlow<UserProfile?> = userProfileRepository.getProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MacTrackApplication
                DashboardViewModel(
                    app.goalRepository,
                    app.mealEntryRepository,
                    app.weightRepository,
                    app.userProfileRepository
                )
            }
        }
    }
}