package com.dirac.mactrack.ui.feature.meals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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

// Create Meal: a labeled batch of foods logged together in one tap. The "+" / "Add Food" buttons
// open the food-search screen in picker mode (onAddFoods); picked foods land in the shared draft and
// show up here as editable rows. No servings/weight/type -- a meal is just its name plus its foods.
@Composable
fun MealsScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onAddFoods: () -> Unit = {},
    onSaved: () -> Unit = {}
) {
    val viewModel: MealsViewModel = viewModel(factory = MealsViewModel.Factory)
    val foods by viewModel.foods.collectAsState()
    val ingredients by viewModel.ingredients.collectAsState()

    var mealName by rememberSaveable { mutableStateOf("") }
    val amounts = remember { mutableStateMapOf<String, String>() }

    // Keep the per-ingredient amount fields in step with the draft (add new keys, drop removed ones)
    // without clobbering what the user is currently typing.
    LaunchedEffect(ingredients) {
        val ids = ingredients.map { it.foodId }.toSet()
        amounts.keys.filter { it !in ids }.forEach { amounts.remove(it) }
        ingredients.forEach { if (it.foodId !in amounts) amounts[it.foodId] = servingText(it.servings) }
    }

    val foodsById = foods.associateBy { it.id }
    val canSave = mealName.isNotBlank() && ingredients.isNotEmpty()

    Column(modifier = modifier.fillMaxSize()) {
        CreateTopBar(
            title = "Create Meal",
            onBack = onBack,
            saveEnabled = canSave,
            onSave = {
                val items = ingredients.mapNotNull { ing ->
                    val v = amounts[ing.foodId]?.toDoubleOrNull() ?: ing.servings
                    if (v > 0.0) ing.foodId to v else null
                }
                viewModel.saveMeal(mealName, items, onSaved)
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    LabeledFieldRow(label = "Name") {
                        InlineValueField(
                            value = mealName,
                            onValueChange = { mealName = it },
                            placeholder = "Name",
                            modifier = Modifier.fillMaxWidth(0.6f)
                        )
                    }
                }
            }

            item {
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SectionCardHeader("Foods", onAddFoods)
                        if (ingredients.isEmpty()) {
                            BuilderEmptyState("Add foods to start building your meal", "Add Food", onAddFoods)
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
                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text("Total", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                Text("$totalCals cal", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    "A meal is a labeled batch of foods you log together in one tap.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
