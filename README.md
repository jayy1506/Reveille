# 🎖️ Reveille — Drill Sergeant Wake-Up & 1% Accountability App

**Reveille** is an Android accountability application built with **Jetpack Compose**, **Android Speech Recognition**, **Text-to-Speech (TTS)**, and an AI-powered **FastAPI backend (Gemini 2.5 Flash)**.

Designed for students, builders, and athletes striving for daily discipline, Reveille transforms morning alarms into mandatory report-for-duty sessions with **Sergeant Reveille**, a no-nonsense AI drill sergeant.

---

## ✨ Key Features

### 1. 🔔 Dawn Alarm & Passcode Verification Challenge
- **Wake-Up Verification**: When the morning alarm rings, the student is presented with a random military passcode word (`REVEILLE`, `ATTENTION`, `DISCIPLINE`, `VICTORY`, `SOLDIER`, `ACCOUNTABILITY`).
- The student **must type the exact matching passcode** to silence the alarm sound and prove they are fully awake before proceeding.

### 2. 🎙️ 2–3 Minute Speech-to-Text Reporting
- **Extended Voice Window**: Integrated `SpeechRecognizer` configured with extended listening timeouts (3-minute window) and live transcript accumulation on screen.
- **Automatic Pointwise Task Parser**: Uses an intelligent multi-mission parser (`SpeechParser.kt`) that automatically extracts individual tasks separated by conjunctions (`and`, `then`, `also`), numbers (`1.`, `2.`), or punctuation into distinct bullet-point checklist items.

### 3. 🤖 Sergeant Reveille AI Judge (Gemini 2.5 Flash)
- **Morning Evaluation (`/judge`)**: Analyzes spoken plans against student context (streak, yesterday's missed tasks, harshness level 1–5).
- **Voice Response via TTS**: Sergeant Reveille speaks brief, hard-hitting orders aloud to confirm the day's missions.
- **Care Override**: Automatically detects reports of illness, injury, or exhaustion and drops the harsh persona to provide recovery guidance.

### 4. 📋 Today's Missions & Gamified Rank Progression
- **Interactive Checklist**: Real-time progress bar with strike-through completions.
- **Rank Progression System**: Automatically updates rank based on streak & completed tasks:
  - 🎖️ `RECRUIT` (0–2 points)
  - 🛡️ `SOLDIER` (3–9 points)
  - ⚡ `COMMANDER` (10–24 points)
  - 👑 `TACTICAL GENERAL` (25+ points)
- **Quick-Add Missions**: Manual mission entry with live point-splitting and individual mission deletion (`✕`).

### 5. 📔 Single-Page 1% Daily Journal
Divides daily accountability into a **Morning Focus** and **Evening Reflection**:
- **☀️ Morning Focus**: Specific 1% Action, Micro-Gratitude (1 small thing), and Daily Mindset Affirmation.
- **🌙 Evening Reflection**: Daily Wins, 1% Growth Achievement, Small Pivot/Adjustment for Tomorrow, and Energy Check (`🪫 Low`, `⚡ Balanced`, `🔋 High`).
- **AI Evening Debrief (`/journal`)**: Generates an end-of-day review and automatically schedules tomorrow's 1% improvement task.

### 6. 💬 Daily Drill Motivation
- Features rotating daily accountability and discipline quotes on Screen 1 & Screen 2.

---

## 🎨 Design & Visual Theme
- **Vibrant Creative Light Tactical Theme**: High-contrast, readable tactical aesthetic using Crisp Tactical Off-White (`#F3F6EF`), Pure White Cards (`#FFFFFF`), Soft Olive Surfaces (`#E4ECE0`), Electric Signal Red (`#E63946`), Tactical Forest Green (`#2B5B3A`), and Military Gold (`#FFB800`).

---

## 🛠️ Tech Stack

### **Android Application** (`/android`)
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose, Material 3, Custom Canvas (`SergeantEmblem`)
- **Concurrency**: Kotlin Coroutines (`lifecycleScope`, `Dispatchers.IO`)
- **Speech & Audio**: Android `SpeechRecognizer`, `TextToSpeech` (TTS), `MediaPlayer`
- **Scheduling**: `AlarmManager` (Exact Alarms), `BroadcastReceiver` (`AlarmReceiver`, `BootReceiver`)
- **Storage**: `SharedPreferences` (`AlarmStore`, `MissionStore`, `JournalStore`)

### **Backend Server** (`/backend`)
- **Framework**: Python 3.11+, FastAPI, Uvicorn, Pydantic
- **AI Model**: Google Gemini 2.5 Flash via `google-genai` SDK
- **Environment Management**: `python-dotenv`

---

## 📂 Project Structure

```
Reveille/
├── android/                             # Android Application
│   └── app/
│       ├── build.gradle.kts             # Gradle build file (BuildConfig & BASE_URL)
│       └── src/main/java/com/example/reveille/
│           ├── MainActivity.kt          # 3-Screen Jetpack Compose Navigation & 1% Journal UI
│           ├── AlarmActivity.kt         # Passcode Match Challenge & Morning Voice Reporting
│           ├── SpeechParser.kt          # Speech-to-Pointwise Task Parser
│           ├── JudgeClient.kt           # HTTP Client for FastAPI /judge Endpoint
│           ├── JournalClient.kt         # HTTP Client for FastAPI /journal Endpoint
│           ├── AlarmStore.kt            # SharedPreferences for Alarm Settings
│           ├── MissionStore.kt          # Mission Persistence & Management
│           ├── JournalStore.kt          # Streak Counter & Submission Storage
│           ├── AlarmScheduler.kt        # AlarmManager Exact Alarm Scheduling
│           ├── AlarmReceiver.kt         # Full-Screen PendingIntent Broadcast Receiver
│           ├── BootReceiver.kt          # Reschedules Alarms on Device Reboot
│           ├── DailyQuotes.kt           # Rotating Motivational Quotes
│           └── ui/                      # Theme, Colors, Typography & Sergeant Emblem
│
└── backend/                             # FastAPI AI Backend
    ├── main.py                          # FastAPI Server with Gemini 2.5 Flash Integration
    ├── requirements.txt                 # Python Dependencies
    └── .env.example                     # Environment Variables Template
```

---

## 🚀 Getting Started

### 1. Backend Setup

1. Navigate to the `backend/` directory:
   ```bash
   cd backend
   ```
2. Create and activate a Python virtual environment:
   ```bash
   python -m venv venv
   # On Windows PowerShell:
   .\venv\Scripts\Activate.ps1
   # On macOS/Linux:
   source venv/bin/activate
   ```
3. Install required packages:
   ```bash
   pip install -r requirements.txt
   ```
4. Create a `.env` file in `backend/`:
   ```env
   GEMINI_API_KEY_1=your_gemini_api_key_here
   MODEL=gemini-2.5-flash
   ```
5. Start the FastAPI server:
   ```bash
   uvicorn main.py:app --host 0.0.0.0 --port 8000 --reload
   ```

---

### 2. Android App Setup

1. Open the `android/` directory in **Android Studio**.
2. **Network Routing Configuration**:
   - **When testing on Android Studio Emulator (AVD)**: The app uses `http://10.0.2.2:8000` (configured in `app/build.gradle.kts`).
   - **When testing on a Physical Android Device via USB**: Open the Terminal in Android Studio and run ADB port forwarding:
     ```bash
     adb reverse tcp:8000 tcp:8000
     ```
3. **Permissions & Settings on Device**:
   - Grant **Microphone** and **Notification** permissions when prompted.
   - On Android 12+ (API 31+), enable **Alarms & Reminders** in `Settings -> Apps -> Reveille -> Alarms & Reminders -> Allow`.
4. Build and run the app (`:app:assembleDebug`).

---

## 📡 API Reference

### `POST /judge`
Evaluates the morning speech report against student context.

**Request Body**:
```json
{
  "transcript": "Two LeetCode problems and go to gym then study Operating Systems",
  "name": "soldier",
  "streak": 5,
  "yesterday_done": ["Study DSA"],
  "yesterday_missed": [],
  "harshness": 3
}
```

**Response**:
```json
{
  "valid": true,
  "tasks": ["Two LeetCode problems", "Go to gym", "Study Operating Systems"],
  "reply": "Missions acknowledged. MOVE OUT, soldier!"
}
```

### `POST /journal`
Processes the single-page 1% Daily Journal report.

**Request Body**:
```json
{
  "missions": [{"title": "Two LeetCode problems", "done": true}],
  "morning_action": "Read 10 pages of OS book",
  "micro_gratitude": "Morning sunshine",
  "daily_affirmation": "Focus over distraction",
  "daily_wins": "Finished 2 LeetCode problems",
  "growth_1percent": "Better time complexity analysis",
  "small_pivot": "Sleep 30 minutes earlier",
  "energy_check": "⚡ Balanced"
}
```

**Response**:
```json
{
  "reply": "Solid progress today, recruit. Keep your momentum high.",
  "tomorrow_mission": "Sleep 30 minutes earlier and tackle OS reading"
}
```

---

## 📜 License
This project is open-source under the MIT License.
