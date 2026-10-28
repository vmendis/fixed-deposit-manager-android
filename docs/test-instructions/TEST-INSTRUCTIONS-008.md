# TEST-INSTRUCTIONS-008 — Firebase dev project wiring + connectivity probe (roadmap #8)

> **What shipped:** Firebase BoM + Auth + Firestore dependencies, conditionally-applied
> google-services plugin, `.gitignore` protection for `google-services.json`,
> `INTERNET` permission, and a debug-only Firestore connectivity probe
> (`FirebaseProbe`, Logcat tag `FirebaseProbe`).
>
> **Two verification modes.** Mode A (no json — what CI proves) and Mode B (json
> present — the roadmap #8 acceptance run, needs the owner's machine + emulator).

> ⚠ **Posture disclosure:** this round adds the `INTERNET` permission — the app
> previously held NO network permission. Holding the permission does not mean data
> leaves the device: sync remains opt-in (roadmap #23), and release builds run no
> probe. The only network caller in this round is the debug probe itself.

---

## §0 STEP 0 — pull  (YOU)

```
git checkout master && git pull
```
Expected: `git log -1 --oneline` shows the merge commit of this round's PR.

---

## §1 Freshness gates  (YOU)

| Gate | Proof |
|---|---|
| Probe exists | `app/src/main/java/com/example/fdmanager/data/FirebaseProbe.kt` present |
| Plugin is conditional | `grep -n "google-services.json" app/build.gradle.kts` → the `if (file(...).exists())` block |
| json ignored | `grep -n "google-services" .gitignore` → one hit |
| INTERNET permission | `grep -n "INTERNET" app/src/main/AndroidManifest.xml` → one hit |

---

## §2 Mode A — build WITHOUT json (CI already proves this; local repeat optional)

The GitHub Actions run on this round's branch/PR **is** Mode A: no json exists in CI,
the plugin is skipped, build + Tier1 green. Local repeat (ROO, optional):

1. Ensure `app/google-services.json` does NOT exist.
2. `./gradlew build testDebugUnitTest --console=plain` → BUILD SUCCESSFUL, all Tier1 pass.
3. Run the app on Pixel_7_API_34, Logcat filter `FirebaseProbe`. Expected single line:
   `Firebase NOT configured (google-services.json absent) — local-only mode, nothing to probe`
4. Full regression: app behaves exactly as before — list, add/edit, calendar, settings,
   lock screen, export. Nothing in this round may change ANY user-visible behavior.

## §3 Mode B — the roadmap #8 acceptance run  (YOU place file, ROO runs)

1. **YOU:** place the downloaded file at `app/google-services.json`
   (guide STEP 6: `mv ~/Downloads/google-services.json .../app/`).
2. **YOU:** `git status` → `google-services.json` must NOT appear. If it appears,
   STOP — report (gitignore failed).
3. **ROO:** clean build & run on Pixel_7_API_34 (emulator has network by default):
   `./gradlew :app:assembleDebug` → install & launch.
4. **ROO:** Logcat, filter tag `FirebaseProbe`. Expected TWO lines:
   ```
   Firebase configured (project=fd-manager-dev) — probing Firestore (server read)...
   Firestore REACHABLE — server answered PERMISSION_DENIED (EXPECTED under locked deny-all rules). Roadmap-8 acceptance: PASS
   ```
   **PERMISSION_DENIED = PASS.** The server refusing us proves (a) the app reaches
   Firestore and (b) the deny-all rules actually work. Both facts are the acceptance.
5. Failure modes:
   | Logcat shows | Meaning | Action |
   |---|---|---|
   | `UNAVAILABLE` | emulator has no network | check emulator Wi-Fi, rerun once |
   | `read SUCCEEDED … CHECK THE RULES TAB` | rules are NOT deny-all | STOP — console → Firestore → Rules, verify `if false` |
   | no `FirebaseProbe` lines at all | not a debug build, or probe not merged | verify §1 gates |
6. **YOU:** Firebase console → Firestore → Usage: reads counter may show ~1–2 reads,
   $0.00 — confirms "zero cost tier" acceptance wording.

## §4 Evidence to capture (ROO → test-artifacts-008/, local only, never committed)

- `logcat-probe.txt` — the filtered FirebaseProbe lines (Mode B)
- screenshot of the Actions green run (Mode A)
- `git-status-after-json.txt` — proving the json is invisible to git

---
*Sensitive info check: NO — no keys, no credentials; project id `fd-manager-dev` is public-safe (it appears in every API URL anyway; security comes from rules + auth, never from hiding the id).*
