package com.dirac.mactrack.ui.feature.meals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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

private fun amountText(x: Double): String =
    if (x % 1.0 == 0.0) x.toInt().toString() else (kotlin.math.round(x * 100.0) / 100.0).toString()

// Create Meal: a labeled batch of foods logged together in one tap. The "+" / "Add Food" buttons open
// the food-search screen in picker mode (onAddFoods); picks land in the shared draft and show up here
// as rows with each food's amount and macro contribution, plus a macro-summary pill row. Tap a row to
// change how much of that food the meal uses, or remove it.
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
    var editing by remember { mutableStateOf<DraftIngredient?>(null) }

    val foodsById = foods.associateBy { it.id }
    val canSave = mealName.isNotBlank() && ingredients.isNotEmpty()

    Column(modifier = modifier.fillMaxSize()) {
        CreateTopBar(
            title = "Create Meal",
            onBack = onBack,
            saveEnabled = canSave,
            onSave = { viewModel.saveMeal(mealName, ingredients.map { it.foodId to it.servings }, onSaved) }
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
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionCardHeader("Foods", onAddFoods)
                        if (ingredients.isEmpty()) {
                            BuilderEmptyState("Add foods to start building your meal", "Add Food", onAddFoods)
                        } else {
                            val p = ingredients.sumOf { (foodsById[it.foodId]?.proteinG ?: 0.0) * it.servings }
                            val c = ingredients.sumOf { (foodsById[it.foodId]?.carbG ?: 0.0) * it.servings }
                            val f = ingredients.sumOf { (foodsById[it.foodId]?.fatG ?: 0.0) * it.servings }
                            val cal = ingredients.sumOf { (foodsById[it.foodId]?.calories ?: 0.0) * it.servings }
                            MacroPills(p, c, f, cal)
                            HorizontalDivider()
                            ingredients.forEach { ing ->
                                val food = foodsById[ing.foodId]
                                val size = food?.servingSize ?: 1.0
                                val unit = food?.servingUnit ?: "serving"
                                IngredientDisplayRow(
                                    icon = foodIcon(food?.emoji, ing.name),
                                    name = ing.name,
                                    amountLabel = "${amountText(ing.servings * size)} $unit",
                                    protein = (food?.proteinG ?: 0.0) * ing.servings,
                                    carb = (food?.carbG ?: 0.0) * ing.servings,
                                    fat = (food?.fatG ?: 0.0) * ing.servings,
                                    calories = (food?.calories ?: 0.0) * ing.servings,
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
                    "A meal is a labeled batch of foods you log together in one tap.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    val ed = editing
    if (ed != null) {
        val food = foodsById[ed.foodId]
        val size = food?.servingSize ?: 1.0
        val unit = food?.servingUnit ?: "serving"
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
