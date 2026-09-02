package com.dirac.mactrack.ui.feature.streak

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dirac.mactrack.ui.common.BackBar
import com.dirac.mactrack.ui.feature.more.MoreStatsViewModel
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

// Food-logging history as a scrollable month calendar (reached from the dashboard Food Logging card).
// A filled blue day = at least one entry logged that day; a plain day = missed. Newest month on top.
@Composable
fun FoodLogCalendarScreen(onBack: () -> Unit = {}, modifier: Modifier = Modifier) {
    val vm: FoodLogCalendarViewModel = viewModel(factory = FoodLogCalendarViewModel.Factory)
    val logged by vm.loggedDates.collectAsState()
    val statsVm: MoreStatsViewModel = viewModel(factory = MoreStatsViewModel.Factory)
    val stats by statsVm.stats.collectAsState()

    val months = remember { (0 until 12).map { YearMonth.now().minusMonths(it.toLong()) } }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        BackBar("Food logging", onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem("${stats.activeStreak}", "day streak")
                        StatItem("${stats.totalTracked}", "days tracked")
                        StatItem("${stats.longestStreak}", "longest streak")
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                    Text(
                        "Days you logged food",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(months) { m -> MonthCalendar(month = m, logged = logged) }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MonthCalendar(month: YearMonth, logged: Set<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "${month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.year}",
            style = MaterialTheme.typography.titleMedium
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { d ->
                Text(
                    d,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        // Sunday-start grid: pad the first week so day 1 lands under its weekday column.
        val lead = month.atDay(1).dayOfWeek.value % 7
        val cells: List<Int?> = List(lead) { null } + (1..month.lengthOfMonth()).toList()
        cells.chunked(7).forEach { week ->
            val padded = week + List(7 - week.size) { null }
            Row(modifier = Modifier.fillMaxWidth()) {
                padded.forEach { day ->
                    Box(
                        modifier = Modifier.weight(1f).aspectRatio(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (day != null) {
                            val tracked = month.atDay(day).toString() in logged
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(if (tracked) MaterialTheme.colorScheme.primary else Color.Transparent),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "$day",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (tracked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
