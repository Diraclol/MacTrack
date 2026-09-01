package com.dirac.mactrack.ui.feature.trends

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dirac.mactrack.data.dao.DailyTotals
import com.dirac.mactrack.data.entity.Goal
import com.dirac.mactrack.ui.common.BackBar
import kotlin.math.roundToInt

private val CalorieColor = Color(0xFFFF9800)
private val ProteinColor = Color(0xFFE91E63)
private val CarbColor = Color(0xFF2196F3)
private val FatColor = Color(0xFF4CAF50)

private fun metricValue(d: DailyTotals, m: TrendMetric): Double = when (m) {
    TrendMetric.CALORIES -> d.calories
    TrendMetric.PROTEIN -> d.proteinG
    TrendMetric.CARBS -> d.carbG
    TrendMetric.FAT -> d.fatG
}

private fun metricGoal(g: Goal?, m: TrendMetric): Double = when (m) {
    TrendMetric.CALORIES -> g?.calorieGoal ?: 0.0
    TrendMetric.PROTEIN -> g?.proteinGoalG ?: 0.0
    TrendMetric.CARBS -> g?.carbGoalG ?: 0.0
    TrendMetric.FAT -> g?.fatGoalG ?: 0.0
}

private fun metricColor(m: TrendMetric): Color = when (m) {
    TrendMetric.CALORIES -> CalorieColor
    TrendMetric.PROTEIN -> ProteinColor
    TrendMetric.CARBS -> CarbColor
    TrendMetric.FAT -> FatColor
}

@Composable
fun TrendsScreen(onBack: () -> Unit = {}, modifier: Modifier = Modifier) {
    val vm: TrendsViewModel = viewModel(factory = TrendsViewModel.Factory)
    val period by vm.period.collectAsState()
    val metric by vm.metric.collectAsState()
    val daily by vm.daily.collectAsState()
    val goal by vm.goal.collectAsState()

    val values = daily.map { metricValue(it, metric) }
    val avg = if (daily.isNotEmpty()) values.sum() / daily.size else 0.0
    val goalVal = metricGoal(goal, metric)
    val color = metricColor(metric)

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        BackBar("Trends", onBack)

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TrendMetric.entries.forEach { m ->
                FilterChip(selected = metric == m, onClick = { vm.setMetric(m) }, label = { Text(m.label) })
            }
        }

        Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Daily average", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "${avg.roundToInt()} ${metric.unit}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (daily.isEmpty()) "No logged days in this period" else "over ${daily.size} logged day${if (daily.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TrendPeriod.entries.forEach { p ->
                FilterChip(selected = period == p, onClick = { vm.setPeriod(p) }, label = { Text(p.label) })
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.fillMaxWidth().height(220.dp).padding(16.dp)) {
                if (values.isEmpty()) {
                    Text(
                        "Log food to see your trend.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    TrendBarChart(values = values, goal = goalVal, avg = avg, color = color, modifier = Modifier.fillMaxSize())
                    // Value labels on the graph itself.
                    Row(
                        modifier = Modifier.align(Alignment.TopStart),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Avg ${avg.roundToInt()}", style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
                        if (goalVal > 0.0) {
                            Text("Goal ${goalVal.roundToInt()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("Max ${(values.maxOrNull() ?: 0.0).roundToInt()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendBarChart(values: List<Double>, goal: Double, avg: Double, color: Color, modifier: Modifier = Modifier) {
    val goalColor = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier = modifier) {
        if (values.isEmpty()) return@Canvas
        val maxV = maxOf(values.max(), goal, avg, 1.0)
        val n = values.size
        val gap = if (n <= 60) 2.dp.toPx() else 0f
        val barW = ((size.width - gap * (n - 1)) / n).coerceAtLeast(1f)
        values.forEachIndexed { i, v ->
            val h = (v / maxV).toFloat() * size.height
            val x = i * (barW + gap)
            drawRect(color = color, topLeft = Offset(x, size.height - h), size = Size(barW, h))
        }
        if (goal > 0.0) {
            val gy = size.height - (goal / maxV).toFloat() * size.height
            drawLine(goalColor, Offset(0f, gy), Offset(size.width, gy), strokeWidth = 2.dp.toPx())
        }
        if (avg > 0.0) {
            // Dashed line at the period average.
            val ay = size.height - (avg / maxV).toFloat() * size.height
            drawLine(
                color, Offset(0f, ay), Offset(size.width, ay), strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
            )
        }
    }
}
