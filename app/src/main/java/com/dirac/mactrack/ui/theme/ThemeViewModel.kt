package com.dirac.mactrack.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dirac.mactrack.MacTrackApplication
import com.dirac.mactrack.data.repository.ThemeRepository
import kotlinx.coroutines.flow.StateFlow

class ThemeViewModel(private val repository: ThemeRepository) : ViewModel() {
    val mode: StateFlow<ThemeMode> = repository.mode
    fun setMode(mode: ThemeMode) = repository.setMode(mode)

    val startScreen: StateFlow<StartScreen> = repository.startScreen
    fun setStartScreen(startScreen: StartScreen) = repository.setStartScreen(startScreen)

    val avatar: StateFlow<String> = repository.avatar
    fun setAvatar(emoji: String) = repository.setAvatar(emoji)

    val nutrientOrder: StateFlow<List<String>> = repository.nutrientOrder
    fun setNutrientOrder(order: List<String>) = repository.setNutrientOrder(order)

    val reminderEnabled: StateFlow<Boolean> = repository.reminderEnabled
    fun setReminderEnabled(enabled: Boolean) = repository.setReminderEnabled(enabled)

    val aiEnabled: StateFlow<Boolean> = repository.aiEnabled
    fun setAiEnabled(enabled: Boolean) = repository.setAiEnabled(enabled)

    val dashboardWeightGraph: StateFlow<Boolean> = repository.dashboardWeightGraph
    fun setDashboardWeightGraph(enabled: Boolean) = repository.setDashboardWeightGraph(enabled)

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MacTrackApplication
                ThemeViewModel(app.themeRepository)
            }
        }
    }
}