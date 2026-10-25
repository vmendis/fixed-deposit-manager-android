# TEST-INSTRUCTIONS-005.md

> **Permanent.** This file is committed and ships to `master`. It is **not** deleted before the
> final merge — the lifecycle rule that said otherwise was retired on 2026-10-25, because a file
> that exists only on a branch is a file Roo cannot find when he needs it.


**Executor:** Roo · **Owner:** vmendis (screenshots only) · **Architect:** Arena agent
**Branch under test:** `arena/de56d239-fixed-deposit-manager-android` @ `6aa2cce`
**Feature:** #5 Trust UX — export, delete-all-data, app lock

> **Roo always follows this file.** No ad-hoc command lists come from the architect unless the
> architect says so explicitly, in writing, in that message.
>
> **Roo guides the owner through screenshots.** The owner does not decide what to capture and does
> not run commands. Roo names one screen, waits, confirms, then names the next.

---

## §0 Executor contract

| # | Rule |
|---|---|
| C1 | bash (MINGW64) only. Never PowerShell, never cmd.exe. **Never substitute a Windows/cmd tool for a bash one** — see the table in §0c |
| C2 | Run commands **directly in the MINGW64 prompt**. Never wrap them in `bash -c "..."` — the wrapper expands `$VAR` and nested quotes before bash sees them, which is what breaks. Verify the shell with `bash --version \| head -1` |
| C3 | Never edit app code, never invent selectors. If a selector is missing, record **SELECTOR-MISSING** and stop |
| C4 | One Tier2 run. No loops, no retry-the-world |
| C5 | Flake protocol: rerun only the failed class, **once**. Still failing → report it as a failure |
| C6 | Every number in the report is copied from a real file, never typed from memory |
| C7 | The owner captures screenshots when **you** tell them to. You confirm each one before moving on |
| C8 | **If a command fails, do not reach for a different tool.** Report the failure with the exact output and stop. Swapping `grep` for `findstr` is how a passing gate turns into a false failure |

---

## §0a Environment — capture it, never guess it

```bash
cd /c/LocalApps/AppDevelopment/fd-manager/fixed-deposit-manager-android
bash --version | head -1
./gradlew --version
java -version 2>&1 | head -1
```

> **Do not run these through `bash -c "..."`.** You already have a MINGW64 prompt open — type
> them into it. Wrapping them re-expands `$BASH_VERSION` and any nested quotes first, and bash
> then tries to execute the value as code:
>
> ```
> bash: -c: line 1: syntax error near unexpected token `('
> bash: -c: line 1: `echo 5.3.9(1)-release'
> ```
>
> That is not a broken environment. It is the wrapper. `bash --version | head -1` has no `$` and no
> nested quotes, so it survives either way.

Record the real output. Expected:

| Field | Expected |
|---|---|
| Gradle | 8.13 |
| Launcher JVM | 17.0.16 (Eclipse Adoptium) |
| Android Studio | Panda 4 \| 2025.3.4 Patch 1 |

`local.properties` must exist and must **not** appear in `git status`. If the SDK is not found,
write it (never commit it):

```bash
cat > local.properties <<'EOF'
sdk.dir=C\:\\Users\\thiss\\AppData\\Local\\Android\\Sdk
EOF
```

---

## §0b Sync the branch — owner runs, or Roo if the owner delegates

> The instruction file is in the repo, so **you can read it on any branch**. Sync the branch under
> test first, then re-read this file from that branch — it is the copy that matches the code.

```bash
git fetch origin --prune
git checkout -B test-6aa2cce origin/arena/de56d239-fixed-deposit-manager-android
git log -1 --oneline
git status --short
```

**Must print:** `6aa2cce test: tool map, no-substitution rule, issue number in artifact folder`
**Must print nothing** for `git status --short`.

---

## §0c Tool map — the only shell you have

Every command in this file is bash. Some of these have already been attempted; all of them failed.

| Do NOT run | Why it fails here | Run this |
|---|---|---|
| `chcp 65001` | Windows **cmd** builtin. Git Bash has no `chcp` → `command not found` | nothing — output is already UTF-8 |
| `findstr "x" file` | Windows tool. In Git Bash it mangles the path → `FINDSTR: Cannot open file` | `grep -n "x" file` |
| `dir` | cmd | `ls -la` |
| `type file` | cmd | `cat file` |
| `copy a b` | cmd | `cp a b` |
| `del file` | cmd | `rm file` |
| `where java` | cmd | `command -v java` |
| `gradlew.bat ...` | cmd shim | `./gradlew ...` |
| `timeout /t 5` | cmd | `sleep 5` |
| `bash -c "..."` | expands `$VAR` and nested quotes first | type the command directly |

**`chcp` and `findstr` appearing in the console are noise, not failures.** They were attempted,
the shell rejected them, and the real command afterwards worked. Do not retry them.

**A `findstr` line that prints `Cannot open` is not a failed gate.** The gate is the `grep -q ... && echo`
line. If that prints `OK`, the gate passed — regardless of what else was attempted.

---

## §0d When a run goes wrong — the loop killer

Observed on the first run of this issue. Read this before troubleshooting anything.

### A. "Unresolved reference" in EVERY test file is not a code error

```
e: ...DataWipeTest.kt:4:30 Unresolved reference 'data'.
e: ...AuthValidationTest.kt:3:30 Unresolved reference 'domain'.
e: ...BankRegistryTest.kt:3:30 Unresolved reference 'data'.
```

Untouched test classes failing the same way means **the app classes were never compiled**.
Look further up for the real error:

```
> Task :app:mergeDebugResources FAILED
> Cannot access output property 'incrementalFolder' ...
>   java.nio.file.NoSuchFileException: ...\app\build\intermediates\incremental\debug\mergeDebugResources\merged.dir\values-th
```

That is a **corrupt `app/build` directory**, not a code defect. One recovery, then rerun:

```bash
./gradlew --stop
rm -rf app/build
./gradlew testDebugUnitTest --rerun-tasks --console=plain | tee test-artifacts-005/unit-run.log
```

If the same `NoSuchFileException` survives that, **stop and report**. Do not repeat the recovery.

### B. Never kill java.exe

```
taskkill /F /IM java.exe        <-- WRONG, never run this
```

This kills the Gradle daemon mid-write, which is exactly what corrupts `app/build`.
`--stop` already shuts daemons down cleanly. There is no reason to force-kill.

### C. Gradle prints nothing while it works

A long task shows no output until it finishes. That is normal.
**Do not poll.** All of these are noise and were seen on the last run:

```
$ echo "waiting"                  x12
$ cho "waiting"                   bash: cho: command not found
$ echo "checking final status"
```

Echoing into the console does not check anything, does not speed anything up, and makes the
transcript unreadable. Run the command, then **wait for the prompt to come back**. One command,
one result, one report.

### D. Two recoveries maximum, ever

| Attempt | Allowed |
|---|---|
| 1st failure | diagnose from the log, apply one recovery, rerun once |
| 2nd failure | report and **STOP** |
| 3rd attempt | never |

A third attempt is the loop. The architect needs the log more than it needs another run.

---

## §0e You are already inside bash — never launch it

Look at your own prompt:

```
thiss@Windows11-DEV MINGW64 /c/LocalApps/.../fixed-deposit-manager-android (test-bae8499)
$
```

**That prompt *is* bash.** Every command below is typed straight into it. There is no bash to find,
no bash to launch, no shell path to set.

### The failure this prevents

```
$ C:\Users\thiss\AppData\Local\Microsoft\WindowsApps\bash.exe -c "ls -la gradlew"
bash: C:UsersthissAppDataLocalMicrosoftWindowsAppsbash.exe: command not found
```

Two bugs in one line:

| Bug | What happened |
|---|---|
| Backslashes are escape characters in bash | `\U`, `\A`, `\M` … were eaten, so the path collapsed to `C:Usersthiss...` |
| That path is the **WSL stub**, not Git Bash | `WindowsApps\bash.exe` launches WSL. It is not your shell and has no JDK, no SDK |

The fix is not a better path. The fix is **not launching bash at all**.

### Hard bans

| Never | Because |
|---|---|
| `C:\...\bash.exe -c "..."` | you are already in bash |
| `where bash` / `where java` / `which bash` | looking for a tool you are already using |
| `sed -i` on **any tracked file** | rewrites the repo. `sed -i 's/\r$//' gradlew` was attempted — it would have modified a tracked file |
| `./gradlew` prefixed by a shell path | `./gradlew` already runs |

### If the console shows exploration, the run is already over

Any of these mean Roo has stopped following instructions and started guessing:

```
where ...        which ...        ls /c/
export JAVA_HOME=...             trying a different bash.exe
the same command repeated        echo "waiting"
```

**Stop. Do not fix it yourself. Report the last good output and end the run.**

### If `gradlew` ever looks broken

Do **not** edit it. Restore it:

```bash
git checkout -- gradlew
chmod +x gradlew
./gradlew --version
```

---

## §1 Freshness gates — run before spending time on tests

```bash
grep -q "RFC 4180" app/src/main/java/com/example/fdmanager/domain/FdExport.kt && echo "GATE export OK"
grep -q "CONFIRM_PHRASE" app/src/main/java/com/example/fdmanager/domain/DataWipe.kt && echo "GATE wipe OK"
grep -q "lockScreen" app/src/main/java/com/example/fdmanager/ui/screens/LockScreen.kt && echo "GATE lock OK"
grep -q "privacyLine" app/src/main/java/com/example/fdmanager/ui/screens/SettingsScreen.kt && echo "GATE privacy OK"
grep -q "Routes.LOCK" app/src/main/java/com/example/fdmanager/MainActivity.kt && echo "GATE route OK"
```

All five must print `OK`. Any miss → the tree is stale. **Stop**, report, do not run tests.

---

## §2 Artifacts — required

```
mkdir test-artifacts-005
```

**The folder carries the issue number.** For issue #5 it is `test-artifacts-005`. No `-p`. No `-Force`.
Both create a folder literally named that.

Contents required at the end:

```
test-artifacts-005/
  unit-run.log
  connected-run.log
  logcat.txt
  unit/            <- copies of app/build/test-results/testDebugUnitTest/*.xml
  connected/       <- copies of app/build/outputs/androidTest-results/connected/*.xml
  shot-settings-data.png
  shot-export-format.png
  shot-delete-confirm.png
  shot-lock-screen.png
```

No artifacts → the report is **REJECTED** and the run is untriageable.

---

## §3 Tier1 — 95 unit tests

```bash
./gradlew testDebugUnitTest --rerun-tasks --console=plain | tee test-artifacts-005/unit-run.log
grep -h "time" app/build/test-results/testDebugUnitTest/*.xml
cp app/build/test-results/testDebugUnitTest/*.xml test-artifacts-005/unit/
```

> This takes **several minutes**. `--rerun-tasks` already forces every task to re-execute, so
> `cleanTestDebugUnitTest` is redundant — dropping it halves the runtime and the odds of hitting a
> command timeout. If Roo kills and restarts the command, that is a timeout, not a failing test.

**Expected — 95 tests, 0 failures:**

| Class | Tests |
|---|---:|
| AuthValidationTest | 4 |
| BankRegistryTest | 7 |
| **DataWipeTest** | **17** |
| **FdExportTest** | **20** |
| FdMathTest | 11 |
| FdRepositoryTest | 17 |
| InstitutionRegistryTest | 11 |
| LoadingStateTest | 4 |
| SyncStateTest | 4 |
| **Total** | **95** |

`DataWipeTest` and `FdExportTest` are new in this round. If either class is **absent**, the tree
does not contain the feature — stop and report.

> AGP emits one aggregate `<testsuite>`. Per-class numbers live in `<testcase classname="…">`.

---

## §4 Tier2 — 38 instrumented tests

ONE RUN. Not a loop.

```bash
adb devices
./gradlew connectedDebugAndroidTest --rerun-tasks --console=plain | tee test-artifacts-005/connected-run.log
grep -h "time" app/build/outputs/androidTest-results/connected/*.xml
cp app/build/outputs/androidTest-results/connected/*.xml test-artifacts-005/connected/
```

**Expected — 38 tests, 0 failures:**

| Class | Tests |
|---|---:|
| AddFdValidationTest | 5 |
| AuthFlowTest | 4 |
| BankMonogramUiTest | 4 |
| LoadingErrorUiTest | 4 |
| NavigationFlowTest | 2 |
| RenewalChainTest | 1 |
| SoftDeleteRestoreTest | 2 |
| SortFilterTest | 2 |
| SyncStateChipTest | 4 |
| **TrustUxUiTest** | **10** |
| **Total** | **38** |

New in this round: `TrustUxUiTest` — export chooser, the delete-all confirmation gate, the lock
screen on cold start, and the privacy line.

Flake protocol: rerun **only** the failed class, **once**:

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.example.fdmanager.TrustUxUiTest --rerun-tasks
```

---

## §5 Logcat

```bash
adb logcat -d | grep -i "TestRunner" > test-artifacts-005/logcat.txt
wc -l test-artifacts-005/logcat.txt
```

Paste up to 30 real lines in the report. A count of zero means instrumentation never ran — report
that as a failure, not as a pass.

---

## §6 Screenshots — ROO LEADS, OWNER CAPTURES

**The owner does not choose. Roo names one screen at a time, then waits.**

Before every capture, Roo runs the foreground check and only proceeds if the app is in front:

```bash
adb shell am start -n com.vssbox.fdmanager/com.example.fdmanager.MainActivity
sleep 5
adb shell dumpsys window | grep mCurrentFocus
```

`mCurrentFocus` must name `com.vssbox.fdmanager`. If it shows the launcher, recapture — a launcher
screenshot is a **REJECTED** artifact.

Then capture and pull:

```bash
adb exec-out screencap -p > test-artifacts-005/shot-settings-data.png
```

> Use `exec-out`. Git Bash rewrites `/`-leading adb arguments if you use `screencap -p /sdcard/...`.

### The four captures, in this order, one at a time

| # | Screen | Roo tells the owner | Tag it must show |
|---|---|---|---|
| 1 | Settings → **Data** | "Scroll to Data. Stop when you see Export deposits and Delete data." | `exportRow`, `deleteDataRow` |
| 2 | **Export format dialog** | "Tap Export deposits. Leave the dialog open." | `exportFormatDialog` |
| 3 | **Delete data dialog** | "Close that. Tap Delete data. Tick Deposits, type DELETE, but do not press Delete." | `wipeDataDialog` |
| 4 | **Lock screen** | "Close the dialog. Go to Security, turn on Biometric lock, then fully close and reopen the app." | `lockScreen` |

After each capture Roo states: *"Capture N done — verify the app is visible, not the home screen."*
Only then move to the next.

### md5 check — all four must differ

```bash
md5sum test-artifacts-005/*.png
```

Four identical hashes means the screen never changed. Report it.

---

## §7 Report — `TEST-REPORT.md`

```
# TEST REPORT

- Branch / commit: <fill from git rev-parse --short HEAD>   <-- MANDATORY
- Environment: Windows 11, Android Studio <ver>, AVD Pixel_7_API_34 / API 34,
  Gradle <ver>, AGP <ver>, Kotlin <ver>, JDK <ver>
- Tiers executed: __ / 2

## Tier1 (95)
| Test class | Tests | Passed | Failed | Duration | Report path |

## Tier2 (38)
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
- Tier1 ≠ 95 or Tier2 ≠ 38
- durations summed instead of copied
- "see logcat" in place of real lines
- a failure section without a `Test.kt:NN` frame
- launcher screenshots
- `--rerun-tasks` omitted
- gates in §1 unchecked
- artifacts at repo root instead of `test-artifacts-<NNN>/`
- the executor edited app code

---

## §8 What to do when it fails

1. Record the failing class, the test name, the verbatim message, and the `Test.kt:NN` frame.
2. Rerun only that class, once.
3. Still failing → write it up and **STOP**.

Do not fetch, checkout, pull, or rerun the whole world. The architect fixes; Roo retests.
