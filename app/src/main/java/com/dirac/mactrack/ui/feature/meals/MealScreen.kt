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
import com.dirac.mactrack.ui.common.IngredientPicker

@Composable
fun MealsScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onCreateFood: () -> Unit = {},
    showBar: Boolean = true
) {
    val viewModel: MealsViewModel = viewModel(factory = MealsViewModel.Factory)
    val foods by viewModel.foods.collectAsState()
    val templates by viewModel.templates.collectAsState()

    var mealName by remember { mutableStateOf("") }
    val amounts = remember { mutableStateMapOf<String, String>() }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (showBar) item { BackBar("Meals", onBack) }

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
                        placeholder = { Text("e.g. Supplements, Breakfast shake") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "A meal is a labeled batch of foods you log together in one tap.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("Foods in this meal:", style = MaterialTheme.typography.bodySmall)
                    IngredientPicker(
                        foods = foods,
                        amounts = amounts,
                        onCreateFood = onCreateFood,
                        amountUnit = "servings"
                    )
                    Button(
                        onClick = {
                            val items = amounts.mapNotNull { (id, txt) ->
                                val v = txt.toDoubleOrNull() ?: 1.0
                                if (v > 0.0) id to v else null
                            }
                            viewModel.saveTemplate(mealName, items)
                            mealName = ""
                            amounts.clear()
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