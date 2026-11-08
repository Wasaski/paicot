package com.paicot.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.paicot.ui.state.ApiHubState

private val moduleDescriptions = mapOf(
    "apps" to "Launch and query installed apps",
    "screen" to "Tap, swipe and read the screen",
    "keyboard" to "Type text and send key events",
    "accessibility" to "Accessibility service controls",
    "display" to "Display size, brightness and orientation",
    "files" to "Private and SAF file access",
    "notifications" to "Read posted notifications",
    "device" to "Device info, battery and sensors",
    "automation" to "Run and schedule scripts",
    "server" to "Local HTTP server and token"
)

@Composable
fun ApiHubScreen(state: ApiHubState) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("API Hub", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
        Text("Version ${state.version}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SectionTitle("Modules")
        if (state.modules.isEmpty()) Text("No modules.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        state.modules.forEach { module ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatusDot(granted = true)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(module, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            moduleDescriptions[module] ?: "PAICOT module",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        SectionTitle("Python API")
        MonoText(
            "import paicot\n" +
                "paicot.apps.open\n" +
                "paicot.screen.tap / swipe\n" +
                "paicot.keyboard.type\n" +
                "paicot.device.info\n" +
                "paicot.files\n" +
                "paicot.notifications\n" +
                "paicot.display"
        )
        Text(
            "Scripts use import paicot to access apps, screen, keyboard, device, files, notifications and display. " +
                "Every call is permission-gated and runs in the embedded Python runtime.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}
