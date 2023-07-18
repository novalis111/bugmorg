package org.bugm.borg

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Start the ScreenUnlockForegroundService when the device is booted
            val serviceIntent = Intent(context, ScreenUnlockForegroundService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }
}
