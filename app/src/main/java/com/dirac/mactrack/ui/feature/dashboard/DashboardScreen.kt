package com.dirac.mactrack.ui.feature.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dirac.mactrack.data.entity.WeightEntry
import kotlin.math.roundToInt

private val ProteinColor = Color(0xFFE91E63)
private val CarbColor = Color(0xFF2196F3)
private val FatColor = Color(0xFF4CAF50)
private val CalorieColor = Color(0xFFFF9800)

@Composable
fun DashboardScreen(modifier: Modifier = Modifier) {
    val viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory)
    val goal by viewModel.goal.collectAsState()
    val entries by viewModel.todayEntries.collectAsState()
    val weights by viewModel.weights.collectAsState()

    val totalCal = entries.sumOf { it.calories }
    val totalP = entries.sumOf { it.proteinG }
    val totalC = entries.sumOf { it.carbG }
    val totalF = entries.sumOf { it.fatG }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Text("Today", style = MaterialTheme.typography.headlineSmall) }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val g = goal
                    StatBar("Calories", totalCal, g?.calorieGoal ?: 0.0, CalorieColor)
                    StatBar("Protein", totalP, g?.proteinGoalG ?: 0.0, ProteinColor)
                    StatBar("Carbs", totalC, g?.carbGoalG ?: 0.0, CarbColor)
                    StatBar("Fat", totalF, g?.fatGoalG ?: 0.0, FatColor)
                    if (g == null) {
                        Text("No goal set yet.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Weight trend", style = MaterialTheme.typography.titleMedium)
                    val latest = weights.lastOrNull()
                    if (latest != null) {
                        Text("Latest: ${latest.weightKg} kg", style = MaterialTheme.typography.bodyMedium)
                    }
                    WeightChart(
                        weights = weights,
                        modifier = Modifier.fillMaxWidth().height(160.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatBar(label: String, current: Double, goal: Double, color: Color) {
    val fraction = if (goal > 0.0) (current / goal).coerceIn(0.0, 1.0).toFloat() else 0f
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val goalText = if (goal > 0.0) " / ${goal.roundToInt()}" else ""
        Text("$label: ${current.roundToInt()}$goalText")
        LinearProgressIndicator(
            progress = { fraction },
            color = color,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun WeightChart(weights: List<WeightEntry>, modifier: Modifier = Modifier) {
    if (weights.size < 2) {
        Text("Log at least two weigh-ins to see a trend.", style = MaterialTheme.typography.bodySmall)
        return
    }
    val values = weights.map { it.weightKg }
    val minV = values.min()
    val maxV = values.max()
    val range = (maxV - minV).let { if (it == 0.0) 1.0 else it }
    val lineColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier) {
        val h = size.height
        val n = values.size
        val stepX = size.width / (n - 1)
        val points = values.mapIndexed { i, v ->
            Offset(stepX * i, h - (((v - minV) / range).toFloat() * h))
        }
        for (i in 0 until points.size - 1) {
            drawLine(color = lineColor, start = points[i], end = points[i + 1], strokeWidth = 5f)
        }
        points.forEach { drawCircle(color = lineColor, radius = 7f, center = it) }
    }
}