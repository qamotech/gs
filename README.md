# GEMSTORM 💎 — Neon Abyss

## Building from this repo (qamotech/gs)

Binary assets (WAV audio, the Gradle wrapper jar) ship as base64 text
(`*.b64`) because GitHub snapshot pushes are UTF-8 only. One-time setup
after cloning:

```sh
python3 decode_all.py     # restores gradle-wrapper.jar + res/raw/*.wav
./gradlew assembleDebug   # APK -> app/build/outputs/apk/debug/app-debug.apk
```

The build also re-decodes the WAVs automatically (the `decodeB64Assets`
task in `app/build.gradle.kts`). To regenerate audio from source, run
`python3 gen_audio.py`, then re-encode: each `app/src/main/res/raw/*.wav`
has a matching `app/b64assets/raw/*.wav.b64`.

The 400-upgrade program lives in [UPGRADES.md](UPGRADES.md).

A wave-survival arena ARPG for Android, built for Nathan Clark (QamoTech).
20 waves, an Abyss Warden boss every 5 waves, XP-gem leveling with roguelite
upgrade choices, and a permanent meta shop (3 heroes, 4 weapons) paid for with
banked gems. Dark neon look, full-strength animation, touch-first controls.

## Controls
- Drag left side: virtual joystick (your weapon auto-fires at the nearest foe)
- DASH button (or tap right side): dash with i-frames
- Pause: ❚❚ top-right or Android Back

## Tech
- Kotlin, single Activity + custom Canvas GameView (60fps Choreographer loop)
- Zero third-party dependencies beyond kotlin-stdlib — no AndroidX needed
- Music/SFX: composed & synthesized with gen_audio.py (126 BPM battle loop,
  ambient menu loop, 11 SFX), bundled as res/raw WAVs
- Saves: SharedPreferences (gem bank, unlocks, records, settings)

## Build (on the Dell, qamelotcipher)
- JDK 21: C:\Program Files\Android\openjdk\jdk-21.0.8 (set JAVA_HOME)
- Gradle 8.9 wrapper: `gradlew.bat assembleDebug`
- SDK: compileSdk 35, minSdk 26, targetSdk 35
- Project on Dell: C:\Users\qamel\GemStorm — APK:
  app\build\outputs\apk\debug\app-debug.apk
- Verified 2026-11-17: BUILD SUCCESSFUL (33 tasks), aapt badging OK.
  No AVDs exist on the Dell, so no emulator run — verified by clean build +
  full code-level playthrough audit of every screen/flow.

## v1.1 (2026-11-18) — 8 additions, EMULATOR-VERIFIED ✅
1. Vampire Core upgrade (lifesteal)  2. Frost Rounds upgrade (slow)
3. Elite enemies (gold ring, 2.6× HP, 3× loot)  4. Combo score multiplier (≤×5)
5. Heart drops (+15 HP)  6. Dash afterimage ghosts + death shockwave rings
7. Projectile trails + muzzle flash  8. Animated menu (orbiting gems, gem rain, shimmer)
Emulator: AVD "gemtest" on the Dell (android-35 google_apis x86_64, WHPX).
Test 2026-11-18: install Success, launch OK, automated play cleared Wave 1
(score 220, combo 9 by Wave 2 pause), pause menu verified, no FATAL EXCEPTION.
Screenshots: GemStorm-v1.1-gameplay.png / GemStorm-v1.1-pause.png in Library.
