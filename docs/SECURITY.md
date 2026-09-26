# Security & Trust — FD Manager

This document explains, in public, how FD Manager protects your financial data —
both in plain English (for users) and in technical detail (for the curious and
for reviewers). This app is open source: **don't take this document's word for
anything — the code and the configuration are auditable.**

---

## The promises, in plain English

1. **We never ask for your bank login. Ever.** FD Manager is a *manual tracker*:
   you type in your FD details yourself. It is not an "aggregator" that needs
   your bank credentials, and it never reads your SMS, contacts, or other apps.
2. **Only you can see your data.** Every account's data is fenced by a
   server-side rule enforced by Google's infrastructure — not by the app. Even
   a modified copy of this app cannot read another user's data.
3. **Encrypted everywhere.** All traffic uses TLS (mandatory); stored data is
   encrypted at rest by the hosting infrastructure (Google Cloud / Firebase).
4. **Your exits are unlocked.** Full data export, and account deletion that
   removes all your data, are built-in features — not support requests.
5. **No ads, no data selling.** The data collected is listed below, in full.
   Nothing else is collected, and nothing is shared.

---

## Current status (honesty section)

> **The app is currently in the front-end mock phase.** All data lives
> in-memory on the device, is never persisted, and never leaves the phone.
> There is no backend, no account system, and no network traffic.
>
> This document describes the **target security architecture** for the
> Firebase backend phase. Where a control is *planned* rather than *active*,
> it is marked as such.

---

## Multi-user tenancy: how isolation works

### Identity — Firebase Authentication

- Users sign in with email + password (or Google Sign-in).
- Passwords are hashed by Firebase Auth; this project never stores or sees them.
- After sign-in, the app holds a **cryptographically signed, short-lived token**
  containing the user's `uid`, issued by Google. The signature cannot be forged
  or edited on the client.

### Data model — tenancy baked into the shape

There is no shared collection of deposits. Every document lives under its
owner's uid:

```
users/{uid}/fds/{fdId}
```

There is no query that *could* accidentally span users, because other users'
data is not merely filtered out — it is not addressable from your session
except by an explicit path that the rules below then reject.

### Authorization — Firestore Security Rules

Every read and write is re-authorized **on Google's servers, per request**,
against rules that ship with the project. The core rule (planned):

```
match /users/{userId}/fds/{fdId} {
    allow read, write: if request.auth != null
                     && request.auth.uid == userId;
}
```

Translation: *only the signed-in user whose uid matches the folder can touch
anything inside it.*

### Threat model — what happens when "Joe" attacks "Jane"

| Attack | Outcome |
|---|---|
| Joe pokes around the app UI | The app can only request paths under Joe's own uid; Jane's data is never even fetched to his device |
| Joe reverse-engineers the APK and requests Jane's document path directly | Rules run server-side against the uid inside Joe's **signed token** → `permission-denied` |
| Joe skips the app and hand-crafts raw Firestore API calls | Same wall — the check does not live in the app binary at all |
| Joe forges a token claiming to be Jane | Token signature verification fails before rules are evaluated |
| Joe floods the backend with junk requests | Per-project quotas and budget alerts cap the damage; App Check (planned) rejects non-genuine clients |

---

## Defense in depth

| Layer | Control | Status |
|---|---|---|
| Identity | Firebase Auth — hashed passwords, signed short-lived tokens, Google Sign-in option | planned |
| Authorization | Firestore Security Rules — server-side, per-request, uid-in-path | planned |
| Data model | Per-user subcollections; no shared/global documents | **active** (schema already shaped this way) |
| Client attestation | Firebase App Check with Play Integrity — rejects repackaged apps & scripts | planned |
| Transport | TLS enforced for all traffic | planned (Firebase default) |
| At rest | Google Cloud encryption of stored data | planned (Firebase default) |
| Device | Optional biometric app-lock; soft-delete recycle bin instead of destructive deletes | mock has recycle bin; app-lock planned |
| Ops | 2FA on the Firebase admin account; budget alerts; least-privilege project access | policy |

---

## What this architecture does **not** protect against

Honesty matters more than marketing, so here are the real residual risks:

1. **Your own credentials.** Phishing, password reuse, or an unlocked phone
   handed to a stranger defeat any backend. Mitigations: prefer Google
   Sign-in, enable the biometric app-lock, and use a password manager.
2. **The project's admin account.** Whoever controls the Firebase project
   controls the database. This project's policy: 2FA on the admin Google
   account, no shared credentials, least-privilege access.
3. **Data you export yourself.** An exported file on shared storage is your
   responsibility — exports are opt-in and clearly labeled.
4. **Trust in the cloud provider.** At-rest encryption and infrastructure
   security are delegated to Google Cloud, whose compliance posture
   (independent audits, ISO/SOC certifications) is public.

**Policy:** the database never runs in Firestore "test mode"
(`allow read, write: if true`) with real data. Rules are deployed *before*
the first real record is written.

---

## Data handling — the complete list

**Collected (once the backend exists):**

| Data | Why |
|---|---|
| Email address | Account identity & sign-in |
| FD records you enter (FD number, bank, amount, rate, dates, branch, notes) | The product's entire purpose — shown only to you |
| App settings (theme, notification lead time) | Synced for your convenience |

**Never collected:** bank credentials or PINs · SMS or call logs · contacts ·
location · advertising identifiers · anything from other apps.

**Deleted:** account deletion removes the user document and everything under
it. Deletions inside the app are soft (recycle bin) so accidents are
recoverable; "delete forever" is explicit.

---

## Reporting a security issue

Please **do not** open a public issue for security vulnerabilities. This
repository uses **GitHub Private Vulnerability Reporting**: go to the
**Security** tab of the repository and click **"Report a vulnerability"**.
Reports are visible only to the maintainers, and we'll coordinate disclosure
before any fix is published.

---

*This document evolves with the project. When the Firebase backend ships,
every "planned" control above becomes "active" and the deployed rules file
will be linked here.*
