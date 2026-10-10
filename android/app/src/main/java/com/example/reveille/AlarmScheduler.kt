package com.example.reveille

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object AlarmScheduler {
    private const val DAILY_CODE = 0
    private const val TEST_CODE = 100

    private fun alarmIntent(context: Context, code: Int, daily: Boolean): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).putExtra("daily", daily)
        return PendingIntent.getBroadcast(
            context, code, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun showIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context, 200, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun nextTriggerMillis(hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    private fun manager(context: Context) =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleDaily(context: Context, hour: Int, minute: Int) {
        val info = AlarmManager.AlarmClockInfo(nextTriggerMillis(hour, minute), showIntent(context))
        manager(context).setAlarmClock(info, alarmIntent(context, DAILY_CODE, true))
    }

    fun cancelDaily(context: Context) {
        manager(context).cancel(alarmIntent(context, DAILY_CODE, true))
    }

    fun scheduleTest(context: Context, secondsFromNow: Int) {
        val triggerAt = System.currentTimeMillis() + secondsFromNow * 1000L
        val info = AlarmManager.AlarmClockInfo(triggerAt, showIntent(context))
        manager(context).setAlarmClock(info, alarmIntent(context, TEST_CODE, false))
    }
}