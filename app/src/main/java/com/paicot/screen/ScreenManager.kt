package com.paicot.screen

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.util.Base64
import com.paicot.Paicot
import com.paicot.accessibility.PaicotAccessibilityService
import com.paicot.core.OperationResult
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.nio.ByteBuffer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Screen control: gestures (via AccessibilityService), geometry, screenshot.
 *
 * Screenshot uses MediaProjection + ImageReader entirely through reflection:
 * the compile-time Android jar on some SDKs omits parts of `android.media`,
 * while the classes always exist at runtime on supported devices (API 21+).
 */
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
        val projection: Any = Paicot.mediaProjection
            ?: return OperationResult.failure("No screen-capture consent. Grant it in Permissions screen.")
        val dm = context.resources.displayMetrics
        var reader: Any? = null
        var virtualDisplay: Any? = null
        return try {
            val w = dm.widthPixels
            val h = dm.heightPixels
            val densityDpi = dm.densityDpi

            // ImageReader.newInstance(w, h, RGBA_8888, 2)
            val imageReaderClass = Class.forName("android.media.ImageReader")
            val newInstance: Method = imageReaderClass.getMethod(
                "newInstance",
                Int::class.javaPrimitiveType, Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType, Int::class.javaPrimitiveType
            )
            val imageReader: Any = newInstance.invoke(null, w, h, PixelFormat.RGBA_8888, 2)
            reader = imageReader

            val mainHandler = Handler(Looper.getMainLooper())
            val latch = CountDownLatch(1)
            var bitmap: Bitmap? = null

            // OnImageAvailableListener via dynamic proxy
            val listenerIface = Class.forName("android.media.ImageReader\$OnImageAvailableListener")
            val handler = InvocationHandler { _, method, args ->
                if (method.name == "onImageAvailable") {
                    val ir = args[0]
                    try {
                        val image = imageReaderClass.getMethod("acquireLatestImage").invoke(ir)
                        if (image == null) {
                            latch.countDown()
                            return@InvocationHandler null
                        }
                        val planes = image.javaClass.getMethod("getPlanes").invoke(image) as Array<out Any>
                        val plane = planes[0]
                        val buffer = plane.javaClass.getMethod("getBuffer").invoke(plane) as ByteBuffer
                        val pixelStride = plane.javaClass.getMethod("getPixelStride").invoke(plane) as Int
                        val rowStride = plane.javaClass.getMethod("getRowStride").invoke(plane) as Int
                        val rowPadding = rowStride - pixelStride * w
                        val bmp = Bitmap.createBitmap(w + rowPadding / pixelStride, h, Bitmap.Config.ARGB_8888)
                        bmp.copyPixelsFromBuffer(buffer)
                        bitmap = Bitmap.createBitmap(bmp, 0, 0, w, h)
                        image.javaClass.getMethod("close").invoke(image)
                    } catch (_: Exception) {
                    } finally {
                        latch.countDown()
                    }
                }
                null
            }
            val listener = Proxy.newProxyInstance(
                imageReaderClass.classLoader, arrayOf(listenerIface), handler)

            imageReaderClass.getMethod(
                "setImageListener", listenerIface, Handler::class.java
            ).invoke(imageReader, listener, mainHandler)

            val surface = imageReaderClass.getMethod("getSurface").invoke(imageReader)
            val callbackClass = Class.forName("android.media.ImageReader\$Callback")
            val vdMethod = projection.javaClass.getMethod(
                "createVirtualDisplay",
                String::class.java,
                Int::class.javaPrimitiveType, Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType, Int::class.javaPrimitiveType,
                android.view.Surface::class.java,
                callbackClass,
                Handler::class.java
            )
            virtualDisplay = vdMethod.invoke(
                projection, "paicot-capture", w, h, densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                surface, null, mainHandler
            )

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
            try { virtualDisplay?.javaClass?.getMethod("release")?.invoke(virtualDisplay) } catch (_: Exception) {}
            try { reader?.javaClass?.getMethod("close")?.invoke(reader) } catch (_: Exception) {}
        }
    }
}
