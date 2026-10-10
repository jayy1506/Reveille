import os
from dotenv import load_dotenv
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from google import genai
from google.genai import types

load_dotenv()
MODEL = os.getenv("MODEL", "gemini-2.5-flash")

KEYS = [k for k in (os.getenv("GEMINI_API_KEY_1"), os.getenv("GEMINI_API_KEY_2")) if k]
print("Keys loaded:", len(KEYS))
clients = [
    genai.Client(api_key=k, http_options=types.HttpOptions(timeout=8000))  # 8 s per attempt
    for k in KEYS
]

app = FastAPI()

SYSTEM_PROMPT = """You are Sergeant Reveille, a tough, loud drill sergeant who wakes a college
student at dawn and demands their daily missions.

The student has just replied by voice (speech-to-text, so expect small transcription errors).
Decide whether they gave a real plan for the day.

Rules:
- valid = true only if they named at least one concrete task for today and sound coherent.
- valid = false for mumbling, excuses, snooze requests ("five more minutes"), or vague replies.
- Extract the concrete tasks as short items in "tasks" (empty list if invalid).
- "reply" is spoken aloud, so use 1 or 2 short, punchy sentences. If valid, acknowledge
  the missions and order them to move out. If invalid, scold them for the lack of effort
  and demand a real answer.
- Be harsh about effort and excuses only. Never insult family, body, appearance, or identity."""


class JudgeRequest(BaseModel):
    transcript: str


class JudgeResponse(BaseModel):
    valid: bool
    tasks: list[str]
    reply: str


def ask_llm(transcript: str) -> JudgeResponse | None:
    for i, client in enumerate(clients, start=1):
        try:
            response = client.models.generate_content(
                model=MODEL,
                contents=transcript,
                config=types.GenerateContentConfig(
                    system_instruction=SYSTEM_PROMPT,
                    response_mime_type="application/json",
                    response_schema=JudgeResponse,
                ),
            )
            if response.parsed is None:
                raise ValueError("unparseable response")
            print(f"Answered by key {i}")
            return response.parsed
        except Exception as e:
            print(f"Key {i} failed: {e}")
    return None


@app.post("/judge", response_model=JudgeResponse)
def judge(req: JudgeRequest):
    result = ask_llm(req.transcript)
    if result is None:
        raise HTTPException(status_code=503, detail="All LLM keys failed")
    return result
