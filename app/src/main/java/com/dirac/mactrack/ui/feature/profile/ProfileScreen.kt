package com.dirac.mactrack.ui.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dirac.mactrack.ui.common.BackBar
import com.dirac.mactrack.ui.feature.more.MoreStatsViewModel

private fun pretty(name: String) = name.lowercase().replaceFirstChar { it.uppercase() }

@Composable
fun ProfileScreen(onBack: () -> Unit = {}, onReassessGoals: () -> Unit = {}, modifier: Modifier = Modifier) {
    val profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory)
    val statsViewModel: MoreStatsViewModel = viewModel(factory = MoreStatsViewModel.Factory)
    val profile by profileViewModel.profile.collectAsState()
    val stats by statsViewModel.stats.collectAsState()

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { BackBar("Profile", onBack) }

        item {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🧑", style = MaterialTheme.typography.headlineMedium)
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("You", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${stats.activeStreak}-day streak · ${stats.totalTracked} days tracked",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        val p = profile
        if (p == null) {
            item {
                Text(
                    "No profile yet. Complete onboarding to set your stats.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(vertical = 4.dp)) {
                        InfoRow("Sex", pretty(p.sex))
                        InfoRow("Age", "${p.age}")
                        InfoRow("Weight", "${p.weightKg} kg")
                        InfoRow("Height", "${p.heightCm} cm")
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth().clickable { onReassessGoals() }) {
                    Column(Modifier.padding(vertical = 4.dp)) {
                        InfoRow("Activity", pretty(p.activityLevel))
                        InfoRow("Goal", pretty(p.goalType))
                        InfoRow("Protein", pretty(p.proteinLevel))
                        InfoRow("Fat", pretty(p.fatLevel), last = true)
                        Text(
                            "Tap to reassess your goals",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        item {
            Text(
                "Account, login, and units are coming with accounts later.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, last: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (!last) HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}
