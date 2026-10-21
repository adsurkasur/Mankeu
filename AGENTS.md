# Mandatory Agent Guidelines: Modular Logging Protocol

This repository adheres to a strict, modular change-tracking and logging standard.
All AI agents (Antigravity, Gemini, Claude, etc.) and automated tools working on this codebase **MUST** comply with these instructions.

---

## 1. Mandatory Logging Rules

Any task that modifies code, configuration, database schemas, documentation, or assets **MUST** record its actions in the `logs/` directory:

1. **`logs/ACTIVITY_LOG.md` (Session Log)**:
   - Append a new entry at the top of the activity log at the conclusion of every work session or task execution.
   - Include: timestamp, agent/role, session goal, specific actions taken, files created/modified, and verification results.
2. **`logs/DECISION_LOG.md` (Decision Records - ADR)**:
   - If the session introduces, changes, or deprecates an architectural pattern, library dependency, database model, API contract, or significant project strategy, document it as a new `[DEC-xxx]` entry.
   - Include: date, status, context, decision made, rationale/alternatives considered, and affected components.
3. **`logs/CHANGELOG.md` (Version & Release History)**:
   - For user-facing feature additions, bug fixes, breaking changes, or version bumps, update the changelog following the [Keep a Changelog](https://keepachangelog.com/en/1.0.0/) standard.
4. **Git Inclusion**:
   - Always stage and commit all updated files in `logs/` alongside the primary changes in the same atomic commit.

---

## 2. Engineering & Operational Invariants

- **Understand Before Touching**: Trace real call paths and read relevant docs before proposing edits.
- **Root Cause over Symptom**: Fix shared utilities and underlying root causes rather than patching one-off symptoms.
- **Verify Every Change**: Every non-trivial change must leave behind a runnable verification check (test suite, verification script, or assertion check).
- **Preserve Documentation Integrity**: Do not delete unrelated comments, docstrings, or architectural notes without explicit instructions.\n\n---\n\n# Agent and Maintainer Rules

These rules apply to every future code, documentation, test, migration, import, and release change in this repository.

## Start here

1. Read `README.md`, `docs/PROJECT_GUIDE.md`, `docs/PRODUCT_REQUIREMENTS.md`, `docs/DOMAIN_MODEL.md`, and `docs/QUALITY_GATES.md`.
2. For import work, also read `docs/REFERENCE_WORKBOOK_CONTRACT.md` and `docs/IMPORT_RECONCILIATION_AND_NOTIFICATIONS.md`.
3. Inspect the current Git status and preserve unrelated changes.
4. Classify every important statement as `verified`, `user-confirmed`, `inferred`, `proposed`, or `unknown`.

## Product invariants

- This product is not a feature-parity port of the web application. It must be significantly better.
- The core domain must remain usable for cultivation, agricultural services, machinery rental/service, trade, and other small businesses.
- Agriculture-specific fields and matching dictionaries belong in optional templates, not required core entities.
- Local operation must not depend on login, network availability, Firebase, Google Drive, or an application-owned server.
- Budget/projection and realization/actual are separate complete datasets. They are not shorthand for RAB versus transaction rows.
- Reports are derived outputs. They must never become a second hidden data-entry system.
- Money must use exact integer rupiah values. Never use floating-point types for stored money.
- A financial result with an unexplained mismatch is not “mostly correct” and must not be presented as complete.

## Import and export rules (DEC-012, 2026-10-20)

- Mankeu owns the Excel format. The blank template and the export file are the same format; an export must re-import without loss.
- Import reads only that template. A file without the template marker, or with missing/renamed sheets or columns, is rejected with the exact list of problems and a way to download the template. Never guess columns, dates, years or categories, and never "enrich" from other workbook shapes.
- Import is bulk entry into the currently selected business unit. It never creates businesses or units, and never names anything after the file.
- Unknown category names become custom categories, shown in the preview before commit.
- Re-importing an identical file is allowed but must show a clear duplicate warning in the preview.
- There are no columns for credit, debt, loans, instalments or interest (DEC-011, strict cash).
- Import must be atomic and undoable through a recorded import batch, and must leave a visible notice of what it added.

## Engineering behavior

- Start from business intent, then domain rules, then calculation, persistence, integration, and finally presentation.
- Test complete user workflows. A visible button, a compiling screen, or one correct subtotal is not evidence that the feature works.
- A claim of “done”, “correct”, or “working” requires named acceptance evidence.
- When evidence conflicts, investigate and explain the conflict. Do not repeat an AI-generated justification as diagnosis.
- Using AI does not transfer responsibility. The contributor must understand, verify, and be able to explain the resulting change.
- If requirements are unclear, state the uncertainty and ask or investigate. Do not protect prior work merely to avoid rework.

## Change discipline

- Prefer pure domain functions and explicit state transitions.
- Keep UI, domain, storage, import, sync, and reporting responsibilities separable and independently testable.
- Add or update golden, unit, integration, and end-to-end tests when domain behavior changes.
- Do not create a Firebase project, Play Console application, package identifier, signing key, production Drive integration, or analytics account without an explicit owner decision.
- Never commit real financial data, research participant records, credentials, tokens, keys, or backup contents.
- Update the nearest authoritative guide and decision log when a material decision changes.\n