package com.dirac.mactrack.ui.feature.food

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dirac.mactrack.ui.common.BackBar
import kotlin.math.roundToInt

@Composable
fun FoodLogScreen(modifier: Modifier = Modifier, onBack: () -> Unit = {}) {
    val viewModel: FoodViewModel = viewModel(factory = FoodViewModel.Factory)
    val foods by viewModel.foods.collectAsState()

    var name by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carb by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var fiber by remember { mutableStateOf("") }
    var sugar by remember { mutableStateOf("") }
    var satFat by remember { mutableStateOf("") }
    var sodium by remember { mutableStateOf("") }
    var potassium by remember { mutableStateOf("") }
    var cholesterol by remember { mutableStateOf("") }
    var servingSize by remember { mutableStateOf("1") }
    var servingUnit by remember { mutableStateOf("serving") }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { BackBar("Saved foods", onBack) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = calories,
                    onValueChange = { calories = it },
                    label = { Text("Calories") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = protein,
                    onValueChange = { protein = it },
                    label = { Text("Protein (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = carb,
                    onValueChange = { carb = it },
                    label = { Text("Carbs (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = fat,
                    onValueChange = { fat = it },
                    label = { Text("Fat (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Micronutrients (optional)", style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(
                    value = fiber,
                    onValueChange = { fiber = it },
                    label = { Text("Fiber (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = sugar,
                    onValueChange = { sugar = it },
                    label = { Text("Sugar (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = satFat,
                    onValueChange = { satFat = it },
                    label = { Text("Saturated fat (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = sodium,
                    onValueChange = { sodium = it },
                    label = { Text("Sodium (mg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = potassium,
                    onValueChange = { potassium = it },
                    label = { Text("Potassium (mg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = cholesterol,
                    onValueChange = { cholesterol = it },
                    label = { Text("Cholesterol (mg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = servingSize,
                    onValueChange = { servingSize = it },
                    label = { Text("Serving size") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Unit")
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("serving", "g", "ml", "piece", "cup", "tbsp", "tsp", "oz", "scoop", "tablet", "capsule").forEach { u ->
                        FilterChip(
                            selected = servingUnit == u,
                            onClick = { servingUnit = u },
                            label = { Text(u) }
                        )
                    }
                }
                Text("Values above are for one serving: ${servingSize.ifBlank { "1" }} $servingUnit")

                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.addFood(
                                name = name.trim(),
                                calories = calories.toDoubleOrNull() ?: 0.0,
                                proteinG = protein.toDoubleOrNull() ?: 0.0,
                                carbG = carb.toDoubleOrNull() ?: 0.0,
                                fatG = fat.toDoubleOrNull() ?: 0.0,
                                fiberG = fiber.toDoubleOrNull() ?: 0.0,
                                sugarG = sugar.toDoubleOrNull() ?: 0.0,
                                satFatG = satFat.toDoubleOrNull() ?: 0.0,
                                sodiumMg = sodium.toDoubleOrNull() ?: 0.0,
                                potassiumMg = potassium.toDoubleOrNull() ?: 0.0,
                                cholesterolMg = cholesterol.toDoubleOrNull() ?: 0.0,
                                servingSize = servingSize.toDoubleOrNull() ?: 1.0,
                                servingUnit = servingUnit
                            )
                            name = ""; calories = ""; protein = ""; carb = ""; fat = ""
                            fiber = ""; sugar = ""; satFat = ""; sodium = ""; potassium = ""; cholesterol = ""
                            servingSize = "1"; servingUnit = "serving"
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save food")
                }
                Text("Your foods", style = MaterialTheme.typography.titleSmall)
            }
        }
        items(items = foods, key = { it.id }) { food ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${food.name} — ${food.calories.roundToInt()} cal · " +
                            "${food.proteinG.roundToInt()}P ${food.carbG.roundToInt()}C ${food.fatG.roundToInt()}F · " +
                            "fiber ${food.fiberG.roundToInt()}g · per ${servingText(food.servingSize)} ${food.servingUnit}",
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { viewModel.deleteFood(food) }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete ${food.name}")
                }
            }
        }
    }
}

private fun servingText(amount: Double): String =
    if (amount % 1.0 == 0.0) amount.toInt().toString() else amount.toString()