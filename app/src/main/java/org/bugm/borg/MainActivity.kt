package org.bugm.borg

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val requestOverlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Permission granted, start the service
            startScreenUnlockService()
        } else {
            // Permission denied, handle accordingly (show a message, etc.)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Check and request the SYSTEM_ALERT_WINDOW permission on devices with Android 12 (API 31) and above
        requestOverlayPermission()
    }

    private fun requestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            intent.data = Uri.parse("package:$packageName")
            requestOverlayPermissionLauncher.launch(intent)
        } else {
            startScreenUnlockService()
        }
    }

    private fun startScreenUnlockService() {
        val serviceIntent = Intent(this, ScreenUnlockForegroundService::class.java)
        startForegroundService(serviceIntent)
    }
}
