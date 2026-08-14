# Push to GitHub & get APK (no Android Studio / no device needed)

GitHub builds the APK in the cloud. Your phone does **not** need to be connected to a PC.

## App requirements on the phone

- **Android 8.0 (API 26) or newer**
- If your phone is Android 7 or older, the APK will refuse to install (“not supported”)

---

## Method A — Upload with browser (no git)

1. Go to https://github.com/new  
2. Repository name: `JARVIS-Mark-XXXIX-Android`  
3. Public → **Create repository**  
4. On the empty repo page, click **uploading an existing file**  
5. Unzip `JARVIS-Mark-XXXIX-Android.zip` on your computer  
6. Drag **all files inside the folder** (including `.github`, `app`, `gradlew`, etc.) onto the upload page  
7. Commit message: `Initial commit` → **Commit changes**  

### Get the APK

1. Open the **Actions** tab  
2. Click the latest **Android CI** run (wait until it’s green ✅ — first run can take 5–15 min)  
3. Scroll to **Artifacts** → download **jarvis-mark39-debug**  
4. Unzip that download → you get `app-debug.apk`  
5. Copy APK to your phone (Drive, Telegram, USB, cable)  
6. Open APK on phone → Install (allow unknown apps if asked)

---

## Method B — Git command line

```bash
cd JARVIS-Mark-XXXIX-Android
git init
git add .
git commit -m "JARVIS Mark XXXIX Android"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/JARVIS-Mark-XXXIX-Android.git
git push -u origin main
```

Then same: **Actions** → download artifact.

### Public download link (optional)

```bash
git tag v1.0.0
git push origin v1.0.0
```

Opens a Release with the APK attached under **Releases**.

---

## After install on phone

1. Open **JARVIS** → Settings → paste Gemini API key from https://aistudio.google.com/apikey  
2. Allow Microphone  
3. Settings → Accessibility → enable JARVIS (for phone control)  
4. Tap the orb  

---

## If Actions fails (red X)

1. Open the failed job → expand **Build debug APK**  
2. Copy the error  
3. Common fixes already in the workflow: JDK 17, SDK 35, licenses  
4. Re-run: Actions → **Android CI** → **Run workflow**

## If phone says app not supported

Your Android version is below 8.0. Need a newer phone or emulator.
