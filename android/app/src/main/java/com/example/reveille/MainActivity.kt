package com.example.reveille

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    private val refreshKey = mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val perms = mutableListOf(android.Manifest.permission.RECORD_AUDIO)
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            perms.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        requestPermissions(perms.toTypedArray(), 0)

        setContent { AlarmScreen(refreshKey.value) }
    }

    override fun onResume() {
        super.onResume()
        refreshKey.value++   // makes the screen reload missions each time you come back
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmScreen(refreshKey: Int) {
    val context = LocalContext.current
    val timeState = rememberTimePickerState(
        initialHour = AlarmStore.hour(context),
        initialMinute = AlarmStore.minute(context),
        is24Hour = false
    )
    var enabled by remember { mutableStateOf(AlarmStore.enabled(context)) }
    var savedHour by remember { mutableStateOf(AlarmStore.hour(context)) }
    var savedMinute by remember { mutableStateOf(AlarmStore.minute(context)) }
    val missions = remember(refreshKey) {
        mutableStateListOf<Mission>().also { it.addAll(MissionStore.today(context)) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("REVEILLE", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        TimePicker(state = timeState)
        Spacer(Modifier.height(16.dp))

        Button(onClick = {
            AlarmStore.save(context, timeState.hour, timeState.minute, true)
            AlarmScheduler.scheduleDaily(context, timeState.hour, timeState.minute)
            savedHour = timeState.hour
            savedMinute = timeState.minute
            enabled = true
        }) {
            Text(if (enabled) "Update wake-up time" else "Set wake-up alarm")
        }

        if (enabled) {
            Spacer(Modifier.height(8.dp))
            Text("Alarm set daily for %02d:%02d".format(savedHour, savedMinute))
            OutlinedButton(onClick = {
                AlarmScheduler.cancelDaily(context)
                AlarmStore.save(context, savedHour, savedMinute, false)
                enabled = false
            }) { Text("Cancel alarm") }
        }

        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = { AlarmScheduler.scheduleTest(context, 30) }) {
            Text("Test: ring in 30 seconds")
        }

        Spacer(Modifier.height(24.dp))
        Text("TODAY'S MISSIONS", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        if (missions.isEmpty()) {
            Text("No missions yet. They appear after you report to the sergeant.")
        } else {
            missions.forEachIndexed { index, m ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = m.done,
                        onCheckedChange = { checked ->
                            MissionStore.setDone(context, m.id, checked)
                            missions[index] = m.copy(done = checked)
                        }
                    )
                    Text(m.title)
                }
            }
        }
    }
}