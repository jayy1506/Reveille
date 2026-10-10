import os
from dotenv import load_dotenv
from google import genai

load_dotenv()
key = os.getenv("GEMINI_API_KEY_1")
print("Key found:", bool(key), "| starts with:", key[:4] if key else None)

client = genai.Client(api_key=key)
r = client.models.generate_content(model="gemini-2.5-flash", contents="Say hello in 3 words")
print(r.text)