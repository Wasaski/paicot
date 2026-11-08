package com.paicot.notifications

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.paicot.Paicot

/**
 * Real listener service (enabled by the user in Settings -> Notifications).
 * Forwards posts/removals to the in-memory collector.
 */
class PaicotNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        val posted = sbn ?: return
        Paicot.notificationCollector.posted(posted.notification, posted.packageName, posted.key)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        val removed = sbn ?: return
        Paicot.notificationCollector.removed(removed.key)
    }
}
