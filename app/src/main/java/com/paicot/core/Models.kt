package com.paicot.core

/** Execution lifecycle of a Python script. */
enum class ExecutionState { IDLE, RUNNING, COMPLETED, FAILED, CANCELLED, TIMED_OUT }

/** Standardised result envelope for bridge + server responses. */
data class OperationResult(
    val ok: Boolean,
    val data: org.json.JSONObject = org.json.JSONObject(),
    val error: String? = null,
    val ts: Long = System.currentTimeMillis()
) {
    fun toJson(): org.json.JSONObject {
        val o = org.json.JSONObject()
        o.put("ok", ok)
        o.put("data", data)
        o.put("timestamp", ts)
        if (error != null) o.put("error", error)
        return o
    }
    companion object {
        fun success(data: org.json.JSONObject = org.json.JSONObject()) = OperationResult(true, data)
        fun failure(message: String) = OperationResult(false, org.json.JSONObject(), message)
    }
}

/** Structured result of one script execution. */
data class ExecutionResult(
    val id: String,
    val state: ExecutionState,
    val stdout: String = "",
    val stderr: String = "",
    val result: String? = null,
    val error: String? = null,
    val startedAt: Long = 0,
    val finishedAt: Long = 0,
    val durationMs: Long = 0,
    val source: String = "inline"
) {
    fun toJson(): org.json.JSONObject {
        val o = org.json.JSONObject()
        o.put("id", id)
        o.put("state", state.name.lowercase())
        o.put("stdout", stdout)
        o.put("stderr", stderr)
        o.put("result", if (result == null) org.json.JSONObject.NULL else result)
        o.put("error", if (error == null) org.json.JSONObject.NULL else error)
        o.put("started_at", startedAt)
        o.put("finished_at", finishedAt)
        o.put("duration_ms", durationMs)
        o.put("source", source)
        return o
    }
}

data class ServerStatus(
    val running: Boolean = false,
    val host: String = "127.0.0.1",
    val port: Int = 7777,
    val actualPort: Int = 0,
    val version: String = "0.1.0",
    val pythonReady: Boolean = false,
    val activeExecutions: Int = 0,
    val lastError: String? = null
) {
    fun toJson(): org.json.JSONObject {
        val o = org.json.JSONObject()
        o.put("running", running)
        o.put("host", host)
        o.put("port", port)
        o.put("actual_port", actualPort)
        o.put("version", version)
        o.put("python_ready", pythonReady)
        o.put("active_executions", activeExecutions)
        o.put("last_error", if (lastError == null) org.json.JSONObject.NULL else lastError)
        return o
    }
}

data class DeviceInfo(
    val manufacturer: String,
    val model: String,
    val brand: String,
    val device: String,
    val product: String,
    val androidVersion: String,
    val sdkInt: Int,
    val board: String,
    val hardware: String,
    val screenWidthPx: Int,
    val screenHeightPx: Int,
    val density: Float,
    val totalMemoryBytes: Long,
    val batteryPercent: Int,
    val batteryStatus: String
) {
    fun toJson(): org.json.JSONObject {
        val o = org.json.JSONObject()
        o.put("manufacturer", manufacturer)
        o.put("model", model)
        o.put("brand", brand)
        o.put("device", device)
        o.put("product", product)
        o.put("android_version", androidVersion)
        o.put("sdk_int", sdkInt)
        o.put("board", board)
        o.put("hardware", hardware)
        o.put("screen_width_px", screenWidthPx)
        o.put("screen_height_px", screenHeightPx)
        o.put("density", density.toDouble())
        o.put("total_memory_bytes", totalMemoryBytes)
        o.put("battery_percent", batteryPercent)
        o.put("battery_status", batteryStatus)
        return o
    }
}

enum class PermissionKind(val displayName: String) {
    ACCESSIBILITY("Accessibility"),
    OVERLAY("Overlay"),
    NOTIFICATION_LISTENER("Notification listener"),
    CAPTURE("Screen capture"),
    FILES("Files (SAF)"),
    POST_NOTIFICATIONS("Notifications")
}

data class PermissionStatus(
    val kind: PermissionKind,
    val granted: Boolean,
    val details: String
)

data class AppInfo(
    val packageName: String,
    val label: String,
    val version: String,
    val isSystem: Boolean,
    val launcherActivity: String?
)

data class LogEntry(
    val ts: Long,
    val level: String,
    val tag: String,
    val message: String
)

data class ActiveNotification(
    val packageName: String,
    val title: String,
    val text: String,
    val postedAt: Long
)

/** Privilege mode. */
enum class PrivilegeMode { STANDARD, ADVANCED, ROOT }
