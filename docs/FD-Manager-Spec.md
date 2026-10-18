# Fixed Deposit (FD) Manager — Technical Specification

## 1. Project Overview

**Project Name:** Fixed Deposit (FD) Manager
**Platform:** Google Firebase (Backend), Android + Web (Frontend)

**Primary Goal:**
Enable users to securely track, manage, and receive alerts for Fixed Deposits (FDs) held at **CBSL-regulated institutions** (licensed commercial banks, specialised banks, and finance companies) in Sri Lanka — with a safety-first guardrail that only supports regulated institutions, helping users avoid unprotected, unregulated deposits.

**Core Principles:**

* Security-first (financial data)
* Simple, clean UX
* Scalable from single-user → multi-user SaaS
* Audit-friendly (no destructive data loss)

---

## 2. Architecture Overview

### 2.1 Tech Stack

**Frontend:**

* Android: Kotlin + Jetpack Compose
* Web: React (recommended) OR Firebase Hosting + SPA

**Backend (Firebase):**

* Firestore (Primary database)
* Firebase Authentication (User management)
* Cloud Functions (automation/logic)
* Firebase Cloud Messaging (Notifications)
* Firebase Hosting (Web app)
* Firebase Security Rules (critical)

**Background Processing:**

* Android WorkManager (fallback)
* Preferred: Cloud Functions scheduler

---

## 3. Data Model (Firestore)

### 3.1 Collection Structure

```
users/{userId}/fds/{fdId}
```

---

### 3.2 FD Document Schema

```json
{
  "fdNumber": "string",
  "bank": "string",
  "amount": number,
  "openedDate": number,
  "duration": number,
  "durationMonths": number,
  "durationDays": number | null,
  "tenorUnit": "MONTHS | DAYS",
  "interestRate": number,
  "maturityDate": number,
  "branch": "string | null",
  "branchCode": "number | null",
  "payoutFrequency": "MONTHLY | AT_MATURITY",
  "renewOption": "CAPITALIZE | PAYOUT",
  "autoRenew": boolean,
  "isActive": boolean,

  "status": "ACTIVE | MATURED | RENEWED",
  "parentFdId": "string | null",

  "createdAt": "timestamp",
  "updatedAt": "timestamp",
  "isDeleted": boolean,
  "userId": "string"
}
```

> `bank` = display name of a **CBSL-regulated institution** (Licensed Commercial Bank, Licensed Specialised Bank, or Licensed Finance Company) selected from the Institution Registry. Free-text custom institutions are **not allowed** — the app refuses to save an FD if the institution is not in the registry. See §3.3.
>
> `duration` = legacy field (months) kept for backward compat — use `durationMonths` / `durationDays` + `tenorUnit` going forward. `durationMonths` = months when `tenorUnit=MONTHS`, `durationDays` = days when `tenorUnit=DAYS` (1–999). `tenorUnit` defaults to `MONTHS` for old data.
>
> `tenorUnit` = `MONTHS` (bank-style: 1,3,6,12,24,36,60) or `DAYS` (NBFI odd tenors: 30,60,90,100,180,300,364). 1-month and 100/300-day are first-class presets (issue #17).
>
> `payoutFrequency` = when the institution pays the interest (monthly vs at maturity) — always chosen when adding an FD (no default); shown on the FD card and detail.
>
> `renewOption` = stored instruction for renewals: `CAPITALIZE` (add accrued interest to the principal) or `PAYOUT` (reopen with the original sum, interest withdrawn); chosen at add/edit (form pre-selects `CAPITALIZE`), overridable per renewal in the renew dialog. `autoRenew` executes at app session start.

### 3.3 Institution Registry — CBSL-Only Guardrail

**Principle:** For user safety, FD Manager only tracks Fixed Deposits at **CBSL-regulated institutions**. Non-regulated entities (co-ops, unlicensed lenders, investment schemes) cannot be stored. The app must refuse to save with a clear safety message.

**Registry contents:**
- **Licensed Commercial Banks (LCB)** — ~26 (e.g., BOC, Peoples Bank, Sampath, HNB, Commercial, NSB, NDB, DFCC, Seylan, NTB, Pan Asia, Union, etc.)
- **Licensed Specialised Banks (LSB)** — ~6
- **Licensed Finance Companies (LFC)** — ~39 licensed under Finance Business Act No. 42 of 2011 (e.g., LOLC Finance, LOLC Development Finance, People's Leasing & Finance PLC, Serendib Finance, Citizens Development Business Finance CDB, Central Finance, Alliance Finance, LB Finance, Commercial Leasing, Orient Finance, Siyapatha, Singer Finance, Softlogic Finance, Vallibel Finance, HNB Finance, etc.)
- Total ~70 entries. Each entry: display name, short code (BOC/NSB/LOLC/PLC/CDB…), InstitutionType { BANK, FINANCE_COMPANY }, brand color, alias list for search/normalization (e.g., "LOLC", "People's Leasing", "PLC" → People's Leasing & Finance PLC).

**Registry implementation:**
- In-app only (bundled, offline, no network fetch) — `InstitutionRegistry` (evolved from `BankRegistry`). `BankRegistry.resolve(name)` → `InstitutionRegistry.resolve(name)` with same monogram fallback for defensive rendering, but **entry is blocked** if not found.
- `lastUpdated` field (e.g., "2026-08-18") sourced from CBSL "Registered Finance Leasing Establishments" and licensed banks lists. Updated with each app release via manual diff of cbsl.gov.lk. Optional helper script in `docs/` to fetch and compare, but manual review required.
- No image assets — monogram tiles only, via existing process (logo evidence → initials, luminance contrast rule).

**Safety UX:**
- Add/Edit institution picker is **searchable, grouped** — `Banks` / `Finance Companies`. No "Other… free-text" option.
- If user types name not in registry → inline validation: "Not found in CBSL regulated list. Check spelling or tap 'Request addition' if it's CBSL-licensed."
- "Request addition" → feedback intent with typed name + optional note. Team verifies against cbsl.gov.lk and adds in next release. No FD is saved until institution exists in registry.
- If user attempts to save with non-CBSL name (e.g., via restored data) → hard block: "For your safety, FD Manager only tracks FDs at CBSL regulated institutions. Learn more: cbsl.gov.lk"
- Info icon in picker header links to CBSL licensed lists for transparency.
- Home copy: "By institution" (not "By bank"), "N institutions". Empty states and dialogs audited for institution wording.

**Why no custom storage:**
- Prevents normalizing risky, unprotected deposits. The app is a tracker, not an advisor, but must not facilitate tracking of unregulated schemes.
- Keeps data clean — no duplicates, typos, or scam names.
- Fallback tile (neutral monogram) remains for defensive rendering of legacy unknown names, but entry path is closed.

---

## 4. Core Features

### 4.1 Home Screen

**Displays:**

* Total invested amount — hero uses natural wording `Lkr.words()` (e.g. "Rs 3.2 Million") + exact secondary `Lkr.exact()` ("Rs 3,200,000.00") — issue #20 — `StatMini` components for Active FDs / Institutions / Next maturity
* Institution-wise summary cards (grouped by Banks / Finance Companies) — **issue #22 Option B (now extended to FD cards per 2026-10-07):**

  * Component: `BankSummaryCard` in `HomeScreen.kt` — ElevatedCard with `BankMonogram` (46.dp) left, institution name + active/total counts center, amount right column
  * Institution name (from CBSL registry via `InstitutionRegistry`)
  * Institution type badge (Bank / Finance Company) — `identity.type.label`
  * Total invested — **leads with natural wording `Lkr.words()` (e.g. "Rs 3.25 Million", sub-million "Rs 750 K" per #20 K family) + exact secondary `Lkr.exact()` ("Rs 3,250,000.00") + "invested" label — Option B (words + exact + invested, 3 lines right column)**
  * Active FD count
  * Maturing soon strip: LazyRow of compact cards (`Lkr.compact()` + `CountdownChip`) — not changed by #22 (compact remains)

**Behavior:**

* Tap card → FD Details Screen (`FdListScreen` filtered by institution via `onOpenBank`)
* Copy: "By institution", "N institutions" (not "By bank")
* Home hero unchanged by #22 — By-institution cards updated to words + exact; FD cards now also Option B + payout (see §4.2)
* **Back navigation fix #21 (2026-10-13):** Back from bank list (`FdListScreen` → `popBackStack()`) returns to Home with `Total invested` hero visible at top (not mid-scroll clipped). Implemented via explicit `rememberLazyListState()` + `LaunchedEffect(Unit) { scrollToItem(0) }` in `HomeScreen` (gate `home-scroll-0100r9`), so state is not preserved across bank navigation. Bottom tab navigation (Home ↔ Calendar ↔ Settings) still preserves state via `saveState=true/restoreState=true` in `MainActivity`.

---

### 4.2 FD List / FD Cards (FdListScreen + FdCard component)

**Displays:**

* List of FDs sorted by maturity date (ascending) — `FdListScreen`
* Filter chips: `StatusFilter` (All / Active / Matured) + sort menu
* TopAppBar with `BankMonogram` for institution header

**Each FD Card (`FdCard` in `Components.kt`) Shows — Issue #22 Option B extended + payout frequency fix (2026-10-07):**

* FD Number (titleMedium bold) + `StatusChip` (ACTIVE/MATURED/RENEWED, color-coded via `statusPalette()`)
* **Amount — Option B natural wording (extended from By-institution to FD cards):**
  * Primary: `Lkr.words(amount)` — e.g. `Rs 1 Million`, `Rs 3.25 Million`, sub-million `Rs 750 K` (same K family as #20)
  * Secondary: `Lkr.exact(amount)` — e.g. `Rs 1,000,000.00`, `Rs 750,000.00` (labelSmall, onSurfaceVariant)
  * Both in left column, `CountdownChip` (days-to-maturity, `FdMath.countdownShort`) on right
* **Meta line — Rate • Tenor • Payout • Matures — bodySmall onSurfaceVariant (spec fix 2026-10-07, was missing payout):**
  * Interest rate: `FdMath.formatRate(fd.interestRate)% p.a.` — e.g. `10% p.a.`, `9.5% p.a.`
  * Tenor: `FdMath.tenorLabel(fd)` — e.g. `12 months`, `3 months`, `100 days`, `1 month` (unit-aware from `tenorUnit`)
  * **Payout frequency: `fd.payoutFrequency.label` — `Monthly payout` / `At maturity` (required, from `PayoutFrequency` enum, issue #25) — THIS WAS MISSING in V21 FdCard, now added**
  * Maturity: `matures ${Dates.format(fd.maturityDate)}` — e.g. `matures 10 Oct 2026`
  * Full example: `10% p.a. • 12 months • Monthly payout • matures 10 Oct 2026` or `9.5% p.a. • 100 days • At maturity • matures 07 Oct 2026`
  * Spacing: single Text with ` • ` separators (Option A minimal), wraps to 2 lines on small screens if needed; alternative Option B two-line meta (Rate•Tenor / Payout•Matures) documented in `docs/fdcard-payout-options.html`
* Institution monogram tile is in `FdListScreen` header, not per card (per-card institution shown via fdNumber grouping)
* Overflow menu: Edit / Renew / Delete (when callbacks provided) → `RenewDialog` / `AlertDialog`
* Auto-renew icon when `fd.autoRenew` — `Icons.Filled.Autorenew` 16.dp primary

**Detail view (`FdDetailScreen`) additionally shows:**

* `Tenor` row: `3 months` or `100 days` (unit-aware, from `tenorUnit`)
* `Interest payout` row (Details card)
* `Amount` rows: both words + exact (reuses `Lkr.words()` / `Lkr.exact()`)
* `Institution` row with type (Bank / Finance Company) and CBSL info link, `BankMonogram`
* **Renewal card**: stored instruction (`Add interest to capital` / `Withdraw interest`),
  new-FD principal preview (principal + accrued interest for `CAPITALIZE`, principal only
  for `PAYOUT`), and an instruction-aware auto-renew banner

---

### 4.3 Add / Modify / Delete

**Add FD:**

* Form with validation
* **Institution picker (CBSL-only):** searchable dialog grouped `Banks` / `Finance Companies`, backed by InstitutionRegistry (~70 entries). No free-text. Selection required. If typed name not in registry → inline error "Not found in CBSL regulated list. Check spelling or tap 'Request addition' if it's CBSL-licensed." Request addition opens feedback intent; no FD saved until institution exists in registry. Hard block on non-CBSL attempt: "For your safety, FD Manager only tracks FDs at CBSL regulated institutions. Learn more: cbsl.gov.lk"
* **Tenor (issue #17):** segmented toggle `Months | Days`. Months presets: 1,3,6,12,24,36,60 (1-month first-class) + custom 1–120. Days presets: 30,60,90,100,180,300,364 + custom 1–999. 100/300-day are NBFI specials. Live maturity preview. Validation: >0.
* **Required:** interest payout frequency (`Monthly payout` / `At maturity`) — save is blocked without a choice ("Select when interest is paid")
* **Renewal instruction** selector, pre-selects `Add interest to capital`
* `Auto-renew` toggle (default off)

**Modify FD:**

* Pre-filled editable form (including institution — still CBSL-only, cannot change to non-CBSL; tenor unit + value + payout frequency + renewal instruction)

**Delete FD:**

* Soft delete using `isDeleted = true`

---

### 4.4 Auto-Renewal Handling

**Renewal (manual or automatic) process:**

1. Mark old FD as:

   * `isActive = false`
   * `status = RENEWED`
2. Create new FD:

   * `parentFdId = oldFdId`
   * opens on the old FD's maturity date, same terms (tenor unit + value preserved)
   * **principal per the effective renew option:**
     * `CAPITALIZE` → `amount + interestEarned(amount, interestRate, durationMonths/days, tenorUnit)`
     * `PAYOUT` → original `amount` (interest paid out, not tracked in-app in v1)

**Renew dialog:** shows both options as radio choices, pre-selected from the
FD's stored `renewOption`, with the resulting next principal in the copy; the user can
override the stored instruction for that renewal.

**Auto-renew sweep:** at app session start, every FD with
`autoRenew = true`, maturity reached, and `status != RENEWED` renews itself using its
stored `renewOption` (no user interaction; works while the app opens).

### 4.5 Institution Safety & Request Flow

- **CBSL-only enforcement:** Add/Edit form validates institution against InstitutionRegistry at save time. No custom free-text storage. If institution string is not in registry → block save, show safety message with CBSL link.
- **Request addition:** "Request addition" button in picker → pre-filled feedback (typed name + optional note). Team verifies against cbsl.gov.lk licensed banks and finance companies lists. If verified, added to registry in next app release (display name, code, type, brand color, aliases). `lastUpdated` bumped.
- **Defensive rendering:** If legacy data contains unknown institution name (e.g., from old backup), UI renders neutral monogram fallback (derived initials) but shows warning badge and blocks editing until user selects CBSL institution.
- **Transparency:** Picker header info icon → CBSL licensed institutions page. Home and detail screens show institution type. No "Other…" group — only Banks / Finance Companies.
- **Disclaimer:** App is a tracker, not a financial advisor. Institution list is for information, sourced from CBSL as at `lastUpdated` date.

### 4.6 Tenor Support — Day-Based & 1-Month (Issue #17)

- **Tenor unit:** `MONTHS` (bank-style) or `DAYS` (NBFI odd tenors). Exclusive — an FD is either months or days, not both.
- **Months:** 1–120, presets 1,3,6,12,24,36,60. 1-month is first-class (common for finance companies). Interest = `amount * rate * months / 12`.
- **Days:** 1–999, presets 30,60,90,100,180,300,364. 100/300-day are NBFI specials (People's Leasing, LOLC etc.). Interest = `amount * rate * days / 365` (simple, 365-day year). Maturity = `openedDate + days`.
- **Renewal:** tenor unit + value preserved on child FD. Child opens on old maturity date, matures per its own tenor.
- **Display:** card meta shows `100 days` or `3 months` + payout frequency; detail shows Tenor row with unit.
- **Backward compat:** old data has `duration` (months) and no `tenorUnit` → treated as `MONTHS` with `durationMonths = duration`. New data writes `durationMonths`/`durationDays` + `tenorUnit`.
- **Validation:** tenor value >0, required.

### 4.7 Branding — vssbox (Issue #27)

**Company:** vssbox — blue neon isometric cube logo (dark bg #0B1220), transparent PNG `vssbox_cube.png` derived from `vssbox_logo.jpg` (manual alpha clean — corner 0,0,0,0).

**Product vs company:** Keep **FD Manager** as product name (users search FD Manager), add **by vssbox** as company — low risk, clear. Full rename to vssbox later if building suite.

**Asset strategy:**
- `app/src/main/res/drawable/vssbox_cube.png` — 1024x1024 RGBA transparent, manual cleaned (gate `vssbox_cube_transparent_manual.png` 408K)
- `ic_launcher_background` = #0B1220 matches logo dark bg
- Adaptive icon: `drawable-anydpi-v26/ic_launcher.xml` + `mipmap-anydpi-v26/ic_launcher.xml` — background #0B1220, foreground `vssbox_cube`, monochrome same for Android 13+ themed icons
- Fallback `drawable/ic_launcher.xml` — layer-list dark bg + cube 72dp centered for < API 26

**Placements (official screens):**
- **Launcher icon:** blue cube on dark bg — strongest branding, replaces teal FD icon
- **Splash screen (Android 12+):** cube centered dark bg + vssbox below (future — theme splash uses same bg)
- **OnboardingScreen (new-user, #1 + #27):** primary branding home — gate `onboarding-0100r9`
  - 96dp cube + `vssbox` headlineMedium bold primary + `FD Manager` titleLarge + `by vssbox • Secure box for your deposits` labelMedium
  - Trust card: `We never ask for your bank login. Your data is encrypted and only you can see it. Runs entirely on local sample data in this mock — no account, no backend, no real money.` + lock icon
  - Features: Track all FDs CBSL-only, Rate•Tenor•Payout•Matures, Renewal chain & Recycle bin
  - CTA: `Get Started — Add your first FD` Button testTag `getStartedButton`, `Explore demo data first` TextButton testTag `exploreDemoButton`
  - Footer: 16dp cube + `Built by vssbox • © 2026 vssbox` labelSmall testTag `builtByVssboxFooter`
  - TestTag `onboardingScreen`, `vssboxLogoOnboarding`, `vssboxNameOnboarding`, `trustCard`
  - Logic: `SettingsStore.hasSeenOnboarding` false → show onboarding full-screen, true → Home. `setHasSeenOnboarding(true)` on CTA.
- **HomeScreen:**
  - TopAppBar title: `FD Manager` bold + `by vssbox` labelSmall onPrimary 80% alpha — keeps product name, adds company
  - Footer: `Built by vssbox • © 2026 vssbox • FD Manager by vssbox` with 16dp cube, centered, testTag `builtByVssboxHome` — low emphasis onSurfaceVariant
- **SettingsScreen → About:**
  - Primary branding home — 72dp cube + `vssbox` titleLarge bold primary + `FD Manager` titleMedium + `by vssbox` labelSmall
  - Version: `Version 1.0 · front-end mock · © 2026 vssbox` bodySmall
  - Description: `Secure box for your deposits — Track CBSL-regulated FDs (24 LCB + 6 LSB + 32 LFC). Runs entirely on local sample data. No account, no backend, no real money — FDs shown are fictional.`
  - Row: `Built by vssbox • vssbox.com (coming soon)` — testTag `aboutBranding`, `vssboxLogoAbout`
  - Demo mode toggle: `SettingsStore.isDemoMode` + `setDemoMode()` — opt-in demo per #1
- **Avoid:** FdCard, BankSummaryCard — those show institution monograms (BOC/NSB/COM). Mixing company logo there would confuse institution identity.

**Color:** Keep teal primary #006A5F for now, blue #5B9CF6 as brand accent (About + onboarding + footer). Shift primary to blue later if desired — bigger theme change.

**Acceptance #27:**
- Owner confirms: app name stays FD Manager, by vssbox added
- Launcher shows blue cube on dark bg
- Onboarding shows vssbox cube + name + trust line + CTA, gate `onboarding-0100r9`
- Home footer shows Built by vssbox + cube
- Settings About shows 72dp cube + vssbox + version + ©
- Spec both copies identical, wire diagram updated
- Tests still GREEN (46/16) — onboarding flag defaults false, but SettingsStore in-memory, tests set hasSeenOnboarding true via @Before

### 4.8 Onboarding Funnel & Empty States (Issue #1)

**Scope — from roadmap #1:**
- Welcome screen on first launch with plain-English trust line: "we never ask for your bank login; your data is encrypted and only you can see it"
- Guided "Add your first FD" flow leading straight into the form
- Proper empty states for Home, FD list, Calendar and Recycle bin (illustration-free is fine; clear copy + primary action button)
- Sample data becomes an opt-in demo mode (Settings toggle) instead of always-seeded

**OnboardingScreen implementation (extends #27 branding):**
- Gate `onboarding-0100r9` + `demo-0100r9`
- Shown when `SettingsStore.hasSeenOnboarding == false` (fresh install default false per #1)
- NavHost startDestination = ONBOARDING if not seen, else HOME
- 96dp cube `vssboxLogoOnboarding` + vssbox name + FD Manager + by vssbox tagline
- Trust card `trustCard`: lock icon + "Your data stays yours" + trust line from #1
- FeatureRow ×3: Track all FDs CBSL-only, Rate•Tenor•Payout•Matures, Renewal chain & Recycle bin
- CTA: `Get Started — Add your first FD` `getStartedButton` → `setHasSeenOnboarding(true)` + `nav.navigate(ADD)` with popUpTo ONBOARDING inclusive — leads straight into form
- Secondary: `Explore demo data first` `exploreDemoButton` → `setHasSeenOnboarding(true)` + `toggleDemoMode(true)` + navigate HOME — opt-in demo
- Footer `builtByVssboxFooter` 16dp cube + © 2026 vssbox
- TestTags: `onboardingScreen`, `vssboxLogoOnboarding`, `vssboxNameOnboarding`, `trustCard`, `getStartedButton`, `exploreDemoButton`, `builtByVssboxFooter`

**Guided Add first FD flow:**
- Fresh install → Onboarding → Get Started → AddEditFdScreen (form with validation, CBSL picker, Months|Days presets, payout required)
- Save → FD visible on Home with correct totals (total invested, active count, next maturity, by institution)
- Acceptance: fresh install → welcome → add first FD → FD visible on Home with correct totals

**Empty states — all screens deliberate, illustration-free, clear copy + primary action button (where applicable):**
- **HomeScreen** (when summaries empty): `EmptyState` icon AccountBalance, title "No deposits yet", message "Welcome! Add your first fixed deposit to see totals, maturing soon, and by institution. Your data is encrypted and only you can see it." (or demo hint if demo on), actionLabel "Add your first FD" → onAddFd (navigates to ADD), testTag `emptyStateAction:No deposits yet`
- **FdListScreen** (bank-filtered, list empty): "No FDs here" + "This institution has no deposits. Add your first FD for ${bank} or try demo mode in Settings." + action "Add FD" → onAdd when filter ALL, else no action (filter message)
- **CalendarScreen**: "Pick a day" + "Days with dots have FD maturities. Add FDs to see them here." when no day selected; "Quiet day" + "No FDs mature on ${date}. Add an FD or check another date." when selected day has none — no CTA (pick day)
- **RecycleBinScreen**: "Bin is empty" + "Deleted FDs land here thanks to soft-delete — restored with one tap." — no CTA needed, but clear copy
- **Component**: `EmptyState(icon, title, message, actionLabel?, onAction?)` — optional Button with testTag `emptyStateAction:$title`

**Demo mode opt-in:**
- `SettingsStore.isDemoMode` default **false** per #1 (opt-in, not always-seeded)
- `FdRepository.get()` initial: empty if demo off, SampleData.seed if demo on (checks SettingsStore flag)
- `setDemoMode(enabled, sample)`: on → add sample FDs (fd1..fd8) if missing, keep user FDs; off → remove sample FDs by id, keep user FDs — preserves user data, no corruption
- `FdViewModel.toggleDemoMode(enabled)`: sets SettingsStore + repo setDemoMode
- SettingsScreen: Demo section with Switch `isDemoMode`, subtitle "Sample FDs visible (opt-in demo)" / "Hide sample data — clean start", plus Reset demo data and Show onboarding rows
- Tests: @Before sets `SettingsStore.setHasSeenOnboarding(true)` + `setDemoMode(true)` + `FdRepository.reset(sample)` to keep 46/16 GREEN, since defaults are false for fresh install
- Acceptance: demo on/off works without corrupting user data — user-added FDs survive toggle

**Acceptance #1:**
- Fresh install (hasSeenOnboarding=false, isDemoMode=false) → welcome → Get Started → Add first FD → FD visible on Home with correct totals
- Every screen has deliberate empty state with CTA where applicable
- Demo mode on/off works without corrupting user data
- Tests still GREEN 46/16 with @Before setting flags true
- Spec both copies identical, wire diagram updated with onboarding funnel + empty states

### 4.9 Application ID Rename & Rebrand (Issue #13)
**Scope — from roadmap #13:** Rename `com.example.fdmanager` → `com.vssbox.fdmanager` (Play Store blocks `example.*`). App name stays FD Manager, by vssbox. Launcher icon finance teal/green identity, splash.

**Implementation #13:**
- `app/build.gradle.kts`: `namespace = "com.vssbox.fdmanager"`, `applicationId = "com.vssbox.fdmanager"` — Play Store requires non-example ID, `com.vssbox.*` groups future vssbox apps (budget, todo etc.)
- Source package `com.example.fdmanager` kept for now (allowed — namespace independent), future move to `com.vssbox.fdmanager` optional. R class now `com.vssbox.fdmanager.R`
- Clean build under new ID installs side-by-side with old `com.example.fdmanager` debug build (different applicationId)
- Owner email for Firebase/Play: `vssbox.com@gmail.com` (or `hello@vssbox.com` via Workspace, you own vssbox.com domain), secondary owner TBD — vmendis@gmail.com NOT secondary per owner rule
- Firebase projects per app for free-tier max: `vssbox-fd-manager-dev` (Spark/emulator) + `vssbox-fd-manager-prod` (Blaze + $5 budget alert) — each project gets own 50K reads/day free, total free multiplies. One company owner, many projects, not many gmails
- Play Console: single developer account under `vssbox.com@gmail.com` can publish many apps `com.vssbox.*` — $25 once, not per app

**Acceptance #13:**
- `com.vssbox.fdmanager` builds, installs, runs 46/46 + 16/16 GREEN (package rename doesn't break tests)
- Old `com.example.fdmanager` debug build remains installable side-by-side (different applicationId)
- Spec both copies identical

---

## 5. Notifications

**Trigger:** Daily at 10:00 AM

**Logic:**

* Find FDs maturing within 5 days

**Implementation:**

* Firebase Cloud Functions (scheduled)
* Firebase Cloud Messaging (push notifications)

---

## 6. Authentication Strategy

**Provider:** Firebase Authentication

**Methods:**

* Email + Password (primary)
* Google Sign-In (optional)

**User Flow:**

1. User registers (web or app)
2. Firebase assigns `userId`
3. Create user profile document

---

## 7. Data Security

### 7.1 Firestore Rules

```javascript
match /users/{userId}/fds/{fdId} {
  allow read, write: if request.auth != null
                     && request.auth.uid == userId;
}
```

### 7.2 Security Measures

* Encryption at rest (Firebase default)
* Encryption in transit (HTTPS/TLS)
* App Check
* Input validation

---

## 8. Dev Workflow

**Repo Structure:**

```
/android-app
/web-app
/functions
/firestore-rules
/docs
```

---

## 9. Future Enhancements

* Interest tracking
* Multi-currency support
* Analytics dashboard
* Export to CSV / Sheets
* Role-based access

---

## 10. Open Questions

1. Should we support multiple currencies?
2. Do you need interest payout tracking?
3. Notification detail level?
4. Manual vs assisted renewal?
5. Biometric/PIN lock required?
6. Priority: Android vs Web?

---

## 11. Key Recommendation

* Build multi-user from day one
* Use Firebase Auth immediately
* Keep UI simple initially

---

(End of document)
