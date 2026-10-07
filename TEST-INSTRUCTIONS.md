# TEST INSTRUCTIONS — Issue #22 By-bank cards natural wording Option B — Round V19 (fix V18 NSB)

## Environment
- Branch: `dev` (fresh from `origin/master` @ 8034f9c + #22 Option B @ 07f8ce5 + swipe fixes @ bf2de38/9981a83)
- Commit: `git log -1 --oneline` — tester records actual SHA (expect **9981a83** with message "aggressive swipe 80%→20%")
- Freshness gate: `OPEN_BANK_GATE = navgate-0400r9` in `app\src\androidTest\java\com\example\fdmanager\TestSupport.kt` — logcat failure fingerprint must contain `openBank=navgate-0400r9` if openBank fails, else STALE sources
- Windows 11, Android Studio, AVD API 34, `adb devices` shows device
- Project root `fd-manager/` — all commands via `.\gradlew.bat` in PowerShell or `gradlew.bat` in CMD (PowerShell requires `.\` prefix)

## Why V19
- V17: 9 failures NSB tag not found at TestSupport.kt:54 openBank — maxSwipes 12 insufficient for taller Option B cards (3 lines vs 2)
- V18 fix 12→20 still 9 failures identical — `swipeUp()` default distance too small, 20 small swipes insufficient to compose 4th card NSB (BOC/COM top 2 pass, NSB 4th fails)
- V18 report violated hard rules: placeholders "See logcat-v18.txt for details" / "Lines 8-18" not real excerpts, `shot-nsb-v18.png` was launcher home (Play Store/Gmail) not app, 15MB boot logcat not filtered
- Fix in this round V19 @ 9981a83:
  - maxSwipes 20→40
  - swipe distance 80%→20% (300ms) via `swipe(Offset)` not small `swipeUp()`
  - openBank resilient: `waitUntil` tag exists, try `performScrollTo` best-effort, click via `onAllNodesWithTag[0]` not fragile exactly-1, throw with gate if not found after 40 swipes
  - gate navgate-0300r9→navgate-0400r9
- No app UI change — only test helper fix

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

## Tier 2 — Instrumented Compose UI tests
### Dependencies (add only if missing)
```kotlin
androidTestImplementation(platform("androidx.compose:compose-bom:2024.09.00"))
androidTestImplementation("androidx.compose.ui:ui-test-junit4")
androidTestImplementation("androidx.test.ext:junit:1.2.1")
androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
androidTestImplementation("androidx.test:runner:1.6.2")
debugImplementation("androidx.compose.ui:ui-test-manifest")
```

### Selector contract
1. `testTag` existing only — `summary:<bank>` tags for By-institution cards (e.g. `summary:National Savings Bank (NSB)`)
2. Visible exact text (Save FD, etc.)
3. `contentDescription`: Add FD, Back, etc.
- `dialogConfirm`/`dialogDismiss` for dialogs
- Bottom buttons need `performScrollTo().performClick()`
- Detail shows FD number twice → use `onAllNodesWithText`

### Test suite — 16 tests (unchanged)
| Class | Tests | Covers |
|---|---|---|
| `AddFdValidationTest` | 5 | empty form, valid save (tenor months/days), payout required, 1-month tenor, 100 days tenor |
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
- Pass: 0 failures, 16/16 GREEN (V18 had 7/16, 9 NSB failures)
- Reports XML: `app\build\outputs\androidTest-results\connected\*.xml`
- HTML: `app\build\reports\androidTests\connected\index.html`

### Logcat extraction (≤30 lines filtered — not full boot dump)
PowerShell:
```powershell
adb logcat -d | findstr /i "TestRunner AssertionError summary:National openBank=navgate-0400r9" > logcat-v19.txt
type logcat-v19.txt
```
CMD:
```bat
adb logcat -d | findstr /i "TestRunner AssertionError summary:National openBank=navgate-0400r9" > logcat-v19.txt
type logcat-v19.txt
```
- Keep ≤30 lines around failure, must include gate `openBank=navgate-0400r9` if openBank fails — proves fresh sources

### Screenshot (must be app, not launcher)
While app foreground on By-institution list showing Option B 3-line right column:
PowerShell:
```powershell
adb shell screencap -p /sdcard/shot.png
adb pull /sdcard/shot.png .\shot-nsb-v19.png
```
- Wrong: home screen with Play Store/Gmail/Photos/YouTube (V17 and V18 both had launcher screenshot)
- Right: app showing By-institution cards with `Rs 3.25 Million` / `Rs 3,250,000.00` / `invested`

### New UI behavior to verify (Option B — unchanged in V19)
- Home hero unchanged: Total invested still `Rs 3.2 Million` + `Rs 3,200,000.00`
- By-institution summary cards (BankSummaryCard): right column 3 lines:
  - Primary: `Lkr.words(total)` → `Rs 3.25 Million` (≥1M) / `Rs 750 K` (<1M) — bold, primary teal
  - Secondary: `Lkr.exact(total)` → `Rs 3,250,000.00` — labelSmall gray
  - Tertiary: `invested` — labelSmall gray
- Visual: matches `mockups/22-bybank-mockups.html` Option B and `mockups/22-option-b-implemented.png`
- Selector `summary:<bank>` still works

## Report spec — `TEST-REPORT-V19.md` at project root
```markdown
# TEST REPORT — <date>
- Branch / commit: dev @ <short SHA> (expect 9981a83)
- Environment: Windows 11, Android Studio <ver>, AVD <name> / API 34, gradle <ver>, JDK <ver>
- Tiers executed: 2 / 2
- Freshness gate: navgate-0400r9 present? yes/no (findstr TestSupport.kt + logcat fingerprint if failure)

## Tier 1 — unit tests
| Test class | Tests | Passed | Failed | Duration | Report path |
|---|---|---|---|---|---|
- Durations from: findstr /i "time" app\build\test-results\testDebugUnitTest\*.xml

## Tier 2 — instrumented tests
| Test class | Tests | Passed | Failed | Duration | Report path |
- Durations from: findstr /i "time" app\build\outputs\androidTest-results\connected\*.xml — real numbers, no invented sums

## Failures (one section each, if any)
### <TestClass>.<method>
- Result: TEST-BUG / CODE-BUG / ENV-FLAKE
- Assertion message: <verbatim from logcat, must include summary:National Savings Bank (NSB) if NSB>
- Relevant logcat: <≤30 lines filtered REAL, must include openBank=navgate-0400r9 if openBank fails — paste actual lines, not "See logcat-*.txt">
- XML failure excerpt: <3–10 lines REAL with at com.example.fdmanager...:NN — paste actual XML, not "Lines 8-18">
- Rerun result: <rerun with class filter — see below>

## Blockers
- SELECTOR-MISSING: ...

## Not covered
- <explicit>
```

Hard rules (START-HERE + user corrections, V18 violations fixed):
- No placeholders — real SHA (9981a83), real versions, real filtered logcat ≤30 lines (not "See logcat-v*.txt"), real XML 3-10 lines with line numbers (not "Lines 8-18"), real durations from `findstr time`
- Tier2 sums to 16 (5+4+2+1+2+2), Tier1 46 (11+17+7+11) — sums must match
- Every failure: verbatim assertion + filtered logcat ≤30 REAL + XML excerpt with `at com.example.fdmanager...:NN` REAL + rerun once with class filter
- Screenshot must be app foreground showing By-institution cards, not launcher home with Play Store/Gmail
- Flake protocol mandatory: on failure, rerun once with:
```bat
gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.fdmanager.<FailingClass> --rerun-tasks
```
- Clean build mandatory: `--rerun-tasks` for both tiers
- PowerShell requires `.\gradlew.bat`, CMD uses `gradlew.bat`
- Freshness gate check: `findstr /S /C:"navgate-0400r9" app\src\androidTest\java\com\example\fdmanager\*.kt` + logcat must contain gate if openBank fails

## Out of scope
- Firebase/backend, screenshot regression, cascade to FD list cards/detail (By-bank only for #22, Home unchanged)
- No sync step in this file — tester handles `git status` clean → `fetch` → `checkout -B dev origin/dev` → verify SHA separately
