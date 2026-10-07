# TEST INSTRUCTIONS — Issue #22 extended to FD cards — Round V21 (FdCard Option B + wire diagram)

## Environment
- Branch: `dev` (fresh from `origin/master` @ 8034f9c + #22 By-institution Option B @ 07f8ce5 + swipe fix @ d7f2b63 + FD card Option B extended @ <new SHA>)
- Commit: `git log -1 --oneline` — tester records actual SHA (expect new SHA after V20 d7f2b63)
- Freshness gate: `OPEN_BANK_GATE = navgate-0500r9` in `app\src\androidTest\java\com\example\fdmanager\TestSupport.kt` — still valid, plus new gate `FDCARD_GATE = fdcard-0100r9` in `Components.kt` (search `fdcard-0100r9`)
- Windows 11, Android Studio, AVD API 34, `adb devices` shows device
- Project root `fd-manager/` — all commands via `.\gradlew.bat` in PowerShell or `gradlew.bat` in CMD

## Why V21
- V20 result GREEN 16/16 @ 14:59:45.486 `run finished: 16 tests, 0 failed` — proves swipe fix stable (maxSwipes 50, no performScrollTo, gate navgate-0500r9). But report V20 had placeholders (durations summed, Evidence section "See logcat..." not pasted, screenshot was launcher in earlier rounds).
- User observed screenshots: image-1.png By-institution list correct Option B `Rs 1 Million / Rs 1,000,000.00 / invested`, image-2.png FD list card `Rs 1,000,000` numbers only — wants #22 extended to FD cards too.
- Changes in V21:
  - `FdCard` in `ui/components/Components.kt` now Option B: Primary `Lkr.words(amount)` titleLarge bold primary (e.g. `Rs 1 Million`, `Rs 750 K`) + Secondary `Lkr.exact(amount)` labelSmall gray (e.g. `Rs 1,000,000.00`) in left Column, `CountdownChip` right — was `Lkr.full()` numbers only
  - Spec `docs/FD-Manager-Spec.md` §4.1 and §4.2 updated: §4.2 now `FD List / FD Cards (FdListScreen + FdCard component)` with Option B description, both copies byte-identical (`/home/user/Docs/FD-Manager-Spec.md` ↔ `docs/`)
  - New docs: `docs/app-wire-diagram.md` + `docs/app-wire-diagram.html` — official Compose names map (HomeScreen, BankSummaryCard, BankMonogram, FdListScreen, FdCard, StatusChip, CountdownChip, FdDetailScreen, AddEditFdScreen, CalendarScreen, SettingsScreen, InstitutionRegistry, Lkr) to avoid misunderstanding
  - README updated to mention wire diagram and FdCard Option B
  - Test helper unchanged (maxSwipes 50, no performScrollTo, gate navgate-0500r9) — still stable

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
### Test suite — 16 tests (existing, no new assertions yet — manual verification of FdCard wording required)
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
- Pass: 0 failures, **16/16 GREEN** expected (same as V20 final)
- Reports XML: `app\build\outputs\androidTest-results\connected\*.xml`
- HTML: `app\build\reports\androidTests\connected\index.html`
- Durations from: `findstr time app\build\outputs\androidTest-results\connected\*.xml` — real numbers, not summed

### Logcat extraction (≤30 lines filtered, must include gate if failure)
PowerShell:
```powershell
adb logcat -d | findstr /i "TestRunner AssertionError summary:National openBank=navgate-0500r9 fdcard" > logcat-v21.txt
type logcat-v21.txt
```
- If GREEN: logcat should show `run started: 16 tests` ... `run finished: 16 tests, 0 failed` — paste that as proof
- If failure: keep ≤30 lines around failure with gate `openBank=navgate-0500r9`

### Screenshots (must be app, not launcher — V17/V18/V19 failed this)
Need 2 screenshots this round:
1. By-institution list (Home) showing Option B 3-line right column `Rs 1 Million / Rs 1,000,000.00 / invested` — same as image-1.png
2. FD list (NSB filtered) showing FD cards with Option B `Rs 1 Million` primary + `Rs 1,000,000.00` secondary + CountdownChip — proves extension works (image-2.png was old numbers-only)

PowerShell:
```powershell
adb shell screencap -p /sdcard/shot-home.png
adb pull /sdcard/shot-home.png .\shot-nsb-v21-home.png
# then navigate into NSB list
adb shell screencap -p /sdcard/shot-list.png
adb pull /sdcard/shot-list.png .\shot-nsb-v21-list.png
```
- Wrong: launcher home with Play Store/Gmail
- Right: app foreground — Home By-institution + FdListScreen FD cards both Option B

### New UI behavior to verify (Option B extended)
- Home hero unchanged: Total invested `Rs 3.2 Million` + `Rs 3,200,000.00`
- By-institution cards: right column 3 lines — Primary `Lkr.words(total)` bold teal, Secondary `Lkr.exact(total)` labelSmall gray, Tertiary `invested`
- **FD cards (FdCard) — NEW in V21:** left Column 2 lines — Primary `Lkr.words(amount)` titleLarge bold primary (e.g. `Rs 1 Million`), Secondary `Lkr.exact(amount)` labelSmall gray (e.g. `Rs 1,000,000.00`), right `CountdownChip` — replaces legacy `Lkr.full()` numbers-only. Matches `docs/app-wire-diagram.md` visual.
- Visual: `mockups/22-bybank-mockups.html` Option B + `docs/app-wire-diagram.html`

## Report spec — `TEST-REPORT-V21.md` at project root
```markdown
# TEST REPORT — <date>
- Branch / commit: dev @ <short SHA> (expect new SHA after d7f2b63)
- Environment: Windows 11, Android Studio <ver>, AVD <name> / API 34, gradle <ver>, JDK <ver>
- Tiers executed: 2 / 2
- Freshness gates: navgate-0500r9 present? yes/no, fdcard-0100r9 present? yes/no (findstr /S /C:"navgate-0500r9" and "fdcard-0100r9")

## Tier 1 — unit tests
| Test class | Tests | Passed | Failed | Duration | Report path |
- Real durations from findstr time

## Tier 2 — instrumented tests
| Test class | Tests | Passed | Failed | Duration | Report path |
- Real durations from findstr time, HTML path, sums to 16

## Evidence of GREEN (if 16/16)
- Logcat final lines: `run started: 16 tests` ... `run finished: 16 tests, 0 failed` pasted (real)
- Screenshots: shot-nsb-v21-home.png is app Home By-institution Option B, shot-nsb-v21-list.png is FdListScreen FD cards Option B (not launcher)
- FdCard verification: NSB-78412 shows `Rs 1 Million` + `Rs 1,000,000.00` (words+exact) per extension

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

Hard rules (V20 violations must be fixed):
- No placeholders — real SHA, real versions, real filtered logcat ≤30 lines pasted (not "See logcat-*.txt"), real XML 3-10 lines pasted (not "Lines 8-18"), real durations from `findstr time` — not summed like `2.56+2.412=9.512`
- Tier2 sums to 16 (5+4+2+1+2+2), Tier1 46
- Screenshots must be app foreground, not launcher home — need 2 shots this round (Home + FD list) proving Option B extended
- Report must reflect FINAL run after flake protocol
- Clean build mandatory `--rerun-tasks`, PowerShell `.\gradlew.bat`
- Freshness gate checks: `findstr /S /C:"navgate-0500r9" app\src\androidTest\java\com\example\fdmanager\TestSupport.kt` and `findstr /S /C:"fdcard-0100r9" app\src\main\java\com\example\fdmanager\ui\components\Components.kt`

## Out of scope
- Firebase/backend, screenshot regression
- No sync step in this file — tester handles `git status` clean → `fetch` → `checkout -B dev origin/dev` → verify SHA
