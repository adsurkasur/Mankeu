---
status: implemented (backup and auth); sync section superseded by docs/SYNC_ARCHITECTURE.md
last_verified: 2026-08-28
audience: human-and-ai
sensitivity: internal
evidence_basis:
  - owner decision 2026-08-28 (D-033): Firebase Authentication as the identity layer
  - app/src/main/java/com/mankeu/backup/EncryptedBackupManager.kt
---

# Cloud sync and authentication

> **2026-10-22 (DEC-024):** sync between phones is a release feature in Pengaturan. **2026-10-20 (DEC-013):** the owner chose multi-device sync. The "What this does not do" section below is superseded; the sync framework is described in `docs/SYNC_ARCHITECTURE.md`. Snapshot backup to Drive remains as described here.

## Status, stated plainly

This document is a design, not a description of shipped code. **No Firebase project, Google Cloud
project, OAuth client, or Drive integration was created while writing it**, and none can be: the
`google-services.json` that the Google Services Gradle plugin requires cannot exist until the
package identifier is decided (O-002), and the OAuth client that Google Sign-In requires is keyed to
a signing certificate that does not exist yet (O-010). Adding the plugin without that file fails the
build outright, and a hand-rolled stub that "looks like" sign-in would be the dead-button problem
D-027 just removed, one layer deeper.

What is settled is the shape, and it is settled in a way that makes the remaining work small and the
security properties checkable before a single line is written.

## The one decision that makes everything else easy

**Drive never receives readable data.** It receives the byte-for-byte `.aabk` envelope that
`EncryptedBackupManager` already produces: AES-256-GCM, key derived by PBKDF2-HMAC-SHA256 at 310,000
iterations from a passphrase the user chooses, with the envelope magic bound in as AAD so a
truncated or spliced file fails authentication instead of decrypting to garbage.

The consequence is worth stating in full, because it decides how much the rest of this has to carry:

- A stolen Google account yields an opaque blob. Not a redacted ledger — an unreadable one.
- Google cannot read the user's transactions. Neither can we.
- A compromised access token cannot be escalated into data disclosure.
- If the user forgets the passphrase, the backup is gone. This is a real cost and it must be said in
  the UI at the moment they set it, not buried in a help page. The existing backup sheet already
  says the app cannot recover a lost password; the Drive flow reuses that wording rather than
  softening it.

So authentication protects *access to the file*, not *access to the contents*. That is a much weaker
thing to have to get right, and it is why this design does not need a server.

## Authentication

**Firebase Authentication with the Google provider, obtained through Credential Manager.**

`GoogleSignInClient` — the API most tutorials still show — is deprecated. The current route is
`androidx.credentials` + Google Identity Services, which returns a Google ID token; that token is
exchanged for a Firebase credential via `GoogleAuthProvider.getCredential(idToken, null)` and signed
in with `FirebaseAuth.signInWithCredential`. Firebase then owns session persistence: the SDK keeps
the refresh token in its own private storage, refreshes the ID token roughly hourly, and survives
process death and reboot without the app storing a password anywhere.

What the app must *not* do, each for a specific reason:

- **No client secret in the APK.** Android OAuth clients do not have one; anything that looks like a
  secret in a shipped APK is not a secret. If a flow appears to need one, the flow is wrong.
- **No hand-rolled token cache.** The Firebase SDK's own storage is the token store. A second copy
  is a second thing to leak and a second thing to get out of sync.
- **No long-lived Drive access token at rest.** `GoogleAuthorizationCodeFlow`-style server-side
  refresh is not used; the Drive access token is requested per session and held in memory only.
- **No silent sign-in on first launch.** The app is local-first (D-006) and stays fully usable
  signed out. Sign-in is offered where the value is — the backup sheet — not at the door.

Session handling:

- Signed-out is the normal state, not an error state. Every Drive call path starts by asking whether
  there is a current user and degrades to the existing local file-picker backup if not.
- Token refresh failure (revoked access, password change, account deleted) surfaces as "sign in
  again", never as a crash and never as a silent no-op that leaves the user believing a backup ran.
- Sign-out clears the Firebase session and drops the in-memory Drive token. It does not touch local
  data — the ledger is the user's and lives on the device.

## Drive authorization

**Scope: `drive.appdata` only.**

`appDataFolder` is a per-application hidden folder. With this scope the app cannot see, list, or
touch anything else in the user's Drive — not their documents, not their photos, not backups from
other apps. It is also invisible in the Drive UI, which is a real trade-off: the user cannot go and
find the file themselves, so the app must be the way to list and restore it, and the export-to-file
path stays available for anyone who wants a copy they control.

`drive.file` is the alternative and it is worse here: it grants access to files the user picks
through Google's own picker, which drags in a second UI and a broader grant for no benefit, since
the app is storing its own opaque blob, not collaborating on the user's documents.

## Reliability

The failure modes that matter for a backup are not exotic, and each has one obvious answer:

- **A phone loses signal mid-upload.** Resumable upload for anything over a few megabytes; a
  partial upload is never promoted to "latest".
- **The same backup is uploaded twice.** The file's `appProperties` carry the envelope's SHA-256, so
  a re-upload of identical bytes is recognised and skipped rather than creating a duplicate.
- **Two devices back up the same account.** Keep the last N (5) by creation time and never merge.
  Backups are snapshots, not a sync protocol — merging two divergent ledgers automatically is how a
  double-entry system silently stops balancing. D-006 and the audit trail rule both point the same
  way here.
- **Restore.** Download, then run the existing `prepareRestore` preview — schema version, table and
  row counts, and the warning — before anything is written. The Drive path reuses that screen
  exactly; it does not get a shortcut that the local path does not have.
- **Nothing runs in the background without being asked.** No `WorkManager` job that quietly uploads
  a ledger on someone's mobile data. Backup is a button.

## What has to exist before any of this can be written

In order, because each blocks the next:

1. **The package identifier** (O-002). Finalized as `com.mankeu`.
2. **A signing certificate** (O-010) and its SHA-1, for the Android OAuth client.
3. **A Firebase project** on an owner-controlled Google account, with the Google provider enabled
   and the OAuth consent screen filled in — including a privacy policy URL, which Google requires
   and which does not exist yet.
4. **`google-services.json`**, added to `app/` and **git-ignored**. It is not a secret in the
   cryptographic sense, but it identifies the owner's project and does not belong in a repository
   that may be shared.

Given those four, the implementation is small: the Credential Manager call, the Firebase exchange,
a Drive REST client for `appDataFolder` (upload, list, download), and the UI additions to the backup
sheet. The test surface is smaller still, because the crypto is already tested and unchanged.

## What this does not do

Not multi-device sync. Not a shared account. Not conflict resolution on ledger rows. Those were
weighed in O-006 and remain out: a snapshot backup that restores exactly what it captured is a
promise the app can keep, and a sync protocol over double-entry books is a promise it cannot keep
without a much larger design and a server to arbitrate it.
