# TEST INSTRUCTIONS — Issue #1 Onboarding funnel & empty states — Round V25

## Environment
- Branch: `dev` (fresh from `origin/master` @ 6574e92 + #1 onboarding funnel + empty states + demo opt-in)
- Commit: `git log -1 --oneline` — tester records actual SHA (expect new SHA after 6574e92, current dev @ d0b43f6 after DEMO_GATE fix)
- Freshness gates: `OPEN_BANK_GATE = navgate-0500r9` in `TestSupport.kt`, `FDCARD_PAYOUT_GATE = fdcard-0200r9` in `Components.kt`, `HOME_SCROLL_GATE = home-scroll-0100r9` in `HomeScreen.kt`, `ONBOARDING_GATE = onboarding-0100r9` + `DEMO_GATE = demo-0100r9` in `FdRepository.kt` companion object, `VSSBOX_CUBE = vssbox_cube.png` drawable exists, `EMPTY_CTA = emptyStateAction` in `Components.kt`
- Windows 11, Android Studio, AVD Pixel_7_API_34 / API 34, `adb devices` shows device
- Project root `fd-manager/` — all commands via `.\gradlew.bat` in PowerShell or `gradlew.bat` in CMD
- **CRITICAL RULE — Executor (Roo) must NOT edit app code**: Stop editing app code. Do NOT fix FdRepository.kt or any *.kt file. Report formatting issue as CODE-BUG if gate fails, don't edit. You are Executor — never edit app code, never invent selectors. Only Architect (arena.ai) edits code. If you see formatting issue or missing DEMO_GATE, record SELECTOR-MISSING or CODE-BUG in report, do not fix.

## Why V25 — Issue #1 Onboarding funnel & empty states
**Title:** New-user onboarding funnel & empty states

**Scope (from roadmap #1 body):**
- Welcome screen on first launch with plain-English trust line ("we never ask for your bank login; your data is encrypted and only you can see it")
- Guided "Add your first FD" flow leading straight into the form
- Proper empty states for Home, FD list, Calendar and Recycle bin (illustration-free is fine; clear copy + primary action button)
- Sample data becomes an opt-in demo mode (Settings toggle) instead of always-seeded

**Implementation #1 in dev:**
- **SettingsStore:** `hasSeenOnboarding` default **false** (fresh install shows welcome per #1), `isDemoMode` default **false** (opt-in, not always-seeded) — gates `onboarding-0100r9` + `demo-0100r9`. Tests set both true in @Before to keep GREEN.
- **FdRepository:** `get()` initial empty if demo off, SampleData.seed if demo on (checks SettingsStore flag). `setDemoMode(enabled, sample)`: on → add fd1..fd8 if missing, keep user FDs; off → remove fd1..fd8, keep user FDs — preserves user data. `clearAll()` for clean start. `reset()` still exists for tests.
- **FdViewModel:** `toggleDemoMode(enabled)` sets SettingsStore + repo setDemoMode, `clearAllFds()`, `setDemoMode()` delegates to toggle (preserves user data), `setHasSeenOnboarding()`
- **MainActivity:** NavHost startDestination = ONBOARDING if !hasSeenOnboarding else HOME. OnboardingScreen: Get Started → setHasSeenOnboarding(true) + navigate ADD with popUpTo ONBOARDING inclusive (leads straight into form), Explore demo → setHasSeenOnboarding(true) + toggleDemoMode(true) + navigate HOME. This satisfies fresh install → welcome → add first FD → FD visible on Home.
- **OnboardingScreen (existing #27 + extended #1):** 96dp cube `vssboxLogoOnboarding`, vssbox name, FD Manager, by vssbox tagline, trustCard with lock + "Your data stays yours" + trust line "We never ask for your bank login...", FeatureRow x3, Button Get Started testTag `getStartedButton` → ADD, TextButton Explore demo testTag `exploreDemoButton` → HOME + demo true, footer builtByVssboxFooter 16dp cube + © 2026
- **EmptyState component:** Enhanced to `EmptyState(icon, title, message, actionLabel?, onAction?)` — optional Button with testTag `emptyStateAction:$title`
- **HomeScreen:** Empty when summaries empty: "No deposits yet" + "Welcome! Add your first fixed deposit to see totals, maturing soon, and by institution. Your data is encrypted and only you can see it." (or demo hint if demo on) + actionLabel "Add your first FD" → onAddFd (navigates to ADD), testTag `emptyStateAction:No deposits yet`. Also has onAddFd param. Footer Built by vssbox still.
- **FdListScreen:** Empty "No FDs here" + "This institution has no deposits. Add your first FD for ${bank} or try demo mode in Settings." + action "Add FD" → onAdd when filter ALL, else filter message, no action
- **CalendarScreen:** Empty "Pick a day" + "Days with dots have FD maturities. Add FDs to see them here." and "Quiet day" + "No FDs mature on ${date}. Add an FD or check another date." — illustration-free, clear copy, no CTA needed (pick day)
- **RecycleBinScreen:** Empty "Bin is empty" + "Deleted FDs land here thanks to soft-delete — restored with one tap." — clear copy
- **SettingsScreen:** Demo section with Switch isDemoMode, subtitle Sample FDs visible / Hide sample data — clean start, plus Reset demo data and Show onboarding rows
- **Tests:** All instrumented tests @Before now set `SettingsStore.setHasSeenOnboarding(true)` + `setDemoMode(true)` + `FdRepository.reset(sample)` to keep 46/16 GREEN despite defaults false

**Acceptance #1:**
- Fresh install (hasSeenOnboarding=false, isDemoMode=false) → welcome → Get Started → Add first FD → Save → FD visible on Home with correct totals
- Every screen has deliberate empty state with CTA where applicable
- Demo mode on/off works without corrupting user data — user FDs survive toggle
- Tests still GREEN 46/16

**No change to #27 vssbox branding (cube, by vssbox, footer, About) — remains**

## Tier 1 — JVM unit tests
PowerShell:
```powershell
.\gradlew.bat cleanTestDebugUnitTest testDebugUnitTest --rerun-tasks
```
- Expected: 46 tests (FdMath 11, FdRepository 17, BankRegistry 7, InstitutionRegistry 11) — all GREEN
- Durations from: `findstr time app\build\test-results\testDebugUnitTest\*.xml`

## Tier 2 — Instrumented Compose UI tests
### Test suite — 16 tests (existing, now with SettingsStore flags set true in @Before)
| Class | Tests | Covers |
|---|---|---|
| `AddFdValidationTest` | 5 | form validation, payout required, tenor |
| `BankMonogramUiTest` | 4 | monogram tags |
| `NavigationFlowTest` | 2 | Home → list → detail → back — hero visible after back still, now with onboarding true |
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
adb logcat -d | findstr /i "TestRunner AssertionError summary:National openBank=navgate-0500r9 fdcard home-scroll onboarding demo emptyStateAction" > logcat-v25.txt
type logcat-v25.txt
```
- If GREEN: `run started: 16 tests` ... `run finished: 16 tests, 0 failed`

### Screenshots (must be app, not launcher — fix timing!)
Need 4 screenshots for #1 + #27:
1. **Onboarding welcome with vssbox + trust line** — 96dp cube + vssbox + FD Manager + by vssbox + trustCard "Your data stays yours" + "We never ask for your bank login..." + Get Started button + footer Built by vssbox
   - How: Fresh install (hasSeenOnboarding=false, isDemoMode=false) OR Settings → Show onboarding → restart app
   - Verify: `adb shell dumpsys window | findstr mCurrentFocus` → `com.example.fdmanager/.MainActivity`
2. **Home empty state with CTA** — No deposits yet + Welcome! Add your first... + Button Add your first FD + footer Built by vssbox
   - How: Fresh install with demo off, no FDs, Home shows empty state
3. **Home after adding first FD** — Total invested hero visible + By institution + footer + FD visible with correct totals (fresh install → welcome → Get Started → Add FD → Save → Home)
4. **Settings Demo toggle** — Demo section showing Switch isDemoMode + subtitle Sample FDs visible / Hide sample data + Show onboarding row + About 72dp cube + vssbox + ©

PowerShell (correct timing — app foreground):
```powershell
adb shell dumpsys window | findstr mCurrentFocus
adb shell screencap -p /sdcard/shot-onboarding.png
adb pull /sdcard/shot-onboarding.png .\shot-nsb-v25-onboarding.png
adb shell screencap -p /sdcard/shot-home-empty.png
adb pull /sdcard/shot-home-empty.png .\shot-nsb-v25-home-empty.png
adb shell screencap -p /sdcard/shot-home-after-first.png
adb pull /sdcard/shot-home-after-first.png .\shot-nsb-v25-home-after-first.png
adb shell screencap -p /sdcard/shot-settings-demo.png
adb pull /sdcard/shot-settings-demo.png .\shot-nsb-v25-settings-demo.png
```
- Wrong: launcher home with Play Store/Gmail/Photos/YouTube
- Right: app onboarding/Home empty/Home with FD/Settings with vssbox cube + demo toggle

### New behavior to verify
- Fresh install (hasSeenOnboarding=false, isDemoMode=false) → onboarding shows vssbox cube 96dp + trust line → Get Started → Add FD form → Save → Home shows Total invested hero + FD visible with correct totals
- Home empty state has Button "Add your first FD" with testTag emptyStateAction:No deposits yet
- FdList empty has Button Add FD when filter ALL
- Calendar empty has clear copy "Pick a day" / "Quiet day"
- RecycleBin empty "Bin is empty" + soft-delete message
- Demo mode toggle on → sample FDs appear (fd1..fd8) + user FDs preserved, off → sample removed, user FDs remain (no corruption)
- Onboarding Explore demo → Home with demo true + sample visible
- Settings About still shows 72dp cube + vssbox + © 2026
- FdCard still shows payout `Monthly payout / At maturity` per #28
- Back from bank list still shows hero visible per #21

## Report spec — `TEST-REPORT-V25.md` at project root
```markdown
# TEST REPORT — <date>
- Branch / commit: dev @ <short SHA>
- Environment: Windows 11, Android Studio <ver>, AVD <name> / API 34, gradle <ver>, JDK <ver>
- Tiers executed: 2 / 2
- Freshness gates: navgate-0500r9 yes, fdcard-0200r9 yes, home-scroll-0100r9 yes, onboarding-0100r9 yes, demo-0100r9 yes, vssbox_cube.png yes, emptyStateAction yes

## Tier 1 — unit tests
| Test class | Tests | Passed | Failed | Duration | Report path |
- Real durations from findstr time

## Tier 2 — instrumented tests
| Test class | Tests | Passed | Failed | Duration | Report path |
- Real, sums to 16

## Evidence of GREEN + #1 fix
- Logcat final lines: run started: 16 ... run finished: 16 tests, 0 failed
- Screenshots: shot-nsb-v25-onboarding.png shows onboarding 96dp cube + trust line + Get Started, shot-nsb-v25-home-empty.png shows Home empty No deposits yet + Add your first FD CTA + footer, shot-nsb-v25-home-after-first.png shows Home after first FD with Total invested hero + FD visible, shot-nsb-v25-settings-demo.png shows Settings Demo toggle + About 72dp cube
- Manual verification: fresh install → welcome → add first FD → FD visible on Home with correct totals, empty states with CTA, demo on/off preserves user data

## Failures (if any)
...

## Blockers
...

## Not covered
...
```

Hard rules:
- **Executor must NOT edit app code** — Stop editing app code. Do NOT fix FdRepository.kt or any *.kt. Report as CODE-BUG / SELECTOR-MISSING if gate fails, don't edit. Architect (arena.ai) only edits code. Roo tried to fix DEMO_GATE formatting — forbidden.
- No placeholders — real SHA, real versions, real logcat ≤30 lines pasted, real durations from findstr time (like 0.014, 10.038), not rounded 1.2s
- Tier2 sums to 16, Tier1 46
- Screenshots app foreground, not launcher, need 4 shots proving #1 onboarding funnel + empty CTA + first FD + demo toggle — verify with dumpsys mCurrentFocus
- Clean build mandatory --rerun-tasks
- Freshness gates: navgate-0500r9, fdcard-0200r9, home-scroll-0100r9, onboarding-0100r9, demo-0100r9, vssbox_cube.png, emptyStateAction

## Out of scope
- Firebase/backend, screenshot regression
- No sync step — tester handles git status clean → fetch → checkout -B dev origin/dev → verify SHA
