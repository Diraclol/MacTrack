package com.dirac.mactrack.ui.feature.recipes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dirac.mactrack.ui.common.BuilderEmptyState
import com.dirac.mactrack.ui.common.CreateTopBar
import com.dirac.mactrack.ui.common.IngredientEditRow
import com.dirac.mactrack.ui.common.InlineValueField
import com.dirac.mactrack.ui.common.LabeledFieldRow
import com.dirac.mactrack.ui.common.SectionCardHeader
import kotlin.math.roundToInt

private fun servingText(x: Double): String =
    if (x % 1.0 == 0.0) x.toInt().toString() else x.toString()

// Create Recipe: name, how many servings it makes, an optional finished (cooked) weight, and an
// ingredient list. The "+" / "Add ingredient" buttons open the food-search screen in picker mode
// (onAddIngredients); picks land in the shared draft and show up here as editable rows. Recipe
// macros are computed per-serving from the ingredients at display/log time, not stored here.
@Composable
fun RecipesScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onAddIngredients: () -> Unit = {},
    onSaved: () -> Unit = {}
) {
    val viewModel: RecipesViewModel = viewModel(factory = RecipesViewModel.Factory)
    val foods by viewModel.foods.collectAsState()
    val ingredients by viewModel.ingredients.collectAsState()

    var name by rememberSaveable { mutableStateOf("") }
    var makes by rememberSaveable { mutableStateOf("1") }
    var cooked by rememberSaveable { mutableStateOf("") }
    val amounts = remember { mutableStateMapOf<String, String>() }

    LaunchedEffect(ingredients) {
        val ids = ingredients.map { it.foodId }.toSet()
        amounts.keys.filter { it !in ids }.forEach { amounts.remove(it) }
        ingredients.forEach { if (it.foodId !in amounts) amounts[it.foodId] = servingText(it.servings) }
    }

    val foodsById = foods.associateBy { it.id }
    val canSave = name.isNotBlank() && (makes.toDoubleOrNull() ?: 0.0) > 0.0 && ingredients.isNotEmpty()

    Column(modifier = modifier.fillMaxSize()) {
        CreateTopBar(
            title = "Create Recipe",
            onBack = onBack,
            saveEnabled = canSave,
            onSave = {
                val items = ingredients.mapNotNull { ing ->
                    val v = amounts[ing.foodId]?.toDoubleOrNull() ?: ing.servings
                    if (v > 0.0) ing.foodId to v else null
                }
                viewModel.saveRecipe(
                    name = name,
                    makesServings = makes.toDoubleOrNull() ?: 1.0,
                    cookedWeightG = cooked.toDoubleOrNull(),
                    emoji = null,
                    ingredients = items,
                    onDone = onSaved
                )
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column {
                        LabeledFieldRow(label = "Name") {
                            InlineValueField(
                                value = name,
                                onValueChange = { name = it },
                                placeholder = "Name",
                                modifier = Modifier.fillMaxWidth(0.6f)
                            )
                        }
                        HorizontalDivider()
                        LabeledFieldRow(label = "Total Servings", subtitle = "A number and unit e.g. 12 cookies") {
                            InlineValueField(
                                value = makes,
                                onValueChange = { makes = it },
                                placeholder = "1",
                                numeric = true,
                                modifier = Modifier.width(56.dp)
                            )
                        }
                        HorizontalDivider()
                        LabeledFieldRow(label = "Total Weight After Cooking", subtitle = "Optionally add a weight measurement") {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                InlineValueField(
                                    value = cooked,
                                    onValueChange = { cooked = it },
                                    placeholder = "0",
                                    numeric = true,
                                    modifier = Modifier.width(56.dp)
                                )
                                Text(" g", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            item {
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SectionCardHeader("Ingredients", onAddIngredients)
                        if (ingredients.isEmpty()) {
                            BuilderEmptyState("Add ingredients to start building your recipe", "Add ingredient", onAddIngredients)
                        } else {
                            ingredients.forEach { ing ->
                                val cals = ((foodsById[ing.foodId]?.calories ?: 0.0) * ing.servings).roundToInt()
                                IngredientEditRow(
                                    name = ing.name,
                                    subtitle = "$cals cal",
                                    amount = amounts[ing.foodId] ?: servingText(ing.servings),
                                    onAmountChange = { txt ->
                                        amounts[ing.foodId] = txt
                                        txt.toDoubleOrNull()?.let { if (it > 0.0) viewModel.setServings(ing.foodId, it) }
                                    },
                                    onRemove = { viewModel.removeIngredient(ing.foodId) }
                                )
                            }
                            val totalCals = ingredients.sumOf { (foodsById[it.foodId]?.calories ?: 0.0) * it.servings }.roundToInt()
                            val makesN = makes.toDoubleOrNull() ?: 1.0
                            val perServing = if (makesN > 0) (totalCals / makesN).roundToInt() else totalCals
                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text("Per serving", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                Text("$perServing cal", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    "For each ingredient, enter how many servings of it the whole recipe uses. A cooked " +
                        "weight lets you later log by grams of the finished dish.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
