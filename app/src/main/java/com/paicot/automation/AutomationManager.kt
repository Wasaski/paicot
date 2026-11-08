package com.paicot.automation

import android.content.Context
import com.paicot.Paicot
import com.paicot.core.ExecutionResult
import com.paicot.core.ExecutionState
import com.paicot.core.PaicotLogger
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Automation controller: runs saved scripts through the Python engine and
 * tracks each active/completed run for the Automation screen.
 */
class AutomationManager(private val context: Context) {

    private data class ActiveRun(
        val runId: String,
        val scriptId: Long?,
        val scriptName: String,
        @Volatile var state: ExecutionState,
        @Volatile var result: ExecutionResult? = null
    )

    private val runs = ConcurrentHashMap<String, ActiveRun>()

    fun runScript(scriptId: Long?, scriptName: String, code: String): JSONObject {
        val runId = UUID.randomUUID().toString().substring(0, 8)
        runs[runId] = ActiveRun(runId, scriptId, scriptName, ExecutionState.RUNNING)
        PaicotLogger.i("automation", "Run $runId started: $scriptName")
        Thread({
            val result = Paicot.pythonRuntime.run(code)
            val active = runs[runId]
            if (active != null) {
                active.state = result.state
                active.result = result
            }
            if (scriptId != null) {
                Paicot.automationManager.afterScriptRun(scriptId, result)
            }
            PaicotLogger.i("automation", "Run $runId finished: ${result.state}")
        }, "paicot-run-$runId").start()
        return JSONObject().put("run_id", runId).put("state", ExecutionState.RUNNING.name.lowercase())
    }

    fun cancel(runId: String): JSONObject {
        val active = runs[runId] ?: return JSONObject().put("ok", false).put("error", "Unknown run")
        // Cooperative cancellation: every paicot.sleep() and bridge call checks the flag.
        Paicot.bridge.cancelRequested = true
        active.state = ExecutionState.CANCELLED
        PaicotLogger.w("automation", "Run $runId cancel requested")
        return JSONObject().put("ok", true).put("run_id", runId)
    }

    fun status(runId: String): JSONObject {
        val active = runs[runId] ?: return JSONObject().put("ok", false).put("error", "Unknown run")
        val o = JSONObject()
            .put("run_id", runId)
            .put("script", active.scriptName)
            .put("state", active.state.name.lowercase())
        active.result?.let { o.put("stdout", it.stdout).put("stderr", it.stderr).put("error", it.error ?: org.json.JSONObject.NULL) }
        return o
    }

    fun recent(): List<JSONObject> = runs.values
        .sortedWith(compareByDescending<ActiveRun> { it.result?.startedAt ?: Long.MAX_VALUE })
        .take(20)
        .map { active ->
            JSONObject()
                .put("run_id", active.runId)
                .put("script", active.scriptName)
                .put("state", active.state.name.lowercase())
        }

    internal fun afterScriptRun(scriptId: Long, result: ExecutionResult) {
        // Script bookkeeping happens in the caller coroutine (DB is suspend);
        // here we only record the last-run state in the log.
        PaicotLogger.i("automation", "Script $scriptId last run: ${result.state}")
    }
}
