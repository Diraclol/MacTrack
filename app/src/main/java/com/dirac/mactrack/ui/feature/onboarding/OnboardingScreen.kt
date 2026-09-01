package com.dirac.mactrack.ui.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dirac.mactrack.domain.calc.ActivityLevel
import com.dirac.mactrack.domain.calc.FatLevel
import com.dirac.mactrack.domain.calc.GoalType
import com.dirac.mactrack.domain.calc.ProteinLevel
import com.dirac.mactrack.domain.calc.Sex
import com.dirac.mactrack.domain.calc.macroTargets
import com.dirac.mactrack.domain.calc.mifflinStJeorBmr
import com.dirac.mactrack.domain.calc.tdee
import com.dirac.mactrack.ui.feature.goals.GoalViewModel
import com.dirac.mactrack.ui.feature.profile.ProfileViewModel
import java.util.Locale
import kotlin.math.roundToInt

private fun activityDesc(a: ActivityLevel): String = when (a) {
    ActivityLevel.SEDENTARY -> "Little or no exercise, desk job"
    ActivityLevel.LIGHT -> "Light exercise 1–3 days a week"
    ActivityLevel.MODERATE -> "Moderate exercise 3–5 days a week"
    ActivityLevel.HEAVY -> "Hard exercise 6–7 days a week"
    ActivityLevel.ATHLETE -> "Very hard exercise or a physical job"
}

private fun proteinLabel(p: ProteinLevel): String = when (p) {
    ProteinLevel.MIN -> "Low"
    ProteinLevel.MODERATE -> "Moderate"
    ProteinLevel.HIGH -> "High"
}

private fun proteinDesc(p: ProteinLevel): String = when (p) {
    ProteinLevel.MIN -> "Lower end of the optimal range"
    ProteinLevel.MODERATE -> "Middle of the optimal range"
    ProteinLevel.HIGH -> "High end of the optimal range"
}

private fun fatLabel(f: FatLevel): String = when (f) {
    FatLevel.LOW -> "Low fat"
    FatLevel.MODERATE -> "Balanced"
    FatLevel.HIGH -> "Higher fat"
}

private fun fatDesc(f: FatLevel): String = when (f) {
    FatLevel.LOW -> "Less fat, more room for carbs"
    FatLevel.MODERATE -> "A standard split of carbs and fat"
    FatLevel.HIGH -> "More fat, fewer carbs"
}

private fun goalDesc(g: GoalType): String = when (g) {
    GoalType.LOSE -> "Run a calorie deficit"
    GoalType.MAINTAIN -> "Stay at your current weight"
    GoalType.GAIN -> "Run a calorie surplus"
    GoalType.RECOMP -> "Slight deficit while building muscle"
    GoalType.MAINGAIN -> "A small surplus"
    GoalType.LEAN_BULK -> "A moderate surplus"
}

private fun Double.kcal(): String = String.format(Locale.US, "%.0f", this)

private const val LAST_STEP = 9

@Composable
fun OnboardingScreen(modifier: Modifier = Modifier) {
    val goalViewModel: GoalViewModel = viewModel(factory = GoalViewModel.Factory)
    val profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory)

    var step by remember { mutableIntStateOf(0) }
    var sex by remember { mutableStateOf(Sex.MALE) }
    var age by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var activity by remember { mutableStateOf(ActivityLevel.MODERATE) }
    var goal by remember { mutableStateOf(GoalType.MAINTAIN) }
    var protein by remember { mutableStateOf(ProteinLevel.MODERATE) }
    var fat by remember { mutableStateOf(FatLevel.MODERATE) }
    var showAdvanced by remember { mutableStateOf(false) }

    val ageVal = age.toIntOrNull()
    val heightVal = height.toDoubleOrNull()
    val weightVal = weight.toDoubleOrNull()
    val bmr = if (ageVal != null && weightVal != null && heightVal != null)
        mifflinStJeorBmr(sex, weightVal, heightVal, ageVal) else null
    val tdeeVal = bmr?.let { tdee(it, activity) }
    val targetCal = tdeeVal?.let { it + goal.defaultAdjustment }
    val targets = if (targetCal != null && weightVal != null)
        macroTargets(targetCal, weightVal * 2.20462, protein.gramsPerLb, fat.fraction) else null

    fun canAdvance(): Boolean = when (step) {
        1 -> ageVal != null && ageVal in 10..100
        2 -> heightVal != null && heightVal > 0
        3 -> weightVal != null && weightVal > 0
        else -> true
    }

    fun finish() {
        val t = targets ?: return
        profileViewModel.saveProfile(
            sex = sex.name, age = ageVal ?: 0, weightKg = weightVal ?: 0.0, heightCm = heightVal ?: 0.0,
            activityLevel = activity.name, goalType = goal.name, proteinLevel = protein.name, fatLevel = fat.name
        )
        goalViewModel.saveGoal(t.calories, t.proteinG, t.carbG, t.fatG)
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        StepProgress(step + 1, LAST_STEP + 1)
        Spacer(Modifier.height(20.dp))

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (step) {
                0 -> {
                    StepTitle("What is your sex?")
                    OptionCard("Female", null, sex == Sex.FEMALE) { sex = Sex.FEMALE }
                    OptionCard("Male", null, sex == Sex.MALE) { sex = Sex.MALE }
                }
                1 -> {
                    StepTitle("How old are you?")
                    NumberField(age, { age = it }, "Age", decimal = false)
                }
                2 -> {
                    StepTitle("What is your height?")
                    NumberField(height, { height = it }, "Height (cm)", decimal = true)
                }
                3 -> {
                    StepTitle("What is your weight?")
                    NumberField(weight, { weight = it }, "Weight (kg)", decimal = true)
                }
                4 -> {
                    StepTitle("How active are you?")
                    Text(
                        "Your daily activity outside of exercise.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ActivityLevel.entries.forEach { a ->
                        OptionCard(pretty(a.name), activityDesc(a), activity == a) { activity = a }
                    }
                }
                5 -> {
                    StepTitle("We estimated your daily expenditure")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${(tdeeVal ?: 0.0).kcal()} kcal",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "That's roughly the calories you'd eat to maintain your current weight. It's an " +
                            "estimate to start — it sharpens as you log food and weight.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                6 -> {
                    StepTitle("What is your goal?")
                    GoalType.entries.filter { !it.advanced || showAdvanced }.forEach { g ->
                        OptionCard(g.label, goalDesc(g), goal == g) { goal = g }
                    }
                    TextButton(onClick = { showAdvanced = !showAdvanced }) {
                        Text(if (showAdvanced) "Fewer options" else "Advanced goals")
                    }
                    if (targetCal != null) {
                        Text(
                            "Daily target: ${targetCal.kcal()} kcal" +
                                if (goal.defaultAdjustment != 0.0) "  (${if (goal.defaultAdjustment > 0) "+" else ""}${goal.defaultAdjustment.roundToInt()} vs maintenance)" else "",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                7 -> {
                    StepTitle("Fat preference")
                    Text(
                        "How much of your calories come from fat vs carbs.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FatLevel.entries.forEach { f ->
                        OptionCard(fatLabel(f), fatDesc(f), fat == f) { fat = f }
                    }
                }
                8 -> {
                    StepTitle("Protein preference")
                    ProteinLevel.entries.forEach { p ->
                        OptionCard(proteinLabel(p), proteinDesc(p), protein == p) { protein = p }
                    }
                }
                9 -> {
                    StepTitle("You're all set")
                    val t = targets
                    if (t != null) {
                        Text("${t.calories.kcal()} kcal", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                        Text(
                            "Protein ${t.proteinG.kcal()} g · Carbs ${t.carbG.kcal()} g · Fat ${t.fatG.kcal()} g",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "You can reassess any time from More → Goals.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (step > 0) {
                OutlinedButton(onClick = { step-- }, modifier = Modifier.weight(1f)) { Text("Back") }
            }
            Button(
                onClick = { if (step < LAST_STEP) step++ else finish() },
                enabled = canAdvance(),
                modifier = Modifier.weight(1f)
            ) {
                Text(if (step < LAST_STEP) "Next" else "Finish")
            }
        }
    }
}

private fun pretty(name: String) = name.lowercase().replaceFirstChar { it.uppercase() }

@Composable
private fun StepProgress(current: Int, total: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(total) { i ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (i < current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    }
}

@Composable
private fun StepTitle(text: String) {
    Text(text, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
}

@Composable
private fun OptionCard(title: String, subtitle: String?, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(14.dp)
            )
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun NumberField(value: String, onValueChange: (String) -> Unit, label: String, decimal: Boolean) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    )
}
