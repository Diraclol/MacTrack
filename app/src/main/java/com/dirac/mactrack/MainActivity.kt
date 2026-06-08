package com.dirac.mactrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dirac.mactrack.ui.navigation.MacTrackApp
import com.dirac.mactrack.ui.theme.MacTrackTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MacTrackTheme {
                MacTrackApp()
            }
        }
    }
}