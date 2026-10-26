# TEST-INSTRUCTIONS-007 — CI: build + unit tests on every PR (roadmap #7)

> **This round is different: the tests run on GitHub's machines, not yours.**
> There is nothing for Roo to build and no emulator to start. Verification is
> reading the Actions tab. The owner can do this round without Roo entirely.

> **`dev` is retired.** The workflow knows only `master` and `arena/**`.

---

## §1 Freshness gates

On branch tip (`git log -1 --oneline` must show the session branch commit):

| Gate | Proof |
|---|---|
| Workflow exists | `.github/workflows/ci.yml` present |
| Right triggers | `grep -n "pull_request\|arena" .github/workflows/ci.yml` hits both |
| gradlew executable in git | `git ls-files -s gradlew` shows **100755** (100644 = CI dies with Permission denied) |

---

## §2 What the workflow does

One job, `Build + Tier1 unit tests`, on `ubuntu-latest` (free — public repo):

1. Checkout → JDK 17 (Temurin) → Gradle with dependency cache
2. `./gradlew build testDebugUnitTest --console=plain`
3. On failure only: uploads `app/build/reports/` + `test-results/` as a
   downloadable artifact (14-day retention)

Triggers: every PR targeting `master` · every push to `master` · every push to
`arena/**` (so a session branch is proven green BEFORE its PR exists).

---

## §3 Verification — OWNER, in the browser, no commands

1. Open **repo → Actions tab**. Expected: a run named **CI** for the session
   branch push, green check, job "Build + Tier1 unit tests".
2. Open the run → the Gradle step log ends with `BUILD SUCCESSFUL` and the
   Tier1 counts (119 at the time of writing; trust the run, not this file).
3. On the PR (once opened): a **check called "Build + Tier1 unit tests"**
   appears near the merge button. Green = mergeable evidence.

First run is the slowest (cold cache, ~4–6 min). Later runs reuse the
dependency cache and should be faster.

## §4 Failure handling

- Red run → open the failing step's log; copy the LAST 30 lines verbatim to
  the agent. Do not re-run more than once (one re-run allowed for
  infrastructure flakes — runner lost, download timeout).
- A red caused by code is fixed by the agent on the session branch; every
  push re-runs CI automatically.

## §5 Report

No local TEST-REPORT file for this round. The CI run itself is the permanent,
public record — link the run URL in the PR if asked.
