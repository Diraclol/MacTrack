package com.dirac.mactrack.ui.feature.foodsearch

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dirac.mactrack.data.food.foodIcon
import kotlin.math.roundToInt

private fun servingText(amount: Double): String =
    if (amount % 1.0 == 0.0) amount.toInt().toString() else amount.toString()

private val TABS = listOf("All", "Foods", "Meals", "Recipes", "Quick")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedSearchScreen(
    onOpenFood: (String, String) -> Unit,
    onLoggedCart: () -> Unit,
    onBack: () -> Unit,
    picker: String = "",
    onDonePicking: () -> Unit = {},
    onCreateFood: () -> Unit = {},
    onCreateMeal: () -> Unit = {},
    onCreateRecipe: () -> Unit = {},
    onScanBarcode: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val viewModel: UnifiedSearchViewModel = viewModel(factory = UnifiedSearchViewModel.Factory)
    val query by viewModel.query.collectAsState()
    val custom by viewModel.custom.collectAsState()
    val common by viewModel.common.collectAsState()
    val branded by viewModel.branded.collectAsState()
    val searchingBranded by viewModel.searchingBranded.collectAsState()
    val cartCount by viewModel.cartCount.collectAsState()
    val builderCount by viewModel.builderCount.collectAsState()
    val recent by viewModel.recent.collectAsState()
    val savedFoods by viewModel.savedFoods.collectAsState()
    val templates by viewModel.templates.collectAsState()
    val recipes by viewModel.recipes.collectAsState()
    val focusManager = LocalFocusManager.current

    // Picker mode: the screen is reused to pick ingredients for a meal/recipe. It adds picked foods
    // to the shared draft instead of the cart, drops the Meals/Quick tabs and barcode, and swaps the
    // "Log" action for "Done". `picker` is "meal" or "recipe" (blank = normal log-to-today mode).
    val isPicker = picker.isNotBlank()
    val tabs = if (isPicker) listOf("All", "Foods") else TABS

    var tab by remember { mutableStateOf(0) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showCreate by remember { mutableStateOf(false) }

    // back returns to the Create screen in picker mode; otherwise it guards a non-empty cart.
    fun attemptBack() {
        if (isPicker) onDonePicking()
        else if (cartCount > 0) showDiscardDialog = true
        else onBack()
    }
    BackHandler(enabled = true) { attemptBack() }

    // The search field shows on every browsing tab; only Quick add (its own form) hides it.
    val showSearchBar = isPicker || tabs[tab] != "Quick"

    // imePadding lifts the docked bottom bar above the keyboard when it opens.
    Column(modifier = modifier.fillMaxSize().imePadding()) {
        // Top bar: back, title, and the cart's Log action pinned to the right.
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { attemptBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                if (isPicker && picker == "recipe") "Add ingredient" else "Add food",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            if (isPicker) {
                Button(onClick = onDonePicking) {
                    Text(if (builderCount > 0) "Done ($builderCount)" else "Done")
                }
            } else {
                Button(onClick = { viewModel.logCart(onLoggedCart) }, enabled = cartCount > 0) {
                    Text(if (cartCount > 0) "Log $cartCount" else "Log")
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabs.forEachIndexed { i, title ->
                FilterChip(
                    selected = tab == i,
                    onClick = { tab = i },
                    label = { Text(title) },
                    shape = RoundedCornerShape(50)
                )
            }
        }

        Column(modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 8.dp)) {
            when (tab) {
                0 -> AllTab(query, recent, custom, common, branded, searchingBranded, onOpenFood, viewModel, isPicker)
                1 -> FoodsTab(savedFoods, onOpenFood, viewModel, isPicker)
                2 -> MealsTab(query, templates, viewModel)
                3 -> RecipesTab(query, recipes, onOpenFood)
                else -> QuickAddTab(viewModel, onLoggedCart)
            }
        }

        // Docked bottom bar: the search field (with a barcode-scan icon) on browsing tabs, plus a
        // create "+" that opens the same Create menu as the Kitchen. Rises with the keyboard.
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (showSearchBar) {
                // No barcode lookup in picker mode: branded/OFF products can't be meal/recipe
                // ingredients yet (no food_items row), so hide it to avoid a dead end.
                val trailing: (@Composable () -> Unit)? = if (isPicker) null else {
                    {
                        IconButton(onClick = onScanBarcode) {
                            Icon(Icons.Filled.QrCodeScanner, contentDescription = "Scan a barcode")
                        }
                    }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { viewModel.onQueryChange(it) },
                    placeholder = { Text("Search for a food") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = trailing,
                    shape = RoundedCornerShape(28.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
            // In picker mode the only sensible create is a new food (to then add it); go straight
            // there instead of offering the Create Meal/Recipe menu (which would nest).
            FloatingActionButton(onClick = { if (isPicker) onCreateFood() else showCreate = true }) {
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

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard cart?") },
            text = { Text("You have $cartCount item(s) in your cart that haven't been logged. Discard them and leave?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.discardCart()
                    showDiscardDialog = false
                    onBack()
                }) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun AllTab(
    query: String,
    recent: List<com.dirac.mactrack.data.entity.MealEntry>,
    custom: List<com.dirac.mactrack.data.entity.FoodItem>,
    common: List<com.dirac.mactrack.data.cnf.CnfFood>,
    branded: List<com.dirac.mactrack.data.off.OffProduct>,
    searchingBranded: Boolean,
    onOpenFood: (String, String) -> Unit,
    viewModel: UnifiedSearchViewModel,
    isPicker: Boolean
) {
    val favorites by viewModel.favorites.collectAsState()
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (query.isBlank()) {
            if (recent.isNotEmpty()) {
                item { SectionLabel("Recent") }
                items(items = recent, key = { "r_" + it.id }) { entry ->
                    FoodRow(
                        name = entry.foodName,
                        line = "${entry.calories.roundToInt()} cal · ${entry.proteinG.roundToInt()}P ${entry.carbG.roundToInt()}C ${entry.fatG.roundToInt()}F · last ${servingText(entry.amount)} ${entry.unitLabel ?: entry.unit}",
                        onOpen = { entry.sourceId?.let { if (isPicker) viewModel.addIngredient(entry.sourceType, it, entry.foodName) else onOpenFood(entry.sourceType, it) } },
                        onAdd = { entry.sourceId?.let { if (isPicker) viewModel.addIngredient(entry.sourceType, it, entry.foodName) else viewModel.addToCart(entry.sourceType, it) } }
                    )
                }
            }
            // Favorites sit after Recent, before anything else.
            if (favorites.isNotEmpty()) {
                item { SectionLabel("Favorites") }
                items(items = favorites, key = { "f_" + it.id }) { food ->
                    FoodRow(
                        name = food.name,
                        line = "${food.calories.roundToInt()} cal · ${food.proteinG.roundToInt()}P ${food.carbG.roundToInt()}C ${food.fatG.roundToInt()}F · per ${servingText(food.servingSize)} ${food.servingUnit}",
                        onOpen = { if (isPicker) viewModel.addIngredient("custom", food.id, food.name) else onOpenFood("custom", food.id) },
                        onAdd = { if (isPicker) viewModel.addIngredient("custom", food.id, food.name) else viewModel.addToCart("custom", food.id) },
                        favorite = food.favorite,
                        onToggleFavorite = { viewModel.toggleFavorite(food) },
                        emojiOverride = food.emoji
                    )
                }
            }
            if (recent.isEmpty() && favorites.isEmpty()) {
                item { EmptyHint("Search for a food below, or add one from your saved Foods and Meals.") }
            }
        } else {
            if (custom.isNotEmpty()) {
                item { SectionLabel("Foods") }
                items(items = custom, key = { "c_" + it.id }) { food ->
                    FoodRow(
                        name = food.name,
                        line = "${food.calories.roundToInt()} cal · ${food.proteinG.roundToInt()}P ${food.carbG.roundToInt()}C ${food.fatG.roundToInt()}F · per ${servingText(food.servingSize)} ${food.servingUnit}",
                        onOpen = { if (isPicker) viewModel.addIngredient("custom", food.id, food.name) else onOpenFood("custom", food.id) },
                        onAdd = { if (isPicker) viewModel.addIngredient("custom", food.id, food.name) else viewModel.addToCart("custom", food.id) },
                        favorite = food.favorite,
                        onToggleFavorite = { viewModel.toggleFavorite(food) },
                        emojiOverride = food.emoji
                    )
                }
            }
            if (common.isNotEmpty()) {
                item { SectionLabel("Common") }
                items(items = common, key = { "n_" + it.code }) { food ->
                    FoodRow(
                        name = food.name,
                        line = "${food.kcal.roundToInt()} cal · ${food.protein.roundToInt()}P ${food.carb.roundToInt()}C ${food.fat.roundToInt()}F · per 100 g",
                        onOpen = { if (isPicker) viewModel.addIngredient("cnf", food.code.toString(), food.name) else onOpenFood("cnf", food.code.toString()) },
                        onAdd = { if (isPicker) viewModel.addIngredient("cnf", food.code.toString(), food.name) else viewModel.addToCart("cnf", food.code.toString()) }
                    )
                }
            }
            // Branded (Open Food Facts, online). Not usable as meal/recipe ingredients yet, so hidden in
            // picker mode (like the barcode scanner). A short "Searching online..." shows while it loads.
            if (!isPicker && (branded.isNotEmpty() || searchingBranded)) {
                item { SectionLabel("Branded") }
                if (branded.isEmpty() && searchingBranded) {
                    item { EmptyHint("Searching online…") }
                } else {
                    items(items = branded, key = { "b_" + it.code }) { p ->
                        FoodRow(
                            name = p.name,
                            line = "${p.kcalPer100.roundToInt()} cal · ${p.proteinPer100.roundToInt()}P ${p.carbPer100.roundToInt()}C ${p.fatPer100.roundToInt()}F · per 100 g",
                            onOpen = { onOpenFood("branded", p.code) },
                            onAdd = { viewModel.addToCart("branded", p.code) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FoodsTab(
    savedFoods: List<com.dirac.mactrack.data.entity.FoodItem>,
    onOpenFood: (String, String) -> Unit,
    viewModel: UnifiedSearchViewModel,
    isPicker: Boolean
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (savedFoods.isEmpty()) {
            item { EmptyHint("No saved foods yet. Foods you create show up here.") }
        }
        items(items = savedFoods, key = { it.id }) { food ->
            FoodRow(
                name = food.name,
                line = "${food.calories.roundToInt()} cal · ${food.proteinG.roundToInt()}P ${food.carbG.roundToInt()}C ${food.fatG.roundToInt()}F · per ${servingText(food.servingSize)} ${food.servingUnit}",
                onOpen = { if (isPicker) viewModel.addIngredient("custom", food.id, food.name) else onOpenFood("custom", food.id) },
                onAdd = { if (isPicker) viewModel.addIngredient("custom", food.id, food.name) else viewModel.addToCart("custom", food.id) },
                favorite = food.favorite,
                onToggleFavorite = { viewModel.toggleFavorite(food) },
                emojiOverride = food.emoji
            )
        }
    }
}

@Composable
private fun MealsTab(
    query: String,
    templates: List<com.dirac.mactrack.data.entity.MealTemplate>,
    viewModel: UnifiedSearchViewModel
) {
    val shown = if (query.isBlank()) templates
        else templates.filter { it.name.contains(query.trim(), ignoreCase = true) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (shown.isEmpty()) {
            item {
                EmptyHint(
                    if (query.isBlank()) "No saved meals yet. Create repeatable meals from the More screen."
                    else "No meals match \"$query\"."
                )
            }
        }
        items(items = shown, key = { it.id }) { template ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🍱", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(end = 12.dp))
                    Text(template.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = { viewModel.addTemplateToCart(template.id) }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add ${template.name} to cart")
                    }
                }
            }
        }
    }
}

@Composable
private fun RecipesTab(
    query: String,
    recipes: List<com.dirac.mactrack.data.entity.Recipe>,
    onOpenFood: (String, String) -> Unit
) {
    val shown = if (query.isBlank()) recipes
        else recipes.filter { it.name.contains(query.trim(), ignoreCase = true) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (shown.isEmpty()) {
            item {
                EmptyHint(
                    if (query.isBlank()) "No saved recipes yet. Create one in the Kitchen."
                    else "No recipes match \"$query\"."
                )
            }
        }
        items(items = shown, key = { it.id }) { recipe ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onOpenFood("recipe", recipe.id) }.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(foodIcon(recipe.emoji, recipe.name), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(end = 12.dp))
                    Text(recipe.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = { onOpenFood("recipe", recipe.id) }) {
                        Icon(Icons.Filled.Add, contentDescription = "Log ${recipe.name}")
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAddTab(viewModel: UnifiedSearchViewModel, onDone: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carb by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }

    val p = protein.toDoubleOrNull() ?: 0.0
    val c = carb.toDoubleOrNull() ?: 0.0
    val f = fat.toDoubleOrNull() ?: 0.0
    val macroKcal = p * 4 + c * 4 + f * 9

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Quick add", style = MaterialTheme.typography.titleMedium)
        Text(
            "A one-off entry logged straight to today. Not saved as a reusable food.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        val fieldShape = RoundedCornerShape(16.dp)
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name (optional)") }, shape = fieldShape, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(
            value = calories, onValueChange = { calories = it },
            label = { Text("Calories (blank = use macro sum)") },
            shape = fieldShape,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Text("Macro sum: ${macroKcal.roundToInt()} kcal", style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(value = protein, onValueChange = { protein = it }, label = { Text("Protein (g)") }, shape = fieldShape, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = carb, onValueChange = { carb = it }, label = { Text("Carbs (g)") }, shape = fieldShape, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = fat, onValueChange = { fat = it }, label = { Text("Fat (g)") }, shape = fieldShape, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), singleLine = true)
        Button(
            onClick = {
                val cal = calories.toDoubleOrNull() ?: macroKcal
                viewModel.quickAdd(name, cal, p, c, f, onDone)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add to today's log")
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall)
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
private fun FoodRow(
    name: String,
    line: String,
    onOpen: () -> Unit,
    onAdd: () -> Unit,
    favorite: Boolean? = null,
    onToggleFavorite: () -> Unit = {},
    emojiOverride: String? = null
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onOpen() }.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(foodIcon(emojiOverride, name), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(end = 12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(line, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            // Heart only shows for saved (custom) foods; recent/common rows pass favorite = null.
            if (favorite != null) {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        if (favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (favorite) "Remove $name from favorites" else "Add $name to favorites",
                        tint = if (favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, contentDescription = "Add $name to cart")
            }
        }
    }
}

@Composable
private fun CreateMenuItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.padding(end = 20.dp))
        Text(label, style = MaterialTheme.typography.titleMedium)
    }
}
