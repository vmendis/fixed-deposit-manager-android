# TEST INSTRUCTIONS — Round V32 — Issue #4 Loading & Error State Patterns
Branch: dev
Expected commit: dev @<will be filled after push> — shared loading/error patterns
Test cycle: V32 — first run for #4

## 0. Preflight — Owner does Steps 1-2 ONLY, then tells Roo: "Open TEST-INSTRUCTIONS.md at repo root and follow it"
```
dir gradlew.bat
dir app\src\androidTest\java\com\example\fdmanager

git status
:: must be clean
git fetch origin --prune
git checkout -B dev origin/dev
git log -1 --oneline
:: record SHA, expect dev with #4 loading & error
:: Verify no littering at root: dir should NOT show shot-*.png, logcat-*.txt, TEST-REPORT-*.md at root — those belong in test-artifacts-v32\ only
```

## 1. Freshness Gates — MUST be yes before Tier1/2
```
findstr /S /C:"navgate-0500r9" app\src\androidTest\java\com\example\fdmanager\TestSupport.kt
findstr /S /C:"fdcard-0200r9" app\src\main\java\com\example\fdmanager\ui\components\Components.kt
findstr /S /C:"home-scroll-0100r9" app\src\main\java\com\example\fdmanager\ui\screens\HomeScreen.kt
findstr /S /C:"onboarding-0100r9" app\src\main\java\com\example\fdmanager\ui\screens\OnboardingScreen.kt
findstr /S /C:"auth-0100r9" app\src\main\java\com\example\fdmanager\ui\screens\AuthScreens.kt
findstr /S /C:"syncstate-0300r9" app\src\main\java\com\example\fdmanager\ui\components\SyncStateChip.kt
findstr /S /C:"loadstate-0400r9" app\src\main\java\com\example\fdmanager\ui\components\LoadingState.kt
dir app\src\main\res\drawable\vssbox_cube.png
findstr /S /C:"com.vssbox.fdmanager" app\build.gradle.kts
findstr /S /C:"com.vssbox.fdmanager.R" app\src\main\java\com\example\fdmanager\ui\screens\AuthScreens.kt
findstr /S /C:"com.example.fdmanager.MainActivity" app\src\main\AndroidManifest.xml
```
All must say present yes. If missing → STOP, report SELECTOR-MISSING.

## 2. Tier1 — Unit Tests — 58 tests (54 existing + 4 LoadingState)
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
- LoadingStateTest 4
Total 58/58 GREEN
```

## 3. Tier2 — Instrumented — 28 tests (24 existing + 4 LoadingErrorUi)
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
- LoadingErrorUiTest 4
Total 28/28 GREEN

Logcat — MUST be real TestRunner lines, NOT placeholder:
adb logcat -d | findstr /i "TestRunner" > test-artifacts-v32\logcat-v32.txt
type test-artifacts-v32\logcat-v32.txt
:: must show REAL lines like:
:: 10-20 13:55:45.546 22782 22803 I TestRunner: run started: 28 tests
:: ...
:: 10-20 14:02:33.736 22782 22803 I TestRunner: run finished: 28 tests, 0 failed, 0 ignored
```

## 4. Screenshots — MUST be app, not launcher — FAIL-SAFE METHOD (user navigates, Roo captures, question/response)

**NEW FAIL-SAFE (to avoid past blind accepts):**
- You (Roo) NEVER use `adb shell input tap` coords for navigation — they suck (Pixel 7 1080x2400, coords shift)
- Owner does navigation manually when prompted via question/response, then Roo takes screenshot and verifies via `uiautomator dump` grep

**Prereq — Owner in bash (confirmed 2026-10-21, Git Bash):**
```bash
echo $BASH_VERSION
# should show 5.x — bash, empty → PowerShell, literal → CMD
echo $0
# should show bash or -bash
echo $SHELL
# /bin/bash
uname -a
# MINGW64 / MSYS
ls -la gradlew.bat
# works in bash
pwd
# /c/... path
# DO NOT use: ps -p $$ -o comm=  → Git Bash ps doesn't support -o (error: unknown option -- o)
```

**Folder — Windows CMD native (no -p, no -Force):**
```cmd
if not exist test-artifacts-v32 mkdir test-artifacts-v32
:: DO NOT use mkdir -p — in CMD creates folder "-p"
:: DO NOT use mkdir -Force — creates folder "-Force"
```

**Launch — MANDATORY before ANY screenshot — CMD native:**
```cmd
gradlew.bat installDebug
adb shell am start -n com.vssbox.fdmanager/com.example.fdmanager.MainActivity
timeout /t 5 >nul
adb shell dumpsys window | findstr mCurrentFocus
:: MUST show com.vssbox.fdmanager/com.example.fdmanager.MainActivity
:: If shows com.google.android.apps.nexuslauncher/NexusLauncherActivity → RE-LAUNCH via am start above, DO NOT capture
:: If onboarding → Owner manually taps "Explore demo data first" (link, NOT Get Started button)
:: In bash (owner): sleep 5 + adb shell dumpsys window | grep mCurrentFocus — but Roo uses findstr for Windows
```

**Fail-safe capture loop — Roo prompts via ask_user (question/response, minimal typing):**

For each of 4 required shots, Roo does:
1. `ask_user`: "Please navigate to [SCREEN]. When you see [EXPECTED TEXT], select Ready"
   - Owner navigates manually (no coords — coords suck)
   - Owner selects Ready
2. Roo runs (CMD native, no && chain in PowerShell — use separate lines):
```cmd
adb shell uiautomator dump /sdcard/window_dump.xml
adb pull /sdcard/window_dump.xml window_dump.xml
findstr /i "EXPECTED" window_dump.xml
:: if not found → ask again
adb shell screencap -p /sdcard/shot-xxx.png
adb pull /sdcard/shot-xxx.png test-artifacts-v32\shot-nsb-v32-xxx.png
dir test-artifacts-v32
```
3. Roo presents file + `ask_user`: "Is this correct? Shows [DESCRIPTION]?"
   - Options: Yes correct / No retake
   - If No → loop

**Required 4 shots for #4:**

1. `shot-nsb-v32-home-loading.png` — Home with loading skeleton (trigger via Settings > Loading & Error (debug #4) > Show loading state ON, then Home) — should show `fullScreenLoading` + `loadingList` + 3 `loadingSkeletonCard`, gate `loadstate-0400r9`
2. `shot-nsb-v32-home-error.png` — Home with error (Settings > Show error state ON, then Home) — should show `fullScreenError` + title "Failed to load" + message "Failed to load FDs (mock) — tap Retry" + Retry button `errorRetryButton`, gate `loadstate-0400r9`
3. `shot-nsb-v32-settings-loading-error.png` — Settings scrolled to "Loading & Error (debug #4)" section showing switches + preview `loadingErrorPreview` with skeleton + error preview
4. `shot-nsb-v32-list-loading.png` — FdList (or Calendar) with loading (enable loading debug, open any institution list, should show loading) OR Calendar loading

**Commands — Windows CMD native (folder part is Windows per owner 2026-10-21, no -p):**
```cmd
:: DO NOT use mkdir -p — in CMD creates folder "-p" (owner rule 2026-10-21)
if not exist test-artifacts-v32 mkdir test-artifacts-v32

adb shell am start -n com.vssbox.fdmanager/com.example.fdmanager.MainActivity
timeout /t 5 >nul
adb shell dumpsys window | findstr mCurrentFocus
:: Must be app

:: Capture after owner says Ready — separate lines, no && for PowerShell compat:
adb shell screencap -p /sdcard/shot-home-loading.png
adb pull /sdcard/shot-home-loading.png test-artifacts-v32\shot-nsb-v32-home-loading.png
dir test-artifacts-v32
```

**Hard rules — report REJECTED if:**
- Screenshot is launcher (Play Store/Gmail) or onboarding Get Started (must be Home/Settings with chip/loading)
- No `dumpsys` check showing `com.vssbox.fdmanager/com.example.fdmanager.MainActivity` (use findstr not grep — grep fails in PowerShell)
- No `uiautomator dump` + `findstr` verification for expected text
- No question/response loop for fail-safe (Roo prompts you to navigate, you navigate manually, Roo captures)
- Litter at root: `shot-*.png`, `logcat-*.txt`, `TEST-REPORT-*.md` at root — must be in `test-artifacts-v32\` only
- `mkdir -p` or `mkdir -Force` creates folder "-p"/"-Force" in CMD — use plain `mkdir` or `if not exist mkdir`, no -p, no -Force (owner rule 2026-10-21)

## 5. Report — TEST-REPORT-V32.md in test-artifacts-v32/
Must contain:
- Branch / commit real SHA
- Environment real versions (Windows 11, AS version, AVD Pixel_7_API_34 API 34, gradle 8.7, JDK 21)
- Tiers executed 2/2, freshness gates 11/11 yes
- Tier1 table 58/58 with real durations from `findstr time`
- Tier2 table 28/28 with real durations
- Logcat final lines REAL (run started 28 ... run finished 28 0 failed) — NOT placeholder
- Screenshots 4 png in test-artifacts-v32, app not launcher, with loadstate-0400r9 gate visible
- Failures section if any, with TEST-BUG/CODE-BUG/ENV-FLAKE
- Artifacts folder check: 4 png + logcat-v32.txt + TEST-REPORT-V32.md in test-artifacts-v32, nothing at root
