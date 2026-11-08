package com.paicot.keyboard

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.PersistableBundle
import android.view.accessibility.AccessibilityNodeInfo
import com.paicot.accessibility.PaicotAccessibilityService
import com.paicot.core.OperationResult
import org.json.JSONObject

/**
 * Text insertion: puts text on the clipboard, then performs ACTION_PASTE on the
 * focused node. Clipboard is restored afterwards. Navigation keys use
 * AccessibilityService.performGlobalAction. Raw keycode injection needs an IME
 * or root — reported as unsupported instead of faked.
 */
class KeyboardManager(private val context: Context) {

    fun type(text: String, restoreClipboard: Boolean = true): OperationResult {
        if (text.isEmpty()) return OperationResult.failure("Empty text")
        if (text.length > 5000) return OperationResult.failure("Text exceeds 5000 characters")
        val svc = PaicotAccessibilityService.instance
            ?: return OperationResult.failure("Accessibility service not connected")
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val previous = if (restoreClipboard) clipboard.primaryClip else null
        clipboard.setPrimaryClip(ClipData.newPlainText("paicot", text))
        val focused = svc.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focused == null) {
            if (previous != null) clipboard.setPrimaryClip(previous)
            return OperationResult.failure("No focused input node — tap a text field first")
        }
        val pasted = focused.performAction(AccessibilityNodeInfo.ACTION_PASTE)
        if (restoreClipboard) {
            if (previous != null) clipboard.setPrimaryClip(previous)
            else clipboard.clearPrimaryClip()
        }
        return if (pasted) {
            OperationResult.success(JSONObject().put("typed_chars", text.length))
        } else OperationResult.failure("Paste action rejected by focused node")
    }

    fun key(keyName: String): OperationResult {
        val svc = PaicotAccessibilityService.instance
            ?: return OperationResult.failure("Accessibility service not connected")
        val normalized = keyName.lowercase()
        val result = if (normalized in GLOBAL_KEYS) svc.globalAction(normalized.replace("_", "-").replace("-", "_"))
        else OperationResult.failure("Key '$keyName' not supported without an IME/root. Supported: ${GLOBAL_KEYS.joinToString()}")
        return result
    }

    private val GLOBAL_KEYS = setOf("back", "home", "recents", "notifications", "quick_settings", "power_dialog")
}

/** Silences the unused-import lint for clipboard description helpers. */
@Suppress("unused")
private fun ClipData.hasText(): Boolean = description.hasPrimaryMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)
