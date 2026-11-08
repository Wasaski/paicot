package com.paicot.ui.screens

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.paicot.ui.state.AutomationState

@Composable
fun AutomationScreen(state: AutomationState, onRefresh: () -> Unit, onRunScript: (Long?, String, String) -> Unit, onDeleteScript: (Long) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Automation", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        Button(onClick = onRefresh, modifier = Modifier.heightIn(min = 48.dp)) { Text("Refresh") }
        if (state.running) Text("Running...", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Scripts", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        if (state.scripts.isEmpty()) Text("No scripts saved yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        state.scripts.forEach { script ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(script.name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    Text("Runs: ${script.runCount}  Status: ${script.lastStatus}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onRunScript(script.id, script.name, script.code) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Run") }
                        TextButton(onClick = { onDeleteScript(script.id) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Delete") }
                    }
                }
            }
        }
        Text("Recent runs", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        if (state.recentRuns.isEmpty()) Text("No recent runs.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        state.recentRuns.forEach { run ->
            Text(
                "${run.optString("run_id")}  ${run.optString("script")}  ${run.optString("state")}",
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (state.runOutput.isNotEmpty()) {
            Text("Run output", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            SelectionContainer {
                Text(state.runOutput, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
