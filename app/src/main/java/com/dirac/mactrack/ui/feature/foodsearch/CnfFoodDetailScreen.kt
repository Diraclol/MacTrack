package com.dirac.mactrack.ui.feature.foodsearch

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dirac.mactrack.data.cnf.CnfMeasure
import java.time.LocalTime
import kotlin.math.roundToInt

private val ProteinColor = Color(0xFFE91E63)
private val CarbColor = Color(0xFF2196F3)
private val FatColor = Color(0xFF4CAF50)

private fun gramsFor(amount: Double, unit: String, measures: List<CnfMeasure>): Double = when (unit) {
    "g" -> amount
    "oz" -> amount * 28.3495
    else -> (measures.find { it.description == unit }?.grams ?: 1.0) * amount
}

@Composable
fun CnfFoodDetailScreen(code: Int, onLogged: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel: CnfFoodDetailViewModel = viewModel(factory = CnfFoodDetailViewModel.Factory)
    val food by viewModel.food.collectAsState()
    val measures by viewModel.measures.collectAsState()

    LaunchedEffect(code) { viewModel.load(code) }

    var amount by remember { mutableStateOf("100") }
    var unit by remember { mutableStateOf("g") }

    val f = food
    if (f == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Loading…") }
        return
    }

    val grams = gramsFor(amount.toDoubleOrNull() ?: 0.0, unit, measures)
    val factor = grams / 100.0
    val cal = f.kcal * factor
    val p = f.protein * factor
    val c = f.carb * factor
    val fat = f.fat * factor

    // macro share of calories (scale-invariant)
    val pC = f.protein * 4; val fC = f.fat * 9; val cC = f.carb * 4
    val tot = (pC + fC + cC).let { if (it <= 0.0) 1.0 else it }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(f.name, style = MaterialTheme.typography.titleLarge)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${cal.roundToInt()}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Calories", style = MaterialTheme.typography.labelSmall)
            }
            ShareRing("Protein", (pC / tot * 100).roundToInt(), p, ProteinColor)
            ShareRing("Fat", (fC / tot * 100).roundToInt(), fat, FatColor)
            ShareRing("Carbs", (cC / tot * 100).roundToInt(), c, CarbColor)
        }

        HorizontalDivider()

        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Amount") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (listOf("g", "oz") + measures.map { it.description }).forEach { u ->
                FilterChip(selected = unit == u, onClick = { unit = u }, label = { Text(u) })
            }
        }
        Text("= ${grams.roundToInt()} g", style = MaterialTheme.typography.bodySmall)

        HorizontalDivider()

        Text("Nutrition (this serving)", style = MaterialTheme.typography.titleSmall)
        MicroRow("Fiber", f.fiber * factor, "g")
        MicroRow("Sugar", f.sugar * factor, "g")
        MicroRow("Saturated fat", f.satFat * factor, "g")
        MicroRow("Sodium", f.sodium * factor, "mg")
        MicroRow("Potassium", f.potassium * factor, "mg")
        MicroRow("Cholesterol", f.cholesterol * factor, "mg")

        Button(
            onClick = {
                val now = LocalTime.now()
                viewModel.logFood(grams, now.hour * 60 + now.minute, onLogged)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add to today's log")
        }
    }
}

@Composable
private fun ShareRing(label: String, pct: Int, grams: Double, color: Color) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 8f
                val d = size.minDimension - stroke
                val tl = Offset((size.width - d) / 2f, (size.height - d) / 2f)
                val arcSize = Size(d, d)
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