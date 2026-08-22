package com.dirac.mactrack.ui.feature.foodsearch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dirac.mactrack.ui.common.BackBar
import java.time.LocalTime
import kotlin.math.roundToInt

@Composable
fun QuickAddScreen(onDone: () -> Unit, onBack: () -> Unit = {}, modifier: Modifier = Modifier) {
    val viewModel: QuickAddViewModel = viewModel(factory = QuickAddViewModel.Factory)

    var name by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carb by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }

    val p = protein.toDoubleOrNull() ?: 0.0
    val c = carb.toDoubleOrNull() ?: 0.0
    val f = fat.toDoubleOrNull() ?: 0.0
    val macroKcal = p * 4 + c * 4 + f * 9

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BackBar("Quick add", onBack)

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name (optional)") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = calories,
            onValueChange = { calories = it },
            label = { Text("Calories (blank = use macro sum)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        Text("Macro sum: ${macroKcal.roundToInt()} kcal", style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(
            value = protein,
            onValueChange = { protein = it },
            label = { Text("Protein (g)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = carb,
            onValueChange = { carb = it },
            label = { Text("Carbs (g)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = fat,
            onValueChange = { fat = it },
            label = { Text("Fat (g)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                val cal = calories.toDoubleOrNull() ?: macroKcal
                val now = LocalTime.now()
                viewModel.quickAdd(name, cal, p, c, f, now.hour * 60 + now.minute, onDone)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add to today's log")
        }
    }
}