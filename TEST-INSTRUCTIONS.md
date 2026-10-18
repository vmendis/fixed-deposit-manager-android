# TEST INSTRUCTIONS — Issue #13 Application ID rename & rebrand — Round V27

## Environment
- Branch: `dev` (fresh from `origin/master` @ 25ae1a2 + #13 rename com.example → com.vssbox.fdmanager)
- Commit: `git log -1 --oneline` — tester records actual SHA (expect new SHA after 25ae1a2, current dev with namespace com.vssbox.fdmanager)
- Freshness gates: `OPEN_BANK_GATE = navgate-0500r9` in `TestSupport.kt`, `FDCARD_PAYOUT_GATE = fdcard-0200r9` in `Components.kt`, `HOME_SCROLL_GATE = home-scroll-0100r9` in `HomeScreen.kt`, `ONBOARDING_GATE = onboarding-0100r9` + `DEMO_GATE = demo-0100r9` in `FdRepository.kt`, `VSSBOX_CUBE = vssbox_cube.png` drawable, `EMPTY_CTA = emptyStateAction` in `Components.kt`, `APPID_GATE = com.vssbox.fdmanager` in `app/build.gradle.kts` namespace + applicationId
- Windows 11, Android Studio, AVD Pixel_7_API_34 / API 34, `adb devices` shows device
- Project root `fd-manager/` — all commands via `.\\gradlew.bat` in PowerShell or `gradlew.bat` in CMD
- **CRITICAL RULE — Executor (Roo) must NOT edit app code**: Do NOT edit *.kt, *.kts. Report CODE-BUG if gate fails, don't edit. Only Architect edits code.

## Why V27 — Issue #13 Application ID rename & rebrand
**Title:** Application ID rename & rebrand

**Scope (from roadmap #13 body):**
- Rename `com.example.fdmanager` → `com.vssbox.fdmanager` (Play Store blocks `example.*`)
- App name, launcher icon (finance teal/green identity), splash

**Implementation #13 in dev:**
- `app/build.gradle.kts`: `namespace = "com.vssbox.fdmanager"`, `applicationId = "com.vssbox.fdmanager"` — Play Store requires non-example, `com.vssbox.*` groups future vssbox apps
- Source package `com.example.fdmanager` kept for now (allowed — namespace independent), R class now `com.vssbox.fdmanager.R`
- Clean build under new ID installs side-by-side with old `com.example.fdmanager` debug build (different applicationId)
- Owner email for Firebase/Play: `vssbox.com@gmail.com` (or `hello@vssbox.com` via Workspace, you own vssbox.com), secondary owner TBD — not vmendis@gmail.com per owner rule
- Firebase projects per app for free-tier max: `vssbox-fd-manager-dev` (Spark/emulator) + `vssbox-fd-manager-prod` (Blaze + $5 budget) — each project gets own 50K reads/day free

**Acceptance #13:**
- `com.vssbox.fdmanager` builds, installs, runs 46/46 + 16/16 GREEN (package rename doesn't break tests)
- Old `com.example.fdmanager` debug build remains installable side-by-side (different applicationId)
- Spec both copies identical md5

## Tier 1 — JVM unit tests
PowerShell:
```powershell
.\gradlew.bat cleanTestDebugUnitTest testDebugUnitTest --rerun-tasks
```
- Expected: 46 tests (FdMath 11, FdRepository 17, BankRegistry 7, InstitutionRegistry 11) — all GREEN
- Durations from: `findstr time app\build\test-results\testDebugUnitTest\*.xml`

## Tier 2 — Instrumented Compose UI tests
### Test suite — 16 tests
| Class | Tests | Covers |
|---|---|---|
| `AddFdValidationTest` | 5 | form validation, payout required, tenor |
| `BankMonogramUiTest` | 4 | monogram tags |
| `NavigationFlowTest` | 2 | Home → list → detail → back |
| `RenewalChainTest` | 1 | renew chain |
| `SoftDeleteRestoreTest` | 2 | soft delete (fixed V26) |
| `SortFilterTest` | 2 | filter chips |

### Run (clean build mandatory --rerun-tasks)
PowerShell:
```powershell
adb devices
.\gradlew.bat connectedDebugAndroidTest --rerun-tasks
```
- Pass: 0 failures, 16/16 GREEN
- Reports XML: `app\build\outputs\androidTest-results\connected\*.xml`
- HTML: `app\build\reports\androidTests\connected\index.html`
- Durations from: `findstr time app\build\outputs\androidTest-results\connected\*.xml`

### Logcat extraction — store in temp folder, not root (NO -Force flag!)
PowerShell (CMD compatible — NO -Force, use if not exist):
```powershell
if not exist test-artifacts-v27 mkdir test-artifacts-v27
adb logcat -d | findstr /i "TestRunner AssertionError summary:National openBank=navgate-0500r9 fdcard home-scroll onboarding demo emptyStateAction com.vssbox" > test-artifacts-v27\logcat-v27.txt
type test-artifacts-v27\logcat-v27.txt
```
- If GREEN: `run started: 16 tests` ... `run finished: 16 tests, 0 failed`
- Do NOT leave logcat-v27.txt at repo root — must be in test-artifacts-v27\

### Screenshots — store in temp folder, not root (NO -Force!)
Need 4 screenshots for #13 + #27 + #1 (same UI, new package):
1. Onboarding welcome with vssbox + trust line (96dp cube + vssbox + FD Manager + by vssbox + trustCard)
2. Home empty state with CTA (No deposits yet + Add your first FD + footer Built by vssbox)
3. Home after adding first FD (Total invested hero + By institution + FD visible)
4. Settings Demo toggle + About 72dp cube + vssbox + ©

PowerShell CMD compatible (NO -Force!):
```powershell
if not exist test-artifacts-v27 mkdir test-artifacts-v27
adb shell dumpsys window | findstr mCurrentFocus
adb shell screencap -p /sdcard/shot-onboarding.png
adb pull /sdcard/shot-onboarding.png .\test-artifacts-v27\shot-nsb-v27-onboarding.png
adb shell screencap -p /sdcard/shot-home-empty.png
adb pull /sdcard/shot-home-empty.png .\test-artifacts-v27\shot-nsb-v27-home-empty.png
adb shell screencap -p /sdcard/shot-home-after-first.png
adb pull /sdcard/shot-home-after-first.png .\test-artifacts-v27\shot-nsb-v27-home-after-first.png
adb shell screencap -p /sdcard/shot-settings-demo.png
adb pull /sdcard/shot-settings-demo.png .\test-artifacts-v27\shot-nsb-v27-settings-demo.png
dir test-artifacts-v27
```
- Must show 4 png + logcat-v27.txt + TEST-REPORT-V27.md inside, NOT at root
- **CRITICAL**: Do NOT use `mkdir ... -Force` in CMD — it creates extra folder named `-Force`. Use `mkdir test-artifacts-v27` or `if not exist test-artifacts-v27 mkdir test-artifacts-v27`

### New behavior to verify
- App builds under `com.vssbox.fdmanager` (check `adb shell pm list packages | findstr vssbox`)
- Old `com.example.fdmanager` debug build still installable side-by-side (different applicationId) — `adb shell pm list packages | findstr example` may still show old if installed
- Fresh install → onboarding → Get Started → Add FD → Home with totals — same as #1, package rename doesn't break flow
- Home empty CTA, FdList empty CTA, Calendar empty, RecycleBin empty — same as #1
- Settings About still 72dp cube + vssbox + © 2026
- FdCard payout Monthly/At maturity still
- Back from bank list hero visible still

## Report spec — `TEST-REPORT-V27.md` inside temp folder (not littering root)
Create folder `test-artifacts-v27\` and write report there: `test-artifacts-v27\TEST-REPORT-V27.md` — do NOT leave TEST-REPORT-V*.md at repo root.

```markdown
# TEST REPORT — <date> — V27 Cycle #13
- Branch / commit: dev @ <short SHA>
- Environment: Windows 11, Android Studio <ver>, AVD <name> / API 34, gradle <ver>, JDK <ver>
- Tiers executed: 2 / 2
- Freshness gates: navgate-0500r9 yes, fdcard-0200r9 yes, home-scroll-0100r9 yes, onboarding-0100r9 yes, demo-0100r9 yes, vssbox_cube.png yes, emptyStateAction yes, com.vssbox.fdmanager yes
- Artifacts folder: test-artifacts-v27\ contains 4 png + logcat-v27.txt + TEST-REPORT-V27.md, nothing at root

## Tier 1 — unit tests
| Test class | Tests | Passed | Failed | Duration | Report path |
- Real durations from findstr time

## Tier 2 — instrumented tests
| Test class | Tests | Passed | Failed | Duration | Report path |
- Real, sums to 16

## Evidence of GREEN + #13 rename
- Logcat final lines: run started: 16 ... run finished: 16 tests, 0 failed (from test-artifacts-v27\logcat-v27.txt)
- Package verification: `adb shell pm list packages | findstr vssbox` shows com.vssbox.fdmanager, old com.example.fdmanager side-by-side if still installed
- Screenshots: test-artifacts-v27\shot-nsb-v27-onboarding.png shows onboarding 96dp cube + trust line, etc.
- Repo root clean: dir at root shows no shot-*.png, no logcat-*.txt, no TEST-REPORT-*.md — all inside test-artifacts-v27\

## Failures (if any)
...

## Blockers
...

## Not covered
...
```

Hard rules:
- **Executor must NOT edit app code** — Report CODE-BUG if gate fails
- **No littering repo root** — All artifacts in test-artifacts-v27\ only, REJECTED if root has shot-*.png or logcat-*.txt
- **No mkdir -Force in CMD** — Use `mkdir test-artifacts-v27` or `if not exist test-artifacts-v27 mkdir test-artifacts-v27`, NOT `mkdir ... -Force` (creates extra -Force folder)
- No placeholders — real SHA, real versions, real logcat ≤30 lines, real durations from findstr time
- Tier2 sums to 16, Tier1 46
- Screenshots app foreground, not launcher, 4 shots proving #1 + #13 rename
- Clean build mandatory --rerun-tasks
- Freshness gates: navgate-0500r9, fdcard-0200r9, home-scroll-0100r9, onboarding-0100r9, demo-0100r9, vssbox_cube.png, emptyStateAction, com.vssbox.fdmanager

## Out of scope
- Firebase/backend, screenshot regression
- No sync step — tester handles git status clean → fetch → checkout -B dev origin/dev → verify SHA
