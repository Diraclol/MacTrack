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
import java.time.LocalTime
import kotlin.math.roundToInt

private fun servings(amount: Double): String =
    if (amount % 1.0 == 0.0) amount.toInt().toString() else amount.toString()

private fun hourLabel(hour: Int): String {
    val h12 = if (hour % 12 == 0) 12 else hour % 12
    val ampm = if (hour < 12) "AM" else "PM"
    return "$h12 $ampm"
}

private fun timeLabel(timeMinutes: Int): String {
    val h = timeMinutes / 60
    val m = timeMinutes % 60
    val h12 = if (h % 12 == 0) 12 else h % 12
    val ampm = if (h < 12) "AM" else "PM"
    return "%d:%02d %s".format(h12, m, ampm)
}

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

    val byHour = entries.groupBy { it.timeMinutes / 60 }.toSortedMap()

    var foodToLog by remember { mutableStateOf<FoodItem?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("Today", style = MaterialTheme.typography.headlineSmall)
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

        if (byHour.isEmpty()) {
            item { Text("No food logged yet today.", style = MaterialTheme.typography.bodySmall) }
        }

        byHour.forEach { (hour, hourEntries) ->
            val hourCal = hourEntries.sumOf { it.calories }
            item {
                Text(
                    "${hourLabel(hour)} — ${hourCal.roundToInt()} cal",
                    style = MaterialTheme.typography.titleSmall
                )
            }
            items(items = hourEntries, key = { it.id }) { entry ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${timeLabel(entry.timeMinutes)}  ${entry.foodName}  ${servings(entry.quantity)} ${entry.unit} — ${entry.calories.roundToInt()} cal",
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.deleteEntry(entry) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Remove ${entry.foodName}")
                    }
                }
            }
        }

        item { Text("Add from your foods", style = MaterialTheme.typography.titleSmall) }
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
        val now = LocalTime.now()
        LogFoodDialog(
            food = selected,
            defaultTimeMinutes = now.hour * 60 + now.minute,
            onDismiss = { foodToLog = null },
            onConfirm = { amount, timeMinutes ->
                viewModel.logFood(selected, amount, timeMinutes)
                foodToLog = null
            }
        )
    }
}

@Composable
private fun LogFoodDialog(
    food: FoodItem,
    defaultTimeMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, timeMinutes: Int) -> Unit
) {
    var amount by remember { mutableStateOf("1") }
    var hour by remember { mutableStateOf((defaultTimeMinutes / 60).toString()) }
    var minute by remember { mutableStateOf((defaultTimeMinutes % 60).toString()) }

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
                Text("Time")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = hour,
                        onValueChange = { hour = it },
                        label = { Text("Hour 0-23") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minute,
                        onValueChange = { minute = it },
                        label = { Text("Min") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val h = (hour.toIntOrNull() ?: 0).coerceIn(0, 23)
                val m = (minute.toIntOrNull() ?: 0).coerceIn(0, 59)
                onConfirm(amount.toDoubleOrNull() ?: 1.0, h * 60 + m)
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}