package com.dirac.mactrack.ui.feature.today

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.QrCodeScanner
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dirac.mactrack.data.entity.MealEntry
import com.dirac.mactrack.data.food.foodEmoji
import com.dirac.mactrack.data.food.mealEntryDetail
import com.dirac.mactrack.ui.common.NumberPad
import com.dirac.mactrack.ui.common.PadAction
import com.dirac.mactrack.ui.theme.ThemeViewModel
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

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

// How a logged portion reads. If the unit label leads with a number (e.g. "15ml", "1 slice"),
// multiply that number by the amount and keep the rest: 2 x "15ml" -> "30ml", 2 x "1 slice" ->
// "2 slice". Otherwise fall back to "amount label" (e.g. "2 serving", "150 g").
private fun displayQuantity(amount: Double, label: String): String {
    val m = Regex("""^(\d+(?:\.\d+)?)(.*)$""").find(label.trim())
    val base = m?.groupValues?.get(1)?.toDoubleOrNull()
    return if (m != null && base != null) servings(base * amount) + m.groupValues[2]
    else "${servings(amount)} $label"
}

private fun hourLabel(hour: Int): String {
    val h12 = if (hour % 12 == 0) 12 else hour % 12
    val ampm = if (hour < 12) "AM" else "PM"
    return "$h12 $ampm"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    onOpenSearch: () -> Unit,
    onOpenEntry: (String) -> Unit,
    onOpenNutrient: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val viewModel: MealLogViewModel = viewModel(factory = MealLogViewModel.Factory)
    val themeViewModel: ThemeViewModel = viewModel(factory = ThemeViewModel.Factory)
    val entries by viewModel.todayEntries.collectAsState()
    val goal by viewModel.goal.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val nutrientOrder by themeViewModel.nutrientOrder.collectAsState()

    val totalCal = entries.sumOf { it.calories }
    val totalP = entries.sumOf { it.proteinG }
    val totalF = entries.sumOf { it.fatG }
    val totalC = entries.sumOf { it.carbG }
    val totalSodium = entries.sumOf { it.sodiumMg }
    val totalPotassium = entries.sumOf { it.potassiumMg }
    val totalFiber = entries.sumOf { it.fiberG }
    val totalCaffeine = entries.sumOf { it.caffeineMg }

    val byHour = entries.groupBy { it.timeMinutes / 60 }.toSortedMap()

    var editing by remember { mutableStateOf<MealEntry?>(null) }
    // Totals view, cycled by swiping the row: 0 = "remaining", 1 = "eaten / goal", 2 = rings.
    var statMode by remember { mutableStateOf(0) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        DayNavigator(
            selected = selectedDate,
            onPrev = { viewModel.shiftDay(-1) },
            onNext = { viewModel.shiftDay(1) },
            onSelect = { viewModel.selectDate(it) },
            onToday = { viewModel.goToToday() }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .pointerInput(Unit) {
                    var acc = 0f
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            // Swipe left = next view, right = previous; wrap around 3 modes.
                            if (kotlin.math.abs(acc) > 40f) {
                                statMode = if (acc < 0f) (statMode + 1) % 3 else (statMode + 2) % 3
                            }
                            acc = 0f
                        },
                        onDragCancel = { acc = 0f }
                    ) { _, dragAmount -> acc += dragAmount }
                },
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (statMode == 2) {
                TotalRing(Modifier.weight(1f), "Cal", totalCal, goal?.calorieGoal ?: 0.0, CalorieColor)
                TotalRing(Modifier.weight(1f), "P", totalP, goal?.proteinGoalG ?: 0.0, ProteinColor)
                TotalRing(Modifier.weight(1f), "F", totalF, goal?.fatGoalG ?: 0.0, FatColor)
                TotalRing(Modifier.weight(1f), "C", totalC, goal?.carbGoalG ?: 0.0, CarbColor)
            } else {
                val totalMode = statMode == 1
                TotalStat(Modifier.weight(1f), "Cal", totalCal, goal?.calorieGoal ?: 0.0, CalorieColor, totalMode)
                TotalStat(Modifier.weight(1f), "P", totalP, goal?.proteinGoalG ?: 0.0, ProteinColor, totalMode)
                TotalStat(Modifier.weight(1f), "F", totalF, goal?.fatGoalG ?: 0.0, FatColor, totalMode)
                TotalStat(Modifier.weight(1f), "C", totalC, goal?.carbGoalG ?: 0.0, CarbColor, totalMode)
            }
        }

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                NutrientBox(
                    sodiumMg = totalSodium,
                    potassiumMg = totalPotassium,
                    fiberG = totalFiber,
                    caffeineMg = totalCaffeine,
                    order = nutrientOrder,
                    onReorder = { themeViewModel.setNutrientOrder(it) },
                    onOpenNutrient = onOpenNutrient
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
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Search for a food", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(Icons.Filled.QrCodeScanner, contentDescription = "Scan or enter a barcode", tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
            // Live totals: recompute as the amount or unit changes, and show the chosen unit
            // (not grams). unit.per is per one of the selected unit; amount is how many.
            val liveUnit = detail.units.find { it.label == selectedUnit } ?: detail.units.firstOrNull()
            val liveAmt = amount.toDoubleOrNull() ?: 0.0
            val live = liveUnit?.let { it.per * liveAmt }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("${(live?.protein ?: 0.0).roundToInt()}P", style = MaterialTheme.typography.bodyMedium, color = ProteinColor)
                    Text("${(live?.carb ?: 0.0).roundToInt()}C", style = MaterialTheme.typography.bodyMedium, color = CarbColor)
                    Text("${(live?.fat ?: 0.0).roundToInt()}F", style = MaterialTheme.typography.bodyMedium, color = FatColor)
                }
                Text(
                    "${(live?.kcal ?: 0.0).roundToInt()} cal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
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
private fun TotalStat(modifier: Modifier, label: String, consumed: Double, goal: Double, color: Color, totalMode: Boolean) {
    val has = goal > 0.0
    val left = (goal - consumed).roundToInt()
    // Number and its word share one line, e.g. "2654 left" / "2654 eaten" / "2654/3000".
    val line = when {
        !has -> "${consumed.roundToInt()} eaten"
        totalMode -> "${consumed.roundToInt()}/${goal.roundToInt()}"
        else -> "$left left"
    }
    // Bar scaled so the goal sits at 80% of the width; a perpendicular tick marks the goal, and the
    // fill runs past it (turning red) once you're over.
    val scaleMax = if (has) goal * 1.25 else 1.0
    val fillFrac = if (has) (consumed / scaleMax).coerceIn(0.0, 1.0).toFloat() else 0f
    val over = has && consumed > goal
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(line, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(fillFrac).fillMaxHeight().clip(RoundedCornerShape(3.dp))
                    .background(if (over) MaterialTheme.colorScheme.error else color)
            )
            if (has) {
                Box(
                    modifier = Modifier.offset(x = maxWidth * 0.8f).width(2.dp).fillMaxHeight()
                        .background(MaterialTheme.colorScheme.onSurface)
                )
            }
        }
    }
}

// The rings view of the totals (cycled to by swiping the totals row): one circular progress ring per
// macro toward its goal, over-goal turning red, with the consumed value in the center.
@Composable
private fun TotalRing(modifier: Modifier, label: String, consumed: Double, goal: Double, color: Color) {
    val has = goal > 0.0
    val frac = if (has) (consumed / goal).coerceIn(0.0, 1.0).toFloat() else 0f
    val over = has && consumed > goal
    val track = MaterialTheme.colorScheme.surfaceVariant
    val ringColor = if (over) MaterialTheme.colorScheme.error else color
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(58.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 7.dp.toPx()
                val d = size.minDimension - stroke
                val topLeft = Offset((size.width - d) / 2f, (size.height - d) / 2f)
                val arcSize = Size(d, d)
                drawArc(color = track, startAngle = 0f, sweepAngle = 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(width = stroke))
                if (frac > 0f) {
                    drawArc(color = ringColor, startAngle = -90f, sweepAngle = 360f * frac, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(width = stroke, cap = StrokeCap.Round))
                }
            }
            Text("${consumed.roundToInt()}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1)
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private data class NutrientDatum(
    val key: String, val label: String, val value: String, val fraction: Float, val color: Color
)

// A row of compact micronutrient cards. Long-press a card and drag it left/right to reorder;
// the order persists (theme prefs). Each card shows a value + a mini bar vs a soft target.
@Composable
private fun NutrientBox(
    sodiumMg: Double,
    potassiumMg: Double,
    fiberG: Double,
    caffeineMg: Double,
    order: List<String>,
    onReorder: (List<String>) -> Unit,
    onOpenNutrient: (String) -> Unit
) {
    val data = mapOf(
        "sodium" to NutrientDatum("sodium", "Sodium", "${sodiumMg.roundToInt()} mg", (sodiumMg / SodiumTargetMg).coerceIn(0.0, 1.0).toFloat(), SodiumColor),
        "potassium" to NutrientDatum("potassium", "Potassium", "${potassiumMg.roundToInt()} mg", (potassiumMg / PotassiumTargetMg).coerceIn(0.0, 1.0).toFloat(), PotassiumColor),
        "fiber" to NutrientDatum("fiber", "Dietary Fiber", "${oneDecimal(fiberG)} g", (fiberG / FiberTargetG).coerceIn(0.0, 1.0).toFloat(), FiberColor),
        "caffeine" to NutrientDatum("caffeine", "Caffeine", "${caffeineMg.roundToInt()} mg", (caffeineMg / CaffeineTargetMg).coerceIn(0.0, 1.0).toFloat(), CaffeineColor)
    )
    // Sanitize the incoming order against known keys, then keep a local working copy for the
    // drag (swaps happen as the dragged card crosses a neighbour); persist on drag end.
    val safeOrder = order.filter { data.containsKey(it) } + data.keys.filter { it !in order }
    var working by remember(safeOrder) { mutableStateOf(safeOrder) }
    var dragKey by remember { mutableStateOf<String?>(null) }
    var dragDx by remember { mutableStateOf(0f) }

    val spacing = 8.dp
    val density = LocalDensity.current
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val n = working.size
        // Center-to-center distance between adjacent equal-width cards.
        val stepPx = with(density) { ((maxWidth - spacing * (n - 1)) / n + spacing).toPx() }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
            working.forEach { key ->
                val d = data[key] ?: return@forEach
                val dragging = key == dragKey
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .zIndex(if (dragging) 1f else 0f)
                        .offset { IntOffset(if (dragging) dragDx.roundToInt() else 0, 0) }
                        .pointerInput(key) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { dragKey = key; dragDx = 0f },
                                onDragEnd = { dragKey = null; dragDx = 0f; onReorder(working) },
                                onDragCancel = { dragKey = null; dragDx = 0f },
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragDx += amount.x
                                    val idx = working.indexOf(key)
                                    if (dragDx > stepPx / 2f && idx < working.size - 1) {
                                        working = working.toMutableList().also { it.add(idx + 1, it.removeAt(idx)) }
                                        dragDx -= stepPx
                                    } else if (dragDx < -stepPx / 2f && idx > 0) {
                                        working = working.toMutableList().also { it.add(idx - 1, it.removeAt(idx)) }
                                        dragDx += stepPx
                                    }
                                }
                            )
                        }
                ) {
                    NutrientCard(d, onClick = { onOpenNutrient(d.key) })
                }
            }
        }
    }
}

@Composable
private fun NutrientCard(d: NutrientDatum, onClick: () -> Unit = {}) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                d.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
            Text(d.value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
            LinearProgressIndicator(
                progress = { d.fraction },
                color = d.color,
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

private fun fullDate(d: LocalDate): String =
    "${d.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()).uppercase()}, " +
        "${d.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).uppercase()} ${d.dayOfMonth}"

private fun relativeLabel(d: LocalDate, today: LocalDate): String = when (d) {
    today -> "Today"
    today.minusDays(1) -> "Yesterday"
    today.plusDays(1) -> "Tomorrow"
    else -> d.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
}

// Date header: prev/next arrows around the day label (tap the label to jump back to today) plus a
// scrollable strip of days; the selected day is filled, every other day is outlined so it reads.
@Composable
private fun DayNavigator(
    selected: LocalDate,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSelect: (LocalDate) -> Unit,
    onToday: () -> Unit
) {
    val today = LocalDate.now()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrev) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous day")
            }
            Column(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).clickable { onToday() },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(fullDate(selected), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(relativeLabel(selected, today), style = MaterialTheme.typography.headlineSmall)
            }
            IconButton(onClick = onNext) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next day")
            }
        }
        DayStrip(selected, today, onSelect)
    }
}

// Week-block strip: pages a whole week at a time (Mon-Sun) instead of a free scroll. The page holding
// the selected day is shown; swiping moves in one-week jumps.
@Composable
private fun DayStrip(selected: LocalDate, today: LocalDate, onSelect: (LocalDate) -> Unit) {
    // Weeks start on Sunday.
    val sunday = java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.SUNDAY)
    val anchorSunday = remember(today) { today.with(sunday) }
    val pageCount = 209            // ~2 years back and forward, in weeks
    val todayPage = 104
    val selectedSunday = selected.with(sunday)
    val selectedPage = remember(selected) {
        (todayPage + java.time.temporal.ChronoUnit.WEEKS.between(anchorSunday, selectedSunday).toInt())
            .coerceIn(0, pageCount - 1)
    }
    val pagerState = rememberPagerState(initialPage = selectedPage) { pageCount }
    LaunchedEffect(selectedPage) {
        if (pagerState.currentPage != selectedPage) pagerState.animateScrollToPage(selectedPage)
    }
    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth()) { page ->
        val weekSunday = anchorSunday.plusWeeks((page - todayPage).toLong())
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            (0L..6L).forEach { i ->
                val d = weekSunday.plusDays(i)
                DayCell(d, d == selected, d == today, Modifier.weight(1f)) { onSelect(d) }
            }
        }
    }
}

@Composable
private fun DayCell(d: LocalDate, isSel: Boolean, isToday: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val skin = when {
        isSel -> Modifier.background(MaterialTheme.colorScheme.primary)
        isToday -> Modifier.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
        else -> Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .then(skin)
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            d.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
            style = MaterialTheme.typography.labelSmall,
            color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "${d.dayOfMonth}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        )
    }
}

// Swipe a row left to REVEAL a red delete panel and rest there; tap the trash to confirm the delete.
// Tapping the row while it's open just closes it; tapping while closed opens the entry. No auto-delete.
@Composable
private fun FoodCard(entry: MealEntry, onClick: () -> Unit, onDelete: () -> Unit) {
    val revealPx = with(LocalDensity.current) { 76.dp.toPx() }
    val offsetX = remember(entry.id) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val dragState = rememberDraggableState { delta ->
        scope.launch { offsetX.snapTo((offsetX.value + delta).coerceIn(-revealPx, 0f)) }
    }
    Box(modifier = Modifier.fillMaxWidth()) {
        // Red delete panel behind the row; its trash button is the confirm action.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.errorContainer),
            contentAlignment = Alignment.CenterEnd
        ) {
            IconButton(onClick = onDelete, modifier = Modifier.padding(end = 12.dp)) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete ${entry.foodName}",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = dragState,
                    onDragStopped = {
                        val target = if (offsetX.value < -revealPx / 2f) -revealPx else 0f
                        offsetX.animateTo(target)
                    }
                )
                .clickable {
                    if (offsetX.value != 0f) scope.launch { offsetX.animateTo(0f) } else onClick()
                }
        ) {
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
                        "${entry.proteinG.roundToInt()}P ${entry.fatG.roundToInt()}F ${entry.carbG.roundToInt()}C · ${displayQuantity(entry.amount, entry.unitLabel ?: entry.unit)}",
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
            }
        }
    }
}