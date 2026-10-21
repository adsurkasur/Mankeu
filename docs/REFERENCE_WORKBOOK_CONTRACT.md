---
status: verified-baseline
last_verified: 2026-08-15
audience: human-and-ai
sensitivity: internal-reference
evidence_basis:
  - ../arina-agri/references/CATATAN KEUANGAN PADI 1 Ha ADE.xlsx
  - ../arina-finance-workbook-fixtures/sources/verified/WB-BASE-001/workbook.xlsx
  - ../arina-agri/AUDIT_REVISI_MANAJEMEN_KEUANGAN.md
  - workbook inspection dated 2026-08-15
---

> **Status: superseded (2026-10-20, DEC-012).** Mankeu now imports only its own Excel template; see `docs/audit/2026-10-20_RENCANA_PERBAIKAN.md` F-01. Kept as historical research evidence.

# Reference Workbook Contract

## Golden reference

| Attribute | Value |
| --- | --- |
| Filename | `CATATAN KEUANGAN PADI 1 Ha ADE.xlsx` |
| SHA-256 | `556192F76E63AB5CEBEE8F389EE49C1C3A337A6E9D0092151B18FEAC40786736` |
| Sheets | `RAB`, `Catatan Transaksi Harian`, `Laporan laba rugi`, `Arus kas`, `Arus Kas Pasca Pembiayaan` |
| Purpose | Golden workflow, calculation, reconciliation, import, and export fixture |

The workbook is the approved source of truth **as a coordinated whole**. No single sheet, formula, cell, or transaction list overrides the full cross-sheet intent automatically. The workbook also contains known defects, so source-of-truth status does not mean copying every formula literally.

The portable private test copy and 48 sanitized structural/business variants are maintained in `../arina-finance-workbook-fixtures`. Its manifest, checksum, correction register, expected-import artifacts, and validation report are the authoritative regression-corpus entry points. Future widyaiswara workbooks enter through that repository's intake protocol rather than being hard-coded into this importer.

## Source precedence

When workbook evidence differs, use this order and record the applied rule:

1. owner-confirmed intent and the approved correction register;
2. consistent final values repeated across multiple sheets;
3. valid formulas and valid cross-sheet relations;
4. individual raw rows and labels;
5. deterministic inference from the remaining evidence;
6. user confirmation when more than one interpretation remains credible.

An unresolved material conflict blocks final import. The importer must not select a convenient value and report success.

## Golden expected outputs

| Output | Expected value |
| --- | ---: |
| Total cost | Rp22,159,000 |
| Revenue | Rp45,500,000 |
| Profit | Rp23,341,000 |
| HPP | Rp3,165.5714 per production unit |
| Break-even production | 3,409.0769 production units |
| B/C ratio | 1.05334 |
| Working capital | Rp18,869,000 |
| Loan | Rp15,000,000 |
| Owner capital | Rp3,869,000 |
| Interest | Rp450,000 |
| Final post-financing cash | Rp22,891,000 |

Expected monthly net cash before financing:

| Month | Expected net cash |
| --- | ---: |
| July | Rp0 |
| August | -Rp13,464,000 |
| September | -Rp1,330,000 |
| October | -Rp840,000 |
| November | -Rp3,235,000 |
| December | Rp42,210,000 |

More display decimals may be retained internally. UI rounding must not change the underlying reconciliation.

## Canonical enrichment case: Sewa Lahan

The daily transaction sheet does not contain a `Sewa Lahan` row. That absence does not mean the expense is absent from the workbook:

- `RAB!E38` records an annual reference rate of Rp21,000,000;
- `RAB!F38` calculates the applicable amount as Rp7,000,000;
- the profit/loss sheet includes `Sewa Lahan` Rp7,000,000 and reconciles to total expense Rp22,159,000;
- the cash-flow sheet places Rp7,000,000 in August and reconciles the monthly flow.

Therefore, for this exact recognized workbook, adding a Rp7,000,000 land-rent expense to the imported model is **deterministic enrichment**, not an arbitrary invention and not a task the user should perform manually.

The importer must still disclose the enrichment. It must preserve the amount and expense meaning, cite all supporting source locations, identify the August period evidence, and mark any exact day chosen by a date-normalization policy as inferred. See [Import, Reconciliation, and Notifications](IMPORT_RECONCILIATION_AND_NOTIFICATIONS.md).

## Known defects and approved interpretation

| Defect | Required behavior |
| --- | --- |
| Break-even formula is inconsistent with the intended business calculation | Use the approved corrected formula and golden value; preserve a correction note. |
| A transport value is mistyped in a cash-flow location | Reconcile against the consistent transaction, RAB, and report evidence; do not copy the typo. |
| A total column omits the land-rent amount | Use the reconciled total of Rp22,159,000 and disclose the correction. |
| External references produce `#REF!` | Do not depend on unavailable workbooks; rebuild the calculation from local canonical records. |
| Some facts appear in reports or cash flow but not daily transactions | Classify with the enrichment policy rather than discarding them or demanding duplicate entry. |

Any newly discovered defect must be added to a versioned correction register with workbook hash, source locations, impact, decision, and approval state.

## Import acceptance

A conforming importer must:

1. recognize or explicitly map all five sheets;
2. validate the workbook identity or explain that it is an unrecognized variant;
3. preview direct rows, normalizations, deterministic enrichments, ambiguities, and conflicts separately;
4. reproduce all approved golden outputs after commit;
5. generate a visible notification for `Sewa Lahan` and every other automatic enrichment;
6. preserve field-level provenance;
7. complete atomically or leave the local database unchanged;
8. allow undoing the committed batch;
9. avoid double counting enriched facts already represented elsewhere.

## Export and round trip

Export must cover the same five financial views plus comparison where applicable. A supported import followed by export and reimport must preserve semantic results even if formatting, column widths, or formula placement differ. Golden totals, transaction identities, financing meaning, time periods, and provenance-relevant corrections must survive the round trip.
