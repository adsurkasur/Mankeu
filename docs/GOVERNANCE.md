---
status: baseline
last_verified: 2026-08-15
audience: human-and-ai
sensitivity: internal
evidence_basis:
  - owner-confirmed collaboration requirements dated 2026-08-15
  - docs/QUALITY_GATES.md
---

# Governance

## Authority

- The product owner has final authority over product intent, business meaning, source precedence, branding, scope, research claims, publication, and release approval.
- A technical contributor owns the quality and explainability of changes they submit, not unilateral authority over product meaning.
- AI may assist analysis and implementation but has no decision authority and is never accepted as evidence by itself.
- Material decisions are recorded in [Decision Log](DECISION_LOG.md), not reconstructed later from chat fragments.

## Collaboration contract

Contributors are expected to:

- investigate the reported user outcome and underlying business rule;
- state uncertainty early and request evidence when needed;
- reproduce failures and provide concrete test/log/device evidence;
- accept rework when implementation conflicts with the documented intent;
- distinguish implementation progress from integrated or user-validated completion;
- keep review focused on behavior and evidence rather than authorship or ego.

Mistakes and uncertainty are normal. Dismissal of reproducible failures, unsupported certainty, silent scope changes, or completion claims without evidence are not acceptable contribution behavior.

## Work ownership and continuity

- No contributor is the sole keeper of a critical rule, credential, release process, or undocumented deployment step.
- Work is divided into bounded issues/PRs with acceptance criteria and handoff notes.
- The repository contains enough context for another capable human or AI to continue from evidence.
- Critical ownership such as signing keys and Play Console access has a named primary and recovery owner before release.
- A contributor can be paused or reassigned without blocking the product; transition should protect both continuity and personal boundaries.

## Review gates

| Change | Required approval/evidence |
| --- | --- |
| Product meaning or source precedence | Product-owner approval and decision entry |
| Money/report calculation | Golden/invariant tests and reconciliation review |
| Import/export format | Template round-trip, rejection, atomicity, notice, and undo evidence (DEC-012) |
| Database schema | Migration, backup, rollback, and data-loss review |
| Cloud/auth/analytics | Privacy/data-flow ADR and explicit owner approval |
| Research protocol or public claim | Source/analysis trace and owner/research approval |
| Signing/publication | Named custody, reproducible release, rollback, and owner approval |

## Disagreement protocol

1. Restate the user outcome and disputed business rule.
2. List direct evidence, conflicting evidence, and assumptions separately.
3. Reproduce the behavior using a minimal fixture or workflow.
4. Compare options by user impact, correctness, migration cost, and reversibility.
5. Escalate product-meaning decisions to the owner and record the result.

Do not resolve disagreement by appealing only to implementation difficulty, AI output, job title, or who wrote the original code.

## Sustainable operating boundary

- Work may be paused when discussion or workload is causing acute health strain; the repository state and next action should make that pause safe.
- Non-urgent messages do not require immediate response.
- A handoff should reduce the owner's cognitive load, not require them to reteach undocumented intent repeatedly.
- Private interpersonal history stays outside this product repository. Only generalized, actionable standards belong here.

## Handoff packet for every work item

```text
Objective and acceptance criteria
Product/business evidence
Files and data affected
Implementation state
Tests/evidence and exact results
Migration/privacy/release impact
Known limitations and open decisions
Safe next action
```
