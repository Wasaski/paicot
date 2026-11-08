package com.paicot.notifications

import android.app.Notification
import android.os.Build
import com.paicot.core.ActiveNotification
import com.paicot.core.PaicotLogger
import org.json.JSONArray
import org.json.JSONObject

/** In-memory store fed by PaicotNotificationListener. */
class NotificationCollector {
    private val store = LinkedHashMap<String, ActiveNotification>()
    private val lock = Any()

    fun posted(notification: Notification, packageName: String, key: String) {
        val extras = notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
        synchronized(lock) {
            store[key] = ActiveNotification(packageName, title, text, System.currentTimeMillis())
        }
        PaicotLogger.i("notifications", "Posted from $packageName: $title")
    }

    fun removed(key: String) {
        synchronized(lock) { store.remove(key) }
    }

    fun active(): List<ActiveNotification> = synchronized(lock) { store.values.toList() }

    fun toJson(): JSONObject {
        val arr = JSONArray()
        active().forEach {
            arr.put(JSONObject()
                .put("package", it.packageName)
                .put("title", it.title)
                .put("text", it.text)
                .put("posted_at", it.postedAt))
        }
        return JSONObject().put("count", arr.length()).put("notifications", arr)
    }
}

@Suppress("DEPRECATION")
fun isAndroidTiramisuOrLater(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
