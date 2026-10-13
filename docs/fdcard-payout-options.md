# FdCard — Payout Frequency Placement Options (Issue #22 fix)

**Problem:** V21 FdCard showed `Rate • Tenor • Matures` but spec §4.2 + §3.2 says FD card must show payout frequency `Monthly payout / At maturity`. User reported missing.

**Current code (V21):**
```kotlin
"${formatRate}% p.a. • ${tenorLabel} • matures ${date}" // missing payout
```

**Spec says:**
- §3.2: `payoutFrequency = when interest is paid — shown on FD card and detail`
- §4.2: Each FD Card Shows Amount + Rate + Tenor + Payout + Matures + Status
- §4.6: card meta shows `100 days` or `3 months` + payout frequency

## Options considered (see html for visuals)

### Option A — Inline 4 bullets (Implemented, recommended)
```
10% p.a. • 12 months • Monthly payout • matures 10 Oct 2026
9.5% p.a. • 100 days • At maturity • matures 07 Oct 2026
```
- **Pros:** Minimal change, one line, spec-compliant, uses existing Text, no new chips. Height unchanged unless wraps on 320dp.
- **Cons:** Long line may wrap to 2 lines on small screens.
- **Implementation:** `Components.kt` FdCard Row3 Text = `"${formatRate}% p.a. • ${tenorLabel} • ${payout.label} • matures ${date}"` — gate `fdcard-0200r9`

### Option B — Two-line meta
```
10% p.a. • 12 months
Monthly payout • matures 10 Oct 2026
```
- Pros: More breathing room, easier to scan
- Cons: Card taller +14dp, fewer cards visible, more scroll (maxSwipes 50 still ok)

### Option C — Payout as chip
- Show `Monthly` / `At maturity` as small purple chip next to CountdownChip or below meta
- Pros: Visual distinction, matches StatusChip pattern
- Cons: Two chips on right crowd, adds new visual element not in spec

### Option D — Short label + icon
```
10% p.a. • 12 months • 💰 Monthly • matures 10 Oct 2026
```
- Pros: Shorter
- Cons: Emoji not M3, ambiguous vs spec label "Monthly payout"

## Decision
**Option A implemented** for V22 fix — minimal, spec-compliant, low impact. If spacing tight on small screens, fallback to Option B two-line meta (same info, more space). Option C could be V2 enhancement for filtering by payout.

## Docs updated
- `docs/FD-Manager-Spec.md` §4.2: now explicit Rate • Tenor • Payout • Matures with examples, both copies byte-identical
- `docs/app-wire-diagram.md` + `.html`: FdCard meta includes `payoutFrequency.label`, gate `fdcard-0200r9`, ASCII wire updated
- `docs/fdcard-payout-options.html` + `.md`: visual options A-D + recommendation
- `ui/components/Components.kt`: FdCard meta now includes `${fd.payoutFrequency.label}` + gate `fdcard-0200r9`

## Tests
- AddFdValidationTest comment said "tenor replaces payout" but spec says payout must be shown — new assertion should check payout label exists on card (Monthly payout / At maturity)
- Existing 16 tests should still pass (they check tenor exists, not payout), but new V22 should add payout check

## Visuals
See `fdcard-payout-options.html` for phone mockups of all options.
