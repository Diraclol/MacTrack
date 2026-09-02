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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.dirac.mactrack.data.profile.AvatarStore
import com.dirac.mactrack.ui.common.AVATAR_EMOJIS
import com.dirac.mactrack.ui.common.ProfileAvatar
import com.dirac.mactrack.ui.common.BackBar
import com.dirac.mactrack.ui.common.EmojiPickerDialog
import com.dirac.mactrack.ui.feature.more.MoreStatsViewModel
import com.dirac.mactrack.ui.theme.ThemeViewModel
import kotlinx.coroutines.launch

private fun pretty(name: String) = name.lowercase().replaceFirstChar { it.uppercase() }

private fun trimPct(x: Double): String = if (x % 1.0 == 0.0) x.toInt().toString() else x.toString()

@Composable
private fun BodyFatDialog(current: Double?, onSet: (Double?) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(current?.let { trimPct(it) } ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Body fat %") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Optional. Leave blank to clear.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Body fat (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSet(text.toDoubleOrNull()?.takeIf { it in 0.0..100.0 }) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ProfileScreen(onBack: () -> Unit = {}, onReassessGoals: () -> Unit = {}, modifier: Modifier = Modifier) {
    val profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory)
    val statsViewModel: MoreStatsViewModel = viewModel(factory = MoreStatsViewModel.Factory)
    val themeViewModel: ThemeViewModel = viewModel(factory = ThemeViewModel.Factory)
    val profile by profileViewModel.profile.collectAsState()
    val stats by statsViewModel.stats.collectAsState()
    val avatar by themeViewModel.avatar.collectAsState()
    val avatarPhoto by themeViewModel.avatarPhotoPath.collectAsState()
    var showAvatarPicker by remember { mutableStateOf(false) }
    var showAvatarChooser by remember { mutableStateOf(false) }
    var showBodyFatDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Pick a photo from the gallery, copy it into app storage, and use it as the avatar.
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val path = AvatarStore.save(context, uri)
                if (path != null) themeViewModel.setAvatarPhoto(path)
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { BackBar("Profile", onBack) }

        item {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                ProfileAvatar(
                    emoji = avatar,
                    photoPath = avatarPhoto,
                    size = 64.dp,
                    emojiStyle = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.clickable { showAvatarChooser = true }
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("You", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Tap the avatar to change it",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
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
                        InfoRow("Height", "${p.heightCm} cm", last = true)
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth().clickable { showBodyFatDialog = true }) {
                    Column(Modifier.padding(vertical = 4.dp)) {
                        InfoRow(
                            "Body fat",
                            p.bodyFatPct?.let { "${trimPct(it)} %" } ?: "Tap to add (optional)",
                            last = true
                        )
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

    if (showAvatarChooser) {
        AlertDialog(
            onDismissRequest = { showAvatarChooser = false },
            title = { Text("Change avatar") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            showAvatarChooser = false
                            photoLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Choose a photo") }
                    TextButton(
                        onClick = { showAvatarChooser = false; showAvatarPicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Choose an emoji") }
                    if (avatarPhoto != null) {
                        TextButton(
                            onClick = { themeViewModel.clearAvatarPhoto(); showAvatarChooser = false },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Remove photo") }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showAvatarChooser = false }) { Text("Cancel") } }
        )
    }

    if (showAvatarPicker) {
        EmojiPickerDialog(
            title = "Choose an avatar",
            current = avatar,
            choices = AVATAR_EMOJIS,
            onPick = { themeViewModel.setAvatar(it); showAvatarPicker = false },
            onDismiss = { showAvatarPicker = false }
        )
    }

    if (showBodyFatDialog) {
        BodyFatDialog(
            current = profile?.bodyFatPct,
            onSet = { profileViewModel.setBodyFat(it); showBodyFatDialog = false },
            onDismiss = { showBodyFatDialog = false }
        )
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
