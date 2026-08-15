# JARVIS Mark XXXIX — Android

Voice-first AI assistant for Android: **Gemini function calling**, full phone control (Accessibility), CameraX + screen vision, floating bubble, multimodal files.

[![Android CI](https://github.com/YOUR_USERNAME/JARVIS-Mark-XXXIX-Android/actions/workflows/android.yml/badge.svg)](https://github.com/YOUR_USERNAME/JARVIS-Mark-XXXIX-Android/actions)

> Replace `YOUR_USERNAME` in the badge URL after you push to GitHub.

---

## Quick start

### 1. Install Android Studio
https://developer.android.com/studio

### 2. Open this project
Unzip / clone → **Open** the folder in Android Studio → wait for Gradle sync.

### 3. Build APK (CLI)

```bash
chmod +x ./gradlew
./gradlew assembleDebug
# APK → app/build/outputs/apk/debug/app-debug.apk
```

Install on phone:

```bash
./gradlew installDebug
# or
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 4. First run
- Settings → paste **Gemini API key** (https://aistudio.google.com/apikey)  
- Allow Microphone  
- Accessibility → enable **JARVIS** for phone control  
- Tap the orb and talk  

**Full deploy guide:** [DEPLOY.md](DEPLOY.md)

---

## GitHub setup (CI builds APK for you)

```bash
git init
git add .
git commit -m "Initial commit: JARVIS Mark XXXIX"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/JARVIS-Mark-XXXIX-Android.git
git push -u origin main
```

Then:

1. **Actions** tab → workflow **Android CI** runs automatically  
2. Download **jarvis-mark39-debug** artifact (the APK)  
3. Tag a release for public download links:

```bash
git tag v1.0.0
git push origin v1.0.0
```

Included workflows:

| File | Purpose |
|------|---------|
| `.github/workflows/android.yml` | Build debug APK on push/PR |
| `.github/workflows/release.yml` | Attach APK to GitHub Release on `v*` tags |

---

## Features

- Voice orb + **local command router** + Gemini tools  
- **Phone control**: Home/Back, open apps, click text, type, scroll, swipe, read screen, call/SMS/maps, volume, lock, screenshot  
- Multimodal files (images, text, PDF pages, video frame)  
- CameraX + MediaProjection screen vision  
- Floating overlay bubble  
- Task agent (ReAct)  
- Share target from other apps  

### Example voice commands

`go home` · `open Chrome` · `click Login` · `scroll down` · `read screen` · `call 5551234` · `volume up` · `search latest news` · `remember I like dark mode`

---

## Project structure

```
./gradlew                 # Build tool (use this)
app/                      # Android application module
.github/workflows/        # CI + Release
DEPLOY.md                 # All deploy methods
```

---

## License / credits

Architecture based on JARVIS Mark XXXIX blueprint. Not affiliated with Marvel.
