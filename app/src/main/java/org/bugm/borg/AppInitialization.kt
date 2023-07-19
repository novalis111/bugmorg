package org.bugm.borg

import android.content.Context

object AppInitialization {
    const val PREFS_NAME = "org.bugmorg.prefs"
    const val NOTIFICATION_CHANNEL_ID = "org.bugmorg.notification_channel"
    const val NOTIFICATION_ID = 1
    const val PREFS_SELECTED_FILES = "org.bugmorg.selected_files"
    private const val KEY_APP_INITIALIZED = "app_initialized"

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_APP_INITIALIZED, false)) {
            // Mark the app as initialized
            prefs.edit().putBoolean(KEY_APP_INITIALIZED, true).apply()
        }
    }
}
