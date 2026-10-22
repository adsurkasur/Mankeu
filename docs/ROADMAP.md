---
status: active-planning
last_verified: 2026-10-22
audience: human-and-ai
sensitivity: internal
evidence_basis:
  - docs/PRODUCT_REQUIREMENTS.md
  - docs/QUALITY_GATES.md
---

# Roadmap

## Scheduling rule

This roadmap is a dependency map, not a fixed queue. Independent tasks may proceed in parallel and priorities may change without implying that lower rows are less valuable. No delivery date is currently committed for this product.

## Workstreams

| ID | Workstream | Outcome | Status | Depends on |
| --- | --- | --- | --- | --- |
| AAC-001 | Product baseline | Requirements, domain, import, UX, architecture, quality, research, and governance are documented | complete 2026-08-15; scope narrowed by DEC-011 to DEC-019 | none |
| AAC-002 | Open product decisions | Final name timing, minimum device baseline, and release ownership | package `com.mankeu` and minSdk 26 set; release ownership open | AAC-001 |
| AAC-003 | Native foundation | Reproducible Kotlin/Compose project with static checks and adaptive shell | implemented; device matrix pending | AAC-001 |
| AAC-004 | Domain kernel | Exact money, balanced cash events, businesses/units, two books, business plan, stock, report calculations | implemented and unit-verified | AAC-001 |
| AAC-005 | Local persistence | Room schema, repositories, migrations, and recovery tests | schema 11; migrations 7→11 tested step by step (`MigrationTest`) | AAC-004 |
| AAC-006 | Core mobile workflows | Scope, money in/out with Ubah/Hapus, Rencana Usaha, Stok, reports on phone/tablet | implemented; workflow-verified on emulator; field UX evidence pending | AAC-003–005 |
| AAC-007 | Template import | Mankeu template only (DEC-012), preview, atomic commit, duplicate warning, Batalkan impor | implemented and tested | AAC-004–005 |
| AAC-008 | Export and round trip | Template-format Excel export, PDF report, lossless reimport | implemented and tested | AAC-005, AAC-007 |
| AAC-009 | Receivables/payables | — | dropped by owner decision DEC-011 (strict cash) | — |
| AAC-010 | Backup/restore | Encrypted versioned local backup, restore, recovery copy | implemented; restores schemas 3–11 | AAC-005 |
| AAC-011 | Drive/auth and sync | Optional Drive backup and Firebase auth; encrypted multi-device sync | Drive backup and auth implemented; sync offered as a release feature (DEC-024; real two-phone Drive test pending) (DEC-013) | AAC-010 |
| AAC-012 | Beta readiness | Privacy, signing, target API, device matrix, release evidence, support/rollback | in progress (`docs/PLAY_STORE_DEPLOYMENT.md`) | core release scope complete |
| AAC-013 | Research validation | Formative tests, pilot/workshop, SUS, DOI analysis, and publication-quality evidence | planned; protocol first | usable beta |

## Milestone outcomes

### M0 — Documented baseline

Exit evidence:

- authoritative documents exist and cross-link;
- owner-confirmed decisions and open decisions are distinguishable;
- owner decisions are recorded in `logs/DECISION_LOG.md`;
- private interpersonal context is excluded from product history.

### M1 — Trustworthy core

Exit evidence:

- pure Kotlin domain tests pass independently of Android;
- every user event balances;
- reports reconcile exactly;
- projection and realization remain independent;
- plan and break-even figures are reproduced by unit tests.

### M2 — Useful offline application

Exit evidence:

- APK installs on representative phone and tablet devices;
- no login/network is required;
- business/unit, entry, plan, stock, and report workflows survive process restart;
- accessibility and adaptive-layout checks pass.

### M3 — Safe data mobility

Exit evidence:

- the Mankeu template imports atomically with a visible notice, and undo is clean;
- export/reimport preserves semantics;
- encrypted backup/restore survives a loss simulation;
- migration and rollback behavior are reproducible.

### M4 — Public beta candidate

Exit evidence:

- privacy, signing custody, Play requirements, support, known limitations, and release rollback are approved;
- no stop-ship condition remains.

### M5 — Validated product and research basis

Exit evidence:

- representative participants complete core tasks;
- SUS is calculated from raw responses with a reproducible pipeline;
- Rogers Diffusion of Innovation constructs are operationalized before analysis;
- product, workshop, and journal claims match the collected evidence.

## Next

1. Representative device matrix (phone and tablet, low-end Android) for the core workflows.
2. Test sync on two real phones with the owner's Google account; then decide compaction (`docs/SYNC_ARCHITECTURE.md`).
3. Release ownership, signing custody, and the first public Play release.
4. Research protocol, then formative tests with representative owners.
