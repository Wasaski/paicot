package com.paicot.bridge

import android.content.Context
import com.paicot.Paicot
import com.paicot.core.ExecutionState
import com.paicot.core.OperationResult
import com.paicot.core.PaicotLogger
import org.json.JSONArray
import org.json.JSONObject

/**
 * Bidirectional bridge: Python `import paicot` resolves attribute access to
 * methods on this class. Every call returns a JSON string:
 *   {"ok": true, "data": {...}} or {"ok": false, "error": "..."}
 * Python-side wrappers (_bridge.py) parse and raise PaicotError on failure.
 * Long-running work runs off the UI thread (see PythonRuntime executor).
 */
class PaicotBridge(private val context: Context) {

    // --- cancellation ------------------------------------------------------

    @Volatile var cancelRequested = false

    fun ping(): String {
        if (cancelRequested) throw RuntimeException("Cancelled")
        return "alive"
    }

    // --- module inventory (for API Hub) -------------------------------------

    fun modules(): String {
        val mods = JSONArray()
        listOf(
            "apps", "screen", "keyboard", "accessibility", "display",
            "files", "notifications", "device", "automation", "server"
        ).forEach { mods.put(it) }
        return JSONObject().put("modules", mods).toString()
    }

    // --- apps ---------------------------------------------------------------

    fun apps_list(include_system: Boolean = false): String {
        val apps = Paicot.appManager.listInstalled(include_system)
        val arr = JSONArray()
        apps.forEach {
            arr.put(JSONObject()
                .put("package", it.packageName)
                .put("label", it.label)
                .put("version", it.version)
                .put("is_system", it.isSystem))
        }
        return ok(JSONObject().put("apps", arr).put("count", arr.length()))
    }

    fun apps_open(package_name: String): String {
        val r = Paicot.appManager.open(package_name)
        return if (r.ok) ok(r.data) else fail(r.error!!)
    }

    fun apps_info(package_name: String): String {
        val r = Paicot.appManager.info(package_name)
        return if (r.ok) ok(r.data) else fail(r.error!!)
    }

    // --- screen -------------------------------------------------------------

    fun screen_tap(x: Int, y: Int): String {
        val r = Paicot.screenManager.tap(x, y)
        return if (r.ok) ok(r.data) else fail(r.error!!)
    }

    fun screen_swipe(x1: Int, y1: Int, x2: Int, y2: Int, duration: Int = 500): String {
        val r = Paicot.screenManager.swipe(x1, y1, x2, y2, duration.toLong())
        return if (r.ok) ok(r.data) else fail(r.error!!)
    }

    fun screen_long_press(x: Int, y: Int, duration: Int = 800): String {
        val r = Paicot.screenManager.longPress(x, y, duration.toLong())
        return if (r.ok) ok(r.data) else fail(r.error!!)
    }

    fun screen_size(): String {
        val r = Paicot.screenManager.size()
        return if (r.ok) ok(r.data) else fail(r.error!!)
    }

    fun screen_screenshot(): String {
        val r = Paicot.screenManager.screenshot()
        return if (r.ok) ok(r.data) else fail(r.error!!)
    }

    // --- keyboard -----------------------------------------------------------

    fun keyboard_type(text: String): String {
        val r = Paicot.keyboardManager.type(text)
        return if (r.ok) ok(r.data) else fail(r.error!!)
    }

    fun keyboard_key(key: String): String {
        val r = Paicot.keyboardManager.key(key)
        return if (r.ok) ok(r.data) else fail(r.error!!)
    }

    // --- accessibility --------------------------------------------------------

    fun accessibility_find(text: String): String {
        val r = com.paicot.accessibility.PaicotAccessibilityService.instance?.findByText(text)
            ?: return fail("Accessibility service not connected")
        return if (r.ok) ok(r.data) else fail(r.error!!)
    }

    fun accessibility_click(text: String): String {
        val r = com.paicot.accessibility.PaicotAccessibilityService.instance?.clickByText(text)
            ?: return fail("Accessibility service not connected")
        return if (r.ok) ok(r.data) else fail(r.error!!)
    }

    // --- display --------------------------------------------------------------

    fun display_show(title: String, body: String = "", x: Int = 60, y: Int = 200): String {
        val r = Paicot.displayManager.showPanel(title, body, x, y)
        return if (r.ok) ok(r.data) else fail(r.error!!)
    }

    fun display_hide(): String {
        val r = Paicot.displayManager.hidePanel()
        return if (r.ok) ok(r.data) else fail(r.error!!)
    }

    // --- files ----------------------------------------------------------------

    fun files_read(path: String): String {
        val r = Paicot.fileManager.readPrivate(path)
        return if (r.ok) ok(r.data) else fail(r.error!!)
    }

    fun files_write(path: String, content: String): String {
        val r = Paicot.fileManager.writePrivate(path, content)
        return if (r.ok) ok(r.data) else fail(r.error!!)
    }

    fun files_list(path: String = ""): String {
        val r = Paicot.fileManager.listPrivate(path)
        return if (r.ok) ok(r.data) else fail(r.error!!)
    }

    // --- notifications ----------------------------------------------------------

    fun notifications_active(): String = ok(Paicot.notificationCollector.toJson())

    // --- device -----------------------------------------------------------------

    fun device_info(): String = ok(Paicot.deviceManager.info().toJson())

    fun device_network(): String = ok(Paicot.deviceManager.networkInfo())

    // --- server -------------------------------------------------------------------

    fun server_status(): String = ok(Paicot.server.status().toJson())

    // --- helpers ---------------------------------------------------------------

    private fun ok(data: JSONObject): String = OperationResult.success(data).toJson().toString()
    private fun ok(data: JSONObject, error: String?): String =
        (if (error == null) OperationResult.success(data) else OperationResult.failure(error)).toJson().toString()
    private fun fail(error: String): String = OperationResult.failure(error).toJson().toString()

    /** Run a Python snippet from native callers (used by the Automation engine). */
    fun runPythonAsync(code: String, onDone: (String) -> Unit) {
        Thread({
            PaicotLogger.i("bridge", "Delegating script to Python engine")
            val result = Paicot.pythonRuntime.run(code)
            val status = if (result.state == ExecutionState.COMPLETED) "ok" else "failed"
            onDone(JSONObject()
                .put("status", status)
                .put("stdout", result.stdout)
                .put("stderr", result.stderr)
                .put("error", result.error ?: JSONObject.NULL)
                .put("duration_ms", result.durationMs)
                .toString())
        }, "paicot-automation").start()
    }
}
