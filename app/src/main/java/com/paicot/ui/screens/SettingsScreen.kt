package com.paicot.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
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
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        if (state.message.isNotEmpty()) Text(state.message, color = MaterialTheme.colorScheme.primary)
        Text("Server", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Text("Status: ${if (state.server.running) "Running" else "Stopped"}", color = MaterialTheme.colorScheme.onSurface)
        Text("Port: ${state.server.actualPort}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (!state.server.lastError.isNullOrEmpty()) Text("Error: ${state.server.lastError}", color = MaterialTheme.colorScheme.primary)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onStartServer, modifier = Modifier.heightIn(min = 48.dp)) { Text("Start server") }
            TextButton(onClick = onStopServer, modifier = Modifier.heightIn(min = 48.dp)) { Text("Stop server") }
        }
        Text("Token", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        SelectionContainer {
            Text(state.token, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurface)
        }
        Button(onClick = onRegenerateToken, modifier = Modifier.heightIn(min = 48.dp)) { Text("Regenerate") }

        OutlinedTextField(
            value = port,
            onValueChange = { port = it; onPortChange(it) },
            label = { Text("Port") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = host,
            onValueChange = { host = it; onHostChange(it) },
            label = { Text("Host") },
            modifier = Modifier.fillMaxWidth()
        )
        Text("Mode", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("STANDARD", "ADVANCED", "ROOT").forEach { mode ->
                FilterChip(
                    selected = state.mode == mode,
                    onClick = { onModeChange(mode) },
                    label = { Text(mode) }
                )
            }
        }
        Button(onClick = onRefresh, modifier = Modifier.heightIn(min = 48.dp)) { Text("Refresh") }
        Text("Logs", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        SelectionContainer {
            Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF1A1A1C), MaterialTheme.shapes.medium).padding(12.dp)) {
                if (state.lines.isEmpty()) Text("No logs.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                state.lines.forEach { line ->
                    Text(line, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
