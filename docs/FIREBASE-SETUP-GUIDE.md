# Firebase Setup Guide — dev project (`fd-manager-dev`)

| | |
|---|---|
| **Version** | 1.0 |
| **Date** | 2026-10-28 |
| **Scope** | Roadmap #8, console side only (owner steps). Gradle/app wiring is the agent's half, shipped separately via PR. |
| **Account** | ALL steps are done signed in as the NEW dedicated Google account (2FA enabled). Never the personal account. |
| **Plan** | Spark (free). If any step asks for a credit card, STOP — something is wrong. |

Decisions this guide implements: separate dev/prod projects (security doc §8, roadmap #24) ·
dev stays Spark forever · email+password auth (roadmap #23) · emulator-first development.

---

## STEP 1 — Create the dev project  (YOU, ~2 min)

1. Open `https://console.firebase.google.com` (signed in as the new account).
2. Click **Create a Firebase project** (first-time accounts may say **Get started** first).
3. Project name: `fd-manager-dev` — exactly. Firebase shows a project ID below the
   name box (`fd-manager-dev` or `fd-manager-dev-xxxxx`) — either is fine.
4. Click **Continue**.
5. Google Analytics screen: toggle **OFF** ("Enable Google Analytics" → disabled).
6. Click **Create project** → wait ~30 s → **Continue**.

**Expected:** the `fd-manager-dev` dashboard opens. No card was requested → you are on Spark.

## STEP 2 — Register the Android app  (YOU, ~2 min)

1. On the project dashboard, click the **Android** icon (robot) under "Get started by
   adding Firebase to your app".
2. **Android package name:** `com.vssbox.fdmanager`  ← copy-paste, must match exactly.
3. **App nickname:** `FD Manager dev`.
4. **Debug signing certificate SHA-1:** leave EMPTY (only needed for Google-sign-in /
   phone auth — we use email+password).
5. Click **Register app**.
6. Click **Download google-services.json** — it lands in your Downloads folder.
7. The console now shows "Add Firebase SDK" Gradle instructions — **IGNORE THEM**.
   Click **Next → Next → Continue to console**. (The Gradle wiring is the agent's half,
   done properly in the PR.)

**Expected:** dashboard shows the app `FD Manager dev` and `google-services.json` is in Downloads.

## STEP 3 — Place the json file locally  (YOU, ~1 min)

Move the downloaded file to the repo, app module root:

```
C:\LocalApps\AppDevelopment\fd-manager\fixed-deposit-manager-android\app\google-services.json
```

(In bash MINGW64: `mv ~/Downloads/google-services.json /c/LocalApps/AppDevelopment/fd-manager/fixed-deposit-manager-android/app/`)

**Do NOT commit it.** The repo is public; the agent's PR adds a `.gitignore` entry so git
will refuse to see it. If `git status` ever shows `google-services.json`, STOP and report.

## STEP 4 — Enable email/password authentication  (YOU, ~1 min)

1. Left menu → **Build → Authentication** → **Get started**.
2. **Sign-in method** tab → **Email/Password** → toggle **Enable** (first toggle only —
   leave "Email link (passwordless)" OFF, per roadmap #23 decision) → **Save**.

**Expected:** Sign-in providers list shows Email/Password = Enabled.

## STEP 5 — Create the Firestore database  (YOU, ~2 min)

1. Left menu → **Build → Firestore Database** → **Create database**.
2. Location: pick `asia-south1 (Mumbai)` — closest region to Sri Lanka; it cannot be
   changed later. Click **Next**.
3. Security rules mode: choose **Start in production mode** (locked — deny all reads/
   writes). Real per-user rules ship with the agent's PR work. Never choose test mode:
   this project's rules posture should match prod from day one.
4. Click **Create**.

**Expected:** empty Firestore "Data" panel; Rules tab shows `allow read, write: if false;`.

## STEP 6 — Report back  (YOU)

Tell the agent: "guide steps done". The agent then ships the code half:
`.gitignore` entry, Gradle plugins/dependencies, emulator-first wiring, and
TEST-INSTRUCTIONS-008 — as a PR for you to merge.

---
*Sensitive info check: NO — package name and console navigation only; no keys, no credentials.*
*History: v1.0 2026-10-28 — initial, covers console-side setup for roadmap #8.*
