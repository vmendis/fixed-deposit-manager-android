# TEST INSTRUCTIONS — Issue #17 Day-based & 1-month tenors — Round V1

## Environment
- Branch: `dev` (fresh from `origin/master` @ 2499e1c + #17 commits)
- Commit: `git log -1 --oneline` — tester records actual SHA under test
- Windows 11, Android Studio, AVD API 34, `adb devices` shows device
- Project root `fd-manager/` — all commands via `gradlew.bat`

## Tier 1 — JVM unit tests
```bat
gradlew.bat testDebugUnitTest
```
- **Expected:** 46 tests (FdMath 11, FdRepository 17, BankRegistry 7, InstitutionRegistry 11) — all GREEN
- Reports: `app\build\test-results\testDebugUnitTest\*.xml` + HTML `app\build\reports\tests\testDebugUnitTest\index.html`
- New coverage (issue #17):
  - `maturity date plusDays for day-based tenor` — 100/300/30/1 day
  - `interest days over 365` — 100d = amount*rate*100/365, 300d
  - `1-month first-class tenor` — Jan31+1m = Feb28 clamp, interest months/12, labels "1 month"/"1 day"/"100 days"/"300 days"
  - `tenor label from FD`
  - `renew preserves day-based tenor and uses days for interest` — child preserves DAYS/100d, maturity +100d, CAPITALIZE adds days interest
  - `renew preserves 1-month tenor` — child preserves MONTHS/1m

## Tier 2 — Instrumented Compose UI tests
### Dependencies (add only if missing in `app/build.gradle.kts`)
```kotlin
androidTestImplementation(platform("androidx.compose:compose-bom:2024.09.00"))
androidTestImplementation("androidx.compose.ui:ui-test-junit4")
androidTestImplementation("androidx.test.ext:junit:1.2.1")
androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
androidTestImplementation("androidx.test:runner:1.6.2")
debugImplementation("androidx.compose.ui:ui-test-manifest")
```

### Selector contract (must follow)
1. `testTag` existing only
2. Visible exact text (e.g. "Save FD", "TEST-100D", chips "Months"/"Days", "1m"/"100d")
3. `contentDescription`: "Add FD", "Back", etc.
- `dialogConfirm`/`dialogDismiss` for dialogs — use `confirmDialog()` helper
- Bottom buttons need `performScrollTo().performClick()`
- Detail shows FD number twice → use `onAllNodesWithText`

### Test suite — 16 tests
| Class | Tests | Covers |
|---|---|---|
| `AddFdValidationTest` | 5 | empty form errors, valid save (tenor months), payout required, **NEW** add 1-month tenor (chip "1m" → card shows "1 month"), **NEW** add 100 days tenor (toggle "Days" → "100d" → card "100 days" — 100/300 NBFI specials) |
| `BankMonogramUiTest` | 4 | Roadmap #18 — monogram tags, codes NSB/BOC |
| `NavigationFlowTest` | 2 | Home → list → detail → back |
| `RenewalChainTest` | 1 | COM-33018 renew chain |
| `SoftDeleteRestoreTest` | 2 | soft delete + restore |
| `SortFilterTest` | 2 | filter chips + sort menu |

### Run
```bat
adb devices
gradlew.bat connectedDebugAndroidTest
```
- Pass: 0 failures, 16/16 GREEN
- Reports XML: `app\build\outputs\androidTest-results\connected\*.xml`
- HTML: `app\build\reports\androidTests\connected\index.html`

### New UI behavior to verify (manual spot-check if instrumented passes)
- Add FD screen: segmented toggle "Months | Days" (TenorUnit)
- Months presets: 1,3,6,12,24,36,60 — 1-month first-class
- Days presets: 30,60,90,100,180,300,364 — 100/300-day NBFI specials from plc.lk/LOLC
- Custom: Months 1–120, Days 1–999 validation
- Live preview: "Tenor: 100 days", maturity = opened.plusDays, interest = amount*rate*days/365
- Card meta: "9.5% p.a. • 100 days • matures ..." / "9.5% p.a. • 3 months • matures ..."
- Detail: Tenor row shows "100 days" or "1 month"
- Renewal preserves unit/value and interest calc

## Report spec — `TEST-REPORT-V1.md` at project root
```markdown
# TEST REPORT — <date>
- Branch / commit: dev @ <short SHA from git rev-parse --short HEAD>
- Environment: Windows 11, Android Studio <ver>, AVD <name> / API 34, gradle <ver>
- Tiers executed: 2 / 2

## Tier 1 — unit tests
| Test class | Tests | Passed | Failed | Report path |

## Tier 2 — instrumented tests
| Test class | Tests | Passed | Failed | Duration | Report path |

## Failures (one section each)
### <TestClass>.<method>
- Result: TEST-BUG / CODE-BUG / ENV-FLAKE
- Assertion message: <verbatim>
- Relevant logcat: <≤30 lines, real output>
- XML failure excerpt: <3–10 lines with at com.example.fdmanager...:NN>
- Rerun result: <result of -Pandroid.testInstrumentationRunnerArguments.class=... rerun>

## Blockers
- SELECTOR-MISSING: ...

## Not covered
- <explicit>
```

Hard rules:
- No placeholders — real SHA, real versions, real logcat
- Tier2 table: one row per class, counts sum to 16 (5+4+2+1+2+2)
- Tier1: 46 total
- Every failure: verbatim message + logcat ≤30 lines + XML excerpt with line number + rerun once with class filter
- Flake protocol mandatory

## Out of scope
- Firebase/backend, screenshot regression, Settings persistence across process death
