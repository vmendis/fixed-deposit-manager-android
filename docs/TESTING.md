# FD Manager — Test Plan v1

**Roles:** *Test architect* (writes tests, owns assertions) → *Executor* (an Android Studio coding
agent: runs commands, collects evidence, writes the report) → *Reviewer* (vmendis submits the
report to the architect for triage).

**Scope:** mock UI (in-memory), all screens. Backend/auth out of scope per project charter.

---

## 0. Prerequisites

- Branch `dev` checked out, up to date with the remote (must contain the `androidTest` sources
  listed in §2). If `androidTest/` doesn't exist yet → stop, report "tests missing", do not improvise.
- Android Studio's embedded JDK (17+) — no manual JAVA_HOME needed when launched from AS.
- **Emulator running, API 34** (matches `compileSdk 34`): verify with `adb devices` (device listed,
  `state: device`).
- Run all commands from the project root (`fd-manager/`). On Windows use `gradlew.bat`.

---

## 1. Tier 1 — JVM unit tests (already exist)

```bat
gradlew.bat testDebugUnitTest
```

- **Pass criteria: all tests green** (29 expected as of issue #25: FdMath 7, FdRepository 15,
  BankRegistry 7; if the count is higher, use it).
- Reports (XML, machine-readable):
  `app\build\test-results\testDebugUnitTest\*.xml`
- Reports (HTML, human-readable):
  `app\build\reports\tests\testDebugUnitTest\index.html`

---

## 2. Tier 2 — Instrumented Compose UI tests

### 2.1 Dependencies

In `app/build.gradle.kts`, inside `dependencies { }`. **Add only if missing; never remove or
change existing lines:**

```kotlin
androidTestImplementation(platform("androidx.compose:compose-bom:2024.09.00"))
androidTestImplementation("androidx.compose.ui:ui-test-junit4")
androidTestImplementation("androidx.test.ext:junit:1.2.1")
androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
androidTestImplementation("androidx.test:runner:1.6.2")
debugImplementation("androidx.compose.ui:ui-test-manifest")
```

(`testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"` is already configured.)

### 2.2 Rules for the executor

- Test classes live in `app/src/androidTest/java/com/example/fdmanager/`.
- Every test starts with state isolation (the architect includes this in each class):

  ```kotlin
  @Before fun resetState() =
      FdRepository.get().reset(SampleData.seed(LocalDate.now()))
  ```

- **Selector contract** — use only these, in this priority order:
  1. `testTag` values that exist in the code (Windows: `findstr /S /C:"testTag(" app\src\main\*.kt`), notably:
     - `bankMonogram` — the bank identity tile (any surface)
     - `summary:<full bank name>` — Home "By bank" summary card, e.g. `summary:Bank of Ceylon (BOC)`
  2. Visible exact text (e.g. `hasText("Save FD")`, `"NSB-78412"`, button labels
     `"Renew"`, `"Delete"`, `"Restore"`, `"Cancel"`, chips `"Matured"`)
  3. `contentDescription` values already on icons: `"Add FD"`, `"Back"`, `"Edit"`,
     `"Sort"`, `"Recycle bin"`, `"Maturing soon"`, `"Delete forever"`
  - `dialogConfirm` / `dialogDismiss` — AlertDialog confirm/dismiss buttons (detail and
    list dialogs, incl. the issue-#25 renew dialog with its two payout options). Use helper
    `confirmDialog()` from `TestSupport.kt`; never match dialog buttons by text (dialog/root
    ordering is not guaranteed by the test API). Renew-dialog option labels ("Add interest
    to capital" / "Withdraw interest") and payout chips ("Monthly payout" / "At maturity")
    exist in BOTH dialog/form AND list-card surfaces — prefer substring/`onAllNodes` when
    the list may be composed (FD-card meta line embeds the payout wording).
  - Screen-bottom buttons (`Save FD`, `Renew`, `Delete`) live inside scrollable columns —
    always `performScrollTo().performClick()` on them (the soft keyboard can cover them).
  - Detail screens show an FD number TWICE (app-bar title + info row): assert presence via
    `onAllNodesWithText(...)` — `onNodeWithText(...).assertIsDisplayed()` will fail as ambiguous.
- **If a needed selector is missing: do not invent one and do not edit app code — record it in the
  report as a blocker** (`SELECTOR-MISSING: <what was needed>`).
- **Never modify assertions, never delete or skip a failing test, never "fix" app code.**
  Your job is execution + evidence.

### 2.3 Test suite (written and owned by the test architect)

| Class | Covers | Key assertions |
|---|---|---|
| `AddFdValidationTest` | Add/Edit form validation (3 tests) | Save with empty FD number / 0 amount / 0 rate → error text visible (e.g. "Enter an amount greater than 0"); valid entry (must pick an **Interest payout** chip — issue #25) → returns to previous screen, new FD appears with payout wording on its card; valid-but-no-payout entry → blocked with "Select when interest is paid" |
| `NavigationFlowTest` | Home → list → detail → back | Summary card tap opens bank list (title = bank name); FD card tap opens detail (amount + "FD number" row visible); back twice returns Home |
| `BankMonogramUiTest` | Roadmap #18 | `bankMonogram` tags exist on Home summary cards; detail of sample NSB FD shows code `NSB`; detail of sample BOC FD shows `BOC` |
| `SoftDeleteRestoreTest` | Spec soft delete | Delete via detail menu + confirm → Home active count decreases; FD present in Recycle Bin; restore → visible in active list again |
| `RenewalChainTest` | Spec renewal | Sample `COM-33018` → Renew → list has one more FD; old FD's detail shows `Renewed` status |
| `SortFilterTest` | List chips | Each filter chip renders a non-empty list for seeded data; sort menu opens and shows the selected option's checkmark |

### 2.4 Run

```bat
adb devices
gradlew.bat connectedDebugAndroidTest
```

- **Pass criteria: 0 failed tests.** Failures are *data for the report*, not something to patch.
- Reports (XML): `app\build\outputs\androidTest-results\connected\*.xml`
- Reports (HTML): `app\build\reports\androidTests\connected\index.html`

---

## 3. Report spec — `TEST-REPORT-V<N>.md` (e.g. `TEST-REPORT-V11.md`)

The executor creates this file at the project root and hands it to the reviewer:

```markdown
# TEST REPORT — <date>
- Branch / commit: <branch> @ <sha (git rev-parse --short HEAD)>
- Environment: Windows 11, Android Studio <ver>, AVD <name> / API <level>, gradle <ver>
- Tiers executed: 2 / 2 (both — only write 1/2 if a tier genuinely was not run)

## Tier 1 — unit tests
| Test class | Tests | Passed | Failed | Report path |

## Tier 2 — instrumented tests
| Test class | Tests | Passed | Failed | Duration | Report path |

## Failures (one section each)
### <TestClass>.<method>
- Result: TEST-BUG / CODE-BUG / ENV-FLAKE   ← executor's *proposal*, final call is the architect's
- Assertion message: <verbatim>
- Relevant logcat: <≤30 lines>
- HTML/XML report path: <path>

## Blockers
- SELECTOR-MISSING: ...   (anything from §2.2)

## Not covered (explicit)
- <what was out of scope or skipped, and why>
```

**Hard rules — a report containing these violations is REJECTED and re-requested:**

- **No placeholders, no fabrication.** Every `<...>` above must be real command output:
  run `git rev-parse --short HEAD`; Android Studio version from Help → About; AVD API level
  from Device Manager or `adb shell getprop ro.build.version.sdk`; Gradle from
  `gradlew.bat --version`. Literal `$(...)`, `(unknown)`, or guessed values are rejections.
- **Tier-2 table completeness:** one row per test class (all 6), per-class counts that sum
  to the suite total — current suite (issue #25): AddFdValidationTest **3**, BankMonogramUiTest **4**,
  NavigationFlowTest **2**, RenewalChainTest **1**, SoftDeleteRestoreTest **2**,
  SortFilterTest **2** = **14**. Any test not executed must be NAMED with its reason under
  "Not covered". The header's commit must equal the SHA actually tested (`git log -1`).
- **Every failure section is full:** verbatim assertion message + ≤30 lines of relevant
  logcat + the `<failure>` excerpt (3–10 lines) from
  `app\build\outputs\androidTest-results\connected\*.xml` — **including the
  `at com.example.fdmanager.<Test>.…(<Test>.kt:NN)` frame that names the failing line** —
  + a classification proposal (TEST-BUG / CODE-BUG / ENV-FLAKE). Blank template rows are
  rejections.

**Flake protocol (MANDATORY):** on a failure, rerun that class **once** with the
instrumentation class filter (plain `--tests` does NOT work on connected tasks):

```bat
gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.fdmanager.RenewalChainTest
```

(comma-separate FQCNs to rerun several). Record BOTH results in the failure section. If it
passes on rerun, label it ENV-FLAKE. A report without rerun results for each failed class
is incomplete and will be returned untriaged.

---

## 4. Out of scope for v1 (deliberate)

- Screenshot/pixel regression (Paparazzi) — candidate for v2, not this run.
- Settings persistence across process death, Calendar edge cases (month boundaries), dark-mode
  pixel checks — manual checklist items, not automated here.
- Any Firebase/backend behaviour (does not exist yet).
