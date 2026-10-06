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
  real backslash-nested folders (`java\\com\\example\\fdmanager\\`), never FQCN-dotted segments.
- Project root: `C:\\LocalApps\\AppDevelopment\\fixed-deposit-manager-android-dev\\fixed-deposit-manager-android-dev`
- The authoritative test plan is **`docs/TESTING.md`** in the repo — read it first;
  everything below must agree with it (if it ever conflicts, TESTING.md wins).
- **Absolute rules:** do not edit app code, test code, or assertions; do not delete or
  skip tests; do not "fix" failures. Only permitted build-file change: add a missing
  dependency line from TESTING.md §2.1 if absent (they are present — expect zero changes).
  Your job = run, collect evidence, report.
- Report location: **`TEST-REPORT-V<x>.md`** at project root (x = round number, e.g.
  `TEST-REPORT-V13.md` for Round 10 fix rerun),
  template in TESTING.md §3.
- **File lifecycle (user's rule, 2026-10-01):** `TEST-INSTRUCTIONS.md` is pushed to `dev`
  at round start so the tester pulls it like any other file; once the round PASSES it is
  REMOVED from the repo (architect commits the deletion before the PR). **Never leave
  `TEST-INSTRUCTIONS.md` in git** — it must be absent from `master` at all times; the
  merge gate includes "TEST-INSTRUCTIONS.md not in tree".
- Hard rules (violations = report rejected): header values are REAL command output
  (commit = `git log -1`; AS version = Help → About or `dir "%LOCALAPPDATA%\\Google"`;
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

## Current round — ROUND 10 fix rerun — Issue #19 CBSL-only InstitutionRegistry (61 institutions) — TEST-BUG fix V12 → V13

**What shipped (issue #19 — Option A strict CBSL-only, no custom storage):**
- **Spec §1 Primary Goal reworded:** "Enable users to securely track, manage, and receive alerts for FDs held at CBSL-regulated institutions (licensed commercial banks, specialised banks, and finance companies) in Sri Lanka — with a safety-first guardrail that only supports regulated institutions, helping users avoid unprotected, unregulated deposits."
- **InstitutionRegistry (new):** 24 LCB + 6 LSB + 31 LFC = 61 allowed (Nation Lanka Finance PLC excluded per CBSL prohibition as at 2025-12-31). Each: displayName, code, type BANK/FINANCE_COMPANY, colorArgb, alias list, LAST_UPDATED 2025-12-31, SOURCE_URL cbsl.gov.lk. No image assets — monogram tiles only.
- **CBSL-only guardrail:** Add/Edit institution picker is searchable grouped dialog Banks / Finance Companies, backed by InstitutionRegistry. No free-text custom storage. Validation at save time: `isCBSLRegulated(name)` must be true, otherwise block save with safety message "For your safety, FD Manager only tracks FDs at CBSL regulated institutions. Learn more: cbsl.gov.lk" + inline error "Not found in CBSL regulated list. Check spelling or tap Request addition if it's CBSL-licensed."
- **Request addition flow:** Button in picker → feedback intent with typed name, team verifies against cbsl.gov.lk and adds in next release. No FD saved until institution exists in registry.
- **Defensive rendering:** `resolve()` falls back to neutral tile with derived initials (significant words filtered: of, the, and, plc, ltd, limited, co, company, bank, finance, leasing, corporation, lanka, sri) — e.g., "Kandy Farmers Bank" → KF, "Serendib" → SER. Entry still blocked.
- **HomeScreen:** "By bank" → "By institution", "N banks" → "N institutions", subtitle "institutions • CBSL-regulated only", shows type badge (Bank / Finance Company), StatMini "Banks" → "Institutions".
- **FdListScreen:** institution type badge + CBSL licensed subtitle "Licensed • Updated 2025-12-31", empty state "This institution has no deposits."
- **FdDetailScreen:** "Bank" → "Institution", shows type + CBSL status row, returns estimate wording "institution's terms".
- **BankRegistry shim:** now delegates to InstitutionRegistry for backward compat (old 12 banks still resolve).
- **Tests:** BankRegistryTest updated (fallback KF), InstitutionRegistryTest 11 new tests (61 count, 30 banks / 31 finance, distinct codes, BANK vs FINANCE_COMPANY, CBSL-only check, Nation Lanka exclusion, alias matching, apostrophes, fallback, lastUpdated, distinct codes). Unit 29 → 40 (FdMath 7, FdRepository 15, BankRegistry 7, InstitutionRegistry 11).
- **V12 triage:** TEST-REPORT-V12.md @ 8b9709d — unit 40/40 green, instrumented 4/14 pass (10 failures identical `performScrollTo() failed: Text+EditableText contains 'By bank'`). Root cause: HomeScreen copy changed to "By institution" in 6710ba9, but androidTest selectors (TestSupport.openBank, BankMonogramUiTest, SoftDeleteRestoreTest) still searched "By bank" and "This bank has no deposits." — **TEST-BUG**, not CODE-BUG. Fix committed 41d710b: TestSupport.kt "By bank"→"By institution", BankMonogramUiTest same, SoftDeleteRestoreTest markers "By bank"→"By institution" + "This bank has no deposits."→"This institution has no deposits." Unit re-verified 40/40 @ navgate-0250r9.
- **Counts:** unit **40/40** expected, instrumented **14/14** expected (3/4/2/1/2/2). Gate `navgate-0250r9` still valid.

**1. Environment:** `adb devices` shows `emulator-5554  device`, API 34. No device → STOP. Toolchain expected AS 2025.3.4 / API 34 / Gradle 8.7 / AGP 8.5.2 / JDK 21 (do NOT upgrade to 8.13/8.13.2 mid-round — revert if AS prompts).

**2. Tier 1 — unit tests (force rerun to get real durations, not UP-TO-DATE):**

```bat
gradlew.bat cleanTestDebugUnitTest testDebugUnitTest --rerun-tasks
```

Expected **40/40** (FdMath 7, FdRepository 15, BankRegistry 7, InstitutionRegistry 11). Check `InstitutionRegistryTest` has 11 tests including Nation Lanka exclusion and CBSL-only check. Record real XML durations (e.g., `findstr time app\build\test-results\testDebugUnitTest\*.xml`), not placeholder `1m 41s`.

**3. Tier 2 — FULL instrumented suite (all 6 classes, force rerun):**

```bat
gradlew.bat connectedDebugAndroidTest --rerun-tasks
```

Expected **14/14** (counts `3/4/2/1/2/2` — see TESTING.md §3 hard rules). Freshness gate: SoftDelete failures must show `openBank=navgate-0250r9` (old `01f2b9e` = stale sources → STOP). Note: AddFdValidationTest still defaults to NSB (National Savings Bank) — picker is now searchable grouped dialog, not dropdown, but default remains NSB.

**4. Deliver `TEST-REPORT-V13.md` (Round 10 fix rerun).** Header = real `git log -1` output (expect `41d710b` — vmendis already verified sync before handoff), **"Tiers executed" = 2 / 2** (record REAL durations per class — no `0.000s`, no repeated `1m 41s` placeholder — parse `connectedDebugAndroidTest` XML or logcat timing), real AS/gradle values (no `(unknown)` — hard rule), Tier-2 table with per-class counts that **sum to 14**, failures (if any) with message + ≤30 **fresh** logcat lines (run `adb logcat -d` immediately after failure, not placeholder `<30 lines...>`), + XML `<failure>` excerpt + classification proposal, reruns per standing rules — **placeholder text in any failure section = rejected report**. Visual checks (capture evidence with `adb shell screencap -p /sdcard\shot.png` + `adb pull /sdcard\shot.png .\shot-a.png` and reference file paths in report; note under observations if a screen can't be reached): (a) Add screen institution picker shows grouped Banks / Finance Companies with search, selecting e.g., "LOLC Finance PLC" saves and tile shows LOLC; (b) Home shows "By institution" + "N institutions" + type badges; (c) Detail shows Institution row + type + CBSL status; (d) Attempt to save FD with non-CBSL name (e.g., "Acme Investment") is blocked with safety message "For your safety, FD Manager only tracks FDs at CBSL regulated institutions"; (e) Info dialog shows CBSL source + LAST_UPDATED 2025-12-31 + total 61 institutions.

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
| v11 @ `8ff759f` (rerun) | **29/29 + 14/14 ✅ MERGED (PR #4 → `6d18687`)** | Round 9: first delivery 2 TEST-BUGs (Save below fold, existence check) + report-quality (placeholder logcat, 1/2 header) → fixed `8ff759f`; rerun green exact SHA, Tiers 2/2, zero failures — accepted with cosmetic notes (identical durations, visual section absent but (a)+(d) proven by AddFd asserts). Round file removed `84a6afd` per lifecycle, #25 closed |
| v12 @ `8b9709d` | **40/40 + 4/14 — TEST-BUG triaged** | Round 10 — #19 CBSL-only 61 institutions — unit 40/40 green, instrumented 10 failures all `By bank` selector missing (Home now "By institution" after 6710ba9, but TestSupport + 2 tests still searched "By bank") — classified TEST-BUG, not CODE-BUG. Report had placeholder durations `1m 41s` repeated + placeholder logcat `<30 lines...>` — noted for V13 quality gate. Fix shipped `41d710b` (By institution + This institution has no deposits) |
