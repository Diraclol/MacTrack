package com.dirac.mactrack.ui.feature.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dirac.mactrack.R
import com.dirac.mactrack.ui.common.BackBar

// About screen (reached from More). Home for the monogram, the name/version, and a short, honest note
// about what the app is. Kept text-light so the mark carries it.
@Composable
fun AboutScreen(onBack: () -> Unit = {}, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val versionName = remember {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        BackBar("About", onBack)

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.ic_mactrack_logo),
                contentDescription = "MacTrack logo",
                modifier = Modifier.size(128.dp)
            )
            Text("MacTrack", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Version $versionName",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "A private, offline-first calorie and macro tracker. Your food log, goals, and weight " +
                    "history stay on this device — no account required.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }
    }
}
