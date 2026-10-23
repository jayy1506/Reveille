package com.example.reveille

import android.content.Context
import java.time.LocalDate

object JournalStore {
    private fun prefs(c: Context) = c.getSharedPreferences("reveille_journal", Context.MODE_PRIVATE)

    fun streak(c: Context): Int = prefs(c).getInt("streak", 0)

    fun recordSubmission(c: Context) {
        val today = LocalDate.now().toString()
        val lastDate = prefs(c).getString("last_date", "")
        if (lastDate != today) {
            val currentStreak = streak(c)
            prefs(c).edit()
                .putInt("streak", currentStreak + 1)
                .putString("last_date", today)
                .apply()
        }
    }
}