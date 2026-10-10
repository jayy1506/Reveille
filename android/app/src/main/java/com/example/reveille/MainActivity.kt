package com.example.reveille

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Button
import androidx.compose.material3.Text

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val perms = mutableListOf(android.Manifest.permission.RECORD_AUDIO)
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            perms.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        requestPermissions(perms.toTypedArray(), 0)

        setContent {
            Button(onClick = { scheduleAlarm(this@MainActivity, 30) }) {
                Text("Set alarm in 30 seconds")
            }
        }
    }
}

fun scheduleAlarm(context: Context, secondsFromNow: Int) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, AlarmReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context, 0, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val triggerAt = System.currentTimeMillis() + secondsFromNow * 1000L
    alarmManager.setAlarmClock(
        AlarmManager.AlarmClockInfo(triggerAt, pendingIntent),
        pendingIntent
    )
}