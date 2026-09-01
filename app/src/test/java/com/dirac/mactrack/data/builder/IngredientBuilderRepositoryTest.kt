package com.dirac.mactrack.data.builder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// The in-memory draft that backs the Create Meal / Create Recipe ingredient picker. Pure logic on a
// StateFlow, so it is exercised synchronously via `.items.value`.
class IngredientBuilderRepositoryTest {

    @Test
    fun startsEmpty() {
        val repo = IngredientBuilderRepository()
        assertTrue(repo.items.value.isEmpty())
    }

    @Test
    fun addAppendsNewIngredientAtOneServing() {
        val repo = IngredientBuilderRepository()
        repo.add("a", "Apple")
        val items = repo.items.value
        assertEquals(1, items.size)
        assertEquals(DraftIngredient("a", "Apple", 1.0), items[0])
    }

    @Test
    fun addingSameFoodTwiceBumpsServingsAndKeepsFirstName() {
        val repo = IngredientBuilderRepository()
        repo.add("a", "Apple")
        // A second add of the same id should read as two servings, not a duplicate row, and keep
        // the original name.
        repo.add("a", "Apple (again)")
        val items = repo.items.value
        assertEquals(1, items.size)
        assertEquals("a", items[0].foodId)
        assertEquals("Apple", items[0].name)
        assertEquals(2.0, items[0].servings, 0.0)
    }

    @Test
    fun addPreservesInsertionOrder() {
        val repo = IngredientBuilderRepository()
        repo.add("a", "Apple")
        repo.add("b", "Banana")
        repo.add("c", "Cherry")
        assertEquals(listOf("a", "b", "c"), repo.items.value.map { it.foodId })
    }

    @Test
    fun setServingsUpdatesOnlyTheTargetIngredient() {
        val repo = IngredientBuilderRepository()
        repo.add("a", "Apple")
        repo.add("b", "Banana")
        repo.setServings("a", 5.0)
        assertEquals(5.0, repo.items.value.first { it.foodId == "a" }.servings, 0.0)
        assertEquals(1.0, repo.items.value.first { it.foodId == "b" }.servings, 0.0)
    }

    @Test
    fun setServingsOnMissingIdIsANoOp() {
        val repo = IngredientBuilderRepository()
        repo.add("a", "Apple")
        val before = repo.items.value
        repo.setServings("missing", 9.0)
        assertEquals(before, repo.items.value)
    }

    @Test
    fun removeDropsOnlyThatIngredient() {
        val repo = IngredientBuilderRepository()
        repo.add("a", "Apple")
        repo.add("b", "Banana")
        repo.remove("a")
        assertEquals(listOf("b"), repo.items.value.map { it.foodId })
    }

    @Test
    fun clearEmptiesTheDraft() {
        val repo = IngredientBuilderRepository()
        repo.add("a", "Apple")
        repo.add("b", "Banana")
        repo.clear()
        assertTrue(repo.items.value.isEmpty())
    }
}
