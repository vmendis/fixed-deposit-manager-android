# Firebase Setup Guide — dev project (`fd-manager-dev`)

| | |
|---|---|
| **Version** | 1.5 |
| **Date** | 2026-10-28 |
| **Scope** | Roadmap #8. Steps 1–4 = console side (owner). Steps 5–7 = repo side, ONLY after the agent's code PR is merged. |
| **Account** | ALL steps are done signed in as the NEW dedicated Google account (2FA enabled). Never the personal account. |
| **Plan** | Spark (free). If any step asks for a credit card, STOP — something is wrong. |

Decisions this guide implements: separate dev/prod projects (security doc §8, roadmap #24) ·
dev stays Spark forever · email+password auth (roadmap #23) · emulator-first development.

---

# PHASE A — Firebase console (YOU, no repo involved)

## STEP 1 — Create the dev project  ✅ DONE 2026-10-28

Created `fd-manager-dev`, Google Analytics OFF, Spark plan verified (no card requested;
"Spark · No-cost ($0/month)" badge visible).

## STEP 2 — Register the Android app  (YOU, ~2 min)

1. On the project dashboard, click **+ Add app** (button directly under the project
   name), then pick the **Android** icon from the platform choices.
   Ignore all "Next steps with Gemini" cards and the Gemini side panel — marketing,
   not setup.
2. **Android package name:** `com.vssbox.fdmanager`  ← copy-paste, must match exactly.
3. **App nickname:** `FD Manager dev`.
4. **Debug signing certificate SHA-1:** leave EMPTY (only needed for Google-sign-in /
   phone auth — we use email+password).
5. Click **Register app**.
6. Click **Download google-services.json** — **leave it in Downloads for now.**
   It goes into the repo only at STEP 6, after the agent's PR is merged.
7. The console now shows "Add Firebase SDK" Gradle instructions — **IGNORE THEM**.
   Click **Next → Next → Continue to console**. (The Gradle wiring is the agent's half.)

**Expected:** dashboard shows the app `FD Manager dev`; `google-services.json` sits in Downloads.

## STEP 3 — Enable email/password authentication  (YOU, ~1 min)

1. Left menu → **Build → Authentication** → **Get started**.
2. **Sign-in method** tab → **Email/Password** → toggle **Enable** (first toggle only —
   leave "Email link (passwordless)" OFF, per roadmap #23 decision) → **Save**.

**Expected:** Sign-in providers list shows Email/Password = Enabled.

## STEP 4 — Create the Firestore database  (YOU, ~2 min)

1. Left menu → **Build → Firestore Database** → **Create database**.
   If asked to choose an edition: **Standard** (Spark free tier + Android SDK + security
   rules live there; Enterprise is the MongoDB-compatible offering — wrong tool, not free).
2. Location: pick `asia-south1 (Mumbai)` — per roadmap #8; closest region to Sri Lanka;
   it cannot be changed later. Click **Next**.
3. Security rules mode: choose **Start in production mode** (locked — deny all reads/
   writes). Real per-user rules ship with later agent work. Never choose test mode:
   this project's rules posture should match prod from day one.
4. **Scheduled backups (optional)** step: leave **Daily** and **Weekly** UNCHECKED.
   They require the Blaze plan (the console shows exactly that warning) — do NOT click
   "Upgrade project". The dev project needs no backups: its data is disposable test data.
   📌 **PROD NOTE (recorded for later):** when `fd-manager-prod` is created on Blaze,
   **enable Daily backups** there — real user data, even ciphertext, deserves them.
5. Click **Create**.

**Expected:** empty Firestore "Data" panel; Rules tab shows `allow read, write: if false;`.

→ **Report "Phase A done" to the agent.** The agent then ships the code PR:
`.gitignore` entry for the json, Gradle plugin + Firebase dependencies (build stays
green WITHOUT the json — CI has no secrets), debug-build Firestore connectivity probe
(roadmap #8 acceptance), TEST-INSTRUCTIONS-008.

# PHASE C — Google Sign-In enablement (YOU, added in v1.5 for roadmap #9)

The roadmap #9 decision (2026-10-31) offers BOTH auth methods — email/password AND
Google Sign-In. Google requires two things we previously skipped: the Google provider
toggle, and the debug SHA-1 fingerprint. One console round + one local command.

## STEP C1 — Enable the Google provider  (YOU, ~1 min)
1. Console → **Build → Authentication → Sign-in method** → **Add new provider** →
   **Google** → toggle **Enable**.
2. It asks for a public-facing name + support email — pick the project's dedicated
   Google account address → **Save**.

**Expected:** providers list shows Email/Password = Enabled AND Google = Enabled.

## STEP C2 — Print the debug SHA-1  (YOU, one command, in the repo root)
```
./gradlew signingReport | grep -B2 -A3 "Variant: debug" | head -12
```
**Expected:** a block containing `SHA1: XX:XX:…` (20 hex pairs). Copy that SHA1 line.
(SHA-1 is a public certificate fingerprint, not a secret.)

## STEP C3 — Register the fingerprint  (YOU, ~1 min)
1. Console → gear icon → **Project settings** → **Your apps** → app `FD Manager dev` →
   **Add fingerprint** → paste the SHA-1 → **Save**.

## STEP C4 — REPLACE the json  (YOU, ~1 min — the old file has no OAuth client)
1. On the same Project settings page → **Download google-services.json**.
2. Overwrite the repo copy:
   `mv ~/Downloads/google-services.json /c/LocalApps/AppDevelopment/fd-manager/fixed-deposit-manager-android/app/`
3. `git status --short` → must NOT list `google-services.json`.

**Expected after C1–C4:** the app's sign-in screen "Continue with Google" button opens
the Google account picker instead of an error.

# PHASE B — Repo side (YOU, only AFTER the agent's PR is merged)

## STEP 5 — STEP 0 rule: pull  (YOU)

The agent gives the exact pull command when the PR merges. Do not place any file
before this step — the `.gitignore` protection does not exist in your working copy
until you pull.

## STEP 6 — Place the json  (YOU, ~1 min)

Move the file from Downloads to the app module root:

```
C:\LocalApps\AppDevelopment\fd-manager\fixed-deposit-manager-android\app\google-services.json
```

(bash MINGW64: `mv ~/Downloads/google-services.json /c/LocalApps/AppDevelopment/fd-manager/fixed-deposit-manager-android/app/`)

**Verify it is invisible to git:** `git status` must NOT list `google-services.json`.
If it appears, STOP and report — do not commit, do not push.

## STEP 7 — Build, run, verify connectivity  (YOU + ROO)

Per TEST-INSTRUCTIONS-008 (ships with the PR): debug build in Android Studio, run on
Pixel_7_API_34, check Logcat for the Firestore reachability line. Expected outcomes
and the exact Logcat filter are in the test instructions.

---
*Sensitive info check: NO — package name and console navigation only; no keys, no credentials.*
*History: v1.0 2026-10-28 initial · v1.1 2026-10-28 Step 2.1 matched to 2026 console UI ·
v1.2 2026-10-28 resequenced: json placement moved AFTER the agent's code PR + pull (old
Step 3 trap removed — .gitignore must exist locally first); phases A/B introduced;
Firestore probe tied to roadmap #8 acceptance. · v1.3 2026-10-28 Step 4.1: edition choice = Standard. · v1.4 2026-10-28 Step 4.4: scheduled backups = skip on dev (Blaze-only); prod note = enable Daily on fd-manager-prod. · v1.5 2026-10-31 PHASE C added (roadmap #9): Google provider toggle, SHA-1 via signingReport, fingerprint registration, json REPLACEMENT.*
