package com.paicot.python

import android.content.Context
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.paicot.bridge.PaicotBridge
import com.paicot.core.ExecutionResult
import com.paicot.core.ExecutionState
import com.paicot.core.OperationResult
import com.paicot.core.PaicotLogger
import com.paicot.database.ExecutionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

/**
 * Embedded Python 3.11 via Chaquopy. All execution is serial (one script at
 * a time) on a dedicated thread; stdout/stderr/result/exceptions are captured
 * by paicot._runner in pure Python so they are identical offline and on-device.
 *
 * Cancellation is cooperative: paicot.sleep() and every bridge method call
 * paicot_bridge.ping(), which raises when cancelRequested is set. A hard
 * timeout also marks the run TIMED_OUT (the thread is abandoned — Python
 * threads cannot be killed safely).
 */
class PythonRuntime(
    private val context: Context,
    private val bridge: PaicotBridge
) {
    @Volatile var ready = false
        private set
    @Volatile var initError: String? = null
        private set

    @Volatile
    private var executor = newScriptExecutor()

    private fun newScriptExecutor() = Executors.newSingleThreadExecutor { r ->
        Thread(r, "paicot-python").apply { isDaemon = true }
    }

    /**
     * On timeout the stuck script thread is un-killable in Python; instead of
     * letting every later script queue behind it forever, snap the executor
     * to a fresh thread and abandon the broken one (it stays a daemon until
     * the process dies).
     */
    private fun abandonStuckThread() {
        PaicotLogger.w("python", "Replacing python executor after timeout")
        try { executor.shutdownNow() } catch (_: Exception) {}
        executor = newScriptExecutor()
    }
    private val scope = CoroutineScope(Dispatchers.IO)

    private var current: Future<ExecutionResult>? = null
    @Volatile private var currentId: String? = null

    fun init() {
        if (ready) return
        try {
            Python.start(AndroidPlatform(context))
            val py = Python.getInstance()
            val builtins = py.getModule("builtins")
            builtins.put("paicot_bridge", bridge)
            ready = true
            val version = try { py.getModule("sys").get("version").toString() } catch (_: Exception) { "3.11" }
            PaicotLogger.i("python", "Chaquopy runtime ready (Python $version)")
        } catch (e: Throwable) {
            initError = e.message
            PaicotLogger.e("python", "Chaquopy init failed: ${e.message}")
        }
    }

    fun run(code: String, source: String = "inline", timeoutSeconds: Long = 30): ExecutionResult {
        init()
        if (!ready) {
            return ExecutionResult(
                id = UUID.randomUUID().toString().substring(0, 8),
                state = ExecutionState.FAILED,
                error = "Python runtime not ready: $initError",
                startedAt = System.currentTimeMillis(),
                finishedAt = System.currentTimeMillis(),
                source = source
            )
        }
        val id = UUID.randomUUID().toString().substring(0, 8)
        bridge.cancelRequested = false
        val startedAt = System.currentTimeMillis()
        val py = Python.getInstance()
        val runnerOrNull = try { py.getModule("paicot._runner") } catch (e: Throwable) { null }
        val runner = runnerOrNull ?: return ExecutionResult(
            id = id, state = ExecutionState.FAILED,
            error = "paicot._runner module missing (Chaquopy asset missing?)",
            startedAt = startedAt, finishedAt = System.currentTimeMillis(), source = source
        )
        currentId = id
        val future: Future<ExecutionResult> = executor.submit<ExecutionResult> {
            try {
                val out = runner.callAttr("run_code", code)
                interpret(id, out, startedAt, source)
            } catch (e: Throwable) {
                ExecutionResult(
                    id = id, state = ExecutionState.FAILED,
                    error = "${e.javaClass.simpleName}: ${e.message}",
                    startedAt = startedAt, finishedAt = System.currentTimeMillis(),
                    durationMs = System.currentTimeMillis() - startedAt, source = source
                )
            }
        }
        current = future
        val result = try {
            future.get(timeoutSeconds, TimeUnit.SECONDS)
        } catch (e: java.util.concurrent.TimeoutException) {
            bridge.cancelRequested = true
            abandonStuckThread()
            ExecutionResult(
                id = id, state = ExecutionState.TIMED_OUT,
                error = "Execution exceeded ${timeoutSeconds}s",
                startedAt = startedAt, finishedAt = System.currentTimeMillis(),
                durationMs = System.currentTimeMillis() - startedAt, source = source
            )
        } catch (e: Exception) {
            ExecutionResult(
                id = id, state = ExecutionState.FAILED,
                error = "${e.javaClass.simpleName}: ${e.message}",
                startedAt = startedAt, finishedAt = System.currentTimeMillis(),
                durationMs = System.currentTimeMillis() - startedAt, source = source
            )
        }
        currentId = null
        persist(result)
        return result
    }

    fun cancel(): OperationResult {
        if (currentId == null) return OperationResult.failure("No execution running")
        bridge.cancelRequested = true
        return OperationResult.success(JSONObject().put("cancelled", currentId))
    }

    fun isRunning(): Boolean = currentId != null

    private fun interpret(id: String, out: Any?, startedAt: Long, source: String): ExecutionResult {
        val finishedAt = System.currentTimeMillis()
        val map = ((out as? PyObject)?.asMap() as? Map<String, Any?>) ?: return ExecutionResult(
            id = id, state = ExecutionState.FAILED,
            error = "Unexpected runner output: $out",
            startedAt = startedAt, finishedAt = finishedAt,
            durationMs = finishedAt - startedAt, source = source)
        val error = map["error"]?.toString()
        val state = if (error != null) {
            if (error.startsWith("Cancelled")) ExecutionState.CANCELLED else ExecutionState.FAILED
        } else ExecutionState.COMPLETED
        return ExecutionResult(
            id = id,
            state = state,
            stdout = map["stdout"]?.toString() ?: "",
            stderr = map["stderr"]?.toString() ?: "",
            result = map["result"]?.toString(),
            error = error,
            startedAt = startedAt,
            finishedAt = finishedAt,
            durationMs = finishedAt - startedAt,
            source = source
        )
    }

    private fun persist(result: ExecutionResult) {
        scope.launch {
            try {
                com.paicot.Paicot.database.executionDao().insert(
                    ExecutionEntity(
                        execId = result.id,
                        source = result.source,
                        state = result.state.name,
                        stdout = result.stdout.take(65536),
                        stderr = result.stderr.take(65536),
                        error = result.error,
                        startedAt = result.startedAt,
                        finishedAt = result.finishedAt,
                        durationMs = result.durationMs
                    )
                )
            } catch (e: Exception) {
                PaicotLogger.w("python", "Execution history persist failed: ${e.message}")
            }
        }
    }
}
