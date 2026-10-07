# FD Manager — App Wire Diagram & Component Map (Official Names)

> Purpose: single source of truth for screen ↔ composable ↔ testTag ↔ data so conversations don't drift.
> Updated: 2026-10-07 — Issue #22 Option B extended to FD cards (`FdCard`).

## 1. Navigation graph (official screen composables)

```
MainActivity
 ├─ BottomNav: Home | Calendar | Settings  +  FAB Add FD  +  AppBar actions (Notifications, RecycleBin)
 │
 ├─ HomeScreen (ui/screens/HomeScreen.kt)
 │   ├─ TopAppBar: title "FD Manager", actions Notifications (badge), RecycleBin
 │   ├─ Hero card: Total invested → Lkr.words(total) headlineLarge + Lkr.exact(total) bodySmall + StatMini ×3
 │   │   └─ StatMini (ui/components/Components.kt): label/value mini stat
 │   ├─ SectionHeader ("Maturing soon") → LazyRow of compact cards
 │   │   └─ Each: bank labelSmall, fdNumber titleMedium, Lkr.compact(amount) + Dates.format(maturity), CountdownChip
 │   └─ SectionHeader ("By institution") → LazyColumn of BankSummaryCard
 │       └─ BankSummaryCard (private in HomeScreen.kt, official name BankSummaryCard)
 │           ├─ BankMonogram (ui/components/BankMonogram.kt) size 46.dp testTag "bankMonogram"
 │           ├─ Center Column: bank name titleMedium, "active • total" bodySmall + InstitutionType label
 │           ├─ Right Column (Option B): Lkr.words(total) titleMedium bold primary
 │           │                          Lkr.exact(total) labelSmall gray
 │           │                          "invested" labelSmall gray
 │           └─ KeyboardArrowRight icon + testTag "summary:<bankName>"  e.g. "summary:NSB"
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
 └─ SettingsScreen (ui/screens/SettingsScreen.kt)
     ├─ Theme: System/Light/Dark
     ├─ Notifications: toggle + leadDays slider (feeds Home maturing strip + bell badge)
     └─ About, Reset demo data, Currency LKR
```

## 2. Shared components (ui/components/)

| Composable | File | Purpose | Key props / tags |
|---|---|---|---|
| `FdCard` | Components.kt | **Core FD row** — used by FdListScreen, CalendarScreen, RecycleBin, maturing strip (compact variant uses same data but compact wording) | `fd: FixedDeposit`, `today: LocalDate`, callbacks `onClick/onEdit/onRenew/onDelete`. Layout: fdNumber + StatusChip top row, **Option B amount** (Lkr.words primary titleLarge bold primary + Lkr.exact secondary labelSmall gray) + CountdownChip middle row, meta line `rate • tenorLabel • matures date` bodySmall, autoRenew icon, overflow menu MoreVert → DropdownMenu Edit/Renew/Delete |
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
> **FD cards (FdCard) — extended 2026-10-07:** Left column 2 lines — Primary `Lkr.words(amount)` titleLarge bold primary (e.g. `Rs 1 Million`), Secondary `Lkr.exact(amount)` labelSmall gray (e.g. `Rs 1,000,000.00`) — Option B extended from BankSummaryCard to FdCard. CountdownChip stays right. Legacy `Lkr.full()` no longer used in FdCard.
>
> **Maturing soon strip (HomeScreen LazyRow):** Compact `Lkr.compact()` remains (space-constrained) — not changed by #22.

Screenshots reference:
- `image-1.png` (Home By-institution) shows correct Option B: `Rs 1 Million / Rs 1,000,000.00 / invested`
- `image-2.png` (FdListScreen before extension) showed `Rs 1,000,000` numbers only — now updated to words+exact per extension.

## 5. Test selectors (contract — do not invent)

- `testTag("bankMonogram")` — any monogram tile
- `testTag("summary:<full bank name>")` — e.g. `summary:NSB`, `summary:Bank of Ceylon (BOC)` — Home → openBank helper uses swipeUntilTag + waitUntil + onAllNodes[0].click (no performScrollTo, maxSwipes 50, gate navgate-0500r9)
- Visible text: fdNumber like `"NSB-78412"`, amount words like `"Rs 1 Million"`, exact like `"Rs 1,000,000.00"`, chips `"Matured"`, `"Active"`, buttons `"Save FD"`, `"Renew"`, `"Delete"`, `"Restore"`, `"Cancel"`
- contentDescription: `"Add FD"`, `"Back"`, `"Edit"`, `"Sort"`, `"Recycle bin"`, `"Maturing soon"`, `"More actions"`, `"Auto-renew on"`
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
 ├─ data/FdRepository.kt, SampleData.kt
 ├─ domain/FdMath.kt (Lkr, Dates, FdMath), FdQueries.kt
 └─ ui/
     ├─ components/
     │   ├─ Components.kt (FdCard, StatusChip, CountdownChip, SectionHeader, StatMini, RenewDialog)
     │   ├─ BankMonogram.kt (BankMonogram)
     │   └─ InstitutionRegistry.kt (InstitutionRegistry, Institution, InstitutionType)
     ├─ screens/
     │   ├─ HomeScreen.kt (HomeScreen, BankSummaryCard private)
     │   ├─ FdListScreen.kt (FdListScreen)
     │   ├─ FdDetailScreen.kt (FdDetailScreen)
     │   ├─ AddEditFdScreen.kt (AddEditFdScreen)
     │   ├─ CalendarScreen.kt (CalendarScreen)
     │   ├─ RecycleBinScreen.kt (RecycleBinScreen)
     │   └─ SettingsScreen.kt (SettingsScreen)
     └─ theme/Theme.kt (statusPalette)
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
│ │ 9.5% p.a. • 12 months • matures 12 Dec 2026  [⋮] ││
│ └───────────────────────────────────────────────────┘│
│ ┌─ FdCard ──────────────────────────────────────────┐│
│ │ NSB-78413                          [MATURED]      ││
│ │ Rs 750 K                           [today]        ││
│ │ Rs 750,000.00                                      ││
│ │ 8% p.a. • 100 days • matures 07 Oct 2026     [⋮] ││
│ └───────────────────────────────────────────────────┘│
└───────────────────────────────────────────────────────┘

FdCard (official) — Components.kt
  Row1: fdNumber titleMedium bold weight1 + StatusChip
  Row2: Column weight1 { Lkr.words titleLarge bold primary + Lkr.exact labelSmall gray } + CountdownChip
  Row3: meta bodySmall onSurfaceVariant weight1 + autoRenew icon + overflow menu Box DropdownMenu
```

---

**For testers:** When a failure says "amount not found", check whether you are asserting `Lkr.full` (legacy numbers-only) vs new Option B `Lkr.words` + `Lkr.exact`. After 2026-10-07, FdCard expects words (e.g. "Rs 1 Million") AND exact (e.g. "Rs 1,000,000.00"), not just "Rs 1,000,000".
