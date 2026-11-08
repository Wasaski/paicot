package com.paicot.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.paicot.core.OperationResult
import org.json.JSONArray
import org.json.JSONObject

/**
 * PAICOT automation engine. Single live instance held in a companion so the
 * scripting bridge can dispatch gestures and read the accessibility tree.
 * All actions are real: dispatchGesture (tap/swipe/long-press) and node
 * inspection/click. Nothing here promises root or shell access.
 *
 * Tree walks are breadth-first with a hard node cap + deadline: big app trees
 * (e.g. Chrome) have thousands of nodes, an unbounded recursive walk can pin
 * the scripting thread for minutes (this exact failure wedged the API once).
 */
class PaicotAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onInterrupt() = Unit

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    fun isConnected(): Boolean = instance != null

    fun tap(x: Int, y: Int): OperationResult {
        val svc = instance ?: return OperationResult.failure("Accessibility service not connected")
        val d = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(
                Path().apply { moveTo(x.toFloat(), y.toFloat()) }, 0L, 80L))
            .build()
        val dispatched = svc.dispatchGesture(d, null, null)
        return if (dispatched) OperationResult.success(JSONObject().put("x", x).put("y", y))
        else OperationResult.failure("dispatchGesture returned false")
    }

    fun swipe(x1: Int, y1: Int, x2: Int, y2: Int, durationMs: Long = 500L): OperationResult {
        val svc = instance ?: return OperationResult.failure("Accessibility service not connected")
        val path = Path().apply { moveTo(x1.toFloat(), y1.toFloat()); lineTo(x2.toFloat(), y2.toFloat()) }
        val d = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, durationMs.coerceIn(50, 5000)))
            .build()
        val dispatched = svc.dispatchGesture(d, null, null)
        return if (dispatched) OperationResult.success(JSONObject().put("from", "$x1,$y1").put("to", "$x2,$y2"))
        else OperationResult.failure("dispatchGesture returned false")
    }

    fun longPress(x: Int, y: Int, durationMs: Long = 800L): OperationResult {
        val svc = instance ?: return OperationResult.failure("Accessibility service not connected")
        val d = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(
                Path().apply { moveTo(x.toFloat(), y.toFloat()) }, 0L, durationMs.coerceIn(300, 5000)))
            .build()
        val dispatched = svc.dispatchGesture(d, null, null)
        return if (dispatched) OperationResult.success(JSONObject().put("x", x).put("y", y))
        else OperationResult.failure("dispatchGesture returned false")
    }

    fun globalAction(action: String): OperationResult {
        val svc = instance ?: return OperationResult.failure("Accessibility service not connected")
        val code = when (action.lowercase()) {
            "back" -> GLOBAL_ACTION_BACK
            "home" -> GLOBAL_ACTION_HOME
            "recents" -> GLOBAL_ACTION_RECENTS
            "notifications" -> GLOBAL_ACTION_NOTIFICATIONS
            "quick_settings" -> GLOBAL_ACTION_QUICK_SETTINGS
            "power_dialog" -> GLOBAL_ACTION_POWER_DIALOG
            else -> return OperationResult.failure("Unknown global action: $action")
        }
        val done = svc.performGlobalAction(code)
        return if (done) OperationResult.success(JSONObject().put("action", action))
        else OperationResult.failure("performGlobalAction returned false")
    }

    fun findByText(text: String, limit: Int = 20): OperationResult {
        val svc = instance ?: return OperationResult.failure("Accessibility service not connected")
        val root = svc.rootInActiveWindow ?: return OperationResult.failure("No active window")
        val deadline = System.nanoTime() + FIND_DEADLINE_MS * 1_000_000L
        val found = mutableListOf<JSONObject>()
        var visited = 0
        walkBreadthFirst(root, deadline, MAX_VISITED_NODES) { node ->
            if (visited > MAX_VISITED_NODES || System.nanoTime() > deadline) return@walkBreadthFirst
            visited++
            val t = node.text?.toString() ?: ""
            val desc = node.contentDescription?.toString() ?: ""
            if (t.contains(text, ignoreCase = true) || desc.contains(text, ignoreCase = true)) {
                val b = android.graphics.Rect()
                node.getBoundsInScreen(b)
                found += JSONObject()
                    .put("text", t)
                    .put("description", desc)
                    .put("class", node.className ?: "")
                    .put("view_id", node.viewIdResourceName ?: "")
                    .put("clickable", node.isClickable)
                    .put("center_x", b.centerX())
                    .put("center_y", b.centerY())
            }
        }
        return OperationResult.success(
            JSONObject()
                .put("elements", JSONArray(found))
                .put("visited_nodes", visited))
    }

    fun clickByText(text: String): OperationResult {
        val svc = instance ?: return OperationResult.failure("Accessibility service not connected")
        val root = svc.rootInActiveWindow ?: return OperationResult.failure("No active window")
        val deadline = System.nanoTime() + FIND_DEADLINE_MS * 1_000_000L
        var clicked: JSONObject? = null
        var visited = 0
        walkBreadthFirst(root, deadline, MAX_VISITED_NODES) { node ->
            if (clicked != null || visited > MAX_VISITED_NODES || System.nanoTime() > deadline) return@walkBreadthFirst
            visited++
            val t = node.text?.toString() ?: ""
            val desc = node.contentDescription?.toString() ?: ""
            if (t.contains(text, ignoreCase = true) || desc.contains(text, ignoreCase = true)) {
                var target: AccessibilityNodeInfo? = node
                var hops = 0
                while (target != null && !target.isClickable && hops < 6) {
                    target = target.parent
                    hops++
                }
                if (target != null && target.isClickable && target.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    val b = android.graphics.Rect()
                    target.getBoundsInScreen(b)
                    clicked = JSONObject()
                        .put("text", t)
                        .put("center_x", b.centerX())
                        .put("center_y", b.centerY())
                }
            }
        }
        val hit = clicked ?: return OperationResult.failure("No clickable element matching: $text")
        return OperationResult.success(hit)
    }

    fun focusedText(): OperationResult {
        val svc = instance ?: return OperationResult.failure("Accessibility service not connected")
        val focused = svc.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        return if (focused != null) {
            OperationResult.success(JSONObject().put("text", focused.text?.toString() ?: ""))
        } else OperationResult.failure("No focused input node")
    }

    /**
     * Iterative BFS. Bounded by node cap + deadline so a huge tree can never
     * pin the caller thread.
     */
    private fun walkBreadthFirst(
        root: AccessibilityNodeInfo,
        deadlineNanos: Long,
        maxNodes: Int,
        visit: (AccessibilityNodeInfo) -> Unit
    ) {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var seen = 0
        while (queue.isNotEmpty() && seen < maxNodes && System.nanoTime() < deadlineNanos) {
            val node = queue.removeFirst()
            seen++
            visit(node)
            val count = node.childCount
            for (i in 0 until count) {
                val child = node.getChild(i)
                if (child != null) queue.add(child)
            }
        }
    }

    companion object {
        @Volatile var instance: PaicotAccessibilityService? = null
        private const val MAX_VISITED_NODES = 1500
        private const val FIND_DEADLINE_MS = 3500L
    }
}
