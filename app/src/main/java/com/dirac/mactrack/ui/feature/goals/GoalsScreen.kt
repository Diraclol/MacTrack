package com.dirac.mactrack.ui.feature.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.roundToInt

@Composable
fun GoalsScreen(modifier: Modifier = Modifier) {
    val viewModel: GoalViewModel = viewModel(factory = GoalViewModel.Factory)
    val goal by viewModel.latestGoal.collectAsState()

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Your goal")
        val g = goal
        if (g == null) {
            Text("No goal set yet.")
        } else {
            Text("Calories: ${g.calorieGoal.roundToInt()}")
            Text("Protein: ${g.proteinGoalG.roundToInt()} g")
            Text("Carbs: ${g.carbGoalG.roundToInt()} g")
            Text("Fat: ${g.fatGoalG.roundToInt()} g")
        }
    }
}