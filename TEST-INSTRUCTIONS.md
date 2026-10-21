# TEST INSTRUCTIONS — V32 — #4 Loading & Error — dev @5cef891+
Branch: dev
SHA: 5cef891+ (after fix)

## 1. Gates — must be yes
```
findstr /S /C:"navgate-0500r9" app\src\androidTest\java\com\example\fdmanager\TestSupport.kt
findstr /S /C:"fdcard-0200r9" app\src\main\java\com\example\fdmanager\ui\components\Components.kt
findstr /S /C:"home-scroll-0100r9" app\src\main\java\com\example\fdmanager\ui\screens\HomeScreen.kt
findstr /S /C:"loadstate-0400r9" app\src\main\java\com\example\fdmanager\ui\components\LoadingState.kt
dir app\src\main\res\drawable\vssbox_cube.png
```
All yes or STOP.

## 2. Tier1 — 58 tests
```
gradlew.bat cleanTestDebugUnitTest testDebugUnitTest --rerun-tasks
findstr time app\build\test-results\testDebugUnitTest\*.xml
```
Expect 58/58 GREEN.

## 3. Tier2 — 28 tests
```
adb devices
gradlew.bat connectedDebugAndroidTest --rerun-tasks
findstr time app\build\outputs\androidTest-results\connected\*.xml
adb logcat -d | findstr /i "TestRunner" > test-artifacts-v32\logcat-v32.txt
type test-artifacts-v32\logcat-v32.txt
```
Expect run started 28 ... run finished 28 0 failed.

**IF FAILS — DO NOT FETCH/CHECKOUT/LOOP. REPORT AND STOP.**
Only rerun failed class ONCE:
```
gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.fdmanager.LoadingErrorUiTest
```
Report both runs.

## 4. Screenshots — fail-safe — you prompt, owner navigates manually, you capture
```
if not exist test-artifacts-v32 mkdir test-artifacts-v32
gradlew.bat installDebug
adb shell am start -n com.vssbox.fdmanager/com.example.fdmanager.MainActivity
timeout /t 5 >nul
adb shell dumpsys window | findstr mCurrentFocus
:: MUST be com.vssbox.fdmanager/com.example.fdmanager.MainActivity, NOT launcher
:: If onboarding → owner taps "Explore demo data first" manually
```

For each shot:
- Ask owner via question: "Navigate to [SCREEN], Ready?"
- Owner navigates manually (no tap coords)
- You run:
```
adb shell uiautomator dump /sdcard/window_dump.xml
adb pull /sdcard/window_dump.xml window_dump.xml
findstr /i "EXPECTED" window_dump.xml
adb shell screencap -p /sdcard/shot-xxx.png
adb pull /sdcard/shot-xxx.png test-artifacts-v32\shot-nsb-v32-xxx.png
dir test-artifacts-v32
```
- Present file + ask "Is this correct? Yes/No" — loop if No.

Required 4:
1. home-loading — Settings > Loading & Error debug #4 > Show loading ON → Home → skeleton + spinner, gate loadstate-0400r9
2. home-error — Show error ON → Home → FullScreenError + Retry
3. settings-loading-error — Settings section Loading & Error (debug #4) with switches + preview
4. list-loading — institution list with loading

## 5. Report — TEST-REPORT-V32.md in test-artifacts-v32
Real SHA, real versions, real durations, real logcat, 4 png app not launcher, no root png, no -p folder.
