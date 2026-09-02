package com.dirac.mactrack.ui.feature.food

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dirac.mactrack.MacTrackApplication
import com.dirac.mactrack.data.entity.FoodItem
import com.dirac.mactrack.data.repository.FoodRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

// Backs both Create Food and Edit Food. When `editId` is set (opened from the Kitchen), it loads that
// food for the screen to prefill and saves as an update (same id, favorite preserved); otherwise it
// creates a new food.
class FoodViewModel(
    private val repository: FoodRepository,
    private val editId: String?,
    // Prefills the barcode field when creating a food (e.g. from a scanned code OFF didn't recognize).
    val initialBarcode: String? = null
) : ViewModel() {

    val isEditing: Boolean = editId != null

    // The food being edited, for the screen to seed its fields once. Null in create mode.
    private val _editing = MutableStateFlow<FoodItem?>(null)
    val editing: StateFlow<FoodItem?> = _editing.asStateFlow()

    init {
        if (editId != null) viewModelScope.launch { _editing.value = repository.getFood(editId) }
    }

    fun save(
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
        brand: String? = null,
        emoji: String? = null,
        barcode: String? = null
    ) {
        viewModelScope.launch {
            // Editing keeps the same id (addFood upserts by id) and preserves the heart state.
            val id = editId ?: UUID.randomUUID().toString()
            val favorite = _editing.value?.favorite ?: false
            repository.addFood(
                FoodItem(
                    id = id,
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
                    servingUnit = servingUnit,
                    favorite = favorite,
                    emoji = emoji,
                    barcode = barcode
                )
            )
        }
    }

    fun deleteFood(onDone: () -> Unit) {
        val id = editId ?: return
        viewModelScope.launch {
            repository.getFood(id)?.let { repository.deleteFood(it) }
            onDone()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MacTrackApplication
                val handle = createSavedStateHandle()
                val editId: String? = handle["id"]
                val barcodeArg: String? = handle["barcode"]
                FoodViewModel(app.foodRepository, editId, barcodeArg?.takeIf { it.isNotBlank() })
            }
        }
    }
}
