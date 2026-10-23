package com.example.reveille

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class JudgeResult(val valid: Boolean, val tasks: List<String>, val reply: String)

object JudgeClient {
    private val BASE_URL: String
        get() = try {
            BuildConfig.BASE_URL
        } catch (_: Exception) {
            "http://10.0.2.2:8000"
        }

    @Volatile
    var lastError: String = ""

    fun judge(
        transcript: String,
        name: String = "soldier",
        streak: Int = 0,
        yesterdayDone: List<String> = emptyList(),
        yesterdayMissed: List<String> = emptyList(),
        harshness: Int = 3,
        lastJournal: String = ""
    ): JudgeResult? {
        return try {
            val conn = URL("$BASE_URL/judge").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 5000
            conn.readTimeout = 20000
            conn.doOutput = true

            val body = JSONObject()
                .put("transcript", transcript)
                .put("name", name)
                .put("streak", streak)
                .put("yesterday_done", JSONArray(yesterdayDone))
                .put("yesterday_missed", JSONArray(yesterdayMissed))
                .put("harshness", harshness)
                .put("last_journal", lastJournal)
                .toString()

            conn.outputStream.use { it.write(body.toByteArray()) }

            if (conn.responseCode != 200) {
                lastError = "HTTP ${conn.responseCode}"
                Log.e("Reveille", "Server returned ${conn.responseCode}")
                return null
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(text)
            val arr = json.getJSONArray("tasks")
            val tasks = List(arr.length()) { arr.getString(it) }
            lastError = ""
            JudgeResult(json.getBoolean("valid"), tasks, json.getString("reply"))
        } catch (e: Exception) {
            lastError = e.javaClass.simpleName + ": " + (e.message ?: "")
            Log.e("Reveille", "judge failed", e)
            null
        }
    }
}