# FD Manager — Android mock UI

Front-end **mock** of the Fixed Deposit (FD) Manager app from the spec.
Kotlin + Jetpack Compose, Material 3, runs 100% on local sample data — **no Firebase, no auth, no backend** (per request).

## What it does

| Area | Details |
|---|---|
| **Home** | Total invested hero (Rs, LKR), active-FD / bank counts, next-maturity stat, "Maturing soon" strip (respects the configured lead window, spec default 5 days), bank summary cards → bank FD list. Notification bell with badge shows the same maturing list. |
| **Bank FD list** | FDs sorted by maturity date (spec) with sort menu (maturity / amount / rate) and status filter chips (All / Active / Matured / Renewed). Cards show FD number, amount, rate, maturity date, color-coded status chip + countdown chip (`in 4d`, `6d overdue`). Overflow menu → Edit / Renew / Delete. |
| **FD detail** | Amount header, status + countdown, simple-interest **returns estimate** (interest earned, maturity value), full detail rows, auto-renew notice, **renewal-history chain** (follows `parentFdId`, oldest→newest, tap to navigate), Renew and Delete actions with confirmations. |
| **Add / Edit FD** | Validated form matching the spec schema: FD number, bank (Sri Lankan bank list), amount, rate, duration presets + custom, opened-date picker, branch/code, auto-renew switch. **Live maturity preview** (maturity date, est. interest, est. value). Edit mode is pre-filled.  |
| **Calendar** | Month grid (Monday-first) with dots on days that have maturity events (color-coded by status), per-day lists, month counts. |
| **Recycle bin** | Soft-delete (`isDeleted`) per spec: restore or "delete forever" (explicitly flagged as bypassing the safeguard). |
| **Settings** | Theme (System/Light/Dark — working), notification toggle + lead-days slider (feeds the Home strip/bell), biometric lock (placeholder, needs auth), currency (LKR), CSV export + demo-data reset, About. |

## Spec behaviours modeled

- Firestore-like document shape (`FixedDeposit` mirrors every schema field incl. `parentFdId`, `isDeleted`, `isActive`).
- Renewal: old FD → `status=RENEWED, isActive=false`; new ACTIVE child opens on the old one's maturity date with same terms, linked via `parentFdId`, numbered `…-R1`, `…-R2`. Second-renewal of an already-renewed FD is rejected.
- Soft delete everywhere; totals exclude RENEWED parents and deleted rows.
- Sorted-by-maturity lists and 5-day maturing-soon window (default).

## Sample data

8 fictional FDs across 6 Sri Lankan banks (NSB, Sampath, HNB, Commercial, BOC, People's + one deleted DFCC), LKR amounts, dates **generated relative to today** so the mock always shows live countdowns, an in-window maturity, an overdue one and a renewal chain.

## Build

Open in Android Studio and Run, or CLI:

```bash
./gradlew :app:assembleDebug        # APK → app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:testDebugUnitTest    # JVM unit tests (domain math, queries, repository)
```

- minSdk 24, target/compile 34 · AGP 8.5.2 · Kotlin 2.0.20 · Compose BOM 2024.09.00 · core library desugaring for `java.time`.

## Architecture (mock)

```
data/            FixedDeposit model (mirrors Firestore schema), FdRepository (in-memory StateFlow store), SampleData, SettingsStore
domain/          FdMath (interest, maturity dates, countdowns, LKR formatting) & FdQueries (summaries, filters, chains) — pure Kotlin, unit-tested
ui/              theme (teal M3 + status palettes), shared components, screens, FdViewModel
MainActivity     Routes + bottom nav + FAB
```

Swap points for the real backend: `FdRepository` (→ Firestore) and `SettingsStore` (→ user doc).

## Security & trust

The public security architecture — multi-user tenancy, threat model, defense
in depth, data-handling promises and honest limitations — is documented in
**[docs/SECURITY.md](docs/SECURITY.md)**.

Current mock phase: all data is in-memory only, never persisted, and never
leaves the device.

## Deliberately not implemented (backend-era)

Authentication, real push notifications (10:00 AM daily job), biometric unlock, CSV export, interest rate history, multi-currency.
