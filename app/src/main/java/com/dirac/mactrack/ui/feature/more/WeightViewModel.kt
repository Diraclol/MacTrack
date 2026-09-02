package com.dirac.mactrack.ui.feature.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dirac.mactrack.MacTrackApplication
import com.dirac.mactrack.data.entity.WeightEntry
import com.dirac.mactrack.data.repository.WeightRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class WeightViewModel(private val repository: WeightRepository) : ViewModel() {

    val weights: StateFlow<List<WeightEntry>> = repository.getAllWeights()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // date lets the user backfill a past weigh-in; defaults to today from the dialog.
    fun logWeight(weightKg: Double, date: LocalDate = LocalDate.now()) {
        viewModelScope.launch {
            repository.logWeight(
                WeightEntry(
                    date = date.toString(),
                    weightKg = weightKg
                )
            )
        }
    }

    fun deleteWeight(entry: WeightEntry) {
        viewModelScope.launch {
            repository.deleteWeight(entry)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MacTrackApplication
                WeightViewModel(app.weightRepository)
            }
        }
    }
}