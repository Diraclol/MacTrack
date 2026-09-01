package com.dirac.mactrack.ui.feature.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dirac.mactrack.MacTrackApplication
import com.dirac.mactrack.data.ai.AiClient
import com.dirac.mactrack.data.ai.AiSettingsStore
import com.dirac.mactrack.data.ai.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// One message in the chat, as shown. `error = true` renders it as an error notice rather than a reply.
data class UiMessage(
    val id: Long,
    val role: String,   // "user" | "assistant"
    val text: String,
    val error: Boolean = false
)

private const val SYSTEM_PROMPT =
    "You are MacTrack's built-in nutrition assistant. Help the user understand food, calories, and " +
        "macronutrients (protein, carbs, fat). When you estimate a food's nutrition, give calories and " +
        "protein/carbs/fat in grams, note that it's an estimate, and keep replies concise. If you are " +
        "unsure, say so rather than inventing precise numbers."

// Backs the AI chat tab. Conversation is in-memory (Slice 1) -- it survives tab switches/rotation but
// resets when the app is killed. Settings (base URL, model, key presence) come from AiSettingsStore.
class AiViewModel(
    private val client: AiClient,
    private val settings: AiSettingsStore
) : ViewModel() {

    private val _messages = MutableStateFlow<List<UiMessage>>(emptyList())
    val messages: StateFlow<List<UiMessage>> = _messages.asStateFlow()

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    val baseUrl: StateFlow<String> = settings.baseUrl
    val model: StateFlow<String> = settings.model
    val hasKey: StateFlow<Boolean> = settings.hasKey

    private val _testResult = MutableStateFlow<String?>(null)
    val testResult: StateFlow<String?> = _testResult.asStateFlow()

    private var nextId = 0L
    private fun add(message: UiMessage): Long {
        _messages.value = _messages.value + message
        return message.id
    }

    fun send(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _isStreaming.value) return

        add(UiMessage(nextId++, "user", trimmed))
        // Build the API history from what's shown so far (before the assistant placeholder).
        val history = buildList {
            add(ChatMessage("system", SYSTEM_PROMPT))
            _messages.value.forEach { add(ChatMessage(it.role, it.text)) }
        }
        val assistantId = add(UiMessage(nextId++, "assistant", ""))

        val key = settings.apiKey()
        val url = baseUrl.value
        val modelName = model.value

        _isStreaming.value = true
        viewModelScope.launch {
            try {
                client.stream(url, key ?: "", modelName, history).collect { delta ->
                    _messages.value = _messages.value.map {
                        if (it.id == assistantId) it.copy(text = it.text + delta) else it
                    }
                }
                // If nothing streamed back, show a gentle placeholder rather than an empty bubble.
                _messages.value = _messages.value.map {
                    if (it.id == assistantId && it.text.isBlank()) it.copy(text = "(no response)") else it
                }
            } catch (e: Exception) {
                val msg = e.message ?: "Something went wrong."
                _messages.value = _messages.value.map {
                    if (it.id == assistantId) it.copy(text = msg, error = true) else it
                }
            } finally {
                _isStreaming.value = false
            }
        }
    }

    fun clearChat() {
        if (_isStreaming.value) return
        _messages.value = emptyList()
    }

    // --- Settings ---

    fun setApiKey(key: String) = settings.setApiKey(key)
    fun clearApiKey() = settings.clearApiKey()
    fun setBaseUrl(url: String) = settings.setBaseUrl(url)
    fun setModel(m: String) = settings.setModel(m)

    fun testConnection() {
        viewModelScope.launch {
            _testResult.value = "Testing…"
            val result = client.testConnection(baseUrl.value, settings.apiKey() ?: "", model.value)
            _testResult.value = result.fold(
                onSuccess = { "Connected successfully." },
                onFailure = { it.message ?: "Connection failed." }
            )
        }
    }

    fun clearTestResult() {
        _testResult.value = null
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MacTrackApplication
                AiViewModel(app.aiClient, app.aiSettingsStore)
            }
        }
    }
}
