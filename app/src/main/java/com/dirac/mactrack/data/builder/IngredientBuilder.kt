package com.dirac.mactrack.data.builder

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// One draft ingredient collected while building a meal or recipe: a saved-food id (food_items),
// its display name, and how many servings of it the meal/recipe uses.
data class DraftIngredient(
    val foodId: String,
    val name: String,
    val servings: Double = 1.0
)

// The in-memory draft ingredient list shared between a Create screen (Create Meal / Create Recipe)
// and the food-search screen reused as an ingredient picker. When you tap "+" on a Create screen it
// navigates to the search screen in "picker" mode, which appends here; the Create screen reads the
// same list, so picks survive the navigation round-trip. Held in memory only, never persisted --
// same idea as the Cart. A Create screen clears it on open (its ViewModel's init) so each new draft
// starts empty, and again after a successful save.
class IngredientBuilderRepository {
    private val _items = MutableStateFlow<List<DraftIngredient>>(emptyList())
    val items: StateFlow<List<DraftIngredient>> = _items.asStateFlow()

    // Adding a food already in the draft bumps its servings by one, so tapping the same food twice
    // in the picker reads as "two servings" rather than a duplicate row.
    fun add(foodId: String, name: String) {
        val current = _items.value
        val index = current.indexOfFirst { it.foodId == foodId }
        _items.value = if (index >= 0) {
            current.mapIndexed { i, it -> if (i == index) it.copy(servings = it.servings + 1.0) else it }
        } else {
            current + DraftIngredient(foodId, name, 1.0)
        }
    }

    fun setServings(foodId: String, servings: Double) {
        _items.value = _items.value.map { if (it.foodId == foodId) it.copy(servings = servings) else it }
    }

    // Add or replace an ingredient at a specific serving count. Used when loading an existing meal or
    // recipe into the draft for editing.
    fun set(foodId: String, name: String, servings: Double) {
        val current = _items.value
        val index = current.indexOfFirst { it.foodId == foodId }
        _items.value = if (index >= 0) {
            current.mapIndexed { i, it -> if (i == index) it.copy(name = name, servings = servings) else it }
        } else {
            current + DraftIngredient(foodId, name, servings)
        }
    }

    fun remove(foodId: String) {
        _items.value = _items.value.filterNot { it.foodId == foodId }
    }

    fun clear() {
        _items.value = emptyList()
    }
}
