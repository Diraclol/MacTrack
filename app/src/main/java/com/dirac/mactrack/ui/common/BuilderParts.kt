package com.dirac.mactrack.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

// Shared building blocks for the Create Meal / Create Recipe screens, so both match the same
// MacroFactor-style layout: a top bar with a Save action, grouped label/value rows, a section card
// header with a round "+", an empty state, a macro-summary pill row, a per-serving/total toggle, and
// tappable ingredient rows showing each food's amount and macro contribution.

// The macro accent colors used across the app (protein/carb/fat/calories), matching the food log and
// Kitchen rows so the pills read consistently.
val ProteinColor = Color(0xFFE91E63)
val CarbColor = Color(0xFF2196F3)
val FatColor = Color(0xFF4CAF50)
val CalorieColor = Color(0xFFFF9800)

private fun amountText(x: Double): String =
    if (x % 1.0 == 0.0) x.toInt().toString() else (kotlin.math.round(x * 100.0) / 100.0).toString()

// Top bar: back arrow, screen title, and a right-aligned Save that enables only when the form is
// valid.
@Composable
fun CreateTopBar(title: String, onBack: () -> Unit, saveEnabled: Boolean, onSave: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        TextButton(onClick = onSave, enabled = saveEnabled) { Text("Save") }
    }
}

// A borderless value field that sits at the right edge of a settings-style row (e.g. the Name and
// Total Servings values), showing a grey placeholder when empty.
@Composable
fun InlineValueField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    numeric: Boolean = false
) {
    val colors = MaterialTheme.colorScheme
    val textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface, textAlign = TextAlign.End)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = textStyle,
        cursorBrush = SolidColor(colors.primary),
        keyboardOptions = if (numeric) KeyboardOptions(keyboardType = KeyboardType.Decimal) else KeyboardOptions.Default,
        modifier = modifier,
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.CenterEnd) {
                if (value.isEmpty()) {
                    Text(
                        placeholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.End
                    )
                }
                inner()
            }
        }
    )
}

// A label (with an optional subtitle) on the left and a trailing value slot on the right.
@Composable
fun LabeledFieldRow(
    label: String,
    subtitle: String? = null,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        trailing()
    }
}

// The "Foods" / "Ingredients" header inside a section card, with a round "+" that opens the picker.
@Composable
fun SectionCardHeader(title: String, onAdd: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        FilledTonalIconButton(onClick = onAdd) {
            Icon(Icons.Filled.Add, contentDescription = "Add")
        }
    }
}

// Empty state shown inside the Foods/Ingredients card before anything is added.
@Composable
fun BuilderEmptyState(message: String, buttonLabel: String, onAdd: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("🍎", style = MaterialTheme.typography.displaySmall, modifier = Modifier.alpha(0.35f))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FilledTonalButton(onClick = onAdd) { Text(buttonLabel) }
    }
}

// A single colored macro pill, e.g. "P63" on pink or "🔥618" on orange.
@Composable
private fun MacroPill(text: String, color: Color) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(50)).background(color).padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = Color(0xFF10130F))
    }
}

// The macro summary for the whole meal/recipe (or per serving): protein, carbs, fat, and calories as
// colored pills.
@Composable
fun MacroPills(protein: Double, carb: Double, fat: Double, calories: Double, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MacroPill("P${protein.roundToInt()}", ProteinColor)
        MacroPill("C${carb.roundToInt()}", CarbColor)
        MacroPill("F${fat.roundToInt()}", FatColor)
        MacroPill("🔥${calories.roundToInt()}", CalorieColor)
    }
}

// A two-option segmented pill (e.g. "Per Serving" | "Recipe Total").
@Composable
fun SegmentedToggle(
    leftLabel: String,
    rightLabel: String,
    leftSelected: Boolean,
    onSelectLeft: () -> Unit,
    onSelectRight: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        SegHalf(leftLabel, leftSelected, Modifier.weight(1f), onSelectLeft)
        SegHalf(rightLabel, !leftSelected, Modifier.weight(1f), onSelectRight)
    }
}

@Composable
private fun SegHalf(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f) else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// One ingredient row: its icon, name, and (on the second line) the amount in the food's unit plus its
// macro contribution. Tapping the row opens the edit-amount dialog.
@Composable
fun IngredientDisplayRow(
    icon: String,
    name: String,
    amountLabel: String,
    protein: Double,
    carb: Double,
    fat: Double,
    calories: Double,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(end = 12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(name, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("$amountLabel  ›", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("P${protein.roundToInt()}", style = MaterialTheme.typography.labelMedium, color = ProteinColor)
                Text("C${carb.roundToInt()}", style = MaterialTheme.typography.labelMedium, color = CarbColor)
                Text("F${fat.roundToInt()}", style = MaterialTheme.typography.labelMedium, color = FatColor)
                Text("🔥${calories.roundToInt()}", style = MaterialTheme.typography.labelMedium, color = CalorieColor)
            }
        }
    }
}

// Dialog to change how much of a food the meal/recipe uses (in the food's own serving unit), or to
// remove it. The amount edits the whole-recipe total, independent of the per-serving/total view.
@Composable
fun EditAmountDialog(
    name: String,
    unit: String,
    initialAmount: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
    onRemove: () -> Unit
) {
    var text by remember { mutableStateOf(amountText(initialAmount)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Amount ($unit)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { text.toDoubleOrNull()?.let { if (it > 0.0) onConfirm(it) } },
                enabled = (text.toDoubleOrNull() ?: 0.0) > 0.0
            ) { Text("Done") }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onRemove) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}
