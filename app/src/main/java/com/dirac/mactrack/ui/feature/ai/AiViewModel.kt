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
import com.dirac.mactrack.data.ai.recipe.BuildPreview
import com.dirac.mactrack.data.ai.recipe.BuildTarget
import com.dirac.mactrack.data.ai.recipe.IngredientResolver
import com.dirac.mactrack.data.ai.recipe.RecipeMealBuilder
import com.dirac.mactrack.data.ai.recipe.RecipeRequestParser
import com.dirac.mactrack.data.entity.MealEntry
import com.dirac.mactrack.data.repository.MealEntryRepository
import com.dirac.mactrack.data.session.LogDateStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalTime
import kotlin.math.roundToInt

// One message in the chat, as shown. `error = true` renders it as an error notice rather than a reply;
// `imageDataUrl` is an attached photo on a user turn.
data class UiMessage(
    val id: Long,
    val role: String,   // "user" | "assistant"
    val text: String,
    val error: Boolean = false,
    val imageDataUrl: String? = null,
    // When set, this assistant message is a resolved recipe/meal awaiting the user's Save tap (AI-4).
    val build: BuildPreview? = null
)

// Injected as the "system" turn on every request (the background brief the model is given the moment
// the chat is used). Kept honest to what the app actually feeds it: text and optional photos -- this
// chat has no live access to the CNF database or Open Food Facts, so it is told to stay *consistent*
// with them and to defer to the app's own barcode lookup for exact label values, not to pretend it can
// query them. Real tool/function-calling access to those sources is a later slice.
private val SYSTEM_PROMPT = """
    You are MacTrack's built-in nutrition assistant. MacTrack is a private, offline-first Android app
    for tracking calories and macros. It has a single user, stores everything on-device, and needs no
    account. The user logs foods and tracks calories plus protein, carbs, and fat, and a few
    micronutrients (sodium, potassium, fiber, caffeine).

    How you are used: the user may send a plain question, a food photo, a photo plus a weight, or a
    pasted list of items (brand names optional). Your main job is to estimate a food's nutrition so it
    can be logged. Give a single best estimate as: a short food name, then calories, then protein,
    carbs, and fat in grams, for the amount described. Always state the serving or amount you assumed.
    Call it an estimate, and when you are unsure give a sensible range instead of inventing precise
    numbers. The app shows your estimate in a review dialog where the user confirms or edits it before
    it is logged, so make the numbers easy to read.

    Data sources: MacTrack itself resolves barcoded and branded products through Open Food Facts, and
    common whole foods through the Canadian Nutrient File (CNF). Keep your estimates consistent with
    those standard databases and typical Canadian serving sizes. If an item has a barcode, remind the
    user they can scan it in the app for exact label values instead of estimating.

    Style: concise and practical, metric by default (grams, millilitres) but accept ounces and cups.
    Stay focused on food and nutrition. You cannot see the user's logged foods, goals, or daily totals
    unless they tell you in the chat.

    Building a recipe or meal: if the user asks you to turn a list of ingredients into a recipe or a
    meal (for example "make a recipe from ..." or "save these as a meal"), reply with ONLY a JSON object
    and nothing else -- no prose, no code fence -- in exactly this shape:
    {"target":"recipe"|"meal","name":"<short name>","ingredients":[{"name":"<food>","quantity":<number>,"unit":"g"|"ml"|"serving"|"cup"|"tbsp"|"tsp"|"oz"}]}
    Use "recipe" if they said recipe and "meal" if they said meal; if it is unclear, use "meal".
    Prefer grams for every quantity -- including countable foods, by multiplying by a typical unit
    weight (e.g. "2 eggs" -> quantity 100, unit "g", since a large egg is about 50 g; "1 slice bread"
    -> about 30 g; "1 cup cooked rice" -> about 200 g). Only fall back to unit "serving" when you truly
    cannot estimate a gram weight. Also give each ingredient a specific, common name ("egg", "chicken
    breast", "white rice"), not a vague one. The app looks up each ingredient's real macros from its
    databases -- you only extract the names and amounts. For anything that is not such a request, answer
    normally.
""".trimIndent()

// Backs the AI chat tab. Conversation is in-memory (Slice 1) -- it survives tab switches/rotation but
// resets when the app is killed. Settings (base URL, model, key presence) come from AiSettingsStore.
class AiViewModel(
    private val client: AiClient,
    private val settings: AiSettingsStore,
    private val mealEntryRepository: MealEntryRepository,
    private val logDateStore: LogDateStore,
    private val builder: RecipeMealBuilder
) : ViewModel() {

    private val _messages = MutableStateFlow<List<UiMessage>>(emptyList())
    val messages: StateFlow<List<UiMessage>> = _messages.asStateFlow()

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    // A photo attached but not yet sent (a data URL), shown as a thumbnail above the input.
    private val _pendingImage = MutableStateFlow<String?>(null)
    val pendingImage: StateFlow<String?> = _pendingImage.asStateFlow()

    fun attachImage(dataUrl: String) { _pendingImage.value = dataUrl }
    fun clearPendingImage() { _pendingImage.value = null }

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
        val image = _pendingImage.value
        if ((trimmed.isEmpty() && image == null) || _isStreaming.value) return

        // If only a photo is attached, ask the obvious question for it.
        val userText = trimmed.ifEmpty { "Estimate the calories and macros of this food." }

        add(UiMessage(nextId++, "user", userText, imageDataUrl = image))
        _pendingImage.value = null
        // Build the API history from what's shown so far (before the assistant placeholder).
        val history = buildList {
            add(ChatMessage("system", SYSTEM_PROMPT))
            _messages.value.forEach { add(ChatMessage(it.role, it.text, it.imageDataUrl)) }
        }
        val assistantId = add(UiMessage(nextId++, "assistant", ""))

        val key = settings.apiKey()
        val url = baseUrl.value
        val modelName = model.value

        _isStreaming.value = true
        viewModelScope.launch {
            val raw = StringBuilder()
            try {
                client.stream(url, key ?: "", modelName, history).collect { delta ->
                    raw.append(delta)
                    // A recipe/meal build reply is a bare JSON object; show a placeholder while it
                    // streams instead of the raw JSON.
                    val display = if (raw.toString().trimStart().startsWith("{")) "Putting that together…"
                    else raw.toString()
                    _messages.value = _messages.value.map {
                        if (it.id == assistantId) it.copy(text = display) else it
                    }
                }
                // Read the reply as a recipe/meal build request if it is one; else it's a normal message.
                val request = RecipeRequestParser.parse(raw.toString())
                if (request != null) {
                    val preview = builder.preview(request)
                    _messages.value = _messages.value.map {
                        if (it.id == assistantId) it.copy(text = summarize(preview), build = preview) else it
                    }
                } else {
                    val finalText = raw.toString().ifBlank { "(no response)" }
                    _messages.value = _messages.value.map {
                        if (it.id == assistantId) it.copy(text = finalText) else it
                    }
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

    // A short, human summary of a resolved recipe/meal, shown in the bubble above the Save button.
    private fun summarize(p: BuildPreview): String {
        if (!p.canSave) {
            val unmatched = if (p.skipped.isNotEmpty())
                " (couldn't match: " + p.skipped.joinToString(", ") { it.parsed.name } + ")" else ""
            return "I couldn't match any of those ingredients to a food, so there's nothing to save$unmatched."
        }
        val kind = if (p.target == BuildTarget.RECIPE) "Recipe" else "Meal"
        val t = p.total
        val n = p.resolved.size
        val head = "$kind \"${p.name}\": ${t.kcal.roundToInt()} cal, " +
            "${t.protein.roundToInt()}P ${t.carb.roundToInt()}C ${t.fat.roundToInt()}F, " +
            "from $n ingredient${if (n == 1) "" else "s"}."
        val unmatched = if (p.skipped.isNotEmpty())
            " Couldn't match: " + p.skipped.joinToString(", ") { it.parsed.name } + "." else ""
        return "$head$unmatched\n\nReview and tap Save below."
    }

    // Persist a previewed recipe/meal (the user tapped Save), then turn the bubble into a confirmation.
    fun commitBuild(messageId: Long, onSaved: () -> Unit = {}) {
        val message = _messages.value.firstOrNull { it.id == messageId } ?: return
        val preview = message.build ?: return
        viewModelScope.launch {
            builder.commit(preview)
            val kind = if (preview.target == BuildTarget.RECIPE) "recipe" else "meal"
            _messages.value = _messages.value.map {
                if (it.id == messageId)
                    it.copy(text = "Saved $kind \"${preview.name}\". Find it in your Kitchen.", build = null)
                else it
            }
            onSaved()
        }
    }

    fun clearChat() {
        if (_isStreaming.value) return
        _messages.value = emptyList()
    }

    // Log a reviewed AI estimate to the current day as a one-off entry (provenance "ai"). Values come
    // from the review dialog, so the user has already confirmed/edited them.
    fun logEstimate(name: String, calories: Double, protein: Double, carb: Double, fat: Double, onLogged: () -> Unit) {
        viewModelScope.launch {
            val now = LocalTime.now()
            mealEntryRepository.logEntry(
                MealEntry(
                    date = logDateStore.current().toString(),
                    timeMinutes = now.hour * 60 + now.minute,
                    foodName = name.ifBlank { "AI estimate" },
                    amount = 1.0,
                    quantity = 1.0,
                    unit = "serving",
                    calories = calories,
                    proteinG = protein,
                    carbG = carb,
                    fatG = fat,
                    sourceType = "ai",
                    unitLabel = "serving",
                    updatedAt = System.currentTimeMillis()
                )
            )
            onLogged()
        }
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
                val resolver = IngredientResolver(app.foodRepository, app.cnfRepository, app.openFoodFactsRepository)
                val builder = RecipeMealBuilder(resolver, app.recipeRepository, app.mealTemplateRepository)
                AiViewModel(app.aiClient, app.aiSettingsStore, app.mealEntryRepository, app.logDateStore, builder)
            }
        }
    }
}
