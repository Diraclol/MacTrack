package com.dirac.mactrack.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class PadAction(val label: String, val onClick: () -> Unit, val primary: Boolean = false)

private val KEY_HEIGHT = 52.dp

@Composable
fun NumberPad(
    value: String,
    onValueChange: (String) -> Unit,
    units: List<String>,
    selectedUnit: String,
    onUnitSelect: (String) -> Unit,
    actions: List<PadAction>,
    modifier: Modifier = Modifier
) {
    fun press(key: String) {
        when (key) {
            "back" -> onValueChange(value.dropLast(1))
            "." -> if (!value.contains(".")) onValueChange(if (value.isEmpty()) "0." else "$value.")
            else -> {
                val dot = value.indexOf('.')
                val decimals = if (dot >= 0) value.length - dot - 1 else 0
                if (!(dot >= 0 && decimals >= 2)) {
                    onValueChange(value + key)
                }
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // value bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                value.ifEmpty { "0" },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(selectedUnit, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // unit chips
        if (units.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                units.forEach { u ->
                    FilterChip(selected = selectedUnit == u, onClick = { onUnitSelect(u) }, label = { Text(u) })
                }
            }
        }

        // rows 1-3
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Key(Modifier.weight(1f), { press("1") }) { KeyText("1") }
            Key(Modifier.weight(1f), { press("2") }) { KeyText("2") }
            Key(Modifier.weight(1f), { press("3") }) { KeyText("3") }
            BlankKey(Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Key(Modifier.weight(1f), { press("4") }) { KeyText("4") }
            Key(Modifier.weight(1f), { press("5") }) { KeyText("5") }
            Key(Modifier.weight(1f), { press("6") }) { KeyText("6") }
            BlankKey(Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Key(Modifier.weight(1f), { press("7") }) { KeyText("7") }
            Key(Modifier.weight(1f), { press("8") }) { KeyText("8") }
            Key(Modifier.weight(1f), { press("9") }) { KeyText("9") }
            Key(Modifier.weight(1f), { press("back") }) {
                Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Delete")
            }
        }

        // bottom row: . 0 [actions]
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Key(Modifier.weight(1f), { press(".") }) { KeyText(".") }
            Key(Modifier.weight(1f), { press("0") }) { KeyText("0") }
            when {
                actions.size >= 2 -> {
                    ActionKey(actions[0], Modifier.weight(1f))
                    ActionKey(actions[1], Modifier.weight(1f))
                }
                actions.size == 1 -> ActionKey(actions[0], Modifier.weight(2f))
                else -> Spacer(Modifier.weight(2f))
            }
        }
    }
}

@Composable
private fun Key(modifier: Modifier, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .height(KEY_HEIGHT)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun KeyText(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge)
}

@Composable
private fun ActionKey(action: PadAction, modifier: Modifier) {
    val bg = if (action.primary) MaterialTheme.colorScheme.primary else Color.Transparent
    val fg = if (action.primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .height(KEY_HEIGHT)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable { action.onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(action.label, color = fg, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BlankKey(modifier: Modifier) {
    Box(
        modifier = modifier
            .height(KEY_HEIGHT)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    )
}