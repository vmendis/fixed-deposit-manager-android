# TEMP RETRY — V17 after emulator reboot — Issue #22 Option B

## Why this file
- Emulator was rebooted since last V17 run (9 failures NSB tag not found)
- Previous report had placeholders — need real logcat + XML + rerun per hard rules
- This file is concise (no scrolling hell) — copy-paste ready for Windows
- **PowerShell vs CMD:** If you are in PowerShell (PS prompt), use `.\gradlew.bat` instead of `gradlew.bat` — PowerShell does not load from current dir by default. CMD uses `gradlew.bat`. All `dir`/`findstr`/`adb` commands work in both.

## 0. Verify device after reboot
```bat
adb devices
adb shell getprop ro.build.version.sdk
dir /b gradlew.bat
git log -1 --oneline
```
- Expect: emulator-5554 device, API 34, gradlew.bat exists, dev @ 8ab440c
- If no device: start AVD from Android Studio Device Manager, wait for boot

## 1. Clean build (reboot = stale APK possible)
```bat
.\gradlew.bat clean
.\gradlew.bat assembleDebug assembleDebugAndroidTest --rerun-tasks
```
- CMD: remove `.\` → `gradlew.bat clean`

## 2. Tier 1 — unit (force rerun, real durations)
```bat
.\gradlew.bat cleanTestDebugUnitTest testDebugUnitTest --rerun-tasks
dir /s /b app\build\test-results\testDebugUnitTest\*.xml
findstr time app\build\test-results\testDebugUnitTest\*.xml
```
- CMD: `gradlew.bat cleanTestDebugUnitTest testDebugUnitTest --rerun-tasks`
- Expected 46/46 GREEN (FdMath 11, FdRepository 17, BankRegistry 7, InstitutionRegistry 11)

## 3. Tier 2 — full instrumented (force rerun)
```bat
.\gradlew.bat connectedDebugAndroidTest --rerun-tasks
dir /s /b app\build\outputs\androidTest-results\connected\*.xml
findstr time app\build\outputs\androidTest-results\connected\*.xml
dir /b app\build\reports\androidTests\connected\debug\index.html
```
- CMD: `gradlew.bat connectedDebugAndroidTest --rerun-tasks`
- Expected 16/16 GREEN (5+4+2+1+2+2) — but V17 had 9 NSB failures, likely due to taller cards (Option B 3 lines) needing more swipes than maxSwipes 12

## 4. If NSB tag still not found — immediate evidence (do this RIGHT after failure, before reboot again)
```bat
adb logcat -d > logcat-v17.txt
type logcat-v17.txt
adb shell screencap -p /sdcard/shot-nsb.png
adb pull /sdcard/shot-nsb.png .\shot-nsb.png
type app\build\outputs\androidTest-results\connected\debug\TEST-Pixel_7(AVD) - 14-_app-.xml
```

## 5. Rerun failed classes once each (flake protocol)
```bat
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.fdmanager.AddFdValidationTest
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.fdmanager.BankMonogramUiTest
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.fdmanager.SoftDeleteRestoreTest
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.fdmanager.SortFilterTest
```
- CMD: remove `.\`

## 6. Freshness gate (prove APK has Option B)
```bat
findstr /S /C:"navgate-0250r9" app\src\androidTest\java\com\example\fdmanager\*.kt
findstr /C:"Lkr.words" app\src\main\java\com\example\fdmanager\ui\screens\HomeScreen.kt
```
- First: should show OPEN_BANK_GATE in TestSupport.kt
- Second: should show Lkr.words(summary.totalInvested) — proves dev @ 8ab440c with Option B in APK
- If second returns nothing → stale APK, redo clean build

## 7. Deliver TEST-REPORT-V17.md (updated)
- Header: dev @ 8ab440c, Windows 11, AS version from Help→About, AVD Pixel_7 API 34, gradle 8.13
- Tier1: 4 rows (BankRegistry 7, FdMath 11, FdRepository 17, InstitutionRegistry 11) = 46, real durations
- Tier2: 6 rows (AddFd 5, BankMonogram 4, Navigation 2, Renewal 1, SoftDelete 2, SortFilter 2) = 16, real durations from findstr time (no sums like 8.126+2.625, use actual time per XML)
- Failures: for each failed test, include:
  - Result: TEST-BUG / CODE-BUG / ENV-FLAKE proposal
  - Assertion message verbatim (e.g. Expected exactly 1 node but could not find any node that satisfies: TestTag = summary:National Savings Bank (NSB))
  - Relevant logcat ≤30 lines REAL (from logcat-v17.txt, not placeholder)
  - XML failure excerpt 3-10 lines REAL with at com.example.fdmanager.AddFdValidationTest.validFormSavesNewFd(AddFdValidationTest.kt:65)
  - Rerun result
- Blockers: SELECTOR-MISSING if tag truly missing
- Not covered: Firebase/backend, screenshot regression
- No placeholders: no <≤30 lines>, no <3–10 lines...>, no (unknown), no $(...)

## 8. Quick triage hint for NSB failures
- BOC (1st card) passes, COM (2nd) passes, NSB (4th) fails → suggests maxSwipes 12 insufficient for taller Option B cards (3 lines vs 2 lines before)
- Possible fix: increase maxSwipes in TestSupport.kt swipeUntilTag from 12 to 20 (local test, do not commit) OR reduce card height
- If screenshot shows NSB card visible but tag not found → CODE-BUG in testTag placement
- If screenshot shows NSB not visible after 12 swipes → TEST-BUG (swipe count)

## Handover for Roo
Open TEMP-V17-RETRY-AFTER-REBOOT.md at repo root and follow it — CRITICAL: never use quotes in dir commands. Use dir /b and dir /s /b without quotes. Use --rerun-tasks for both test tasks. After reboot, clean build first. PowerShell uses .\gradlew.bat, CMD uses gradlew.bat.
