---
status: active
last_verified: 2026-10-22
audience: human-and-ai
sensitivity: internal
evidence_basis:
  - owner-confirmed workshop and product context dated 2026-08-15
  - ../arina-agri/AUDIT_REVISI_MANAJEMEN_KEUANGAN.md
  - ../arina-agri/references/CATATAN KEUANGAN PADI 1 Ha ADE.xlsx
---

# Project Guide

## Purpose

Build a standalone Arina Agri-branded Android application for practical financial management on phones and tablets. The product should serve people who may not own a laptop and may not have stable internet access.

The initial need came from workshop participants who asked for a mobile version of financial management. Their need was broader than cultivation: financial management should also work for agricultural machinery and equipment services, rentals, trading, and other small-business activities.

That request is evidence of a real problem. It is not yet evidence that a particular interface or feature set is usable; the product must be validated independently.

## Product thesis

Small-business operators need a low-friction way to record money movement, plan costs and revenue, understand cash position, compare plans with reality, and recover useful information from existing workbooks without becoming accountants or database operators.

The product must preserve accounting meaning while presenting familiar language such as `Uang Masuk`, `Uang Keluar`, `Anggaran`, `Setoran Modal`, `Ambil Pribadi`, `Stok`, and `Ringkasan`. It records cash only (DEC-011): money that actually moved, with no receivables, payables, loans or interest.

## Relationship to Arina Agri web

The web application supplies three kinds of evidence:

1. **Intent evidence:** projection, realization, comparison, RAB/budget, daily transactions, income statement, cash flow, financing, import, export, and reports.
2. **Golden-case evidence:** the Padi 1 Ha workbook and its corrected expected values.
3. **Failure evidence:** domain assumptions, implementation gaps, and quality failures that the new application must not inherit.

The new product may reuse validated formulas, fixtures, terms, and test ideas. It must not copy the legacy module wholesale or treat visual parity as a requirement.

## Primary users

- Farmers managing one or more cultivation cycles.
- Agricultural machinery or equipment service providers.
- Small agricultural traders and processors.
- Other micro and small businesses that need basic project and cash management.
- Workshop facilitators who teach financial management using phones or tablets.

## Jobs to be done

- Record a transaction in seconds while working in the field or workshop.
- Know current cash, income, expense, profit and stock without manual recapitulation.
- Create an estimate before starting a project and compare it with actual results.
- Fill the Mankeu Excel template once and bring many records in at the same time, with a preview and a clean undo.
- Export or back up data without being locked into an application server.
- Review richer reports on a tablet without losing the quick-entry experience on a phone.

## Current state

As of 2026-10-22 (verified against the code; decisions in `logs/DECISION_LOG.md`):

- Android app `com.mankeu`, version 0.0.6, Room schema 11: cash recording with Ubah/Hapus, two books per unit (Realisasi and Proyeksi), business plan with products, cost recipes and break-even, stock per unit, reports, PDF and Excel export, template import with undo, encrypted backup to a file or Google Drive, offered page tours and an in-app help center.
- Strict cash (DEC-011): no receivables, payables, loans, instalments or interest anywhere.
- Multi-device sync: framework implemented (DEC-013), not wired to the UI.
- Releases: v0.0.4 to v0.0.6 went through the Play Console pipeline to the internal testing track (`docs/PLAY_STORE_DEPLOYMENT.md`); no public production release is recorded. Schema 6 to 11 are unreleased.
- Research protocol: planned, not approved or executed.

## Success definition

Success is not the number of screens or features. A release succeeds when representative users can complete core tasks, every financial output can be traced and reconciled, offline use is dependable, phone and tablet layouts are usable, and import corrections are transparent.

## Sensitive boundary

The private history that motivated stronger governance is not part of this repository. The repository records only product requirements, engineering rules, objective legacy evidence, and contribution boundaries. Do not add private chat transcripts or personal accusations to product history.
