# TEMP RETRY — V18 after V17 NSB failures — Issue #22 Option B swipe fix

## Why this file (V18 supersedes V17)
- V17 had 9 failures: `filterChipsNarrowTheList` + 8 others — all `TestTag = 'summary:National Savings Bank (NSB)'` not found at `TestSupport.kt:54 openBank` → `SortFilterTest.kt:31`
- V17 report violated hard rules: placeholders `(from logcat-v17.txt, ≤30 lines)`, `shot-nsb.png` was Android launcher home (Play Store/Gmail) not app, full 15MB boot logcat not filtered
- Root cause: Option B cards taller (words + exact + invested = 3 lines) vs 2 lines before, BOC/COM top 2 pass, NSB 4th fails — maxSwipes 12 insufficient
- Fix in dev @ 1cb748c (commit bf2de38): `swipeUntilTag/Text` maxSwipes 12→20, gate navgate-0250r9→navgate-0300r9 — app UI unchanged
- This file is concise, PowerShell vs CMD clarified

## 0. Sync (tester does this — no sync step in TEST-INSTRUCTIONS.md per user rule)
```bat
git status
git fetch origin
git checkout -B dev origin/dev
git log -1 --oneline
dir /b gradlew.bat
adb devices
```
- Expect: dev @ 1cb748c (or bf2de38+), clean working tree, gradlew.bat exists, emulator-5554 device
- **CRITICAL:** `git pull` does NOT bring TEMP-* if local dev behind or .gitignore TEMP* — use `checkout -B dev origin/dev` not plain pull
- PowerShell vs CMD: PS uses `.\gradlew.bat`, CMD uses `gradlew.bat` — `gradlew.bat` alone fails in PS with CommandNotFoundException

## 1. Verify freshness gate (prove APK will have fix)
```bat
findstr /S /C:"navgate-0300r9" app\src\androidTest\java\com\example\fdmanager\*.kt
findstr /C:"maxSwipes" app\src\androidTest\java\com\example\fdmanager\TestSupport.kt
```
- Expect: `OPEN_BANK_GATE = "navgate-0300r9"` + `maxSwipes: Int = 20` (2 places)
- If shows navgate-0250r9 or maxSwipes 12 → stale checkout, re-fetch

## 2. Clean build (reboot = stale APK possible, mandatory --rerun-tasks)
PowerShell:
```powershell
.\gradlew.bat clean
.\gradlew.bat assembleDebug assembleDebugAndroidTest --rerun-tasks
```
CMD:
```bat
gradlew.bat clean
gradlew.bat assembleDebug assembleDebugAndroidTest --rerun-tasks
```

## 3. Tier 1 — unit (force rerun, real durations)
PowerShell:
```powershell
.\gradlew.bat cleanTestDebugUnitTest testDebugUnitTest --rerun-tasks
dir /s /b app\build\test-results\testDebugUnitTest\*.xml
findstr time app\build\test-results\testDebugUnitTest\*.xml
```
CMD: remove `.\`
- Expected 46/46 GREEN (FdMath 11, FdRepository 17, BankRegistry 7, InstitutionRegistry 11)

## 4. Tier 2 — full instrumented (force rerun) — V18 should be 16/16 GREEN
PowerShell:
```powershell
.\gradlew.bat connectedDebugAndroidTest --rerun-tasks
dir /s /b app\build\outputs\androidTest-results\connected\*.xml
findstr time app\build\outputs\androidTest-results\connected\*.xml
dir /b app\build\reports\androidTests\connected\debug\index.html
```
CMD: remove `.\`
- Expected 16/16 GREEN (5+4+2+1+2+2) — V17 NSB failures should be fixed by maxSwipes 20
- If still fails: capture evidence immediately (step 5) before reboot

## 5. Evidence capture (if any failure — do RIGHT after failure, before reboot)
PowerShell (filtered logcat ≤30 lines, not 15MB boot dump):
```powershell
adb logcat -d | findstr /i "TestRunner AssertionError summary:National openBank=navgate-0300r9" > logcat-v18.txt
type logcat-v18.txt
adb shell screencap -p /sdcard/shot.png
adb pull /sdcard/shot.png .\shot-nsb-v18.png
type app\build\outputs\androidTest-results\connected\debug\TEST-Pixel_7(AVD) - 14-_app-.xml
```
- Screenshot MUST be app foreground showing By-institution cards (Option B 3-line right column), NOT launcher home with Play Store/Gmail/Photos/YouTube
- Logcat MUST include gate `openBank=navgate-0300r9` if openBank fails — proves fresh sources (v7 stale bug had byte-identical signature to v6)
- XML MUST include 3-10 lines with `at com.example.fdmanager...:NN`

## 6. Rerun failed classes once each (flake protocol mandatory per hard rules)
PowerShell:
```powershell
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.fdmanager.AddFdValidationTest --rerun-tasks
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.fdmanager.BankMonogramUiTest --rerun-tasks
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.fdmanager.NavigationFlowTest --rerun-tasks
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.fdmanager.RenewalChainTest --rerun-tasks
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.fdmanager.SoftDeleteRestoreTest --rerun-tasks
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.fdmanager.SortFilterTest --rerun-tasks
```
CMD: remove `.\`

## 7. Deliver TEST-REPORT-V18.md (no placeholders)
- Header: dev @ 1cb748c (or actual bf2de38+), Windows 11, AS version from Help→About, AVD Pixel_7 API 34, gradle 8.13, JDK 21, gate navgate-0300r9 present yes/no
- Tier1: 4 rows = 46 tests, real durations from `findstr time`, report paths
- Tier2: 6 rows = 16 tests (5+4+2+1+2+2), real durations from `findstr time` per XML (no invented sums like 8.126+2.625), HTML path
- If failures: for each, include verbatim assertion + filtered logcat ≤30 lines REAL + XML 3-10 lines REAL with line number + rerun result
- Screenshot: `shot-nsb-v18.png` must be app, not launcher
- No placeholders: no `<≤30 lines>`, no `(from logcat-*.txt)`, no `(unknown)`, no `$(...)`
- Tiers executed 2/2, sums must match

## 8. Handover for Roo
Open TEST-INSTRUCTIONS.md at repo root and follow it + open TEMP-V18-RETRY-AFTER-REBOOT.md for reboot/PowerShell handling — CRITICAL: never use quotes in dir commands, use dir /b and dir /s /b without quotes, use --rerun-tasks for both tiers, PowerShell uses .\gradlew.bat, CMD uses gradlew.bat, screenshot must be app foreground, logcat filtered ≤30 lines with gate.
