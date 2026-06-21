package com.dirac.mactrack.ui.feature.food

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.Alignment
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip

@Composable
fun FoodLogScreen(modifier: Modifier = Modifier) {
    val viewModel: FoodViewModel = viewModel(factory = FoodViewModel.Factory)
    val foods by viewModel.foods.collectAsState()

    var name by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var servingSize by remember { mutableStateOf("1") }
    var servingUnit by remember { mutableStateOf("serving") }
    var protein by remember { mutableStateOf("") }
    var carb by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
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
        Text("Macros above are for one serving: ${servingSize.ifBlank { "1" }} $servingUnit")
        Button(
            onClick = {
                if (name.isNotBlank()) {
                    viewModel.addFood(
                        name = name.trim(),
                        calories = calories.toDoubleOrNull() ?: 0.0,
                        proteinG = protein.toDoubleOrNull() ?: 0.0,
                        carbG = carb.toDoubleOrNull() ?: 0.0,
                        fatG = fat.toDoubleOrNull() ?: 0.0,
                        servingSize = servingSize.toDoubleOrNull() ?: 1.0,
                        servingUnit = servingUnit
                    )
                    name = ""
                    calories = ""
                    protein = ""
                    carb = ""
                    fat = ""
                    servingSize = "1"
                    servingUnit = "serving"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save food")
        }
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items = foods, key = { it.id }) { food ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${food.name} — ${food.calories} cal, ${food.proteinG}g P, ${food.carbG}g C, ${food.fatG}g F",
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.deleteFood(food) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete ${food.name}")
                    }
                }
            }
        }
    }
}