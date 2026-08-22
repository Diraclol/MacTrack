package com.dirac.mactrack.ui.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dirac.mactrack.domain.calc.ActivityLevel
import com.dirac.mactrack.domain.calc.FatLevel
import com.dirac.mactrack.domain.calc.GoalType
import com.dirac.mactrack.domain.calc.MacroTargets
import com.dirac.mactrack.domain.calc.ProteinLevel
import com.dirac.mactrack.domain.calc.Sex
import com.dirac.mactrack.domain.calc.macroTargets
import com.dirac.mactrack.domain.calc.mifflinStJeorBmr
import com.dirac.mactrack.domain.calc.tdee
import java.util.Locale
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dirac.mactrack.ui.feature.goals.GoalViewModel
import com.dirac.mactrack.ui.feature.profile.ProfileViewModel

private fun pretty(name: String) = name.lowercase().replaceFirstChar { it.uppercase() }
private fun Double.clean(): String = String.format(Locale.US, "%.0f", this)

@Composable
fun OnboardingScreen(modifier: Modifier = Modifier) {
    var sex by remember { mutableStateOf(Sex.MALE) }
    var age by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var activity by remember { mutableStateOf(ActivityLevel.SEDENTARY) }
    var goal by remember { mutableStateOf(GoalType.MAINTAIN) }
    var protein by remember { mutableStateOf(ProteinLevel.MODERATE) }
    var fat by remember { mutableStateOf(FatLevel.MODERATE) }
    var tdeeValue by remember { mutableStateOf<Double?>(null) }
    var result by remember { mutableStateOf<MacroTargets?>(null) }
    val goalViewModel: GoalViewModel = viewModel(factory = GoalViewModel.Factory)
    val profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Set up your profile")

        Text("Sex")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = sex == Sex.MALE, onClick = { sex = Sex.MALE }, label = { Text("Male") })
            FilterChip(selected = sex == Sex.FEMALE, onClick = { sex = Sex.FEMALE }, label = { Text("Female") })
        }

        OutlinedTextField(
            value = age, onValueChange = { age = it }, label = { Text("Age") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = weight, onValueChange = { weight = it }, label = { Text("Weight (kg)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = height, onValueChange = { height = it }, label = { Text("Height (cm)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        Text("Activity level")
        ActivityLevel.entries.forEach { level ->
            FilterChip(selected = activity == level, onClick = { activity = level }, label = { Text(pretty(level.name)) })
        }

        Text("Goal")
        GoalType.entries.forEach { g ->
            FilterChip(selected = goal == g, onClick = { goal = g }, label = { Text(g.label) })
        }

        Text("Protein")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ProteinLevel.entries.forEach { p ->
                FilterChip(selected = protein == p, onClick = { protein = p }, label = { Text(pretty(p.name)) })
            }
        }

        Text("Fat")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FatLevel.entries.forEach { f ->
                FilterChip(selected = fat == f, onClick = { fat = f }, label = { Text(pretty(f.name)) })
            }
        }

        Button(
            onClick = {
                val ageVal = age.toIntOrNull()
                val weightVal = weight.toDoubleOrNull()
                val heightVal = height.toDoubleOrNull()
                if (ageVal != null && weightVal != null && heightVal != null) {
                    val bmr = mifflinStJeorBmr(sex, weightVal, heightVal, ageVal)
                    val tdeeResult = tdee(bmr, activity)
                    val target = tdeeResult + goal.defaultAdjustment
                    val weightLb = weightVal * 2.20462
                    tdeeValue = tdeeResult
                    result = macroTargets(
                        targetCalories = target,
                        bodyWeightLb = weightLb,
                        proteinPerLb = protein.gramsPerLb,
                        fatFraction = fat.fraction
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate")
        }

        val r = result
        val t = tdeeValue
        if (r != null && t != null) {
            Text("Maintenance (TDEE): ${t.clean()}")
            Text("Target calories: ${r.calories.clean()}")
            Text("Protein: ${r.proteinG.clean()} g")
            Text("Carbs: ${r.carbG.clean()} g")
            Text("Fat: ${r.fatG.clean()} g")
            Button(
                onClick = {
                    profileViewModel.saveProfile(
                        sex = sex.name,
                        age = age.toIntOrNull() ?: 0,
                        weightKg = weight.toDoubleOrNull() ?: 0.0,
                        heightCm = height.toDoubleOrNull() ?: 0.0,
                        activityLevel = activity.name,
                        goalType = goal.name,
                        proteinLevel = protein.name,
                        fatLevel = fat.name
                    )
                    goalViewModel.saveGoal(r.calories, r.proteinG, r.carbG, r.fatG)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save & continue")
            }
        }
    }
}