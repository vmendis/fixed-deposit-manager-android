# TEST INSTRUCTIONS — #4 Loading & Error — session branch @<SHA> — BASH (MINGW64)
Branch: arena/<session-id>-fixed-deposit-manager-android   (NOT dev — see docs/TESTING.md §0.0)
SHA: <paste `git log -1 --oneline` output — mandatory, a report without it is rejected>

WHY: this chat connects through the Arena.ai GitHub connector, which is scoped to the session
branch and cannot push to dev. Testing dev would test code WITHOUT the fix.

SHELL NOTE: Owner + Roo in bash MINGW64. Use bash commands (grep/ls/./gradlew/mkdir). Do NOT use CMD: chcp/findstr/dir/gradlew.bat/if not exist/timeout /t

## 1. Gates — must be yes (bash)
```
grep -r "navgate-0500r9" app/src/androidTest/java/com/example/fdmanager/
grep -r "fdcard-0200r9" app/src/main/java/com/example/fdmanager/ui/components/
grep -r "home-scroll-0100r9" app/src/main/java/com/example/fdmanager/ui/screens/
grep -r "loadstate-0400r9" app/src/main/java/com/example/fdmanager/ui/components/
grep -r "syncMockSection" app/src/main/java/com/example/fdmanager/ui/screens/SettingsScreen.kt
grep -r "loadingErrorSection" app/src/main/java/com/example/fdmanager/ui/screens/SettingsScreen.kt
ls app/src/main/res/drawable/vssbox_cube.png
```
All yes or STOP.

## 2. Tier1 — 58 tests (bash)
```
./gradlew cleanTestDebugUnitTest testDebugUnitTest --rerun-tasks
grep -h "time" app/build/test-results/testDebugUnitTest/*.xml
```
Expect 58/58 GREEN. Paste XML times.

## 3. Tier2 — 28 tests (bash) — ONE RUN ONLY, NO LOOP
```
adb devices
./gradlew connectedDebugAndroidTest --rerun-tasks
grep -h "time" app/build/outputs/androidTest-results/connected/*.xml
adb logcat -d | grep -i TestRunner > test-artifacts-v33/logcat-v33.txt
cat test-artifacts-v33/logcat-v33.txt
```
Expect run started 28 ... finished 28 0 failed.

**ANTI-LOOP: IF FAILS — DO NOT fetch/checkout/pull/loop. REPORT LOG + XML AND STOP.**
Flake protocol: rerun failed class ONCE only, no fetch:
```
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.fdmanager.LoadingErrorUiTest --rerun-tasks
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.fdmanager.SyncStateChipTest --rerun-tasks
```
Report both runs, then STOP. Do NOT auto-fix.

## 4. Screenshots — fail-safe — you prompt, owner navigates manually, you capture (bash)
```
mkdir test-artifacts-v33
./gradlew installDebug
adb shell am start -n com.vssbox.fdmanager/com.example.fdmanager.MainActivity
sleep 5
adb shell dumpsys window | grep mCurrentFocus
# MUST be com.vssbox.fdmanager/com.example.fdmanager.MainActivity, NOT launcher
# If onboarding → owner taps "Explore demo data first" manually
```
For each shot:
- Ask owner via question: "Navigate to [SCREEN], Ready? Yes/No"
- Owner navigates manually (no tap coords)
- You run:
```
adb shell uiautomator dump /sdcard/window_dump.xml
adb pull /sdcard/window_dump.xml window_dump.xml
grep -i "EXPECTED" window_dump.xml
adb shell screencap -p /sdcard/shot-xxx.png
adb pull /sdcard/shot-xxx.png test-artifacts-v33/shot-nsb-v33-xxx.png
ls test-artifacts-v33/
```
- Present file + ask "Is this correct? Yes/No" — loop if No.

Required 4:
1. home-loading — Settings > Loading & Error debug #4 > Show loading ON → Home → skeleton + spinner, gate loadstate-0400r9
2. home-error — Show error ON → Home → FullScreenError + Retry
3. settings-loading-error — Settings section Loading & Error (debug #4) with switches + preview (tag loadingErrorSection)
4. list-loading — institution list with loading (or syncMockSection for sync)

## 5. Report — TEST-REPORT-V33.md in test-artifacts-v33
Real SHA, real versions, real durations, real logcat, 4 png app not launcher, no root png, no -p/-Force folder, no ps -p $$ check.
