package com.paicot.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.paicot.ui.state.ApiHubState

@Composable
fun ApiHubScreen(state: ApiHubState) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("API Hub", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        Text("Version ${state.version}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Modules", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        if (state.modules.isEmpty()) Text("No modules.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        state.modules.forEach { module ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.size(12.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                Text(module, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        Text("Python API", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(
            "import paicot\n" +
                "paicot.apps.open\n" +
                "paicot.screen.tap / swipe\n" +
                "paicot.keyboard.type\n" +
                "paicot.device.info\n" +
                "paicot.files\n" +
                "paicot.notifications\n" +
                "paicot.display",
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "Scripts use import paicot to access apps, screen, keyboard, device, files, notifications and display. " +
                "Every call is permission-gated and runs in the embedded Python runtime.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
