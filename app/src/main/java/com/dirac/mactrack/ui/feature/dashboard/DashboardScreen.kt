package com.dirac.mactrack.ui.feature.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dirac.mactrack.data.entity.Goal
import com.dirac.mactrack.ui.theme.ThemeViewModel
import java.time.LocalDate
import kotlin.math.roundToInt
import java.time.format.TextStyle
import java.util.Locale

private val ProteinColor = Color(0xFFE91E63)
private val CarbColor = Color(0xFF2196F3)
private val FatColor = Color(0xFF4CAF50)
private val CalorieColor = Color(0xFFFF9800)
private val SodiumColor = Color(0xFF26A69A)
private val PotassiumColor = Color(0xFF66BB6A)
private val FiberColor = Color(0xFF42A5F5)
private val CaffeineColor = Color(0xFFAB47BC)

// Soft daily references for the nutrient bars (a scale, not a user goal) — same as the food log.
private const val SodiumRefMg = 2300.0
private const val PotassiumRefMg = 3400.0
private const val FiberRefG = 28.0
private const val CaffeineRefMg = 400.0

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    onOpenProfile: () -> Unit = {},
    onOpenTrends: () -> Unit = {},
    onOpenNutrient: (String) -> Unit = {}
) {
    val viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory)
    val themeViewModel: ThemeViewModel = viewModel(factory = ThemeViewModel.Factory)
    val goal by viewModel.goal.collectAsState()
    val avatar by themeViewModel.avatar.collectAsState()
    val weeklyAvg by viewModel.weeklyAvg.collectAsState()
    val weeklyNutrientAvg by viewModel.weeklyNutrientAvg.collectAsState()
    val loggedDates by viewModel.loggedDates.collectAsState()

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            val today = LocalDate.now()
            val dateLabel = "${today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()).uppercase()}, " +
                    "${today.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).uppercase()} ${today.dayOfMonth}"
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        dateLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("Dashboard", style = MaterialTheme.typography.headlineSmall)
                }
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable { onOpenProfile() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(avatar, style = MaterialTheme.typography.titleLarge)
                }
            }
        }
        item { MacroCard(avg = weeklyAvg, goal = goal, onClick = onOpenTrends) }
        item { NutrientCard(avg = weeklyNutrientAvg, onOpenNutrient = onOpenNutrient) }
        item { FoodStreakCard(loggedDates = loggedDates.toSet()) }
    }
}

@Composable
private fun CalorieCard(consumed: Double, target: Double) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Daily Nutrition", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val remaining = (target - consumed).roundToInt()
                StatNumber(value = if (target > 0) remaining.toString() else "—", label = "Remaining")
                CalorieRing(consumed = consumed, target = target)
                StatNumber(value = if (target > 0) target.roundToInt().toString() else "—", label = "Target")
            }
        }
    }
}

@Composable
private fun StatNumber(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CalorieRing(consumed: Double, target: Double) {
    val fraction = if (target > 0.0) (consumed / target).coerceIn(0.0, 1.0).toFloat() else 0f
    val track = MaterialTheme.colorScheme.surfaceVariant
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 16f
            val d = size.minDimension - stroke
            val tl = Offset((size.width - d) / 2f, (size.height - d) / 2f)
            val arcSize = Size(d, d)
            drawArc(color = track, startAngle = 0f, sweepAngle = 360f, useCenter = false, topLeft = tl, size = arcSize, style = Stroke(width = stroke))
            drawArc(color = CalorieColor, startAngle = -90f, sweepAngle = 360f * fraction, useCenter = false, topLeft = tl, size = arcSize, style = Stroke(width = stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${consumed.roundToInt()}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Consumed", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun MacroCard(avg: WeeklyAvg, goal: Goal?, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Cals + Macros", style = MaterialTheme.typography.titleMedium)
                Text("See more ›", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            MacroBar("Calories", avg.cal, goal?.calorieGoal ?: 0.0, CalorieColor, unit = "cal")
            MacroBar("Protein", avg.p, goal?.proteinGoalG ?: 0.0, ProteinColor)
            MacroBar("Fat", avg.f, goal?.fatGoalG ?: 0.0, FatColor)
            MacroBar("Carbs", avg.c, goal?.carbGoalG ?: 0.0, CarbColor)
            if (avg.days == 0) {
                Text(
                    "Log food to see your weekly average.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private data class NutrientTileData(
    val key: String, val label: String, val value: String, val fraction: Float, val color: Color
)

// The dashboard nutrients section: same tile look as the food log's nutrient box, but bigger — a 2x2
// grid of tappable tiles (7-day average vs each soft daily reference), inside a card with a See more
// header. Each tile opens that nutrient's detail.
@Composable
private fun NutrientCard(avg: NutrientAvg, onOpenNutrient: (String) -> Unit) {
    val tiles = listOf(
        NutrientTileData("sodium", "Sodium", "${avg.sodiumMg.roundToInt()} mg", (avg.sodiumMg / SodiumRefMg).coerceIn(0.0, 1.0).toFloat(), SodiumColor),
        NutrientTileData("potassium", "Potassium", "${avg.potassiumMg.roundToInt()} mg", (avg.potassiumMg / PotassiumRefMg).coerceIn(0.0, 1.0).toFloat(), PotassiumColor),
        NutrientTileData("fiber", "Dietary Fiber", "${avg.fiberG.roundToInt()} g", (avg.fiberG / FiberRefG).coerceIn(0.0, 1.0).toFloat(), FiberColor),
        NutrientTileData("caffeine", "Caffeine", "${avg.caffeineMg.roundToInt()} mg", (avg.caffeineMg / CaffeineRefMg).coerceIn(0.0, 1.0).toFloat(), CaffeineColor)
    )
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onOpenNutrient("sodium") },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Nutrients", style = MaterialTheme.typography.titleMedium)
                Text("See more ›", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            tiles.chunked(2).forEach { rowTiles ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowTiles.forEach { t ->
                        NutrientTile(t, Modifier.weight(1f)) { onOpenNutrient(t.key) }
                    }
                }
            }
            if (avg.days == 0) {
                Text(
                    "Log food to see your weekly nutrient average.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun NutrientTile(d: NutrientTileData, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clickable { onClick() },
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(d.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Text(d.value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
            LinearProgressIndicator(
                progress = { d.fraction },
                color = d.color,
                trackColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
            )
        }
    }
}

@Composable
private fun MacroBar(label: String, current: Double, goal: Double, color: Color, unit: String = "g") {
    val fraction = if (goal > 0.0) (current / goal).coerceIn(0.0, 1.0).toFloat() else 0f
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(
                if (goal > 0.0) "${current.roundToInt()} / ${goal.roundToInt()} $unit" else "${current.roundToInt()} $unit",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LinearProgressIndicator(progress = { fraction }, color = color, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun FoodStreakCard(loggedDates: Set<String>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Food Logging", style = MaterialTheme.typography.titleMedium)
            Text("Last 30 days", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val today = LocalDate.now()
            val days = (0 until 30).map { today.minusDays((29 - it).toLong()).toString() }
            val filled = MaterialTheme.colorScheme.primary
            val empty = MaterialTheme.colorScheme.surfaceVariant
            days.chunked(10).forEach { rowDays ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    rowDays.forEach { d ->
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (d in loggedDates) filled else empty)
                        )
                    }
                }
            }
            val count = days.count { it in loggedDates }
            Text("$count / 30 days logged", style = MaterialTheme.typography.bodySmall)
        }
    }
}