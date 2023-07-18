package org.bugm.borg

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

object AppInitialization {
    private const val PREFS_NAME = "BugmOrgPrefs"
    private const val KEY_APP_INITIALIZED = "app_initialized"

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_APP_INITIALIZED, false)) {
            // The app is launched for the first time, start the service here
            startScreenUnlockForegroundService(context)

            // Mark the app as initialized
            prefs.edit().putBoolean(KEY_APP_INITIALIZED, true).apply()
        }
    }

    private fun startScreenUnlockForegroundService(context: Context) {
        val serviceIntent = Intent(context, ScreenUnlockForegroundService::class.java)
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}
