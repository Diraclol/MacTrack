package com.dirac.mactrack.ui.feature.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.dirac.mactrack.ui.common.BackBar
import com.dirac.mactrack.ui.feature.food.FoodLogScreen
import com.dirac.mactrack.ui.feature.meals.MealsScreen
import com.dirac.mactrack.ui.feature.recipes.RecipesScreen

// The Kitchen: Foods, Recipes, and Meals as tabs, with a "+" that opens a create menu
// (Create Food / Meal / Recipe). Each tab hosts its existing screen with its back bar hidden.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onBack: () -> Unit = {},
    onCreateFood: () -> Unit = {},
    onCreateMeal: () -> Unit = {},
    onCreateRecipe: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var tab by remember { mutableStateOf(0) }
    var showCreate by remember { mutableStateOf(false) }
    val titles = listOf("Foods", "Recipes", "Meals")

    Box(modifier = modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            BackBar("Kitchen", onBack, modifier = Modifier.padding(horizontal = 16.dp))
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

        FloatingActionButton(
            onClick = { showCreate = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Create")
        }
    }

    if (showCreate) {
        ModalBottomSheet(
            onDismissRequest = { showCreate = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            CreateMenuItem(Icons.Filled.Fastfood, "Create Food") { showCreate = false; onCreateFood() }
            CreateMenuItem(Icons.Filled.Restaurant, "Create Meal") { showCreate = false; onCreateMeal() }
            CreateMenuItem(Icons.Filled.MenuBook, "Create Recipe") { showCreate = false; onCreateRecipe() }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CreateMenuItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.padding(end = 20.dp))
        Text(label, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
    }
}
