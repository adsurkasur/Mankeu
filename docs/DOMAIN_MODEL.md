---
status: implemented-schema-11
last_verified: 2026-10-22
audience: human-and-ai
sensitivity: internal
evidence_basis:
  - owner-confirmed multi-business and multi-project direction
  - ../arina-agri/AUDIT_REVISI_MANAJEMEN_KEUANGAN.md
  - app/schemas/com.mankeu.data.AppDatabase/11.json
  - logs/DECISION_LOG.md (DEC-011 to DEC-019)
---

# Domain Model

## Design objective

Give non-accountants a fast `Uang Masuk` and `Uang Keluar` experience while keeping an exact, balanced, and auditable financial model underneath. The domain must work without agriculture-specific fields; agriculture is an optional template.

## Aggregate hierarchy

```text
Local Profile
└── Business (one or many)
    ├── Category (role: REVENUE, EXPENSE, CAPITAL_IN, DRAWING, ASSET)
    ├── Stock item shared by every unit
    └── Unit / project (zero or many)
        ├── Projection book  -> plan lines (budget_items)
        ├── Realization book -> cash events -> journal -> postings
        ├── Products -> cost components (recipe)
        └── Stock items held by this unit
```

- A **Business** is the accounting and reporting boundary selected by the user.
- A **Unit** (`ProjectEntity`, shown as "Unit Usaha") is an operational boundary: branch, field, season, machinery-service job, or another unit of work, with an optional plan period.
- A **Book** is either `PROJECTION` or `REALIZATION`. Realization holds recorded cash events; Projection holds the business plan (plan lines), and the unit's products feed both the plan and realization break-even.

### How this maps onto the web prototype (owner question, 2026-08-28)

The web app has **one** level, not two. `arina-agri` stores a `FinanceProject` — name, commodity,
land area and unit, season label, start and end dates, status — and every dataset, RAB and report
hangs directly off it. There is no business entity above it.

So: web **Proyek** ≈ Android **Unit Usaha** (`ProjectEntity`). Android's **Usaha**
(`BusinessEntity`) is an extra level the web does not have, added under D-005 because one owner can
run several separate books — a rice farm and a machinery-rental service should not share a profit
and loss statement. A single-farm user can create one Usaha and forget it exists; the level is
there for the user who needs it, not as a step everyone must climb.

Note that `isGeneral` on `ProjectEntity` no longer marks an auto-created `Umum` workspace — nothing
is created on the user's behalf since D-015.

## Core entities

| Entity | Essential fields | Invariants |
| --- | --- | --- |
| `Business` | id, name, base currency, status, created/updated time | Name is user-visible; archived businesses remain recoverable. |
| `Project` | id, business id, name, optional type/template, start/end period, status | Belongs to one business; template fields are optional. |
| `Book` | id, project id, mode, timestamps | Unique project + mode pair; exactly one projection and one realization book. |
| `Account` | compile-time account role | `LedgerAccount`: CASH, REVENUE, OPERATING_EXPENSE, OWNER_EQUITY, OWNER_DRAWING, EQUIPMENT_ASSET; user labels live in categories. |
| `Category` | id, business id, name, role, optional cost nature | The role decides the cash type of a record (DEC-011 K-2); cost nature FIXED/VARIABLE feeds break-even. |
| `BudgetItem` (plan line) | Projection book id, category, amount, recurrence MONTHLY/ONCE, month, cost nature, useful life | Fixed costs, one-off costs and income, assets, planned capital (DEC-014). |
| `Product` | unit id, name, unit, price, target per month, first selling month | Contribution margin = price minus recipe cost per unit (DEC-014, DEC-017). |
| `ProductCostComponent` | product id, name, amount, yield | Cost per unit = amount / yield. |
| `StockItem` / `StockMovement` | business, optional unit, levels; IN/OUT/ADJUSTMENT with quantity | Stock out never exceeds stock on hand; movement and level change in one transaction. |
| `FinancialEvent` | business/unit/book, type (CASH_INCOME, CASH_EXPENSE, OWNER_CAPITAL, OWNER_DRAWING, ASSET_PURCHASE_CASH), date, amount, description, category, optional reference/quantity/budget/product/import links | User-facing record; must produce a balanced journal. Corrected in place or deleted (DEC-019). |
| `JournalEntry` | source event id (unique), date, description | Total postings must balance before writing. |
| `Posting` | journal id, account id, side, amount | Integer rupiah; debit total equals credit total. |
| `ImportBatch` | source hash, importer version, state COMMITTED/REVERSED (shown as dibatalkan), counts, applied plan period | Commit is atomic; undo is batch-addressable. |
| `ProvenanceRecord` | target record/field, source locator, rationale | Kept for history; the template importer writes none (DEC-012). |
| `ImportNotice` | severity, title, explanation, batch (null for SYSTEM notices) | Every import and every data cleanup leaves one. |

## Implemented persistence (schema 11)

Room schema 11 implements businesses, units, books, categories, plan lines, cash events, journals, postings, products and cost components, stock items and movements, import batches/notices/provenance, and the sync outbox. Repository transaction boundaries enforce:

- scope consistency across business, unit, book, and category;
- exact positive rupiah values;
- one projection and one realization book per unit;
- balanced posting insertion in the same database transaction as the source event;
- correction by editing in place (same id, rebuilt journal) or deleting (DEC-019);
- stock movement and stock level written together;
- report derivation from joined postings and cash-flow roles.

The schema history is in `docs/DATABASE_MIGRATIONS.md`.

### Correction rules (edit and delete, DEC-019)

Every rule lives in `FinanceRepository`, inside the same transaction as the write, and is covered by `LedgerIntegrityTest` (instrumented). "Balik Transaksi" (an inverse journal kept beside the original) was removed in schema 11: owners fix a record by editing it.

| Action | Allowed when | Effect |
| --- | --- | --- |
| Record an event | amount > 0; scope, book and category agree | Event + balanced journal |
| Edit an event (Ubah) | the same checks as recording | Same id, import batch and creation time; journal and postings rebuilt from the corrected values |
| Delete an event (Hapus) | always | Event, journal and postings removed by cascade |
| Erase all data (Hapus semua data) | the owner typed HAPUS | Every data table emptied in one transaction with change capture off; sync outbox and state cleared, new device id (DEC-020) |
| Undo an import batch | the batch is `COMMITTED` | Its events deleted (edited ones included), plan lines archived, products and stock it added removed, the plan period it set cleared if unchanged; the batch row stays, marked undone, and the same file may be imported again (D-076) |

## Money and quantity

- Stored money uses signed 64-bit integer rupiah (`Long` in Kotlin). Never store rupiah in `Float` or `Double`.
- Percentages, production quantities, and physical units use decimal types with an explicit scale and rounding policy.
- A rounding remainder is visible and attributable; it is never silently discarded to force reconciliation.
- Currency conversion is out of scope for the first implementation. Each business has one base currency, initially IDR.

## User entry and hidden balancing

The user normally chooses a familiar event, not debit and credit. The domain translates that event into balanced postings.

| User event | Simplified internal meaning |
| --- | --- |
| Cash sale / income received | Cash increases; revenue increases. |
| Expense paid | Expense increases; cash decreases. |
| Owner capital deposited | Cash increases; owner equity increases. |
| Owner drawing (prive) | Owner drawing increases; cash decreases. |
| Asset bought with cash | Equipment asset increases; cash decreases. |

The form shows only Uang Masuk and Uang Keluar; the chosen category's role picks the row (`CategoryRole.cashTypeFor`). There is no credit, loan, or interest event (DEC-011).

These mappings live in `LedgerPolicy.journalFor` and are covered by `LedgerPolicyTest`. A contributor may not “fix” a report by adding an unrelated synthetic transaction outside this event model.

## Projection, realization, and comparison

- Projection is the business plan: plan period, products with recipes and a first selling month, plan lines, planned capital. `PlanCalculator` derives the monthly averages, break-even (mix and per product), R/C, B/C, and capital need (deepest cumulative deficit before capital), and a cash schedule over the real months (DEC-014, DEC-016, DEC-017).
- Realization is the recorded cash. `ActualBepCalculator` compares this month's revenue with its break-even.
- Comparison (Realisasi vs Target) reads both and compares averages per month on a cash basis. It does not own editable financial facts.

## Financing semantics

- Owner capital (Setoran Modal) and owner drawing (Ambil Pribadi) are financing cash flow, never operating revenue or expense.
- An asset purchase is investing cash flow; it enters break-even as straight-line depreciation over its useful life.
- There is no borrowing or interest in the model (DEC-011). An owner who records a loan as plain money in under a custom category is left alone.

## Derived reports

Reports are deterministic projections of canonical records:

- profit/loss derives from revenue and expense postings;
- cash flow derives from cash-account postings grouped by period;
- cash flow separates operating, investing, and financing movements;
- break-even, R/C, B/C, and capital need are shown only when the plan has the products, targets, and costs they need.

Every displayed total must allow drill-down to contributing records. A mismatch is a first-class reconciliation result, not a formatting issue.

## Agriculture as an optional template

An agriculture template may supply crop, land area, season, production unit, common cost categories, and workbook aliases. A service business may instead supply equipment, service unit, and job metadata. Neither template changes the universal core entities or makes its fields mandatory elsewhere.
