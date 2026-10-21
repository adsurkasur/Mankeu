---
status: active
last_verified: 2026-08-15
audience: human-and-ai
sensitivity: internal
---

# Evidence Index

## Evidence classes

| Label | Meaning |
| --- | --- |
| `owner-confirmed` | Direct product decision from the owner, dated and recorded in this baseline. |
| `verified` | Directly inspected artifact, source, history, test result, or official documentation. |
| `inferred` | Reasoned conclusion that still requires validation. |
| `proposed` | Recommended design, not yet implemented or user-validated. |
| `unknown` | Required information not established. |

## Local product evidence

| Evidence | Location | Supports | Limits/sensitivity |
| --- | --- | --- | --- |
| Legacy finance audit | `../arina-agri/AUDIT_REVISI_MANAJEMEN_KEUANGAN.md` | Intended five-view workflow, corrected calculations, legacy gaps | Snapshot; verify against current code before reuse |
| Reference workbook | `../arina-agri/references/CATATAN KEUANGAN PADI 1 Ha ADE.xlsx` | Historical golden case for the former reference importer (superseded by DEC-012) | Financial reference; do not redistribute casually; verify SHA-256 |
| Legacy finance source | `../arina-agri/lib/finance/` and related hooks/components | Existing parser, types, calculations, and implementation lessons | Not an authoritative native design |
| Legacy finance tests | `../arina-agri/tests/` | Regression cases and tested expectations | Passing tests do not prove complete user workflow |
| Native/mobile request summary | `../jurnal_sus_test_arina_agri/README.md` | Six respondents requesting mobile access in current draft | Must be regenerated from authoritative response data before publication |
| Submission discipline | `../jurnal_sus_test_arina_agri/SUBMISSION_GUIDE.md` | Mobile request is not proof of solution effectiveness | Publication guide, not product validation |
| Owner decisions | [D-log](DECISION_LOG.md) (D-001 to D-087) and [`logs/DECISION_LOG.md`](../logs/DECISION_LOG.md) (DEC-001 on) | Scope, local-first, multi-business/unit, strict cash, template import, business plan, edit/delete | Product authority; DEC entries state implementation status |

The former reference workbook identity is kept in [Reference Workbook Contract](REFERENCE_WORKBOOK_CONTRACT.md) (historical). Record future fixtures by hash, version, sensitivity, and permitted storage location.

## Official technical evidence

| Topic | Source |
| --- | --- |
| Compose | <https://developer.android.com/develop/ui/compose> |
| Adaptive layouts | <https://developer.android.com/develop/ui/compose/layouts/adaptive> |
| Android architecture | <https://developer.android.com/topic/architecture> |
| Offline-first guidance | <https://developer.android.com/topic/architecture/data-layer/offline-first> |
| Room | <https://developer.android.com/training/data-storage/room> |
| App Bundles | <https://developer.android.com/guide/app-bundle> |
| Google Play target API policy | <https://support.google.com/googleplay/android-developer/answer/11926878> |
| Capacitor architecture/product context | <https://capacitorjs.com/docs> |
| Flutter architecture overview | <https://docs.flutter.dev/resources/architectural-overview> |

Recheck time-sensitive platform policies immediately before implementation or release decisions.

## Deliberately excluded evidence

Private conversations and subjective interpersonal summaries were reviewed only to recover product intent, collaboration risks, and continuity requirements. They are deliberately excluded from this repository. Do not add personal accusations, health disclosures, or chat transcripts to product history. Objective behavior belongs in reproducible tests, issue descriptions, decisions, and governance rules.

## Evidence update rule

Every material claim must identify its class and nearest source. When evidence conflicts, preserve the conflict and resolve it through the precedence/decision process; do not silently overwrite inconvenient evidence.
