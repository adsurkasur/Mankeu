---
status: baseline
last_verified: 2026-10-22
audience: human-and-ai
sensitivity: internal
evidence_basis:
  - owner-confirmed decisions dated 2026-08-15
  - docs/PROJECT_GUIDE.md
  - logs/DECISION_LOG.md (DEC-011 to DEC-019 supersede the original receivables, loans, enrichment and reversal requirements)
---

# Product Requirements

## Product shape

Mankeu is a standalone Android application with a universal financial core and optional business templates. It is not limited to cultivation and operates 100% offline-first without external dependencies.

## Functional requirements

### Businesses and projects

- **FR-BIZ-001:** A user can create, rename, archive, restore, and delete multiple businesses.
- **FR-BIZ-002:** A business can contain multiple units (branches, fields, seasons, or activities), each with its own Rencana (plan, formerly "Proyeksi") and Realisasi book. A new business starts with one unit, "Utama", so a single-location owner can record at once (audit 2026-10-22).
- **FR-BIZ-003:** Every record belongs to an explicit unit the owner created; the app never invents a hidden default unit.
- **FR-BIZ-004:** Agriculture-specific attributes are optional template metadata, not required business fields.

### Simple financial entry

- **FR-TXN-001:** The primary entry flow supports money in and money out with date, amount, description, and category, into the selected unit. The category role decides the cash type: income, expense, owner capital, owner drawing, or asset purchase (DEC-011 K-2).
- **FR-TXN-002:** Quantity, unit, unit price, product, reference, cost nature, and useful life (assets) are optional.
- **FR-TXN-003:** The UI does not expose debit and credit as required user input, while the internal model preserves balanced financial meaning.
- **FR-TXN-004:** A record can be corrected in place (Ubah: same id, journal rebuilt) or deleted (Hapus), and every derived report updates deterministically (DEC-019).
- **FR-TXN-005:** Strict cash: owner capital, owner drawing, asset purchase, revenue, and expense remain semantically distinct; there are no receivables, payables, loans, instalments, or interest (DEC-011).

### Budget, projection, and realization

- **FR-PLAN-001:** Each project can contain a complete projection and a complete realization dataset.
- **FR-PLAN-002:** The projection is a business plan (DEC-014, DEC-017): plan period, products with per-unit cost recipes and a first selling month, monthly fixed costs, one-off costs and income, assets with useful life, and planned capital. Realization holds the recorded cash.
- **FR-PLAN-004 (Fase 1, DEC-028):** A product has a base unit, sale units with their own prices, and a per-batch recipe of materials and free costs. The app shows the HPP and profit of every sale unit without manual calculation, and the plan uses the main (first) sale unit.
- **FR-PLAN-005 (Fase 1, DEC-028):** Product stock is automatic: Produksi adds, sales subtract, and a stock count (Hitung stok) corrects it. Materials are kept apart and are not reduced by production.
- **FR-PLAN-006 (Fase 1, DEC-028):** A material's price follows its latest purchase. When a purchase changes it, the owner sees which products' HPP and profit change; deleting the purchase restores the price.
- **FR-PLAN-003:** Comparison reads final projection and realization outputs (averages per month, cash basis) and never acts as a transaction source.

### Reports

- **FR-RPT-001:** Provide income and expense summary, profit/loss, cash flow, cumulative cash, financing cash flow, and budget-versus-actual variance.
- **FR-RPT-002:** Show break-even (mix and per product), R/C, B/C, and capital need only when the plan has the products, targets, and costs they need.
- **FR-RPT-003:** Every report offers a path back to its contributing records.
- **FR-RPT-004:** An unexplained reconciliation difference is visible and cannot be labeled complete.
- **FR-RPT-005:** Reports are read-only outputs; corrections happen at the originating record or through an explicit adjustment.

### Stock

- **FR-STK-001:** Stock items belong to one unit or are shared by every unit of the business (DEC-017).
- **FR-STK-002:** Stock in, stock out, and stock count (opname) are recorded atomically; stock out cannot exceed what is on hand, and low stock is flagged.

### Import, export, and backup

- **FR-IMP-001:** Import reads only the Mankeu template (DEC-012); any other file is rejected with the exact missing or renamed sheets and columns and a way to download the template. Nothing is guessed or enriched.
- **FR-IMP-002:** Import is bulk entry into the selected unit: preview (new categories, conflicts, duplicate warning), then one atomic commit with a visible notice.
- **FR-IMP-003:** Undoing an import (Batalkan impor) deletes its records, archives its plan lines, removes its products and stock, and keeps the batch as history (DEC-019).
- **FR-IMP-004:** Re-importing an identical file is allowed after a clear duplicate warning.
- **FR-EXP-001:** The Excel export is the template format (lossless round trip); the PDF export carries the income statement, cash flow, and feasibility.
- **FR-BKP-001:** Local backup and restore work without an account.
- **FR-BKP-002:** Backups are versioned, integrity-checked, and encrypted before public release.
- **FR-SYNC-001:** Google Drive backup and Firebase authentication are optional, never core dependencies. Multi-device sync (DEC-013, DEC-024) is offered in Pengaturan: same Google account on every phone, end-to-end encrypted with a sync passphrase, automatic with a manual button.

### Notifications and recovery

- **FR-NOT-001:** Notifications distinguish information, automatic recovery, action required, conflict, and failure.
- **FR-NOT-002:** A notice says what an import or a data cleanup changed, how many records and how much money, and where it came from.
- **FR-NOT-003:** Offline, interrupted import, failed restore, and storage errors provide a concrete recovery path.

## Non-functional requirements

- **NFR-001 Exactness:** Stored money uses integer rupiah and deterministic rounding policies.
- **NFR-002 Offline:** Core entry, reports, import review, and backup creation remain usable without network access.
- **NFR-003 Performance:** Common entry and report interactions remain responsive on representative lower-middle Android hardware; exact device baseline is pending field confirmation.
- **NFR-004 Adaptivity:** Core tasks work at compact, medium, and expanded widths without mandatory horizontal scrolling.
- **NFR-005 Accessibility:** Touch targets, contrast, labels, text scaling, keyboard navigation on tablets, and reduced motion are tested.
- **NFR-006 Privacy:** Financial data is private by default; no analytics or cloud copy is enabled silently.
- **NFR-007 Auditability:** Every derived number and import mutation has traceable inputs.
- **NFR-008 Recoverability:** Destructive actions, migrations, imports, and restores have explicit rollback or recovery behavior.
- **NFR-009 Testability:** Domain behavior can be tested without Android UI or network services.

## Out of scope for the first code milestone

- iOS implementation;
- full inventory valuation and a cost-of-goods engine (recipes do not consume stock yet);
- tax filing or regulated accounting compliance claims;
- payroll;
- marketplace, payments, or banking integration;
- multi-user collaboration and server-authoritative sync;
- AI financial advice;
- final Play Store branding and monetization.

These boundaries may change through an owner-approved decision record; they must not expand silently during implementation.
