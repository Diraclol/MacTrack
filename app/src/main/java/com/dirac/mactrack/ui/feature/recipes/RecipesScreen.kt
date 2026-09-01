package com.dirac.mactrack.ui.feature.recipes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dirac.mactrack.ui.common.BackBar

@Composable
fun RecipesScreen(modifier: Modifier = Modifier, onBack: () -> Unit = {}, showBar: Boolean = true) {
    val viewModel: RecipesViewModel = viewModel(factory = RecipesViewModel.Factory)
    val foods by viewModel.foods.collectAsState()

    var name by remember { mutableStateOf("") }
    var makes by remember { mutableStateOf("1") }
    var cooked by remember { mutableStateOf("") }
    val amounts = remember { mutableStateMapOf<String, String>() }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (showBar) item { BackBar("Recipes", onBack) }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Recipe name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = makes,
                        onValueChange = { makes = it },
                        label = { Text("Makes how many servings") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = cooked,
                        onValueChange = { cooked = it },
                        label = { Text("Cooked weight in grams (optional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "For each ingredient, enter how many servings of it the whole recipe uses. " +
                            "A cooked weight lets you later log by grams of the finished dish.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        item { Text("Ingredients", style = MaterialTheme.typography.titleMedium) }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                foods.forEach { food ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(food.name, modifier = Modifier.weight(1f))
                        OutlinedTextField(
                            value = amounts[food.id] ?: "",
                            onValueChange = { amounts[food.id] = it },
                            label = { Text("servings") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.width(120.dp)
                        )
                    }
                }
            }
        }

        item {
            Button(
                onClick = {
                    val ingredients = amounts
                        .mapNotNull { (id, txt) ->
                            val v = txt.toDoubleOrNull()
                            if (v != null && v > 0.0) id to v else null
                        }
                        .toMap()
                    viewModel.saveRecipe(
                        name = name,
                        makesServings = makes.toDoubleOrNull() ?: 1.0,
                        cookedWeightG = cooked.toDoubleOrNull(),
                        emoji = null,
                        ingredients = ingredients
                    )
                    name = ""
                    makes = "1"
                    cooked = ""
                    amounts.clear()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save recipe")
            }
        }
    }
}