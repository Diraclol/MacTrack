package com.dirac.mactrack.ui.feature.foodsearch

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dirac.mactrack.data.food.foodEmoji
import com.dirac.mactrack.ui.common.BackBar
import kotlin.math.roundToInt
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

private fun servingText(amount: Double): String =
    if (amount % 1.0 == 0.0) amount.toInt().toString() else amount.toString()

@Composable
fun UnifiedSearchScreen(
    onOpenFood: (String, String) -> Unit,
    onLoggedCart: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: UnifiedSearchViewModel = viewModel(factory = UnifiedSearchViewModel.Factory)
    val query by viewModel.query.collectAsState()
    val custom by viewModel.custom.collectAsState()
    val common by viewModel.common.collectAsState()
    val cartCount by viewModel.cartCount.collectAsState()
    val recent by viewModel.recent.collectAsState()
    val focusManager = LocalFocusManager.current
    var showDiscardDialog by remember { mutableStateOf(false) }
    var barcode by remember { mutableStateOf("") }

    // back should guard when the cart has items
    fun attemptBack() {
        if (cartCount > 0) showDiscardDialog = true else onBack()
    }

    // intercept the system back gesture the same way
    BackHandler(enabled = true) { attemptBack() }

    // imePadding lifts the docked bottom bar above the keyboard when it opens.
    Column(modifier = modifier.fillMaxSize().imePadding()) {
        BackBar("Search foods", onBack = { attemptBack() }, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (query.isBlank()) {
                if (recent.isNotEmpty()) {
                    item { Text("Recent", style = MaterialTheme.typography.titleSmall) }
                    items(items = recent, key = { "r_" + it.id }) { entry ->
                        FoodRow(
                            name = entry.foodName,
                            line = "${entry.calories.roundToInt()} cal · ${entry.proteinG.roundToInt()}P ${entry.carbG.roundToInt()}C ${entry.fatG.roundToInt()}F · last ${servingText(entry.amount)} ${entry.unitLabel ?: entry.unit}",
                            onOpen = { entry.sourceId?.let { onOpenFood(entry.sourceType, it) } },
                            onAdd = { entry.sourceId?.let { viewModel.addToCart(entry.sourceType, it) } }
                        )
                    }
                }
            } else {
                if (custom.isNotEmpty()) {
                    item { Text("Foods", style = MaterialTheme.typography.titleSmall) }
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
                    item { Text("Common", style = MaterialTheme.typography.titleSmall) }
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

        // Docked bottom bar: cart action, temporary barcode entry, and the rounded search
        // field pinned to the bottom (it rises with the keyboard via imePadding above).
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Cart: $cartCount", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Button(onClick = { viewModel.logCart(onLoggedCart) }, enabled = cartCount > 0) {
                    Text("Log Foods")
                }
            }
            // Temporary barcode entry to test Open Food Facts lookups until camera scanning lands.
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text("Barcode") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (barcode.isNotBlank()) onOpenFood("branded", barcode.trim()) })
                )
                Button(onClick = { if (barcode.isNotBlank()) onOpenFood("branded", barcode.trim()) }, enabled = barcode.isNotBlank()) {
                    Text("Look up")
                }
            }
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.onQueryChange(it) },
                placeholder = { Text("Search for a food") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
            )
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
private fun FoodRow(name: String, line: String, onOpen: () -> Unit, onAdd: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(foodEmoji(name), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(end = 12.dp))
        Column(modifier = Modifier.weight(1f).clickable { onOpen() }) {
            Text(name, style = MaterialTheme.typography.bodyLarge)
            Text(line, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onAdd) {
            Icon(Icons.Filled.Add, contentDescription = "Add $name to cart")
        }
    }
    HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
}
