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

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MacTrackApplication
                ThemeViewModel(app.themeRepository)
            }
        }
    }
}