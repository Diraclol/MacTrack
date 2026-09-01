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

    fun addFood(
        name: String,
        calories: Double,
        proteinG: Double,
        carbG: Double,
        fatG: Double,
        fiberG: Double = 0.0,
        sugarG: Double = 0.0,
        satFatG: Double = 0.0,
        sodiumMg: Double = 0.0,
        potassiumMg: Double = 0.0,
        cholesterolMg: Double = 0.0,
        caffeineMg: Double = 0.0,
        servingSize: Double,
        servingUnit: String,
        brand: String? = null
    ) {
        viewModelScope.launch {
            repository.addFood(
                FoodItem(
                    name = name,
                    brand = brand,
                    calories = calories,
                    proteinG = proteinG,
                    carbG = carbG,
                    fatG = fatG,
                    fiberG = fiberG,
                    sugarG = sugarG,
                    satFatG = satFatG,
                    sodiumMg = sodiumMg,
                    potassiumMg = potassiumMg,
                    cholesterolMg = cholesterolMg,
                    caffeineMg = caffeineMg,
                    servingSize = servingSize,
                    servingUnit = servingUnit
                )
            )
        }
    }
    fun deleteFood(foodItem: FoodItem) {
        viewModelScope.launch {
            repository.deleteFood(foodItem)
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