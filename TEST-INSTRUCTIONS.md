# TEST INSTRUCTIONS — Issue #28 FdCard payout frequency fix — Round V22

## Environment
- Branch: `dev` (fresh from `origin/master` @ 930fdb3 + payout fix)
- Commit: `git log -1 --oneline` — tester records actual SHA (expect new SHA after 930fdb3)
- Freshness gates: `OPEN_BANK_GATE = navgate-0500r9` in `TestSupport.kt`, `FDCARD_PAYOUT_GATE = fdcard-0200r9` in `Components.kt` (search `fdcard-0200r9`)
- Windows 11, Android Studio, AVD API 34, `adb devices` shows device
- Project root `fd-manager/` — all commands via `.\gradlew.bat` in PowerShell or `gradlew.bat` in CMD

## Why V22
- V21 GREEN 16/16 @ 15:47:45.745 — By-institution Option B + FD cards Option B words+exact verified (NSB-78412 Rs 1 Million / Rs 1,000,000.00)
- User found issue: FDListScreen must display interest payout frequency (Monthly payout / At maturity) — spec §3.2, §4.2, §4.6 says card meta shows tenor + payout. V21 FdCard showed `Rate • Tenor • Matures` missing payout.
- Fix in V22:
  - `FdCard` in `Components.kt` now: `"${formatRate}% p.a. • ${tenorLabel} • ${payoutFrequency.label} • matures ${date}"` — e.g. `10% p.a. • 12 months • Monthly payout • matures 10 Oct 2026`
  - Gate `fdcard-0100r9` (V21) → `fdcard-0200r9` (V22) — freshness marker
  - Spec `docs/FD-Manager-Spec.md` §4.2 updated explicit Rate•Tenor•Payout•Matures with examples, both copies byte-identical
  - Wire diagram `docs/app-wire-diagram.md` + `.html` updated with payoutFrequency.label + gate fdcard-0200r9, ASCII wire shows Monthly/At maturity
  - New docs: `docs/fdcard-payout-options.html` + `.md` — visual options A-D (A implemented: inline 4 bullets, B two-line, C chip, D icon) + recommendation
- No change to By-institution cards, hero, amount formatting — only meta line

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
- Reports: `app\build\test-results\testDebugUnitTest\*.xml` + HTML
- Durations from: `findstr time app\build\test-results\testDebugUnitTest\*.xml`

## Tier 2 — Instrumented Compose UI tests
### Test suite — 16 tests
| Class | Tests | Covers |
|---|---|---|
| `AddFdValidationTest` | 5 | empty form, valid save, payout required, 1-month, 100 days — card meta now should include payout wording |
| `BankMonogramUiTest` | 4 | monogram tags |
| `NavigationFlowTest` | 2 | Home → list → detail → back |
| `RenewalChainTest` | 1 | renew chain |
| `SoftDeleteRestoreTest` | 2 | soft delete + restore |
| `SortFilterTest` | 2 | filter chips + sort |

### Run (clean build mandatory --rerun-tasks)
PowerShell:
```powershell
adb devices
.\gradlew.bat connectedDebugAndroidTest --rerun-tasks
```
- Pass: 0 failures, 16/16 GREEN expected
- Reports XML: `app\build\outputs\androidTest-results\connected\*.xml`
- HTML: `app\build\reports\androidTests\connected\index.html`
- Durations from: `findstr time app\build\outputs\androidTest-results\connected\*.xml`

### Logcat extraction (≤30 lines filtered, must include gates if failure)
PowerShell:
```powershell
adb logcat -d | findstr /i "TestRunner AssertionError summary:National openBank=navgate-0500r9 fdcard" > logcat-v22.txt
type logcat-v22.txt
```
- If GREEN: `run started: 16 tests` ... `run finished: 16 tests, 0 failed`

### Screenshots (must be app, not launcher)
Need 2 screenshots:
1. FD list NSB filtered showing FD cards with payout: `10% p.a. • 12 months • Monthly payout • matures 10 Oct 2026` and `8% p.a. • 100 days • At maturity • matures ...` — proves fix
2. Home By-institution still Option B `Rs 1 Million / Rs 1,000,000.00 / invested`

PowerShell:
```powershell
adb shell screencap -p /sdcard/shot-list.png
adb pull /sdcard/shot-list.png .\shot-nsb-v22-list.png
adb shell screencap -p /sdcard/shot-home.png
adb pull /sdcard/shot-home.png .\shot-nsb-v22-home.png
```

### New UI behavior to verify
- FdCard meta line now 4 bullets: Rate • Tenor • Payout • Matures
  - Example NSB-78412: `10% p.a. • 12 months • Monthly payout • matures 10 Oct 2026` (from seed data, payout is AT_MATURITY? Check SampleData — NSB-78412 is AT_MATURITY? Actually seed: check FdRepository — NSB-78412 is AT_MATURITY per code? Need verify: in FdRepository.kt line 146 AT_MATURITY, line 131 MONTHLY — so both exist)
  - Visual matches `docs/fdcard-payout-options.html` Option A
- Amount still Option B words+exact (Rs 1 Million + Rs 1,000,000.00) — unchanged from V21
- By-institution cards still Option B 3 lines

## Report spec — `TEST-REPORT-V22.md` at project root
```markdown
# TEST REPORT — <date>
- Branch / commit: dev @ <short SHA>
- Environment: Windows 11, Android Studio <ver>, AVD <name> / API 34, gradle <ver>, JDK <ver>
- Tiers executed: 2 / 2
- Freshness gates: navgate-0500r9 yes, fdcard-0200r9 yes (findstr /S /C:"navgate-0500r9" and "fdcard-0200r9")

## Tier 1 — unit tests
| Test class | Tests | Passed | Failed | Duration | Report path |
- Real durations from findstr time

## Tier 2 — instrumented tests
| Test class | Tests | Passed | Failed | Duration | Report path |
- Real durations, sums to 16

## Evidence of GREEN
- Logcat final lines: run started: 16 tests ... run finished: 16 tests, 0 failed pasted
- Screenshots: shot-nsb-v22-list.png shows FD cards with payout frequency (Monthly payout / At maturity), shot-nsb-v22-home.png Home By-institution Option B
- FdCard verification: NSB-78412 shows `Monthly payout` or `At maturity` in meta per seed

## Failures (if any)
...

## Blockers
...

## Not covered
...
```

Hard rules:
- No placeholders — real SHA, real versions, real filtered logcat ≤30 lines pasted, real XML 3-10 lines if failures, real durations from findstr time
- Tier2 sums to 16, Tier1 46
- Screenshots app foreground, not launcher, need 2 shots proving payout
- Clean build mandatory --rerun-tasks
- Freshness gates: navgate-0500r9 and fdcard-0200r9

## Out of scope
- Firebase/backend, branding (on hold), screenshot regression
- No sync step — tester handles git status clean → fetch → checkout -B dev origin/dev → verify SHA
