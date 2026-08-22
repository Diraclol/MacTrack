package com.dirac.mactrack.ui.feature.foodsearch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dirac.mactrack.MacTrackApplication
import com.dirac.mactrack.data.cnf.CnfFood
import com.dirac.mactrack.data.cnf.CnfMeasure
import com.dirac.mactrack.data.cnf.CnfRepository
import com.dirac.mactrack.data.entity.MealEntry
import com.dirac.mactrack.data.repository.MealEntryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class CnfFoodDetailViewModel(
    private val cnfRepository: CnfRepository,
    private val mealEntryRepository: MealEntryRepository
) : ViewModel() {

    private val today: String = LocalDate.now().toString()

    private val _food = MutableStateFlow<CnfFood?>(null)
    val food: StateFlow<CnfFood?> = _food.asStateFlow()

    private val _measures = MutableStateFlow<List<CnfMeasure>>(emptyList())
    val measures: StateFlow<List<CnfMeasure>> = _measures.asStateFlow()

    fun load(code: Int) {
        viewModelScope.launch {
            val f = withContext(Dispatchers.IO) { cnfRepository.getFood(code) }
            val m = withContext(Dispatchers.IO) { cnfRepository.measures(code) }
            _food.value = f
            _measures.value = m
        }
    }

    fun logFood(grams: Double, timeMinutes: Int, onDone: () -> Unit) {
        val f = _food.value ?: return
        val factor = grams / 100.0
        viewModelScope.launch {
            mealEntryRepository.logEntry(
                MealEntry(
                    date = today,
                    timeMinutes = timeMinutes,
                    foodName = f.name,
                    amount = grams,
                    quantity = grams,
                    unit = "g",
                    calories = f.kcal * factor,
                    proteinG = f.protein * factor,
                    carbG = f.carb * factor,
                    fatG = f.fat * factor,
                    fiberG = f.fiber * factor,
                    sugarG = f.sugar * factor,
                    satFatG = f.satFat * factor,
                    sodiumMg = f.sodium * factor,
                    potassiumMg = f.potassium * factor,
                    cholesterolMg = f.cholesterol * factor
                )
            )
            onDone()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MacTrackApplication
                CnfFoodDetailViewModel(app.cnfRepository, app.mealEntryRepository)
            }
        }
    }
}