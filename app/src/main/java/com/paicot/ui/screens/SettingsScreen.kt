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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.paicot.ui.state.SettingsState

@Composable
fun SettingsScreen(
    state: SettingsState,
    onRefresh: () -> Unit,
    onStartServer: () -> Unit,
    onStopServer: () -> Unit,
    onRegenerateToken: () -> Unit,
    onPortChange: (String) -> Unit,
    onHostChange: (String) -> Unit,
    onModeChange: (String) -> Unit
) {
    var port by rememberSaveable { androidx.compose.runtime.mutableStateOf(state.port) }
    var host by rememberSaveable { androidx.compose.runtime.mutableStateOf(state.host) }
    val clipboard = LocalClipboardManager.current
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
        if (state.message.isNotEmpty()) Text(state.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)

        SectionTitle("Server")
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatusDot(granted = state.server.running)
                    Text(
                        "Status: ${if (state.server.running) "Running" else "Stopped"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text("Port: ${state.server.actualPort}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!state.server.lastError.isNullOrEmpty()) Text("Error: ${state.server.lastError}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onStartServer, modifier = Modifier.heightIn(min = 48.dp)) { Text("Start server") }
                    Button(onClick = onStopServer, modifier = Modifier.heightIn(min = 48.dp)) { Text("Stop server") }
                }
            }
        }

        SectionTitle("Token")
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MonoText(state.token, modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = { clipboard.setText(AnnotatedString(state.token)) },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copy token", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
                Button(onClick = onRegenerateToken, modifier = Modifier.heightIn(min = 48.dp)) { Text("Regenerate") }
            }
        }

        SectionTitle("Connection")
        OutlinedTextField(
            value = port,
            onValueChange = { port = it; onPortChange(it) },
            label = { Text("Port") },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small
        )
        OutlinedTextField(
            value = host,
            onValueChange = { host = it; onHostChange(it) },
            label = { Text("Host") },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small
        )

        SectionTitle("Mode")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("STANDARD", "ADVANCED", "ROOT").forEach { mode ->
                FilterChip(
                    selected = state.mode == mode,
                    onClick = { onModeChange(mode) },
                    label = { Text(mode) },
                    modifier = Modifier.heightIn(min = 48.dp)
                )
            }
        }
        Button(onClick = onRefresh, modifier = Modifier.heightIn(min = 48.dp)) { Text("Refresh") }

        SectionTitle("Logs")
        MonoText(
            if (state.lines.isEmpty()) "No logs." else state.lines.joinToString("\n")
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}
