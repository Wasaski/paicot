package com.paicot.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.paicot.Paicot
import com.paicot.ui.screens.ApiHubScreen
import com.paicot.ui.screens.AutomationScreen
import com.paicot.ui.screens.FilesScreen
import com.paicot.ui.screens.OverviewScreen
import com.paicot.ui.screens.PermissionsScreen
import com.paicot.ui.screens.SettingsScreen
import com.paicot.ui.screens.TerminalScreen
import com.paicot.ui.state.UiStore

private data class Tab(val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("Overview", Icons.Filled.Home),
    Tab("Terminal", Icons.Filled.Edit),
    Tab("Automation", Icons.Filled.PlayArrow),
    Tab("API Hub", Icons.Filled.Build),
    Tab("Files", Icons.Filled.Folder),
    Tab("Permissions", Icons.Filled.Lock),
    Tab("Settings", Icons.Filled.Settings)
)

@Composable
fun PaicotRoot(onRequestSafTree: () -> Unit, onRequestCapture: () -> Unit) {
    val overview by UiStore.overview.collectAsState()
    val terminal by UiStore.terminal.collectAsState()
    val automation by UiStore.automation.collectAsState()
    val files by UiStore.files.collectAsState()
    val permissions by UiStore.permissions.collectAsState()
    val settings by UiStore.settings.collectAsState()
    val apiHub by UiStore.apiHub.collectAsState()

    var selectedTab by rememberSaveable { androidx.compose.runtime.mutableStateOf(0) }
    val context = LocalContext.current

    LaunchedEffect(Unit) { UiStore.refreshAll() }

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).padding(16.dp)) {
        when (selectedTab) {
            0 -> OverviewScreen(
                state = overview,
                onRefresh = { UiStore.refreshOverview() },
                onGoPermissions = { selectedTab = 5 },
                onGoSettings = { selectedTab = 6 }
            )
            1 -> TerminalScreen(
                state = terminal,
                onCodeChange = { UiStore.setCode(it) },
                onRun = { UiStore.runTerminal() },
                onCancel = { UiStore.cancelTerminal() }
            )
            2 -> AutomationScreen(
                state = automation,
                onRefresh = { UiStore.refreshScripts() },
                onRunScript = { id, name, code -> UiStore.runAutomation(id, name, code) },
                onDeleteScript = { UiStore.deleteScript(it) }
            )
            3 -> ApiHubScreen(state = apiHub)
            4 -> FilesScreen(
                state = files,
                onRefresh = { UiStore.refreshScripts() },
                onSave = { name, code -> UiStore.saveScript(name, code) },
                onDelete = { UiStore.deleteScript(it) },
                onEditScript = { name, code ->
                    UiStore.files.value = UiStore.files.value.copy(editingName = name, editingCode = code)
                },
                onRunFile = { name, code -> UiStore.runAutomation(null, name, code) },
                onRequestSafTree = onRequestSafTree
            )
            5 -> PermissionsScreen(
                state = permissions,
                onRefresh = { UiStore.refreshPermissions() },
                onOpenAccessibility = { context.startActivity(Paicot.permissionManager.openAccessibilitySettings()) },
                onOpenOverlay = { context.startActivity(Paicot.permissionManager.openOverlaySettings()) },
                onOpenNotifications = { context.startActivity(Paicot.permissionManager.openNotificationAccessSettings()) },
                onRequestCapture = onRequestCapture,
                onOpenAppSettings = { context.startActivity(Paicot.permissionManager.openAppSettings()) }
            )
            6 -> SettingsScreen(
                state = settings,
                onRefresh = { UiStore.refreshSettings() },
                onStartServer = { UiStore.startServer() },
                onStopServer = { UiStore.stopServer() },
                onRegenerateToken = { UiStore.regenerateToken() },
                onPortChange = { UiStore.setPort(it) },
                onHostChange = { UiStore.setHost(it) },
                onModeChange = { UiStore.setMode(it) }
            )
        }
        }
    }
}
