package com.paicot.device

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.StatFs
import android.view.WindowManager
import com.paicot.core.DeviceInfo
import java.io.File

/** Read-only device facts (no root, no privileged APIs). */
class DeviceManager(private val context: Context) {

    fun info(): DeviceInfo {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val bounds = android.graphics.Rect()
        @Suppress("DEPRECATION")
        wm.defaultDisplay?.getRect(bounds)
        val battery = battery()
        return DeviceInfo(
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            brand = Build.BRAND,
            device = Build.DEVICE,
            product = Build.PRODUCT,
            androidVersion = Build.VERSION.RELEASE ?: "unknown",
            sdkInt = Build.VERSION.SDK_INT,
            board = Build.BOARD,
            hardware = Build.HARDWARE,
            screenWidthPx = bounds.width(),
            screenHeightPx = bounds.height(),
            density = context.resources.displayMetrics.density,
            totalMemoryBytes = android.app.ActivityManager.MemoryInfo().let {
                (context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager)
                    .getMemoryInfo(it); it.totalMem
            },
            batteryPercent = battery.first,
            batteryStatus = battery.second
        )
    }

    fun battery(): Pair<Int, String> {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return -1 to "unknown"
        val pct = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) * 100 /
            maxOf(1, intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100))
        val status = when (intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "full"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "not_charging"
            else -> "unknown"
        }
        return pct to status
    }

    fun networkInfo(): org.json.JSONObject {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork)
        val types = mutableListOf<String>()
        if (caps != null) {
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) types += "wifi"
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) types += "cellular"
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) types += "ethernet"
        }
        val internal = StatFs(File(context.filesDir.absolutePath).path)
        return org.json.JSONObject().apply {
            put("transports", org.json.JSONArray(types))
            put("available_bytes", internal.availableBytes)
            put("total_bytes", internal.totalBytes)
        }
    }
}
