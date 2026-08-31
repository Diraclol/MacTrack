package com.dirac.mactrack.data.food

import com.dirac.mactrack.data.cnf.CnfRepository
import com.dirac.mactrack.data.entity.MealEntry
import com.dirac.mactrack.data.repository.FoodRepository

// Reopen a logged entry as a FoodDetail: reload the real food (its full portion list) from the
// entry's provenance when the logged unit still exists there, else fall back to the frozen
// snapshot's single unit. Shared by the food detail screen (entry mode) and the food-log edit
// sheet so both offer the same units. Runs blocking work; call on Dispatchers.IO.
suspend fun entryFoodDetail(entry: MealEntry, cnf: CnfRepository, foods: FoodRepository): FoodDetail {
    val logged = entry.unitLabel
    val real = when (entry.sourceType) {
        "cnf" -> entry.sourceId?.toIntOrNull()?.let { code ->
            cnf.getFood(code)?.let { f -> cnfFoodDetail(f, cnf.measures(f.code)) }
        }
        "custom" -> entry.sourceId?.let { fid -> foods.getFood(fid)?.let { foodItemDetail(it) } }
        else -> null
    }
    return if (real != null && logged != null && real.units.any { it.label == logged }) {
        real.copy(defaultUnitLabel = logged, defaultAmount = entry.amount)
    } else {
        mealEntryDetail(entry)
    }
}
