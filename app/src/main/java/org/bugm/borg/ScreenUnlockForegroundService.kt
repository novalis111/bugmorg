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
import android.view.WindowManager
import androidx.core.app.NotificationCompat

class ScreenUnlockForegroundService : Service() {

    private lateinit var windowManager: WindowManager

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "org.bugm.borg.notification_channel"
        private const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        showReminderActivity()

        // Create a notification channel (Required for Android 8.0 and above)
        createNotificationChannel()

        // Display the notification and start the service as a foreground service
        startForeground(NOTIFICATION_ID, createNotification())
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
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
