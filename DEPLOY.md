# Deploy JARVIS Mark XXXIX

## Prerequisites

1. **Android Studio** — https://developer.android.com/studio  
2. **JDK 17** (bundled with Android Studio is fine)  
3. **Gemini API key** — https://aistudio.google.com/apikey  
4. Android phone (API 26+) or emulator  

---

## Method 1 — Android Studio (easiest)

1. Unzip / clone this repo  
2. Android Studio → **Open** → select this folder  
3. Wait for Gradle sync  
4. Connect phone (USB debugging ON) or start emulator  
5. Click **Run ▶️**  

---

## Method 2 — Command line (`./gradlew`)

```bash
# Make wrapper executable (Linux/macOS)
chmod +x ./gradlew

# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Or install via adb
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

**Windows:**
```bat
gradlew.bat assembleDebug
gradlew.bat installDebug
```

APK output:
```
app/build/outputs/apk/debug/app-debug.apk
```

Copy that file to your phone and open it to install (allow “Unknown sources” / “Install unknown apps”).

### SDK path (`local.properties`)

Android Studio creates this automatically. For CLI only:

```bash
cp local.properties.example local.properties
# Edit sdk.dir=... to your Android SDK path
```

---

## Method 3 — GitHub (upload + CI builds APK for you)

### 3a. Create repo and push

```bash
# On your machine, inside the project folder:
git init
git add .
git commit -m "JARVIS Mark XXXIX Android"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/JARVIS-Mark-XXXIX-Android.git
git push -u origin main
```

Or on GitHub.com:

1. **New repository** → name it e.g. `JARVIS-Mark-XXXIX-Android`  
2. Don’t add README if you already have one  
3. Upload the zip contents, or use the commands above  

### 3b. Automatic APK on every push (GitHub Actions)

This repo includes `.github/workflows/android.yml`.

1. Push to `main`  
2. Open **Actions** tab on GitHub  
3. Wait for **Android CI** → green check  
4. Click the run → **Artifacts** → download **jarvis-mark39-debug**  
5. Unzip the artifact → install the APK on your phone  

You can also run it manually: **Actions → Android CI → Run workflow**.

### 3c. GitHub Release (public download link)

```bash
git tag v1.0.0
git push origin v1.0.0
```

Workflow `.github/workflows/release.yml` builds an APK and attaches it to a Release.  
Share: `https://github.com/YOUR_USERNAME/JARVIS-Mark-XXXIX-Android/releases`

### 3d. Host APK on a simple webpage

1. Build APK (`./gradlew assembleDebug` or download from Actions)  
2. Upload APK to GitHub Releases / Google Drive / any host  
3. Link it from a page:

```html
<a href="https://github.com/YOU/JARVIS-Mark-XXXIX-Android/releases/latest/download/JARVIS-Mark-XXXIX-v1.0.0.apk">
  Download JARVIS for Android
</a>
```

Users still need to allow install from that source on the phone.

---

## Method 4 — Google Play Store

1. Create account: https://play.google.com/console ($25 one-time)  
2. Android Studio → **Build → Generate Signed App Bundle / APK** → **Android App Bundle**  
3. Or CLI (after signing config): `./gradlew bundleRelease`  
4. Upload AAB, fill store listing + **privacy policy** (required for mic/accessibility)  
5. Submit for review  

---

## Method 5 — Firebase App Distribution (testers)

1. Firebase console → App Distribution  
2. Upload APK  
3. Invite testers by email  

---

## First launch on phone

1. Open JARVIS → **Settings** → paste Gemini API key → Save  
2. Allow **Microphone**  
3. **Settings → Accessibility → JARVIS → ON** (for phone control)  
4. Optional: Camera, Overlay, Notifications  
5. Tap the cyan orb and speak  

---

## Useful Gradle commands

| Command | What it does |
|---------|----------------|
| `./gradlew assembleDebug` | Build debug APK |
| `./gradlew installDebug` | Build + install on device |
| `./gradlew assembleRelease` | Release APK (needs signing for install on some devices) |
| `./gradlew bundleRelease` | Play Store AAB |
| `./gradlew clean` | Clean build folders |
| `./gradlew tasks` | List all tasks |

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| `sdk.dir` missing | Open in Android Studio once, or set `local.properties` |
| `Permission denied: gradlew` | `chmod +x ./gradlew` |
| Gradle download fails | Check internet; Gradle 8.9 is downloaded on first run |
| Phone not listed | Enable USB debugging; accept RSA prompt |
| Install blocked | Settings → allow install from Files / browser |
| Gemini errors | Check API key + quota at aistudio.google.com |
