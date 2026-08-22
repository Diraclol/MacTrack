package com.dirac.mactrack.ui.feature.meals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dirac.mactrack.ui.common.BackBar

@Composable
fun MealsScreen(modifier: Modifier = Modifier, onBack: () -> Unit = {}) {
    val viewModel: MealsViewModel = viewModel(factory = MealsViewModel.Factory)
    val foods by viewModel.foods.collectAsState()
    val templates by viewModel.templates.collectAsState()

    var mealName by remember { mutableStateOf("") }
    val selected = remember { mutableStateMapOf<String, Boolean>() }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { BackBar("Meals", onBack) }

        items(items = templates, key = { it.id }) { template ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(template.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { viewModel.deleteTemplate(template) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete ${template.name}")
                        }
                    }
                    Button(onClick = { viewModel.logTemplate(template) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Log this meal (now)")
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Create a meal", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = mealName,
                        onValueChange = { mealName = it },
                        label = { Text("Meal name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Pick foods (1 serving each):", style = MaterialTheme.typography.bodySmall)
                    foods.forEach { food ->
                        FilterChip(
                            selected = selected[food.id] == true,
                            onClick = { selected[food.id] = !(selected[food.id] ?: false) },
                            label = { Text(food.name) }
                        )
                    }
                    Button(
                        onClick = {
                            val items = selected.filterValues { it }.keys.map { it to 1.0 }
                            viewModel.saveTemplate(mealName, items)
                            mealName = ""
                            selected.clear()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save meal")
                    }
                }
            }
        }
    }
}