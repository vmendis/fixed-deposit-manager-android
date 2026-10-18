# TEST INSTRUCTIONS — Issue #13 Application ID rename & rebrand — Round V28

## Environment
- Branch: `dev` (fresh from `origin/master` @ 25ae1a2 + #13 rename com.example → com.vssbox.fdmanager + R class + manifest fix)
- Commit: `git log -1 --oneline` — tester records actual SHA (expect de42e57 — fix R class import com.example → com.vssbox + manifest FQCN)
- Freshness gates: `OPEN_BANK_GATE = navgate-0500r9` in `TestSupport.kt`, `FDCARD_PAYOUT_GATE = fdcard-0200r9` in `Components.kt`, `HOME_SCROLL_GATE = home-scroll-0100r9` in `HomeScreen.kt`, `ONBOARDING_GATE = onboarding-0100r9` + `DEMO_GATE = demo-0100r9` in `FdRepository.kt`, `VSSBOX_CUBE = vssbox_cube.png` drawable, `EMPTY_CTA = emptyStateAction` in `Components.kt`, `APPID_GATE = com.vssbox.fdmanager` in `app/build.gradle.kts` namespace + applicationId, `R_GATE = com.vssbox.fdmanager.R` in `HomeScreen.kt` / `OnboardingScreen.kt` / `SettingsScreen.kt`, `MANIFEST_GATE = com.example.fdmanager.MainActivity` in `AndroidManifest.xml`
- Windows 11, Android Studio, AVD Pixel_7_API_34 / API 34, `adb devices` shows device
- Project root `fd-manager/` — all commands via `.\gradlew.bat` in PowerShell or `gradlew.bat` in CMD
- **CRITICAL RULE — Executor (Roo) must NOT edit app code**: Do NOT edit *.kt, *.kts. Report CODE-BUG if gate fails, don't edit. Only Architect edits code.

## Why V28 — Issue #13 Application ID rename & rebrand — FIX V27 build
**Title:** Application ID rename & rebrand

**V27 result:** Tier1 46/46 GREEN, Tier2 16/16 GREEN (BUILD SUCCESSFUL 1m33s) per Roo console, but build was incremental UP-TO-DATE. Clean build would fail without R class + manifest fix because namespace changed to com.vssbox.fdmanager but 3 files still imported com.example.fdmanager.R and manifest used ".MainActivity" resolving to com.vssbox.fdmanager.MainActivity (class actually in com.example.fdmanager).

**Fix in dev @ de42e57:**
- `HomeScreen.kt`, `OnboardingScreen.kt`, `SettingsScreen.kt`: `import com.example.fdmanager.R` → `import com.vssbox.fdmanager.R`
- `AndroidManifest.xml`: `android:name=".MainActivity"` → `android:name="com.example.fdmanager.MainActivity"` (FQCN, since source package kept)
- Ensures clean build under new ID works, side-by-side install still

**Scope (from roadmap #13 body):**
- Rename `com.example.fdmanager` → `com.vssbox.fdmanager` (Play Store blocks `example.*`)
- App name, launcher icon (finance teal/green identity), splash

**Implementation #13 in dev @ de42e57:**
- `app/build.gradle.kts`: `namespace = "com.vssbox.fdmanager"`, `applicationId = "com.vssbox.fdmanager"`
- Source package kept `com.example.fdmanager` (allowed), R now `com.vssbox.fdmanager.R`
- Manifest FQCN fix for MainActivity
- Owner email placeholder `vssbox.com@gmail.com`, secondary TBD — not vmendis@gmail.com per owner rule
- Firebase per-app projects for free-tier max: `vssbox-fd-manager-dev` (Spark/emulator) + `vssbox-fd-manager-prod` (Blaze + $5 budget) — each 50K reads/day free

**Acceptance #13:**
- `com.vssbox.fdmanager` clean builds, installs, runs 46/46 + 16/16 GREEN
- Old `com.example.fdmanager` debug build side-by-side still installable
- Spec both copies identical

## Tier 1 — JVM unit tests
PowerShell:
```powershell
.\gradlew.bat cleanTestDebugUnitTest testDebugUnitTest --rerun-tasks
```
- Expected: 46 tests — all GREEN
- Durations from: `findstr time app\build\test-results\testDebugUnitTest\*.xml`

## Tier 2 — Instrumented Compose UI tests
| Class | Tests | Covers |
|---|---|---|
| `AddFdValidationTest` | 5 | form validation |
| `BankMonogramUiTest` | 4 | monogram |
| `NavigationFlowTest` | 2 | Home → list → detail → back |
| `RenewalChainTest` | 1 | renew chain |
| `SoftDeleteRestoreTest` | 2 | soft delete |
| `SortFilterTest` | 2 | filter chips |

Run:
```powershell
adb devices
.\gradlew.bat connectedDebugAndroidTest --rerun-tasks
```
- Pass: 16/16 GREEN
- Durations from: `findstr time app\build\outputs\androidTest-results\connected\*.xml`

### Logcat — temp folder, NO -Force!
CMD compatible:
```powershell
if not exist test-artifacts-v28 mkdir test-artifacts-v28
adb logcat -d | findstr /i "TestRunner AssertionError summary:National openBank=navgate-0500r9 fdcard home-scroll onboarding demo emptyStateAction com.vssbox" > test-artifacts-v28\logcat-v28.txt
type test-artifacts-v28\logcat-v28.txt
```
- Must show `run started: 16 tests` ... `run finished: 16 tests, 0 failed`

### Screenshots — temp folder, NO -Force!
CMD compatible (NO mkdir -Force!):
```powershell
if not exist test-artifacts-v28 mkdir test-artifacts-v28
adb shell dumpsys window | findstr mCurrentFocus
adb shell screencap -p /sdcard/shot-onboarding.png
adb pull /sdcard/shot-onboarding.png .\test-artifacts-v28\shot-nsb-v28-onboarding.png
adb shell screencap -p /sdcard/shot-home-empty.png
adb pull /sdcard/shot-home-empty.png .\test-artifacts-v28\shot-nsb-v28-home-empty.png
adb shell screencap -p /sdcard/shot-home-after-first.png
adb pull /sdcard/shot-home-after-first.png .\test-artifacts-v28\shot-nsb-v28-home-after-first.png
adb shell screencap -p /sdcard/shot-settings-demo.png
adb pull /sdcard/shot-settings-demo.png .\test-artifacts-v28\shot-nsb-v28-settings-demo.png
dir test-artifacts-v28
```
- Must show 4 png + logcat-v28.txt + TEST-REPORT-V28.md inside, NOT at root
- **CRITICAL**: Do NOT use `mkdir ... -Force` in CMD — creates folder `-Force`. Use `mkdir test-artifacts-v28` or `if not exist ... mkdir ...`

### Verify new package
```powershell
adb shell pm list packages | findstr vssbox
:: must show com.vssbox.fdmanager
adb shell pm list packages | findstr example
:: may show old com.example.fdmanager if still installed — side-by-side ok
```

## Report spec — `TEST-REPORT-V28.md` inside temp folder
Create `test-artifacts-v28\TEST-REPORT-V28.md`

```markdown
# TEST REPORT — <date> — V28 Cycle #13
- Branch / commit: dev @ de42e57
- Environment: Windows 11, Android Studio <ver>, AVD <name> / API 34, gradle <ver>, JDK <ver>
- Tiers executed: 2 / 2
- Freshness gates: navgate-0500r9 yes, fdcard-0200r9 yes, home-scroll-0100r9 yes, onboarding-0100r9 yes, demo-0100r9 yes, vssbox_cube.png yes, emptyStateAction yes, com.vssbox.fdmanager yes, com.vssbox.fdmanager.R yes, com.example.fdmanager.MainActivity yes
- Artifacts folder: test-artifacts-v28\ contains 4 png + logcat-v28.txt + TEST-REPORT-V28.md, nothing at root

## Tier 1 — unit tests
| Test class | Tests | Passed | Failed | Duration | Report path |
- Real durations

## Tier 2 — instrumented tests
| Test class | Tests | Passed | Failed | Duration | Report path |
- Real, sums to 16

## Evidence of GREEN + #13 rename + fix
- Logcat: run started: 16 ... run finished: 16 tests, 0 failed (from test-artifacts-v28\logcat-v28.txt)
- Package: adb shell pm list packages | findstr vssbox shows com.vssbox.fdmanager
- Screenshots: onboarding, home empty, home after first, settings demo in test-artifacts-v28\
- Repo root clean

## Failures
...

## Blockers
...

## Not covered
...
```

Hard rules:
- No littering root — all in test-artifacts-v28\
- No mkdir -Force in CMD
- No placeholders — real SHA de42e57, real versions, real logcat ≤30, real durations from findstr time
- Tier2 16, Tier1 46
- Clean build mandatory --rerun-tasks
- Freshness gates: navgate-0500r9, fdcard-0200r9, home-scroll-0100r9, onboarding-0100r9, demo-0100r9, vssbox_cube.png, emptyStateAction, com.vssbox.fdmanager, com.vssbox.fdmanager.R, com.example.fdmanager.MainActivity

## Out of scope
- Firebase/backend
- No sync step — tester handles git status clean → fetch → checkout -B dev origin/dev → verify SHA de42e57
