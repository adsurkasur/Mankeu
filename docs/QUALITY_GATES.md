---
status: baseline
last_verified: 2026-10-22
audience: human-and-ai
sensitivity: internal
evidence_basis:
  - owner-confirmed zero-unexplained-mismatch standard
  - logs/DECISION_LOG.md (DEC-011 strict cash, DEC-012 template import, DEC-019 edit and delete)
  - objective lessons from the legacy web implementation
---

# Quality Gates

## Non-negotiable financial gate

There is no acceptable unexplained financial mismatch. A result can be intentionally rounded for display, but its underlying calculation, rounding rule, and remainder must be known and testable. A feature with a 1% unexplained discrepancy is not complete.

## Evidence vocabulary

| Label | Meaning |
| --- | --- |
| `implemented` | Code exists; behavior is not yet claimed. |
| `unit-verified` | Isolated rules pass focused automated tests. |
| `integrated` | Connected persistence/import/UI path passes integration tests. |
| `workflow-verified` | A complete representative user journey passes on a device/emulator. |
| `user-validated` | Representative users completed defined tasks under a recorded protocol. |
| `release-ready` | All relevant gates, migration, privacy, signing, and distribution evidence pass. |

Do not collapse these labels into `done`.

## Test layers

### Domain and invariant tests

- integer-money arithmetic and explicit decimal rounding;
- balanced postings for every user-facing event type;
- cash-type classification by category role: income, expense, owner capital, owner drawing, asset purchase;
- projection/realization independence;
- business plan: mix and per-product break-even, seasonal products, capital need, depreciation;
- stock levels: in, out (never below zero), count;
- report totals equal contributing canonical postings;
- edit in place keeps the record's identity and rebuilds a balanced journal; delete removes the journal;
- duplicate import and import undo behavior;
- invariants such as debit equals credit for every journal.

### Template tests

The Mankeu template is the only import format (DEC-012). `TemplateImportTest` and the instrumented import tests must verify:

- an export imports back without loss, including seasonal products and the plan period;
- the blank template imports as nothing, without errors;
- a non-template workbook, a missing sheet, or a renamed column is rejected with the exact reason;
- invalid cells are reported by sheet and cell;
- older template versions with optional columns absent still import;
- commit is atomic, a duplicate file shows a warning, and undo deletes exactly the batch's records.

The earlier reference-workbook golden fixture is kept only as history in `docs/evidence/verification-workbook/legacy-reference-format/`.

### Persistence and migration tests

- Room CRUD and transaction boundaries;
- schema migration from every publicly released version;
- downgrade/unsupported-version behavior;
- backup checksum, encryption, restore, and failure recovery;
- import-batch undo after later edits to its rows;
- process death and storage exhaustion at critical points.

### UI and accessibility tests

- fast entry on compact width;
- adaptive navigation and list-detail on medium/expanded width;
- selected business/unit always visible;
- TalkBack labels/order, 200% font scale, contrast, touch targets, keyboard focus;
- visible reconciliation and offline state;
- import preview shows new categories, conflicts, and the duplicate warning before commit; every import leaves a visible notice.

### End-to-end and device tests

- first run without login or network;
- create multiple businesses and units, then prevent cross-scope leakage;
- projection → realization → comparison workflow;
- cash in and out under each category role, edit and delete, stock movements, business plan;
- template export → import into another unit → reports → undo import;
- local backup → application data loss simulation → restore;
- APK install/upgrade and AAB pre-release checks on representative Android versions.

## Definition of done by change type

### Domain rule

- rule and source documented;
- pure tests cover normal, boundary, and invalid cases;
- report and migration impact assessed;
- no unexplained reconciliation change.

### Importer change

- source variants and precedence stated;
- mutation class and provenance tested;
- preview, atomic commit, duplicate behavior, and undo tested;
- template round-trip and rejection tests pass;
- user-visible notice/error content reviewed.

### UI change

- phone and tablet layouts verified;
- loading, empty, offline, error, and destructive states covered;
- accessibility checks pass;
- complete user action changes the correct persisted data and report.

### Release

- automated suite passes from a clean checkout;
- no secret or private-data scan findings;
- database migrations and backup/restore verified;
- representative workflow evidence recorded;
- target API/Play policy rechecked;
- signed artifacts, ownership, privacy declarations, changelog, rollback plan, and known limitations approved.

## Required change report

Every material handoff or pull request records:

```text
Claim:
Business evidence:
Changed behavior:
Tests and exact result:
Manual/device evidence:
Data migration/rollback impact:
Privacy/security impact:
Not verified:
Known limitations:
```

A screenshot of a screen, a passing parser unit test, or an AI statement is supporting evidence only. None proves the complete workflow by itself.

## Stop-ship conditions

- unexplained money mismatch or double counting;
- an import that guesses columns or silently discards rows;
- loss/corruption during import, migration, backup, or restore;
- core workflow depends on login/network contrary to local-first requirements;
- inaccessible primary task with no alternative;
- real private data or credentials in repository/artifacts;
- completion claimed without reproducible evidence;
- package signing or publication ownership is unclear.
