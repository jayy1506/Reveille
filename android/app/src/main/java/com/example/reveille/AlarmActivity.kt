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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

class AlarmActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var player: MediaPlayer? = null
    private var tts: TextToSpeech? = null
    private var recognizer: SpeechRecognizer? = null
    private var ttsReady = false
    private val status = mutableStateOf("UP, SOLDIER!")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        tts = TextToSpeech(this, this)
        startAlarmSound()

        setContent {
            BackHandler(enabled = true) { }
            Column(
                modifier = Modifier.fillMaxSize().background(Color(0xFFB71C1C)),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    status.value,
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp)
                )
                Button(onClick = { onImUp() }) { Text("I'M UP") }
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
        status.value = text
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
    }

    // ---------- the conversation ----------
    private fun onImUp() {
        if (!ttsReady) return
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Microphone permission missing", Toast.LENGTH_LONG).show()
            return
        }
        player?.pause()
        say("What are your missions today? Report!", "ask")
    }

    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            say("No speech service on this phone.", "end")
            return
        }
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { status.value = "Listening..." }
            override fun onResults(results: Bundle?) {
                val heard = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull() ?: ""
                handleAnswer(heard)
            }
            override fun onError(error: Int) {
                say("I heard nothing, soldier! Try again!", "reject")
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        }
        recognizer?.startListening(intent)
    }

    private fun handleAnswer(heard: String) {
        status.value = "Sergeant is reviewing your report..."
        Thread {
            val result = JudgeClient.judge(heard)
            runOnUiThread {
                when {
                    result == null -> localCheck(heard)          // server down: fall back
                    result.valid -> say(result.reply, "end")
                    else -> say(result.reply, "reject")
                }
            }
        }.start()
    }

    private fun localCheck(heard: String) {
        val lower = heard.lowercase()
        val wordCount = lower.split(" ").count { it.isNotBlank() }
        val excuses = listOf("five more", "5 more", "snooze", "not now")
        val valid = wordCount >= 4 && excuses.none { lower.contains(it) }
        if (valid) {
            say("Server is down, but I'll accept that. Move out!", "end")
        } else {
            say("That is not a plan, soldier! Try again!", "reject")
        }
    }

    // ---------- alarm sound ----------
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
        status.value = "UP, SOLDIER! Tap I'M UP and report!"
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