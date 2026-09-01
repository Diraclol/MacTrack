package com.dirac.mactrack.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.dirac.mactrack.MainActivity

const val REMINDER_CHANNEL_ID = "log_reminder"
private const val REMINDER_NOTIFICATION_ID = 1001

// Fired by the daily alarm (ReminderScheduler); posts the "log your food" reminder. minSdk is 26,
// so notification channels always exist.
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(REMINDER_CHANNEL_ID, "Log reminders", NotificationManager.IMPORTANCE_DEFAULT)
        )
        val open = Intent(context, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            context, 0, open,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Log your food")
            .setContentText("Tap to log what you've eaten today.")
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        nm.notify(REMINDER_NOTIFICATION_ID, notification)
    }
}
