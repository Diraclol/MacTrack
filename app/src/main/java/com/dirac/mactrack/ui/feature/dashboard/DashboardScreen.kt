package com.dirac.mactrack.ui.feature.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dirac.mactrack.R
import com.dirac.mactrack.data.entity.Goal
import com.dirac.mactrack.data.entity.WeightEntry
import com.dirac.mactrack.ui.common.ProfileAvatar
import com.dirac.mactrack.ui.theme.ThemeViewModel
import com.dirac.mactrack.ui.theme.ProteinColor
import com.dirac.mactrack.ui.theme.CarbColor
import com.dirac.mactrack.ui.theme.FatColor
import com.dirac.mactrack.ui.theme.CalorieColor
import com.dirac.mactrack.ui.theme.SodiumColor
import com.dirac.mactrack.ui.theme.PotassiumColor
import com.dirac.mactrack.ui.theme.FiberColor
import com.dirac.mactrack.ui.theme.CaffeineColor
import java.time.LocalDate
import kotlin.math.roundToInt
import java.time.format.TextStyle
import java.util.Locale

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
    onOpenNutrient: (String) -> Unit = {},
    onOpenStreak: () -> Unit = {}
) {
    val viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory)
    val themeViewModel: ThemeViewModel = viewModel(factory = ThemeViewModel.Factory)
    val goal by viewModel.goal.collectAsState()
    val avatar by themeViewModel.avatar.collectAsState()
    val avatarPhoto by themeViewModel.avatarPhotoPath.collectAsState()
    val weeklyAvg by viewModel.weeklyAvg.collectAsState()
    val weeklyNutrientAvg by viewModel.weeklyNutrientAvg.collectAsState()
    val loggedDates by viewModel.loggedDates.collectAsState()
    val weights by viewModel.weights.collectAsState()
    val showWeightGraph by themeViewModel.dashboardWeightGraph.collectAsState()

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.ic_mactrack_logo),
                            contentDescription = null,
                            modifier = Modifier.size(40.dp)
                        )
                        Text("Dashboard", style = MaterialTheme.typography.headlineSmall)
                    }
                }
                ProfileAvatar(
                    emoji = avatar,
                    photoPath = avatarPhoto,
                    size = 40.dp,
                    emojiStyle = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.clickable { onOpenProfile() }
                )
            }
        }
        item { MacroCard(avg = weeklyAvg, goal = goal, onClick = onOpenTrends) }
        item { NutrientCard(avg = weeklyNutrientAvg, onOpenNutrient = onOpenNutrient) }
        item { FoodStreakCard(loggedDates = loggedDates.toSet(), onClick = onOpenStreak) }
        if (showWeightGraph) {
            item { WeightTrendCard(weights = weights) }
        }
    }
}

@Composable
private fun MacroCard(avg: WeeklyAvg, goal: Goal?, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
        NutrientTileData("fiber", "Fiber", "${avg.fiberG.roundToInt()} g", (avg.fiberG / FiberRefG).coerceIn(0.0, 1.0).toFloat(), FiberColor),
        NutrientTileData("caffeine", "Caffeine", "${avg.caffeineMg.roundToInt()} mg", (avg.caffeineMg / CaffeineRefMg).coerceIn(0.0, 1.0).toFloat(), CaffeineColor)
    )
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onOpenNutrient("sodium") },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Nutrients", style = MaterialTheme.typography.titleMedium)
                Text("See more ›", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            // One compact row of four (instead of a 2x2 grid) so the card is short enough to leave the
            // Food Logging card visible on the dashboard.
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tiles.forEach { t ->
                    NutrientTile(t, Modifier.weight(1f)) { onOpenNutrient(t.key) }
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
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(d.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Text(d.value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
            LinearProgressIndicator(
                progress = { d.fraction },
                color = d.color,
                trackColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp))
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
private fun FoodStreakCard(loggedDates: Set<String>, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Food Logging", style = MaterialTheme.typography.titleMedium)
                Text("See more ›", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            Text("Last 30 days", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val today = LocalDate.now()
            val days = (0 until 30).map { today.minusDays((29 - it).toLong()).toString() }
            val filled = MaterialTheme.colorScheme.primary
            val empty = MaterialTheme.colorScheme.surfaceVariant
            days.chunked(10).forEach { rowDays ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    rowDays.forEach { d ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
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

private fun oneDecimal(x: Double): String = (Math.round(x * 10.0) / 10.0).toString()

// Optional dashboard card (toggled in More -> Display): current weight + a compact trend line.
@Composable
private fun WeightTrendCard(weights: List<WeightEntry>) {
    val sorted = weights.sortedBy { it.date }
    val latest = sorted.lastOrNull()
    val prev = if (sorted.size >= 2) sorted[sorted.size - 2] else null
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Weight", style = MaterialTheme.typography.titleMedium)
                latest?.let {
                    Text("${oneDecimal(it.weightKg)} kg", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
            if (sorted.size < 2) {
                Text(
                    "Log at least two weigh-ins to see a trend.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val lineColor = MaterialTheme.colorScheme.primary
                Box(modifier = Modifier.fillMaxWidth().height(90.dp)) {
                    WeightSparkline(sorted, lineColor, Modifier.fillMaxSize())
                }
                prev?.let {
                    val delta = latest!!.weightKg - it.weightKg
                    val sign = if (delta > 0) "+" else ""
                    Text(
                        "$sign${oneDecimal(delta)} kg since last",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun WeightSparkline(points: List<WeightEntry>, color: Color, modifier: Modifier) {
    val days = points.map { LocalDate.parse(it.date).toEpochDay() }
    val minW = points.minOf { it.weightKg }
    val maxW = points.maxOf { it.weightKg }
    val span = (maxW - minW).takeIf { it > 0.0 } ?: 1.0
    val minX = days.min()
    val maxX = days.max()
    val xSpan = (maxX - minX).takeIf { it > 0L } ?: 1L
    Canvas(modifier = modifier) {
        val pad = 6.dp.toPx()
        val w = size.width - 2 * pad
        val h = size.height - 2 * pad
        val path = Path()
        points.indices.forEach { i ->
            val x = pad + (days[i] - minX).toFloat() / xSpan * w
            val y = pad + (1f - ((points[i].weightKg - minW) / span).toFloat()) * h
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
    }
}