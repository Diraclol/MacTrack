package com.dirac.mactrack.ui.feature.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dirac.mactrack.ui.common.BackBar
import kotlin.math.roundToInt

// Goals and reassessment are folded into one screen: it shows the current goal and offers a single
// "Reassess goals" action (which opens the recalculate-or-custom flow).
@Composable
fun GoalsScreen(modifier: Modifier = Modifier, onBack: () -> Unit = {}, onReassess: () -> Unit = {}) {
    val viewModel: GoalViewModel = viewModel(factory = GoalViewModel.Factory)
    val goal by viewModel.latestGoal.collectAsState()

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BackBar("Goals", onBack)
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Current goal", style = MaterialTheme.typography.titleMedium)
                val g = goal
                if (g == null) {
                    Text("No goal set yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    GoalRow("Calories", "${g.calorieGoal.roundToInt()} cal")
                    GoalRow("Protein", "${g.proteinGoalG.roundToInt()} g")
                    GoalRow("Carbs", "${g.carbGoalG.roundToInt()} g")
                    GoalRow("Fat", "${g.fatGoalG.roundToInt()} g")
                }
            }
        }
        Button(onClick = onReassess, modifier = Modifier.fillMaxWidth()) {
            Text("Reassess goals")
        }
        Text(
            "Recalculate from your profile with the TDEE algorithm, or set custom targets by hand.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun GoalRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
