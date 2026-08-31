package com.dirac.mactrack.data.cart

import com.dirac.mactrack.data.food.Nutrients
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class CartItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val quantity: Double,
    val amount: Double,
    val unit: String,
    val nutrients: Nutrients,
    val sourceType: String = "unknown",
    val sourceId: String? = null,
    val unitLabel: String? = null
)

class CartRepository {
    private val _items = MutableStateFlow<List<CartItem>>(emptyList())
    val items: StateFlow<List<CartItem>> = _items.asStateFlow()

    fun add(item: CartItem) { _items.value = _items.value + item }
    fun remove(id: String) { _items.value = _items.value.filterNot { it.id == id } }
    fun clear() { _items.value = emptyList() }
}