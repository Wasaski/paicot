package com.paicot.screen

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.os.Handler
import android.os.Looper
import android.util.Base64
import com.paicot.Paicot
import com.paicot.accessibility.PaicotAccessibilityService
import com.paicot.core.OperationResult
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

/** Screen control: gestures (via AccessibilityService), geometry, screenshot. */
class ScreenManager(private val context: Context) {

    fun tap(x: Int, y: Int): OperationResult =
        PaicotAccessibilityService.instance?.tap(x, y)
            ?: OperationResult.failure("Accessibility service not connected — enable it in Permissions")

    fun swipe(x1: Int, y1: Int, x2: Int, y2: Int, durationMs: Long = 500L): OperationResult =
        PaicotAccessibilityService.instance?.swipe(x1, y1, x2, y2, durationMs)
            ?: OperationResult.failure("Accessibility service not connected")

    fun longPress(x: Int, y: Int, durationMs: Long = 800L): OperationResult =
        PaicotAccessibilityService.instance?.longPress(x, y, durationMs)
            ?: OperationResult.failure("Accessibility service not connected")

    fun size(): OperationResult {
        val dm = context.resources.displayMetrics
        return OperationResult.success(JSONObject()
            .put("width_px", dm.widthPixels)
            .put("height_px", dm.heightPixels)
            .put("density", dm.density.toDouble())
            .put("density_dpi", dm.densityDpi)
            .put("scaled_density", dm.scaledDensity.toDouble()))
    }

    /**
     * Screenshot via MediaProjection. Requires the user consent token captured
     * by MainActivity (Settings -> Permissions -> Screen capture).
     * Returns Base64 PNG of the current display.
     */
    fun screenshot(): OperationResult {
        val projection = Paicot.mediaProjection
            ?: return OperationResult.failure("No screen-capture consent. Grant it in Permissions screen.")
        val dm = context.resources.displayMetrics
        var reader: ImageReader? = null
        var virtualDisplay: android.hardware.display.VirtualDisplay? = null
        return try {
            val w = dm.widthPixels
            val h = dm.heightPixels
            val densityDpi = dm.densityDpi
            val imageReader = ImageReader.newInstance(w, h, android.graphics.PixelFormat.RGBA_8888, 2)
            reader = imageReader
            virtualDisplay = projection.createVirtualDisplay(
                "paicot-capture", w, h, densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.surface, null, Handler(Looper.getMainLooper())
            )
            val latch = java.util.concurrent.CountDownLatch(1)
            var bitmap: Bitmap? = null
            imageReader.setImageListener({ ir ->
                try {
                    val image = ir.acquireLatestImage() ?: run { latch.countDown(); return@setImageListener }
                    val planes = image.planes
                    val buffer = planes[0].buffer
                    val pixelStride = planes[0].pixelStride
                    val rowStride = planes[0].rowStride
                    val rowPadding = rowStride - pixelStride * w
                    val bmp = Bitmap.createBitmap(w + (rowPadding / pixelStride).toInt(), h, Bitmap.Config.ARGB_8888)
                    bmp.copyPixelsFromBuffer(buffer)
                    bitmap = Bitmap.createBitmap(bmp, 0, 0, w, h)
                    image.close()
                } catch (e: Exception) {
                    // fall through to mime latch
                } finally {
                    latch.countDown()
                }
            }, Handler(Looper.getMainLooper()))
            if (!latch.await(8, TimeUnit.SECONDS)) {
                OperationResult.failure("Screenshot timed out after 8s")
            } else {
                val bmp = bitmap ?: return OperationResult.failure("Screenshot returned no frame")
                val out = ByteArrayOutputStream()
                bmp.compress(Bitmap.CompressFormat.PNG, 90, out)
                OperationResult.success(JSONObject()
                    .put("format", "png")
                    .put("width_px", bmp.width)
                    .put("height_px", bmp.height)
                    .put("base64", Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)))
            }
        } catch (e: Exception) {
            OperationResult.failure("Screenshot failed: ${e.message}")
        } finally {
            try { virtualDisplay?.release() } catch (_: Exception) {}
            try { reader?.close() } catch (_: Exception) {}
        }
    }
}
