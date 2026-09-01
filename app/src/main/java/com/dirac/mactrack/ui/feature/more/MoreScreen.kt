package com.dirac.mactrack.ui.feature.more

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dirac.mactrack.ui.feature.profile.ProfileViewModel
import com.dirac.mactrack.ui.theme.StartScreen
import com.dirac.mactrack.ui.theme.ThemeMode
import com.dirac.mactrack.ui.theme.ThemeViewModel

private fun pretty(name: String) = name.lowercase().replaceFirstChar { it.uppercase() }

@Composable
fun MoreScreen(
    modifier: Modifier = Modifier,
    onOpenLibrary: () -> Unit = {},
    onOpenGoals: () -> Unit = {},
    onReassessGoals: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
) {
    val weightViewModel: WeightViewModel = viewModel(factory = WeightViewModel.Factory)
    val profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory)
    val themeViewModel: ThemeViewModel = viewModel(factory = ThemeViewModel.Factory)
    val statsViewModel: MoreStatsViewModel = viewModel(factory = MoreStatsViewModel.Factory)
    val themeMode by themeViewModel.mode.collectAsState()
    val startScreen by themeViewModel.startScreen.collectAsState()
    val weights by weightViewModel.weights.collectAsState()
    val profile by profileViewModel.profile.collectAsState()
    val stats by statsViewModel.stats.collectAsState()
    var weight by remember { mutableStateOf("") }
    val context = LocalContext.current
    val versionName = remember {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Profile header (tap to open the full profile)
        item {
            Card(modifier = Modifier.fillMaxWidth().clickable { onOpenProfile() }) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(48.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🧑", style = MaterialTheme.typography.titleLarge)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Your profile", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "MacTrack $versionName",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        StatItem("Active streak", "${stats.activeStreak} days")
                        StatItem("Longest streak", "${stats.longestStreak} days")
                        StatItem("Total tracked", "${stats.totalTracked} days")
                    }
                }
            }
        }
        // Navigation cards
        item { MoreCard("Saved Foods, Meals & Recipes", onOpenLibrary) }
        item { MoreCard("Goals", onOpenGoals) }
        item { MoreCard("Reassess goals", onReassessGoals) }
        // Preferences
        item {
            Text(
                "PREFERENCES",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                    Text("Start screen", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StartScreen.entries.forEach { s ->
                            FilterChip(
                                selected = startScreen == s,
                                onClick = { themeViewModel.setStartScreen(s) },
                                label = { Text(if (s == StartScreen.DASHBOARD) "Dashboard" else "Food log") }
                            )
                        }
                    }
                }
            }
        }
        // Weight logging
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                    if (weights.isNotEmpty()) {
                        Text("History", style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
        items(items = weights, key = { it.id }) { entry ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${entry.date} — ${entry.weightKg} kg", modifier = Modifier.weight(1f))
                IconButton(onClick = { weightViewModel.deleteWeight(entry) }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete weight from ${entry.date}")
                }
            }
        }
        item {
            Text(
                "MacTrack v$versionName",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MoreCard(label: String, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RowScope.StatItem(label: String, value: String) {
    Column(modifier = Modifier.weight(1f)) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
