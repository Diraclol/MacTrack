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
import androidx.compose.material3.OutlinedButton
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
import com.dirac.mactrack.ui.common.BackBar
import java.time.LocalTime
import kotlin.math.roundToInt

private val ProteinColor = Color(0xFFE91E63)
private val CarbColor = Color(0xFF2196F3)
private val FatColor = Color(0xFF4CAF50)

@Composable
fun FoodDetailScreen(source: String, id: String, onLogged: () -> Unit, onBack: () -> Unit = {}, modifier: Modifier = Modifier) {
    val viewModel: FoodDetailViewModel = viewModel(factory = FoodDetailViewModel.Factory)
    val detail by viewModel.detail.collectAsState()

    LaunchedEffect(source, id) { viewModel.load(source, id) }

    val d = detail
    if (d == null || d.units.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Loading…") }
        return
    }

    var amount by remember(d) {
        mutableStateOf(if (d.defaultAmount % 1.0 == 0.0) d.defaultAmount.toInt().toString() else d.defaultAmount.toString())
    }
    var unitLabel by remember(d) { mutableStateOf(d.units.first().label) }
    val unit = d.units.find { it.label == unitLabel } ?: d.units.first()

    val amt = amount.toDoubleOrNull() ?: 0.0
    val s = unit.per * amt

    val pC = unit.per.protein * 4; val fC = unit.per.fat * 9; val cC = unit.per.carb * 4
    val tot = (pC + fC + cC).let { if (it <= 0.0) 1.0 else it }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BackBar(d.name, onBack)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${s.kcal.roundToInt()}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Calories", style = MaterialTheme.typography.labelSmall)
            }
            ShareRing("Protein", (pC / tot * 100).roundToInt(), s.protein, ProteinColor)
            ShareRing("Fat", (fC / tot * 100).roundToInt(), s.fat, FatColor)
            ShareRing("Carbs", (cC / tot * 100).roundToInt(), s.carb, CarbColor)
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
            d.units.forEach { u ->
                FilterChip(selected = unitLabel == u.label, onClick = { unitLabel = u.label }, label = { Text(u.label) })
            }
        }
        unit.grams?.let { g -> Text("= ${(amt * g).roundToInt()} g", style = MaterialTheme.typography.bodySmall) }

        HorizontalDivider()

        Text("Nutrition (this serving)", style = MaterialTheme.typography.titleSmall)
        MicroRow("Fiber", s.fiber, "g")
        MicroRow("Sugar", s.sugar, "g")
        MicroRow("Saturated fat", s.satFat, "g")
        MicroRow("Sodium", s.sodium, "mg")
        MicroRow("Potassium", s.potassium, "mg")
        MicroRow("Cholesterol", s.cholesterol, "mg")

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { viewModel.addToCart(amt, unit); onLogged() },
                modifier = Modifier.weight(1f)
            ) { Text("Add to cart") }
            Button(
                onClick = {
                    val now = LocalTime.now()
                    viewModel.log(amt, unit, now.hour * 60 + now.minute, onLogged)
                },
                modifier = Modifier.weight(1f)
            ) { Text("Log now") }
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