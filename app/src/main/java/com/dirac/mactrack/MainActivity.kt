package com.dirac.mactrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dirac.mactrack.ui.feature.goals.GoalViewModel
import com.dirac.mactrack.ui.feature.onboarding.OnboardingScreen
import com.dirac.mactrack.ui.navigation.MacTrackApp
import com.dirac.mactrack.ui.theme.MacTrackTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MacTrackTheme {
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