package com.paicot.display

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.paicot.core.OperationResult
import org.json.JSONObject

/**
 * Floating panels over other apps (TYPE_APPLICATION_OVERLAY). Requires the
 * "Draw over other apps" permission, requested from the Permissions screen.
 * Panel content is a real View added to the WindowManager; hide() removes it.
 */
class DisplayManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var panelView: View? = null

    fun canDrawOverlay(): Boolean = Settings.canDrawOverlays(context)

    fun showPanel(title: String, body: String, x: Int = 60, y: Int = 200): OperationResult {
        if (!canDrawOverlay()) {
            return OperationResult.failure("Overlay permission missing — grant it in Permissions screen")
        }
        hidePanel()
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF141416.toInt())
            setPadding(24, 24, 24, 24)
        }
        layout.addView(TextView(context).apply {
            text = title
            textSize = 16f
            setTextColor(0xFFFFFFFF.toInt())
        })
        if (body.isNotEmpty()) {
            layout.addView(TextView(context).apply {
                text = body
                textSize = 13f
                setTextColor(0xFFCFCFD2.toInt())
            })
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = x
            this.y = y
        }
        return try {
            windowManager.addView(layout, params)
            panelView = layout
            OperationResult.success(JSONObject().put("panel", "shown").put("title", title))
        } catch (e: Exception) {
            OperationResult.failure("showPanel failed: ${e.message}")
        }
    }

    fun hidePanel(): OperationResult {
        val view = panelView ?: return OperationResult.success(JSONObject().put("panel", "already_hidden"))
        return try {
            windowManager.removeView(view)
            panelView = null
            OperationResult.success(JSONObject().put("panel", "hidden"))
        } catch (e: Exception) {
            OperationResult.failure("hidePanel failed: ${e.message}")
        }
    }

    fun isPanelVisible(): Boolean = panelView != null
}
