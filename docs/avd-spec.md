# AVD Spec — Used for Tier2 instrumented tests (V15-V22)

This is the exact Virtual Device spec the tester used for all GREEN runs (V15 14/14, V19 16/16, V20 16/16, V21 16/16 @ 15:47:45.745).

## Device
- **Name:** `Pixel_7_API_34` (in reports shows as `Pixel_7(AVD) - 14` or `Pixel_7_API_34`)
- **Device definition:** Pixel 7 (1080 x 2400, 420dpi, 6.3")
- **Category:** Phone

## System Image (critical — must match compileSdk 34)
- **Release:** Android 14.0 (UpsideDownCake) — API Level **34**
- **ABI:** **x86_64** (on Windows 11) — if you are on ARM, use arm64-v8a
- **Target:** **Google APIs** (not Google Play Store) — `google_apis` 
  - In Device Manager: "Google APIs" column, not "Google Play"
  - Why: Play Store images have Play Services restrictions, Google APIs is cleaner for `connectedDebugAndroidTest`
  - Image file: `system-images;android-34;google_apis;x86_64`
- **If you only see Play Store images:** Use `Pixel 7` + `Android 14.0 Google Play` API 34 x86_64 also works, but disable Play Store auto-update

## AVD Advanced Settings (what tester had)
- **AVD Name:** `Pixel_7_API_34` (keep same, tests reference this in XML report path `TEST-Pixel_7(AVD) - 14-_app-.xml`)
- **Startup orientation:** Portrait
- **RAM:** 2048 MB (default 1536 for Pixel 7, bump to 2048 for Compose tests stability)
- **Internal Storage:** 4096 MB (or 2048 MB minimum — tests write APK + coverage)
- **SD Card:** Studio-managed 512 MB (or 512 MB)
- **Graphics:** Hardware - GLES 2.0 (Auto)
- **Device Frame:** Enabled (optional)
- **Keyboard:** Enable keyboard input (optional)
- **Snapshot:** Quick Boot (default) — for flaky first run after cold boot, do one warm reboot before final run (as in logcat-v21.txt: first run 12 failed `No compose hierarchies`, second run 16/16 GREEN after warm)

## Host Environment (from TEST-REPORT-V21.md)
- **OS:** Windows 11
- **Android Studio:** 8.1.0 (or 2025.3.4 in V15) — any recent Hedgehog/Iguana/Jellyfish works, but keep JDK 21
- **Gradle:** 8.9 (wrapper `gradlew.bat`)
- **AGP:** 8.5.2 (from build.gradle.kts)
- **JDK:** 21 (Android Studio embedded JDK, `jbr-21`)
- **Kotlin:** 2.0.20
- **CompileSdk / TargetSdk:** 34
- **MinSdk:** 24
- **Test runner:** `androidx.test.runner.AndroidJUnitRunner`

## Verification after creation
```bat
:: List devices
adb devices
:: Should show:
:: emulator-5554 device

:: Check API level
adb shell getprop ro.build.version.sdk
:: Should return 34

:: Check ABI
adb shell getprop ro.product.cpu.abi
:: Should return x86_64 (or arm64-v8a)

:: Check AVD name
emulator -list-avds
:: Should include Pixel_7_API_34
```

## Steps to recreate in Android Studio
1. Tools → Device Manager → Create Virtual Device
2. Select **Pixel 7** → Next
3. Tab **x86_64** → Download **Android 14.0 Google APIs** API 34 → Next
   - If not visible: SDK Manager → System Images → Show x86_64 → download `Google APIs` API 34
4. AVD Name: `Pixel_7_API_34` → Advanced Settings:
   - RAM: 2048 MB
   - Internal Storage: 4096 MB
   - SD Card: 512 MB Studio-managed
   - Graphics: Hardware
5. Finish → Launch (cold boot first time)
6. Wait for boot, then run:
```bat
adb devices
gradlew.bat connectedDebugAndroidTest --rerun-tasks
```

## Notes from previous failures
- V17/V18/V19 screenshots were launcher (Play Store/Gmail) not app — because emulator was on home screen, not app foreground. Always `adb shell screencap` while app foreground on By-institution list.
- V17 first run failed `No compose hierarchies` — happens after cold boot. Fix: run tests once (may fail 12), then rerun with `--rerun-tasks` (second run GREEN). That's what logcat-v21.txt shows: 15:38 run 12 failed, 15:46 run 16/16 GREEN.
- maxSwipes 50 + no performScrollTo (gate navgate-0500r9) needed for taller Option B cards (words+exact+invested 3 lines + payout 4 bullets)

## For V22 (current dev @ c70ca0a)
- Same AVD `Pixel_7_API_34` API 34
- Freshness gates: `navgate-0500r9` in `TestSupport.kt`, `fdcard-0200r9` in `Components.kt` (search `findstr /S /C:"fdcard-0200r9"`)
- Expected: Tier1 46, Tier2 16 GREEN, FdCard meta shows `Monthly payout` / `At maturity`
