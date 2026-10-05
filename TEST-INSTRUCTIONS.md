# TEST INSTRUCTIONS — hand this to the test agent

> **How to use (vmendis):** the architect PUSHES this file to `dev` as
> `TEST-INSTRUCTIONS.md` (repo root) at round start. Give the tester the usual pull
> commands + "open `TEST-INSTRUCTIONS.md` at the repo root and follow it" — no copy-paste
> of the contents. After the round PASSES, the architect REMOVES the file from git before
> any merge (**never in `master`** — see standing rules).
> **How to keep it current (architect):** after ANY push that touches tests or
> `docs/TESTING.md`, update the "Current round" block — SHA, gates, expectations — and
> re-push to `dev`. The standing rules below never change.

---

## Standing rules (every round)

- **Windows 11 environment (user's reminder, 2026-10-01): all agent-facing commands must
  be Windows cmd native — `git`, `findstr`, `dir`, `type`, `gradlew.bat`. NEVER unix
  commands (`grep`, `sed`, `cat`, `bash`, `ls`, `chmod`…), and NO `cd /d` — commands run
  from the agent's default cwd, which IS the project root (rounds ran fine without it);
  the sync block proves location with `dir /b gradlew.bat` instead. Paths in findstr use
  real backslash-nested folders (`java\com\example\fdmanager\`), never FQCN-dotted segments.
- Project root: `C:\LocalApps\AppDevelopment\fixed-deposit-manager-android-dev\fixed-deposit-manager-android-dev`
- The authoritative test plan is **`docs/TESTING.md`** in the repo — read it first;
  everything below must agree with it (if it ever conflicts, TESTING.md wins).
- **Absolute rules:** do not edit app code, test code, or assertions; do not delete or
  skip tests; do not "fix" failures. Only permitted build-file change: add a missing
  dependency line from TESTING.md §2.1 if absent (they are present — expect zero changes).
  Your job = run, collect evidence, report.
- Report location: **`TEST-REPORT-V<x>.md`** at project root (x = round number, e.g.
  `TEST-REPORT-V11.md` for Round 9 — matches the v1…v10 delivery convention),
  template in TESTING.md §3.
- **File lifecycle (user's rule, 2026-10-01):** `TEST-INSTRUCTIONS.md` is pushed to `dev`
  at round start so the tester pulls it like any other file; once the round PASSES it is
  REMOVED from the repo (architect commits the deletion before the PR). **Never leave
  `TEST-INSTRUCTIONS.md` in git** — it must be absent from `master` at all times; the
  merge gate includes "TEST-INSTRUCTIONS.md not in tree".
- Hard rules (violations = report rejected): header values are REAL command output
  (commit = `git log -1`; AS version = Help → About or `dir "%LOCALAPPDATA%\Google"`;
  API = `adb shell getprop ro.build.version.sdk`; Gradle = `gradlew.bat --version`).
  No `$(...)`, no `(unknown)`, no guesses. Tier-2 table = all 6 classes, counts must sum
  to the suite total. Every failure: assertion message + ≤30 logcat lines + `<failure>`
  XML excerpt with the `(<Test>.kt:NN)` frame + a classification proposal
  (TEST-BUG / CODE-BUG / ENV-FLAKE). Any not-run test named under "Not covered".
- Failure rerun (per failed class, once) — plain `--tests` does NOT work:

  ```bat
  gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.fdmanager.<TestClass>
  ```

  Record BOTH results. ENV-FLAKE only if the rerun PASSES.

---

## Current round — ROUND 9 (issue #25: payout frequency + renew payout options) → report = **v11**

**What shipped (issue #25):** FD-card meta line now reads `8.5% p.a. • Monthly payout •
matures 13 Oct 2026` (payout wording = `Monthly payout` / `At maturity`); detail screen
gains an **Interest payout** row + a **Renewal** card (instruction, *New FD principal*
preview, instruction-aware caption + auto-renew banner); the renew dialog has two radio
options (*Add interest to capital* / *Withdraw interest*) pre-selected from the stored
instruction; Add/Edit gains *Interest payout* chips (required — error *"Select when
interest is paid"*), *On renewal* chips (pre-selects *Add interest to capital*), updated
auto-renew subtitle; session-start auto-renew sweep (`autoRenewDue`).
**Counts changed: unit 25 → 29 (+4), instrumented 13 → 14 (AddFdValidation 2 → 3).**
Fresh `dev` @ **`ea4cf23`** (issue #25, pushed 2026-10-01). `dialogConfirm` contract kept.

**1. Sync (STOP and report if any check fails):**

```bat
dir /b gradlew.bat
git status
git fetch origin
git checkout -B dev origin/dev
git log -1 --oneline
findstr /C:"payoutFrequency" app\src\main\java\com\example\fdmanager\data\model\FixedDeposit.kt
findstr /C:"Select when interest is paid" app\src\main\java\com\example\fdmanager\ui\screens\AddEditFdScreen.kt
dir app\build\outputs\apk\androidTest\debug\*.apk
```

- `git status` must be clean BEFORE fetching (local modifications → STOP and report them).
- `git log -1` must show **`ea4cf23`** (or later SHA if I say so).
- BOTH findstr lines must print a match (new model field + new validation contract string).
- Record the APK timestamp shown by `dir` — it must be AFTER the sync (paste it below).

**2. Environment:** `adb devices` shows `emulator-5554  device`, API 34. No device → STOP.

**3. Tier 1 — unit tests:**

```bat
gradlew.bat testDebugUnitTest
```

Expected **29/29** (FdMath 7, FdRepository 15, BankRegistry 7).

**4. Tier 2 — FULL instrumented suite (all 6 classes):**

```bat
gradlew.bat connectedDebugAndroidTest
```

Expected **14/14** (counts `3/4/2/1/2/2` — see TESTING.md §3 hard rules). Freshness gate:
SoftDelete failures must show `openBank=navgate-0250r9` (old `01f2b9e` = stale sources → STOP).

**5. Deliver `TEST-REPORT-V11.md` (Round 9).** Header = real `git log -1` (the handoff
SHA I gave in chat — `ea4cf23` or later),
real AS/gradle values (no `(unknown)` — hard rule), "Tiers executed" = 2/2,
**"Sync proof" subsection** verbatim (git log + both findstr + dir APK timestamp), Tier-2
table with per-class counts that **sum to 14**, failures (if any) with message + ≤30
**fresh** logcat lines + XML `<failure>` excerpt + classification proposal, reruns per
standing rules. Visual checks (note under observations / "Not covered" if a screen can't
be reached): (a) bank-list FD card meta line shows the payout segment; (b) detail shows
*Interest payout* + **Renewal** card with principal preview; (c) renew dialog shows both
radio options with one pre-selected; (d) Add screen: saving without an *Interest payout*
choice shows *"Select when interest is paid"*, with a choice → saves and the card shows
the chosen wording.

---

## Round history (triage archive)

| Round | Result | Outcome |
|---|---|---|
| v1 @ `4703b8a` | 5/13 fail | All TEST-BUG: IME covered Save, merge-tree monogram, dup FD-number text, dialog-order clicks, bottom-of-screen clicks → fixed `46e6071` |
| v2 @ `46e6071` | 11/13 fail×2 | Renewal scroll-restore + empty-state hidden → XML lines 47/43 extracted → fixed `9d3b1bc`, `76095cf` |
| v3 @ `76095cf` | 12/13 fail×1 | Renewal GREEN; empty-state = assert racing async pop (ENV-FLAKE rejected: rerun failed identically) → fixed `0377e3d` |
| v4 @ `0377e3d` | 12/13 fail×1 | waitUntil timed out 5s — empty state NEVER appears; static paths all verified clean → self-diagnosing screen fingerprint shipped `04dc2dd` |
| v5 @ `04dc2dd` | fingerprint bypassed | ComposeTimeoutException extends AssertionError → `catch (Exception)` never ran (raw timeout escaped, agent then quoted source instead of XML) → widened to `Throwable` in `d3226c2` |
| v6 @ `d3226c2` | **delivered (diagnostic)** | Markers `[HOME<Total invested>, HOME-section<By bank>]` → detail was pushed FROM HOME (openBank click miss + Home strip card masked it) → TEST-BUG, CODE-BUG rejected → fix `01f2b9e` (scroll-visible + Sort-icon gate) |
| v7 @ `01f2b9e` | **12/13 — REJECTED (stale sources)** | Failure byte-identical to v6 + reused v6 logcat lines = pre-fix test code ran despite 01f2b9e header; CODE-BUG again rejected → anti-stale stamps shipped `1d62c2b` |
| v8 @ `61f1274` | **12/13 — CODE-BUG finally proven** | Sync proof + `openBank=navgate-01f2b9e` stamp → fresh sources, gate PASSED (was on FdList) → test-bug theory overturned; auto-pop guard on `fd.isDeleted` double-popped list→Home (executor's "expected failure" proposal rejected) → fixed `2732ee4` |
| v9 @ `2732ee4` | **13/13 ✅ MERGED (PR #2 → `e52a6e2`)** | Double-pop fix green on-device; #18 closed; violations noted (headers unknown, table counts misfilled, wrong rerun cmd); user UX sign-off → squash-merge 2026-10-01 |
| v10 @ `920fb44` | **13/13 + 25/25 ✅ MERGED (PR #3 → `177b963`)** | Fresh-source proof via 25-test count; violations: sync-proof block omitted + AS "(unknown)" — accepted as cosmetic; user sign-off → squash 2026-10-01, #20 closed |
