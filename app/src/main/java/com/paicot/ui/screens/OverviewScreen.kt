package com.paicot.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.paicot.ui.state.OverviewState

@Composable
fun OverviewScreen(state: OverviewState, onRefresh: () -> Unit, onGoPermissions: () -> Unit, onGoSettings: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Overview", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.fillMaxWidth().paddingSafe(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(state.serverLine, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(state.runtimeLine, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.fillMaxWidth().paddingSafe(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Device", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                val d = state.device
                if (d == null) {
                    Text("Loading device info...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text("${d.manufacturer} ${d.model}", color = MaterialTheme.colorScheme.onSurface)
                    Text("Android ${d.androidVersion} (SDK ${d.sdkInt})", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Screen ${d.screenWidthPx} x ${d.screenHeightPx}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Battery ${d.batteryPercent}% (${d.batteryStatus})", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.fillMaxWidth().paddingSafe(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Permissions pending: ${state.pendingPermissions}", color = MaterialTheme.colorScheme.onSurface)
                Button(onClick = onGoPermissions, modifier = Modifier.heightIn(min = 48.dp)) { Text("Open Permissions") }
                Text("Scripts running: ${state.runningScripts}", color = MaterialTheme.colorScheme.onSurface)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onRefresh, modifier = Modifier.heightIn(min = 48.dp)) { Text("Refresh") }
            if (!state.server.running) {
                TextButton(onClick = onGoSettings, modifier = Modifier.heightIn(min = 48.dp)) { Text("Server") }
            }
        }
        if (state.loading) Text("Loading...", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun Modifier.paddingSafe(): Modifier = this.then(Modifier.padding(16.dp))
