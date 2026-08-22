package com.dirac.mactrack.ui.feature.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dirac.mactrack.data.entity.FoodItem
import kotlin.math.roundToInt

val MEAL_SLOTS = listOf("M1", "M2", "M3", "Supplements")

private fun servings(amount: Double): String =
    if (amount % 1.0 == 0.0) amount.toInt().toString() else amount.toString()

@Composable
fun TodayScreen(modifier: Modifier = Modifier) {
    val viewModel: MealLogViewModel = viewModel(factory = MealLogViewModel.Factory)
    val foods by viewModel.foods.collectAsState()
    val entries by viewModel.todayEntries.collectAsState()
    val goal by viewModel.goal.collectAsState()

    val totalCalories = entries.sumOf { it.calories }
    val totalProtein = entries.sumOf { it.proteinG }
    val totalCarb = entries.sumOf { it.carbG }
    val totalFat = entries.sumOf { it.fatG }

    var foodToLog by remember { mutableStateOf<FoodItem?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("Today")
            val g = goal
            if (g == null) {
                Text("Calories: ${totalCalories.roundToInt()}")
                Text("Protein ${totalProtein.roundToInt()}g · Carbs ${totalCarb.roundToInt()}g · Fat ${totalFat.roundToInt()}g")
            } else {
                Text("Calories: ${totalCalories.roundToInt()} / ${g.calorieGoal.roundToInt()}")
                Text("Protein: ${totalProtein.roundToInt()} / ${g.proteinGoalG.roundToInt()} g")
                Text("Carbs: ${totalCarb.roundToInt()} / ${g.carbGoalG.roundToInt()} g")
                Text("Fat: ${totalFat.roundToInt()} / ${g.fatGoalG.roundToInt()} g")
            }
        }

        MEAL_SLOTS.forEach { slot ->
            val slotEntries = entries.filter { it.mealLabel == slot }
            val slotCalories = slotEntries.sumOf { it.calories }
            val slotProtein = slotEntries.sumOf { it.proteinG }
            val slotCarb = slotEntries.sumOf { it.carbG }
            val slotFat = slotEntries.sumOf { it.fatG }
            item {
                Column {
                    Text("$slot — ${slotCalories.roundToInt()} cal")
                    Text(
                        "P ${slotProtein.roundToInt()}g · C ${slotCarb.roundToInt()}g · F ${slotFat.roundToInt()}g",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            items(items = slotEntries, key = { it.id }) { entry ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${entry.foodName}  ${servings(entry.quantity)} ${entry.unit} — ${entry.calories.roundToInt()} cal",
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.deleteEntry(entry) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Remove ${entry.foodName}")
                    }
                }
            }
        }

        item { Text("Add from your foods") }
        items(items = foods, key = { it.id }) { food ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${food.name} — ${food.calories.roundToInt()} cal",
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = { foodToLog = food }) { Text("Log") }
            }
        }
    }

    val selected = foodToLog
    if (selected != null) {
        LogFoodDialog(
            food = selected,
            onDismiss = { foodToLog = null },
            onConfirm = { meal, amount ->
                viewModel.logFood(selected, meal, amount)
                foodToLog = null
            }
        )
    }
}

@Composable
private fun LogFoodDialog(
    food: FoodItem,
    onDismiss: () -> Unit,
    onConfirm: (mealLabel: String, amount: Double) -> Unit
) {
    var amount by remember { mutableStateOf("1") }
    var meal by remember { mutableStateOf(MEAL_SLOTS.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log ${food.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Servings") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Text("Meal")
                MEAL_SLOTS.forEach { slot ->
                    FilterChip(
                        selected = meal == slot,
                        onClick = { meal = slot },
                        label = { Text(slot) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(meal, amount.toDoubleOrNull() ?: 1.0) }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}