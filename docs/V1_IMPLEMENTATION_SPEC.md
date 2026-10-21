---
status: implementation-baseline
last_verified: 2026-08-15
audience: human-and-ai
sensitivity: internal
evidence_basis:
  - owner-confirmed scope and accounting decisions dated 2026-08-15
  - docs/WEB_FINANCE_CAPABILITY_MATRIX.md
  - docs/DOMAIN_MODEL.md
  - docs/QUALITY_GATES.md
---

# V1 Implementation Specification

## Release outcome

V1 is a complete, offline-capable native Android financial manager for small businesses and project-based work. It must let a first-time non-accountant create a business, optionally create a project, plan, record cash or credit activity, settle obligations, understand reports, import/export Excel, and create/restore an encrypted backup without an account.

## Primary task flows

1. Start privately on-device and create or rename a business.
2. Work in `Umum` or create a project/job with optional dates and template metadata.
3. Switch explicitly between `Proyeksi` and `Realisasi`.
4. Create budget categories/items or import them from Excel.
5. Record money in/out, credit sales/purchases, owner capital/drawings, loans, principal, and interest.
6. Review and partially/fully settle receivables and payables.
7. Link realization records to budget items and resolve unmatched/ambiguous suggestions.
8. Read profit/loss, cash flow, financing, working-capital, AR/AP, feasibility, and variance reports with drill-down.
9. Export a workbook whose source sheets can be re-imported without changing canonical meaning.
10. Create a password-encrypted backup and safely restore it after validation and preview.

## Native information architecture

### Compact phone

- **Beranda** — current scope, cash, accrual profit, due items, budget variance, recent activity/notices.
- **Catat** — one-action entry with task-language choices and recent templates.
- **Anggaran** — projection/realization budget items and comparison.
- **Laporan** — profit/loss, cash flow, financing, AR/AP, feasibility, and variance.
- **Lainnya** — business/project, master data, import/export, backup/restore, activity/audit, help.

A persistent quick-entry action is available from Beranda, Anggaran, and Laporan.

### Expanded tablet

- Navigation rail replaces bottom navigation.
- List-detail panes are used for ledger/detail, budget/detail, obligations/detail, and notices/provenance.
- Keyboard focus order and visible focus are part of acceptance.

## Canonical domain records

### Scope

- `Business`: local owner scope, name, currency, archive state.
- `Project`: belongs to one business; universal name/description/dates/status; `Umum` is the protected default project.
- `Book`: exactly one `PROJECTION` and one `REALIZATION` per project.

### Master data and planning

- `Category`: stable ID, direction/account role, name, aliases, unit default, active/archive state, optional template origin.
- `Unit`: business-owned name, symbol, active/archive state.
- `BudgetItem`: book, category, name, decimal quantity text, unit, integer unit price, integer planned total, planned date/month, aliases, sort order, archive state.
- Planned total is either `quantity × unit price` under an explicit rounding policy or a user-entered authoritative amount accompanied by a visible difference warning.

### Accounting event and journal

- `FinancialEvent`: immutable posted business fact with book/scope, kind, date, description, category, counterparty/reference, quantity/unit/unit price, amount, cash state, import/provenance link, and reversal link.
- `JournalEntry` and `Posting`: balanced debit/credit effects derived by domain policy.
- Core accounts: cash, accounts receivable, accounts payable, revenue, operating expense, interest expense, owner equity, owner drawing, loan payable, and generic asset/equipment.
- A posted record is corrected by editing it in place or deleting it (DEC-019 superseded reversal plus replacement).

### Obligations

- `Obligation`: receivable/payable, counterparty, issue/due dates, original amount, description/category, source event, status.
- `Settlement`: links an obligation to a cash event and amount; supports partial settlement.
- Outstanding amount is derived from original amount minus non-reversed settlements.

### Import and audit

- `ImportBatch`: source name/hash, importer version, mapping version, status, timestamps, target book, totals.
- `ImportCandidate`: staging-only classification and proposed canonical mutation.
- `Provenance`: source workbook/sheet/cell/row evidence, source value, target record/field, rule/version, rationale, confidence.
- `Notice`: severity, human explanation, amount, review state, action, batch.
- ~~`SavedMapping`~~: planned reusable sheet/header/field mapping. Never implemented beyond an empty table and dropped in D-026; reinstate it with the unfamiliar-workbook mapping UX that needs it, not before.

## Accounting policy

| User action | Debit | Credit | Cash effect | Profit effect |
| --- | --- | --- | --- | --- |
| Cash sale/income | Cash | Revenue | increase | revenue now |
| Credit sale | Accounts receivable | Revenue | none | revenue now |
| Receive receivable | Cash | Accounts receivable | increase | none |
| Cash operating purchase/expense | Operating expense | Cash | decrease | expense now |
| Credit operating purchase | Operating expense | Accounts payable | none | expense now |
| Pay payable | Accounts payable | Cash | decrease | none |
| Owner capital | Cash | Owner equity | increase | none |
| Owner drawing | Owner drawing | Cash | decrease | none |
| Receive loan | Cash | Loan payable | increase | none |
| Repay loan principal | Loan payable | Cash | decrease | none |
| Pay loan interest | Interest expense | Cash | decrease | expense now |
| Buy asset for cash | Generic asset/equipment | Cash | decrease | none at purchase |

Depreciation and inventory/COGS subledgers are outside v1. The UI must make the expense-vs-asset choice understandable.

## Report contract

- **Dashboard:** current cash, accrual profit/loss, cash in/out, total outstanding receivable/payable, overdue amount, budget variance, notices.
- **Income statement:** revenue and expenses from postings; financing principal/equity excluded; interest included.
- **Cash flow:** cash postings by operating/investing/financing role and month, with cumulative balance and zero months.
- **Financing:** opening cash, owner capital, loan receipts, principal, interest, other financing cost, working-capital need, ending cash.
- **AR/AP:** open/partial/paid/overdue totals and detail.
- **Feasibility:** output volume/price assumptions, revenue estimate, HPP, production BEP, B/C ratio, status; optional for projects where it makes sense.
- **Comparison:** projection vs realization for headline metrics, categories/items, monthly cash, and unmatched records.

All report amounts are derived at read time from canonical records/postings. No report total is a writable source of truth.

## Excel contract

### Import stages

1. fingerprint and safety limits;
2. inspect workbook and detect candidate roles/headers;
3. apply known/saved mapping or request mapping;
4. parse all relevant sheets into staging candidates;
5. normalize and match categories/budget links;
6. reconcile workbook-wide totals;
7. show direct, normalized, deterministic enrichment, ambiguous, conflict, and unsupported groups;
8. block unresolved conflicts and decisions;
9. atomic commit with provenance/notices;
10. allow batch reversal.

The golden Padi 1 Ha workbook is a required regression baseline. It is not a hard-coded filename or exclusive schema.

### Export sheets

- `Petunjuk & Metadata`
- `Anggaran - Proyeksi`
- `Anggaran - Realisasi`
- `Transaksi - Proyeksi`
- `Transaksi - Realisasi`
- `Piutang & Utang`
- `Laba Rugi`
- `Arus Kas`
- `Pembiayaan`
- `Perbandingan`
- `Jejak Impor`

Derived sheets are explanatory; source sheets and metadata reconstruct canonical meaning. A semantic round-trip test compares records and totals, not XML bytes or styling.

## Backup contract

- The portable backup contains a versioned manifest and canonical local records, not cached UI/report state.
- User selects a password at creation; use a salted password-derived key and authenticated encryption.
- The encrypted envelope records algorithm, KDF parameters, schema/app version, creation time, payload checksum, and business count without exposing financial detail.
- Restore decrypts, validates checksum/schema/references/invariants, and previews counts before mutation.
- Restore is atomic. The previous database is retained as a recovery copy where practical.
- A forgotten backup password cannot be bypassed; the UI must say this before creation.

## Security and privacy

- No account/network is required and no financial content leaves the device implicitly.
- Android auto-backup is disabled for the finance database until an explicit encrypted strategy exists.
- Logs never contain descriptions, counterparties, amounts, workbook contents, passwords, or backup plaintext.
- Imported files are read through the system file picker and are not copied permanently unless the user exports/backups explicitly.
- Network AI, analytics, Firebase, Drive, signing material, and production IDs remain excluded without an approved decision.

## Delivery sequence

1. Replace schema and domain policy with full book/event/posting/obligation/audit model.
2. Implement repositories and tested reports.
3. Replace the MVP UI with the Arina design system and primary task flows.
4. Complete budget, ledger, AR/AP, financing, comparison, and reversal.
5. Complete generic import, export, and encrypted backup/restore.
6. Add unit, Room, importer/exporter/backup, Compose, and device tests.
7. Validate fresh API 30 phone/tablet AVDs, then modern API compatibility.
8. Update all handoff docs, commit/push calculator, and update the parent repository pointer.

## Definition of v1 complete

V1 is complete only when every owner-confirmed flow above is reachable in the UI, survives process restart, preserves accounting/import invariants, passes the quality gates, and is evidenced on stable phone/tablet devices. A compiled screen or isolated calculation is not enough.
