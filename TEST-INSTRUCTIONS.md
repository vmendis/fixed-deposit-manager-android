# TEST INSTRUCTIONS — Issue #22 By-bank cards natural wording Option B — Round V17

## Environment
- Branch: `dev` (fresh from `origin/master` @ 8034f9c + #22 Option B @ 07f8ce5)
- Commit: `git log -1 --oneline` — tester records actual SHA (expect 07f8ce5)
- Windows 11, Android Studio, AVD API 34, `adb devices` shows device
- Project root `fd-manager/` — all commands via `gradlew.bat`

## Tier 1 — JVM unit tests
```bat
gradlew.bat testDebugUnitTest
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
1. `testTag` existing only — `summary:<bank>` tags for By-institution cards
2. Visible exact text (Save FD, etc.)
3. `contentDescription`: Add FD, Back, etc.
- `dialogConfirm`/`dialogDismiss` for dialogs
- Bottom buttons need `performScrollTo().performClick()`
- Detail shows FD number twice → use `onAllNodesWithText`

### Test suite — 16 tests (unchanged from V16)
| Class | Tests | Covers |
|---|---|---|
| `AddFdValidationTest` | 5 | empty form, valid save (tenor months/days), payout required, 1-month tenor, 100 days tenor |
| `BankMonogramUiTest` | 4 | monogram tags, codes NSB/BOC |
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

### New UI behavior to verify (Option B — as selected)
- **Home hero unchanged:** Total invested still `Rs 3.2 Million` + `Rs 3,200,000.00` — no change per user confirmation
- **By-institution summary cards (BankSummaryCard):** right column now 3 lines:
  - Primary: `Lkr.words(total)` → `Rs 3.25 Million` (≥1M) / `Rs 750 K` (<1M, K family per #20) — bold, primary color (teal)
  - Secondary: `Lkr.exact(total)` → `Rs 3,250,000.00` — labelSmall gray
  - Tertiary: `invested` — labelSmall gray
- Visual: matches `mockups/22-bybank-mockups.html` Option B and `mockups/sampa-bybank-wording.png` and `mockups/22-option-b-implemented.png`
- Edge cases: `Rs 0`, `Rs 5 Million` (trailing .0 dropped), `Rs 12.5 Million`, `Rs 75 K`
- Selector `summary:<bank>` still works, amounts not asserted in tests

## Report spec — `TEST-REPORT-V17.md` at project root
```markdown
# TEST REPORT — <date>
- Branch / commit: dev @ <short SHA>
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
- Relevant logcat: <≤30 lines>
- XML failure excerpt: <3–10 lines with at com.example.fdmanager...:NN>
- Rerun result: <rerun>

## Blockers
- SELECTOR-MISSING: ...

## Not covered
- <explicit>
```

Hard rules:
- No placeholders — real SHA, versions, logcat
- Tier2 sums to 16 (5+4+2+1+2+2), Tier1 46 (11+17+7+11)
- Every failure: verbatim + logcat ≤30 + XML excerpt with line number + rerun once with class filter
- Flake protocol mandatory

## Out of scope
- Firebase/backend, screenshot regression, cascade to FD list cards/detail (per user: By-bank only for #22, Home unchanged)
