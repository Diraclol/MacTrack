package com.dirac.mactrack.ui.feature.more

import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dirac.mactrack.data.food.FAVORITE_UNIT_CATALOG
import com.dirac.mactrack.data.food.GenericServingUnit
import com.dirac.mactrack.ui.common.BackBar
import com.dirac.mactrack.ui.theme.ThemeViewModel
import kotlin.math.roundToInt

@Composable
fun FavoriteUnitsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeViewModel: ThemeViewModel = viewModel(factory = ThemeViewModel.Factory)
    val favorites by themeViewModel.favoriteUnits.collectAsState()

    // Tap toggles a pin. At the 2-pin cap, adding a third drops the oldest (never a dead end).
    fun toggle(key: String) {
        val next = if (key in favorites) favorites - key else (favorites + key).takeLast(2)
        themeViewModel.setFavoriteUnits(next)
    }

    Column(modifier = modifier.fillMaxSize()) {
        BackBar(
            "Favorite Serving Units",
            onBack,
            modifier = Modifier.padding(horizontal = 16.dp).padding(top = 8.dp)
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Pin up to 2 units to the front of the serving picker on every food, so your go-to " +
                        "units are always first. (Pinning a third replaces the oldest.)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                Text(
                    if (favorites.isEmpty()) "None pinned yet"
                    else "Pinned: " + favorites.joinToString(", "),
                    style = MaterialTheme.typography.labelLarge
                )
            }
            item { SectionHeader("WEIGHT") }
            items(FAVORITE_UNIT_CATALOG.filter { !it.isVolume }, key = { it.key }) { u ->
                UnitRow(u, checked = u.key in favorites, onToggle = { toggle(u.key) })
            }
            item { SectionHeader("VOLUME (ESTIMATED)") }
            item {
                Text(
                    "Volume conversions assume about 1 g per ml, so the gram weights they show are " +
                        "estimates.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(FAVORITE_UNIT_CATALOG.filter { it.isVolume }, key = { it.key }) { u ->
                UnitRow(u, checked = u.key in favorites, onToggle = { toggle(u.key) })
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp)
    )
}

// The gram size shown under each unit. "g" needs no figure; volume units are flagged with "~".
private fun sizeCaption(u: GenericServingUnit): String = when {
    u.key == "g" -> "grams"
    u.isVolume -> "~${u.grams.roundToInt()} g"
    else -> "${u.grams.roundToInt()} g"
}

@Composable
private fun UnitRow(u: GenericServingUnit, checked: Boolean, onToggle: () -> Unit) {
    // The whole row is one toggleable target (announced to TalkBack as a checkbox with the unit's
    // label + state); the Checkbox just reflects state, so a tap fires the toggle exactly once.
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, onValueChange = { onToggle() }, role = Role.Checkbox)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(u.label, style = MaterialTheme.typography.titleMedium)
                Text(
                    sizeCaption(u),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Checkbox(checked = checked, onCheckedChange = null)
        }
    }
}
