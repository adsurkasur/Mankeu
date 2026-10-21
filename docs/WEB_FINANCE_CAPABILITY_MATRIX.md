---
status: historical
last_verified: 2026-08-15
audience: human-and-ai
sensitivity: internal
evidence_basis:
  - owner-confirmed scope and accounting decisions dated 2026-08-15
  - ../arina-agri source and tests at commit 949a9da26a1f35f5579ab0a8c7692197f01aabe1
  - ../arina-agri/AUDIT_REVISI_MANAJEMEN_KEUANGAN.md
  - docs/REFERENCE_WORKBOOK_CONTRACT.md
---

# Web Finance Capability Matrix

> **Dokumen riwayat.** Matriks kemampuan web 2026-08-15, dipakai sebagai bukti awal. Cakupan Mankeu kemudian dipersempit oleh DEC-011 s.d. DEC-019. Keadaan sekarang (Room schema 11): `README.md`, `docs/DOMAIN_MODEL.md`, `docs/V1_IMPLEMENTATION_SPEC.md`, `docs/diagrams/`, dan keputusan di `logs/DECISION_LOG.md`.

## Purpose

This audit identifies the complete financial-management intent visible in Arina Agri web and decides how each capability enters the native application. It is a behavior inventory, not permission to port web implementation defects.

Status meanings:

- **preserve** — legitimate product intent remains;
- **rebuild** — preserve intent but replace the implementation or interaction;
- **extend** — add behavior required by the owner-confirmed native scope;
- **defer** — deliberately outside native v1;
- **evidence only** — useful reference but not a native product requirement by itself.

## Capability matrix

| Capability | Web evidence | Web behavior / limitation | Native v1 decision | Acceptance direction |
| --- | --- | --- | --- | --- |
| Finance projects | `lib/finance/rabTypes.ts`, `useFinanceProjectController.ts`, `FinanceProjectDialog.tsx` | CRUD with name, commodity, land area, season, dates, and status; agriculture is structurally mandatory | **rebuild + extend** | Universal business/project fields first; agriculture fields are optional template metadata; archive instead of unsafe deletion |
| Multiple financial contexts | `FinanceScenarioEntity`, `useFinanceScenarioController.ts` | Creates `PROJECTION` and `REALIZATION`, defaults to realization, remembers mode per project | **preserve + rebuild** | Every project owns exactly one projection book and one realization book; active book always visible; unsaved work is protected when switching |
| RAB categories | `rabApi`, `useRabController.ts`, tests | Income/expense categories, presets and custom names, scenario-scoped | **preserve + extend** | User-managed universal categories with account role and optional template source; stable identity survives rename |
| RAB/budget items | `RabItem`, `RabPlanningView.tsx`, `RabItemForm.tsx` | Quantity, unit, unit price, planned total, planned cash month, aliases; add/edit/delete/filter/bulk delete | **preserve + rebuild** | Mobile-first budget editor, exact rupiah, decimal quantity, formula/explanation, reorder, archive/reversal-safe removal, projection and realization separation |
| RAB import | `RabImportDialog.tsx`, `parseRabWorkbook` | `.xlsx` drop zone; parser expects a recognizable workbook; warns and records skip reasons | **rebuild + extend** | Android file picker, workbook inspection, role/mapping preview, explicit unsupported rows, reconciliation, saved mappings, atomic commit |
| Transaction entry | `TransactionEntryForm.tsx`, `useTransactionBatchController.ts` | Income/expense, date, category, quantity, unit, unit price, nominal, note; batch create and preview | **preserve + extend** | Fast money-in/out plus credit sale/purchase, capital, drawing, loan, repayment, and settlement flows; progressive details; exact accounting preview |
| Transaction edit/delete | `TransactionEditForm.tsx`, ledger UI/controller tests | Direct edit, single/bulk delete | **rebuild** | Posted records use amend/reverse semantics with visible history; drafts may be deleted; no silent destruction of audit history |
| Transaction master data | `useTransactionMasterController.ts` | Preset/custom categories and units; custom data partly session/API-backed | **preserve + rebuild** | Fully local persistent categories, units, aliases, active/archive state, and business-specific defaults |
| Ledger browsing | `KeuanganView.tsx` and tests | Search, type/month filters, sort, selection, seven-row pagination, mobile cards/desktop table | **preserve + rebuild** | Search/filter/sort native list, grouped dates, amount direction, status/source badges, list-detail tablet pane, no arbitrary pagination |
| Link realization to budget | `useRabTransactionLinkController.ts`, matcher tests | Manual/bulk link and fuzzy suggestions; type compatibility enforced | **preserve + extend** | Suggested links expose confidence/reasons; automatic links only above a tested threshold; ambiguity requires confirmation; unlink/relink audited |
| Summary dashboard | `KeuanganView.tsx`, `financeCategoryChart.ts` | Income, expenses, profit/deficit, expense distribution | **preserve + extend** | Cash balance, accrual profit, money in/out, due AR/AP, budget variance, notices; charts always have text equivalents |
| Income statement | `FinanceIncomeStatementView.tsx`, `incomeStatementWorksheet.ts` | Groups by RAB category/item with fallback category/description; totals income, expense, profit/loss | **preserve + rebuild** | Derive from balanced postings, not transaction labels; accrual revenue/expense is separate from cash; drill down to sources |
| Cash flow | `FinanceCashFlowView.tsx`, `computeArusKasBulanan` | Monthly cash-in/out/net/cumulative, including zero months | **preserve + rebuild** | Derive strictly from cash postings; operating, investing, financing classification; zero periods remain explicit |
| Financing assumptions | `FinancingAssumptions`, `useFinancingController.ts` | Opening cash, owner capital, loan, periodic interest, disbursement/payment dates, other cost | **preserve + rebuild** | Planning assumptions in projection; actual capital/loan/interest/principal events in realization; every saved field has a tested report effect |
| Post-financing cash flow | `FinanceFinancingView.tsx`, `computeArusKasPascaPembiayaan` | Working-capital need, interest, ending cash, monthly flow | **preserve + correct** | Opening cash and owner capital must affect output; loan principal is not income; interest is expense; dates use explicit precision |
| Production/sales assumptions | `ProductionSalesAssumptions`, `useProductionSalesController.ts` | Production volume/unit and selling price | **preserve as optional template** | Generic output-volume/price assumptions; agriculture labels supplied by template, not core schema |
| Feasibility metrics | `scenarioCalculations.ts`, `FinanceIncomeStatementView.tsx`, BFA dialog in `KeuanganView.tsx` | Revenue estimate, profit, HPP, production BEP, B/C ratio, viability status; a second standalone BFA path also exists | **rebuild + consolidate** | One calculation engine; universal cost/unit economics terminology with optional agricultural copy; undefined denominators are explicit, never zero-shaped |
| Projection vs realization comparison | `compareScenarios`, `FinanceComparisonView.tsx` | Top metrics, categories, monthly flow, amount/percentage variance, unmatched flags | **preserve + extend** | Compare budget and actual by stable IDs/aliases; unmatched records remain visible; favorable/unfavorable meaning depends on income/expense role |
| Working-capital need | `computeKebutuhanModalKerja` | Absolute maximum pre-financing cumulative deficit | **preserve + explain** | Same core metric with source period and drill-down; opening cash treatment is explicit and tested |
| Simple receivables/payables | Not implemented in the web finance module | Cash-only transaction model cannot represent earned/incurred vs paid timing | **extend; owner-confirmed** | Accrual recognition plus separate settlement; due date, counterparty, outstanding balance, status, partial settlement, overdue view |
| Inventory | No complete finance implementation | Agriculture stock exists elsewhere, not a coherent universal finance subledger | **defer** | No quantity-on-hand or COGS engine in v1; asset/expense classification remains available |
| Complex invoices | No complete finance implementation | No end-to-end invoice lifecycle | **defer** | Simple reference/notes and receivable/payable records only |
| Excel export | `buildFinanceExportWorkbook` | Writes RAB, daily ledger, income statement, and cash flow; mode labeled; four sheets only | **rebuild + extend** | Export source tables plus derived views, projection/realization and AR/AP; re-import must preserve financial meaning and provenance |
| Cross-sheet enrichment | `parseRabWorkbook` lines 376–460; actual/golden tests | Intentionally creates missing daily facts from RAB/workbook evidence, but broadly treats every unmatched RAB item as a transaction and does not make provenance first-class | **preserve intent + constrain** | Automatic only when deterministic coordinated evidence agrees; preview and persistent notice show amount, inferred fields, sources, and reversal action |
| Reconciliation | `RabCategoryReconciliation`, parser tests | Declared vs computed category totals, skipped rows, math warnings | **preserve + extend** | Workbook-wide totals across all recognized views; blocking conflicts vs warnings; zero unexplained difference before commit |
| Generic workbook formats | Parser recognizes reference-like labels/positions | One family of workbook shape dominates behavior | **extend** | Detect header candidates and sheet roles, let user map fields, save mapping templates, never hard-code one workbook as the only format |
| PDF report | `lib/pdfReport.ts`, manual and AI report actions | Manual PDF plus network AI narrative/quota | **rebuild offline** | Shareable offline report from canonical local data; local deterministic insights first |
| Network AI analysis | `app/api/ai/financial-report/route.ts` | Requires network/service/quota and carries financial data outside device | **defer pending explicit privacy ADR** | Core reporting works without AI; no financial data leaves device in v1 |
| Backup/restore | Not a complete web finance workflow | Server persistence is not a portable user-owned backup | **extend; owner-confirmed** | Password-encrypted, versioned, checksummed local backup; validate and preview before atomic restore; retain recovery copy when possible |
| Cloud sync/auth | Web uses authenticated Supabase paths | Conflicts with local-first/no-login v1 and would introduce cloud ownership | **defer** | Potential Drive snapshot and Firebase auth experiments only after separate owner-approved design |
| Migration/classification | `useMigrationController.ts`, `migrationApi` | Moves legacy unclassified web data to projection/realization with audit log | **evidence only + reuse principle** | Native schema starts fresh; all future migrations are transactional, tested, and recoverable; import classifications are audited |

## Visual language to preserve

Source: `lib/theme.ts`, `lib/themeColors.ts`, `lib/ui/dashboardDesign.ts`, and finance components.

| Token/behavior | Web value | Native interpretation |
| --- | --- | --- |
| Primary | `#2D6A4F`; dark `#1B4332`; soft `#D8F3DC` | Arina green primary, deep-green emphasis, pale-green containers |
| Accent/error | `#E07A5F` | Warm terracotta for attention/error where contrast passes |
| Background | `#FAFAF8` | Warm off-white app background |
| Surface | `#FFFFFF` | White content cards/sheets |
| Text | `#2C2A29`; secondary `#6B6866` | Warm charcoal and muted warm gray |
| Divider | `#EBEBE6` | Quiet borders rather than heavy shadows |
| Card radius | 24 px | 24 dp major cards |
| General shape | 16 px | 16 dp sheets, fields, and panels where appropriate |
| Action/chip radius | 8 px | 8–12 dp controls/chips; keep touch targets at least 48 dp |
| Typography intent | Sora headings, Plus Jakarta Sans body | Use bundled/system-safe native typography initially; hierarchy/weight must preserve the same character without a network font dependency |
| Layout | compact stacked cards; wide tables/dialogs on desktop | phone cards and bottom navigation; tablet rail plus list-detail—not a scaled desktop table |

## Web defects and ambiguities that become native invariants

1. Reports must be derived from postings and source records, never independently persisted totals.
2. Projection and realization never share mutable records by accident.
3. Every financing input either affects an identified output or is removed from the form.
4. Revenue/expense recognition and cash movement are not conflated.
5. Import enrichment is never silent and never based only on a weak fuzzy match.
6. Deleting or editing posted/imported data retains reversible history.
7. Exact rupiah is stored as integer `Long`; decimal quantity is not used as money.
8. Every comparison preserves unmatched items rather than making totals appear reconciled.
9. Exported source data can reconstruct the same canonical records on re-import.
10. Agricultural defaults are optional templates, not universal required fields.
