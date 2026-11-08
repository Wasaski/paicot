package com.paicot.permissions

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.core.app.NotificationManagerCompat
import com.paicot.Paicot
import com.paicot.accessibility.PaicotAccessibilityService
import com.paicot.core.PermissionKind
import com.paicot.core.PermissionStatus

/**
 * Permission diagnosis + settings intents. PAICOT never tries to bypass
 * Android protections; every capability is granted by the user.
 */
class PermissionManager(private val context: Context) {

    fun accessibilityEnabled(): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabled = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
            .any { it.resolveInfo?.serviceInfo?.packageName == context.packageName }
        return enabled || PaicotAccessibilityService.instance != null
    }

    fun overlayGranted(): Boolean = Settings.canDrawOverlays(context)

    fun notificationListenerGranted(): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    fun captureGranted(): Boolean = Paicot.mediaProjection != null

    fun safGranted(): Boolean = runCatching {
        // Persisted URI permissions were granted at least once.
        context.contentResolver.persistedUriPermissions.isNotEmpty()
    }.getOrDefault(false)

    fun all(): List<PermissionStatus> = listOf(
        PermissionStatus(PermissionKind.ACCESSIBILITY, accessibilityEnabled(),
            if (accessibilityEnabled()) "Service connected" else "Enable PAICOT Automation in Accessibility settings"),
        PermissionStatus(PermissionKind.OVERLAY, overlayGranted(),
            if (overlayGranted()) "Draw-over-apps allowed" else "Grant overlay permission"),
        PermissionStatus(PermissionKind.NOTIFICATION_LISTENER, notificationListenerGranted(),
            if (notificationListenerGranted()) "Listener bound" else "Enable PAICOT in notification-access settings"),
        PermissionStatus(PermissionKind.CAPTURE, captureGranted(),
            if (captureGranted()) "Projection token held" else "Grant screen capture (upper right button)"),
        PermissionStatus(PermissionKind.FILES, safGranted(),
            if (safGranted()) "A storage tree is granted" else "Grant a folder via SAF (Files screen)"),
        PermissionStatus(PermissionKind.POST_NOTIFICATIONS,
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            } else true,
            "Runtime notifications permission")
    )

    fun pendingCount(): Int = all().count { !it.granted }

    // --- Settings intents -------------------------------------------------

    fun openAccessibilitySettings(): Intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    fun openOverlaySettings(): Intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        android.net.Uri.parse("package:${context.packageName}"))
    fun openNotificationAccessSettings(): Intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    fun openAppSettings(): Intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        android.net.Uri.parse("package:${context.packageName}"))
    fun openCaptureIntent(): Intent =
        (context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as android.media.MediaProjectionManager)
            .createScreenCaptureIntent()
}
