package com.paicot

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.paicot.applications.AppManager
import com.paicot.automation.AutomationManager
import com.paicot.bridge.PaicotBridge
import com.paicot.core.PaicotLogger
import com.paicot.core.Prefs
import com.paicot.database.PaicotDatabase
import com.paicot.device.DeviceManager
import com.paicot.display.DisplayManager
import com.paicot.keyboard.KeyboardManager
import com.paicot.notifications.NotificationCollector
import com.paicot.permissions.PermissionManager
import com.paicot.python.PythonRuntime
import com.paicot.screen.ScreenManager
import com.paicot.server.PaicotServer
import com.paicot.storage.FileManager

/**
 * Application entry point. Owns the dependency graph (composition root) used
 * by the UI, the HTTP server and the Python bridge.
 */
class PaicotApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Paicot.init(this)
        PaicotLogger.i("app", "PAICOT started")
        // Auto-start the local server as a foreground service so it survives
        // the user switching away. Failures are logged, never fatal.
        try {
            com.paicot.server.PaicotForegroundService.start(this)
        } catch (e: Exception) {
            PaicotLogger.e("app", "Auto-start server failed: ${e.message}")
        }
    }
}

/** Singleton holder for the composition root. */
object Paicot {
    lateinit var app: Context
        private set
    lateinit var prefs: Prefs
        private set
    lateinit var database: PaicotDatabase
        private set
    lateinit var pythonRuntime: PythonRuntime
        private set
    lateinit var server: PaicotServer
        private set
    lateinit var bridge: PaicotBridge
        private set
    lateinit var appManager: AppManager
        private set
    lateinit var screenManager: ScreenManager
        private set
    lateinit var keyboardManager: KeyboardManager
        private set
    lateinit var deviceManager: DeviceManager
        private set
    lateinit var fileManager: FileManager
        private set
    lateinit var displayManager: DisplayManager
    lateinit var notificationCollector: NotificationCollector
        private set
    lateinit var permissionManager: PermissionManager
        private set
    lateinit var automationManager: AutomationManager
        private set

    /** MediaProjection token granted by the user in MainActivity (screenshot consent).
     * Held as Any because the class itself is invoked reflectively. */
    @Volatile var mediaProjection: Any? = null

    fun init(context: Context) {
        app = context.applicationContext
        prefs = Prefs(app)
        database = Room.databaseBuilder(app, PaicotDatabase::class.java, "paicot.db")
            .enableMultiInstanceInvalidation()
            .build()
        deviceManager = DeviceManager(app)
        appManager = AppManager(app)
        screenManager = ScreenManager(app)
        keyboardManager = KeyboardManager(app)
        fileManager = FileManager(app)
        displayManager = DisplayManager(app)
        notificationCollector = NotificationCollector()
        permissionManager = PermissionManager(app)
        automationManager = AutomationManager(app)
        bridge = PaicotBridge(app)
        pythonRuntime = PythonRuntime(app, bridge)
        server = PaicotServer(app)
    }
}
