package com.example.reveille

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Arm tomorrow's alarm first, so it's set even if something below fails
        if (intent.getBooleanExtra("daily", false) && AlarmStore.enabled(context)) {
            AlarmScheduler.scheduleDaily(context, AlarmStore.hour(context), AlarmStore.minute(context))
        }

        Log.d("Reveille", "ALARM FIRED")

        val channelId = "reveille_alarm"
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(channelId, "Reveille Alarm", NotificationManager.IMPORTANCE_HIGH)
        channel.setSound(null, null) // the Activity plays the sound itself
        nm.createNotificationChannel(channel)

        val screenIntent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 1, screenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("REVEILLE")
            .setContentText("UP, SOLDIER!")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setFullScreenIntent(pendingIntent, true)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        nm.notify(1, notification)
    }
}