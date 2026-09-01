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
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dirac.mactrack.data.food.foodEmoji
import com.dirac.mactrack.ui.common.EmojiPickerDialog
import com.dirac.mactrack.ui.common.FOOD_EMOJIS
import kotlin.math.roundToInt

private val ProteinColor = Color(0xFFE91E63)
private val CarbColor = Color(0xFF2196F3)
private val FatColor = Color(0xFF4CAF50)
private val CalorieColor = Color(0xFFFF9800)

// Shared rounded shape for every entry box on this screen.
private val FieldShape = RoundedCornerShape(16.dp)

private val UNITS = listOf("serving", "g", "ml", "piece", "cup", "tbsp", "tsp", "oz", "scoop", "tablet", "capsule")

// A "create a saved food" screen styled like the food detail screen (cards, a live summary
// ring) but with entry boxes. Values entered are for ONE serving.
@Composable
fun CreateFoodScreen(onBack: () -> Unit, onSaved: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel: FoodViewModel = viewModel(factory = FoodViewModel.Factory)

    var name by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carb by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var fiber by remember { mutableStateOf("") }
    var sugar by remember { mutableStateOf("") }
    var satFat by remember { mutableStateOf("") }
    var sodium by remember { mutableStateOf("") }
    var potassium by remember { mutableStateOf("") }
    var cholesterol by remember { mutableStateOf("") }
    var caffeine by remember { mutableStateOf("") }
    var servingSize by remember { mutableStateOf("1") }
    var servingUnit by remember { mutableStateOf("serving") }
    var showMicros by remember { mutableStateOf(false) }
    var emoji by remember { mutableStateOf<String?>(null) }
    var barcode by remember { mutableStateOf("") }
    var showIconPicker by remember { mutableStateOf(false) }

    val p = protein.toDoubleOrNull() ?: 0.0
    val c = carb.toDoubleOrNull() ?: 0.0
    val f = fat.toDoubleOrNull() ?: 0.0
    val macroKcal = p * 4 + c * 4 + f * 9
    val cal = calories.toDoubleOrNull() ?: macroKcal
    val canSave = name.isNotBlank()

    fun save() {
        if (!canSave) return
        viewModel.addFood(
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
            Text("Create Food", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
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
