package com.example.reveille

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

data class Mission(val id: Long, val title: String, val date: String, val done: Boolean)

object MissionStore {
    private const val KEY = "missions"

    private fun prefs(c: Context) =
        c.getSharedPreferences("reveille_missions", Context.MODE_PRIVATE)

    private fun loadAll(c: Context): MutableList<Mission> {
        val raw = prefs(c).getString(KEY, "[]") ?: "[]"
        val arr = JSONArray(raw)
        val list = mutableListOf<Mission>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                Mission(o.getLong("id"), o.getString("title"), o.getString("date"), o.getBoolean("done"))
            )
        }
        return list
    }

    private fun saveAll(c: Context, list: List<Mission>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id)
                    .put("title", it.title)
                    .put("date", it.date)
                    .put("done", it.done)
            )
        }
        prefs(c).edit().putString(KEY, arr.toString()).apply()
    }

    fun today(c: Context): List<Mission> {
        val today = LocalDate.now().toString()   // like "2026-10-10"
        return loadAll(c).filter { it.date == today }
    }

    // A new report replaces today's missions, so repeating the alarm doesn't duplicate them
    fun replaceToday(c: Context, titles: List<String>) {
        val today = LocalDate.now().toString()
        val kept = loadAll(c).filter { it.date != today }.toMutableList()
        val base = System.currentTimeMillis()
        titles.forEachIndexed { i, t -> kept.add(Mission(base + i, t, today, false)) }
        saveAll(c, kept)
    }

    fun addTomorrowMission(c: Context, title: String) {
        val tomorrow = LocalDate.now().plusDays(1).toString()
        val list = loadAll(c)
        list.add(Mission(System.currentTimeMillis(), title, tomorrow, false))
        saveAll(c, list)
    }

    fun addMissionForToday(c: Context, title: String) {
        val today = LocalDate.now().toString()
        val list = loadAll(c)
        list.add(Mission(System.currentTimeMillis(), title, today, false))
        saveAll(c, list)
    }

    fun setDone(c: Context, id: Long, done: Boolean) {
        saveAll(c, loadAll(c).map { if (it.id == id) it.copy(done = done) else it })
    }

    fun deleteMission(c: Context, id: Long) {
        saveAll(c, loadAll(c).filter { it.id != id })
    }
}