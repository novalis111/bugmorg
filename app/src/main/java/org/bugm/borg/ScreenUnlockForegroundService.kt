package org.bugm.borg

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.Uri
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import org.bugm.borg.AppInitialization.NOTIFICATION_CHANNEL_ID
import org.bugm.borg.AppInitialization.NOTIFICATION_ID
import org.bugm.borg.AppInitialization.PREFS_SELECTED_FILES

class ScreenUnlockForegroundService : Service() {

    private lateinit var selectedFiles: MutableList<SelectedFile>

    companion object {
        fun startService(context: Context) {
            val intent = Intent(context, ScreenUnlockForegroundService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        // Store granted uris
        private val grantedUris: MutableSet<Uri> = mutableSetOf()
        fun addGrantedUri(uri: Uri) {
            grantedUris.add(uri)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        selectedFiles = getSelectedFilesFromSharedPreferences(this)
        createNotificationChannel()

        // No notifications for now
        startForeground(NOTIFICATION_ID, createNotification())

        // Access the files using the stored permissions
        for (file in selectedFiles) {
            val uri = file.uriString.toUri()
            contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            addGrantedUri(uri)
        }

        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        registerScreenUnlockReceiver()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove the foreground service notification when the service is destroyed
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun createNotificationChannel() {
        val channelName = "Foreground Service Channel"
        val channelDescription = "Channel for the foreground service"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, importance).apply {
            description = channelDescription
        }

        // Set the LED color for the notification (Optional)
        channel.enableLights(true)
        channel.lightColor = Color.RED

        // Register the channel with the system
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent =
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher).setContentTitle("BugmOrg Service")
            .setContentText("Running in the background").setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MIN).setOngoing(false).setAutoCancel(true)

        return notificationBuilder.build()
    }

    private fun registerScreenUnlockReceiver() {
        val unlockReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_USER_PRESENT) {
                    showReminderActivity()
                }
            }
        }
        registerReceiver(unlockReceiver, IntentFilter(Intent.ACTION_USER_PRESENT))
    }

    private fun showReminderActivity() {
        val uriList: MutableList<Uri> = mutableListOf()
        for (uri in grantedUris) {
            try {
                uriList.add(uri)
            } catch (e: Exception) {
                // Handle invalid URIs here (e.g., log the error or skip the file)
                e.printStackTrace()
            }
        }
        val intent = Intent(this, ReminderActivity::class.java)
        intent.putParcelableArrayListExtra("uris", ArrayList(uriList))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
