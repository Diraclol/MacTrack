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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dirac.mactrack.data.builder.DraftIngredient
import com.dirac.mactrack.data.food.foodIcon
import com.dirac.mactrack.ui.common.BuilderEmptyState
import com.dirac.mactrack.ui.common.CreateTopBar
import com.dirac.mactrack.ui.common.EditAmountDialog
import com.dirac.mactrack.ui.common.IngredientDisplayRow
import com.dirac.mactrack.ui.common.InlineValueField
import com.dirac.mactrack.ui.common.LabeledFieldRow
import com.dirac.mactrack.ui.common.MacroPills
import com.dirac.mactrack.ui.common.SectionCardHeader
import com.dirac.mactrack.ui.common.SegmentedToggle

private fun amountText(x: Double): String =
    if (x % 1.0 == 0.0) x.toInt().toString() else (kotlin.math.round(x * 100.0) / 100.0).toString()

// Create Recipe: name, how many servings it makes, an optional finished (cooked) weight, and an
// ingredient list. The "+" / "Add ingredient" buttons open the food-search screen in picker mode
// (onAddIngredients). Ingredients show with their amount and macros, above a macro-summary pill row;
// a Per Serving / Recipe Total toggle divides everything by the serving count. Tap a row to change how
// much of that food the recipe uses, or remove it.
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
    val initial by viewModel.initial.collectAsState()

    var name by rememberSaveable { mutableStateOf("") }
    var makes by rememberSaveable { mutableStateOf("1") }
    var cooked by rememberSaveable { mutableStateOf("") }
    // false = Recipe Total (matches the reference default); true = Per Serving.
    var perServing by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf<DraftIngredient?>(null) }

    // When editing, seed the fields once from the loaded recipe.
    var seeded by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(initial) {
        val r = initial
        if (r != null && !seeded) {
            name = r.name
            makes = amountText(r.makesServings)
            cooked = r.cookedWeightG?.let { amountText(it) } ?: ""
            seeded = true
        }
    }

    val foodsById = foods.associateBy { it.id }
    val makesN = makes.toDoubleOrNull() ?: 1.0
    val div = if (perServing && makesN > 0.0) makesN else 1.0
    val canSave = name.isNotBlank() && makesN > 0.0 && ingredients.isNotEmpty()

    Column(modifier = modifier.fillMaxSize()) {
        CreateTopBar(
            title = if (viewModel.isEditing) "Edit Recipe" else "Create Recipe",
            onBack = onBack,
            saveEnabled = canSave,
            onSave = {
                viewModel.saveRecipe(
                    name = name,
                    makesServings = makesN,
                    cookedWeightG = cooked.toDoubleOrNull(),
                    emoji = initial?.emoji,
                    ingredients = ingredients.map { it.foodId to it.servings },
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
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionCardHeader("Ingredients", onAddIngredients)
                        if (ingredients.isEmpty()) {
                            BuilderEmptyState("Add ingredients to start building your recipe", "Add ingredient", onAddIngredients)
                        } else {
                            SegmentedToggle(
                                leftLabel = "Per Serving",
                                rightLabel = "Recipe Total",
                                leftSelected = perServing,
                                onSelectLeft = { perServing = true },
                                onSelectRight = { perServing = false }
                            )
                            val p = ingredients.sumOf { (foodsById[it.foodId]?.proteinG ?: 0.0) * it.servings } / div
                            val c = ingredients.sumOf { (foodsById[it.foodId]?.carbG ?: 0.0) * it.servings } / div
                            val f = ingredients.sumOf { (foodsById[it.foodId]?.fatG ?: 0.0) * it.servings } / div
                            val cal = ingredients.sumOf { (foodsById[it.foodId]?.calories ?: 0.0) * it.servings } / div
                            MacroPills(p, c, f, cal)
                            HorizontalDivider()
                            ingredients.forEach { ing ->
                                val food = foodsById[ing.foodId]
                                val size = food?.servingSize ?: 1.0
                                val unit = food?.servingUnit ?: "serving"
                                IngredientDisplayRow(
                                    icon = foodIcon(food?.emoji, ing.name),
                                    name = ing.name,
                                    amountLabel = "${amountText(ing.servings * size / div)} $unit",
                                    protein = (food?.proteinG ?: 0.0) * ing.servings / div,
                                    carb = (food?.carbG ?: 0.0) * ing.servings / div,
                                    fat = (food?.fatG ?: 0.0) * ing.servings / div,
                                    calories = (food?.calories ?: 0.0) * ing.servings / div,
                                    onClick = { editing = ing }
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    "For each ingredient, tap to set how much of it the whole recipe uses. A cooked weight " +
                        "lets you later log by grams of the finished dish.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (viewModel.isEditing) {
                item {
                    Button(
                        onClick = { viewModel.deleteRecipe(onSaved) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete Recipe", color = MaterialTheme.colorScheme.onError)
                    }
                }
            }
        }
    }

    val ed = editing
    if (ed != null) {
        val food = foodsById[ed.foodId]
        val size = food?.servingSize ?: 1.0
        val unit = food?.servingUnit ?: "serving"
        // The edit dialog always works in whole-recipe terms, regardless of the per-serving view.
        EditAmountDialog(
            name = ed.name,
            unit = unit,
            initialAmount = ed.servings * size,
            onDismiss = { editing = null },
            onConfirm = { amt ->
                if (size > 0.0) viewModel.setServings(ed.foodId, amt / size)
                editing = null
            },
            onRemove = {
                viewModel.removeIngredient(ed.foodId)
                editing = null
            }
        )
    }
}
