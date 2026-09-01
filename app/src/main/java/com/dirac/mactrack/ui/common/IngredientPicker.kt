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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dirac.mactrack.data.entity.FoodItem

// A reusable "add ingredients" section for the Create Meal / Create Recipe screens: search your
// saved foods, tap to add, set an amount per added food, and a shortcut to create a food when you
// have none. `amounts` is foodId -> amount text (the caller owns the state and reads it on save).
@Composable
fun IngredientPicker(
    foods: List<FoodItem>,
    amounts: SnapshotStateMap<String, String>,
    onCreateFood: () -> Unit,
    modifier: Modifier = Modifier,
    amountUnit: String = "servings"
) {
    var query by remember { mutableStateOf("") }

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
            onValueChange = { query = it },
            placeholder = { Text("Search your foods to add") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )

        val matches = foods.filter { it.id !in amounts.keys && (query.isBlank() || it.name.contains(query, ignoreCase = true)) }
        if (foods.isEmpty()) {
            Text(
                "You have no saved foods yet. Create one to add it as an ingredient.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        matches.take(8).forEach { food ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { amounts[food.id] = "1" }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(food.name, style = MaterialTheme.typography.bodyMedium)
            }
        }

        OutlinedButton(onClick = onCreateFood, modifier = Modifier.fillMaxWidth()) {
            Text("Create a new food")
        }
    }
}
