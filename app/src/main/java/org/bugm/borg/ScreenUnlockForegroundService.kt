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
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import org.bugm.borg.AppInitialization.NOTIFICATION_CHANNEL_ID
import org.bugm.borg.AppInitialization.NOTIFICATION_ID
import org.bugm.borg.AppInitialization.PREFS_SELECTED_FILES

class ScreenUnlockForegroundService : Service() {

    private lateinit var selectedFiles: MutableList<SelectedFile>

    companion object {
        // Method to start the service with selectedFiles as an argument
        fun startService(context: Context, selectedFiles: List<SelectedFile>) {
            val intent = Intent(context, ScreenUnlockForegroundService::class.java)
            intent.putParcelableArrayListExtra(PREFS_SELECTED_FILES, ArrayList(selectedFiles))
            ContextCompat.startForegroundService(context, intent)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Retrieve the selected files from the intent
        selectedFiles =
            intent?.getParcelableArrayListExtra(PREFS_SELECTED_FILES, SelectedFile::class.java)
                ?: mutableListOf()

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        return START_REDELIVER_INTENT
    }

    override fun onCreate() {
        super.onCreate()
        registerScreenUnlockReceiver()
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
            .setPriority(NotificationCompat.PRIORITY_LOW).setOngoing(false).setAutoCancel(true)

        // Return the notification
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
        val intent = Intent(this, ReminderActivity::class.java)
        intent.putParcelableArrayListExtra(PREFS_SELECTED_FILES, ArrayList(selectedFiles))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
