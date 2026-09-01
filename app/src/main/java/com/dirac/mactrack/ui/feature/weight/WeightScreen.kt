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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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
            Box(modifier = Modifier.fillMaxWidth().height(200.dp).padding(16.dp), contentAlignment = Alignment.Center) {
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
            onSave = { viewModel.logWeight(it); showLog = false },
            onDismiss = { showLog = false }
        )
    }
}

@Composable
private fun WeightGraph(points: List<WeightEntry>, modifier: Modifier = Modifier) {
    val line = MaterialTheme.colorScheme.primary
    val dot = MaterialTheme.colorScheme.primary
    val days = points.map { LocalDate.parse(it.date).toEpochDay() }
    val minW = points.minOf { it.weightKg }
    val maxW = points.maxOf { it.weightKg }
    val wSpan = (maxW - minW).takeIf { it > 0.0 } ?: 1.0
    val minX = days.min()
    val maxX = days.max()
    val xSpan = (maxX - minX).takeIf { it > 0L } ?: 1L

    Canvas(modifier = modifier) {
        val pad = 8.dp.toPx()
        val w = size.width - 2 * pad
        val h = size.height - 2 * pad
        fun at(i: Int): Offset {
            val x = pad + (days[i] - minX).toFloat() / xSpan * w
            val y = pad + (1f - ((points[i].weightKg - minW) / wSpan).toFloat()) * h
            return Offset(x, y)
        }
        val path = Path()
        points.indices.forEach { i ->
            val o = at(i)
            if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
        }
        drawPath(path, color = line, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
        points.indices.forEach { drawCircle(dot, radius = 4.dp.toPx(), center = at(it)) }
    }
}

@Composable
private fun WeightLogDialog(current: Double?, onSave: (Double) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(current?.let { oneDecimal(it) } ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log weight") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val w = text.toDoubleOrNull()
                if (w != null && w > 0) onSave(w)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
