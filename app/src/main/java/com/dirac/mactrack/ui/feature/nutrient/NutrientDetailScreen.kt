package com.dirac.mactrack.ui.feature.nutrient

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dirac.mactrack.data.entity.MealEntry
import com.dirac.mactrack.ui.common.BackBar
import com.dirac.mactrack.ui.feature.trends.TrendPeriod
import com.dirac.mactrack.ui.theme.SodiumColor
import com.dirac.mactrack.ui.theme.PotassiumColor
import com.dirac.mactrack.ui.theme.FiberColor
import com.dirac.mactrack.ui.theme.CaffeineColor
import java.time.LocalDate
import kotlin.math.roundToInt

private data class NutrientSpec(
    val key: String,
    val label: String,
    val unit: String,
    val target: Double,
    val color: Color,
    val selector: (MealEntry) -> Double
)

private val NUTRIENTS = listOf(
    NutrientSpec("sodium", "Sodium", "mg", 2300.0, SodiumColor) { it.sodiumMg },
    NutrientSpec("potassium", "Potassium", "mg", 3400.0, PotassiumColor) { it.potassiumMg },
    NutrientSpec("fiber", "Fiber", "g", 28.0, FiberColor) { it.fiberG },
    NutrientSpec("caffeine", "Caffeine", "mg", 400.0, CaffeineColor) { it.caffeineMg }
)

// Time-range pills for the chart, mirroring the Trends (Cals + Macros) screen (no "All": the ViewModel
// loads a year).
private val NUTRIENT_PERIODS = listOf(TrendPeriod.W1, TrendPeriod.M1, TrendPeriod.M3, TrendPeriod.M6, TrendPeriod.Y1)

private fun fmt(x: Double): String = if (x >= 100) x.roundToInt().toString() else (Math.round(x * 10.0) / 10.0).toString()

@Composable
fun NutrientDetailScreen(nutrientKey: String, onBack: () -> Unit = {}, modifier: Modifier = Modifier) {
    var selectedKey by remember(nutrientKey) { mutableStateOf(nutrientKey) }
    var period by remember { mutableStateOf(TrendPeriod.M1) }
    val spec = NUTRIENTS.find { it.key == selectedKey } ?: NUTRIENTS.first()
    val vm: NutrientDetailViewModel = viewModel(factory = NutrientDetailViewModel.Factory)
    val entries by vm.entries.collectAsState()

    val today = LocalDate.now().toString()
    val todayTotal = entries.filter { it.date == today }.sumOf { spec.selector(it) }

    // Daily series over the selected period (missing days filled with 0), oldest -> newest.
    val byDate = entries.groupBy { it.date }.mapValues { (_, list) -> list.sumOf { spec.selector(it) } }
    val days = (period.days ?: 365L).toInt()
    val series = (0 until days).map { i ->
        byDate[LocalDate.now().minusDays(days - 1L - i).toString()] ?: 0.0
    }

    // Today's contributors, summed per food, biggest first.
    val contributors = entries.filter { it.date == today && spec.selector(it) > 0.0 }
        .groupBy { it.foodName }
        .map { (name, list) -> name to list.sumOf { spec.selector(it) } }
        .sortedByDescending { it.second }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { BackBar("Nutrients", onBack) }
        item {
            // Switch between the tracked micronutrients. The four chips share one row (each takes an
            // equal quarter) so they all fit on screen instead of wrapping to a second line.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NUTRIENTS.forEach { n ->
                    NutrientChip(
                        label = n.label,
                        selected = n.key == selectedKey,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedKey = n.key }
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Today", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${fmt(todayTotal)} ${spec.unit}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    LinearProgressIndicator(
                        progress = { (todayTotal / spec.target).coerceIn(0.0, 1.0).toFloat() },
                        color = spec.color,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Reference: ${fmt(spec.target)} ${spec.unit}/day",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NUTRIENT_PERIODS.forEach { p ->
                    FilterChip(selected = period == p, onClick = { period = p }, label = { Text(p.label) })
                }
            }
        }
        item { Text("Daily total", style = MaterialTheme.typography.titleSmall) }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.fillMaxWidth().height(180.dp).padding(16.dp)) {
                    NutrientBarChart(series = series, target = spec.target, color = spec.color, modifier = Modifier.fillMaxSize())
                }
            }
        }

        item { Text("Today's contributors", style = MaterialTheme.typography.titleSmall) }
        if (contributors.isEmpty()) {
            item {
                Text(
                    "Nothing logged with ${spec.label.lowercase()} today.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(items = contributors, key = { it.first }) { (name, amount) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(name, style = MaterialTheme.typography.bodyMedium)
                    Text("${fmt(amount)} ${spec.unit}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// A compact selectable chip, smaller than a Material FilterChip, so four fit across one row. Selected
// = filled accent + white label; unselected = outlined. Text ellipsises rather than wrapping.
@Composable
private fun NutrientChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    val bg = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Box(
        modifier = modifier
            .clip(shape)
            .border(1.dp, borderColor, shape)
            .background(bg)
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = fg,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun NutrientBarChart(series: List<Double>, target: Double, color: Color, modifier: Modifier = Modifier) {
    val goalColor = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier = modifier) {
        if (series.isEmpty()) return@Canvas
        val maxV = maxOf(series.max(), target, 1.0)
        val n = series.size
        val gap = if (n <= 60) 2.dp.toPx() else 0f
        val barW = ((size.width - gap * (n - 1)) / n).coerceAtLeast(1f)
        series.forEachIndexed { i, v ->
            val h = (v / maxV).toFloat() * size.height
            val x = i * (barW + gap)
            drawRect(color = color, topLeft = Offset(x, size.height - h), size = Size(barW, h))
        }
        if (target > 0.0) {
            val gy = size.height - (target / maxV).toFloat() * size.height
            drawLine(goalColor, Offset(0f, gy), Offset(size.width, gy), strokeWidth = 2.dp.toPx())
        }
    }
}
