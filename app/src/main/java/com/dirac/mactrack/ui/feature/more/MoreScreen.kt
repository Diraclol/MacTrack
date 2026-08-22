package com.dirac.mactrack.ui.feature.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dirac.mactrack.ui.feature.profile.ProfileViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilterChip
import com.dirac.mactrack.ui.theme.ThemeMode
import com.dirac.mactrack.ui.theme.ThemeViewModel

private fun pretty(name: String) = name.lowercase().replaceFirstChar { it.uppercase() }

@Composable
fun MoreScreen(
    modifier: Modifier = Modifier,
    onOpenSavedFoods: () -> Unit = {},
    onOpenMeals: () -> Unit = {},
    onOpenRecipes: () -> Unit = {},
    onOpenGoals: () -> Unit = {} ,
    onOpenCnfSearch: () -> Unit = {}
) {
    val weightViewModel: WeightViewModel = viewModel(factory = WeightViewModel.Factory)
    val profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory)
    val themeViewModel: ThemeViewModel = viewModel(factory = ThemeViewModel.Factory)
    val themeMode by themeViewModel.mode.collectAsState()
    val weights by weightViewModel.weights.collectAsState()
    val profile by profileViewModel.profile.collectAsState()
    var weight by remember { mutableStateOf("") }
    val context = LocalContext.current
    val versionName = remember {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedButton(onClick = onOpenCnfSearch, modifier = Modifier.fillMaxWidth()) {
                    Text("Search foods (CNF)")
                }
                OutlinedButton(onClick = onOpenSavedFoods, modifier = Modifier.fillMaxWidth()) {
                    Text("Saved foods")
                }
                OutlinedButton(onClick = onOpenMeals, modifier = Modifier.fillMaxWidth()) {
                    Text("Meals")
                }
                OutlinedButton(onClick = onOpenRecipes, modifier = Modifier.fillMaxWidth()) {
                    Text("Recipes")
                }
                OutlinedButton(onClick = onOpenGoals, modifier = Modifier.fillMaxWidth()) {
                    Text("Goals")
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text("Theme", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = themeMode == mode,
                            onClick = { themeViewModel.setMode(mode) },
                            label = { Text(pretty(mode.name)) }
                        )
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text("Your profile", style = MaterialTheme.typography.titleMedium)
                val p = profile
                if (p == null) {
                    Text("Not set yet. It's saved when you complete onboarding.")
                } else {
                    Text("Sex: ${pretty(p.sex)}   Age: ${p.age}")
                    Text("Weight: ${p.weightKg} kg   Height: ${p.heightCm} cm")
                    Text("Activity: ${pretty(p.activityLevel)}")
                    Text("Goal: ${pretty(p.goalType)}")
                    Text("Protein: ${pretty(p.proteinLevel)}   Fat: ${pretty(p.fatLevel)}")
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text("Log weight", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        val w = weight.toDoubleOrNull()
                        if (w != null && w > 0) {
                            weightViewModel.logWeight(w)
                            weight = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save weight")
                }
                Text("History", style = MaterialTheme.typography.titleSmall)
            }
        }
        items(items = weights, key = { it.id }) { entry ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${entry.date} — ${entry.weightKg} kg",
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { weightViewModel.deleteWeight(entry) }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete weight from ${entry.date}")
                }
            }
        }
        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text(
                "MacTrack v$versionName",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}