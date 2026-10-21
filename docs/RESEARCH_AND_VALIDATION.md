---
status: proposed
last_verified: 2026-08-15
audience: human-and-ai
sensitivity: internal-research
evidence_basis:
  - owner-confirmed workshop context dated 2026-08-15
  - ../jurnal_sus_test_arina_agri/README.md
  - ../jurnal_sus_test_arina_agri/SUBMISSION_GUIDE.md
---

# Research and Validation

## Evidence available now

- Workshop participants requested mobile access because phones are more broadly available than laptops in the intended audience.
- The related Arina Agri manuscript currently records that six respondents requested mobile access; that claim still requires regeneration from the authoritative response workbook before publication.
- The owner clarified that the need is universal financial management, including cultivation, machinery/equipment services, rentals, trade, and other small businesses.

These observations support further product discovery. They do **not** prove that the proposed application, interface, architecture, or feature set solves the problem.

## Research questions

1. Can intended users record common financial events correctly and quickly on their own devices?
2. Do users understand business/project scope, projection versus realization, cash versus profit, owner capital and drawings, and break-even?
3. Do the import preview and its notice build trust without creating unnecessary alarm or workload?
4. Can users restore, export, or move data without an application-owned server?
5. Which relative-advantage, compatibility, complexity, trialability, and observability factors predict adoption intent?
6. How do phone-only and tablet-capable users differ in tasks and report needs?

## Staged validation

### Stage 1 — Formative discovery

- interview representative farmers, service providers, rental operators, traders, and workshop facilitators;
- observe current notebook, messaging, spreadsheet, and memory-based workflows;
- test terminology and information hierarchy with low-fidelity flows;
- include users with different accounting literacy and device capability.

### Stage 2 — Prototype usability

Use scenario tasks, not feature tours:

- create two businesses and assign a unit correctly;
- record cash income, expense, owner capital, and an asset purchase, then correct one record with Ubah;
- create a simple budget and compare it with realization;
- build a simple business plan and read its break-even;
- fill the Mankeu template, import it, and undo the import;
- create and restore a backup.

Capture completion, error/recovery, time-on-task, assistance, comprehension, and qualitative reasoning. A successful tap sequence with misunderstood money meaning is not success.

### Stage 3 — Pilot and workshop

- use a versioned release candidate and frozen facilitator guide;
- record device/Android characteristics without collecting unnecessary identifiers;
- separate training effectiveness from product usability;
- log defects, workarounds, and unsupported business cases;
- do not change scoring rules after seeing outcomes.

### Stage 4 — SUS and adoption study

- administer the standard 10-item System Usability Scale consistently after meaningful hands-on use;
- preserve raw response mapping, reverse scoring, per-participant scores, exclusions, and analysis code/output;
- report sample size, distribution, missing data, and uncertainty, not only a mean;
- design Rogers Diffusion of Innovation items/protocol before collecting data and distinguish the five constructs;
- avoid causal or general-population claims unsupported by the design.

### Stage 5 — Journal package

- freeze product version, protocol, data dictionary, analysis procedure, figures, and claim register;
- reconcile abstract, tables, results, discussion, and conclusion against one analysis output;
- disclose convenience sampling, training exposure, device constraints, and researcher/product-owner roles;
- preserve an anonymized reproducibility package where consent and ethics permit.

## Minimum research governance

- Obtain informed consent in language participants understand.
- Participation, product access, workshop benefit, and research consent are separate choices.
- Collect the minimum personal and financial data required; use synthetic scenarios where possible.
- State compensation, withdrawal, recording, retention, deletion, and publication rules before data collection.
- Keep identities and raw financial records out of Git and product analytics.
- Seek institutional/ethics guidance appropriate to the intended publication before recruitment.
- Research telemetry, if any, is opt-in and separate from required application function.

## Claim discipline

Maintain a claim register with source, calculation, product version, sample, limitations, and approval. Use labels such as `participant request`, `observed behavior`, `SUS result`, `qualitative theme`, and `product hypothesis`; do not present them as interchangeable evidence.
