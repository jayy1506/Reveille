package com.example.reveille

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.reveille.ui.SergeantEmblem
import com.example.reveille.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

enum class AlarmStep {
    VERIFY_CODE,
    RECORDING,
    REVIEWING
}

class AlarmActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var player: MediaPlayer? = null
    private var tts: TextToSpeech? = null
    private var recognizer: SpeechRecognizer? = null
    private var ttsReady = false

    private val targetCode = mutableStateOf("REVEILLE")
    private val currentStep = mutableStateOf(AlarmStep.VERIFY_CODE)
    private val statusMessage = mutableStateOf("WAKE UP, SOLDIER! TYPE CODE TO SILENCE ALARM!")
    private val liveTranscript = mutableStateOf("")

    private val passcodeWords = listOf("REVEILLE", "ATTENTION", "DISCIPLINE", "VICTORY", "SOLDIER", "ACCOUNTABILITY")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        targetCode.value = passcodeWords.random()
        tts = TextToSpeech(this, this)
        startAlarmSound()

        setContent {
            ReveilleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = TacticalLightBg
                ) {
                    BackHandler(enabled = true) { }
                    AlarmContent(
                        targetCode = targetCode.value,
                        step = currentStep.value,
                        statusMessage = statusMessage.value,
                        liveTranscript = liveTranscript.value,
                        onVerifySuccess = {
                            player?.pause()
                            currentStep.value = AlarmStep.RECORDING
                            say("What are your missions today? Report!", "ask")
                        },
                        onSubmitSpeech = {
                            if (liveTranscript.value.isNotBlank()) {
                                recognizer?.stopListening()
                                handleAnswer(liveTranscript.value)
                            }
                        }
                    )
                }
            }
        }
    }

    // ---------- TTS setup ----------
    override fun onInit(initStatus: Int) {
        if (initStatus != TextToSpeech.SUCCESS) return
        tts?.language = Locale.US
        tts?.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                Log.e("Reveille", "TTS error for utteranceId: $utteranceId")
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                Log.e("Reveille", "TTS error code: $errorCode")
            }

            override fun onDone(utteranceId: String?) {
                runOnUiThread {
                    when (utteranceId) {
                        "ask" -> startListening()
                        "reject" -> resumeAlarm()
                        "end" -> finishAlarm()
                    }
                }
            }
        })
        ttsReady = true
    }

    private fun say(text: String, id: String) {
        statusMessage.value = text
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
    }

    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            say("No speech service on this phone.", "end")
            return
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Microphone permission missing", Toast.LENGTH_LONG).show()
            return
        }

        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                statusMessage.value = "RECORDING REPORT... SPEAK ALL YOUR MISSIONS!"
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: liveTranscript.value
                liveTranscript.value = text
                handleAnswer(text)
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()
                if (!text.isNullOrBlank()) {
                    liveTranscript.value = text
                }
            }
            override fun onError(error: Int) {
                if (liveTranscript.value.isNotBlank()) {
                    handleAnswer(liveTranscript.value)
                } else {
                    say("I heard nothing, soldier! Speak up!", "reject")
                }
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            // Allow up to 3 minutes window (180,000 ms) and 10s silence tolerance
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 180000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 10000L)
        }
        recognizer?.startListening(intent)
    }

    private fun handleAnswer(heard: String) {
        currentStep.value = AlarmStep.REVIEWING
        statusMessage.value = "Sergeant is reviewing your report..."
        val currentStreak = JournalStore.streak(this)

        lifecycleScope.launch(Dispatchers.IO) {
            val result = JudgeClient.judge(
                transcript = heard,
                streak = currentStreak
            )
            withContext(Dispatchers.Main) {
                when {
                    result == null -> localCheck(heard)
                    result.valid -> {
                        val parsedFromHeard = SpeechParser.parseSpeechToMissions(heard)
                        val parsedFromLlm = result.tasks.flatMap { SpeechParser.parseSpeechToMissions(it) }

                        val points = (parsedFromLlm + parsedFromHeard)
                            .filter { it.isNotBlank() }
                            .distinct()

                        MissionStore.replaceToday(this@AlarmActivity, points)
                        say(result.reply, "end")
                    }
                    else -> say(result.reply, "reject")
                }
            }
        }
    }

    private fun localCheck(heard: String) {
        val lower = heard.lowercase()
        val wordCount = lower.split(" ").count { it.isNotBlank() }
        val excuses = listOf("five more", "5 more", "snooze", "not now")
        val valid = wordCount >= 3 && excuses.none { lower.contains(it) }
        if (valid) {
            val points = SpeechParser.parseSpeechToMissions(heard)
            MissionStore.replaceToday(this@AlarmActivity, points)
            val pointsText = points.joinToString(", ")
            say("Missions recorded: $pointsText. Move out!", "end")
        } else {
            say("That is not a plan, soldier! Speak up!", "reject")
        }
    }

    private fun startAlarmSound() {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            setDataSource(this@AlarmActivity, uri)
            isLooping = true
            prepare()
            start()
        }
    }

    private fun resumeAlarm() {
        statusMessage.value = "UP, SOLDIER! Report your concrete missions!"
        player?.start()
    }

    private fun finishAlarm() {
        player?.stop()
        player?.release()
        player = null
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).cancel(1)
        finish()
    }

    override fun onDestroy() {
        player?.release()
        recognizer?.destroy()
        tts?.shutdown()
        super.onDestroy()
    }
}

@Composable
fun AlarmContent(
    targetCode: String,
    step: AlarmStep,
    statusMessage: String,
    liveTranscript: String,
    onVerifySuccess: () -> Unit,
    onSubmitSpeech: () -> Unit
) {
    var typedCode by remember { mutableStateOf("") }
    var codeError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(16.dp))
            SergeantEmblem(size = 90.dp)
            Spacer(Modifier.height(16.dp))

            Text(
                text = "SERGEANT REVEILLE",
                style = MaterialTheme.typography.displayLarge,
                color = TextDarkTactical
            )
            Text(
                text = "MORNING ACCOUNTABILITY ALARM",
                style = MaterialTheme.typography.labelLarge,
                color = SignalRedVibrant
            )

            Spacer(Modifier.height(20.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.5.dp, SignalRedVibrant, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = TacticalCardBg)
            ) {
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.titleLarge,
                    color = SignalRedVibrant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(18.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            when (step) {
                AlarmStep.VERIFY_CODE -> {
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
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "STEP 1: WAKE-UP CODE MATCH",
                                style = MaterialTheme.typography.labelLarge,
                                color = TacticalForestGreen
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "TYPE THIS CODE TO SILENCE ALARM:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMutedTactical
                            )
                            Spacer(Modifier.height(10.dp))

                            // Target Passcode Display
                            Surface(
                                color = SignalRedVibrant,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = targetCode,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TacticalCardBg,
                                    letterSpacing = 4.sp,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
                                )
                            }

                            Spacer(Modifier.height(16.dp))

                            OutlinedTextField(
                                value = typedCode,
                                onValueChange = {
                                    typedCode = it.uppercase()
                                    codeError = false
                                },
                                label = { Text("Enter Passcode", color = TextDarkTactical) },
                                isError = codeError,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = TacticalCardBg,
                                    unfocusedContainerColor = TacticalCardBg,
                                    focusedBorderColor = SignalRedVibrant,
                                    unfocusedBorderColor = OliveBorderCreative,
                                    focusedTextColor = TextDarkTactical,
                                    unfocusedTextColor = TextDarkTactical
                                )
                            )

                            if (codeError) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "INCORRECT CODE! TYPE EXACT CODE TO SYNC!",
                                    color = SignalRedVibrant,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }

                            Spacer(Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    if (typedCode.trim() == targetCode) {
                                        onVerifySuccess()
                                    } else {
                                        codeError = true
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SignalRedVibrant)
                            ) {
                                Text(
                                    text = "VERIFY CODE & SILENCE ALARM",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TacticalCardBg
                                )
                            }
                        }
                    }
                }

                AlarmStep.RECORDING, AlarmStep.REVIEWING -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, OliveBorderCreative, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = TacticalCardBg)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Text(
                                text = "STEP 2: VOICE REPORTING (2-3 MIN WINDOW)",
                                style = MaterialTheme.typography.labelLarge,
                                color = TacticalForestGreen
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Speak all your missions for today clearly. They will be formatted into point tasks:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMutedTactical
                            )
                            Spacer(Modifier.height(14.dp))

                            // Live Transcript Accumulator Box
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 120.dp)
                                    .border(1.dp, OliveBorderCreative, RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = TacticalOliveCard)
                            ) {
                                Text(
                                    text = liveTranscript.ifBlank { "Listening for your missions..." },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (liveTranscript.isBlank()) TextMutedTactical else TextDarkTactical,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }

                            Spacer(Modifier.height(20.dp))

                            Button(
                                onClick = onSubmitSpeech,
                                enabled = step == AlarmStep.RECORDING && liveTranscript.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SignalRedVibrant)
                            ) {
                                Text(
                                    text = "SUBMIT MISSION REPORT",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TacticalCardBg
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}