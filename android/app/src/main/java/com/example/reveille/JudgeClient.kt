package com.example.reveille

import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class JudgeResult(val valid: Boolean, val tasks: List<String>, val reply: String)

object JudgeClient {
    private const val BASE_URL = "http://127.0.0.1:8000" // CHANGE to your laptop's IP

    fun judge(transcript: String): JudgeResult? {
        return try {
            val conn = URL("$BASE_URL/judge").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 5000
            conn.readTimeout = 20000
            conn.doOutput = true

            val body = JSONObject().put("transcript", transcript).toString()
            conn.outputStream.use { it.write(body.toByteArray()) }

            if (conn.responseCode != 200) {
                Log.e("Reveille", "Server returned ${conn.responseCode}")
                return null
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(text)
            val arr = json.getJSONArray("tasks")
            val tasks = List(arr.length()) { arr.getString(it) }
            JudgeResult(json.getBoolean("valid"), tasks, json.getString("reply"))
        } catch (e: Exception) {
            Log.e("Reveille", "judge failed", e)
            null
        }
    }
}