package com.dirac.mactrack.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dirac.mactrack.data.cnf.CnfFood
import com.dirac.mactrack.data.entity.FoodItem

// A reusable "add ingredients" section spanning the whole food catalog (your saved foods + Common
// foods), like the food search. Custom foods are filtered here from `foods`; Common (CNF) foods
// come from the caller's search (`cnfMatches`) and are imported into food_items when added
// (`onAddCnf`). `amounts` is foodId -> amount text; the caller owns it and reads it on save.
@Composable
fun IngredientPicker(
    foods: List<FoodItem>,
    cnfMatches: List<CnfFood>,
    query: String,
    onQueryChange: (String) -> Unit,
    amounts: androidx.compose.runtime.snapshots.SnapshotStateMap<String, String>,
    onAddCnf: (CnfFood) -> Unit,
    onCreateFood: () -> Unit,
    modifier: Modifier = Modifier,
    amountUnit: String = "servings"
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Chosen ingredients, each with an editable amount.
        amounts.keys.toList().forEach { id ->
            val food = foods.find { it.id == id } ?: return@forEach
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(food.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = amounts[id] ?: "",
                        onValueChange = { amounts[id] = it },
                        label = { Text(amountUnit) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.width(120.dp)
                    )
                    IconButton(onClick = { amounts.remove(id) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove ${food.name}")
                    }
                }
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search all foods to add") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )

        val customMatches = foods.filter {
            it.id !in amounts.keys && (query.isBlank() || it.name.contains(query, ignoreCase = true))
        }
        if (customMatches.isNotEmpty()) {
            Text("Your foods", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            customMatches.take(6).forEach { food ->
                AddRow(food.name) { amounts[food.id] = "1" }
            }
        }

        val commonMatches = cnfMatches.filter { "cnf_${it.code}" !in amounts.keys }
        if (commonMatches.isNotEmpty()) {
            Text("Common foods", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            commonMatches.take(8).forEach { cnf ->
                AddRow("${cnf.name}  ·  per 100 g") { onAddCnf(cnf) }
            }
        }

        if (foods.isEmpty() && cnfMatches.isEmpty() && query.isBlank()) {
            Text(
                "Search for a food above, or create your own.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        OutlinedButton(onClick = onCreateFood, modifier = Modifier.fillMaxWidth()) {
            Text("Create a new food")
        }
    }
}

@Composable
private fun AddRow(label: String, onAdd: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onAdd() }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
    }
}
