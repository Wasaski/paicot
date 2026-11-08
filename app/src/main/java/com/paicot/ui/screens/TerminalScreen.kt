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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paicot.ui.state.TerminalState

private val monoStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp, color = Color.White)
private val monoSurface = Color(0xFF1A1A1C)

@Composable
fun TerminalScreen(state: TerminalState, onCodeChange: (String) -> Unit, onRun: () -> Unit, onCancel: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Terminal", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        BasicTextField(
            value = state.code,
            onValueChange = onCodeChange,
            textStyle = monoStyle,
            modifier = Modifier.fillMaxWidth().background(monoSurface, MaterialTheme.shapes.medium).padding(12.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onRun, enabled = !state.running, modifier = Modifier.heightIn(min = 48.dp)) { Text("Run") }
            TextButton(onClick = onCancel, modifier = Modifier.heightIn(min = 48.dp)) { Text("Stop") }
        }
        if (state.running) Text("Running...", color = MaterialTheme.colorScheme.onSurfaceVariant)
        state.output?.let { out ->
            Text("Output", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            SelectionContainer {
                Column(modifier = Modifier.fillMaxWidth().background(monoSurface, MaterialTheme.shapes.medium).padding(12.dp)) {
                    Text("State: ${out.state.name}", style = monoStyle)
                    if (out.stdout.isNotEmpty()) Text(out.stdout, style = monoStyle)
                    if (out.stderr.isNotEmpty()) Text(out.stderr, style = monoStyle)
                    if (!out.error.isNullOrEmpty()) Text("Error: ${out.error}", style = monoStyle)
                    if (!out.result.isNullOrEmpty()) Text("Result: ${out.result}", style = monoStyle)
                }
            }
        }
        if (state.history.isNotEmpty()) {
            Text("History", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            state.history.forEach { item ->
                Text("${item.id}  ${item.state.name}  ${item.durationMs} ms", style = monoStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
