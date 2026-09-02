package com.dirac.mactrack.ui.feature.ai

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dirac.mactrack.data.ai.ImageEncoder
import com.dirac.mactrack.data.ai.MacroParser
import com.dirac.mactrack.ui.common.DataUrlImage
import com.dirac.mactrack.ui.common.MarkdownText
import kotlinx.coroutines.launch

// The AI tab: an OpenWebUI-style chat. User bubbles right, assistant bubbles left, streaming replies.
// A gear opens AI settings (base URL, key, model). Conversation is in-memory for now.
@Composable
fun AiScreen(onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    val vm: AiViewModel = viewModel(factory = AiViewModel.Factory)
    val messages by vm.messages.collectAsState()
    val isStreaming by vm.isStreaming.collectAsState()
    val hasKey by vm.hasKey.collectAsState()
    val model by vm.model.collectAsState()
    val pendingImage by vm.pendingImage.collectAsState()

    var input by rememberSaveable { mutableStateOf("") }
    var review by remember { mutableStateOf<MacroParser.Estimate?>(null) }
    val listState = rememberLazyListState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Modern photo picker (no storage permission). The pick is downscaled + base64-encoded off the
    // main thread, then attached to the next send.
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            scope.launch {
                ImageEncoder.toDataUrl(context.contentResolver, it)?.let { dataUrl -> vm.attachImage(dataUrl) }
            }
        }
    }

    // Keep the newest message (and streaming text) in view.
    LaunchedEffect(messages.size, messages.lastOrNull()?.text) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Column(modifier = modifier.fillMaxSize().imePadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("AI", style = MaterialTheme.typography.titleLarge)
                Text("Powered by $model", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (messages.isNotEmpty()) {
                IconButton(onClick = { vm.clearChat() }) {
                    Icon(Icons.Filled.Add, contentDescription = "New chat")
                }
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Filled.Settings, contentDescription = "AI settings")
            }
        }

        if (messages.isEmpty()) {
            EmptyState(hasKey = hasKey, onOpenSettings = onOpenSettings, modifier = Modifier.weight(1f))
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items = messages, key = { it.id }) { m ->
                    MessageBubble(m)
                    // Offer to log a parsed estimate under assistant replies that contain macros.
                    if (m.role == "assistant" && !m.error && m.text.isNotBlank()) {
                        val est = remember(m.text) { MacroParser.parse(m.text) }
                        if (est.hasAny) {
                            TextButton(onClick = { review = est }) { Text("Log this") }
                        }
                    }
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
            // Attached-photo thumbnail (before sending), with a remove button.
            pendingImage?.let { img ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    DataUrlImage(dataUrl = img, modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)))
                    IconButton(onClick = { vm.clearPendingImage() }) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove photo")
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = {
                    imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) {
                    Icon(Icons.Filled.AddPhotoAlternate, contentDescription = "Attach photo")
                }
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("Ask about food or macros…") },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                    maxLines = 4,
                    modifier = Modifier.weight(1f)
                )
                FilledIconButton(
                    onClick = {
                        vm.send(input)
                        input = ""
                    },
                    enabled = (input.isNotBlank() || pendingImage != null) && !isStreaming
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                }
            }
        }
    }

    val currentReview = review
    if (currentReview != null) {
        LogReviewDialog(
            estimate = currentReview,
            onDismiss = { review = null },
            onConfirm = { name, cal, p, c, f ->
                vm.logEstimate(name, cal, p, c, f) {
                    Toast.makeText(context, "Logged to today", Toast.LENGTH_SHORT).show()
                }
                review = null
            }
        )
    }
}

@Composable
private fun LogReviewDialog(
    estimate: MacroParser.Estimate,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Double, Double, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var cal by remember { mutableStateOf(numField(estimate.calories)) }
    var protein by remember { mutableStateOf(numField(estimate.protein)) }
    var carb by remember { mutableStateOf(numField(estimate.carb)) }
    var fat by remember { mutableStateOf(numField(estimate.fat)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log to today") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(cal, { cal = it }, "Cal", Modifier.weight(1f))
                    NumberField(protein, { protein = it }, "P (g)", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(carb, { carb = it }, "C (g)", Modifier.weight(1f))
                    NumberField(fat, { fat = it }, "F (g)", Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(
                    name,
                    cal.toDoubleOrNull() ?: 0.0,
                    protein.toDoubleOrNull() ?: 0.0,
                    carb.toDoubleOrNull() ?: 0.0,
                    fat.toDoubleOrNull() ?: 0.0
                )
            }) { Text("Log") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun NumberField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier
    )
}

private fun numField(x: Double): String =
    if (x == 0.0) "" else if (x % 1.0 == 0.0) x.toInt().toString() else x.toString()

@Composable
private fun MessageBubble(m: UiMessage) {
    val isUser = m.role == "user"
    val colors = MaterialTheme.colorScheme
    val bg = when {
        m.error -> colors.errorContainer
        isUser -> colors.primaryContainer
        else -> colors.surfaceVariant
    }
    val fg = when {
        m.error -> colors.onErrorContainer
        isUser -> colors.onPrimaryContainer
        else -> colors.onSurface
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(color = bg, shape = RoundedCornerShape(18.dp), modifier = Modifier.widthIn(max = 320.dp)) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                if (m.imageDataUrl != null) {
                    DataUrlImage(
                        dataUrl = m.imageDataUrl,
                        modifier = Modifier.size(180.dp).clip(RoundedCornerShape(12.dp))
                    )
                    if (m.text.isNotBlank()) Spacer(Modifier.height(8.dp))
                }
                val textStyle = MaterialTheme.typography.bodyMedium
                when {
                    // Waiting on the first token (the model is processing the prompt): show a spinner.
                    m.text.isBlank() && m.imageDataUrl == null -> CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = fg
                    )
                    m.text.isBlank() -> Unit
                    isUser || m.error -> Text(m.text, color = fg, style = textStyle)
                    // Assistant replies may contain Markdown (bold, bullet lists); render it cleanly.
                    else -> MarkdownText(text = m.text, color = fg, style = textStyle)
                }
            }
        }
    }
}

@Composable
private fun EmptyState(hasKey: Boolean, onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("✨", style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(12.dp))
        Text("Ask about food and macros", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            "Estimate calories from a description, break down a meal, or plan your day. Nothing is logged automatically.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (!hasKey) {
            Spacer(Modifier.height(20.dp))
            FilledTonalButton(onClick = onOpenSettings) { Text("Add your API key") }
        }
    }
}
