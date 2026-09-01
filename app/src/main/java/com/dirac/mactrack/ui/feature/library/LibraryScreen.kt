package com.dirac.mactrack.ui.feature.library

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dirac.mactrack.data.entity.FoodItem
import com.dirac.mactrack.data.food.foodIcon
import com.dirac.mactrack.ui.common.BackBar
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val ProteinColor = Color(0xFFE91E63)
private val CarbColor = Color(0xFF2196F3)
private val FatColor = Color(0xFF4CAF50)

private val KITCHEN_TABS = listOf("All", "Recipes", "Meals", "Foods")

private fun servingText(x: Double): String =
    if (x % 1.0 == 0.0) x.toInt().toString() else x.toString()

// The Kitchen: browse saved foods, meals, and recipes with pill tabs, a docked search, and a "+" that
// opens a create menu. Tap a row to edit that item; swipe it left to reveal a trash button (same
// gesture as the food log). Managing saved foods/meals/recipes lives here.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onBack: () -> Unit = {},
    onCreateFood: () -> Unit = {},
    onCreateMeal: () -> Unit = {},
    onCreateRecipe: () -> Unit = {},
    onOpenFood: (String) -> Unit = {},
    onOpenMeal: (String) -> Unit = {},
    onOpenRecipe: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val viewModel: KitchenViewModel = viewModel(factory = KitchenViewModel.Factory)
    val query by viewModel.query.collectAsState()
    val foods by viewModel.foods.collectAsState()
    val meals by viewModel.meals.collectAsState()
    val recipes by viewModel.recipes.collectAsState()

    var tab by remember { mutableStateOf(0) }
    var showCreate by remember { mutableStateOf(false) }

    val showFoods = tab == 0 || tab == 3
    val showMeals = tab == 0 || tab == 2
    val showRecipes = tab == 0 || tab == 1

    Column(modifier = modifier.fillMaxSize().imePadding()) {
        BackBar("Kitchen", onBack, modifier = Modifier.padding(horizontal = 16.dp))

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KITCHEN_TABS.forEachIndexed { i, title ->
                FilterChip(
                    selected = tab == i,
                    onClick = { tab = i },
                    label = { Text(title) },
                    shape = RoundedCornerShape(50)
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Text("Saved", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(vertical = 4.dp)) }

            if (showRecipes) {
                items(items = recipes, key = { "r_" + it.recipe.id }) { summary ->
                    RecipeRow(summary = summary, onOpen = { onOpenRecipe(summary.recipe.id) }, onDelete = { viewModel.deleteRecipe(summary.recipe) })
                }
            }
            if (showFoods) {
                items(items = foods, key = { "f_" + it.id }) { food ->
                    FoodRow(food = food, onOpen = { onOpenFood(food.id) }, onDelete = { viewModel.deleteFood(food) })
                }
            }
            if (showMeals) {
                items(items = meals, key = { "m_" + it.template.id }) { summary ->
                    MealRow(summary = summary, onOpen = { onOpenMeal(summary.template.id) }, onDelete = { viewModel.deleteMeal(summary.template) })
                }
            }
            when (tab) {
                1 -> if (recipes.isEmpty()) item { EmptyHint("No saved recipes yet. Tap + to create one.") }
                2 -> if (meals.isEmpty()) item { EmptyHint("No saved meals yet. Tap + to create one.") }
                3 -> if (foods.isEmpty()) item { EmptyHint("No saved foods yet. Tap + to create one.") }
                else -> if (foods.isEmpty() && meals.isEmpty() && recipes.isEmpty()) item { EmptyHint("Nothing saved yet. Tap + to create a food, meal, or recipe.") }
            }
        }

        // Docked search + create FAB.
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.onQueryChange(it) },
                placeholder = { Text("Search food") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                shape = RoundedCornerShape(28.dp),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            FloatingActionButton(onClick = { showCreate = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Create")
            }
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

// A row that slides left to reveal a red trash panel (same gesture as the food log). Tapping the row
// while closed opens it for editing; while open, tapping just closes it. The trash button deletes.
@Composable
private fun SwipeToDeleteRow(
    rowKey: Any,
    deleteLabel: String,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    val revealPx = with(LocalDensity.current) { 76.dp.toPx() }
    val offsetX = remember(rowKey) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val dragState = rememberDraggableState { delta ->
        scope.launch { offsetX.snapTo((offsetX.value + delta).coerceIn(-revealPx, 0f)) }
    }
    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.errorContainer),
            contentAlignment = Alignment.CenterEnd
        ) {
            IconButton(onClick = onDelete, modifier = Modifier.padding(end = 12.dp)) {
                Icon(Icons.Filled.Delete, contentDescription = deleteLabel, tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = dragState,
                    onDragStopped = {
                        val target = if (offsetX.value < -revealPx / 2f) -revealPx else 0f
                        offsetX.animateTo(target)
                    }
                )
                .clickable {
                    if (offsetX.value != 0f) scope.launch { offsetX.animateTo(0f) } else onOpen()
                }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}

@Composable
private fun FoodRow(food: FoodItem, onOpen: () -> Unit, onDelete: () -> Unit) {
    SwipeToDeleteRow(rowKey = food.id, deleteLabel = "Delete ${food.name}", onOpen = onOpen, onDelete = onDelete) {
        Text(foodIcon(food.emoji, food.name), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(end = 12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(food.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${servingText(food.servingSize)} ${food.servingUnit}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("P${food.proteinG.roundToInt()}", style = MaterialTheme.typography.labelMedium, color = ProteinColor)
                Text("C${food.carbG.roundToInt()}", style = MaterialTheme.typography.labelMedium, color = CarbColor)
                Text("F${food.fatG.roundToInt()}", style = MaterialTheme.typography.labelMedium, color = FatColor)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${food.calories.roundToInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("cal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RecipeRow(summary: RecipeSummary, onOpen: () -> Unit, onDelete: () -> Unit) {
    val r = summary.recipe
    SwipeToDeleteRow(rowKey = r.id, deleteLabel = "Delete ${r.name}", onOpen = onOpen, onDelete = onDelete) {
        Text(foodIcon(r.emoji, r.name), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(end = 12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(r.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Recipe · ${servingText(r.makesServings)} serv", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("P${summary.proteinG.roundToInt()}", style = MaterialTheme.typography.labelMedium, color = ProteinColor)
                Text("C${summary.carbG.roundToInt()}", style = MaterialTheme.typography.labelMedium, color = CarbColor)
                Text("F${summary.fatG.roundToInt()}", style = MaterialTheme.typography.labelMedium, color = FatColor)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${summary.calories.roundToInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("cal/serv", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MealRow(summary: MealSummary, onOpen: () -> Unit, onDelete: () -> Unit) {
    SwipeToDeleteRow(rowKey = summary.template.id, deleteLabel = "Delete ${summary.template.name}", onOpen = onOpen, onDelete = onDelete) {
        Text("🍱", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(end = 12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(summary.template.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Meal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("P${summary.proteinG.roundToInt()}", style = MaterialTheme.typography.labelMedium, color = ProteinColor)
                Text("C${summary.carbG.roundToInt()}", style = MaterialTheme.typography.labelMedium, color = CarbColor)
                Text("F${summary.fatG.roundToInt()}", style = MaterialTheme.typography.labelMedium, color = FatColor)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${summary.calories.roundToInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("cal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 12.dp)
    )
}

@Composable
private fun CreateMenuItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.padding(end = 20.dp))
        Text(label, style = MaterialTheme.typography.titleMedium)
    }
}
