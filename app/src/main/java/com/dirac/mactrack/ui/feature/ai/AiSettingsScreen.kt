package com.dirac.mactrack.ui.feature.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dirac.mactrack.ui.common.BackBar

private val FieldShape = RoundedCornerShape(16.dp)

// Suggested Gemini model endpoints, cheapest/fastest first. gemini-3.5-flash-lite is the app default
// (main); gemini-2.5-flash is a solid higher-quality fallback. The field stays editable, so a homelab
// model name (e.g. "qwen2.5-vl") can be typed in when the base URL points at a local server.
private val SUGGESTED_MODELS = listOf(
    "gemini-3.5-flash-lite",
    "gemini-2.5-flash",
    "gemini-3.5-flash",
    "gemini-3.7-flash",
    "gemini-2.5-flash-lite",
    "gemini-2.5-pro"
)

// AI connection settings: base URL, model, and the API key (stored encrypted via the Keystore).
// Defaults to Gemini; changing the base URL points the same client at any OpenAI-compatible server.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val vm: AiViewModel = viewModel(factory = AiViewModel.Factory)
    val currentBaseUrl by vm.baseUrl.collectAsState()
    val currentModel by vm.model.collectAsState()
    val hasKey by vm.hasKey.collectAsState()
    val testResult by vm.testResult.collectAsState()

    var baseUrl by rememberSaveable { mutableStateOf(currentBaseUrl) }
    var model by rememberSaveable { mutableStateOf(currentModel) }
    var apiKey by rememberSaveable { mutableStateOf("") }
    var showKey by rememberSaveable { mutableStateOf(false) }
    var modelMenuOpen by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().imePadding()) {
        BackBar("AI settings", onBack, modifier = Modifier.padding(horizontal = 16.dp))

        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "MacTrack talks to any OpenAI-compatible chat API. The default is Google Gemini — get a " +
                        "free key at aistudio.google.com. Your key is stored encrypted on this device and is " +
                        "never shared. Photos and messages you send go to whichever provider you configure.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            }

            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                label = { Text("Base URL") },
                singleLine = true,
                shape = FieldShape,
                modifier = Modifier.fillMaxWidth()
            )
            ExposedDropdownMenuBox(
                expanded = modelMenuOpen,
                onExpandedChange = { modelMenuOpen = !modelMenuOpen }
            ) {
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Model") },
                    singleLine = true,
                    shape = FieldShape,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelMenuOpen) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = modelMenuOpen,
                    onDismissRequest = { modelMenuOpen = false }
                ) {
                    SUGGESTED_MODELS.forEach { m ->
                        DropdownMenuItem(
                            text = { Text(m) },
                            onClick = { model = m; modelMenuOpen = false }
                        )
                    }
                }
            }

            HorizontalDivider()

            Text(
                if (hasKey) "A key is saved. Leave the field blank to keep it, or paste a new one to replace it."
                else "No key saved yet.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("API key") },
                singleLine = true,
                shape = FieldShape,
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { showKey = !showKey }) {
                        Icon(
                            if (showKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (showKey) "Hide key" else "Show key"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    vm.setBaseUrl(baseUrl)
                    vm.setModel(model)
                    if (apiKey.isNotBlank()) {
                        vm.setApiKey(apiKey)
                        apiKey = ""
                    }
                    vm.clearTestResult()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { vm.testConnection() }, modifier = Modifier.weight(1f)) {
                    Text("Test connection")
                }
                if (hasKey) {
                    TextButton(onClick = { vm.clearApiKey() }) { Text("Remove key") }
                }
            }

            testResult?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
