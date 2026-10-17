# TEST INSTRUCTIONS — Issue #27 Branding vssbox + Onboarding — Round V24

## Environment
- Branch: `dev` (fresh from `origin/master` @ 64a863e + #27 vssbox branding + onboarding)
- Commit: `git log -1 --oneline` — tester records actual SHA (expect new SHA after 64a863e)
- Freshness gates: `OPEN_BANK_GATE = navgate-0500r9` in `TestSupport.kt`, `FDCARD_PAYOUT_GATE = fdcard-0200r9` in `Components.kt`, `HOME_SCROLL_GATE = home-scroll-0100r9` in `HomeScreen.kt`, `ONBOARDING_GATE = onboarding-0100r9` in `OnboardingScreen.kt`, `VSSBOX_CUBE = vssbox_cube.png` drawable exists
- Windows 11, Android Studio, AVD Pixel_7_API_34 / API 34, `adb devices` shows device
- Project root `fd-manager/` — all commands via `.\gradlew.bat` in PowerShell or `gradlew.bat` in CMD

## Why V24 — Issue #27 Branding
**Title:** Branding — vssbox logo + company name placement

**Context:** User uploaded logo (blue neon isometric cube, dark bg) — `/home/user/uploads/vssbox_logo.jpg` in workspace. Company name: **vssbox**. App name stays FD Manager, company by vssbox.

**Own creations used (from supplied JPG):**
- `docs/branding/vssbox_logo_transparent_manual.png` — 1024x1024 RGBA, corner 0,0,0,0 transparent, center 185,217,249,255 — best transparent, used as `app/src/main/res/drawable/vssbox_cube.png` (408K)
- Other attempts: `vssbox_transparent_final.png` corner alpha 35 semi-transparent, `vssbox_transparent_final2.png` alpha 0 but dark tint, `vssbox_cube_transparent_exact.png` RGB not RGBA — discarded

**Implementation #27:**
- **Asset:** `vssbox_cube.png` 1024x1024 transparent manual → drawable, `ic_launcher_background` #0B1220 matches logo dark bg, `vssbox_blue` #5B9CF6 accent
- **Launcher icon:** `drawable/ic_launcher.xml` fallback layer-list dark bg + cube 72dp centered (< API 26), `drawable-anydpi-v26/ic_launcher.xml` adaptive bg #0B1220 + fg vssbox_cube + monochrome, `mipmap-anydpi-v26/ic_launcher.xml` same adaptive — blue cube on dark bg
- **OnboardingScreen (new-user, #1 + #27):** new file `ui/screens/OnboardingScreen.kt` gate `onboarding-0100r9`
  - 96dp cube Image testTag `vssboxLogoOnboarding`
  - `vssbox` headlineMedium bold primary testTag `vssboxNameOnboarding` + `FD Manager` titleLarge + `by vssbox • Secure box for your deposits` labelMedium
  - Trust card ElevatedCard testTag `trustCard`: lock icon + "Your data stays yours" + trust line "We never ask for your bank login. Your data is encrypted and only you can see it. Runs entirely on local sample data in this mock — no account, no backend, no real money."
  - Features 3 rows: Track all FDs CBSL-only (24 LCB + 6 LSB + 32 LFC), Rate•Tenor•Payout•Matures, Renewal chain & Recycle bin
  - CTA Button "Get Started — Add your first FD" testTag `getStartedButton` → `setHasSeenOnboarding(true)`, TextButton "Explore demo data first" testTag `exploreDemoButton`
  - Footer Row testTag `builtByVssboxFooter`: 16dp cube + "Built by vssbox • © 2026 vssbox"
  - Logic: `SettingsStore.hasSeenOnboarding` default **true** for existing tests GREEN, set false to show onboarding on fresh install. `MainActivity` checks flag, if false shows OnboardingScreen full-screen, else normal NavHost. Settings → Show onboarding row sets flag false for manual replay.
- **HomeScreen:** TopAppBar title Column "FD Manager" bold + "by vssbox" labelSmall onPrimary 80% — keeps product, adds company. Footer Row testTag `builtByVssboxHome`: 16dp cube + "Built by vssbox • © 2026 vssbox • FD Manager by vssbox" labelSmall gray centered
- **SettingsScreen → About:** 72dp cube Image testTag `vssboxLogoAbout` + vssbox titleLarge bold primary + FD Manager titleMedium + by vssbox labelSmall + Version 1.0 · front-end mock · © 2026 vssbox + description Secure box... + Row Built by vssbox • vssbox.com (coming soon) testTag `aboutBranding`. Demo mode toggle `isDemoMode` + Show onboarding row.
- **Spec:** §4.7 Branding — vssbox added, both copies identical
- **Wire diagram:** updated with OnboardingScreen, Home footer, Settings About, file map includes vssbox_cube.png + ic_launcher adaptive

**No change to FdCard payout (V22) or Home scroll fix (V23) — those remain**

## Tier 1 — JVM unit tests
PowerShell:
```powershell
.\gradlew.bat cleanTestDebugUnitTest testDebugUnitTest --rerun-tasks
```
- Expected: 46 tests (FdMath 11, FdRepository 17, BankRegistry 7, InstitutionRegistry 11) — all GREEN
- Durations from: `findstr time app\build\test-results\testDebugUnitTest\*.xml`

## Tier 2 — Instrumented Compose UI tests
### Test suite — 16 tests (existing, onboarding default true so they still pass)
| Class | Tests | Covers |
|---|---|---|
| `AddFdValidationTest` | 5 | form validation, payout required, tenor |
| `BankMonogramUiTest` | 4 | monogram tags |
| `NavigationFlowTest` | 2 | Home → list → detail → back — hero visible after back still |
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
adb logcat -d | findstr /i "TestRunner AssertionError summary:National openBank=navgate-0500r9 fdcard home-scroll onboarding vssbox" > logcat-v24.txt
type logcat-v24.txt
```
- If GREEN: `run started: 16 tests` ... `run finished: 16 tests, 0 failed`

### Screenshots (must be app, not launcher — fix timing!)
Need 3 screenshots for #27:
1. **Onboarding with vssbox branding** — show 96dp cube + vssbox name + FD Manager + trust card + Get Started button + footer Built by vssbox
   - How: In Settings → Show onboarding → restart app OR set `hasSeenOnboarding=false` via code, then take screenshot when onboarding visible
   - Verify: `adb shell dumpsys window | findstr mCurrentFocus` should show `com.example.fdmanager/.MainActivity`, not launcher
2. **Home after onboarding with footer** — Home hero Total invested + By institution + footer Built by vssbox with cube 16dp visible at bottom
3. **Settings About with 72dp cube** — About section showing 72dp cube + vssbox + version + ©

PowerShell (correct timing — app foreground):
```powershell
adb shell dumpsys window | findstr mCurrentFocus
adb shell screencap -p /sdcard/shot-onboarding.png
adb pull /sdcard/shot-onboarding.png .\shot-nsb-v24-onboarding.png
adb shell screencap -p /sdcard/shot-home-footer.png
adb pull /sdcard/shot-home-footer.png .\shot-nsb-v24-home-footer.png
adb shell screencap -p /sdcard/shot-about.png
adb pull /sdcard/shot-about.png .\shot-nsb-v24-about.png
```
- Wrong: launcher home with Play Store/Gmail/Photos/YouTube icons (like V23 failure)
- Right: app onboarding/Home/Settings with vssbox cube visible

### New behavior to verify
- Fresh install (hasSeenOnboarding=false) → onboarding shows vssbox cube 96dp + vssbox name + trust line + Get Started → tap Get Started → Home shows hero visible + footer Built by vssbox + cube
- Home TopAppBar shows "FD Manager" + "by vssbox" subtitle
- Settings About shows 72dp cube + vssbox + © 2026 vssbox
- Launcher icon shows blue cube on dark bg #0B1220 (not teal)
- FdCard still shows `10% p.a. • 12 months • Monthly payout • matures ...` (V22)
- By-institution cards still Option B `Rs 1 Million / Rs 1,000,000.00 / invested`
- Back from bank list still shows hero visible (V23 fix)

## Report spec — `TEST-REPORT-V24.md` at project root
```markdown
# TEST REPORT — <date>
- Branch / commit: dev @ <short SHA>
- Environment: Windows 11, Android Studio <ver>, AVD <name> / API 34, gradle <ver>, JDK <ver>
- Tiers executed: 2 / 2
- Freshness gates: navgate-0500r9 yes, fdcard-0200r9 yes, home-scroll-0100r9 yes, onboarding-0100r9 yes, vssbox_cube.png yes

## Tier 1 — unit tests
| Test class | Tests | Passed | Failed | Duration | Report path |
- Real durations

## Tier 2 — instrumented tests
| Test class | Tests | Passed | Failed | Duration | Report path |
- Real, sums to 16

## Evidence of GREEN + #27 fix
- Logcat final lines: run started: 16 ... run finished: 16 tests, 0 failed
- Screenshots: shot-nsb-v24-onboarding.png shows onboarding with 96dp cube + vssbox + trust card + Get Started + footer Built by vssbox, shot-nsb-v24-home-footer.png shows Home footer with cube + Built by vssbox, shot-nsb-v24-about.png shows About 72dp cube + vssbox + ©
- Manual verification: launcher icon blue cube dark bg, onboarding branding, Home by vssbox subtitle, footer, About 72dp

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
- Screenshots app foreground, not launcher, need 3 shots proving #27 branding (onboarding 96dp + Home footer + About 72dp) — verify with dumpsys mCurrentFocus
- Clean build mandatory --rerun-tasks
- Freshness gates: navgate-0500r9, fdcard-0200r9, home-scroll-0100r9, onboarding-0100r9, vssbox_cube.png exists

## Out of scope
- Firebase/backend, screenshot regression
- No sync step — tester handles git status clean → fetch → checkout -B dev origin/dev → verify SHA
