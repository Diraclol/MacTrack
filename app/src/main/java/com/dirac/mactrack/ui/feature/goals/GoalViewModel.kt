package com.dirac.mactrack.ui.feature.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dirac.mactrack.MacTrackApplication
import com.dirac.mactrack.data.entity.Goal
import com.dirac.mactrack.data.repository.GoalRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GoalViewModel(private val repository: GoalRepository) : ViewModel() {

    val hasGoal: StateFlow<Boolean?> = repository.getLatestGoal()
        .map { it != null }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val latestGoal: StateFlow<Goal?> = repository.getLatestGoal()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun saveGoal(calories: Double, proteinG: Double, carbG: Double, fatG: Double) {
        viewModelScope.launch {
            repository.saveGoal(
                Goal(
                    calorieGoal = calories,
                    proteinGoalG = proteinG,
                    carbGoalG = carbG,
                    fatGoalG = fatG
                )
            )
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MacTrackApplication
                GoalViewModel(app.goalRepository)
            }
        }
    }
}