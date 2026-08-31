package com.dirac.mactrack.ui.feature.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dirac.mactrack.data.entity.MealEntry
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

private val CalorieColor = Color(0xFFFF9800)
private val ProteinColor = Color(0xFFE91E63)
private val FatColor = Color(0xFF4CAF50)
private val CarbColor = Color(0xFF2196F3)

private fun servings(amount: Double): String =
    if (amount % 1.0 == 0.0) amount.toInt().toString() else amount.toString()

private fun hourLabel(hour: Int): String {
    val h12 = if (hour % 12 == 0) 12 else hour % 12
    val ampm = if (hour < 12) "AM" else "PM"
    return "$h12 $ampm"
}

@Composable
fun TodayScreen(onOpenSearch: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel: MealLogViewModel = viewModel(factory = MealLogViewModel.Factory)
    val entries by viewModel.todayEntries.collectAsState()
    val goal by viewModel.goal.collectAsState()

    val totalCal = entries.sumOf { it.calories }
    val totalP = entries.sumOf { it.proteinG }
    val totalF = entries.sumOf { it.fatG }
    val totalC = entries.sumOf { it.carbG }

    val byHour = entries.groupBy { it.timeMinutes / 60 }.toSortedMap()

    val today = LocalDate.now()
    val dateLabel = "${today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()).uppercase()}, " +
            "${today.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).uppercase()} ${today.dayOfMonth}"

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(dateLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Today", style = MaterialTheme.typography.headlineSmall)

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TotalStat(Modifier.weight(1f), "Cal", totalCal, goal?.calorieGoal ?: 0.0, CalorieColor)
            TotalStat(Modifier.weight(1f), "P", totalP, goal?.proteinGoalG ?: 0.0, ProteinColor)
            TotalStat(Modifier.weight(1f), "F", totalF, goal?.fatGoalG ?: 0.0, FatColor)
            TotalStat(Modifier.weight(1f), "C", totalC, goal?.carbGoalG ?: 0.0, CarbColor)
        }

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (byHour.isEmpty()) {
                item {
                    Text(
                        "No food logged yet today. Search below to add something.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            byHour.forEach { (hour, hourEntries) ->
                val hourCal = hourEntries.sumOf { it.calories }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceVariant) {
                            Text(
                                hourLabel(hour),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        Text(
                            "${hourCal.roundToInt()} cal",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(items = hourEntries, key = { it.id }) { entry ->
                    FoodCard(entry = entry, onDelete = { viewModel.deleteEntry(entry) })
                }
            }
        }

        Surface(
            onClick = onOpenSearch,
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Search for a food", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TotalStat(modifier: Modifier, label: String, consumed: Double, goal: Double, color: Color) {
    val has = goal > 0.0
    val frac = if (has) (consumed / goal).coerceIn(0.0, 1.0).toFloat() else 0f
    val left = (goal - consumed).roundToInt()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(if (has) "$left" else "${consumed.roundToInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(if (has) "left" else "eaten", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        LinearProgressIndicator(progress = { frac }, color = color, modifier = Modifier.fillMaxWidth().height(4.dp))
    }
}

@Composable
private fun FoodCard(entry: MealEntry, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(entry.foodName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    "${entry.proteinG.roundToInt()}P ${entry.fatG.roundToInt()}F ${entry.carbG.roundToInt()}C · ${servings(entry.quantity)} ${entry.unit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text("${entry.calories.roundToInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Remove ${entry.foodName}", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}