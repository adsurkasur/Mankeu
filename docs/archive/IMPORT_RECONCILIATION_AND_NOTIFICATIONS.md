---
status: baseline
last_verified: 2026-08-15
audience: human-and-ai
sensitivity: internal
evidence_basis:
  - owner-confirmed enrichment-notification requirement dated 2026-08-15
  - docs/archive/REFERENCE_WORKBOOK_CONTRACT.md
---

> **Status: superseded (2026-10-20, DEC-012).** Mankeu now imports only its own Excel template; see `docs/audit/2026-10-20_RENCANA_PERBAIKAN.md` F-01. Kept as historical research evidence.

# Import, Reconciliation, and Notifications

## Product promise

Import should remove avoidable manual work without hiding assumptions. Recoverable facts are recovered; uncertain facts are presented honestly; conflicting facts are not converted into a false success state.

## Import pipeline

```text
select file
→ hash and inspect safely
→ identify workbook/schema
→ parse all coordinated sheets
→ normalize labels, units, dates, and money
→ build candidate domain events
→ reconcile cross-sheet evidence
→ classify every mutation
→ preview and request only necessary decisions
→ atomic commit
→ notify, audit, and offer review/undo
```

Parsing and reconciliation run before persistent financial records are changed. Import preview is generated from a temporary candidate model.

## Mutation classes

| Class | Definition | Commit behavior | User treatment |
| --- | --- | --- | --- |
| `DIRECT` | Value is present and internally consistent at the source location. | Automatic | Included in summary. |
| `NORMALIZED` | Meaning is unchanged; formatting, label, unit, or date representation is standardized. | Automatic | Explain material normalization; retain provenance. |
| `ENRICHED_DETERMINISTIC` | A missing canonical record/field is established by consistent evidence elsewhere in the supplied workbook. | Automatic | Persistent recovery notification; review and undo available. |
| `ASSUMPTION_AMBIGUOUS` | Evidence supports multiple credible values or meanings. | Wait for choice | Show alternatives, impact, and source evidence. |
| `CONFLICT` | Material sources contradict and no approved precedence resolves them. | Block commit | State exactly what prevents success and how to resolve it. |
| `UNSUPPORTED` | Content is outside the recognized schema or domain. | Skip only with explicit visibility | Preserve raw evidence where safe; never imply it was imported. |

Deterministic enrichment is still an inference operation; its automatic status comes from the strength of evidence, not from pretending no assumption occurred.

## Provenance contract

For each normalized or enriched field, store:

- import-batch identifier, source filename, hash, and importer version;
- target record and field;
- source sheet, cell/range or row locator;
- original display value and normalized value where relevant;
- method/classification and rule identifier;
- short human-readable rationale;
- confidence category (`deterministic`, `ambiguous`, or `unresolved`), not a decorative AI percentage;
- inferred attributes, including date precision;
- commit time, review state, and undo state.

If raw source content may contain personal or financial data, provenance remains local and follows the same backup/privacy controls as the financial database.

## Notification contract

An automatic-enrichment notification must include:

1. **what happened**—added, completed, normalized, or repaired;
2. **record and amount**;
3. **why**—the reconciliation rule in plain language;
4. **source evidence**—sheet and cell/row references;
5. **assumptions**—especially inferred period, exact date, category, or counterparty;
6. **impact**—which totals or reports changed;
7. **actions**—review details, edit source mapping, or undo the import batch.

The notification remains available in an import activity center after any temporary banner disappears. It must not be a one-time toast.

### Canonical Sewa Lahan notice

> **Pengeluaran Sewa Lahan Rp7.000.000 ditambahkan dari data workbook**<br>
> Catatan harian tidak memuat transaksi ini, tetapi RAB, laporan laba rugi, dan arus kas sama-sama menunjukkan Sewa Lahan Rp7.000.000. Sistem menambahkannya agar hasil tetap sesuai dengan workbook. Periode Agustus berasal dari sheet Arus kas; tanggal harian, jika diperlukan penyimpanan, ditandai sebagai hasil normalisasi. [Tinjau sumber] [Batalkan impor]

The implemented notice may be shorter on a phone, but all details must remain available in its review view.

## Date precision

Do not invent a precise historical day when the source only establishes a month:

- store source precision (`DAY`, `MONTH`, or `PERIOD`) alongside the normalized date/period;
- prefer a period-capable domain field for imported cash-flow facts;
- if a database or export requires a day, apply a documented deterministic policy and label the day as inferred;
- never present that normalized day as if it appeared in the workbook.

## Reconciliation rules

- Match semantically using normalized category, amount, unit, period, and source relation—not row order alone.
- Preserve different events that coincidentally share amount and description.
- Detect the same economic fact across budget, transaction, and report representations so enrichment does not double count it.
- Reconcile total expense, revenue, profit, monthly cash, and post-financing cash before commit.
- Reports are supporting evidence during import, then regenerated from canonical records after commit.
- A discrepancy includes its amount, affected scope, candidate causes, and source locations.

## Atomicity, idempotency, and undo

- No committed domain record is visible until the entire selected import passes required checks.
- Reimporting the same hash warns the user and offers cancel, replace, or an evidence-based merge; it never silently duplicates.
- Every created or changed record links to the batch.
- Undo deletes only that batch's effects (its transactions, edited ones included; plan lines are archived) and keeps the batch row as history (DEC-019).
- An interrupted or crashed import is resumed only from a safe staged state or discarded cleanly.

## Failure states

`Success` means committed records reconcile and all required notices exist. Other terminal states are `Needs decision`, `Blocked by conflict`, `Unsupported`, `Cancelled`, and `Failed safely`. A partially parsed workbook with missing totals is never presented as successful.
