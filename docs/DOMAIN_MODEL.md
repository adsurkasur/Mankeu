---
status: v2-foundation-implemented
last_verified: 2026-08-28
audience: human-and-ai
sensitivity: internal
evidence_basis:
  - owner-confirmed multi-business and multi-project direction
  - ../arina-agri/AUDIT_REVISI_MANAJEMEN_KEUANGAN.md
  - docs/REFERENCE_WORKBOOK_CONTRACT.md
---

# Domain Model

## Design objective

Give non-accountants a fast `Uang Masuk` and `Uang Keluar` experience while keeping an exact, balanced, and auditable financial model underneath. The domain must work without agriculture-specific fields; agriculture is an optional template.

## Aggregate hierarchy

```text
Local Profile
└── Business (one or many)
    └── Project / job / season (zero or many)
        ├── Projection book
        └── Realization book
```

- A **Business** is the accounting and reporting boundary selected by the user.
- A **Project** is an optional operational boundary: cultivation season, machinery-service job, rental period, trading activity, or another unit of work.
- A **Book** is either `PROJECTION` or `REALIZATION`. Each book may contain budget items, dated events, reports, cash-flow timing, and financing—not merely one type of row.

### How this maps onto the web prototype (owner question, 2026-08-28)

The web app has **one** level, not two. `arina-agri` stores a `FinanceProject` — name, commodity,
land area and unit, season label, start and end dates, status — and every dataset, RAB and report
hangs directly off it. There is no business entity above it.

So: web **Proyek** ≈ Android **Lahan / musim tanam** (`ProjectEntity`). Android's **Usaha**
(`BusinessEntity`) is an extra level the web does not have, added under D-005 because one owner can
run several separate books — a rice farm and a machinery-rental service should not share a profit
and loss statement. A single-farm user can create one Usaha and forget it exists; the level is
there for the user who needs it, not as a step everyone must climb.

A web project imported into Android therefore lands as a Lahan under whichever Usaha is selected.
Note that `isGeneral` on `ProjectEntity` no longer marks an auto-created `Umum` workspace — nothing
is created on the user's behalf since D-015.

## Core entities

| Entity | Essential fields | Invariants |
| --- | --- | --- |
| `Business` | id, name, base currency, status, created/updated time | Name is user-visible; archived businesses remain recoverable. |
| `Project` | id, business id, name, optional type/template, start/end period, status | Belongs to one business; template fields are optional. |
| `Book` | id, project id, mode, timestamps | Unique project + mode pair; exactly one projection and one realization book. |
| `Account` | compile-time account role | V2 uses a versioned `LedgerAccount` policy enum for cash, receivable, payable, equity, revenue, expense, loan, and asset roles; user labels live in categories. |
| `Category` | id, business id, name, semantic role | User labels may change; semantic role remains stable for reports. |
| `BudgetItem` | book id, category, quantity, unit, unit price, amount, timing | Amount is exact rupiah; timing may be a date or period. |
| `FinancialEvent` | business/project/book, kind, date, amount, description, category, optional reference/quantity/budget/product/import links | User-facing posted record; must produce a balanced journal event. |
| `JournalEntry` | date/period, description, source type/id, status | Total postings must balance before posting. |
| `Posting` | journal id, account id, side, amount | Integer rupiah; debit total equals credit total. |
| `Obligation` | kind, counterparty, issue/due date, original amount, source event, reversed state | Kind distinguishes receivable/payable; remaining/status are derived from non-reversed settlements. |
| `Settlement` | obligation id, cash-event id, amount | Partial settlement is allowed; sum cannot exceed the remaining amount. |
| `ScenarioAssumption` | book id, opening cash, owner capital, loan/interest/timing/cost, optional output quantity/unit/price | Planning inputs are book-scoped; each saved field must affect an identified report. |
| `ImportBatch` | source hash, importer version, mode, state, timestamps | Commit is atomic; undo is batch-addressable. |
| `ProvenanceRecord` | target record/field, source locator, method, confidence, rationale | Required for every normalized, repaired, or enriched value. |
| `Notification` | type, title, explanation, related record/batch, review state | Persistent for import mutations until reviewed or archived. |

## Implemented v2 persistence foundation

Room schema 5 implements businesses, projects, books, categories, budget items, financial events, journals, postings, obligations, settlements, scenario assumptions, and import batches/notices/provenance (units were retired in schema 4, D-064; the import-hash index stopped being unique in schema 5, D-076). Repository transaction boundaries now enforce:

- scope consistency across business, project, book, and category;
- exact positive rupiah values;
- one projection and realization book per project;
- balanced posting insertion in the same database transaction as the source event;
- credit-event creation of a matching obligation;
- partial settlement no greater than the outstanding amount;
- correction by editing in place (same id, rebuilt journal) or deleting (DEC-019);
- report derivation from joined postings and cash-flow roles.

### Correction rules (edit and delete, DEC-019)

Every rule lives in `FinanceRepository`, inside the same transaction as the write, and is covered by `LedgerIntegrityTest` (instrumented). "Balik Transaksi" (an inverse journal kept beside the original) was removed in schema 11: owners fix a record by editing it.

| Action | Allowed when | Effect |
| --- | --- | --- |
| Record an event | amount > 0; scope, book and category agree | Event + balanced journal |
| Edit an event (Ubah) | the same checks as recording | Same id, import batch and creation time; journal and postings rebuilt from the corrected values |
| Delete an event (Hapus) | always | Event, journal and postings removed by cascade |
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
| Loan received | Cash increases; loan liability increases. |
| Loan principal repaid | Loan liability decreases; cash decreases. |
| Loan interest paid | Interest expense increases; cash decreases. |
| Sale on credit | Receivable increases; revenue increases; cash is unchanged. |
| Receivable collected | Cash increases; receivable decreases; revenue is not counted again. |
| Purchase on credit | Expense or asset increases; payable increases; cash is unchanged. |
| Payable settled | Payable decreases; cash decreases; expense is not counted again. |

Templates for these mappings must be testable and versioned. A contributor may not “fix” a report by adding an unrelated synthetic transaction outside this event model.

## Projection, realization, and comparison

- Projection and realization are complete parallel books.
- Duplicating projection into realization creates independent records with lineage; later projection edits do not mutate realization silently.
- Comparison reads both books and calculates variance. It does not own editable financial facts.
- Budget items and dated transactions may coexist in one book, but reconciliation rules prevent double counting.
- A report must declare which source class it uses: budget, transactions, journal postings, or an explicitly reconciled combination.

## Receivable and payable lifecycle

```text
OPEN → PARTIAL → PAID
  └──────┴─────→ OVERDUE   (due date passed, balance remaining)
OPEN → REVERSED            (source credit event reversed; blocked while payments stand)
```

- Status is never stored: `ObligationBalance.status(today)` derives it from the original amount, non-reversed settlements, the due date and the `reversed` flag.
- `overdue` is derived when the due date passes with a remaining balance.
- Every settlement creates a linked money movement and balanced posting.
- Cancelling an obligation preserves history and reverses or voids its accounting effect according to posting state.
- Full invoice design, tax, inventory valuation, and purchase-order workflows remain separate future scope.

## Financing semantics

- Loan disbursement and owner capital affect cash but are not operating revenue.
- Loan principal repayment affects financing cash flow but is not an operating expense.
- Interest is an expense and a cash outflow when paid.
- Post-financing cash flow starts from an explicitly defined opening cash balance and includes the recorded financing events exactly once.

## Derived reports

Reports are deterministic projections of canonical records:

- profit/loss derives from revenue and expense postings;
- cash flow derives from cash-account postings grouped by period;
- post-financing cash flow separately exposes operating and financing movements;
- budget variance compares compatible categories and periods;
- HPP, break-even production, and B/C ratio are shown only when required production assumptions are complete.

Every displayed total must allow drill-down to contributing records. A mismatch is a first-class reconciliation result, not a formatting issue.

## Agriculture as an optional template

An agriculture template may supply crop, land area, season, production unit, common cost categories, and workbook aliases. A service business may instead supply equipment, service unit, and job metadata. Neither template changes the universal core entities or makes its fields mandatory elsewhere.
