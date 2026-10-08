package com.example.reveille

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("Reveille", "ALARM FIRED")
        Toast.makeText(context, "UP, SOLDIER!", Toast.LENGTH_LONG).show()
    }
}