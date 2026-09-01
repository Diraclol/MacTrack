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
}