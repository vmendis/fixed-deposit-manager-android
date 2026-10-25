# TEST-INSTRUCTIONS.md

**Executor:** Roo · **Owner:** vmendis (screenshots only) · **Architect:** Arena agent
**Branch under test:** `arena/de56d239-fixed-deposit-manager-android` @ `fd5dd80`
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
| C1 | bash (MINGW64) only. Never PowerShell, never cmd.exe |
| C2 | Verify the shell before anything else: `echo $BASH_VERSION` — must print a version |
| C3 | Never edit app code, never invent selectors. If a selector is missing, record **SELECTOR-MISSING** and stop |
| C4 | One Tier2 run. No loops, no retry-the-world |
| C5 | Flake protocol: rerun only the failed class, **once**. Still failing → report it as a failure |
| C6 | Every number in the report is copied from a real file, never typed from memory |
| C7 | The owner captures screenshots when **you** tell them to. You confirm each one before moving on |

---

## §0a Environment — capture it, never guess it

```bash
cd /c/LocalApps/AppDevelopment/fd-manager/fixed-deposit-manager-android
echo $BASH_VERSION
./gradlew --version
java -version 2>&1 | head -1
```

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

```bash
git fetch origin --prune
git checkout -B test-fd5dd80 origin/arena/de56d239-fixed-deposit-manager-android
git log -1 --oneline
git status --short
```

**Must print:** `fd5dd80 #5 Trust UX: real export, per-category delete-all, app lock`
**Must print nothing** for `git status --short`.

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
mkdir test-artifacts
```

No `-p`. No `-Force`. Both create a folder literally named that.

Contents required at the end:

```
test-artifacts/
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
./gradlew cleanTestDebugUnitTest testDebugUnitTest --rerun-tasks --console=plain
grep -h "time" app/build/test-results/testDebugUnitTest/*.xml
mkdir -p test-artifacts/unit
cp app/build/test-results/testDebugUnitTest/*.xml test-artifacts/unit/
```

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
./gradlew connectedDebugAndroidTest --rerun-tasks --console=plain
grep -h "time" app/build/outputs/androidTest-results/connected/*.xml
mkdir -p test-artifacts/connected
cp app/build/outputs/androidTest-results/connected/*.xml test-artifacts/connected/
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
adb logcat -d | grep -i "TestRunner" > test-artifacts/logcat.txt
wc -l test-artifacts/logcat.txt
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
adb exec-out screencap -p > test-artifacts/shot-settings-data.png
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
md5sum test-artifacts/*.png
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
- artifacts at repo root instead of `test-artifacts/`
- the executor edited app code

---

## §8 What to do when it fails

1. Record the failing class, the test name, the verbatim message, and the `Test.kt:NN` frame.
2. Rerun only that class, once.
3. Still failing → write it up and **STOP**.

Do not fetch, checkout, pull, or rerun the whole world. The architect fixes; Roo retests.
