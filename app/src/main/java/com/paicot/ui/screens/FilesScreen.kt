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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
        Text("Files", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        if (state.message.isNotEmpty()) Text(state.message, color = MaterialTheme.colorScheme.primary)
        Button(onClick = onRefresh, modifier = Modifier.heightIn(min = 48.dp)) { Text("Refresh") }

        Text("Scripts", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        if (state.scripts.isEmpty()) Text("No scripts.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        state.scripts.forEach { script ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(script.name, color = MaterialTheme.colorScheme.onSurface)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onRunFile(script.name, script.code) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Run") }
                        TextButton(onClick = { onEditScript(script.name, script.code) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Edit") }
                        TextButton(onClick = { onDelete(script.id) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Delete") }
                    }
                }
            }
        }

        Text("Editor", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            value = code,
            onValueChange = { code = it },
            label = { Text("Code") },
            modifier = Modifier.fillMaxWidth(),
            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace)
        )
        Button(onClick = { onSave(name, code) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Save") }

        Text("Private files", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        if (state.privateFiles.isEmpty()) Text("No private files.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        state.privateFiles.forEach { file ->
            val isDir = file.optBoolean("is_dir", false)
            val label = file.optString("name", file.optString("path", ""))
            val size = file.optLong("size", 0L)
            Text(if (isDir) "$label/  ($size bytes)" else "$label  ($size bytes)", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Text("SAF status: ${if (state.safGranted) "Granted" else "Not granted"}", color = MaterialTheme.colorScheme.onSurface)
        Button(onClick = onRequestSafTree, modifier = Modifier.heightIn(min = 48.dp)) { Text("Grant folder (SAF)") }
    }
}
