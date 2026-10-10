package com.example.reveille

import android.content.Context

object AlarmStore {
    private fun prefs(c: Context) = c.getSharedPreferences("reveille", Context.MODE_PRIVATE)

    fun save(c: Context, hour: Int, minute: Int, enabled: Boolean) {
        prefs(c).edit()
            .putInt("hour", hour)
            .putInt("minute", minute)
            .putBoolean("enabled", enabled)
            .apply()
    }

    fun hour(c: Context) = prefs(c).getInt("hour", 6)
    fun minute(c: Context) = prefs(c).getInt("minute", 0)
    fun enabled(c: Context) = prefs(c).getBoolean("enabled", false)
}