package com.dirac.mactrack.ui.feature.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dirac.mactrack.ui.common.BackBar
import com.dirac.mactrack.ui.feature.food.FoodLogScreen
import com.dirac.mactrack.ui.feature.meals.MealsScreen
import com.dirac.mactrack.ui.feature.recipes.RecipesScreen

// One combined screen for the saved-food library: Foods, Recipes, and Meals as tabs. Each
// tab hosts its existing screen with its own back bar hidden (showBar = false).
@Composable
fun LibraryScreen(onBack: () -> Unit = {}, modifier: Modifier = Modifier) {
    var tab by remember { mutableStateOf(0) }
    val titles = listOf("Foods", "Recipes", "Meals")
    Column(modifier = modifier.fillMaxSize()) {
        BackBar("Saved Foods, Meals & Recipes", onBack, modifier = Modifier.padding(horizontal = 16.dp))
        TabRow(selectedTabIndex = tab) {
            titles.forEachIndexed { i, title ->
                Tab(selected = tab == i, onClick = { tab = i }, text = { Text(title) })
            }
        }
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            when (tab) {
                0 -> FoodLogScreen(showBar = false)
                1 -> RecipesScreen(showBar = false)
                else -> MealsScreen(showBar = false)
            }
        }
    }
}
