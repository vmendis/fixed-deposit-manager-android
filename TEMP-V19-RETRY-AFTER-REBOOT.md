# TEMP RETRY — V19 after V18 still 9 NSB failures — Issue #22 Option B aggressive swipe

## Why V19 (supersedes V18)
- V18 @ 1b71cee: maxSwipes 12→20 still 9/16 NSB failures identical to V17 — `swipeUp()` default distance too small for taller Option B cards (3 lines)
- V18 report violated hard rules: "See logcat-v18.txt" placeholders, "Lines 8-18" placeholders, `shot-nsb-v18.png` was launcher home (Play Store/Gmail) not app
- BOC/COM (top 2) pass, NSB (4th) fails consistently — needs more aggressive scroll
- Fix @ 9981a83: maxSwipes 20→40, swipe 80%→20% (300ms) via `swipe(Offset)` not `swipeUp()`, openBank resilient (waitUntil exists + try scrollTo + click via onAllNodes[0] + gate throw), gate navgate-0300r9→navgate-0400r9

## 0. Sync (tester handles — no sync in TEST-INSTRUCTIONS.md per user rule)
```bat
git status
git fetch origin
git checkout -B dev origin/dev
git log -1 --oneline
dir /b gradlew.bat
adb devices
```
- Expect: dev @ 9981a83, clean tree, gradlew.bat exists, emulator-5554 device
- CRITICAL: `git pull` does NOT bring TEMP-* if local dev behind — use `checkout -B dev origin/dev`
- PowerShell: `.\gradlew.bat`, CMD: `gradlew.bat`

## 1. Verify freshness gate (prove APK will have fix)
```bat
findstr /S /C:"navgate-0400r9" app\src\androidTest\java\com\example\fdmanager\*.kt
findstr /C:"maxSwipes" app\src\androidTest\java\com\example\fdmanager\TestSupport.kt
findstr /C:"0.8f" app\src\androidTest\java\com\example\fdmanager\TestSupport.kt
```
- Expect: `navgate-0400r9` + `maxSwipes: Int = 40` + `h * 0.8f` aggressive swipe
- If shows navgate-0300r9 or 20 → stale checkout, re-fetch

## 2. Clean build (mandatory --rerun-tasks)
PowerShell:
```powershell
.\gradlew.bat clean
.\gradlew.bat assembleDebug assembleDebugAndroidTest --rerun-tasks
```
CMD: remove `.\`

## 3. Tier 1 — unit
PowerShell:
```powershell
.\gradlew.bat cleanTestDebugUnitTest testDebugUnitTest --rerun-tasks
dir /s /b app\build\test-results\testDebugUnitTest\*.xml
findstr time app\build\test-results\testDebugUnitTest\*.xml
```
- Expected 46/46 GREEN

## 4. Tier 2 — full instrumented — V19 should be 16/16 GREEN
PowerShell:
```powershell
.\gradlew.bat connectedDebugAndroidTest --rerun-tasks
dir /s /b app\build\outputs\androidTest-results\connected\*.xml
findstr time app\build\outputs\androidTest-results\connected\*.xml
dir /b app\build\reports\androidTests\connected\debug\index.html
```
CMD: remove `.\`
- Expected 16/16 GREEN (5+4+2+1+2+2) — V18 7/16 with 9 NSB failures should be fixed by aggressive swipe 40

## 5. Evidence capture (if any failure — RIGHT after failure, before reboot, app foreground)
PowerShell filtered logcat ≤30 lines:
```powershell
adb logcat -d | findstr /i "TestRunner AssertionError summary:National openBank=navgate-0400r9" > logcat-v19.txt
type logcat-v19.txt
```
Screenshot MUST be app foreground (By-institution list with Option B 3-line right column), NOT launcher home:
```powershell
adb shell screencap -p /sdcard/shot.png
adb pull /sdcard/shot.png .\shot-nsb-v19.png
```
XML must be real 3-10 lines:
```bat
type app\build\outputs\androidTest-results\connected\debug\TEST-Pixel_7(AVD) - 14-_app-.xml
```

## 6. Rerun failed classes once each (flake protocol mandatory)
PowerShell:
```powershell
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.fdmanager.AddFdValidationTest --rerun-tasks
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.fdmanager.BankMonogramUiTest --rerun-tasks
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.fdmanager.NavigationFlowTest --rerun-tasks
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.fdmanager.RenewalChainTest --rerun-tasks
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.fdmanager.SoftDeleteRestoreTest --rerun-tasks
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.fdmanager.SortFilterTest --rerun-tasks
```

## 7. Deliver TEST-REPORT-V19.md (no placeholders — V18 had them)
- Header: dev @ 9981a83, Windows 11, AS version, AVD Pixel_7 API 34, gradle 8.13, JDK 21, gate navgate-0400r9 present yes/no
- Tier1: 4 rows = 46 tests, real durations from `findstr time`, report paths
- Tier2: 6 rows = 16 tests, real durations from `findstr time` per XML (no invented sums), HTML path
- If failures: for each, verbatim assertion + filtered logcat ≤30 REAL (paste lines, not "See logcat-*.txt") + XML 3-10 REAL with line number (paste, not "Lines 8-18") + rerun result
- Screenshot: `shot-nsb-v19.png` must be app, not launcher (V17/V18 both had launcher)
- No placeholders, Tiers 2/2, sums match

## 8. Handover for Roo
Open TEST-INSTRUCTIONS.md at repo root and follow it + open TEMP-V19-RETRY-AFTER-REBOOT.md for reboot/PowerShell handling — CRITICAL: dir /b without quotes, --rerun-tasks, PowerShell .\gradlew.bat, screenshot app foreground, logcat filtered ≤30 with gate, XML real 3-10 lines.
