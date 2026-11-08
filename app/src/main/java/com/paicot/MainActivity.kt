package com.paicot

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.paicot.server.PaicotForegroundService
import com.paicot.ui.PaicotRoot
import com.paicot.ui.theme.PaicotTheme

/**
 * Single-activity Compose host. Handles the three ActivityResult flows that
 * need a foreground activity:
 *  - SAF folder grant (OpenDocumentTree) for the Files engine
 *  - MediaProjection consent for screenshots
 */
class MainActivity : ComponentActivity() {

    private val safTreeLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            contentResolver.takePersistableUriPermission(uri, flags)
            Paicot.fileManager.let { }
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                Paicot.prefs.setSafTreeUri(uri.toString())
            }
            com.paicot.core.PaicotLogger.i("files", "SAF tree granted: $uri")
        }
    }

    private val captureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as android.media.MediaProjectionManager
            Paicot.mediaProjection = manager.getMediaProjection(result.resultCode, result.data!!)
            com.paicot.core.PaicotLogger.i("screen", "Screen-capture consent granted")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        // Touch only: emit; SAF/persist happens in the launcher above.
        setContent {
            PaicotTheme {
                PaicotRoot(
                    onRequestSafTree = { safTreeLauncher.launch(null) },
                    onRequestCapture = { captureLauncher.launch(Paicot.permissionManager.openCaptureIntent()) }
                )
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* best effort */ }
                    .launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    fun startServerService() = PaicotForegroundService.start(this)
    fun stopServerService() = PaicotForegroundService.stop(this)
}
