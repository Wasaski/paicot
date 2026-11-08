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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.paicot.ui.state.FilesState

@Composable
fun FilesScreen(
    state: FilesState,
    onRefresh: () -> Unit,
    onSave: (String, String) -> Unit,
    onDelete: (Long) -> Unit,
    onEditScript: (String, String) -> Unit,
    onRunFile: (String, String) -> Unit,
    onRequestSafTree: () -> Unit
) {
    var name by rememberSaveable { androidx.compose.runtime.mutableStateOf(state.editingName) }
    var code by rememberSaveable { androidx.compose.runtime.mutableStateOf(state.editingCode) }
    LaunchedEffect(state.editingName, state.editingCode) {
        name = state.editingName
        code = state.editingCode
    }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Files", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
        if (state.message.isNotEmpty()) Text(state.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        Button(onClick = onRefresh, modifier = Modifier.heightIn(min = 48.dp)) { Text("Refresh") }

        SectionTitle("Scripts")
        if (state.scripts.isEmpty()) Text("No scripts.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        state.scripts.forEach { script ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(script.name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onRunFile(script.name, script.code) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Run") }
                        TextButton(onClick = { onEditScript(script.name, script.code) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Edit") }
                        TextButton(onClick = { onDelete(script.id) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Delete") }
                    }
                }
            }
        }

        SectionTitle("Editor")
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Code") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace)
                )
                Button(onClick = { onSave(name, code) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Save") }
            }
        }

        SectionTitle("Private files")
        if (state.privateFiles.isEmpty()) Text("No private files.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        state.privateFiles.forEach { file ->
            val isDir = file.optBoolean("is_dir", false)
            val label = file.optString("name", file.optString("path", ""))
            val size = file.optLong("size", 0L)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = if (isDir) Icons.Filled.Folder else Icons.Filled.Description,
                    contentDescription = if (isDir) "Folder" else "File",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    if (isDir) "$label/  ($size bytes)" else "$label  ($size bytes)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatusDot(granted = state.safGranted)
                    Text(
                        "SAF status: ${if (state.safGranted) "Granted" else "Not granted"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Button(onClick = onRequestSafTree, modifier = Modifier.heightIn(min = 48.dp)) { Text("Grant folder (SAF)") }
            }
        }
    }
}
