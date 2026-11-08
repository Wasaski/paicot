package com.paicot.applications

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.paicot.core.AppInfo
import com.paicot.core.OperationResult

/** Installed-app listing + launching via PackageManager / Intents. */
class AppManager(private val context: Context) {

    private val pm: PackageManager get() = context.packageManager

    fun listInstalled(includeSystem: Boolean = false, limit: Int = 200): List<AppInfo> {
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = try {
            if (Build.VERSION.SDK_INT >= 33) {
                pm.queryIntentActivities(launcherIntent, PackageManager.ResolveInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION") pm.queryIntentActivities(launcherIntent, 0)
            }
        } catch (e: Exception) {
            emptyList<android.content.pm.ResolveInfo>()
        }
        return resolveInfos.mapNotNull { ri ->
            val pkg = ri.activityInfo?.packageName ?: return@mapNotNull null
            val appInfo = runCatching {
                if (Build.VERSION.SDK_INT >= 33) {
                    pm.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION") pm.getApplicationInfo(pkg, 0)
                }
            }.getOrNull() ?: return@mapNotNull null
            val isSystem = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
            if (isSystem && !includeSystem) return@mapNotNull null
            val version = runCatching {
                val pi = if (Build.VERSION.SDK_INT >= 33) {
                    pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION") pm.getPackageInfo(pkg, 0)
                }
                pi.versionName ?: "?"
            }.getOrDefault("?")
            AppInfo(pkg, pm.getApplicationLabel(appInfo).toString(), version, isSystem, ri.activityInfo.name)
        }.sortedWith(compareBy({ it.label.lowercase() }, { it.packageName }))
            .take(limit.coerceIn(10, 500))
    }

    fun open(packageName: String, activity: String? = null): OperationResult {
        try {
            val intent = if (activity != null) {
                Intent(Intent.ACTION_VIEW).apply {
                    setClassName(packageName, activity)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                pm.getLaunchIntentForPackage(packageName)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                } ?: return OperationResult.failure("No launch intent for $packageName")
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return OperationResult.success(org.json.JSONObject().put("opened", packageName))
        } catch (e: Exception) {
            return OperationResult.failure("open failed: ${e.message}")
        }
    }

    fun openUrl(url: String): OperationResult {
        return try {
            context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            OperationResult.success(org.json.JSONObject().put("url", url))
        } catch (e: Exception) {
            OperationResult.failure("openUrl failed: ${e.message}")
        }
    }

    fun info(packageName: String): OperationResult {
        return try {
            val ai = if (Build.VERSION.SDK_INT >= 33) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION") pm.getApplicationInfo(packageName, 0)
            }
            val data = org.json.JSONObject()
                .put("package", packageName)
                .put("label", pm.getApplicationLabel(ai).toString())
                .put("is_system", (ai.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0)
            OperationResult.success(data)
        } catch (e: Exception) {
            OperationResult.failure("info failed: ${e.message}")
        }
    }
}
