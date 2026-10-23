package com.example.reveille

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class JournalResult(val reply: String, val tomorrowMission: String)

object JournalClient {
    private val BASE_URL: String
        get() = try {
            BuildConfig.BASE_URL
        } catch (e: Exception) {
            "http://10.0.2.2:8000"
        }

    fun submit(
        missions: List<Mission>,
        better: String = "",
        obstacle: String = "",
        tomorrow: String = "",
        morningAction: String = "",
        microGratitude: String = "",
        dailyAffirmation: String = "",
        dailyWins: String = "",
        growth1Percent: String = "",
        smallPivot: String = "",
        energyCheck: String = ""
    ): JournalResult? {
        return try {
            val conn = URL("$BASE_URL/journal").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 5000
            conn.readTimeout = 20000
            conn.doOutput = true

            val missionArr = JSONArray()
            missions.forEach { m ->
                missionArr.put(JSONObject().put("title", m.title).put("done", m.done))
            }

            val finalWins = dailyWins.ifBlank { better }
            val finalGrowth = growth1Percent.ifBlank { better }
            val finalPivot = smallPivot.ifBlank { obstacle }
            val finalAction = morningAction.ifBlank { tomorrow }

            val body = JSONObject()
                .put("missions", missionArr)
                .put("better", better)
                .put("obstacle", obstacle)
                .put("tomorrow", tomorrow)
                .put("morning_action", finalAction)
                .put("micro_gratitude", microGratitude)
                .put("daily_affirmation", dailyAffirmation)
                .put("daily_wins", finalWins)
                .put("growth_1percent", finalGrowth)
                .put("small_pivot", finalPivot)
                .put("energy_check", energyCheck)
                .toString()

            conn.outputStream.use { it.write(body.toByteArray()) }

            if (conn.responseCode != 200) {
                Log.e("Reveille", "Journal server returned ${conn.responseCode}")
                return null
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(text)
            JournalResult(
                reply = json.getString("reply"),
                tomorrowMission = json.getString("tomorrow_mission")
            )
        } catch (e: Exception) {
            Log.e("Reveille", "Journal submit failed", e)
            null
        }
    }
}