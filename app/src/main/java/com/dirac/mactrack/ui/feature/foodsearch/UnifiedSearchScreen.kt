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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dirac.mactrack.data.food.foodEmoji
import kotlin.math.roundToInt

private fun servingText(amount: Double): String =
    if (amount % 1.0 == 0.0) amount.toInt().toString() else amount.toString()

private val TABS = listOf("All", "Foods", "Meals", "Quick")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedSearchScreen(
    onOpenFood: (String, String) -> Unit,
    onLoggedCart: () -> Unit,
    onBack: () -> Unit,
    onCreateFood: () -> Unit = {},
    onCreateMeal: () -> Unit = {},
    onCreateRecipe: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val viewModel: UnifiedSearchViewModel = viewModel(factory = UnifiedSearchViewModel.Factory)
    val query by viewModel.query.collectAsState()
    val custom by viewModel.custom.collectAsState()
    val common by viewModel.common.collectAsState()
    val cartCount by viewModel.cartCount.collectAsState()
    val recent by viewModel.recent.collectAsState()
    val savedFoods by viewModel.savedFoods.collectAsState()
    val templates by viewModel.templates.collectAsState()
    val focusManager = LocalFocusManager.current

    var tab by remember { mutableStateOf(0) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showBarcodeDialog by remember { mutableStateOf(false) }
    var showCreate by remember { mutableStateOf(false) }

    // back should guard when the cart has items
    fun attemptBack() {
        if (cartCount > 0) showDiscardDialog = true else onBack()
    }
    BackHandler(enabled = true) { attemptBack() }

    // The search field only makes sense on the browsing tabs (All, Foods).
    val showSearchBar = tab == 0 || tab == 1

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
            Text("Add food", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Button(onClick = { viewModel.logCart(onLoggedCart) }, enabled = cartCount > 0) {
                Text(if (cartCount > 0) "Log $cartCount" else "Log")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TABS.forEachIndexed { i, title ->
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
                0 -> AllTab(query, recent, custom, common, onOpenFood, viewModel)
                1 -> FoodsTab(savedFoods, onOpenFood, viewModel)
                2 -> MealsTab(templates, viewModel)
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
                OutlinedTextField(
                    value = query,
                    onValueChange = { viewModel.onQueryChange(it) },
                    placeholder = { Text("Search for a food") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { showBarcodeDialog = true }) {
                            Icon(Icons.Filled.QrCodeScanner, contentDescription = "Scan or enter a barcode")
                        }
                    },
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
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

    if (showBarcodeDialog) {
        BarcodeDialog(
            onDismiss = { showBarcodeDialog = false },
            onLookUp = { code ->
                showBarcodeDialog = false
                onOpenFood("branded", code)
            }
        )
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
    onOpenFood: (String, String) -> Unit,
    viewModel: UnifiedSearchViewModel
) {
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
                        onOpen = { entry.sourceId?.let { onOpenFood(entry.sourceType, it) } },
                        onAdd = { entry.sourceId?.let { viewModel.addToCart(entry.sourceType, it) } }
                    )
                }
            } else {
                item { EmptyHint("Search for a food below, or add one from your saved Foods and Meals.") }
            }
        } else {
            if (custom.isNotEmpty()) {
                item { SectionLabel("Foods") }
                items(items = custom, key = { "c_" + it.id }) { food ->
                    FoodRow(
                        name = food.name,
                        line = "${food.calories.roundToInt()} cal · ${food.proteinG.roundToInt()}P ${food.carbG.roundToInt()}C ${food.fatG.roundToInt()}F · per ${servingText(food.servingSize)} ${food.servingUnit}",
                        onOpen = { onOpenFood("custom", food.id) },
                        onAdd = { viewModel.addToCart("custom", food.id) }
                    )
                }
            }
            if (common.isNotEmpty()) {
                item { SectionLabel("Common") }
                items(items = common, key = { "n_" + it.code }) { food ->
                    FoodRow(
                        name = food.name,
                        line = "${food.kcal.roundToInt()} cal · ${food.protein.roundToInt()}P ${food.carb.roundToInt()}C ${food.fat.roundToInt()}F · per 100 g",
                        onOpen = { onOpenFood("cnf", food.code.toString()) },
                        onAdd = { viewModel.addToCart("cnf", food.code.toString()) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FoodsTab(
    savedFoods: List<com.dirac.mactrack.data.entity.FoodItem>,
    onOpenFood: (String, String) -> Unit,
    viewModel: UnifiedSearchViewModel
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
                onOpen = { onOpenFood("custom", food.id) },
                onAdd = { viewModel.addToCart("custom", food.id) }
            )
        }
    }
}

@Composable
private fun MealsTab(
    templates: List<com.dirac.mactrack.data.entity.MealTemplate>,
    viewModel: UnifiedSearchViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (templates.isEmpty()) {
            item { EmptyHint("No saved meals yet. Create repeatable meals from the More screen.") }
        }
        items(items = templates, key = { it.id }) { template ->
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
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(
            value = calories, onValueChange = { calories = it },
            label = { Text("Calories (blank = use macro sum)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Text("Macro sum: ${macroKcal.roundToInt()} kcal", style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(value = protein, onValueChange = { protein = it }, label = { Text("Protein (g)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = carb, onValueChange = { carb = it }, label = { Text("Carbs (g)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = fat, onValueChange = { fat = it }, label = { Text("Fat (g)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), singleLine = true)
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
private fun FoodRow(name: String, line: String, onOpen: () -> Unit, onAdd: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onOpen() }.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(foodEmoji(name), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(end = 12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(line, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

@Composable
private fun BarcodeDialog(onDismiss: () -> Unit, onLookUp: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Barcode") },
        text = {
            Column {
                Text(
                    "Enter a product barcode to look it up in Open Food Facts. Camera scanning is coming soon.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Barcode number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (code.isNotBlank()) onLookUp(code.trim()) }),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (code.isNotBlank()) onLookUp(code.trim()) }, enabled = code.isNotBlank()) {
                Text("Look up")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
