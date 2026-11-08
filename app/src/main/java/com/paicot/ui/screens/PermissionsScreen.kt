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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.paicot.ui.state.PermissionsState

@Composable
fun PermissionsScreen(
    state: PermissionsState,
    onRefresh: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenOverlay: () -> Unit,
    onOpenNotifications: () -> Unit,
    onRequestCapture: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Permissions", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        Text("Pending: ${state.pending}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = onRefresh, modifier = Modifier.heightIn(min = 48.dp)) { Text("Refresh") }
        if (state.permissions.isEmpty()) Text("No permissions listed.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        state.permissions.forEach { permission ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(permission.kind.displayName, color = MaterialTheme.colorScheme.onSurface)
                        Text(if (permission.granted) "Granted" else "Not granted", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(permission.details, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(
                        onClick = {
                            when (permission.kind.name) {
                                "ACCESSIBILITY" -> onOpenAccessibility()
                                "OVERLAY" -> onOpenOverlay()
                                "NOTIFICATION_LISTENER" -> onOpenNotifications()
                                "CAPTURE" -> onRequestCapture()
                                "FILES" -> onOpenAppSettings()
                                "POST_NOTIFICATIONS" -> onOpenAppSettings()
                            }
                        },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) { Text("Open settings") }
                }
            }
        }
    }
}
