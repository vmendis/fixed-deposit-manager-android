# TEST-INSTRUCTIONS-010 — Firestore-backed FdRepository (roadmap #10)

> **What shipped:** `FdRepository` is now an INTERFACE with two backends, switched at
> runtime by `FdRepositoryProvider` (CLOUD_GATE = cloud-1000r1):
> `InMemoryFdRepository` (signed-out, demo, ALL tests, CI — the exact pre-#10 store) and
> `FirestoreFdRepository` (`users/{uid}/fds`, offline persistence = Firestore default,
> client-UUID doc ids, server timestamps, soft-delete/bin/renew semantics identical).
> One-shot migration moves USER-ENTERED FDs (never fd1..fd8 demo samples) to the cloud on
> first sign-in per uid. The #3 sync chip is REAL while the cloud backend is live
> (pending writes → Syncing, cache-only → Offline, server ack → Saved).
>
> **Decisions recorded (owner, 2026-11-04):** migrate user-entered FDs only · signed-out
> sessions keep showing the local store · sync chip wired to real Firestore state ·
> "Delete forever" REALLY deletes the cloud document · demo data is local-only (the two
> demo rows in Settings explain instead of acting while a cloud session is live).
>
> **Safety:** Tier2 can NEVER touch Firestore — `FdRepositoryProvider` probes for the
> androidTest class `AuthFlowTest` inside the running process and locks itself to the
> in-memory backend whenever any instrumentation APK is loaded, json or no json.

> **Counts this round: Tier1 = 131 (was 123, +8 FdFirestoreMapperTest) · Tier2 = 46 (unchanged).**

---

## §0 STEP 0 — pull  (YOU)
`git checkout master && git pull` after the PR merges.

## §1 Freshness gates  (YOU)

| Gate | Proof |
|---|---|
| Cloud gate | `grep -rn "cloud-1000r1" app/src/main --include="*.kt"` → ≥3 hits (mapper, repo, provider) |
| New files | `data/FirestoreFdRepository.kt`, `data/FdRepositoryProvider.kt`, `data/FdFirestoreMapper.kt` exist |
| Facade wired | `grep -n "FdRepositoryProvider.facade" app/src/main/java/com/example/fdmanager/data/FdRepository.kt` → one hit |
| Tier2 shield | `grep -n "AuthFlowTest" app/src/main/java/com/example/fdmanager/data/FdRepositoryProvider.kt` → ≥1 hit (instrumentation probe) |
| Demo local-only | `grep -c "Demo data is local-only" app/src/main/java/com/example/fdmanager/ui/screens/SettingsScreen.kt` → ≥2 |
| Migration flag | `grep -n "isCloudMigrated" app/src/main/java/com/example/fdmanager/data/DevicePrefs.kt` → one hit |

## §2 Tier1  (ROO)
`./gradlew testDebugUnitTest --console=plain` → BUILD SUCCESSFUL, **131 tests**, 0 failures
(new: `FdFirestoreMapperTest` ×8 — pure JVM, zero Firebase). CI runs the same on every
push, so the json-less build path is proven there automatically.

## §3 Tier2  (ROO)
`./gradlew connectedDebugAndroidTest` on Pixel_7_API_34 → **46 tests**, 0 failures.
No test touches Firestore: the provider's instrumentation probe forces the in-memory
backend for EVERY class, and `AuthFlowTest` additionally sets
`FdRepositoryProvider.forceFakeForTests` explicitly. All flows and assertions unchanged.

## §4 Real-cloud acceptance  (YOU + ROO — needs a signed-in session on the real backend)

**Prereq:** FIREBASE-SETUP-GUIDE PHASE C done (it is — #9 shipped on it). Console open at
Firestore Database → Data. **Firestore must be ENABLED in the console** (if
`users/{uid}/fds` can't be created, enable the database first — see guide).

| # | Step (who) | Expected |
|---|---|---|
| 1 | YOU: signed OUT, demo OFF → add FD "MIGRATE-1" → Settings → Sign in | after sign-in, FD list still shows MIGRATE-1 |
| 2 | YOU: console → Firestore → `users/{uid}/fds` | MIGRATE-1 doc there: id `fd-…` (client UUID), `createdAt` timestamp, dates as numbers, `userId` = your uid — **migration acceptance** |
| 3 | YOU: signed in → add FD "CLOUD-1" | appears in app instantly; console doc appears; sync chip ends at Saved |
| 4 | YOU: AIRPLANE MODE ON → add FD "OFFLINE-1", edit CLOUD-1's amount | both change in the app instantly; chip shows Offline/Syncing, NOT an error |
| 5 | YOU: AIRPLANE MODE OFF, wait a few seconds | chip → Saved; console shows OFFLINE-1 + the edited amount — **acceptance "airplane-mode edits sync on reconnect"** |
| 6 | YOU: delete CLOUD-1 (soft) → Recycle bin → restore it → delete again → Delete forever | after soft delete console doc has `isDeleted: true`; after restore `false`; after Delete forever the doc is GONE — **bin semantics + real delete** |
| 7 | YOU: renew MIGRATE-1 | child doc `…-R1` with `parentFdId` set; parent doc `status: "RENEWED"` |
| 8 | YOU (bash): `adb shell pm clear com.vssbox.fdmanager` → relaunch → sign in | all cloud FDs come back — **acceptance "new device sign-in replicates all data"** |
| 9 | YOU: Settings → Demo mode switch, then Reset demo data row | both show "Demo data is local-only - sign out to use" / snackbar; nothing happens to cloud data |
| 10 | YOU: Sign out → Home | session gate as in #9; sign back in → data returns (hidden, never wiped) |
| 11 | ROO: logcat during rows 1–10 | no app crashes, no Firestore permission errors while signed in |

## §5 Evidence  (ROO → `test-artifacts-010/`, local only)
`tier1.txt`, `tier2.txt`, `logcat-cloud.txt` — each headed by branch, SHA, date, real durations.
