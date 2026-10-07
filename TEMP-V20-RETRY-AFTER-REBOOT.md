# TEMP RETRY — V20 after V19 final run DID pass 16/16 but report said 9 failures — Issue #22 Option B

## Why V20 (supersedes V19)
- V19 logcat-v19.txt:
  - 14:22-14:28: 9 failures at TestSupport.kt:57 performScrollTo (old V18 line) — 7/16
  - 14:42:18-14:43:54: FINAL run 16/16 GREEN — `run started: 16 tests` ... `run finished: 16 tests, 0 failed` — proves fix works
  - But TEST-REPORT-V19.md said 9 failures (captured first run not final) + shot-nsb-v19.png was launcher home not app
- Fix V20 @ d7f2b63: remove fragile performScrollTo entirely, maxSwipes 40→50, gate navgate-0500r9

## 0. Sync
```bat
git status
git fetch origin
git checkout -B dev origin/dev
git log -1 --oneline
dir /b gradlew.bat
adb devices
```
- Expect dev @ d7f2b63, clean tree, gradlew.bat, emulator-5554
- PowerShell: .\gradlew.bat, CMD: gradlew.bat

## 1. Verify freshness gate
```bat
findstr /S /C:"navgate-0500r9" app\src\androidTest\java\com\example\fdmanager\*.kt
findstr /C:"maxSwipes" app\src\androidTest\java\com\example\fdmanager\TestSupport.kt
```
- Expect navgate-0500r9 + maxSwipes 50 + no performScrollTo at line 57 (now removed)

## 2. Clean build
PowerShell:
```powershell
.\gradlew.bat clean
.\gradlew.bat assembleDebug assembleDebugAndroidTest --rerun-tasks
```

## 3. Tier 1 — unit
```powershell
.\gradlew.bat cleanTestDebugUnitTest testDebugUnitTest --rerun-tasks
dir /s /b app\build\test-results\testDebugUnitTest\*.xml
findstr time app\build\test-results\testDebugUnitTest\*.xml
```
- Expected 46/46 GREEN

## 4. Tier 2 — full instrumented — V20 should be 16/16 GREEN first run (V19 final run already proved 16/16)
```powershell
.\gradlew.bat connectedDebugAndroidTest --rerun-tasks
dir /s /b app\build\outputs\androidTest-results\connected\*.xml
findstr time app\build\outputs\androidTest-results\connected\*.xml
```
- Expected 16/16 GREEN

## 5. Evidence capture (RIGHT after run, app foreground)
Filtered logcat ≤30 lines — if GREEN, capture final success lines:
```powershell
adb logcat -d | findstr /i "TestRunner run started run finished openBank=navgate-0500r9" > logcat-v20.txt
type logcat-v20.txt
```
Screenshot MUST be app foreground (By-institution cards Option B 3 lines), NOT launcher:
```powershell
adb shell screencap -p /sdcard/shot.png
adb pull /sdcard/shot.png .\shot-nsb-v20.png
```

## 6. If failure, rerun per class (flake protocol)
```powershell
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.fdmanager.AddFdValidationTest --rerun-tasks
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.fdmanager.BankMonogramUiTest --rerun-tasks
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.fdmanager.SoftDeleteRestoreTest --rerun-tasks
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.fdmanager.SortFilterTest --rerun-tasks
```

## 7. Deliver TEST-REPORT-V20.md (no placeholders, reflect FINAL run)
- Header dev @ d7f2b63, Windows 11, AS version, AVD Pixel_7 API 34, gradle 8.13, JDK 21, gate navgate-0500r9 present yes
- Tier1 4 rows =46, Tier2 6 rows=16, real durations from findstr time
- If GREEN: paste logcat final `run started: 16 tests` ... `run finished: 16 tests, 0 failed` + screenshot app not launcher
- If failures: verbatim assertion + filtered logcat ≤30 REAL + XML 3-10 REAL + rerun result
- No "See logcat-*.txt" or "Lines 8-18" placeholders — V19 had them
- Tiers 2/2, sums match

## 8. Handover for Roo
Open TEST-INSTRUCTIONS.md V20 at repo root + TEMP-V20-RETRY-AFTER-REBOOT.md — CRITICAL: final run must be reported, not first run, screenshot app foreground, logcat filtered ≤30 with gate, XML real 3-10 lines, dir /b without quotes, --rerun-tasks, PowerShell .\gradlew.bat.
