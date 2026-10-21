---
status: historical
last_verified: 2026-08-15
audience: human-and-ai
sensitivity: internal
evidence_basis:
  - ../arina-agri source, tests, and Git history at commit 949a9da
  - ../arina-agri/AUDIT_REVISI_MANAJEMEN_KEUANGAN.md
---

# Legacy Web Evidence

> **Dokumen riwayat.** Bukti dari aplikasi web lama (2026-08-15). Keadaan sekarang (Room schema 11): `README.md`, `docs/DOMAIN_MODEL.md`, `docs/V1_IMPLEMENTATION_SPEC.md`, `docs/diagrams/`, dan keputusan di `logs/DECISION_LOG.md`.

## How to use this evidence

The existing Arina Agri web finance module is not the implementation blueprint. It contains valuable intent, terminology, calculations, tests, and known failure modes. Port only behavior that is reconciled with the product and workbook contracts.

This snapshot describes commit `949a9da` on the stable-handoff branch as inspected on 2026-08-15. Reinspect before relying on later code.

## Reusable intent

- projection, realization, and comparison contexts;
- RAB/budget and dated transaction detail;
- income statement, cash flow, and post-financing cash flow;
- HPP, break-even, B/C ratio, working capital, loan, owner capital, and interest;
- multi-sheet Excel import/export and reconciliation;
- category matching and the intent to recover workbook information rather than demand duplicate manual entry;
- existing tests and fixtures as regression clues.

## Important correction about enrichment

The existing parser's recovery of a missing daily transaction from other workbook evidence was intentional. For the reference workbook, recovering `Sewa Lahan Rp7.000.000` is the correct product behavior because the complete workbook is authoritative.

The new application should retain that intent and improve its contract: deterministic recovery is automatic, provenance-backed, visible in preview, announced after commit, and reversible. The improvement is transparency and rigor—not removal of enrichment.

## Gaps that must not be copied

| Evidence | Risk in a universal native product | Required direction |
| --- | --- | --- |
| `lib/finance/rabTypes.ts` requires commodity, land area, and season-like fields | Service, rental, trade, and non-cultivation businesses become second-class or impossible | Universal core; optional agriculture templates |
| `useScenarioOutput.ts` derives reports from transactions while data-presence logic may also accept RAB-only state | A screen may appear populated while reports omit intended source facts | One documented canonical calculation model and explicit reconciliation |
| `scenarioCalculations.ts` stores financing inputs that are not consistently applied to post-financing output | Correct-looking fields can have no effect | Invariant tests connecting every persisted financing field to reports |
| Legacy export covers four principal sheets rather than the complete five-sheet contract | Import/export cannot round-trip the whole workflow | Export post-financing and comparison explicitly |
| Large finance UI/hooks concentrate multiple responsibilities | Changes are hard to reason about and verify end-to-end | Separate domain, application, data, and adaptive presentation |
| Web-oriented interaction and agricultural defaults | A responsive page is not automatically a field-friendly native workflow | Research and design for phone-first, tablet-adaptive use |

## What current tests prove—and do not prove

The inspected branch reported 517 passing automated tests during the earlier handoff stabilization. This is useful evidence that the tested legacy behaviors were green at that commit.

It does not prove:

- that every finance feature is usable or connected end-to-end;
- that the reference workbook round-trips with all five views;
- that financing fields affect every expected output;
- that mobile and tablet user tasks work on real devices;
- that legacy assumptions are suitable for a universal product;
- that a new native implementation may copy the module without independent verification.

Treat every migrated rule as a new domain decision with tests against this repository's contracts.

## Migration posture

- Do not copy the web UI or state hooks wholesale.
- Extract candidate formulas and mappings into isolated tests, compare them with the golden workbook, and rewrite in the native domain model.
- Preserve source attribution for corrections and matching rules.
- Plan legacy user-data migration only after the native schema stabilizes and real migration demand is confirmed.
- Keep the web and native release claims separate; success in one does not imply success in the other.
