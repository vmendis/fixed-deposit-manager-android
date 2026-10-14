# TEST INSTRUCTIONS — Issue #21 Back from bank list top cut off — Round V23

## Environment
- Branch: `dev` (fresh from `origin/master` @ 352523c + #21 scroll fix)
- Commit: `git log -1 --oneline` — tester records actual SHA (expect new SHA after 352523c)
- Freshness gates: `OPEN_BANK_GATE = navgate-0500r9` in `TestSupport.kt`, `FDCARD_PAYOUT_GATE = fdcard-0200r9` in `Components.kt`, `HOME_SCROLL_GATE = home-scroll-0100r9` in `HomeScreen.kt`
- Windows 11, Android Studio, AVD Pixel_7_API_34 / API 34, `adb devices` shows device
- Project root `fd-manager/` — all commands via `.\gradlew.bat` in PowerShell or `gradlew.bat` in CMD

## Why V23 — Issue #21 BUG
**Title:** BUG: Back from bank list returns to Home with top cut off (hero not visible, mid-scroll)

**Repro (from roadmap #21 body):**
1. Home → scroll down to By institution
2. Tap Sampath Bank → bank list (FdListScreen) opens
3. Tap back arrow (app bar) → returns to Home
4. **Observed:** Home top cut off — Total invested hero not visible, first visible item NSB maturing card clipped at top edge under app bar
5. **Expected:** back from bank list should land with Total invested hero visible at top, not clipped

**Likely cause (from issue):** HomeScreen LazyColumn uses remembered/saved LazyListState — scroll position preserved across navigation via NavHost saveState=true/restoreState=true. Standard behavior but reads as cut off.

**Fix implemented in dev:**
- `HomeScreen.kt`: explicit `val listState = rememberLazyListState()` (non-saveable) + `LaunchedEffect(Unit) { listState.scrollToItem(0) }` — gate `home-scroll-0100r9`
- When Home re-enters composition after popBackStack from FdListScreen, it scrolls to top, hero visible
- Bottom tab navigation (Home ↔ Calendar ↔ Settings) still preserves state via `saveState=true/restoreState=true` in MainActivity — only bank back resets
- Spec §4.1 Behavior updated with back navigation fix note, both copies byte-identical
- Wire diagram md updated with listState + gate

**No change to FdCard payout (V22) or Option B wording — those remain: Rate • Tenor • Payout • Matures + Rs 1 Million / Rs 1,000,000.00**

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
| `NavigationFlowTest` | 2 | Home → list → detail → back — this test should now pass with hero visible after back |
| `RenewalChainTest` | 1 | renew chain |
| `SoftDeleteRestoreTest` | 2 | soft delete |
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

### Logcat extraction (≤30 lines filtered, must include gates if failure)
PowerShell:
```powershell
adb logcat -d | findstr /i "TestRunner AssertionError summary:National openBank=navgate-0500r9 fdcard home-scroll" > logcat-v23.txt
type logcat-v23.txt
```
- If GREEN: `run started: 16 tests` ... `run finished: 16 tests, 0 failed`

### Screenshots (must be app, not launcher)
Need 2 screenshots for #21:
1. **Home after back from bank list** — showing Total invested hero visible at top (not clipped), with By institution below — proves fix
2. **FD list with payout** — showing `Monthly payout / At maturity` still present (from V22)

PowerShell:
```powershell
adb shell screencap -p /sdcard/shot-home-after-back.png
adb pull /sdcard/shot-home-after-back.png .\shot-nsb-v23-home-after-back.png
adb shell screencap -p /sdcard/shot-list.png
adb pull /sdcard/shot-list.png .\shot-nsb-v23-list.png
```
- Wrong: launcher home with Play Store/Gmail
- Right: app Home with hero visible after back

### New behavior to verify
- Repro steps 1-3 from issue #21: after back, hero `Total invested Rs X Million` visible, not clipped NSB card under app bar
- FdCard still shows `10% p.a. • 12 months • Monthly payout • matures ...` (V22)
- By-institution cards still Option B `Rs 1 Million / Rs 1,000,000.00 / invested`

## Report spec — `TEST-REPORT-V23.md` at project root
```markdown
# TEST REPORT — <date>
- Branch / commit: dev @ <short SHA>
- Environment: Windows 11, Android Studio <ver>, AVD <name> / API 34, gradle <ver>, JDK <ver>
- Tiers executed: 2 / 2
- Freshness gates: navgate-0500r9 yes, fdcard-0200r9 yes, home-scroll-0100r9 yes

## Tier 1 — unit tests
| Test class | Tests | Passed | Failed | Duration | Report path |
- Real durations

## Tier 2 — instrumented tests
| Test class | Tests | Passed | Failed | Duration | Report path |
- Real, sums to 16

## Evidence of GREEN + #21 fix
- Logcat final lines: run started: 16 ... run finished: 16 tests, 0 failed
- Screenshots: shot-nsb-v23-home-after-back.png shows Home hero visible after back from bank list (not clipped), shot-nsb-v23-list.png shows payout
- Manual verification: repro steps from #21 no longer show clipped hero

## Failures (if any)
...

## Blockers
...

## Not covered
...
```

Hard rules:
- No placeholders — real SHA, real versions, real logcat ≤30 lines pasted, real durations from findstr time
- Tier2 sums to 16, Tier1 46
- Screenshots app foreground, not launcher, need 2 shots proving #21 fix + payout still there
- Clean build mandatory --rerun-tasks
- Freshness gates: navgate-0500r9, fdcard-0200r9, home-scroll-0100r9

## Out of scope
- Firebase/backend, branding on hold, screenshot regression
- No sync step — tester handles git status clean → fetch → checkout -B dev origin/dev → verify SHA
