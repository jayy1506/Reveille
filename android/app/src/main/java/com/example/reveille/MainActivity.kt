package com.example.reveille

import android.app.AlarmManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.reveille.ui.SergeantEmblem
import com.example.reveille.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class Screen {
    ENTRY,
    MISSIONS,
    JOURNAL
}

class MainActivity : ComponentActivity() {
    private val currentScreen = mutableStateOf(Screen.ENTRY)
    private val refreshKey = mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val perms = mutableListOf(android.Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 33) {
            perms.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        requestPermissions(perms.toTypedArray(), 0)

        checkExactAlarmPermission()

        setContent {
            ReveilleTheme {
                ReveilleApp(
                    screenState = currentScreen,
                    refreshKey = refreshKey.value
                )
            }
        }
    }

    private fun checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                try {
                    startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                } catch (_: Exception) { }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshKey.value++
    }
}

@Composable
fun ReveilleApp(
    screenState: MutableState<Screen>,
    refreshKey: Int
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = TacticalLightBg
    ) {
        Crossfade(targetState = screenState.value, label = "screen_transition") { screen ->
            when (screen) {
                Screen.ENTRY -> EntryScreen(
                    onReportForDuty = { screenState.value = Screen.MISSIONS }
                )
                Screen.MISSIONS -> MissionsScreen(
                    refreshKey = refreshKey,
                    onOpenJournal = { screenState.value = Screen.JOURNAL },
                    onBack = { screenState.value = Screen.ENTRY }
                )
                Screen.JOURNAL -> NightJournalScreen(
                    onBack = { screenState.value = Screen.MISSIONS },
                    onSubmitComplete = { screenState.value = Screen.MISSIONS }
                )
            }
        }
    }
}

private fun getRankTitle(streak: Int, completedMissions: Int): String {
    val totalScore = streak * 2 + completedMissions
    return when {
        totalScore >= 25 -> "TACTICAL GENERAL 👑"
        totalScore >= 10 -> "COMMANDER ⚡"
        totalScore >= 3 -> "SOLDIER 🛡️"
        else -> "RECRUIT 🎖️"
    }
}

@Composable
fun EntryScreen(onReportForDuty: () -> Unit) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(AlarmStore.enabled(context)) }
    var hour by remember { mutableStateOf(AlarmStore.hour(context)) }
    var minute by remember { mutableStateOf(AlarmStore.minute(context)) }
    var showVoiceTestDialog by remember { mutableStateOf(false) }

    val streak = remember { JournalStore.streak(context) }
    val todayDone = remember { MissionStore.today(context).count { it.done } }
    val rankTitle = remember(streak, todayDone) { getRankTitle(streak, todayDone) }

    val formattedTime = remember(hour, minute) {
        val h = if (hour == 0 || hour == 12) 12 else hour % 12
        val amPm = if (hour < 12) "AM" else "PM"
        "%02d:%02d %s".format(h, minute, amPm)
    }

    val timePickerDialog = TimePickerDialog(
        context,
        { _, selectedHour, selectedMinute ->
            hour = selectedHour
            minute = selectedMinute
            AlarmStore.save(context, hour, minute, enabled)
            if (enabled) {
                AlarmScheduler.scheduleDaily(context, hour, minute)
            }
        },
        hour,
        minute,
        false
    )

    if (showVoiceTestDialog) {
        VoiceTestDialog(
            onDismiss = { showVoiceTestDialog = false },
            onAddMissions = { newPoints ->
                newPoints.forEach { MissionStore.addMissionForToday(context, it) }
                showVoiceTestDialog = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(Modifier.height(12.dp))
            
            // Rank Badge Chip
            Surface(
                color = GoldRankAccent.copy(alpha = 0.2f),
                shape = CircleShape,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldRankAccent)
            ) {
                Text(
                    text = "RANK: $rankTitle",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextDarkTactical,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "REVEILLE",
                style = MaterialTheme.typography.displayLarge,
                color = TextDarkTactical
            )
            Text(
                text = "DRILL SERGEANT ACCOUNTABILITY",
                style = MaterialTheme.typography.labelLarge,
                color = SignalRedVibrant
            )
            Spacer(Modifier.height(16.dp))

            SergeantEmblem(size = 90.dp)

            Spacer(Modifier.height(16.dp))

            // Daily Motivational Quote Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, OliveBorderCreative, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = TacticalOliveCard)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Text(
                        text = "💬 DAILY DRILL MOTIVATION",
                        style = MaterialTheme.typography.labelLarge,
                        color = TacticalForestGreen
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "\"${DailyQuotes.todayQuote()}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextDarkTactical,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Wake-up Time Display Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.5.dp, OliveBorderCreative, RoundedCornerShape(20.dp))
                    .clickable { timePickerDialog.show() },
                colors = CardDefaults.cardColors(containerColor = TacticalCardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "WAKE-UP ALARM TIME",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextMutedTactical
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = formattedTime,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Black,
                        color = TextDarkTactical
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Tap to change wake time",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TacticalForestGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Alarm Toggle Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, OliveBorderCreative, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = TacticalOliveCard)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "DAILY ALARM STATUS",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextDarkTactical
                        )
                        Text(
                            text = if (enabled) "Scheduled for $formattedTime" else "Alarm is disarmed",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (enabled) TacticalForestGreen else TextMutedTactical,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Switch(
                        checked = enabled,
                        onCheckedChange = { isChecked ->
                            enabled = isChecked
                            AlarmStore.save(context, hour, minute, isChecked)
                            if (isChecked) {
                                AlarmScheduler.scheduleDaily(context, hour, minute)
                            } else {
                                AlarmScheduler.cancelDaily(context)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TacticalCardBg,
                            checkedTrackColor = SignalRedVibrant,
                            uncheckedThumbColor = TextMutedTactical,
                            uncheckedTrackColor = TacticalLightBg
                        )
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Big Red REPORT FOR DUTY button
            Button(
                onClick = onReportForDuty,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SignalRedVibrant)
            ) {
                Text(
                    text = "REPORT FOR DUTY",
                    style = MaterialTheme.typography.titleLarge,
                    color = TacticalCardBg
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showVoiceTestDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextDarkTactical)
                ) {
                    Text("SPEECH REPORT")
                }

                OutlinedButton(
                    onClick = { AlarmScheduler.scheduleTest(context, 30) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextDarkTactical)
                ) {
                    Text("TEST ALARM (30s)")
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
fun MissionsScreen(
    refreshKey: Int,
    onOpenJournal: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var triggerReload by remember { mutableIntStateOf(0) }
    val missions = remember(refreshKey, triggerReload) {
        mutableStateListOf<Mission>().also { it.addAll(MissionStore.today(context)) }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var showIncompleteWarning by remember { mutableStateOf(false) }

    val todayFormatted = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.US)).uppercase()
    }

    val doneCount = missions.count { it.done }
    val totalCount = missions.size
    val progress = if (totalCount > 0) doneCount.toFloat() / totalCount else 0f

    BackHandler(onBack = onBack)

    if (showAddDialog) {
        AddMissionDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { newPoints ->
                newPoints.forEach { MissionStore.addMissionForToday(context, it) }
                triggerReload++
                showAddDialog = false
            }
        )
    }

    if (showIncompleteWarning) {
        AlertDialog(
            onDismissRequest = { showIncompleteWarning = false },
            title = {
                Text("UNFINISHED MISSIONS DETECTED", style = MaterialTheme.typography.titleLarge, color = SignalRedVibrant)
            },
            text = {
                Column {
                    Text(
                        "You have ${totalCount - doneCount} incomplete mission(s) today. Sergeant Reveille expects 100% effort!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextDarkTactical
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Complete your remaining missions before ending the day, or proceed to submit your debrief.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMutedTactical
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showIncompleteWarning = false
                        onOpenJournal()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SignalRedVibrant)
                ) {
                    Text("PROCEED TO JOURNAL")
                }
            },
            dismissButton = {
                TextButton(onClick = { showIncompleteWarning = false }) {
                    Text("COMPLETE MISSIONS FIRST", color = TacticalForestGreen, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = TacticalCardBg
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        // Top Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Text("←", fontSize = 24.sp, color = TextDarkTactical, fontWeight = FontWeight.Bold)
            }
            Text(
                text = "TODAY'S MISSIONS",
                style = MaterialTheme.typography.titleLarge,
                color = TextDarkTactical
            )
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = TacticalForestGreen),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("+ ADD", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(10.dp))

        // Daily Motivational Quote Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, OliveBorderCreative, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = TacticalOliveCard)
        ) {
            Text(
                text = "💬 \"${DailyQuotes.todayQuote()}\"",
                style = MaterialTheme.typography.bodyMedium,
                color = TextDarkTactical,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(12.dp)
            )
        }

        Spacer(Modifier.height(12.dp))

        // Progress Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, OliveBorderCreative, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = TacticalCardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = todayFormatted,
                    style = MaterialTheme.typography.labelLarge,
                    color = TacticalForestGreen
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$doneCount OF $totalCount COMPLETED",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextDarkTactical
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (progress == 1.0f) SuccessEmerald else SignalRedVibrant
                    )
                }
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = if (progress == 1.0f) SuccessEmerald else SignalRedVibrant,
                    trackColor = TacticalOliveCard
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Mission List
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            if (missions.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, OliveBorderCreative, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = TacticalOliveCard)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "NO MISSIONS RECORDED YET",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextDarkTactical
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Tap '+ ADD' above or report your missions to Sergeant Reveille during your morning alarm.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMutedTactical,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                missions.forEachIndexed { index, m ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                1.dp,
                                if (m.done) SuccessEmerald else OliveBorderCreative,
                                RoundedCornerShape(12.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (m.done) SuccessEmeraldBg else TacticalCardBg
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = m.done,
                                    onCheckedChange = { checked ->
                                        MissionStore.setDone(context, m.id, checked)
                                        missions[index] = m.copy(done = checked)
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = SuccessEmerald,
                                        uncheckedColor = TextDarkTactical,
                                        checkmarkColor = TacticalCardBg
                                    )
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = m.title,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            textDecoration = if (m.done) TextDecoration.LineThrough else TextDecoration.None
                                        ),
                                        color = if (m.done) SuccessEmerald else TextDarkTactical
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    MissionStore.deleteMission(context, m.id)
                                    missions.removeAt(index)
                                }
                            ) {
                                Text("✕", color = SignalRedVibrant, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Open Night Journal Button
        Button(
            onClick = {
                if (totalCount > 0 && doneCount < totalCount) {
                    showIncompleteWarning = true
                } else {
                    onOpenJournal()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SignalRedVibrant)
        ) {
            Text(
                text = "NIGHT JOURNAL (1% METHOD)",
                style = MaterialTheme.typography.titleLarge,
                color = TacticalCardBg
            )
        }
    }
}

@Composable
fun NightJournalScreen(
    onBack: () -> Unit,
    onSubmitComplete: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Morning Focus Fields
    var morningAction by remember { mutableStateOf("") }
    var microGratitude by remember { mutableStateOf("") }
    var dailyAffirmation by remember { mutableStateOf("") }

    // Evening Reflection Fields
    var dailyWins by remember { mutableStateOf("") }
    var growth1Percent by remember { mutableStateOf("") }
    var smallPivot by remember { mutableStateOf("") }
    var energyCheck by remember { mutableStateOf("⚡ Balanced") }

    var isSubmitting by remember { mutableStateOf(false) }
    var streakCount by remember { mutableStateOf(JournalStore.streak(context)) }
    var feedbackReply by remember { mutableStateOf<String?>(null) }
    var tomorrowMissionAssigned by remember { mutableStateOf<String?>(null) }

    BackHandler(onBack = onBack)

    if (feedbackReply != null) {
        AlertDialog(
            onDismissRequest = {
                feedbackReply = null
                onSubmitComplete()
            },
            title = {
                Text(
                    text = "SERGEANT'S DEBRIEF",
                    style = MaterialTheme.typography.titleLarge,
                    color = SignalRedVibrant
                )
            },
            text = {
                Column {
                    Text(
                        text = feedbackReply ?: "",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextDarkTactical
                    )
                    if (!tomorrowMissionAssigned.isNullOrBlank()) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "ASSIGNED FOR TOMORROW:",
                            style = MaterialTheme.typography.labelLarge,
                            color = TacticalForestGreen
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "• $tomorrowMissionAssigned",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextDarkTactical,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        feedbackReply = null
                        onSubmitComplete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SignalRedVibrant)
                ) {
                    Text("DISMISSED, SERGEANT")
                }
            },
            containerColor = TacticalCardBg
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Text("←", fontSize = 24.sp, color = TextDarkTactical, fontWeight = FontWeight.Bold)
            }
            Text(
                text = "1% DAILY JOURNAL",
                style = MaterialTheme.typography.titleLarge,
                color = TextDarkTactical
            )
            Spacer(Modifier.width(48.dp))
        }

        Spacer(Modifier.height(12.dp))

        // Streak Banner Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, OliveBorderCreative, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = TacticalOliveCard)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "🔥  ACCOUNTABILITY STREAK: $streakCount DAYS",
                    style = MaterialTheme.typography.titleLarge,
                    color = TacticalForestGreen
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // --- SECTION 1: MORNING FOCUS ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, OliveBorderCreative, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = TacticalCardBg)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "☀️ MORNING FOCUS",
                    style = MaterialTheme.typography.titleLarge,
                    color = TacticalForestGreen
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = morningAction,
                    onValueChange = { morningAction = it },
                    label = { Text("Specific 1% Action for Today", color = TextDarkTactical) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = TacticalLightBg,
                        unfocusedContainerColor = TacticalLightBg,
                        focusedBorderColor = SignalRedVibrant,
                        unfocusedBorderColor = OliveBorderCreative,
                        focusedTextColor = TextDarkTactical,
                        unfocusedTextColor = TextDarkTactical
                    )
                )

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = microGratitude,
                    onValueChange = { microGratitude = it },
                    label = { Text("Micro-Gratitude (1 Small Thing)", color = TextDarkTactical) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = TacticalLightBg,
                        unfocusedContainerColor = TacticalLightBg,
                        focusedBorderColor = SignalRedVibrant,
                        unfocusedBorderColor = OliveBorderCreative,
                        focusedTextColor = TextDarkTactical,
                        unfocusedTextColor = TextDarkTactical
                    )
                )

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = dailyAffirmation,
                    onValueChange = { dailyAffirmation = it },
                    label = { Text("Daily Mindset Affirmation", color = TextDarkTactical) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = TacticalLightBg,
                        unfocusedContainerColor = TacticalLightBg,
                        focusedBorderColor = SignalRedVibrant,
                        unfocusedBorderColor = OliveBorderCreative,
                        focusedTextColor = TextDarkTactical,
                        unfocusedTextColor = TextDarkTactical
                    )
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // --- SECTION 2: EVENING REFLECTION ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, OliveBorderCreative, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = TacticalCardBg)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🌙 EVENING REFLECTION",
                    style = MaterialTheme.typography.titleLarge,
                    color = SignalRedVibrant
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = dailyWins,
                    onValueChange = { dailyWins = it },
                    label = { Text("Daily Wins Today", color = TextDarkTactical) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = TacticalLightBg,
                        unfocusedContainerColor = TacticalLightBg,
                        focusedBorderColor = SignalRedVibrant,
                        unfocusedBorderColor = OliveBorderCreative,
                        focusedTextColor = TextDarkTactical,
                        unfocusedTextColor = TextDarkTactical
                    )
                )

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = growth1Percent,
                    onValueChange = { growth1Percent = it },
                    label = { Text("1% Growth Achievement", color = TextDarkTactical) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = TacticalLightBg,
                        unfocusedContainerColor = TacticalLightBg,
                        focusedBorderColor = SignalRedVibrant,
                        unfocusedBorderColor = OliveBorderCreative,
                        focusedTextColor = TextDarkTactical,
                        unfocusedTextColor = TextDarkTactical
                    )
                )

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = smallPivot,
                    onValueChange = { smallPivot = it },
                    label = { Text("Small Pivot / Adjustment for Tomorrow", color = TextDarkTactical) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = TacticalLightBg,
                        unfocusedContainerColor = TacticalLightBg,
                        focusedBorderColor = SignalRedVibrant,
                        unfocusedBorderColor = OliveBorderCreative,
                        focusedTextColor = TextDarkTactical,
                        unfocusedTextColor = TextDarkTactical
                    )
                )

                Spacer(Modifier.height(14.dp))

                Text("Brief Energy Check:", style = MaterialTheme.typography.labelLarge, color = TextDarkTactical)
                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("🪫 Low", "⚡ Balanced", "🔋 High").forEach { level ->
                        FilterChip(
                            selected = energyCheck == level,
                            onClick = { energyCheck = level },
                            label = { Text(level, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SignalRedVibrant,
                                selectedLabelColor = TacticalCardBg
                            )
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Submit Button
        Button(
            onClick = {
                isSubmitting = true
                JournalStore.recordSubmission(context)
                streakCount = JournalStore.streak(context)

                val todayMissions = MissionStore.today(context)

                coroutineScope.launch(Dispatchers.IO) {
                    val result = JournalClient.submit(
                        missions = todayMissions,
                        better = growth1Percent.ifBlank { dailyWins },
                        obstacle = smallPivot,
                        tomorrow = morningAction,
                        morningAction = morningAction,
                        microGratitude = microGratitude,
                        dailyAffirmation = dailyAffirmation,
                        dailyWins = dailyWins,
                        growth1Percent = growth1Percent,
                        smallPivot = smallPivot,
                        energyCheck = energyCheck
                    )

                    val reply = result?.reply
                        ?: "Good debrief, soldier. Consistency is victory. Keep pushing every single day."

                    val parsedTomorrowTasks = SpeechParser.parseSpeechToMissions(
                        result?.tomorrowMission ?: smallPivot.ifBlank { morningAction.ifBlank { "Execute 1% improvement task" } }
                    )
                    val nextMission = parsedTomorrowTasks.firstOrNull() ?: "Execute 1% improvement task"

                    MissionStore.addTomorrowMission(context, nextMission)

                    withContext(Dispatchers.Main) {
                        isSubmitting = false
                        feedbackReply = reply
                        tomorrowMissionAssigned = nextMission
                    }
                }
            },
            enabled = !isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SignalRedVibrant)
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(color = TacticalCardBg, modifier = Modifier.size(24.dp))
            } else {
                Text(
                    text = "SUBMIT 1% DAILY JOURNAL",
                    style = MaterialTheme.typography.titleLarge,
                    color = TacticalCardBg
                )
            }
        }
    }
}

@Composable
fun AddMissionDialog(
    onDismiss: () -> Unit,
    onAdd: (List<String>) -> Unit
) {
    var textInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("ADD MISSIONS", style = MaterialTheme.typography.titleLarge, color = TextDarkTactical)
        },
        text = {
            Column {
                Text(
                    "Enter your tasks or plan. Conjunctions (and, then, commas) will automatically split into bullet point missions!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMutedTactical
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("e.g., 2 LeetCode problems and go to gym then study OS") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = SpeechParser.parseSpeechToMissions(textInput)
                    if (parsed.isNotEmpty()) {
                        onAdd(parsed)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SignalRedVibrant)
            ) {
                Text("ADD MISSIONS")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = TextMutedTactical)
            }
        },
        containerColor = TacticalCardBg
    )
}

@Composable
fun VoiceTestDialog(
    onDismiss: () -> Unit,
    onAddMissions: (List<String>) -> Unit
) {
    var sampleSpeech by remember { mutableStateOf("I will solve two LeetCode problems, go to the gym, and finish project") }
    val parsedPoints = remember(sampleSpeech) { SpeechParser.parseSpeechToMissions(sampleSpeech) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("SPEECH REPORT PARSER", style = MaterialTheme.typography.titleLarge, color = TextDarkTactical)
        },
        text = {
            Column {
                Text(
                    "Test how your spoken report is recorded into distinct point items:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMutedTactical
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = sampleSpeech,
                    onValueChange = { sampleSpeech = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                Spacer(Modifier.height(14.dp))
                Text("PARSED POINT ITEMS:", style = MaterialTheme.typography.labelLarge, color = TacticalForestGreen)
                Spacer(Modifier.height(6.dp))
                parsedPoints.forEach { point ->
                    Text("• $point", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAddMissions(parsedPoints)
                },
                colors = ButtonDefaults.buttonColors(containerColor = SignalRedVibrant)
            ) {
                Text("SAVE THESE POINTS")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = TextMutedTactical)
            }
        },
        containerColor = TacticalCardBg
    )
}