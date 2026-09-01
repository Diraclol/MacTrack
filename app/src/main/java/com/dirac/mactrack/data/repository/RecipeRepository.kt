package com.dirac.mactrack.data.repository

import com.dirac.mactrack.data.dao.RecipeDao
import com.dirac.mactrack.data.entity.Recipe
import com.dirac.mactrack.data.entity.RecipeIngredient
import kotlinx.coroutines.flow.Flow

class RecipeRepository(private val dao: RecipeDao) {
    fun getRecipes(): Flow<List<Recipe>> = dao.getRecipes()
    suspend fun getRecipe(id: String): Recipe? = dao.getRecipe(id)
    suspend fun getIngredients(recipeId: String): List<RecipeIngredient> = dao.getIngredients(recipeId)

    // Save a recipe and its ingredient list. `ingredients` is (foodId -> servings of that food).
    suspend fun saveRecipe(
        name: String,
        makesServings: Double,
        cookedWeightG: Double?,
        emoji: String?,
        ingredients: List<Pair<String, Double>>
    ): String {
        val recipe = Recipe(
            name = name,
            makesServings = makesServings,
            cookedWeightG = cookedWeightG,
            emoji = emoji
        )
        dao.insertRecipe(recipe)
        ingredients.forEach { (foodId, amount) ->
            dao.insertIngredient(RecipeIngredient(recipeId = recipe.id, foodId = foodId, amount = amount))
        }
        return recipe.id
    }

    // Update an existing recipe in place: keep the same row (id, createdAt), update its fields, and
    // replace its ingredients. insertRecipe uses REPLACE, so re-inserting the copied row updates it.
    suspend fun updateRecipe(
        id: String,
        name: String,
        makesServings: Double,
        cookedWeightG: Double?,
        emoji: String?,
        ingredients: List<Pair<String, Double>>
    ) {
        val existing = dao.getRecipe(id) ?: return
        dao.insertRecipe(existing.copy(name = name, makesServings = makesServings, cookedWeightG = cookedWeightG, emoji = emoji))
        dao.deleteIngredientsFor(id)
        ingredients.forEach { (foodId, amount) ->
            dao.insertIngredient(RecipeIngredient(recipeId = id, foodId = foodId, amount = amount))
        }
    }

    suspend fun deleteRecipe(recipe: Recipe) {
        dao.deleteIngredientsFor(recipe.id)
        dao.deleteRecipe(recipe)
    }
}
