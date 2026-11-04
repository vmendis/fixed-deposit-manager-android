# TEST-INSTRUCTIONS-009 — Firebase Auth integration (roadmap #9)

> **What shipped:** real `FirebaseAuthRepository` (email/password + Google Sign-In via
> Credential Manager, reset email, sign-out), selected at runtime by
> `AuthRepositoryProvider` — fake path remains for CI/tests/json-less builds.
> Session gate (`session-0900r9`) for ACCOUNT-HOLDERS ONLY. First persisted pref
> (`DevicePrefs`: has-account flag).
>
> **Decisions recorded (owner, 2026-10-31):** both auth methods offered · gate applies
> only once a real account exists on the device · sign-out HIDES (never wipes) ·
> "persists across restart" = the session (FD storage is roadmap #10). Google Sign-In
> AMENDS the earlier email-password-only mechanics answer.

> **Counts this round: Tier1 = 123 (was 119, +4 SessionGateTest) · Tier2 = 46 (was 44, +2 round 3: password toggle, locked Settings rows).**

---

## §0 STEP 0 — pull  (YOU)
`git checkout master && git pull` after the round's PR merges.

## §1 Freshness gates  (YOU)

| Gate | Proof |
|---|---|
| Gate logic | `app/src/main/java/com/example/fdmanager/domain/SessionGate.kt` exists |
| Real repo | `data/FirebaseAuthRepository.kt`, `data/AuthRepositoryProvider.kt`, `data/GoogleCredentialHelper.kt`, `data/DevicePrefs.kt` exist |
| Google button | `grep -n googleSignInButton app/src/main/java/com/example/fdmanager/ui/screens/AuthScreens.kt` → one hit |
| Test force-fake | `grep -n forceFakeAuth app/src/androidTest/java/com/example/fdmanager/AuthFlowTest.kt` → one hit |
| **Hotfix applied** | `grep -n "val auth by vm.authState" app/src/main/java/com/example/fdmanager/ui/screens/SettingsScreen.kt` → one hit (round 2 fix: Account section was reading the mock flag — real sign-in showed as signed-out, §4 halted at row 5) |
| **Round 3 applied** | `grep -n "sign in to export" app/src/main/java/com/example/fdmanager/ui/screens/SettingsScreen.kt` → one hit (locked data rows) · `grep -n passwordToggle app/src/main/java/com/example/fdmanager/ui/screens/AuthScreens.kt` → ≥2 hits (#30 eye toggle) |

## §2 Tier1  (ROO)
`./gradlew testDebugUnitTest --console=plain` → BUILD SUCCESSFUL, **123 tests**, 0 failures
(new: `SessionGateTest` ×4). CI runs the same on every push — Mode A (json-less) is
proven there automatically.

## §3 Tier2  (ROO)
`./gradlew connectedDebugAndroidTest` on Pixel_7_API_34 → **46 tests**, 0 failures.
`AuthFlowTest` now forces the FAKE auth path at class-load (`@BeforeClass`) — zero
Firebase traffic, zero quota, even with google-services.json present. Round 3 adds `passwordToggleRevealsAndHides` and
`lockedSettingsDataRowsRedirectToSignIn`; all earlier flows and assertions unchanged.

## §4 Real-auth acceptance  (YOU + ROO — needs guide PHASE C done first)

**Prereq:** FIREBASE-SETUP-GUIDE v1.5 PHASE C completed (Google provider ON, SHA-1
registered, FRESH google-services.json in `app/` — the old json has no oauth client).

| # | Step (who) | Expected |
|---|---|---|
| 1 | YOU: app → Settings → Sign in → footer text | "Secured by Firebase Authentication." (NOT the mock line) |
| 2 | YOU: Create account with a REAL email you own + a password | lands back in Settings, signed in, email shown |
| 3 | YOU: console → Authentication → Users | your email listed, provider "password" |
| 4 | YOU: kill app (swipe away), reopen | **still signed in** — acceptance "persists across restart" |
| 5 | YOU: Settings → Sign out → tap Home tab | **session gate** (`Signed out — … Sign in to continue`), FAB hidden — acceptance "sign out hides all data" |
| 6 | YOU: Sign in again | data screens return |
| 7 | YOU: Sign out → Sign in screen → Forgot password with your email | "If an account exists…" message; **real email arrives** (check spam) — acceptance "reset email flow works" |
| 8 | YOU: reset via the email link, sign in with the NEW password | works |
| 9 | YOU: Sign out → "Continue with Google" | Google account picker → signed in; console Users shows provider "google.com" |
| 10 | ROO: logcat during steps 2–9 | no crashes, no `FirebaseProbe` errors |

**Gate sanity (the opt-in promise):** on a FRESH install (or after wiping app storage):
no account → Home/Calendar/Add all fully usable, no sign-in nag, no gate. The gate may
only ever appear AFTER step 2 has happened on the device.

## §4b Round 3 re-check  (YOU — after the round-3 PR merges; full §4 already PASSED 2026-11-04)

| # | Step (who) | Expected |
|---|---|---|
| 1 | YOU: sign in, then Sign out → Settings → scroll to Data section | Export row reads "Signed out - sign in to export"; Delete row "Signed out - sign in to manage data"; Reset demo row "Signed out - sign in to reset data" |
| 2 | YOU: tap the locked Export row | NO file picker — lands on the Sign in screen |
| 3 | YOU: tap the locked Delete row | NO wipe dialog — lands on the Sign in screen |
| 4 | YOU: sign back in → Settings → Data section | all three rows back to normal subtitles and actions |
| 5 | YOU: Sign out → Sign in screen → Forgot password | copy reads "Enter your email and we'll send you a password reset link." — the words "mock" and "no backend" appear NOWHERE |
| 6 | YOU: submit with your email | confirmation reads "…a reset link has been sent. Check your inbox - and the spam folder." (no "(mock)") |
| 7 | YOU: Sign in screen → eye icon in the password field | tap reveals the typed password, tap again masks it; Create account has one per field (password + confirm, independent) |

## §5 Evidence  (ROO → `test-artifacts-009/`, local only)
`tier1.txt` (123/0) · `tier2.txt` (46/0) · screenshot: session gate · screenshot:
console Users list (both providers) · `logcat-auth.txt`

---
*Sensitive info check: NO — no keys/secrets; testers use their own emails; SHA-1 is not a secret (it's a public certificate fingerprint).*
