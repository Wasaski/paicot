package com.paicot.ui.state

import com.paicot.Paicot
import com.paicot.core.DeviceInfo
import com.paicot.core.ExecutionResult
import com.paicot.core.PermissionStatus
import com.paicot.core.ServerStatus
import com.paicot.database.ScriptEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ----- Overview -------------------------------------------------------------

data class OverviewState(
    val server: ServerStatus = ServerStatus(),
    val device: DeviceInfo? = null,
    val pythonReady: Boolean = false,
    val pythonError: String? = null,
    val pendingPermissions: Int = 0,
    val runningScripts: Int = 0,
    val loading: Boolean = false
) {
    // Derived status lines shown on the Overview screen.
    val serverLine: String get() = if (server.running) "Listening on ${server.publishedHost()}:${server.actualPort}"
        else if (server.lastError != null) "Start failed: ${server.lastError}"
        else "Stopped"
    val runtimeLine: String get() = when {
        pythonReady -> "Python 3.11 embedded (Chaquopy)"
        pythonError != null -> "Runtime failed: $pythonError"
        else -> "Not initialised yet"
    }
}

data class TerminalState(
    val code: String = "print('Hello from PAICOT')\n\ninfo = __import__('paicot').device.info()\nprint(info)",
    val running: Boolean = false,
    val output: ExecutionResult? = null,
    val history: List<ExecutionResult> = emptyList()
)

data class AutomationState(
    val scripts: List<ScriptEntity> = emptyList(),
    val recentRuns: List<org.json.JSONObject> = emptyList(),
    val runOutput: String = "",
    val running: Boolean = false
)

data class FilesState(
    val scripts: List<ScriptEntity> = emptyList(),
    val privateFiles: List<org.json.JSONObject> = emptyList(),
    val editingName: String = "",
    val editingCode: String = "",
    val safGranted: Boolean = false,
    val message: String = ""
)

data class PermissionsState(
    val permissions: List<PermissionStatus> = emptyList(),
    val pending: Int = 0
)

data class SettingsState(
    val server: ServerStatus = ServerStatus(),
    val token: String = "",
    val port: String = "7777",
    val host: String = "127.0.0.1",
    val mode: String = "STANDARD",
    val lines: List<String> = emptyList(),
    val message: String = ""
)

data class ApiHubState(
    val modules: List<String> = emptyList(),
    val version: String = "0.1.0"
)

// ----- Store -----------------------------------------------------------------

object UiStore {
    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    val overview = MutableStateFlow(OverviewState())
    val terminal = MutableStateFlow(TerminalState())
    val automation = MutableStateFlow(AutomationState())
    val files = MutableStateFlow(FilesState())
    val permissions = MutableStateFlow(PermissionsState())
    val settings = MutableStateFlow(SettingsState())
    val apiHub = MutableStateFlow(ApiHubState())

    fun refreshOverview() {
        overview.value = overview.value.copy(loading = true)
        scope.launch(Dispatchers.IO) {
            val device = Paicot.deviceManager.info()
            val server = Paicot.server.status()
            val pending = Paicot.permissionManager.pendingCount()
            val runningScripts = Paicot.automationManager.recent().count { it.optString("state") == "running" }
            val state = OverviewState(
                server = server,
                device = device,
                pythonReady = Paicot.pythonRuntime.ready,
                pythonError = Paicot.pythonRuntime.initError,
                pendingPermissions = pending,
                runningScripts = runningScripts,
                loading = false
            )
            kotlinx.coroutines.withContext(Dispatchers.Main) { overview.value = state }
        }
    }

    fun setCode(code: String) { terminal.value = terminal.value.copy(code = code) }

    fun runTerminal() {
        val code = terminal.value.code
        terminal.value = terminal.value.copy(running = true)
        scope.launch(Dispatchers.IO) {
            val result = Paicot.pythonRuntime.run(code, source = "terminal")
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                val history = (listOf(result) + terminal.value.history).take(20)
                terminal.value = terminal.value.copy(running = false, output = result, history = history)
            }
        }
    }

    fun cancelTerminal() {
        Paicot.pythonRuntime.cancel()
    }

    fun runAutomation(scriptId: Long?, name: String, code: String) {
        automation.value = automation.value.copy(running = true)
        scope.launch(Dispatchers.IO) {
            val started = Paicot.automationManager.runScript(scriptId, name, code)
            val history = Paicot.database.executionDao().recent(10)
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                automation.value = automation.value.copy(
                    running = false,
                    runOutput = started.toString(),
                    recentRuns = Paicot.automationManager.recent()
                )
                files.value = files.value.copy(message = "Execution history: ${history.size} entries")
            }
        }
    }

    fun refreshScripts() {
        scope.launch(Dispatchers.IO) {
            val scripts = Paicot.database.scriptDao().all()
            val privateList = mutableListOf<org.json.JSONObject>()
            val listed = Paicot.fileManager.listPrivate("scripts")
            if (listed.ok) {
                val arr = listed.data.optJSONArray("entries")
                if (arr != null) for (i in 0 until arr.length()) privateList += arr.getJSONObject(i)
            }
            val saf = Paicot.permissionManager.safGranted()
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                automation.value = automation.value.copy(scripts = scripts, recentRuns = Paicot.automationManager.recent())
                files.value = files.value.copy(scripts = scripts, privateFiles = privateList, safGranted = saf)
            }
        }
    }

    fun saveScript(name: String, code: String) {
        if (name.isBlank()) {
            files.value = files.value.copy(message = "Give the script a name first")
            return
        }
        scope.launch(Dispatchers.IO) {
            Paicot.database.scriptDao().upsert(ScriptEntity(name = name, code = code))
            Paicot.fileManager.writePrivate("scripts/$name.py", code)
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                files.value = files.value.copy(message = "Saved $name.py")
                refreshScripts()
            }
        }
    }

    fun deleteScript(id: Long) {
        scope.launch(Dispatchers.IO) {
            Paicot.database.scriptDao().delete(id)
            kotlinx.coroutines.withContext(Dispatchers.Main) { refreshScripts() }
        }
    }

    fun refreshPermissions() {
        scope.launch(Dispatchers.IO) {
            val all = Paicot.permissionManager.all()
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                permissions.value = PermissionsState(permissions = all, pending = all.count { !it.granted })
            }
        }
    }

    fun refreshSettings() {
        scope.launch(Dispatchers.IO) {
            val token = Paicot.prefs.token()
            val port = Paicot.prefs.port()
            val host = Paicot.prefs.host()
            val mode = Paicot.prefs.mode().name
            val logs = com.paicot.core.PaicotLogger.snapshot().takeLast(80).map { "[${it.level}] ${it.tag}: ${it.message}" }
            val server = Paicot.server.status()
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                settings.value = SettingsState(
                    server = server, token = token, port = port.toString(),
                    host = host, mode = mode, lines = logs)
            }
        }
    }

    fun startServer() {
        scope.launch(Dispatchers.IO) {
            Paicot.server.start()
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                settings.value = settings.value.copy(message = "Server start requested")
                refreshSettings(); refreshOverview()
            }
        }
    }

    fun stopServer() {
        Paicot.server.stop()
        settings.value = settings.value.copy(message = "Server stopped")
        refreshSettings(); refreshOverview()
    }

    fun regenerateToken() {
        scope.launch(Dispatchers.IO) {
            Paicot.prefs.regenerateToken()
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                settings.value = settings.value.copy(message = "Token regenerated")
                refreshSettings()
            }
        }
    }

    fun setPort(port: String) {
        val value = port.toIntOrNull() ?: return
        scope.launch(Dispatchers.IO) { Paicot.prefs.setPort(value) }
        settings.value = settings.value.copy(port = port)
    }

    fun setHost(host: String) {
        scope.launch(Dispatchers.IO) { Paicot.prefs.setHost(host) }
        settings.value = settings.value.copy(host = host)
    }

    fun setMode(mode: String) {
        scope.launch(Dispatchers.IO) {
            Paicot.prefs.setMode(com.paicot.core.PrivilegeMode.valueOf(mode))
        }
        settings.value = settings.value.copy(mode = mode)
    }

    fun refreshApiHub() {
        apiHub.value = ApiHubState(
            modules = listOf("apps", "screen", "keyboard", "accessibility", "display",
                "files", "notifications", "device", "automation", "server"),
            version = "0.1.0"
        )
    }

    fun refreshAll() {
        refreshOverview(); refreshScripts(); refreshPermissions(); refreshSettings(); refreshApiHub()
    }
}

// ----- helpers ----------------------------------------------------------------

private fun ServerStatus.publishedHost(): String = if (host == "0.0.0.0") "0.0.0.0 (LAN)" else host
