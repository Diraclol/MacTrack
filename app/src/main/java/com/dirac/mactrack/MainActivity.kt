package com.dirac.mactrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dirac.mactrack.ui.feature.goals.GoalViewModel
import com.dirac.mactrack.ui.feature.onboarding.OnboardingScreen
import com.dirac.mactrack.ui.navigation.MacTrackApp
import com.dirac.mactrack.ui.theme.MacTrackTheme
import com.dirac.mactrack.ui.theme.ThemeMode
import com.dirac.mactrack.ui.theme.ThemeViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeViewModel: ThemeViewModel = viewModel(factory = ThemeViewModel.Factory)
            val themeMode by themeViewModel.mode.collectAsState()
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            MacTrackTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val goalViewModel: GoalViewModel = viewModel(factory = GoalViewModel.Factory)
                    val hasGoal by goalViewModel.hasGoal.collectAsState()
                    when (hasGoal) {
                        null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                        false -> OnboardingScreen()
                        true -> MacTrackApp()
                    }
                }
            }
        }
    }
}