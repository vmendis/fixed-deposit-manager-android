# FD Manager — App Wire Diagram & Component Map (Official Names)

> Purpose: single source of truth for screen ↔ composable ↔ testTag ↔ data so conversations don't drift.
> Updated: 2026-10-17 — Issue #27 vssbox branding + onboarding (cube 96dp hero, by vssbox, built-by footer, About 72dp) + #21/#28.

## 1. Navigation graph (official screen composables)

```
MainActivity
 ├─ OnboardingScreen (ui/screens/OnboardingScreen.kt) — #27 vssbox branding gate onboarding-0100r9, shown if SettingsStore.hasSeenOnboarding=false
 │   ├─ Image vssbox_cube 96dp testTag vssboxLogoOnboarding
 │   ├─ Text vssbox headlineMedium bold primary testTag vssboxNameOnboarding + FD Manager titleLarge + "by vssbox • Secure box for your deposits" labelMedium
 │   ├─ Trust Card ElevatedCard testTag trustCard: lock icon + "Your data stays yours" + trust line "We never ask for your bank login..."
 │   ├─ FeatureRow ×3: Track all FDs CBSL-only, Rate•Tenor•Payout•Matures, Renewal chain & Recycle bin
 │   ├─ Button Get Started testTag getStartedButton → setHasSeenOnboarding(true), TextButton Explore demo testTag exploreDemoButton
 │   └─ Footer Row testTag builtByVssboxFooter: cube 16dp + "Built by vssbox • © 2026 vssbox"
 │
 ├─ BottomNav: Home | Calendar | Settings  +  FAB Add FD  +  AppBar actions (Notifications, RecycleBin)
 │
 ├─ HomeScreen (ui/screens/HomeScreen.kt) — #21 fix: rememberLazyListState() + LaunchedEffect scrollToItem(0) gate home-scroll-0100r9 + #27 footer
 │   ├─ TopAppBar: title Column "FD Manager" bold + "by vssbox" labelSmall onPrimary 80%, actions Notifications (badge), RecycleBin
 │   ├─ Hero card: Total invested → Lkr.words(total) headlineLarge + Lkr.exact(total) bodySmall + StatMini ×3
 │   │   └─ StatMini (ui/components/Components.kt): label/value mini stat
 │   ├─ SectionHeader ("Maturing soon") → LazyRow of compact cards
 │   │   └─ Each: bank labelSmall, fdNumber titleMedium, Lkr.compact(amount) + Dates.format(maturity), CountdownChip
 │   ├─ SectionHeader ("By institution") → LazyColumn(state = listState) of BankSummaryCard — back from bank resets to top (hero visible) per #21
 │   │   └─ BankSummaryCard (private in HomeScreen.kt, official name BankSummaryCard)
 │   │       ├─ BankMonogram (ui/components/BankMonogram.kt) size 46.dp testTag "bankMonogram"
 │   │       ├─ Center Column: bank name titleMedium, "active • total" bodySmall + InstitutionType label
 │   │       ├─ Right Column (Option B): Lkr.words(total) titleMedium bold primary
 │   │       │                          Lkr.exact(total) labelSmall gray
 │   │       │                          "invested" labelSmall gray
 │   │       └─ KeyboardArrowRight icon + testTag "summary:<bankName>"  e.g. "summary:NSB"
 │   └─ Footer Row testTag builtByVssboxHome: cube 16dp + "Built by vssbox • © 2026 vssbox • FD Manager by vssbox" labelSmall gray
 │
 ├─ FdListScreen (ui/screens/FdListScreen.kt) — bank-filtered list
 │   ├─ TopAppBar: BankMonogram (institution header) + bank name title, Back, Sort menu
 │   ├─ StatusFilter chips: All / Active / Matured / Renewed (FilterChip)
 │   └─ LazyColumn of FdCard
 │
 ├─ FdDetailScreen (ui/screens/FdDetailScreen.kt)
 │   ├─ TopAppBar: fdNumber title, Back, Edit, Delete
 │   ├─ Header: Lkr.words(amount) headline + Lkr.exact(amount) bodySmall + StatusChip + CountdownChip
 │   ├─ Info rows: Institution (BankMonogram + InstitutionRegistry type), Tenor, Payout frequency, Dates
 │   ├─ Returns estimate: FdMath.interestEarned + maturity value
 │   ├─ Renewal chain: parentFdId → child list oldest→newest, tap navigates
 │   └─ Actions: Renew (→ RenewDialog), Delete (→ AlertDialog dialogConfirm/dialogDismiss)
 │
 ├─ AddEditFdScreen (ui/screens/AddEditFdScreen.kt)
 │   ├─ Form fields: fdNumber, Institution picker (InstitutionRegistry), amount, rate, tenorUnit toggle Months|Days
 │   │   ├─ Tenor presets: Months 1,3,6,12,24,36,60 + custom 1-120; Days 30,60,90,100,180,300,364 + custom 1-999
 │   │   ├─ Institution picker dialog: grouped Banks / Finance Companies, search, info icon → CBSL link, "Request addition"
 │   │   ├─ Payout frequency chips: Monthly payout / At maturity (required)
 │   │   └─ RenewOption radio: CAPITALIZE / PAYOUT (pre-select CAPITALIZE), autoRenew switch
 │   ├─ Live maturity preview: maturity date, est. interest, est. value (FdMath)
 │   └─ Bottom Save button: "Save FD" (scrollTo + click required — keyboard may cover)
 │
 ├─ CalendarScreen (ui/screens/CalendarScreen.kt)
 │   ├─ Month grid Monday-first, dots on maturity days color-coded via statusPalette()
 │   └─ Selected day list: FdCard reused (Option B)
 │
 ├─ RecycleBinScreen (ui/screens/RecycleBinScreen.kt)
 │   └─ List of isDeleted=true FDs, FdCard + Restore / Delete forever actions
 │
 └─ SettingsScreen (ui/screens/SettingsScreen.kt) — #27 vssbox About branding 72dp cube + Built by vssbox
     ├─ Theme: System/Light/Dark
     ├─ Notifications: toggle + leadDays slider (feeds Home maturing strip + bell badge)
     ├─ Security: Biometric lock placeholder
     ├─ Data: Currency LKR, Export CSV placeholder, Reset demo data
     ├─ About: Image vssbox_cube 72dp testTag vssboxLogoAbout + vssbox titleLarge bold primary + FD Manager titleMedium + by vssbox labelSmall + Version 1.0 © 2026 vssbox + description + Row Built by vssbox • vssbox.com testTag aboutBranding
     └─ Demo: Demo mode toggle isDemoMode → SettingsStore.setDemoMode (opt-in per #1)
```

## 2. Shared components (ui/components/)

| Composable | File | Purpose | Key props / tags |
|---|---|---|---|
| `FdCard` | Components.kt | **Core FD row** — used by FdListScreen, CalendarScreen, RecycleBin, maturing strip (compact variant uses same data but compact wording) | `fd: FixedDeposit`, `today: LocalDate`, callbacks `onClick/onEdit/onRenew/onDelete`. Layout: fdNumber + StatusChip top row, **Option B amount** (Lkr.words primary titleLarge bold primary + Lkr.exact secondary labelSmall gray) + CountdownChip middle row, **meta line `rate • tenorLabel • payoutFrequency.label • matures date` bodySmall (fix 2026-10-07: was missing payout, now `10% p.a. • 12 months • Monthly payout • matures 10 Oct 2026`)**, autoRenew icon, overflow menu MoreVert → DropdownMenu Edit/Renew/Delete |
| `StatusChip` | Components.kt | Status pill | `status: FdStatus` → colors from `statusPalette()` |
| `CountdownChip` | Components.kt | Days-to-maturity pill | `days: Long` → FdMath.countdownShort, red ≤5d, amber ≤30d |
| `SectionHeader` | Components.kt | Section title + optional subtitle | `title, subtitle` |
| `StatMini` | Components.kt | Hero mini stat | `label, value` |
| `BankMonogram` | BankMonogram.kt | Institution tile, no images — initials from InstitutionRegistry | `bank: String`, size Dp, `testTag("bankMonogram")` |
| `RenewDialog` | Components.kt | Renew confirmation with payout options | `fd`, `onConfirm(RenewOption)`, `onDismiss` — tags `dialogConfirm` / `dialogDismiss`, radio options "Add interest to capital" / "Withdraw interest", preview next principal Lkr.full() |
| `BankSummaryCard` | HomeScreen.kt (private) | By-institution card | `summary: BankSummary` → BankMonogram + counts + Option B right column + testTag `summary:<bank>` |

## 3. Domain / data (official names)

| Object | File | Key members |
|---|---|---|
| `Lkr` | domain/FdMath.kt | `full(amount): "Rs 1,000,000"` (grouped 0 decimals, legacy), `words(amount): "Rs 1 Million" / "Rs 750 K"` (≥1M Million with 1 decimal trimmed, <1M K per #20), `exact(amount): "Rs 1,000,000.00"` (2 decimals), `compact(amount): "Rs 1 M" / "Rs 750 K"` |
| `FdMath` | domain/FdMath.kt | `interestEarned()`, `maturityValue()`, `maturityDate()`, `tenorLabel()`, `daysUntil()`, `countdownLabel()`, `countdownShort()`, `formatRate()` |
| `InstitutionRegistry` | ui/components/InstitutionRegistry.kt | `find(name): Institution?`, `resolve(name): Institution` (fallback monogram), `all: List<Institution>`, grouped `banks` / `financeCompanies`, `lastUpdated` |
| `FixedDeposit` | data/model/FixedDeposit.kt | schema §3.2: fdNumber, bank (CBSL-only), amount, openedDate, durationMonths/days, tenorUnit, interestRate, maturityDate, branch, payoutFrequency, renewOption, autoRenew, status, parentFdId, isDeleted, isActive |
| `FdRepository` | data/FdRepository.kt | in-memory StateFlow, `reset(seed)`, `add/update/delete/renew`, `totalInvested()`, queries via `FdQueries` |
| `FdQueries` | domain/FdQueries.kt | `summaries()`, `maturingSoon()`, `sortedByMaturity()` etc. |

## 4. Amount formatting — Option B (official rule)

> **Hero (HomeScreen total):** Primary `Lkr.words(total)` headlineLarge bold primary, Secondary `Lkr.exact(total)` bodySmall gray — issue #20
>
> **By-institution cards (BankSummaryCard):** Right column 3 lines — Primary `Lkr.words(totalInvested)` titleMedium bold primary, Secondary `Lkr.exact(totalInvested)` labelSmall gray, Tertiary `"invested"` labelSmall gray — issue #22 Option B
>
> **FD cards (FdCard) — extended 2026-10-07 + payout fix 2026-10-07:** Left column 2 lines — Primary `Lkr.words(amount)` titleLarge bold primary (e.g. `Rs 1 Million`), Secondary `Lkr.exact(amount)` labelSmall gray (e.g. `Rs 1,000,000.00`) — Option B extended from BankSummaryCard to FdCard. **Meta line now `rate • tenor • payoutFrequency.label • matures` e.g. `10% p.a. • 12 months • Monthly payout • matures 10 Oct 2026` (was missing payout in V21, fixed per spec §4.2 + §4.6)**. CountdownChip stays right. Legacy `Lkr.full()` no longer used in FdCard.
>
> **Maturing soon strip (HomeScreen LazyRow):** Compact `Lkr.compact()` remains (space-constrained) — not changed by #22.

Screenshots reference:
- `image-1.png` (Home By-institution) shows correct Option B: `Rs 1 Million / Rs 1,000,000.00 / invested`
- `image-2.png` (FdListScreen before extension) showed `Rs 1,000,000` numbers only — now updated to words+exact per extension.

## 5. Test selectors (contract — do not invent)

- `testTag("bankMonogram")` — any monogram tile
- `testTag("summary:<full bank name>")` — e.g. `summary:NSB`, `summary:Bank of Ceylon (BOC)` — Home → openBank helper uses swipeUntilTag + waitUntil + onAllNodes[0].click (no performScrollTo, maxSwipes 50, gate navgate-0500r9)
- `testTag("onboardingScreen")`, `vssboxLogoOnboarding`, `vssboxNameOnboarding`, `trustCard`, `getStartedButton`, `exploreDemoButton`, `builtByVssboxFooter`, `builtByVssboxHome`, `aboutBranding`, `vssboxLogoAbout` — #27 branding gates
- Visible text: fdNumber like `"NSB-78412"`, amount words like `"Rs 1 Million"`, exact like `"Rs 1,000,000.00"`, chips `"Matured"`, `"Active"`, buttons `"Save FD"`, `"Renew"`, `"Delete"`, `"Restore"`, `"Cancel"`, onboarding `"Get Started — Add your first FD"`, `"Built by vssbox"`, `"vssbox"`
- contentDescription: `"Add FD"`, `"Back"`, `"Edit"`, `"Sort"`, `"Recycle bin"`, `"Maturing soon"`, `"More actions"`, `"Auto-renew on"`, `"vssbox logo"`
- Dialog tags: `dialogConfirm` / `dialogDismiss` — use `confirmDialog()` helper, never match by text for dialog buttons
- RenewDialog options: radio labels `"Add interest to capital"` / `"Withdraw interest"` + payout wording on card meta line embeds payout frequency — prefer `onAllNodes` when list composed

## 6. CBSL guardrail

- `InstitutionRegistry` = single source of truth, ~70 entries: 24 LCB + 6 LSB + 32 LFC (as at CBSL Notice 31.12.2025). Display name + short code + type BANK/FINANCE_COMPANY + brand color + aliases.
- Add/Edit picker: searchable, grouped Banks / Finance Companies, no free-text. Not found → inline error + "Request addition" intent. Save blocked if not in registry.
- Defensive rendering: legacy unknown names → neutral monogram fallback but warning badge, editing blocked until CBSL institution selected.

## 7. File map (official paths)

```
app/src/main/java/com/example/fdmanager/
 ├─ data/model/FixedDeposit.kt
 ├─ data/FdRepository.kt, SampleData.kt, SettingsStore.kt (hasSeenOnboarding, isDemoMode)
 ├─ domain/FdMath.kt (Lkr, Dates, FdMath), FdQueries.kt
 └─ ui/
     ├─ components/
     │   ├─ Components.kt (FdCard, StatusChip, CountdownChip, SectionHeader, StatMini, RenewDialog)
     │   ├─ BankMonogram.kt (BankMonogram)
     │   └─ InstitutionRegistry.kt (InstitutionRegistry, Institution, InstitutionType)
     ├─ screens/
     │   ├─ OnboardingScreen.kt (OnboardingScreen) — #27 vssbox 96dp cube + trust card + CTA gate onboarding-0100r9
     │   ├─ HomeScreen.kt (HomeScreen, BankSummaryCard private) — #21 scroll fix + #27 footer builtByVssboxHome
     │   ├─ FdListScreen.kt (FdListScreen)
     │   ├─ FdDetailScreen.kt (FdDetailScreen)
     │   ├─ AddEditFdScreen.kt (AddEditFdScreen)
     │   ├─ CalendarScreen.kt (CalendarScreen)
     │   ├─ RecycleBinScreen.kt (RecycleBinScreen)
     │   └─ SettingsScreen.kt (SettingsScreen) — #27 About 72dp cube + aboutBranding + demo toggle
     └─ theme/Theme.kt (statusPalette)
app/src/main/res/
 ├─ drawable/vssbox_cube.png — 1024x1024 RGBA transparent manual (408K) from vssbox_logo.jpg
 ├─ drawable/ic_launcher.xml — fallback layer-list dark bg #0B1220 + cube 72dp
 ├─ drawable-anydpi-v26/ic_launcher.xml — adaptive bg #0B1220 + fg vssbox_cube + monochrome
 └─ mipmap-anydpi-v26/ic_launcher.xml — same adaptive for mipmap
```

## 8. Visual wire (ASCII)

```
┌─ HomeScreen ──────────────────────────────────────────┐
│ TopAppBar: FD Manager [🔔badge] [🗑️]                  │
│ ┌─ Hero ────────────────────────────────────────────┐ │
│ │ Total invested                                    │ │
│ │ Rs 3.2 Million (Lkr.words) headlineLarge primary  │ │
│ │ Rs 3,200,000.00 (Lkr.exact) bodySmall             │ │
│ │ [Active FDs] [Institutions] [Next maturity] StatMini│
│ └───────────────────────────────────────────────────┘ │
│ SectionHeader: Maturing soon                        │
│ [LazyRow] [BOC-123 • Rs 750 K • 12 Dec • CountdownChip] │
│ SectionHeader: By institution (N institutions)      │
│ ┌─ BankSummaryCard summary:NSB ────────────────────┐│
│ │ [NSB Monogram 46dp]  NSB        Rs 1 Million      ││
│ │  2 active • 2 total   Bank     Rs 1,000,000.00    ││
│ │                                   invested   >   ││
│ └───────────────────────────────────────────────────┘│
└───────────────────────────────────────────────────────┘

┌─ FdListScreen (bank filtered) ────────────────────────┐
│ TopAppBar: [←Back] [Monogram] NSB  [Sort]             │
│ FilterChips: [All] [Active] [Matured] [Renewed]       │
│ ┌─ FdCard ──────────────────────────────────────────┐│
│ │ NSB-78412                          [ACTIVE Chip]  ││
│ │ Rs 1 Million                       [in 4d Chip]   ││
│ │ Rs 1,000,000.00                                    ││
│ │ 10% p.a. • 12 months • Monthly payout • matures 10 Oct 2026 [⋮] ││
│ └───────────────────────────────────────────────────┘│
│ ┌─ FdCard ──────────────────────────────────────────┐│
│ │ NSB-78413                          [MATURED]      ││
│ │ Rs 750 K                           [today]        ││
│ │ Rs 750,000.00                                      ││
│ │ 8% p.a. • 100 days • At maturity • matures 07 Oct 2026 [⋮] ││
│ └───────────────────────────────────────────────────┘│
└───────────────────────────────────────────────────────┘

FdCard (official) — Components.kt — FDCARD_PAYOUT_GATE = fdcard-0200r9
  Row1: fdNumber titleMedium bold weight1 + StatusChip
  Row2: Column weight1 { Lkr.words titleLarge bold primary + Lkr.exact labelSmall gray } + CountdownChip
  Row3: meta bodySmall onSurfaceVariant weight1 = rate • tenorLabel • payoutFrequency.label • matures date (e.g. "10% p.a. • 12 months • Monthly payout • matures 10 Oct 2026") + autoRenew icon + overflow menu Box DropdownMenu
  Options: A inline 4 bullets (implemented), B two-line meta, C payout chip, D icon — see docs/fdcard-payout-options.html
```

---

**For testers:** When a failure says "amount not found", check whether you are asserting `Lkr.full` (legacy numbers-only) vs new Option B `Lkr.words` + `Lkr.exact`. After 2026-10-07, FdCard expects words (e.g. "Rs 1 Million") AND exact (e.g. "Rs 1,000,000.00"), not just "Rs 1,000,000".
