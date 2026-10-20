# TEST INSTRUCTIONS — Round V30 — Issue #3 Sync-State Chip & Error/Retry
Branch: dev
Expected commit: dev @79dc36c — robust scroll helpers (ensure Sync mock header, scrollToTag, ensureHomeTop, 15s)
Test cycle: V30 — seventh run

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

Logcat — MUST be real TestRunner lines, NOT placeholder:
adb logcat -d | findstr /i "TestRunner" > test-artifacts-v30\logcat-v30.txt
type test-artifacts-v30\logcat-v30.txt
:: must show REAL lines like:
:: 10-20 13:55:45.546 22782 22803 I TestRunner: run started: 24 tests
:: ...
:: 10-20 14:02:33.736 22782 22803 I TestRunner: run finished: 24 tests, 0 failed, 0 ignored
:: If file is 330 bytes with "Logcat output for v30" placeholder → FAKE → REJECTED, re-run real command above
:: Previous VALID logcat at 13:55:45 proves 24/24 GREEN @79dc36c — keep that 156K file, don't overwrite with fake
```

## 4. Screenshots — MUST be app, not launcher — use 5s settle per owner tip 2026-10-18 — APP MUST BE FOREGROUND
```
:: 0. Ensure app installed and launch it — MANDATORY before ANY screenshot
gradlew.bat installDebug
adb shell am start -n com.vssbox.fdmanager/com.example.fdmanager.MainActivity
:: Correct component is com.vssbox.fdmanager/com.example.fdmanager.MainActivity (applicationId + FQCN activity)
:: Wrong: com.vssbox.fdmanager/.MainActivity → resolves to com.vssbox.fdmanager.MainActivity which does not exist → Error type 3

timeout /t 5 >nul
adb shell dumpsys window | findstr mCurrentFocus
:: MUST show com.vssbox.fdmanager/com.example.fdmanager.MainActivity
:: If shows com.google.android.apps.nexuslauncher/NexusLauncherActivity → you are on launcher, NOT app → RE-LAUNCH via am start above, DO NOT capture

if not exist test-artifacts-v30 mkdir test-artifacts-v30

:: 1. Home with sync chip Saved — MUST be app foreground
adb shell am start -n com.vssbox.fdmanager/com.example.fdmanager.MainActivity
timeout /t 5 >nul
adb shell dumpsys window | findstr mCurrentFocus
:: verify app again
adb shell screencap -p /sdcard/shot-home-sync-saved.png && adb pull /sdcard/shot-home-sync-saved.png .\test-artifacts-v30\shot-nsb-v30-home-sync-saved.png

:: 2. Settings Sync demo section — navigate to Settings tab inside app, scroll to "Sync (mock) #3"
adb shell am start -n com.vssbox.fdmanager/com.example.fdmanager.MainActivity
timeout /t 3 >nul
:: Tap Settings bottom nav — coordinates may vary, or manually tap Settings in app
:: adb shell input tap 850 2100  :: example for Settings tab — adjust or tap manually
:: After Settings visible:
adb shell dumpsys window | findstr mCurrentFocus
adb shell screencap -p /sdcard/shot-settings-sync.png && adb pull /sdcard/shot-settings-sync.png .\test-artifacts-v30\shot-nsb-v30-settings-sync.png

:: 3. Home with Offline chip — in Settings, tap "Simulate offline", then tap Home tab
adb shell am start -n com.vssbox.fdmanager/com.example.fdmanager.MainActivity
timeout /t 3 >nul
adb shell screencap -p /sdcard/shot-home-offline.png && adb pull /sdcard/shot-home-offline.png .\test-artifacts-v30\shot-nsb-v30-home-offline.png

:: 4. Home with Error + Snackbar Retry — in Settings, tap "Simulate sync error", then Home tab, wait for Snackbar
adb shell am start -n com.vssbox.fdmanager/com.example.fdmanager.MainActivity
timeout /t 3 >nul
adb shell screencap -p /sdcard/shot-home-error.png && adb pull /sdcard/shot-home-error.png .\test-artifacts-v30\shot-nsb-v30-home-error.png

:: Verify all in folder, nothing at root
dir test-artifacts-v30
:: must show 4 png + logcat-v30.txt + TEST-REPORT-V30.md
dir *.png
:: second dir MUST show no png at root — if shows png at root, you littered root → move to test-artifacts-v30\

:: Use >nul to avoid "Input redirection not supported" when stdin redirected:
:: timeout /t 5 >nul  — NOT timeout /t 5 alone
:: If timeout still fails, use ping alternative: ping -n 6 127.0.0.1 >nul && adb shell screencap...
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

## 7. Notes for #3 — V30 seventh run @79dc36c
- Sync-state = mock only, 0 quota, fake session. Phase 2 will swap to FirebaseSyncRepository implementing same SyncRepository interface without layout changes.
- Home shows persistent chip: Saved (green CheckCircle), Syncing (spinner tertiary), Offline (CloudOff surfaceVariant), Error (Error errorContainer) + description text.
- Snackbar for transient errors with Retry action — retry re-runs last mock operation per acceptance.
- Settings Demo section #3: cycle Saved→Syncing→Offline→Error, simulate error, simulate offline, clear error — for design review.
- State machine documented in spec §4.11 and PR description.
- When Firebase configured later, tests should use emulator (local, 0 quota) or remain fake via BuildConfig. Real prod reads/writes count toward free tier, but CI should never hit prod.
- V30 fixes: 0db52e2 initial 4 fails, 1dec19d chip inside LazyColumn first item + home-scroll, 0f6fedc CODE-BUG conflict markers, 2bd9be2 clean 21/24 3 fails, e4d48e3 robust home-scroll fallback, c1bbd1c wait Sync (mock) header + waitForIdle, b93056b performScrollTo fallback + OFFLINE check in Settings, 6c27ad8 same, 79dc36c final robust helpers ensureSyncMockHeader + scrollToTag + ensureHomeTop + 15s.
- If still 3 fails (offlineChip, syncError, syncStateCycles), check logcat line numbers: L54 syncStateRow not displayed → need performScrollTo before assert; L119/124 offline chip timeout → ensure Home top scroll; L91/98/102 syncError timeout → ensure Settings ERROR before Home + chip Sync failed + Snackbar FD-123/mock/Retry fallback.
