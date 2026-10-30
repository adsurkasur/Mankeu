---
status: implementation-baseline
last_verified: 2026-10-22
audience: human-and-ai
sensitivity: internal
evidence_basis:
  - logs/DECISION_LOG.md (DEC-011 to DEC-019 narrowed the 2026-08-15 scope)
  - docs/DOMAIN_MODEL.md
  - docs/QUALITY_GATES.md
  - app/schemas/com.mankeu.data.AppDatabase/11.json
---

# V1 Implementation Specification

The original 2026-08-15 version of this spec (credit sales, receivables and payables, loans and interest, reversal, a generic workbook importer and an eleven-sheet export) is in Git history. Owner decisions DEC-011 to DEC-024 replaced or extended those parts; this version states V1 as it stands on Room schema 12.

## Release outcome

V1 is an offline native Android cash book and business planner for small businesses. A first-time non-accountant can create a business and a unit, plan the unit (products, costs, capital), record cash, correct or delete a record, track stock, read reports, move data in and out with the Mankeu Excel template, and create or restore an encrypted backup, all without an account.

## Primary task flows

1. Start privately on-device; create a business, then a unit (branch, field, season). Nothing is created on the owner's behalf.
2. Switch explicitly between `Rencana` and `Realisasi` (shown once a unit exists).
3. In Rencana, build the plan in Rencana Usaha: period, products with cost recipes and a first selling month, monthly fixed costs, one-off costs and income, assets with useful life, planned capital.
4. In Realisasi, record Uang Masuk or Uang Keluar. The category decides whether it is income, expense, owner capital, owner drawing, or an asset purchase.
5. Correct a record with Ubah or remove it with Hapus.
6. Keep stock per unit or shared: stock in, stock out, stock count, low-stock warning.
7. Read profit and loss, cash flow, feasibility and break-even, and plan versus actual.
8. Export the template-format workbook or a PDF; import a filled template into the selected unit and undo it if needed.
9. Create a password-encrypted backup to a file or Google Drive and restore it after a preview.
10. Find help: a welcome card on first launch, page tours behind the ? button, and the Pusat Bantuan.
11. Delete all data from Pengaturan, including the Mankeu files on Google Drive and the sign-in account (DEC-020).

## Native information architecture

### Compact phone

- **Beranda**: cash, the four key metrics, monthly cash flow, expense distribution, stock, plan feasibility, this month's break-even status.
- **Catatan**: the entry form and the record list (search, Semua/Masuk/Keluar) in Realisasi; Rencana Usaha in Rencana.
- **Stok**: items of the selected unit and shared items.
- **Laporan**: Laba Rugi dan Arus Kas, Kelayakan & BEP, Uang vs rencana.
- **Overlays** from the top bar: Berkas & Data (import, export, backup, categories, import history), Pengaturan, Pusat Bantuan.

A record speed dial (Uang Masuk, Uang Keluar) is available on Beranda and Catatan.

### Expanded tablet

- A navigation rail replaces the bottom bar from 840dp.
- Keyboard focus order and visible focus are part of acceptance.

## Canonical domain records

See `docs/DOMAIN_MODEL.md` for fields and invariants, and `docs/diagrams/01_entity_relationship_diagram.md` for the schema.

- Scope: `Business`, `Project` (unit), exactly one `PROJECTION` and one `REALIZATION` book per unit.
- Planning: `Category` (role and optional cost nature), `BudgetItem` (plan line), `Product` with `ProductUnit` (sale units), `ProductRecipeLine` (per-batch recipe) and `ProductMovement` (production and counts), DEC-028.
- Accounting: `FinancialEvent` (five cash types) with one balanced `JournalEntry` and its `Posting` rows. A record is corrected in place or deleted (DEC-019).
- Stock: `StockItem` (business, optional unit) and `StockMovement`.
- Import: `ImportBatch`, `ImportNotice`; `ProvenanceRecord` is kept for history.

## Accounting policy

| User action | Debit | Credit | Cash flow role | Profit effect |
| --- | --- | --- | --- | --- |
| Cash income | Cash | Revenue | operating | revenue now |
| Cash expense | Operating expense | Cash | operating | expense now |
| Owner capital | Cash | Owner equity | financing | none |
| Owner drawing | Owner drawing | Cash | financing | none |
| Buy asset for cash | Equipment asset | Cash | investing | depreciation in the plan's break-even |

There are no credit, receivable, payable, loan, or interest events (DEC-011).

## Report contract

- **Beranda:** current cash, operating cash flow, profit, operating income and expense, expense distribution, stock summary.
- **Laba Rugi dan Arus Kas:** revenue and expenses from postings; cash by operating, investing, and financing role and month.
- **Kelayakan & BEP:** monthly breakdown (fixed costs, one-off costs averaged, depreciation, variable cost, revenue, profit, R/C, B/C), break-even by mix and per product, capital need (DEC-014, DEC-016, DEC-017).
- **Uang vs rencana** (was "Realisasi vs Target"): plan and actual money in/out averages per month, cash basis; never called profit (U2).

All amounts are derived at read time. No report total is a writable source of truth.

## Excel contract (DEC-012)

- One format for the blank template and the export: sheets Petunjuk, Transaksi, Rencana Anggaran, Produk, Satuan Jual, Resep Produk, Produksi & Stok, Stok Barang, Kategori, with the `MANKEU_TEMPLATE` marker (version 5, DEC-028). Every column is required with the exact header; files of an older version are rejected with the list of differing headers.
- Import reads only that template. Any other file is rejected with the exact missing or renamed sheets and columns and a way to download the template.
- Import is bulk entry into the selected unit: preview with new categories, conflicts, and a duplicate warning, then one atomic commit with a notice.
- Batalkan impor deletes the batch's records (edited ones included), archives its plan lines, removes its products and stock, and clears the plan period it set if unchanged. The batch row stays as history.
- Round trip: an export imports back without loss (`TemplateImportTest` for the workbook; `ImportCommitTest.productionAndStockCountsSurviveExportAndImportIntoANewUnit` for product stock in the database). Produksi & Stok rows go only with a product the import adds; on one date they count after that day's sales (DEC-028 §9).

## Backup contract

- The backup is a versioned `PortableBackupCodec` snapshot of every table in `AppDatabase.DATA_TABLES`, encrypted with AES-256-GCM under a PBKDF2-HMAC-SHA256 key (310,000 iterations) from a password of at least 8 characters.
- Restore accepts schemas 3 to 11, previews counts, keeps an encrypted recovery copy, replaces all data in one transaction, and upgrades older snapshots with the same cleanups as the migrations.
- A forgotten backup password cannot be bypassed; the UI says so before creation.

## Security and privacy

- No account or network is required and no financial content leaves the device implicitly.
- Android auto-backup is off (`allowBackup="false"`).
- Logs never contain descriptions, amounts, workbook contents, passwords, or backup plaintext.
- Imported files are read through the system file picker and are not copied.
- Firebase Auth and Drive are optional and only carry encrypted backups (D-033, D-040). No analytics.
- **Hapus semua data** (DEC-020) empties every table, the sync outbox and state, the recovery copy and the local name; when signed in it also deletes every file in the Drive app folder, the Firebase user, and the Google grant. Drive is deleted first, so a failure leaves the phone untouched.

## Definition of v1 complete

V1 is complete only when every flow above is reachable in the UI, survives process restart, preserves accounting and import invariants, passes the quality gates, and is evidenced on stable phone and tablet devices. A compiled screen or isolated calculation is not enough.
