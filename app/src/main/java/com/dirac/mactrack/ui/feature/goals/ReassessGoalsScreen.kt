package com.dirac.mactrack.ui.feature.goals

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dirac.mactrack.domain.calc.ActivityLevel
import com.dirac.mactrack.domain.calc.FatLevel
import com.dirac.mactrack.domain.calc.GoalType
import com.dirac.mactrack.domain.calc.MacroTargets
import com.dirac.mactrack.domain.calc.ProteinLevel
import com.dirac.mactrack.domain.calc.Sex
import com.dirac.mactrack.domain.calc.macroTargets
import com.dirac.mactrack.domain.calc.mifflinStJeorBmr
import com.dirac.mactrack.domain.calc.tdee
import com.dirac.mactrack.ui.common.BackBar
import com.dirac.mactrack.ui.feature.profile.ProfileViewModel
import kotlin.math.roundToInt

private fun pretty(name: String) = name.lowercase().replaceFirstChar { it.uppercase() }
private inline fun <reified T : Enum<T>> parseEnum(value: String, default: T): T =
    runCatching { enumValueOf<T>(value) }.getOrDefault(default)

// Reassess goals: a popup offers "Recalculate" (the TDEE algorithm, adjusting activity / goal /
// protein / fat while keeping the physical stats) or "Custom (advanced)" (set the targets by hand).
@Composable
fun ReassessGoalsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val goalViewModel: GoalViewModel = viewModel(factory = GoalViewModel.Factory)
    val profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory)
    val profile by profileViewModel.profile.collectAsState()
    val currentGoal by goalViewModel.latestGoal.collectAsState()

    // null = show the chooser popup; "algo" or "custom" once chosen.
    var mode by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BackBar("Reassess goals", onBack)

        when (mode) {
            "algo" -> AlgoReassess(
                profile = profile,
                onSwitch = { mode = null },
                onSave = { levels, targets ->
                    val p = profile ?: return@AlgoReassess
                    profileViewModel.saveProfile(
                        sex = p.sex, age = p.age, weightKg = p.weightKg, heightCm = p.heightCm,
                        activityLevel = levels.activity.name, goalType = levels.goal.name,
                        proteinLevel = levels.protein.name, fatLevel = levels.fat.name
                    )
                    goalViewModel.saveGoal(targets.calories, targets.proteinG, targets.carbG, targets.fatG)
                    onBack()
                }
            )
            "custom" -> CustomReassess(
                calorie = currentGoal?.calorieGoal,
                proteinG = currentGoal?.proteinGoalG,
                carbG = currentGoal?.carbGoalG,
                fatG = currentGoal?.fatGoalG,
                onSwitch = { mode = null },
                onSave = { c, p, ca, f -> goalViewModel.saveGoal(c, p, ca, f); onBack() }
            )
            else -> Text(
                "Choose how to reassess your goals.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (mode == null) {
        AlertDialog(
            onDismissRequest = onBack,
            title = { Text("Reassess your goals") },
            text = { Text("Recalculate from your profile with the app's TDEE algorithm, or set custom targets yourself.") },
            confirmButton = { TextButton(onClick = { mode = "algo" }) { Text("Recalculate (TDEE)") } },
            dismissButton = { TextButton(onClick = { mode = "custom" }) { Text("Custom (advanced)") } }
        )
    }
}

private data class Levels(
    val activity: ActivityLevel,
    val goal: GoalType,
    val protein: ProteinLevel,
    val fat: FatLevel
)

@Composable
private fun AlgoReassess(
    profile: com.dirac.mactrack.data.entity.UserProfile?,
    onSwitch: () -> Unit,
    onSave: (Levels, MacroTargets) -> Unit
) {
    val p = profile
    if (p == null) {
        Text("Complete onboarding first so we have your height, weight, and age.")
        TextButton(onClick = onSwitch) { Text("Use custom instead") }
        return
    }

    var activity by remember(p) { mutableStateOf(parseEnum(p.activityLevel, ActivityLevel.SEDENTARY)) }
    var goal by remember(p) { mutableStateOf(parseEnum(p.goalType, GoalType.MAINTAIN)) }
    var protein by remember(p) { mutableStateOf(parseEnum(p.proteinLevel, ProteinLevel.MODERATE)) }
    var fat by remember(p) { mutableStateOf(parseEnum(p.fatLevel, FatLevel.MODERATE)) }

    val bmr = mifflinStJeorBmr(parseEnum(p.sex, Sex.MALE), p.weightKg, p.heightCm, p.age)
    val maintenance = tdee(bmr, activity)
    val target = maintenance + goal.defaultAdjustment
    val targets = macroTargets(target, p.weightKg * 2.20462, protein.gramsPerLb, fat.fraction)

    Text("Recalculate (TDEE)", style = MaterialTheme.typography.titleMedium)
    Text("Physical stats stay as they are; adjust the four levers below.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

    ChipGroup("Activity level", ActivityLevel.entries, activity, { pretty(it.name) }) { activity = it }
    ChipGroup("Goal", GoalType.entries, goal, { it.label }) { goal = it }
    ChipGroup("Protein", ProteinLevel.entries, protein, { pretty(it.name) }) { protein = it }
    ChipGroup("Fat", FatLevel.entries, fat, { pretty(it.name) }) { fat = it }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Maintenance (TDEE): ${maintenance.roundToInt()} cal", style = MaterialTheme.typography.bodyMedium)
            Text("Target: ${targets.calories.roundToInt()} cal", style = MaterialTheme.typography.titleSmall)
            Text("Protein ${targets.proteinG.roundToInt()} g   Carbs ${targets.carbG.roundToInt()} g   Fat ${targets.fatG.roundToInt()} g", style = MaterialTheme.typography.bodyMedium)
        }
    }

    Button(onClick = { onSave(Levels(activity, goal, protein, fat), targets) }, modifier = Modifier.fillMaxWidth()) {
        Text("Save new goal")
    }
    TextButton(onClick = onSwitch) { Text("Change method") }
}

@Composable
private fun CustomReassess(
    calorie: Double?,
    proteinG: Double?,
    carbG: Double?,
    fatG: Double?,
    onSwitch: () -> Unit,
    onSave: (Double, Double, Double, Double) -> Unit
) {
    var calStr by remember(calorie) { mutableStateOf(calorie?.roundToInt()?.toString() ?: "") }
    var proteinStr by remember(proteinG) { mutableStateOf(proteinG?.roundToInt()?.toString() ?: "") }
    var carbStr by remember(carbG) { mutableStateOf(carbG?.roundToInt()?.toString() ?: "") }
    var fatStr by remember(fatG) { mutableStateOf(fatG?.roundToInt()?.toString() ?: "") }

    Text("Custom (advanced)", style = MaterialTheme.typography.titleMedium)
    Text("Set your calorie and macro targets directly.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

    NumField(calStr, { calStr = it }, "Calories")
    NumField(proteinStr, { proteinStr = it }, "Protein (g)")
    NumField(carbStr, { carbStr = it }, "Carbs (g)")
    NumField(fatStr, { fatStr = it }, "Fat (g)")

    Button(
        onClick = {
            onSave(
                calStr.toDoubleOrNull() ?: 0.0,
                proteinStr.toDoubleOrNull() ?: 0.0,
                carbStr.toDoubleOrNull() ?: 0.0,
                fatStr.toDoubleOrNull() ?: 0.0
            )
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Save new goal")
    }
    TextButton(onClick = onSwitch) { Text("Change method") }
}

@Composable
private fun <T> ChipGroup(label: String, options: List<T>, selected: T, labelOf: (T) -> String, onSelect: (T) -> Unit) {
    Text(label, style = MaterialTheme.typography.labelLarge)
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            FilterChip(selected = selected == option, onClick = { onSelect(option) }, label = { Text(labelOf(option)) })
        }
    }
}

@Composable
private fun NumField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}
