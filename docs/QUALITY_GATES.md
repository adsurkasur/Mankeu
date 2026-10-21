---
status: baseline
last_verified: 2026-08-15
audience: human-and-ai
sensitivity: internal
evidence_basis:
  - owner-confirmed zero-unexplained-mismatch standard
  - docs/REFERENCE_WORKBOOK_CONTRACT.md
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
- financing classification: revenue versus capital versus loan principal versus interest;
- projection/realization independence;
- receivable/payable partial settlement and overdue derivation;
- report totals equal contributing canonical postings;
- duplicate and undo behavior;
- property tests for invariants such as debit equals credit and remaining obligation never becomes negative.

### Golden workbook tests

The exact hashed workbook in [Reference Workbook Contract](REFERENCE_WORKBOOK_CONTRACT.md) is a required golden fixture in a secure test-data strategy. Automated acceptance must verify:

- five coordinated sheets are recognized;
- approved totals and monthly values match;
- `Sewa Lahan Rp7.000.000` is enriched once, not omitted or duplicated;
- the enrichment notification and field provenance exist;
- known broken formulas/references do not corrupt the result;
- import is atomic and repeat handling is explicit;
- export/reimport preserves semantic values.

If the real workbook cannot be distributed in a public or shared test environment, keep its hash and expected-output contract in Git and use a sanitized structurally equivalent fixture. Record where restricted evidence was executed without committing the source.

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
- selected business/project always visible;
- TalkBack labels/order, 200% font scale, contrast, touch targets, keyboard focus;
- visible reconciliation and offline state;
- enrichment review shows source, rationale, inferred fields, impact, and reversal action.

### End-to-end and device tests

- first run without login or network;
- create multiple businesses and projects, then prevent cross-scope leakage;
- projection → realization → comparison workflow;
- cash, loan, capital, interest, receivable, payable, and partial settlement;
- reference import → notification review → reports → export → reimport;
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
- golden fixture passes;
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
- silent import enrichment or discarded source content;
- loss/corruption during import, migration, backup, or restore;
- core workflow depends on login/network contrary to local-first requirements;
- inaccessible primary task with no alternative;
- real private data or credentials in repository/artifacts;
- completion claimed without reproducible evidence;
- package signing or publication ownership is unclear.
