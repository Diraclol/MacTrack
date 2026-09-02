package com.dirac.mactrack.ui.feature.food

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dirac.mactrack.data.food.foodEmoji
import com.dirac.mactrack.ui.common.EmojiPickerDialog
import com.dirac.mactrack.ui.common.FOOD_EMOJIS
import com.dirac.mactrack.ui.theme.ProteinColor
import com.dirac.mactrack.ui.theme.CarbColor
import com.dirac.mactrack.ui.theme.FatColor
import com.dirac.mactrack.ui.theme.CalorieColor
import kotlin.math.roundToInt

// Shared rounded shape for every entry box on this screen.
private val FieldShape = RoundedCornerShape(16.dp)

private val UNITS = listOf("serving", "g", "ml", "piece", "cup", "tbsp", "tsp", "oz", "scoop", "tablet", "capsule")

// Prefill text for a numeric field: blank for zero, no trailing ".0" for whole numbers.
private fun numText(x: Double): String =
    if (x == 0.0) "" else if (x % 1.0 == 0.0) x.toInt().toString() else x.toString()

// A create/edit saved-food screen styled like the food detail screen (cards, a live summary ring) but
// with entry boxes. Values entered are for ONE serving. Opened from the Kitchen with an id, it edits
// that food in place (and offers Delete); otherwise it creates a new one.
@Composable
fun CreateFoodScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onScanBarcode: () -> Unit = {},
    scannedBarcode: String? = null,
    onScannedConsumed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val viewModel: FoodViewModel = viewModel(factory = FoodViewModel.Factory)
    val editing by viewModel.editing.collectAsState()

    // rememberSaveable so the half-filled form survives a rotation AND the trip out to the barcode
    // scanner and back (the editor stays on the back stack while the scanner is on top).
    var name by rememberSaveable { mutableStateOf("") }
    var brand by rememberSaveable { mutableStateOf("") }
    var calories by rememberSaveable { mutableStateOf("") }
    var protein by rememberSaveable { mutableStateOf("") }
    var carb by rememberSaveable { mutableStateOf("") }
    var fat by rememberSaveable { mutableStateOf("") }
    var fiber by rememberSaveable { mutableStateOf("") }
    var sugar by rememberSaveable { mutableStateOf("") }
    var satFat by rememberSaveable { mutableStateOf("") }
    var sodium by rememberSaveable { mutableStateOf("") }
    var potassium by rememberSaveable { mutableStateOf("") }
    var cholesterol by rememberSaveable { mutableStateOf("") }
    var caffeine by rememberSaveable { mutableStateOf("") }
    var servingSize by rememberSaveable { mutableStateOf("1") }
    var servingUnit by rememberSaveable { mutableStateOf("serving") }
    var showMicros by rememberSaveable { mutableStateOf(false) }
    var emoji by rememberSaveable { mutableStateOf<String?>(null) }
    // Prefilled from a scanned-but-unrecognized barcode when creating (blank otherwise; edit mode
    // overwrites it from the loaded food in the seed effect below).
    var barcode by rememberSaveable { mutableStateOf(viewModel.initialBarcode ?: "") }
    var showIconPicker by remember { mutableStateOf(false) }

    // When editing, seed the fields once from the loaded food.
    var seeded by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(editing) {
        val e = editing
        if (e != null && !seeded) {
            name = e.name
            brand = e.brand ?: ""
            calories = numText(e.calories)
            protein = numText(e.proteinG)
            carb = numText(e.carbG)
            fat = numText(e.fatG)
            fiber = numText(e.fiberG)
            sugar = numText(e.sugarG)
            satFat = numText(e.satFatG)
            sodium = numText(e.sodiumMg)
            potassium = numText(e.potassiumMg)
            cholesterol = numText(e.cholesterolMg)
            caffeine = numText(e.caffeineMg)
            servingSize = numText(e.servingSize)
            servingUnit = e.servingUnit
            emoji = e.emoji
            barcode = e.barcode ?: ""
            if (e.fiberG != 0.0 || e.sugarG != 0.0 || e.satFatG != 0.0 || e.sodiumMg != 0.0 ||
                e.potassiumMg != 0.0 || e.cholesterolMg != 0.0 || e.caffeineMg != 0.0
            ) showMicros = true
            seeded = true
        }
    }

    // A barcode returned from the scanner fills the field, leaving the rest of the form untouched.
    LaunchedEffect(scannedBarcode) {
        if (!scannedBarcode.isNullOrBlank()) {
            barcode = scannedBarcode
            onScannedConsumed()
        }
    }

    val p = protein.toDoubleOrNull() ?: 0.0
    val c = carb.toDoubleOrNull() ?: 0.0
    val f = fat.toDoubleOrNull() ?: 0.0
    val macroKcal = p * 4 + c * 4 + f * 9
    val cal = calories.toDoubleOrNull() ?: macroKcal
    val canSave = name.isNotBlank()

    fun save() {
        if (!canSave) return
        viewModel.save(
            name = name.trim(),
            calories = cal,
            proteinG = p, carbG = c, fatG = f,
            fiberG = fiber.toDoubleOrNull() ?: 0.0,
            sugarG = sugar.toDoubleOrNull() ?: 0.0,
            satFatG = satFat.toDoubleOrNull() ?: 0.0,
            sodiumMg = sodium.toDoubleOrNull() ?: 0.0,
            potassiumMg = potassium.toDoubleOrNull() ?: 0.0,
            cholesterolMg = cholesterol.toDoubleOrNull() ?: 0.0,
            caffeineMg = caffeine.toDoubleOrNull() ?: 0.0,
            servingSize = servingSize.toDoubleOrNull() ?: 1.0,
            servingUnit = servingUnit,
            brand = brand.ifBlank { null },
            emoji = emoji,
            barcode = barcode.ifBlank { null }
        )
        onSaved()
    }

    Column(modifier = modifier.fillMaxSize().imePadding()) {
        // Top bar: back, title, save check.
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(if (viewModel.isEditing) "Edit Food" else "Create Food", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = { save() }, enabled = canSave) {
                Icon(Icons.Filled.Check, contentDescription = "Save food", tint = if (canSave) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon avatar: the chosen emoji, or one derived from the name until you pick. Tap to change.
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier.size(72.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { showIconPicker = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(emoji ?: foodEmoji(name.ifBlank { "food" }), style = MaterialTheme.typography.headlineMedium)
                    }
                    Text(
                        "Tap to choose an icon",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Identity card: name full width, brand + barcode share a row to save space.
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Name") },
                            singleLine = true,
                            shape = FieldShape,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = brand,
                                onValueChange = { brand = it },
                                label = { Text("Brand") },
                                placeholder = { Text("Optional") },
                                singleLine = true,
                                shape = FieldShape,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = barcode,
                                onValueChange = { barcode = it },
                                label = { Text("Barcode") },
                                placeholder = { Text("Optional") },
                                singleLine = true,
                                shape = FieldShape,
                                // Tap to scan a barcode with the camera instead of typing the digits.
                                trailingIcon = {
                                    IconButton(onClick = onScanBarcode) {
                                        Icon(Icons.Filled.QrCodeScanner, contentDescription = "Scan barcode")
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item { Text("Nutrition details", style = MaterialTheme.typography.titleMedium) }

            // Serving size + unit.
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Serving size", style = MaterialTheme.typography.labelLarge)
                        Text("A number and a unit, e.g. 1 bar or 38 g", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(
                            value = servingSize,
                            onValueChange = { servingSize = it },
                            label = { Text("Amount") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = FieldShape,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            UNITS.forEach { u ->
                                FilterChip(selected = servingUnit == u, onClick = { servingUnit = u }, label = { Text(u) })
                            }
                        }
                    }
                }
            }

            // Live summary.
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CalorieRing(cal)
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            MacroSummary("Protein", p, if (cal > 0) (p * 4 / cal * 100).roundToInt() else 0, ProteinColor)
                            MacroSummary("Carbs", c, if (cal > 0) (c * 4 / cal * 100).roundToInt() else 0, CarbColor)
                            MacroSummary("Fat", f, if (cal > 0) (f * 9 / cal * 100).roundToInt() else 0, FatColor)
                        }
                    }
                }
            }

            // Calories + macros.
            item {
                NutrientField(calories, { calories = it }, "Calories", "Auto from macros if blank")
            }
            item { NutrientField(protein, { protein = it }, "Protein (g)") }
            item { NutrientField(carb, { carb = it }, "Carbs (g)") }
            item { NutrientField(fat, { fat = it }, "Fat (g)") }

            item {
                TextButton(onClick = { showMicros = !showMicros }) {
                    Text(if (showMicros) "Hide micronutrients" else "Add micronutrients (optional)")
                }
            }
            if (showMicros) {
                item { NutrientField(fiber, { fiber = it }, "Fiber (g)") }
                item { NutrientField(sugar, { sugar = it }, "Sugar (g)") }
                item { NutrientField(satFat, { satFat = it }, "Saturated fat (g)") }
                item { NutrientField(sodium, { sodium = it }, "Sodium (mg)") }
                item { NutrientField(potassium, { potassium = it }, "Potassium (mg)") }
                item { NutrientField(cholesterol, { cholesterol = it }, "Cholesterol (mg)") }
                item { NutrientField(caffeine, { caffeine = it }, "Caffeine (mg)") }
            }

            if (viewModel.isEditing) {
                item {
                    Button(
                        onClick = { viewModel.deleteFood(onSaved) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete Food", color = MaterialTheme.colorScheme.onError)
                    }
                }
            }

            item { Box(Modifier.size(8.dp)) }
        }
    }

    if (showIconPicker) {
        EmojiPickerDialog(
            title = "Choose an icon",
            current = emoji ?: "",
            choices = FOOD_EMOJIS,
            onPick = { emoji = it; showIconPicker = false },
            onDismiss = { showIconPicker = false }
        )
    }
}

@Composable
private fun NutrientField(value: String, onValueChange: (String) -> Unit, label: String, helper: String? = null) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            shape = FieldShape,
            modifier = Modifier.fillMaxWidth()
        )
        if (helper != null) {
            Text(helper, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 12.dp, top = 2.dp))
        }
    }
}

@Composable
private fun CalorieRing(cal: Double) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(88.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 14f
            val d = size.minDimension - stroke
            val tl = Offset((size.width - d) / 2f, (size.height - d) / 2f)
            val arcSize = Size(d, d)
            drawArc(color = track, startAngle = 0f, sweepAngle = 360f, useCenter = false, topLeft = tl, size = arcSize, style = Stroke(width = stroke))
            drawArc(color = CalorieColor, startAngle = -90f, sweepAngle = 360f, useCenter = false, topLeft = tl, size = arcSize, style = Stroke(width = stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${cal.roundToInt()}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Cal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MacroSummary(label: String, grams: Double, pct: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Box(
            modifier = Modifier.clip(RoundedCornerShape(50)).background(color).padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text("$pct%", style = MaterialTheme.typography.labelSmall, color = Color.White)
        }
        Text("${grams.roundToInt()} g", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
