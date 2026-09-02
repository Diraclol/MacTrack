package com.dirac.mactrack.ui.feature.foodsearch

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dirac.mactrack.data.food.foodEmoji
import com.dirac.mactrack.ui.common.BackBar
import com.dirac.mactrack.ui.common.NumberPad
import com.dirac.mactrack.ui.common.PadAction
import java.time.LocalTime
import kotlin.math.roundToInt

private val ProteinColor = Color(0xFFE91E63)
private val CarbColor = Color(0xFF2196F3)
private val FatColor = Color(0xFF4CAF50)
private val CalorieColor = Color(0xFFFF9800)

private fun fmtAmount(a: Double): String =
    if (a % 1.0 == 0.0) a.toInt().toString() else a.toString()

@Composable
fun FoodDetailScreen(
    source: String,
    id: String,
    onLogged: () -> Unit,
    onAdded: () -> Unit = onLogged,
    onBack: () -> Unit = {},
    onScanAgain: () -> Unit = {},
    onCreateFoodWithBarcode: (String) -> Unit = {},
    onEditFood: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val viewModel: FoodDetailViewModel = viewModel(factory = FoodDetailViewModel.Factory)
    val detail by viewModel.detail.collectAsState()
    val loaded by viewModel.loaded.collectAsState()
    val goal by viewModel.goal.collectAsState()
    val todayEntries by viewModel.todayEntries.collectAsState()

    LaunchedEffect(source, id) { viewModel.load(source, id) }

    val d = detail
    if (d == null || d.units.isEmpty()) {
        Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            when {
                !loaded -> Text("Loading…")
                // A scanned/looked-up barcode that Open Food Facts doesn't know: offer to scan again
                // or create a food carrying this barcode.
                source == "branded" -> AlertDialog(
                    onDismissRequest = onBack,
                    title = { Text("Barcode not recognized") },
                    text = { Text("We couldn't find this barcode in Open Food Facts. Scan again, or create a food with this barcode.") },
                    confirmButton = { TextButton(onClick = { onCreateFoodWithBarcode(id) }) { Text("Create food") } },
                    dismissButton = { TextButton(onClick = onScanAgain) { Text("Scan again") } }
                )
                else -> Text("Couldn't find that food.")
            }
        }
        return
    }

    var menuOpen by remember { mutableStateOf(false) }
    var amount by remember(d) { mutableStateOf(fmtAmount(d.defaultAmount)) }
    var unitLabel by remember(d) { mutableStateOf(d.defaultUnitLabel) }
    var padOpen by remember(d) { mutableStateOf(true) }
    var amountFresh by remember(d) { mutableStateOf(true) }

    val unit = d.units.find { it.label == unitLabel } ?: d.units.first()
    val amt = amount.toDoubleOrNull() ?: 0.0
    val s = unit.per * amt

    val pC = unit.per.protein * 4; val fC = unit.per.fat * 9; val cC = unit.per.carb * 4
    val tot = (pC + fC + cC).let { if (it <= 0.0) 1.0 else it }

    // What is left of today's goal, before this food gets logged. null = no goal saved yet.
    val totalCal = todayEntries.sumOf { it.calories }
    val totalP = todayEntries.sumOf { it.proteinG }
    val totalC = todayEntries.sumOf { it.carbG }
    val totalF = todayEntries.sumOf { it.fatG }
    val remCal = goal?.let { (it.calorieGoal - totalCal).coerceAtLeast(0.0) }
    val remP = goal?.let { (it.proteinGoalG - totalP).coerceAtLeast(0.0) }
    val remC = goal?.let { (it.carbGoalG - totalC).coerceAtLeast(0.0) }
    val remF = goal?.let { (it.fatGoalG - totalF).coerceAtLeast(0.0) }

    fun doLog() {
        val now = LocalTime.now()
        viewModel.log(amt, unit, now.hour * 60 + now.minute, onLogged)
    }
    fun doAdd() { viewModel.addToCart(amt, unit); onAdded() }
    fun doDone() { if (amt > 0.0) viewModel.updateEntry(amt, unit, onLogged) else onLogged() }
    val isEntry = source == "entry"

    // Show the gram weight next to a named serving (e.g. "1 thigh (68 g)") so the size is legible.
    // Gram/millilitre base units and unknown-weight units are shown as-is. This only affects display;
    // the stored unit label stays the plain key.
    fun labelWithWeight(label: String): String {
        val g = d.units.find { it.label == label }?.grams
        return if (g != null && g > 0.0 && label != "g" && label != "ml") "$label (${g.roundToInt()} g)" else label
    }

    // First key press after the pad opens replaces the prefilled amount instead of appending.
    fun onPadValue(new: String) {
        val next = if (amountFresh && new.length > amount.length) new.drop(amount.length) else new
        amount = if (next.startsWith(".")) "0$next" else next
        amountFresh = false
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            BackBar(
                "${foodEmoji(d.name)}  ${d.name}",
                onBack,
                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 8.dp),
                actions = {
                    // A food can be duplicated into an editable custom copy (near-match labels, or to
                    // tweak an AI-logged item). Recipes are excluded here -- duplicating a recipe into
                    // the recipe editor is a separate action. Your own custom food also gets a plain Edit.
                    if (source != "recipe") {
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text("Duplicate & edit") },
                                    onClick = {
                                        menuOpen = false
                                        viewModel.duplicateAsFood { newId -> onEditFood(newId) }
                                    }
                                )
                                if (source == "custom") {
                                    DropdownMenuItem(
                                        text = { Text("Edit") },
                                        onClick = { menuOpen = false; onEditFood(id) }
                                    )
                                }
                            }
                        }
                    }
                }
            )

            // scrollable content
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${s.kcal.roundToInt()}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("Calories", style = MaterialTheme.typography.labelSmall)
                    }
                    ShareRing("Protein", (pC / tot * 100).roundToInt(), s.protein, ProteinColor)
                    ShareRing("Fat", (fC / tot * 100).roundToInt(), s.fat, FatColor)
                    ShareRing("Carbs", (cC / tot * 100).roundToInt(), s.carb, CarbColor)
                }

                ContributionCard(
                    title = if (isEntry) "Contribution to Daily Goal" else "Contribution to Remaining Daily Macros",
                    protein = s.protein, carb = s.carb, fat = s.fat, calories = s.kcal,
                    remProtein = if (isEntry) goal?.proteinGoalG else remP,
                    remCarb = if (isEntry) goal?.carbGoalG else remC,
                    remFat = if (isEntry) goal?.fatGoalG else remF,
                    remCalories = if (isEntry) goal?.calorieGoal else remCal
                )

                HorizontalDivider()

                Text("Nutrition (this serving)", style = MaterialTheme.typography.titleSmall)
                MicroRow("Fiber", s.fiber, "g")
                MicroRow("Sugar", s.sugar, "g")
                MicroRow("Saturated fat", s.satFat, "g")
                MicroRow("Sodium", s.sodium, "mg")
                MicroRow("Potassium", s.potassium, "mg")
                MicroRow("Cholesterol", s.cholesterol, "mg")
                MicroRow("Caffeine", s.caffeine, "mg")
            }

            // docked bottom bar
            Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp, shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                            .clickable { padOpen = true; amountFresh = true }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(amount.ifEmpty { "0" }, fontWeight = FontWeight.Bold)
                            Text(labelWithWeight(unitLabel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (isEntry) {
                        Button(onClick = { doDone() }) { Text("Done") }
                    } else {
                        OutlinedButton(onClick = { doLog() }) { Text("Log") }
                        Button(onClick = { doAdd() }) { Text("Add") }
                    }
                }
            }
        }

        // number pad popup + dismiss scrim
        if (padOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.32f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { padOpen = false }
            )
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 12.dp
            ) {
                NumberPad(
                    value = amount,
                    onValueChange = { onPadValue(it) },
                    units = d.units.map { it.label },
                    selectedUnit = unitLabel,
                    onUnitSelect = { unitLabel = it },
                    unitDisplay = { labelWithWeight(it) },
                    actions = if (isEntry) listOf(
                        PadAction("Done", primary = true, onClick = { doDone() })
                    ) else listOf(
                        PadAction("Log", onClick = { doLog() }),
                        PadAction("Add", primary = true, onClick = { doAdd() })
                    ),
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun ContributionCard(
    title: String,
    protein: Double,
    carb: Double,
    fat: Double,
    calories: Double,
    remProtein: Double?,
    remCarb: Double?,
    remFat: Double?,
    remCalories: Double?
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ContribColumn("P", protein, remProtein, ProteinColor, Modifier.weight(1f))
                ContribColumn("C", carb, remCarb, CarbColor, Modifier.weight(1f))
                ContribColumn("F", fat, remFat, FatColor, Modifier.weight(1f))
                ContribColumn("Cal", calories, remCalories, CalorieColor, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ContribColumn(
    label: String,
    value: Double,
    remaining: Double?,
    color: Color,
    modifier: Modifier = Modifier
) {
    val hasGoal = remaining != null
    val target = remaining ?: 0.0
    val pct = when {
        !hasGoal -> 0
        target <= 0.0 -> 100
        else -> (value / target * 100).roundToInt()
    }
    val fill = when {
        !hasGoal -> 0f
        target <= 0.0 -> 1f
        else -> (value / target).coerceIn(0.0, 1.0).toFloat()
    }
    val caption = if (hasGoal) "$pct% › ${target.roundToInt()}" else "no goal"
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "$label ${value.roundToInt()}",
            style = MaterialTheme.typography.titleSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color.copy(alpha = 0.24f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fill)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
        Text(caption, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ShareRing(label: String, pct: Int, grams: Double, color: Color) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(64.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 14f
                val dd = size.minDimension - stroke
                val tl = Offset((size.width - dd) / 2f, (size.height - dd) / 2f)
                val arcSize = Size(dd, dd)
                drawArc(color = track, startAngle = 0f, sweepAngle = 360f, useCenter = false, topLeft = tl, size = arcSize, style = Stroke(width = stroke))
                drawArc(color = color, startAngle = -90f, sweepAngle = 360f * (pct / 100f), useCenter = false, topLeft = tl, size = arcSize, style = Stroke(width = stroke, cap = StrokeCap.Round))
            }
            Text("$pct%", style = MaterialTheme.typography.labelSmall)
        }
        Text("${grams.roundToInt()}", style = MaterialTheme.typography.titleSmall)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun MicroRow(label: String, value: Double, unit: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text("${value.roundToInt()} $unit", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}