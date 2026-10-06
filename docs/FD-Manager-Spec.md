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

* Total invested amount
* Institution-wise summary cards (grouped by Banks / Finance Companies):

  * Institution name (from CBSL registry)
  * Institution type badge (Bank / Finance Company)
  * Total invested
  * Active FD count

**Behavior:**

* Tap card → FD Details Screen
* Copy: "By institution", "N institutions" (not "By bank")

---

### 4.2 FD Details Screen

**Displays:**

* List of FDs sorted by maturity date (ascending)

**Each FD Card Shows:**

* FD Number
* Amount
* Interest Rate
* Interest payout frequency (`Monthly payout` / `At maturity`)
* Maturity Date
* Status (color-coded)
* Institution monogram tile (from InstitutionRegistry)

**Detail view additionally shows:**

* `Interest payout` row (Details card)
* `Institution` row with type (Bank / Finance Company) and CBSL info link
* **Renewal card**: stored instruction (`Add interest to capital` / `Withdraw interest`),
  new-FD principal preview (principal + accrued interest for `CAPITALIZE`, principal only
  for `PAYOUT`), and an instruction-aware auto-renew banner

---

### 4.3 Add / Modify / Delete

**Add FD:**

* Form with validation
* **Institution picker (CBSL-only):** searchable dialog grouped `Banks` / `Finance Companies`, backed by InstitutionRegistry (~70 entries). No free-text. Selection required. If typed name not in registry → inline error "Not found in CBSL regulated list. Check spelling or tap 'Request addition' if it's CBSL-licensed." Request addition opens feedback intent; no FD saved until institution exists in registry. Hard block on non-CBSL attempt: "For your safety, FD Manager only tracks FDs at CBSL regulated institutions. Learn more: cbsl.gov.lk"
* **Required:** interest payout frequency (`Monthly payout` / `At maturity`) — save is blocked without a choice ("Select when interest is paid")
* **Renewal instruction** selector, pre-selects `Add interest to capital`
* `Auto-renew` toggle (default off)

**Modify FD:**

* Pre-filled editable form (including institution — still CBSL-only, cannot change to non-CBSL; payout frequency + renewal instruction)

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
   * opens on the old FD's maturity date, same terms
   * **principal per the effective renew option:**
     * `CAPITALIZE` → `amount + interestEarned(amount, interestRate, duration)`
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
