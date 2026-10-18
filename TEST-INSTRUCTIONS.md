# TEST INSTRUCTIONS — Round V29 — Issue #2 Mock Auth Flow UI Only
Branch: dev
Expected commit: f60a60c (fix syntax — remove escaped quotes, auth-0100r9 gate present)
Test cycle: V29

## 0. Preflight — Owner does Steps 1-2 ONLY, then tells Roo: "Open TEST-INSTRUCTIONS.md at repo root and follow it"
```
dir gradlew.bat
dir app\src\androidTest\java\com\example\fdmanager

git status
:: must be clean
git fetch origin --prune
git checkout -B dev origin/dev
git log -1 --oneline
:: record SHA, expect dev with #2 mock auth
:: Verify no littering at root: dir should NOT show shot-*.png, logcat-*.txt, TEST-REPORT-*.md at root — those belong in test-artifacts-v29\ only
```

## 1. Freshness Gates — MUST be yes before Tier1/2
```
findstr /S /C:"navgate-0500r9" app\src\androidTest\java\com\example\fdmanager\TestSupport.kt
findstr /S /C:"fdcard-0200r9" app\src\main\java\com\example\fdmanager\ui\components\Components.kt
findstr /S /C:"home-scroll-0100r9" app\src\main\java\com\example\fdmanager\ui\screens\HomeScreen.kt
findstr /S /C:"onboarding-0100r9" app\src\main\java\com\example\fdmanager\ui\screens\OnboardingScreen.kt
findstr /S /C:"auth-0100r9" app\src\main\java\com\example\fdmanager\ui\screens\AuthScreens.kt
dir app\src\main\res\drawable\vssbox_cube.png
findstr /S /C:"com.vssbox.fdmanager" app\build.gradle.kts
findstr /S /C:"com.vssbox.fdmanager.R" app\src\main\java\com\example\fdmanager\ui\screens\AuthScreens.kt
findstr /S /C:"com.example.fdmanager.MainActivity" app\src\main\AndroidManifest.xml
```
All must say present yes. If missing → STOP, report SELECTOR-MISSING.

## 2. Tier1 — Unit Tests — 50 tests (46 existing + 4 AuthValidation)
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
Total 50/50 GREEN
```

## 3. Tier2 — Instrumented — 20 tests (16 existing + 4 AuthFlow)
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
Total 20/20 GREEN

Logcat:
adb logcat -d | findstr /i "TestRunner" > test-artifacts-v29\logcat-v29.txt
type test-artifacts-v29\logcat-v29.txt
:: must show run started: 20 tests ... run finished: 20 tests, 0 failed
```

## 4. Screenshots — MUST be app, not launcher — use 5s settle per owner tip 2026-10-18
```
if not exist test-artifacts-v29 mkdir test-artifacts-v29

adb shell dumpsys window | findstr mCurrentFocus
:: must show com.vssbox.fdmanager/com.example.fdmanager.MainActivity

:: Home empty (existing)
timeout /t 5 >nul && adb shell screencap -p /sdcard/shot-home-empty.png && adb pull /sdcard/shot-home-empty.png .\test-artifacts-v29\shot-nsb-v29-home-empty.png

:: Settings showing Sign in (mock auth entry)
timeout /t 3 >nul && adb shell screencap -p /sdcard/shot-settings.png && adb pull /sdcard/shot-settings.png .\test-artifacts-v29\shot-nsb-v29-settings.png

:: Sign-in screen with vssbox branding + emailField + auth-0100r9 gate
adb shell input tap 500 500
:: navigate Settings -> Sign in via UI or via monkey? Use manual tap or use adb shell am start...
:: Simplest: open Settings tab, tap Sign in row (approx center), then screencap
timeout /t 5 >nul && adb shell screencap -p /sdcard/shot-signin.png && adb pull /sdcard/shot-signin.png .\test-artifacts-v29\shot-nsb-v29-signin.png

:: Sign-up screen
timeout /t 3 >nul && adb shell screencap -p /sdcard/shot-signup.png && adb pull /sdcard/shot-signup.png .\test-artifacts-v29\shot-nsb-v29-signup.png

:: After successful sign-in, Settings shows email
timeout /t 3 >nul && adb shell screencap -p /sdcard/shot-settings-loggedin.png && adb pull /sdcard/shot-settings-loggedin.png .\test-artifacts-v29\shot-nsb-v29-settings-loggedin.png

:: Verify all in folder, nothing at root
dir test-artifacts-v29
dir *.png
:: second dir should show no png at root
```

## 5. Report — TEST-REPORT-V29.md
Write at project root per BUILD-TEST-FLOW.html spec:
- Branch / commit: dev @ <actual SHA>
- Environment: Windows 11, Android Studio <ver>, AVD Pixel_7_API_34 / API 34, gradle <ver>, JDK <ver>
- Tiers executed: 2 / 2
- Freshness gates: navgate-0500r9 yes, fdcard-0200r9 yes, home-scroll-0100r9 yes, onboarding-0100r9 yes, auth-0100r9 yes, vssbox_cube.png yes, com.vssbox.fdmanager yes, com.vssbox.fdmanager.R yes, com.example.fdmanager.MainActivity yes
- Tier1 table with real durations, 50 total
- Tier2 table with real durations, 20 total (5+4+2+1+2+2+4)
- Evidence: logcat final lines REAL pasted (run started 20 ... 0 failed), screenshots app not launcher (Settings Sign in, SignIn with vssbox logo, SignUp, Settings logged-in with email)
- Failures section if any with TEST-BUG/CODE-BUG/ENV-FLAKE, real logcat ≤30 lines with gate, XML excerpt 3-10 lines
- Blockers: SELECTOR-MISSING if any
- Artifacts folder: test-artifacts-v29\ contains 5 png + logcat-v29.txt + TEST-REPORT-V29.md, nothing at root, no -Force folder

## 6. Hard Rules — Report REJECTED if:
- Literal $(...), (unknown), guessed versions
- Tier1 not 50, Tier2 not 20 (5+4+2+1+2+2+4=20)
- Durations summed like 2.56+2.412
- Evidence section "See logcat" placeholder
- Screenshot is launcher
- No final run after flake protocol
- No --rerun-tasks
- Freshness gates not checked
- Littering root: shot-*.png, logcat-*.txt, TEST-REPORT-*.md at repo root — must be in test-artifacts-v29\ only
- Executor edited app code — must NOT edit *.kt, report CODE-BUG instead
- mkdir with -Force in CMD — creates extra folder -Force, use plain mkdir or if not exist mkdir

## 7. Notes for #2
- Mock auth = fake session flag only, no Firebase, 0 quota. Phase 2 will swap to FirebaseAuthRepository implementing same AuthRepository interface without layout changes.
- Settings top Account section: Sign in when logged out, email + Sign out when logged in (mockEmail)
- Validation: Email required / valid, Password 6+ chars, Confirm match
- When Firebase configured later, tests should use Auth Emulator (local, 0 quota) or remain fake via BuildConfig. Real prod reads/writes count toward free tier (Auth 50K MAU free, Firestore 50K reads/day), but CI should never hit prod.
