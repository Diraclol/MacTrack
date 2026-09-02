package com.dirac.mactrack.ui.feature.weight

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dirac.mactrack.data.entity.WeightEntry
import com.dirac.mactrack.ui.common.BackBar
import com.dirac.mactrack.ui.feature.more.WeightViewModel
import java.time.LocalDate

private enum class WeightRange(val label: String, val days: Long?) {
    M1("1M", 30), M3("3M", 90), Y1("1Y", 365), ALL("All", null)
}

private fun oneDecimal(x: Double): String = (Math.round(x * 10.0) / 10.0).toString()

@Composable
fun WeightScreen(onBack: () -> Unit = {}, modifier: Modifier = Modifier) {
    val viewModel: WeightViewModel = viewModel(factory = WeightViewModel.Factory)
    val weights by viewModel.weights.collectAsState()
    var range by remember { mutableStateOf(WeightRange.M3) }
    var showLog by remember { mutableStateOf(false) }

    val shown = remember(weights, range) {
        val cutoff = range.days?.let { LocalDate.now().minusDays(it) }
        weights.filter { cutoff == null || !LocalDate.parse(it.date).isBefore(cutoff) }
            .sortedBy { it.date }
    }
    val latest = weights.maxByOrNull { it.date }
    val previous = weights.sortedBy { it.date }.let { if (it.size >= 2) it[it.size - 2] else null }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        BackBar("Weight", onBack)

        Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Current", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        latest?.let { "${oneDecimal(it.weightKg)} kg" } ?: "—",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (latest != null && previous != null) {
                        val delta = latest.weightKg - previous.weightKg
                        val sign = if (delta > 0) "+" else ""
                        Text(
                            "$sign${oneDecimal(delta)} kg since last",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Button(onClick = { showLog = true }) { Text("Log weight") }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WeightRange.entries.forEach { r ->
                FilterChip(selected = range == r, onClick = { range = r }, label = { Text(r.label) })
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    if (shown.size < 2) {
                        Text(
                            "Log at least two weigh-ins to see a trend.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        WeightGraph(shown, modifier = Modifier.fillMaxSize())
                    }
                }
                // Start / end of the visible window, so the range (1M vs 3M ...) has a time reference.
                if (shown.size >= 2) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 40.dp, top = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(shown.first().date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(shown.last().date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Text(
            "History",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
        )
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(items = shown.reversed(), key = { it.id }) { entry ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${entry.date} — ${oneDecimal(entry.weightKg)} kg", modifier = Modifier.weight(1f))
                    IconButton(onClick = { viewModel.deleteWeight(entry) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete weigh-in from ${entry.date}")
                    }
                }
            }
        }
    }

    if (showLog) {
        WeightLogDialog(
            current = latest?.weightKg,
            onSave = { w, d -> viewModel.logWeight(w, d); showLog = false },
            onDismiss = { showLog = false }
        )
    }
}

@Composable
private fun WeightGraph(points: List<WeightEntry>, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary
    val dotColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val measurer = rememberTextMeasurer()

    val days = points.map { LocalDate.parse(it.date).toEpochDay() }
    val minW = points.minOf { it.weightKg }
    val maxW = points.maxOf { it.weightKg }
    // Pad the weight axis a little so points aren't glued to the top/bottom edges, and so a flat
    // series (min == max) still draws a readable band.
    val padW = ((maxW - minW) * 0.1).takeIf { it > 0.0 } ?: 1.0
    val axisMax = maxW + padW
    val axisMin = minW - padW
    val axisSpan = (axisMax - axisMin).takeIf { it > 0.0 } ?: 1.0
    val minX = days.min()
    val maxX = days.max()
    val xSpan = (maxX - minX).takeIf { it > 0L } ?: 1L

    val labelStyle = TextStyle(fontSize = 10.sp, color = labelColor)

    Canvas(modifier = modifier) {
        val gutter = 40.dp.toPx()          // left space for the weight-axis labels
        val padV = 8.dp.toPx()
        val plotW = size.width - gutter
        val plotH = size.height - 2 * padV
        fun y(weight: Double): Float = padV + (1f - ((weight - axisMin) / axisSpan).toFloat()) * plotH
        fun x(i: Int): Float = gutter + (days[i] - minX).toFloat() / xSpan * plotW

        // Three horizontal reference lines with their kg values: max, midpoint, min of the padded axis.
        listOf(axisMax, (axisMax + axisMin) / 2.0, axisMin).forEach { w ->
            val gy = y(w)
            drawLine(gridColor, Offset(gutter, gy), Offset(size.width, gy), strokeWidth = 1.dp.toPx())
            val tl = measurer.measure(oneDecimal(w), labelStyle)
            drawText(tl, topLeft = Offset(gutter - tl.size.width - 6.dp.toPx(), gy - tl.size.height / 2f))
        }

        val path = Path()
        points.indices.forEach { i ->
            val px = x(i)
            val py = y(points[i].weightKg)
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        drawPath(path, color = lineColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
        points.indices.forEach { drawCircle(dotColor, radius = 4.dp.toPx(), center = Offset(x(it), y(points[it].weightKg))) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeightLogDialog(current: Double?, onSave: (Double, LocalDate) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(current?.let { oneDecimal(it) } ?: "") }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log weight") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                // Tap the date to backfill a weigh-in from a past day.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Date", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { showDatePicker = true }) {
                        Text(if (date == LocalDate.now()) "Today" else date.toString())
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val w = text.toDoubleOrNull()
                if (w != null && w > 0) onSave(w, date)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (showDatePicker) {
        // The picker works in UTC millis; convert with ZoneOffset.UTC both ways so the day never shifts.
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = date.toEpochDay() * 86_400_000L)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        date = java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = pickerState)
        }
    }
}
