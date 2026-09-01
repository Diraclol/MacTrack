package com.dirac.mactrack.ui.feature.today

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dirac.mactrack.data.entity.MealEntry
import com.dirac.mactrack.data.food.foodEmoji
import com.dirac.mactrack.data.food.mealEntryDetail
import com.dirac.mactrack.ui.common.NumberPad
import com.dirac.mactrack.ui.common.PadAction
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

private val CalorieColor = Color(0xFFFF9800)
private val ProteinColor = Color(0xFFE91E63)
private val FatColor = Color(0xFF4CAF50)
private val CarbColor = Color(0xFF2196F3)
private val SodiumColor = Color(0xFF26A69A)
private val PotassiumColor = Color(0xFF66BB6A)
private val FiberColor = Color(0xFF42A5F5)
private val CaffeineColor = Color(0xFFAB47BC)

// Soft daily reference targets for the micronutrient mini-bars (a scale, not a user goal).
private const val SodiumTargetMg = 2300.0
private const val PotassiumTargetMg = 3400.0
private const val FiberTargetG = 28.0
private const val CaffeineTargetMg = 400.0

private fun oneDecimal(x: Double): String = String.format(Locale.US, "%.1f", x)

private fun servings(amount: Double): String =
    if (amount % 1.0 == 0.0) amount.toInt().toString() else amount.toString()

private fun hourLabel(hour: Int): String {
    val h12 = if (hour % 12 == 0) 12 else hour % 12
    val ampm = if (hour < 12) "AM" else "PM"
    return "$h12 $ampm"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(onOpenSearch: () -> Unit, onOpenEntry: (String) -> Unit, modifier: Modifier = Modifier) {
    val viewModel: MealLogViewModel = viewModel(factory = MealLogViewModel.Factory)
    val entries by viewModel.todayEntries.collectAsState()
    val goal by viewModel.goal.collectAsState()

    val totalCal = entries.sumOf { it.calories }
    val totalP = entries.sumOf { it.proteinG }
    val totalF = entries.sumOf { it.fatG }
    val totalC = entries.sumOf { it.carbG }
    val totalSodium = entries.sumOf { it.sodiumMg }
    val totalPotassium = entries.sumOf { it.potassiumMg }
    val totalFiber = entries.sumOf { it.fiberG }
    // Caffeine isn't a tracked column yet (needs a schema field); shows 0 until that lands.
    val totalCaffeine = 0.0

    val byHour = entries.groupBy { it.timeMinutes / 60 }.toSortedMap()

    var editing by remember { mutableStateOf<MealEntry?>(null) }

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
            item {
                NutrientBox(
                    sodiumMg = totalSodium,
                    potassiumMg = totalPotassium,
                    fiberG = totalFiber,
                    caffeineMg = totalCaffeine
                )
            }
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
                val hourP = hourEntries.sumOf { it.proteinG }
                val hourC = hourEntries.sumOf { it.carbG }
                val hourF = hourEntries.sumOf { it.fatG }
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
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            MacroPill("${hourP.roundToInt()}P", ProteinColor)
                            MacroPill("${hourC.roundToInt()}C", CarbColor)
                            MacroPill("${hourF.roundToInt()}F", FatColor)
                            MacroPill("${hourCal.roundToInt()}", CalorieColor, Icons.Filled.LocalFireDepartment)
                        }
                    }
                }
                items(items = hourEntries, key = { it.id }) { entry ->
                    FoodCard(
                        entry = entry,
                        onClick = { editing = entry },
                        onDelete = { viewModel.deleteEntry(entry) }
                    )
                }
            }
        }

        Surface(
            onClick = onOpenSearch,
            shape = RoundedCornerShape(28.dp),
            color = Color.Transparent,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
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

    val e = editing
    if (e != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val editDetail by viewModel.editDetail.collectAsState()
        // Synchronous snapshot (the single logged unit) shown until the full portion list loads.
        val snapshotDetail = remember(e) { mealEntryDetail(e) }
        val detail = editDetail ?: snapshotDetail
        var amount by remember(e) { mutableStateOf(servings(e.amount)) }
        var selectedUnit by remember(e) { mutableStateOf(e.unitLabel ?: e.unit) }
        // First key press replaces the prefilled amount instead of appending (matches the
        // food detail screen's pad).
        var amountFresh by remember(e) { mutableStateOf(true) }
        LaunchedEffect(e) { viewModel.loadEditDetail(e) }
        ModalBottomSheet(
            onDismissRequest = { editing = null; viewModel.clearEditDetail() },
            sheetState = sheetState
        ) {
            Text(
                e.foodName,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            NumberPad(
                value = amount,
                onValueChange = { new ->
                    val next = if (amountFresh && new.length > amount.length) new.drop(amount.length) else new
                    amount = if (next.startsWith(".")) "0$next" else next
                    amountFresh = false
                },
                units = detail.units.map { it.label },
                selectedUnit = selectedUnit,
                onUnitSelect = { selectedUnit = it },
                actions = listOf(
                    PadAction("Details", onClick = {
                        editing = null
                        viewModel.clearEditDetail()
                        onOpenEntry(e.id)
                    }),
                    PadAction("Done", primary = true, onClick = {
                        val amt = amount.toDoubleOrNull() ?: 0.0
                        val unit = detail.units.find { it.label == selectedUnit } ?: detail.units.firstOrNull()
                        if (amt > 0.0 && unit != null) viewModel.updateEntry(e, amt, unit)
                        editing = null
                        viewModel.clearEditDetail()
                    })
                ),
                modifier = Modifier.padding(bottom = 24.dp)
            )
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

// A row of compact micronutrient cards (value + a mini bar vs a reference target).
@Composable
private fun NutrientBox(sodiumMg: Double, potassiumMg: Double, fiberG: Double, caffeineMg: Double) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NutrientCard("Sodium", "${sodiumMg.roundToInt()} mg", (sodiumMg / SodiumTargetMg).coerceIn(0.0, 1.0).toFloat(), SodiumColor)
        NutrientCard("Potassium", "${potassiumMg.roundToInt()} mg", (potassiumMg / PotassiumTargetMg).coerceIn(0.0, 1.0).toFloat(), PotassiumColor)
        NutrientCard("Dietary Fiber", "${oneDecimal(fiberG)} g", (fiberG / FiberTargetG).coerceIn(0.0, 1.0).toFloat(), FiberColor)
        NutrientCard("Caffeine", "${caffeineMg.roundToInt()} mg", (caffeineMg / CaffeineTargetMg).coerceIn(0.0, 1.0).toFloat(), CaffeineColor)
    }
}

@Composable
private fun RowScope.NutrientCard(label: String, value: String, fraction: Float, color: Color) {
    Card(modifier = Modifier.weight(1f)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
            LinearProgressIndicator(
                progress = { fraction },
                color = color,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
            )
        }
    }
}

// A small outlined pill for a macro (or calories, with a flame icon) on the hour header.
@Composable
private fun MacroPill(text: String, color: Color, icon: ImageVector? = null) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
        }
        Text(text, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

@Composable
private fun FoodCard(entry: MealEntry, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(foodEmoji(entry.foodName), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(end = 12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    entry.foodName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${entry.proteinG.roundToInt()}P ${entry.fatG.roundToInt()}F ${entry.carbG.roundToInt()}C · ${servings(entry.quantity)} ${entry.unit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "${entry.calories.roundToInt()} cal",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Remove ${entry.foodName}", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}