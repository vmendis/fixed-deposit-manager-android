# Fixed Deposit (FD) Manager — Technical Specification

## 1. Project Overview

**Project Name:** Fixed Deposit (FD) Manager
**Platform:** Google Firebase (Backend), Android + Web (Frontend)

**Primary Goal:**
Enable users to securely track, manage, and receive alerts for Fixed Deposits (FDs) across multiple banks.

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

---

## 4. Core Features

### 4.1 Home Screen

**Displays:**

* Total invested amount
* Bank-wise summary cards:

  * Bank name
  * Total invested
  * Active FD count

**Behavior:**

* Tap card → FD Details Screen

---

### 4.2 FD Details Screen

**Displays:**

* List of FDs sorted by maturity date (ascending)

**Each FD Card Shows:**

* FD Number
* Amount
* Interest Rate
* Maturity Date
* Status (color-coded)

---

### 4.3 Add / Modify / Delete

**Add FD:**

* Form with validation

**Modify FD:**

* Pre-filled editable form

**Delete FD:**

* Soft delete using `isDeleted = true`

---

### 4.4 Auto-Renewal Handling

**Process:**

1. Mark old FD as:

   * `isActive = false`
   * `status = RENEWED`
2. Create new FD:

   * `parentFdId = oldFdId`

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
