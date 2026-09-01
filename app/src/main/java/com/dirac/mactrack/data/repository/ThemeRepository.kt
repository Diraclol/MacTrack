package com.dirac.mactrack.data.repository

import android.content.Context
import com.dirac.mactrack.ui.theme.StartScreen
import com.dirac.mactrack.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThemeRepository(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _mode = MutableStateFlow(load())
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    private val _startScreen = MutableStateFlow(loadStart())
    val startScreen: StateFlow<StartScreen> = _startScreen.asStateFlow()

    private val _avatar = MutableStateFlow(prefs.getString("avatar_emoji", "🧑") ?: "🧑")
    val avatar: StateFlow<String> = _avatar.asStateFlow()

    // The order of the food-log micronutrient cards (user-draggable). Stored as CSV keys.
    private val _nutrientOrder = MutableStateFlow(loadNutrientOrder())
    val nutrientOrder: StateFlow<List<String>> = _nutrientOrder.asStateFlow()

    private fun load(): ThemeMode =
        runCatching {
            ThemeMode.valueOf(prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
        }.getOrDefault(ThemeMode.SYSTEM)

    private fun loadStart(): StartScreen =
        runCatching {
            StartScreen.valueOf(prefs.getString("start_screen", StartScreen.DASHBOARD.name) ?: StartScreen.DASHBOARD.name)
        }.getOrDefault(StartScreen.DASHBOARD)

    fun setMode(mode: ThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
        _mode.value = mode
    }

    fun setStartScreen(startScreen: StartScreen) {
        prefs.edit().putString("start_screen", startScreen.name).apply()
        _startScreen.value = startScreen
    }

    fun setAvatar(emoji: String) {
        prefs.edit().putString("avatar_emoji", emoji).apply()
        _avatar.value = emoji
    }

    fun setNutrientOrder(order: List<String>) {
        prefs.edit().putString("nutrient_order", order.joinToString(",")).apply()
        _nutrientOrder.value = order
    }

    private fun loadNutrientOrder(): List<String> {
        val known = DEFAULT_NUTRIENT_ORDER
        val saved = prefs.getString("nutrient_order", null)
            ?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }
        // Keep saved order, drop unknown keys, and append any known key that's missing
        // (so a nutrient added to the app later still appears).
        return if (saved.isNullOrEmpty()) known
        else (saved.filter { it in known } + known.filter { it !in saved }).ifEmpty { known }
    }

    companion object {
        val DEFAULT_NUTRIENT_ORDER = listOf("sodium", "potassium", "fiber", "caffeine")
    }
}