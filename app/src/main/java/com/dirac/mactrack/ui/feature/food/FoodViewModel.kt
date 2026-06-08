package com.dirac.mactrack.ui.feature.food

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dirac.mactrack.MacTrackApplication
import com.dirac.mactrack.data.entity.FoodItem
import com.dirac.mactrack.data.repository.FoodRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FoodViewModel(private val repository: FoodRepository) : ViewModel() {

    val foods: StateFlow<List<FoodItem>> = repository.getAllFoods()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addSampleFood() {
        viewModelScope.launch {
            repository.addFood(
                FoodItem(
                    name = "Egg",
                    calories = 78.0,
                    proteinG = 6.0,
                    carbG = 0.6,
                    fatG = 5.0,
                    baseServingAmount = 1.0,
                    baseServingUnit = "egg"
                )
            )
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MacTrackApplication
                FoodViewModel(app.foodRepository)
            }
        }
    }
}