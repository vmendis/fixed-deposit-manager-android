# TEST INSTRUCTIONS — Issue #22 By-bank cards natural wording Option B — Round V18 (fix V17 swipe)

## Environment
- Branch: `dev` (fresh from `origin/master` @ 8034f9c + #22 Option B @ 07f8ce5 + swipe fix @ bf2de38)
- Commit: `git log -1 --oneline` — tester records actual SHA (expect **bf2de38** with message "maxSwipes 12→20")
- Freshness gate: `OPEN_BANK_GATE = navgate-0300r9` in `app\src\androidTest\java\com\example\fdmanager\TestSupport.kt` line ~54 — logcat failure fingerprint must contain `openBank=navgate-0300r9` if openBank fails, else STALE sources
- Windows 11, Android Studio, AVD API 34, `adb devices` shows device
- Project root `fd-manager/` — all commands via `.\gradlew.bat` in PowerShell or `gradlew.bat` in CMD (PowerShell requires `.\` prefix — `gradlew.bat` alone fails with CommandNotFoundException)

## Why V18
- V17 logcat: `TestRunner: failed: filterChipsNarrowTheList` `AssertionError: Action performScrollTo() failed. Expected exactly '1' node but could not find any node that satisfies: (TestTag = 'summary:National Savings Bank (NSB)')` at `TestSupport.kt:54 openBank` → `SortFilterTest.kt:31`
- Diagnosis: Option B cards taller (words + exact + invested = 3 lines) vs 2 lines before, BOC/COM (top 2) pass, NSB (4th) fails — maxSwipes 12 insufficient
- Fix in this round: `swipeUntilTag`/`swipeUntilText` maxSwipes 12→20, gate navgate-0250r9→navgate-0300r9
- No app UI change — only test helper fix

## Tier 1 — JVM unit tests
```bat
.\gradlew.bat clean testDebugUnitTest --rerun-tasks
```
- Expected: 46 tests (FdMath 11, FdRepository 17, BankRegistry 7, InstitutionRegistry 11) — all GREEN (no new unit logic, reuses Lkr.words()/exact() from #20)
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

### Test suite — 16 tests (unchanged from V16/V17)
| Class | Tests | Covers |
|---|---|---|
| `AddFdValidationTest` | 5 | empty form, valid save (tenor months/days), payout required, 1-month tenor, 100 days tenor |
| `BankMonogramUiTest` | 4 | monogram tags, codes NSB/BOC |
| `NavigationFlowTest` | 2 | Home → list → detail → back |
| `RenewalChainTest` | 1 | COM-33018 renew chain |
| `SoftDeleteRestoreTest` | 2 | soft delete + restore |
| `SortFilterTest` | 2 | filter chips + sort menu |

### Run (PowerShell vs CMD)
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
- Must be clean build with `--rerun-tasks` (no cached results)
- Pass: 0 failures, 16/16 GREEN
- Reports XML: `app\build\outputs\androidTest-results\connected\*.xml`
- HTML: `app\build\reports\androidTests\connected\index.html`

### Logcat extraction (≤30 lines, filtered — not 15MB full boot)
```powershell
adb logcat -d | findstr /i "TestRunner AssertionError summary:National openBank=navgate-0300r9" | more
```
- Keep ≤30 lines around failure, must include gate `openBank=navgate-0300r9` if openBank fails

### Screenshot (must be app, not launcher)
While app foreground on By-institution list:
```powershell
adb shell screencap -p /sdcard/shot.png
adb pull /sdcard/shot.png shot-nsb.png
```
- Wrong: home screen with Play Store/Gmail/Photos/YouTube (V17 `shot-nsb.png` was launcher)
- Right: app showing By-institution cards with Option B 3-line right column

### New UI behavior to verify (Option B — as selected, unchanged in V18)
- **Home hero unchanged:** Total invested still `Rs 3.2 Million` + `Rs 3,200,000.00` — no change per user confirmation
- **By-institution summary cards (BankSummaryCard):** right column now 3 lines:
  - Primary: `Lkr.words(total)` → `Rs 3.25 Million` (≥1M) / `Rs 750 K` (<1M, K family per #20) — bold, primary color (teal)
  - Secondary: `Lkr.exact(total)` → `Rs 3,250,000.00` — labelSmall gray
  - Tertiary: `invested` — labelSmall gray
- Visual: matches `mockups/22-bybank-mockups.html` Option B and `mockups/sampa-bybank-wording.png` and `mockups/22-option-b-implemented.png`
- Edge cases: `Rs 0`, `Rs 5 Million` (trailing .0 dropped), `Rs 12.5 Million`, `Rs 75 K`
- Selector `summary:<bank>` still works, amounts not asserted in tests

## Report spec — `TEST-REPORT-V18.md` at project root
```markdown
# TEST REPORT — <date>
- Branch / commit: dev @ <short SHA> (expect bf2de38)
- Environment: Windows 11, Android Studio <ver>, AVD <name> / API 34, gradle <ver>, JDK <ver>
- Tiers executed: 2 / 2
- Freshness gate: navgate-0300r9 present? yes/no (grep TestSupport.kt + logcat fingerprint)

## Tier 1 — unit tests
| Test class | Tests | Passed | Failed | Report path |

## Tier 2 — instrumented tests
| Test class | Tests | Passed | Failed | Duration | Report path |
- Durations from: findstr /i "time" app\build\outputs\androidTest-results\connected\*.xml — no invented numbers

## Failures (one section each, if any)
### <TestClass>.<method>
- Result: TEST-BUG / CODE-BUG / ENV-FLAKE
- Assertion message: <verbatim from logcat>
- Relevant logcat: <≤30 lines filtered, must include openBank=navgate-0300r9 if openBank fails>
- XML failure excerpt: <3–10 lines with at com.example.fdmanager...:NN>
- Rerun result: <rerun with class filter — see below>

## Blockers
- SELECTOR-MISSING: ...

## Not covered
- <explicit>
```

Hard rules (from START-HERE + user corrections):
- No placeholders — real SHA (bf2de38), real versions, real logcat ≤30 lines (not 103k boot dump), real XML 3-10 lines with line numbers, real durations from `findstr time`
- Tier2 sums to 16 (5+4+2+1+2+2), Tier1 46 (11+17+7+11) — if sum mismatches, report is invalid
- Every failure: verbatim assertion + filtered logcat ≤30 + XML excerpt with `at com.example.fdmanager...:NN` + rerun once with class filter
- Screenshot must be app foreground, not launcher home
- Flake protocol mandatory: on failure, rerun once with:
```bat
gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.fdmanager.<FailingClass> --rerun-tasks
```
- Clean build mandatory: `--rerun-tasks` for both tiers
- PowerShell requires `.\gradlew.bat`, CMD uses `gradlew.bat`

## Out of scope
- Firebase/backend, screenshot regression, cascade to FD list cards/detail (per user: By-bank only for #22, Home unchanged)
- No sync step in this file — tester handles `git status` clean → `fetch` → `checkout -B dev origin/dev` → verify SHA separately per START-HERE
