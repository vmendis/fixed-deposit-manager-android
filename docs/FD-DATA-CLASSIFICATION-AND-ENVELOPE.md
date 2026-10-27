# FD Manager — Field Classification & Sync Envelope Schema

| | |
|---|---|
| **Version** | 1.1 (FINAL) |
| **Date** | 2026-10-27 |
| **Status** | FINAL — all §F items confirmed by owner 2026-10-27 in the roadmap-23 close-out. |
| **Inputs** | `FD_Manager_Security_Architecture.md` (owner + security advisor) §2–§5; repo `docs/FD-Manager-Spec.md` §3.2; code `FixedDeposit.kt` @ `67464fa` |
| **Size / md5** | *(filled in footer)* |

---

## A. Classification rule

Under the owner-inaccessible-plaintext decision (option (c)), every piece of FD data
falls into exactly one of four buckets:

| Bucket | Meaning |
|---|---|
| **BLOB** | Lives only inside the AES-256-GCM ciphertext. Server/owner can never read it. |
| **ENVELOPE** | Cleartext on the Firestore document. Needed by the server/sync machinery itself. |
| **PATH** | Not a field at all — encoded in the document path `users/{uid}/fds/{fdId}`. |
| **DROPPED** | Does not exist in the sync contract. |

**Default is BLOB. Every ENVELOPE entry needs a stated reason.**

## B. Field classification — union of spec §3.2 (23 fields) + `FixedDeposit.kt` (21 fields)

| # | Field | In code? | In spec §3.2? | Class | Reason / note |
|---|---|---|---|---|---|
| 1 | `id` | ✅ | — (doc ID) | **PATH** + inside BLOB | Firestore document ID. Also serialized inside the blob and bound as AEAD associated data (see D3) so a blob cannot be copied onto another document. |
| 2 | `fdNumber` | ✅ | ✅ | **BLOB** | Account-identifying — highest sensitivity. |
| 3 | `bank` | ✅ | ✅ | **BLOB** | Reveals customer's institutions. |
| 4 | `amount` | ✅ | ✅ | **BLOB** | Core financial data. |
| 5 | `openedDate` | ✅ | ✅ | **BLOB** | Financial behavior. |
| 6 | `duration` (legacy) | ❌ | ✅ | **DROPPED** | Dead legacy field (months). Client migrates to `durationMonths` before first encryption. Never enters the sync contract. |
| 7 | `durationMonths` | ✅ | ✅ | **BLOB** | — |
| 8 | `durationDays` | ✅ | ✅ | **BLOB** | — |
| 9 | `tenorUnit` | ✅ | ✅ | **BLOB** | — |
| 10 | `interestRate` | ✅ | ✅ | **BLOB** | — |
| 11 | `compounding` | ✅ | ✅ | **BLOB** | — |
| 12 | `maturityDate` | ✅ | ✅ | **BLOB** | **Deliberate call:** stays encrypted → server-side maturity reminders are impossible, forever. Reminders are scheduled on-device. (Security doc §2.1 wins over convenience.) |
| 13 | `branch` | ✅ | ✅ | **BLOB** | — |
| 14 | `branchCode` | ✅ | ✅ | **BLOB** | — |
| 15 | `payoutFrequency` | ✅ | ✅ | **BLOB** | — |
| 16 | `renewOption` | ✅ | ✅ | **BLOB** | — |
| 17 | `autoRenew` | ✅ | ✅ | **BLOB** | — |
| 18 | `isActive` | ✅ | ✅ | **BLOB** | — |
| 19 | `status` | ✅ | ✅ | **BLOB** | Even ACTIVE/MATURED counts leak financial posture. |
| 20 | `parentFdId` | ✅ | ✅ | **BLOB** | Renewal chains reveal behavior patterns. |
| 21 | `isDeleted` | ✅ | ✅ | **BLOB** | Soft-delete travels inside the blob; the document itself is the tombstone. Server never knows deleted-vs-live ratio. |
| 22 | `createdAt` | ✅ | ✅ | **BLOB** (domain) + **ENVELOPE** (server) | Domain value (user's device clock) inside the blob. The envelope carries a separate *server* timestamp — see C. |
| 23 | `updatedAt` | ❌ | ✅ | **ENVELOPE** | Sync machinery: last-write-wins conflict resolution needs it readable. Server sets it; it is not domain data. |
| 24 | `userId` | ❌ | ✅ | **PATH** | `users/{uid}/…`. Redundant as a field — dropped. Security rules enforce it from the path. |

**Result: all 21 domain fields → BLOB. Zero domain fields in cleartext. The only cleartext is crypto bookkeeping + two server timestamps.**

## C. Envelope schema — the complete Firestore document (everything the owner can ever see)

```
users/{uid}/fds/{fdId}
├── blob            bytes    AES-256-GCM ciphertext of the plaintext contract (§D)
├── nonce           bytes    96-bit, unique per write, never reused
├── encVersion      int      ciphersuite id (1 = Tink AES-256-GCM). For future algorithm migration.
├── keyVersion      int      which wrapped DEK decrypts this blob (supports §5.7 key rotation)
├── createdAt       server timestamp
└── updatedAt       server timestamp
```

Key material lives in a sibling collection exactly per security doc §3.2 / §5
(`users/{uid}/keys/…`: recovery-key-wrapped DEK envelope, trusted-device-wrapped
envelopes, all versioned). No plaintext key ever leaves a device.

## D. Plaintext contract rules (the thing that actually freezes)

1. **`schemaVersion` is the first field inside the blob**, from day one. The server can
   never migrate ciphertext it cannot read → every schema change ships with client-side
   migration code, maintained forever. v1 is seeded from the FdExport JSON shape.
2. **Defaults are law.** Every new field MUST have a default that reproduces pre-field
   behavior (precedent: `compounding=SIMPLE` for pre-#6 records, `tenorUnit=MONTHS` for
   pre-#17). A client reading `schemaVersion: 1` with v3 code applies defaults, never fails.
3. **AEAD binding:** `uid + fdId` passed as associated data on every encrypt/decrypt
   (Tink supports this natively). A blob moved to another document or another user
   fails authentication instead of decrypting.
4. **Freeze trigger:** `schemaVersion: 1` is declared final at the **first sync build**
   (#8 era), not before. Until then the domain model keeps evolving freely — encryption
   makes domain-model changes invisible to the server schema.

## E. Leak budget — what the operator can still see (to be disclosed in the privacy policy)

uid · auth email · sign-in times · number of FD documents · blob sizes · write timestamps
· device/key-envelope count. **Content: none.**

## F. Decisions — ✅ ALL FOUR CONFIRMED by owner, 2026-10-27 (roadmap-23 close-out)

1. All 21 domain fields in the blob — **zero cleartext exceptions** (accepting: no
   server-side reminders, ever).
2. `duration` legacy field is dropped from the sync contract.
3. Soft-delete travels inside the blob (no cleartext tombstone flag).
4. `schemaVersion`-inside-blob + defaults-are-law + AEAD binding adopted as stated.

---
*Footer: v1.1 · size and md5 recorded in the session log at commit time. Sensitive info: NO — generic schema design, no credentials, no personal data.*
*History: v1.0 DRAFT 2026-10-27 (6,349 B, md5 97450f7a) → v1.1 FINAL 2026-10-27 (§F confirmed, status FINAL).*
