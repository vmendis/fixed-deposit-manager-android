# TEST-INSTRUCTIONS-006.md

> **Permanent.** This file is committed and ships to `master`. It is not deleted before the final
> merge — a file that exists only on a branch is a file Roo cannot find when he needs it.

**Executor:** Roo · **Owner:** vmendis (screenshots only) · **Architect:** Arena agent
**Branch under test:** `arena/9e247e5f-fixed-deposit-manager-android`
**Feature:** #6 — compounding frequency for interest math

> **Roo always follows this file.** No ad-hoc command lists come from the architect unless the
> architect says so explicitly, in writing, in that message.
>
> **Roo guides the owner through screenshots.** The owner does not decide what to capture and does
> not run commands. Roo names one screen, waits, confirms, then names the next.

> **`dev` is retired (2026-10-26).** There is one branch path: session branch → `master`. Do not
> check out `dev`; it does not exist. Do not create it.

---

## §0 Executor contract

| # | Rule |
|---|---|
| C1 | bash (MINGW64) only. Never PowerShell, never cmd.exe. **Never substitute a Windows/cmd tool for a bash one** — see §0c |
| C2 | Run commands **directly in the MINGW64 prompt**. Never wrap them in `bash -c "..."` |
| C3 | Never edit app code, never invent selectors. If a selector is missing, record **SELECTOR-MISSING** and stop |
| C4 | One Tier2 run. No loops, no retry-the-world |
| C5 | Flake protocol: rerun only the failed class, **once**. Still failing → report it as a failure |
| C6 | Every number in the report is copied from a real file, never typed from memory |
| C7 | The owner captures screenshots when **you** tell them to. You confirm each one before moving on |
| C8 | **If a command fails, do not reach for a different tool.** Report the failure with the exact output and stop |

---

## §0a Environment — capture it, never guess it

```bash
cd /c/LocalApps/AppDevelopment/fd-manager/fixed-deposit-manager-android
bash --version | head -1
./gradlew --version
java -version 2>&1 | head -1
```

Type these into the prompt you already have. Do **not** wrap them — the wrapper expands `$VAR` and
nested quotes before bash sees them:

```
bash: -c: line 1: syntax error near unexpected token `('
```

That is the wrapper failing, not a broken environment.

| Field | Expected |
|---|---|
| Gradle | 8.13 |
| Android Gradle Plugin | 8.13.2 |
| Kotlin plugin | 2.0.20 |
| Launcher JVM | 17.0.16 (Eclipse Adoptium) |
| Android Studio | Panda 4 \| 2025.3.4 Patch 1 |

`local.properties` must exist and must **not** appear in `git status`. If the SDK is not found,
write it — never commit it:

```bash
cat > local.properties <<'EOF'
sdk.dir=C\:\\Users\\thiss\\AppData\\Local\\Android\\Sdk
EOF
```

---

## §0b Sync the branch — record the SHA, it goes in the report

```bash
git fetch origin --prune
git checkout -B test-compounding origin/arena/9e247e5f-fixed-deposit-manager-android
git log -1 --oneline
git status --short
```

`git status --short` **must print nothing**. Copy the `git log -1 --oneline` line into the report —
that SHA is the one under test. There is no expected SHA to memorise: §1 is what proves the tree
actually carries the feature.

---

## §0c Tool map — the only shell you have

| Do NOT run | Why it fails here | Run this |
|---|---|---|
| `chcp 65001` | Windows **cmd** builtin → `command not found` | nothing — output is already UTF-8 |
| `findstr "x" file` | Windows tool, mangles the path → `FINDSTR: Cannot open` | `grep -n "x" file` |
| `dir` | cmd | `ls -la` |
| `type file` | cmd | `cat file` |
| `copy a b` | cmd | `cp a b` |
| `del file` | cmd | `rm file` |
| `where java` | cmd | `command -v java` |
| `gradlew.bat ...` | cmd shim | `./gradlew ...` |
| `timeout /t 5` | cmd | `sleep 5` |
| `bash -c "..."` | expands `$VAR` and nested quotes first | type the command directly |

**`chcp` and `findstr` in the console are noise, not failures.** Do not retry them with another tool.

---

## §0d When a run goes wrong — the loop killer

### A. "Unresolved reference" in EVERY test file is not a code error

Untouched test classes failing the same way means the app classes were never compiled. Look further
up for the real error — usually `mergeDebugResources` with a `NoSuchFileException` on something
under `app\build\`. That is a **corrupt build directory**, not a defect. One recovery, then rerun:

```bash
./gradlew --stop
rm -rf app/build
./gradlew testDebugUnitTest --rerun-tasks --console=plain | tee test-artifacts-006/unit-run.log
```

Survives that → **stop and report**. Do not repeat the recovery.

### B. Never kill java.exe

`taskkill /F /IM java.exe` kills the daemon mid-write, which is what corrupts `app/build`.
`--stop` already shuts daemons down cleanly.

### C. Gradle prints nothing while it works

A long task shows no output until it finishes. That is normal. **Do not poll.** No `echo "waiting"`,
no repeated status lines. Run the command, then wait for the prompt to come back.

### D. Two recoveries maximum, ever

| Attempt | Allowed |
|---|---|
| 1st failure | diagnose from the log, apply one recovery, rerun once |
| 2nd failure | report and **STOP** |
| 3rd attempt | never |

---

## §0e You are already inside bash — never launch it

Your prompt is already `MINGW64`. There is no bash to find and no shell path to set.

| Never | Because |
|---|---|
| `C:\...\bash.exe -c "..."` | backslashes are escape characters; that path is the **WSL stub**, which has no JDK or SDK |
| `where bash` / `which java` | looking for a tool you are already using |
| `sed -i` on **any tracked file** | rewrites the repo |
| `./gradlew` prefixed by a shell path | `./gradlew` already runs |

If the console shows `where …`, `which …`, `export JAVA_HOME=…`, a different `bash.exe` or the same
command repeated — **the run is already over.** Report the last good output and stop.

If `gradlew` ever looks broken, restore it; never edit it:

```bash
git checkout -- gradlew
chmod +x gradlew
```

---

## §1 Freshness gates — run before spending time on tests

```bash
grep -q "interestForYears" app/src/main/java/com/example/fdmanager/domain/FdMath.kt && echo "GATE math OK"
grep -q "enum class CompoundingFrequency" app/src/main/java/com/example/fdmanager/data/model/FixedDeposit.kt && echo "GATE model OK"
grep -q "compound_" app/src/main/java/com/example/fdmanager/ui/screens/AddEditFdScreen.kt && echo "GATE form OK"
grep -q "previewCompounding" app/src/main/java/com/example/fdmanager/ui/screens/AddEditFdScreen.kt && echo "GATE preview OK"
grep -q "detailCompounding" app/src/main/java/com/example/fdmanager/ui/screens/FdDetailScreen.kt && echo "GATE detail OK"
grep -q "compounding" app/src/main/java/com/example/fdmanager/domain/FdExport.kt && echo "GATE export OK"
```

All six must print `OK`. Any miss → the tree is stale. **Stop**, report, do not run tests.

---

## §2 Artifacts — required

```
mkdir test-artifacts-006
```

No `-p`. No `-Force`. Both create a folder literally named `-p` or `-Force`.

Required at the end:

```
test-artifacts-006/
  unit-run.log
  connected-run.log
  logcat.txt
  unit/            <- copies of app/build/test-results/testDebugUnitTest/*.xml
  connected/       <- copies of app/build/outputs/androidTest-results/connected/*.xml
  shot-form-quarterly.png
  shot-form-simple.png
  shot-detail-compounding.png
  shot-form-days.png
```

No artifacts → the report is **REJECTED** and the run is untriageable.

---

## §3 Tier1 — 119 unit tests

```bash
./gradlew testDebugUnitTest --rerun-tasks --console=plain | tee test-artifacts-006/unit-run.log
grep -h "time" app/build/test-results/testDebugUnitTest/*.xml
cp app/build/test-results/testDebugUnitTest/*.xml test-artifacts-006/unit/
```

> This takes **several minutes**. `--rerun-tasks` already forces every task to re-execute;
> `cleanTestDebugUnitTest` is redundant and only makes a command timeout more likely.

**Expected — 119 tests, 0 failures:**

| Class | Tests |
|---|---:|
| AuthValidationTest | 4 |
| BankRegistryTest | 7 |
| **CompoundingTest** | **18** |
| DataWipeTest | 17 |
| FdExportTest | 23 |
| FdMathTest | 13 |
| FdRepositoryTest | 18 |
| InstitutionRegistryTest | 11 |
| LoadingStateTest | 4 |
| SyncStateTest | 4 |
| **Total** | **119** |

`CompoundingTest` is new in this round. If it is **absent**, the tree does not contain the feature —
stop and report.

---

## §4 Tier2 — 44 instrumented tests

ONE RUN. Not a loop.

```bash
adb devices
./gradlew connectedDebugAndroidTest --rerun-tasks --console=plain | tee test-artifacts-006/connected-run.log
grep -h "time" app/build/outputs/androidTest-results/connected/*.xml
cp app/build/outputs/androidTest-results/connected/*.xml test-artifacts-006/connected/
```

**Expected — 44 tests, 0 failures:**

| Class | Tests |
|---|---:|
| AddFdValidationTest | 5 |
| AuthFlowTest | 4 |
| BankMonogramUiTest | 4 |
| **CompoundingUiTest** | **6** |
| LoadingErrorUiTest | 4 |
| NavigationFlowTest | 2 |
| RenewalChainTest | 1 |
| SoftDeleteRestoreTest | 2 |
| SortFilterTest | 2 |
| SyncStateChipTest | 4 |
| TrustUxUiTest | 10 |
| **Total** | **44** |

New in this round: `CompoundingUiTest` — the five frequency chips, the Quarterly default, the live
preview following the selection, the detail screen agreeing with the form, an edit keeping the saved
frequency, and a day-based tenor compounding.

Flake protocol — rerun **only** the failed class, **once**:

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.example.fdmanager.CompoundingUiTest --rerun-tasks
```

---

## §5 Logcat

```bash
adb logcat -d | grep -i "TestRunner" > test-artifacts-006/logcat.txt
wc -l test-artifacts-006/logcat.txt
```

Paste up to 30 real lines in the report. **Count the starts, not the file** — `logcat.txt` may
contain more than one run if the device was not cleared. Quote the PID and the timestamp window for
the run you are reporting. A count of zero means instrumentation never ran: report that as a
failure, never as a pass.

---

## §6 Screenshots — ROO LEADS, OWNER CAPTURES

**The owner does not choose. Roo names one screen at a time, then waits.**

Before every capture, run the foreground check and only proceed if the app is in front:

```bash
adb shell am start -n com.vssbox.fdmanager/com.example.fdmanager.MainActivity
sleep 5
adb shell dumpsys window | grep mCurrentFocus
```

`mCurrentFocus` must name `com.vssbox.fdmanager`. A launcher screenshot is a **REJECTED** artifact.

Then capture:

```bash
adb exec-out screencap -p > test-artifacts-006/shot-form-quarterly.png
```

> Use `exec-out`. Git Bash rewrites `/`-leading adb arguments if you use `screencap -p /sdcard/...`.

**Install first.** `connectedDebugAndroidTest` uninstalls the app when it finishes:

```bash
./gradlew installDebug
```

### The four captures, in this order, one at a time

| # | Screen | Roo tells the owner | What it must show |
|---|---|---|---|
| 1 | **Add FD → Compounding** | "Tap Add FD. Type 1000000 in Amount and 10 in Interest rate. Scroll down until you see the Compounding chips and the maturity preview. Stop there." | Chips **Simple / Monthly / Quarterly / Half-yearly / Yearly**, **Quarterly** selected, preview line `Compounding  Quarterly`, `Est. interest  Rs 103,813` |
| 2 | **Same form, Simple selected** | "Tap **Simple**. Leave the screen exactly there." | `Compounding  Simple`, `Est. interest  Rs 100,000` — the preview moved |
| 3 | **FD detail → Returns** | "Close the form without saving. Go back. Open National Savings Bank, then NSB-78412." | Returns card with `Compounding  Quarterly` and `Est. interest earned  Rs 103,813` |
| 4 | **Add FD → Days tenor** | "Back out and tap Add FD again. Amount 1000000, Interest rate 10. Tap **Days**. Scroll to the preview." | `Compounding  Quarterly` with `Est. interest  Rs 27,457` — a 100-day tenor |

After each capture Roo states: *"Capture N done — verify the app is visible, not the home screen."*
Only then move to the next.

### md5 check — all four must differ

```bash
md5sum test-artifacts-006/*.png
```

Four identical hashes means the screen never changed. Report it.

---

## §7 Report — `TEST-REPORT.md`

```
# TEST REPORT

- Branch / commit: <session branch> @ <git log -1 --oneline>   <-- MANDATORY
- Environment: Windows 11, Android Studio <ver>, AVD Pixel_7_API_34 / API 34,
  Gradle <ver>, AGP <ver>, Kotlin <ver>, JDK <ver>
- Tiers executed: __ / 2

## Tier1 (119)
| Test class | Tests | Passed | Failed | Duration | Report path |

## Tier2 (44)
| Test class | Tests | Passed | Failed | Duration | Report path |

## Evidence of GREEN
## Failures   (verbatim message + <=30 logcat lines + 3-10 XML lines incl. "(Test.kt:NN)")
## Blockers   (SELECTOR-MISSING)
## Screenshots (4, with md5)
## Not covered
```

**REJECTED if any of:**

- no branch / SHA
- literal `$(...)` or `(unknown)` anywhere
- guessed version numbers
- Tier1 ≠ 119 or Tier2 ≠ 44
- durations summed instead of copied
- "see logcat" in place of real lines
- a failure section without a `Test.kt:NN` frame
- launcher screenshots
- `--rerun-tasks` omitted
- gates in §1 unchecked
- artifacts at repo root instead of `test-artifacts-006/`
- the executor edited app code

---

## §8 What to do when it fails

1. Record the failing class, the test name, the verbatim message, and the `Test.kt:NN` frame.
2. Rerun only that class, once.
3. Still failing → write it up and **STOP**.

Do not fetch, checkout, pull, or rerun the whole world. The architect fixes; Roo retests.
