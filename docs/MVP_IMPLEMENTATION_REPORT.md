---
status: historical
last_verified: 2026-08-15
audience: human-and-ai
sensitivity: internal
---

# MVP Implementation Report

> **Dokumen riwayat.** Laporan MVP 2026-08-15. Keadaan sekarang (Room schema 11): `README.md`, `docs/DOMAIN_MODEL.md`, `docs/V1_IMPLEMENTATION_SPEC.md`, `docs/diagrams/`, dan keputusan di `logs/DECISION_LOG.md`.

## Claim

An offline native Android vertical slice is implemented. It is suitable for engineering continuation and debug installation, not public release or user-validation claims.

## Business evidence

- Owner-confirmed need for a universal phone/tablet financial tool, not a cultivation-only port.
- Owner-confirmed multi-business, multi-project, and local-first behavior.
- Verified five-view facilitator workbook and correction contract.
- Validated sibling corpus with 48 synthetic cases across canonical, universal-business, enrichment, ambiguity/conflict, structural, and numerical-edge families.

## Changed behavior

- A first launch creates a local `Usaha Saya` business and explicit `Umum` workspace; users can add and switch businesses/projects.
- Cash income, cash expense, owner capital, and loan receipts persist in Room.
- Each event creates two exact integer-rupiah postings and is rejected unless debit equals credit.
- Reports distinguish operating income/expense from capital, loan principal, and financing cash.
- Compact widths use bottom navigation; expanded widths use a navigation rail.
- `.xlsx` import reads OOXML locally with entry-count, uncompressed-size, path, and external-entity guards.
- Import requires five recognized views and parses the transaction ledger before any database mutation.
- A RAB fact absent from the ledger is auto-recovered only when matching amount/meaning is independently present in profit/loss and cash-flow evidence.
- Accepted import commit, event journals, provenance, and persistent recovery notices share one Room transaction.
- Reimport of the same SHA-256 hash is rejected.

## Tests and exact result

`testDebugUnitTest` passes 7 tests locally:

- every supported event balances at an odd rupiah boundary value;
- zero-rupiah events are rejected;
- capital/loan classification does not inflate profit;
- Indonesian fuzzy label normalizations preserve clear matches and reject an unrelated example;
- verified workbook recognizes five sheets, imports Rp45,500,000 revenue and Rp22,159,000 expense, and creates exactly one Rp7,000,000 land-rent enrichment;
- sanitized enrichment fixture reproduces the same totals and one enrichment.

`lintDebug` and `assembleDebug` pass. The generated debug APK is approximately 19 MB. Dependency-update warnings are retained as visible maintenance signals; no lint error is present.

## Manual/device evidence

The debug APK was installed and exercised on an Android 11/API 30 x86_64 emulator using the relocated `E:\Android\Sdk` and `E:\Android\avd` toolchain:

- cold install and launch succeeded on a compact 480 × 800 px, 240 dpi phone viewport; the verified cold launch was 4.22 s;
- the system-bar overlap found in the first visual pass was fixed with status-bar insets;
- one `Uang masuk` event for `Servis traktor`, category `Jasa`, and exactly Rp1,250,000 was recorded;
- the report showed one canonical record, Rp1,250,000 operating income, Rp0 expense, and no financing-income distortion;
- after force-stop and cold restart, the same one-record/Rp1,250,000 result remained; the restarted cold launch was 0.96 s;
- an expanded 960 × 1600 px viewport at 120 dpi (1,280 dp wide) changed from bottom navigation to the intended navigation rail and retained the event/report totals.

Evidence: [compact home](evidence/mvp-phone-api30-home.png), [compact report](evidence/mvp-phone-api30-report.png), and [expanded layout](evidence/mvp-tablet-api30-home.png).

This is emulator smoke evidence, not proof from a physical phone or tablet. Touch entry, Room persistence across process restart, and the width-class transition are verified; physical-device performance, keyboard behavior, accessibility, and OS-version coverage are not.

## Data migration and rollback impact

- Room schema version: 1.
- No published predecessor exists, so no upgrade migration is required yet.
- Import commits are atomic and duplicate-hash protected.
- Import-batch reversal and user-event edit/delete audit behavior are not implemented; do not use the MVP with irreplaceable real records.

## Privacy and security impact

- Core operation has no network dependency or internet permission.
- Android automatic cloud backup and device-transfer backup are disabled for the database and preferences.
- No Firebase, Drive, analytics, signing secret, credential, or real workbook is bundled in the APK or repository.
- Workbook parsing is local and bounded, but it has not received a dedicated hostile-file security audit.

## Not verified

- process-death behavior during an import commit;
- Room migration, corruption, disk-full, and restore behavior;
- accessibility with TalkBack, 200% font, switch access, or tablet keyboard;
- physical phone/tablet behavior and Android versions other than API 30;
- lower-middle device performance and large-workbook benchmarks;
- ambiguity/conflict resolution UI across the whole 48-fixture corpus;
- exported workbook round trip;
- user comprehension, SUS, or Rogers Diffusion of Innovation outcomes.

## Known limitations

- Budget/projection/realization and comparison are documented but not implemented.
- Receivables, payables, partial settlement, inventory, and invoices are not implemented.
- Import preview supports the five-view family but does not yet expose manual sheet mapping, alternatives, or reversible batches.
- Export, encrypted backup/restore, and optional Drive/auth experiments are absent.
- The current matcher is deterministic/fuzzy; it is not an embedding model and may not authorize an automatic mutation without independent exact evidence.
- The package identity, final name, minimum-device matrix, signing owner, and release process remain open decisions.
