# TEST INSTRUCTIONS — Round V30 — Issue #3 Sync-State Chip & Error/Retry
Branch: dev
Expected commit: dev with #3 sync-state chip
Test cycle: V30

## 0. Preflight — Owner does Steps 1-2 ONLY, then tells Roo: "Open TEST-INSTRUCTIONS.md at repo root and follow it"
```
dir gradlew.bat
dir app\src\androidTest\java\com\example\fdmanager

git status
:: must be clean
git fetch origin --prune
git checkout -B dev origin/dev
git log -1 --oneline
:: record SHA, expect dev with #3 sync-state
:: Verify no littering at root: dir should NOT show shot-*.png, logcat-*.txt, TEST-REPORT-*.md at root — those belong in test-artifacts-v30\ only
```

## 1. Freshness Gates — MUST be yes before Tier1/2
```
findstr /S /C:"navgate-0500r9" app\src\androidTest\java\com\example\fdmanager\TestSupport.kt
findstr /S /C:"fdcard-0200r9" app\src\main\java\com\example\fdmanager\ui\components\Components.kt
findstr /S /C:"home-scroll-0100r9" app\src\main\java\com\example\fdmanager\ui\screens\HomeScreen.kt
findstr /S /C:"onboarding-0100r9" app\src\main\java\com\example\fdmanager\ui\screens\OnboardingScreen.kt
findstr /S /C:"auth-0100r9" app\src\main\java\com\example\fdmanager\ui\screens\AuthScreens.kt
findstr /S /C:"syncstate-0300r9" app\src\main\java\com\example\fdmanager\ui\components\SyncStateChip.kt
dir app\src\main\res\drawable\vssbox_cube.png
findstr /S /C:"com.vssbox.fdmanager" app\build.gradle.kts
findstr /S /C:"com.vssbox.fdmanager.R" app\src\main\java\com\example\fdmanager\ui\screens\AuthScreens.kt
findstr /S /C:"com.example.fdmanager.MainActivity" app\src\main\AndroidManifest.xml
```
All must say present yes. If missing → STOP, report SELECTOR-MISSING.

## 2. Tier1 — Unit Tests — 54 tests (50 existing + 4 SyncState)
```
gradlew.bat cleanTestDebugUnitTest testDebugUnitTest --rerun-tasks

findstr time app\build\test-results\testDebugUnitTest\*.xml
:: paste REAL numbers, NOT summed

Expected:
- BankRegistryTest 7
- FdMathTest 11
- FdRepositoryTest 17
- InstitutionRegistryTest 11
- AuthValidationTest 4
- SyncStateTest 4
Total 54/54 GREEN
```

## 3. Tier2 — Instrumented — 24 tests (20 existing + 4 SyncStateChip)
```
adb devices
:: must show emulator-5554 device

gradlew.bat connectedDebugAndroidTest --rerun-tasks

findstr time app\build\outputs\androidTest-results\connected\*.xml
:: paste REAL numbers

Expected:
- AddFdValidationTest 5
- BankMonogramUiTest 4
- NavigationFlowTest 2
- RenewalChainTest 1
- SoftDeleteRestoreTest 2
- SortFilterTest 2
- AuthFlowTest 4
- SyncStateChipTest 4
Total 24/24 GREEN

Logcat:
adb logcat -d | findstr /i "TestRunner" > test-artifacts-v30\logcat-v30.txt
type test-artifacts-v30\logcat-v30.txt
:: must show run started: 24 tests ... run finished: 24 tests, 0 failed
```

## 4. Screenshots — MUST be app, not launcher — use 5s settle per owner tip 2026-10-18
```
if not exist test-artifacts-v30 mkdir test-artifacts-v30

adb shell dumpsys window | findstr mCurrentFocus
:: must show com.vssbox.fdmanager/com.example.fdmanager.MainActivity

:: Home with sync chip Saved
timeout /t 5 >nul && adb shell screencap -p /sdcard/shot-home-sync-saved.png && adb pull /sdcard/shot-home-sync-saved.png .\test-artifacts-v30\shot-nsb-v30-home-sync-saved.png

:: Settings Sync demo section
timeout /t 3 >nul && adb shell screencap -p /sdcard/shot-settings-sync.png && adb pull /sdcard/shot-settings-sync.png .\test-artifacts-v30\shot-nsb-v30-settings-sync.png

:: Home with Offline chip
timeout /t 3 >nul && adb shell screencap -p /sdcard/shot-home-offline.png && adb pull /sdcard/shot-home-offline.png .\test-artifacts-v30\shot-nsb-v30-home-offline.png

:: Home with Error + Snackbar Retry
timeout /t 3 >nul && adb shell screencap -p /sdcard/shot-home-error.png && adb pull /sdcard/shot-home-error.png .\test-artifacts-v30\shot-nsb-v30-home-error.png

:: Verify all in folder, nothing at root
dir test-artifacts-v30
dir *.png
:: second dir should show no png at root
```

## 5. Report — TEST-REPORT-V30.md
Write at project root per BUILD-TEST-FLOW.html spec but final location must be test-artifacts-v30\ per hard rule 2026-10-18:
- Branch / commit: dev @ <actual SHA>
- Environment: Windows 11, Android Studio <ver>, AVD Pixel_7_API_34 / API 34, gradle <ver>, JDK <ver>
- Tiers executed: 2 / 2
- Freshness gates: navgate-0500r9 yes, fdcard-0200r9 yes, home-scroll-0100r9 yes, onboarding-0100r9 yes, auth-0100r9 yes, syncstate-0300r9 yes, vssbox_cube.png yes, com.vssbox.fdmanager yes, com.vssbox.fdmanager.R yes, com.example.fdmanager.MainActivity yes
- Tier1 table with real durations, 54 total
- Tier2 table with real durations, 24 total (5+4+2+1+2+2+4+4)
- Evidence: logcat final lines REAL pasted (run started 24 ... 0 failed), screenshots app not launcher (Home sync Saved, Settings Sync demo, Home Offline, Home Error+Snackbar)
- Failures section if any with TEST-BUG/CODE-BUG/ENV-FLAKE, real logcat ≤30 lines with gate, XML excerpt 3-10 lines
- Blockers: SELECTOR-MISSING if any
- Artifacts folder: test-artifacts-v30\ contains 4 png + logcat-v30.txt + TEST-REPORT-V30.md, nothing at root, no -Force folder

## 6. Hard Rules — Report REJECTED if:
- Literal $(...), (unknown), guessed versions
- Tier1 not 54, Tier2 not 24 (5+4+2+1+2+2+4+4=24)
- Durations summed like 2.56+2.412
- Evidence section "See logcat" placeholder
- Screenshot is launcher
- No final run after flake protocol
- No --rerun-tasks
- Freshness gates not checked
- Littering root: shot-*.png, logcat-*.txt, TEST-REPORT-*.md at repo root — must be in test-artifacts-v30\ only
- Executor edited app code — must NOT edit *.kt, report CODE-BUG instead
- mkdir with -Force in CMD — creates extra folder -Force, use plain mkdir or if not exist mkdir

## 7. Notes for #3
- Sync-state = mock only, 0 quota, fake session. Phase 2 will swap to FirebaseSyncRepository implementing same SyncRepository interface without layout changes.
- Home shows persistent chip: Saved (green CheckCircle), Syncing (spinner tertiary), Offline (CloudOff surfaceVariant), Error (Error errorContainer) + description text.
- Snackbar for transient errors with Retry action — retry re-runs last mock operation per acceptance.
- Settings Demo section #3: cycle Saved→Syncing→Offline→Error, simulate error, simulate offline, clear error — for design review.
- State machine documented in spec §4.11 and PR description.
- When Firebase configured later, tests should use emulator (local, 0 quota) or remain fake via BuildConfig. Real prod reads/writes count toward free tier, but CI should never hit prod.
