# TEST INSTRUCTIONS — Issue #22 By-bank cards natural wording Option B — Round V20 (fix V19 report + final GREEN)

## Environment
- Branch: `dev` (fresh from `origin/master` @ 8034f9c + #22 Option B @ 07f8ce5 + swipe fixes @ bf2de38/9981a83/d7f2b63)
- Commit: `git log -1 --oneline` — tester records actual SHA (expect **d7f2b63** with message "remove fragile performScrollTo")
- Freshness gate: `OPEN_BANK_GATE = navgate-0500r9` in `app\src\androidTest\java\com\example\fdmanager\TestSupport.kt` — logcat failure fingerprint must contain `openBank=navgate-0500r9` if openBank fails, else STALE sources
- Windows 11, Android Studio, AVD API 34, `adb devices` shows device
- Project root `fd-manager/` — all commands via `.\gradlew.bat` in PowerShell or `gradlew.bat` in CMD

## Why V20
- V19 logcat-v19.txt analysis:
  - 14:22-14:28: first runs 9 failures at `TestSupport.kt:57` `performScrollTo` (old V18 code line) — 7/16 pass
  - 14:42:18-14:43:54: **FINAL run PASSED 16/16 GREEN** — `run started: 16 tests` ... `run finished: 16 tests, 0 failed` — proves aggressive swipe 40 works
  - But TEST-REPORT-V19.md still reported 9 failures because it captured first run not final run, and `shot-nsb-v19.png` was launcher home (Play Store/Gmail) not app — violates hard rules
- Fix in V20 @ d7f2b63:
  - Remove fragile `performScrollTo()` entirely from `openBank` (was cause of 9 failures at line 57), now just `swipeUntilTag` + `waitUntil` exists + click via `onAllNodesWithTag[0]`
  - maxSwipes 40→50 for extra margin for taller Option B cards (3 lines: words + exact + invested)
  - gate navgate-0400r9→navgate-0500r9
- No app UI change — only test helper stability

## Tier 1 — JVM unit tests
PowerShell:
```powershell
.\gradlew.bat cleanTestDebugUnitTest testDebugUnitTest --rerun-tasks
```
CMD:
```bat
gradlew.bat cleanTestDebugUnitTest testDebugUnitTest --rerun-tasks
```
- Expected: 46 tests (FdMath 11, FdRepository 17, BankRegistry 7, InstitutionRegistry 11) — all GREEN
- Reports: `app\build\test-results\testDebugUnitTest\*.xml` + HTML `app\build\reports\tests\testDebugUnitTest\index.html`
- Durations from: `findstr time app\build\test-results\testDebugUnitTest\*.xml`

## Tier 2 — Instrumented Compose UI tests
### Test suite — 16 tests
| Class | Tests | Covers |
|---|---|---|
| `AddFdValidationTest` | 5 | empty form, valid save, payout required, 1-month tenor, 100 days tenor |
| `BankMonogramUiTest` | 4 | monogram tags, codes NSB/BOC |
| `NavigationFlowTest` | 2 | Home → list → detail → back |
| `RenewalChainTest` | 1 | COM-33018 renew chain |
| `SoftDeleteRestoreTest` | 2 | soft delete + restore |
| `SortFilterTest` | 2 | filter chips + sort menu |

### Run (clean build mandatory --rerun-tasks)
PowerShell:
```powershell
adb devices
.\gradlew.bat connectedDebugAndroidTest --rerun-tasks
```
CMD:
```bat
adb devices
gradlew.bat connectedDebugAndroidTest --rerun-tasks
```
- Pass: 0 failures, **16/16 GREEN** — V19 final run at 14:42 already proved 16/16 with aggressive swipe, V20 should be stable first run
- Reports XML: `app\build\outputs\androidTest-results\connected\*.xml`
- HTML: `app\build\reports\androidTests\connected\index.html`
- Durations from: `findstr time app\build\outputs\androidTest-results\connected\*.xml` — real numbers

### Logcat extraction (≤30 lines filtered, must include gate if failure)
PowerShell:
```powershell
adb logcat -d | findstr /i "TestRunner AssertionError summary:National openBank=navgate-0500r9" > logcat-v20.txt
type logcat-v20.txt
```
- If GREEN: logcat should show `run started: 16 tests` ... `run finished: 16 tests, 0 failed` — paste that as proof
- If failure: keep ≤30 lines around failure with gate `openBank=navgate-0500r9`

### Screenshot (must be app, not launcher — V17/V18/V19 all had launcher)
While app foreground on By-institution list showing Option B 3-line right column (words + exact + invested):
PowerShell:
```powershell
adb shell screencap -p /sdcard/shot.png
adb pull /sdcard/shot.png .\shot-nsb-v20.png
```
- Wrong: home screen with Play Store/Gmail/Photos/YouTube (all previous shots)
- Right: app showing `Rs 3.25 Million` / `Rs 3,250,000.00` / `invested` — use this as evidence Option B is in APK

### New UI behavior to verify (Option B — unchanged)
- Home hero unchanged: Total invested `Rs 3.2 Million` + `Rs 3,200,000.00`
- By-institution cards: right column 3 lines — Primary `Lkr.words(total)` bold teal, Secondary `Lkr.exact(total)` labelSmall gray, Tertiary `invested` labelSmall gray
- Visual: matches `mockups/22-bybank-mockups.html` Option B

## Report spec — `TEST-REPORT-V20.md` at project root
```markdown
# TEST REPORT — <date>
- Branch / commit: dev @ <short SHA> (expect d7f2b63)
- Environment: Windows 11, Android Studio <ver>, AVD <name> / API 34, gradle <ver>, JDK <ver>
- Tiers executed: 2 / 2
- Freshness gate: navgate-0500r9 present? yes/no

## Tier 1 — unit tests
| Test class | Tests | Passed | Failed | Duration | Report path |
- Real durations from findstr time

## Tier 2 — instrumented tests
| Test class | Tests | Passed | Failed | Duration | Report path |
- Real durations from findstr time, HTML path, sums to 16

## Evidence of GREEN (if 16/16)
- Logcat final lines: `run started: 16 tests` ... `run finished: 16 tests, 0 failed` pasted
- Screenshot: shot-nsb-v20.png is app not launcher

## Failures (if any, one section each)
### <TestClass>.<method>
- Result: TEST-BUG / CODE-BUG / ENV-FLAKE
- Assertion message: <verbatim>
- Relevant logcat: <≤30 lines REAL with gate if openBank fails>
- XML failure excerpt: <3–10 lines REAL with at com.example.fdmanager...:NN>
- Rerun result: <rerun with class filter>

## Blockers
- ...

## Not covered
- ...
```

Hard rules (V19 violations must be fixed):
- No placeholders — real SHA (d7f2b63), real versions, real filtered logcat ≤30 lines pasted (not "See logcat-*.txt"), real XML 3-10 lines pasted (not "Lines 8-18"), real durations from `findstr time`
- Tier2 sums to 16 (5+4+2+1+2+2), Tier1 46
- Screenshot must be app foreground, not launcher home — V17/V18/V19 all failed this
- Report must reflect FINAL run after flake protocol — V19 logcat proved final run at 14:42 passed 16/16 but report still said 9 failures
- Clean build mandatory `--rerun-tasks`, PowerShell `.\gradlew.bat`
- Freshness gate check `findstr /S /C:"navgate-0500r9"`

## Out of scope
- Firebase/backend, screenshot regression, cascade to FD list cards/detail
- No sync step in this file — tester handles `git status` clean → `fetch` → `checkout -B dev origin/dev` → verify SHA
