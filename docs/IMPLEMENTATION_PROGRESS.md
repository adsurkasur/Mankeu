---
status: historical
last_updated: 2026-08-29 05:45:00 +07:00
audience: human-and-ai
sensitivity: internal
purpose: interruption-safe implementation checkpoint
---

# Implementation Progress

> **Dokumen riwayat.** Catatan kemajuan 2026-08 (aplikasi masih bernama Arina Agri Calculator). Piutang, pinjaman, importer workbook referensi, dan pembalikan yang dibahas di sini sudah dihapus. Keadaan sekarang (Room schema 11): `README.md`, `docs/DOMAIN_MODEL.md`, `docs/V1_IMPLEMENTATION_SPEC.md`, `docs/diagrams/`, dan keputusan di `logs/DECISION_LOG.md`.

This is the live recovery record for the Arina Agri Calculator v1 implementation. Update it after every material milestone, before switching to a substantially different task, and before ending an implementation session. Do not rely on chat history as the only record of progress.

## Objective

Finalize the standalone native Android financial-management application under the Arina Agri brand. The scope is the financial-management capability intended by Arina Agri web, rebuilt into a substantially more usable, reliable, auditable, mobile-first product for phones and tablets.

This does **not** include the non-financial Arina Agri web features.

## Owner-confirmed scope and decisions

- One user can manage multiple businesses; each business can manage multiple projects or jobs.
- The first release is local-first and usable without login or internet.
- Android phone is primary; tablet layout must be adaptive.
- The app is native Kotlin/Jetpack Compose, not a wrapped web app.
- Financial intent and legitimate business rules from the web implementation are evidence, but broken or incomplete technical behavior must not be copied.
- Budget/projection and realization are separate, complete financial datasets and must be comparable.
- Receivables and payables use accrual semantics internally while cash movement remains separately visible:
  - a credit sale recognizes revenue and a receivable; collection increases cash and reduces the receivable without recognizing revenue again;
  - a credit purchase recognizes an expense or asset and a payable; settlement reduces cash and the payable without recognizing the expense again.
- Version 1 includes simple receivables/payables, Excel import/export, and encrypted local backup/restore.
- Version 1 postpones full inventory, complex invoicing, cloud sync, Firebase, Play publication, production package ID, and signing decisions.
- Excel support must be generic and must not be hard-coded to one reference workbook. The validated training workbook is a golden baseline, not the only accepted format.
- Import must support mapping and preview, coordinated workbook-wide reconciliation, visible enrichment/inference notices, provenance, ambiguity confirmation, conflict blocking, idempotency, atomic commit, reversible batches, and semantic export/re-import round trips.
- Deterministic enrichment such as the missing `Sewa Lahan Rp7.000.000` fact is intentional when supported by workbook evidence, but it must always produce a clear user-visible alert and audit trail.
- Primary compatibility validation uses stable Android 11/API 30 phone and tablet virtual devices. Modern compile/target checks remain separate.

## Durable baseline

| Repository | Commit at start | Start state |
| --- | --- | --- |
| `arina-agri-calculator` | `e831c7845c862e119408fbbc29626dbe092d9202` | clean |
| parent `managed-projects-handoff` | `442730977b526940409fab6751129f4b114eeb51` | clean |
| legacy evidence `arina-agri` | `949a9da26a1f35f5579ab0a8c7692197f01aabe1` | clean |
| workbook fixtures | `fcfd97d5456c5eb881db865278f5f964b2960010` | clean |

Baseline verification already recorded before this implementation session:

- `testDebugUnitTest` passed;
- `lintDebug` passed;
- `assembleDebug` passed;
- the MVP contains seven automated tests;
- the debug application was exercised on phone and tablet layouts;
- the old `arina_api30` AVD can show `System UI keeps stopping` when opening Android Settings and must not be treated as a trustworthy compatibility device.

Latest verified implementation checkpoints, both pushed to private `origin/main` on 2026-08-15:

- `2af4a9d` — Excel round trip, encrypted backup/restore, reversible imports, expanded reports, search/filter, budget edit, master data, 23-test gate, and API 30 evidence.
- `2b34f2a` — offline financial PDF, Android SAF action, real two-page API 30 artifact, Poppler/pdfplumber/visual acceptance, and a repeated complete quality gate.

## Phase status

| Phase | Status | Durable output / acceptance condition |
| --- | --- | --- |
| 1. Audit instructions, legacy finance capabilities/design, and Android baseline | completed | reproducibility repair, capability matrix, design tokens, and Android gap map documented |
| 2. Finalize domain architecture and rebuildable schema | completed | Room schema 2, balanced accrual policy, obligations/settlements, reversals, posting-derived summary, and schema export implemented |
| 3. Implement native Arina design system and intuitive information architecture | completed | five-destination phone/tablet experience implemented and visually verified on fresh API 30 AVDs |
| 4. Implement budget, realization, financing, AR/AP, reporting, and reversals | in progress | core entry, editable budget, partial settlement, financing/feasibility/monthly reports, master data, search/filter, and reversal flows are wired; the latest additions await the complete code/device gate |
| 5. Implement generic Excel import/export and encrypted backup/restore | completed | native exporter/import variants, encrypted portable backup, SAF UI, exact device round trip, wrong-password safety, restore, and reversible import batch verified on API 30 |
| 6. Validate with fixtures, automated tests, and fresh API 30 AVDs | in progress | fresh phone/tablet runtime smoke tests and UI evidence exist; workflow and final quality gates remain |
| 7. Security/UX review, documentation, commits, and pushes | pending | clean repositories and reproducible handoff |

## Current findings

- **Reproducibility defect found and closed at commit `3d141e1`:** `.gitignore` used the unanchored pattern `data/`, which ignored the production Kotlin package `app/src/main/java/.../data/`. `AppDatabase.kt`, `Entities.kt`, `FinanceDao.kt`, and `FinanceRepository.kt` existed locally but were absent from Git. The rule is now anchored as `/data/`, the four source files are tracked, and a separate clean checkout successfully passed the complete quality command.
- The existing MVP proves the basic native stack, exact integer-rupiah posting path, local multi-business/project scope, adaptive navigation, native OOXML parsing, visible deterministic enrichment, import provenance, atomic import, and duplicate-workbook protection.
- The MVP presentation has been replaced by an Arina-native five-destination experience: `Beranda`, `Catat`, `Anggaran`, `Laporan`, and `Lainnya`. Phone uses bottom navigation; expanded tablet uses a navigation rail and two-column content. Business, project, and projection/realization context remain explicit.
- Core UI paths now cover general cash/accrual event entry, budget items, credit sale/purchase, partial AR/AP settlement, capital/drawing/loans/assets, posting-derived summaries, projection-versus-realization comparison, assumptions, visible import notices, and auditable reversal rather than destructive deletion.
- Runtime accounting exercise found and fixed a cross-report reversal defect before checkpointing: reversing a receivable collection correctly restored cash and receivable balances, but its original operating cash-flow contribution remained because the reversal event was classified `NON_CASH`. Reversals now inherit the source event's operating/investing/financing role, and the report regression asserts that cash flow also nets to zero.
- Fresh API 30 AVDs are available at `E:\Android\avd\arina_phone_api30_stable.avd` and `E:\Android\avd\arina_tablet_api30_stable.avd`. They are cold-boot/no-snapshot compatibility devices. Opening Android Settings on the fresh phone did not reproduce the old `System UI keeps stopping` failure, confirming that failure belonged to the broken legacy AVD rather than this app.
- The remaining v1 hardening gaps are user-configurable/saved column mappings for unfamiliar spreadsheet layouts, replacement-after-reversal convenience, production-safe Room migrations, physical low/mid-range device tests, accessibility review, and release identity/signing. These are not silently represented as complete.
- Phase 5 implementation now writes ordinary multi-sheet OOXML without a heavyweight runtime dependency. Export sheets include metadata, projection/realization budgets and transactions, AR/AP, reports, comparison, assumptions, and import provenance/notices.
- Import now accepts three evidence levels: the validated five-view facilitator workbook with deterministic reconciliation; generic single-ledger workbooks recognized by date/description/income/expense headers; and canonical Arina exports with active credit/settlement linkage. Reversed source/history rows are intentionally excluded from state reconstruction and surfaced as a warning.
- Full-fidelity local backup uses a versioned logical database snapshot encrypted with PBKDF2-HMAC-SHA256 (310,000 iterations) and AES-256-GCM. Restore validates authentication, schema, and the complete table allow-list before an atomic replace. A wrong password or modified ciphertext cannot reach the database transaction.
- Android document-provider actions are connected for `.xlsx` export and `.aabk` create/restore. Backup creation requires an 8-character minimum and confirmation; restore warns that verified data replaces all local data. Android automatic cloud backup remains disabled.
- Committed import batches are visible and reversible. Reversal creates inverse journals in reverse event order, retains batch/provenance history, and never deletes the imported facts.
- Reporting now includes filled monthly cash-flow periods, financing needs, and business-feasibility calculations with explicit undefined-denominator handling. Projection-versus-realization comparison includes total budget. These additions have unit coverage but still require the current complete build/lint and API 30 UI gate.
- The ledger now has keyword and cash-direction filters. Budget items can be edited or archived. Active businesses/projects can be renamed, and category/unit master data can be added or archived from the app. These management surfaces likewise await the current final UI smoke test.
- The Android Gradle Plugin is intentionally left at `8.13.2` with Gradle `8.13`; no upgrade or daemon-toolchain migration is part of this product finalization unless a verified build requirement emerges.
- Android Studio must use a compatible JDK such as its bundled JVM 21, not JVM 25, for the current Gradle baseline.
- The audited web capability set and native treatment are recorded in `docs/WEB_FINANCE_CAPABILITY_MATRIX.md`. It includes projects, projection/realization, budget/RAB, ledger CRUD/batch/master data, budget links, grouped income statement, cash flow, financing, feasibility, comparison, Excel, PDF, and legacy migration evidence.
- The native visual source of truth is explicit: warm off-white `#FAFAF8`, white surfaces, Arina green `#2D6A4F`, deep green `#1B4332`, pale green `#D8F3DC`, terracotta `#E07A5F`, charcoal `#2C2A29`, 24 dp major cards, and 8–16 dp control/panel radii.
- `docs/V1_IMPLEMENTATION_SPEC.md` freezes the native information architecture, canonical records, accrual/cash journal policy, report contract, generic Excel stages/sheets, encrypted backup contract, and completion criteria before schema changes.
- Room schema 2 now contains businesses, projects, projection/realization books, units, stable-role categories, budget items, expanded financial events, journals/postings, obligations/settlements, scenario assumptions, import/audit records, and saved mappings.
- Repository writes validate business/project/book/category scope, create a balanced journal atomically, create obligations for credit events, cap partial settlement at outstanding balance, and reverse posted events with inverse postings rather than deleting history.
- The primary v2 accounting summary can now be derived from joined posting rows. Cash collection/payment does not duplicate revenue/expense; original and inverse journals net to zero.
- Schema 1 to schema 2 uses an explicitly documented destructive migration because no owner data requires migration. This is a temporary pre-release exception and a known release gate.

## Files changed in the active session

- `docs/IMPLEMENTATION_PROGRESS.md` — created as this interruption-safe checkpoint.
- `README.md` — adds this checkpoint as the first document in the reading order.
- `.gitignore` — anchors the local runtime-data exclusion so it no longer hides a Kotlin `data` source package.
- `app/src/main/java/com/example/arinaagri/calculator/data/AppDatabase.kt` — previously local-only Room database definition, now brought under version control.
- `app/src/main/java/com/example/arinaagri/calculator/data/Entities.kt` — previously local-only Room entities, now brought under version control.
- `app/src/main/java/com/example/arinaagri/calculator/data/FinanceDao.kt` — previously local-only DAO, now brought under version control.
- `app/src/main/java/com/example/arinaagri/calculator/data/FinanceRepository.kt` — previously local-only repository, now brought under version control.
- `docs/WEB_FINANCE_CAPABILITY_MATRIX.md` — evidence-backed inventory and native treatment for every audited web-finance capability.
- `docs/V1_IMPLEMENTATION_SPEC.md` — implementation contract for domain, UX, Excel, backup, privacy, and acceptance.
- `app/src/main/java/com/example/arinaagri/calculator/domain/*` — expanded accrual/cash event model, journal/reversal policy, obligation status, and posting-derived report engine.
- `app/src/main/java/com/example/arinaagri/calculator/data/*` — Room schema 2, DAO projections, scope-safe repository, budget, AR/AP, settlement, reversal, and audit persistence.
- `app/schemas/com.example.arinaagri.calculator.data.AppDatabase/2.json` — exported Room schema 2.
- `app/src/test/java/com/example/arinaagri/calculator/domain/*` — new accrual, reversal, AR/AP status, and posting-report regressions.
- `app/src/main/java/com/example/arinaagri/calculator/presentation/Theme.kt` — Arina light/dark palette, typography-compatible Material theme, and rounded component shapes.
- `app/src/main/java/com/example/arinaagri/calculator/presentation/ArinaCalculatorApp.kt` — adaptive five-destination UI and the financial workflow surfaces listed above.
- `app/src/main/java/com/example/arinaagri/calculator/presentation/AppViewModel.kt` — explicit business/project/book state plus budget, event, obligation, report, assumptions, reversal, and import orchestration.
- `app/src/main/java/com/example/arinaagri/calculator/data/FinanceRepository.kt` — UI-facing obligation/assumption operations plus source-role-preserving reversal classification.
- `app/src/test/java/com/example/arinaagri/calculator/domain/PostingReportCalculatorTest.kt` — cash-flow reversal regression added after the API 30 runtime exercise.
- `docs/evidence/v2-phone-api30-home.png` and `docs/evidence/v2-tablet-api30-home.png` — runtime visual evidence from fresh Android 11 compatibility devices.
- `app/src/main/java/com/example/arinaagri/calculator/exporter/*` — dependency-free OOXML writer and finance workbook composition.
- `app/src/main/java/com/example/arinaagri/calculator/backup/EncryptedBackupManager.kt` — bounded portable snapshot codec, authenticated password encryption, capture, and atomic restore.
- `app/src/test/java/com/example/arinaagri/calculator/exporter/*`, `backup/*`, and `importer/GenericWorkbookImportTest.kt` — workbook readability, generic/canonical import, encryption, tamper, and wrong-password regressions.
- `app/src/main/java/com/example/arinaagri/calculator/domain/BusinessAnalysis.kt` — deterministic financing, feasibility, and filled-month cash-flow calculations with explicit unavailable states.
- `app/src/test/java/com/example/arinaagri/calculator/domain/BusinessAnalysisTest.kt` — financing/feasibility and monthly-period regressions.
- `app/src/main/java/com/example/arinaagri/calculator/presentation/ArinaCalculatorApp.kt` — adds report tabs, ledger search/filter, budget editing, master-data maintenance, rename actions, Excel actions, encrypted-backup actions, and import-batch history/reversal controls.
- `docs/evidence/v1-tablet-api30-*.png` — gated runtime evidence for financing, feasibility, ledger search, budget editing, and the expanded management screen.
- `docs/evidence/v1-tablet-api30-restore-preview.png` — verified, pre-mutation restore summary on Android 11.
- `app/src/main/java/com/example/arinaagri/calculator/presentation/Theme.kt` — rewritten: web-aligned `ArinaWebPalette`, `ArinaSemanticColors`/`LocalArinaSemanticColors` for success/danger/warning tones.
- `app/src/main/java/com/example/arinaagri/calculator/presentation/Formatting.kt` — new; `toRupiah()`/`toDisplayDate()` extensions and `IndonesianLocale`, extracted so every screen file shares one definition.
- `app/src/main/java/com/example/arinaagri/calculator/presentation/components/SharedComponents.kt` — new; cross-screen composables (`NoticeCard`, `EmptyCard`, `SectionTitle`, `EventCard`, `ReportCard`, `ReportRow`, `ComparisonRow`, `SimpleMetricGrid`/`SimpleMetricCard`/`MetricTone`/`SimpleMetric`).
- `app/src/main/java/com/example/arinaagri/calculator/presentation/screens/HomeScreen.kt` — new; Beranda destination (hero card, metric grid, notices, ledger search/filter, activity feed).
- `app/src/main/java/com/example/arinaagri/calculator/presentation/screens/EntryScreen.kt` — new; Catat destination (event-kind picker and entry form).
- `app/src/main/java/com/example/arinaagri/calculator/presentation/screens/BudgetScreen.kt` — new; Anggaran destination (budget list, add/edit dialog).
- `app/src/main/java/com/example/arinaagri/calculator/presentation/screens/ReportsScreen.kt` — new; Laporan destination (overview, laba rugi, arus kas, pembiayaan, kelayakan, piutang/utang, perbandingan tabs and their dialogs).
- `app/src/main/java/com/example/arinaagri/calculator/presentation/screens/MoreScreen.kt` — new; Lainnya destination (business/project management, master data, import, export/backup, `RestorePreviewDialog`, `NameDialog`).
- `app/src/main/java/com/example/arinaagri/calculator/presentation/ArinaCalculatorApp.kt` — reduced from 1,831 lines to the navigation shell only (~460 lines); every screen composable moved to the files above. Later in the same session: floating action button removed, logo added to `ScopeHeader`/`AppRail`.
- `.gitignore` (parent `Managed Projects` repository, not this repository) — added `.claude-workspace/` so the assistant's sandbox-side SDK/build scratch space is never committed.
- `.gitignore` (this repository) — added `.kotlin/` (Kotlin compiler daemon session state that Gradle creates at the repo root regardless of `buildDir` redirection; harmless but was untracked clutter).
- `app/src/main/res/drawable/ic_arina_logo.xml` — new; Arina brand mark converted from the web repo's `public/logo arina.svg` (same path data, same `#029049` fill).
- `app/src/main/java/com/example/arinaagri/calculator/presentation/Theme.kt` — palette and shape values corrected against `lib/theme.ts` (the web's real, rendered design system) after the first owner visual review rejected the previous `globals.css`-sourced values; see the "design-fix round 1" verification log entry below.
- `app/src/main/java/com/example/arinaagri/calculator/presentation/screens/HomeScreen.kt`, `components/SharedComponents.kt`, `screens/EntryScreen.kt`, `screens/ReportsScreen.kt`, `screens/MoreScreen.kt` — corner-radius consistency pass (icon tiles, hero card) in the same design-fix round.

## Verification log

### 2026-08-15 reproducibility repair

- Working repository command: `gradlew.bat testDebugUnitTest lintDebug assembleDebug --stacktrace` with SDK and Gradle cache on `E:`.
- Working repository result: **BUILD SUCCESSFUL in 33s**; 58 tasks, 7 executed, 5 from cache, 46 up-to-date.
- Separate checkout: cloned private `main` at `3d141e101aa03bf2fb453fef0c9ac37d6e7b4cb2` to `E:\Android\temp\arina-calculator-clean-20260815-1558` without `local.properties`.
- Separate-checkout command: the same complete quality command using only `ANDROID_HOME`/`ANDROID_SDK_ROOT=E:\Android\Sdk` and `GRADLE_USER_HOME=E:\Android\gradle-cache`.
- Separate-checkout result: **BUILD SUCCESSFUL in 45s**; 58 tasks, 39 executed and 19 from cache.
- Unit result from generated XML: **7 tests, 0 failures/errors, 0 skipped** across four suites.
- Clean-checkout APK: `app/build/outputs/apk/debug/app-debug.apk`, 19,161,406 bytes.
- The temporary verification checkout remains on `E:` because automated recursive cleanup was rejected by the command safety layer. It is not a source of truth and contains no changes.

### 2026-08-15 v2 domain/schema foundation

- First transitional test run intentionally exposed one obsolete expectation: the old test invoked normal journal creation for the new `REVERSAL` event. The test was corrected to exercise `reversalFor()` with the original journal; this was a test-contract update, not a suppressed product failure.
- `gradlew.bat testDebugUnitTest`: **BUILD SUCCESSFUL in 9s**.
- Generated XML: **14 tests, 0 failures/errors, 0 skipped** across six suites.
- `gradlew.bat lintDebug assembleDebug`: **BUILD SUCCESSFUL in 28s**; 49 tasks, 11 executed and 38 up-to-date.
- Room exported `app/schemas/com.example.arinaagri.calculator.data.AppDatabase/2.json`.

### 2026-08-15 native Arina UI and fresh API 30 compatibility devices

- Replaced the technical MVP screens with the frozen five-destination information architecture and exact documented Arina color/shape tokens.
- Built and installed the debug application on both newly created Android 11/API 30 AVDs.
- Phone evidence: `docs/evidence/v2-phone-api30-home.png`, 1080 x 2400 at density 420.
- Tablet evidence: `docs/evidence/v2-tablet-api30-home.png`, 2560 x 1600 at density 320; visual review confirms navigation rail and two-column adaptive layout rather than a stretched phone surface.
- The fresh phone opened Android Settings without the legacy AVD's System UI fatal-stop pattern.
- First complete post-UI command `gradlew.bat testDebugUnitTest lintDebug assembleDebug --stacktrace`: **BUILD SUCCESSFUL in 35s**; 58 tasks, 17 executed and 41 up-to-date; **14 tests, 0 failures/errors, 0 skipped** across six suites. APK size: 19,776,115 bytes.
- Tablet workflow exercise created a Rp150,000 cash sale, a Rp200,000 credit sale, and a Rp75,000 partial collection. Observed states were correct before reversal: cash Rp225,000, profit Rp350,000, receivable Rp125,000. Revenue was not duplicated by collection.
- Reversing the collection restored cash to Rp150,000 and receivable to Rp200,000 while leaving profit Rp350,000. The exercise exposed the stale operating-cash-flow defect described above.
- Post-fix command `gradlew.bat testDebugUnitTest lintDebug assembleDebug --stacktrace`: **BUILD SUCCESSFUL in 22s**; 58 tasks, 17 executed and 41 up-to-date; **14 tests, 0 failures/errors, 0 skipped**.
- The fixed APK was installed over a deliberately cleared tablet-emulator app sandbox, and the full cash-sale → credit-sale → partial-collection → reversal sequence was repeated from a clean app state. Final values were cash Rp150,000, profit Rp350,000, operating cash flow Rp150,000, receivable Rp200,000, payable Rp0. This confirms the source-role reversal fix in the real Room/Compose runtime.
- Force-stop and cold activity restart preserved all final values. Log inspection found no application `FATAL EXCEPTION`, Room failure, or SQLite exception.
- Switching to `Proyeksi` showed five Rp0 headline/report values; switching back to `Realisasi` restored Rp150,000 cash, Rp350,000 profit, Rp150,000 operating cash flow, and Rp200,000 receivable. The two books are visibly and persistently isolated.

### 2026-08-15 Excel and encrypted-backup implementation and API 30 proof

- `XlsxWorkbookWriter` unit round trip proves two generated sheets, UTF-8 text/XML escaping, and exact integer Rp7,000,000 survive re-read by the hardened native OOXML reader. Invalid/duplicate sheet names are normalized uniquely within Excel's 31-character limit.
- Portable snapshot plus encryption round trip preserves null/integer/real/text/blob values. Wrong-password and one-byte-tamper tests fail authenticated decryption as required.
- Generic single-ledger test imports a non-agriculture workshop/services example without requiring the five-view baseline.
- Canonical export/import test preserves an active credit sale and linked partial settlement while excluding reversed source/history rows with a visible warning.
- First canonical test run failed because the fuzzy matcher intentionally removes the generic word `transaksi`, so it was inappropriate for exact export sheet identification. Canonical names/headers now use order-preserving exact normalization; the targeted two-test suite then passed.
- Real device export created `arina-agri-2026-08-15.xlsx` (11,412 bytes). Independent ZIP/XML inspection found 17 OOXML entries and ten semantic sheets: Metadata, projection/realization budgets and transactions, AR/AP, reports, comparison, assumptions, and import trail.
- Android's XML parser initially rejected an optional `DocumentBuilderFactory.isXIncludeAware` assignment with `UnsupportedOperationException`. Optional hardening setters are now guarded, while the required parser-hardening features remain enforced. The same real workbook then previewed successfully on Android 11.
- Canonical device round trip imported two active candidates into a new project, skipped two reversal-history rows with a visible warning, and reproduced the source state exactly: cash Rp150,000; profit Rp350,000; operating cash flow Rp150,000; receivable Rp200,000; payable Rp0.
- Reversing that import batch from the app produced auditable inverse events and returned all five values to zero without deleting provenance/history.
- Real device backup created `arina-agri-backup-2026-08-15.aabk` (9,490 bytes). After a deliberate Rp10,000 mutation, a wrong password was rejected before any database change and the mutated state remained intact. The correct password restored 39 rows across 16 tables and returned the exact pre-mutation values. Force-stop/cold restart preserved the restored state.
- Pulled verification artifacts are kept outside the repository at `E:\Android\temp\arina-device-export.xlsx` and `E:\Android\temp\arina-device-backup.aabk`; emulator Downloads were cleaned of the temporary backup after verification.

### 2026-08-15 finalization checkpoint after runtime proof (active)

- Added pure-domain financing and feasibility analysis, including pre-financing cash, financing gap, interest, ending cash, estimated revenue, total cost, unit cost, break-even quantity, and B/C ratio. Missing or zero denominators produce an explicit unavailable result instead of invented values.
- Added monthly cash-flow composition that fills months with no transactions and preserves operating/investing/financing/reversal classification.
- Added UI report pages for financing and feasibility, projection/realization total-budget comparison, ledger search/cash-direction filtering, budget editing, business/project rename, and category/unit master-data add/archive actions.
- The first complete unit invocation after these additions compiled production and test sources, then reported one assertion-type mismatch: the computed nullable revenue was the correct `Long` value Rp8,000,000, while the test supplied an `Int` expected value. The two nullable rupiah expectations were corrected to explicit `Long` literals; no production formula was changed.
- Complete quality command `gradlew.bat testDebugUnitTest lintDebug assembleDebug --stacktrace`: **BUILD SUCCESSFUL in 44s**; 58 tasks, 13 executed and 45 up-to-date. Generated XML reports **23 tests, 0 failures/errors, 0 skipped** across ten suites. Debug APK size: 19,567,316 bytes.
- Installed that exact APK over the existing Android 11/API 30 tablet data. Financing displayed cash before/after financing, gap, capital, loan, interest, and other cost from the existing Rp150,000 operating flow. Feasibility showed an explicit incomplete-assumptions state instead of zero-shaped fabricated metrics.
- Ledger search for `PakBudi` returned the collection and its reversal; applying the `Keluar` filter retained only the negative reversal. This verifies keyword/counterparty search and cash-direction filtering against persisted data.
- Added a Rp2,000,000 realization budget item (`PompaUji`, 2 buah × Rp1,000,000), opened its edit dialog with all source values prefilled, renamed it to `PompaUjiEdit`, and observed the update confirmation and unchanged exact total.
- Added temporary category `UjiKategori`, observed the active count increase, then archived it and observed it disappear while the active count returned to eight. This validates add/archive without touching built-in master data.
- Force-stop/cold restart retained `PompaUjiEdit` and Rp2,000,000. The Android crash buffer was empty after the complete exercise.
- Runtime evidence: `v1-tablet-api30-financing.png`, `v1-tablet-api30-feasibility.png`, `v1-tablet-api30-ledger-search.png`, `v1-tablet-api30-budget-edit.png`, and `v1-tablet-api30-more.png` under `docs/evidence/`.
- The latest report/management additions have therefore passed both code and API 30 tablet gates. Remaining matrix gaps are still listed honestly under Risks and unknowns.

### 2026-08-15 offline PDF follow-up (completed)

- Added a native Android PDF exporter that renders canonical local data without a network call or AI narrative. It includes business/project identity, projection and realization summaries, active budgets, transactions including reversal markers, outstanding AR/AP, assumptions, and import/provenance counts.
- Added a distinct `PDF` document-provider action beside Excel and encrypted backup. Excel remains the machine-readable round-trip format; PDF is explicitly positioned as the human-readable sharing/report format.
- The first quality invocation stopped at compilation because four exporter references used presentation-style names rather than the canonical `FinancialSummary` property names. They were corrected to `cashBalanceRupiah`, `operatingIncomeRupiah`, `receivableOutstandingRupiah`, and `payableOutstandingRupiah`; no calculation or persistence code changed.
- After the correction, the complete unit/lint/build gate succeeded in 42 seconds. The exact rebuilt APK exported a real 372,757-byte, two-page A4 PDF through Android SAF. Poppler `pdfinfo` recognized PDF 1.4 with no encryption, forms, or JavaScript.
- First visual render found a presentation defect: the final Realisasi summary row crossed onto page 2, and transaction kinds exposed raw English enum labels. Pagination now keeps each mode header with its complete summary, raw types are rendered in Indonesian, and the audit paragraph receives explicit top spacing. A second device export/render is required before this PDF is accepted.
- The second render confirmed correct geometry with `pdfplumber`, but its text extraction represented bullet separators as replacement glyphs. All PDF-only separators are now ASCII hyphens/pipes to improve search, copy/paste, and compatibility with older readers; a final device export/render remains required.
- The third device export is the accepted visual: 372,776 bytes, two A4 pages, PDF 1.4, with no JavaScript, forms, or encryption. Both pages were rendered at 120 dpi and page 2 was rechecked at 240 dpi. Headings, aligned amounts, pagination, dividers, Indonesian transaction labels, footers, and long audit copy are legible with no clipping/overlap.
- `pdfplumber` extraction contains zero replacement glyphs and confirms `Realisasi`, `Rp350.000`, `Penjualan kredit`, `Piutang - PakBudi`, and `Halaman 2 | laporan lokal` as searchable text.
- Accepted device artifact and first-page render are tracked as `docs/evidence/v1-tablet-api30-offline-report.pdf` and `docs/evidence/v1-tablet-api30-offline-report-page1.png`.
- Final quality command `gradlew.bat testDebugUnitTest lintDebug assembleDebug --stacktrace`: **BUILD SUCCESSFUL in 42s**; 58 tasks, 12 executed and 46 up-to-date. The existing ten suites remain **23 tests, 0 failures/errors, 0 skipped**. Offline PDF is ready for checkpoint commit/push.

### 2026-08-15 restore preview and recovery-copy hardening (device accepted; final gate pending)

- Restore is being split into two explicit stages: authenticate/decrypt/validate and display counts first; mutate only after a second owner confirmation. The preview exposes creation time, schema, table/row counts, and business/project/budget/transaction counts without logging content.
- A prepared restore and the copied password exist in memory only while the confirmation is open; cancel, ViewModel destruction, success, and failure zero the password buffer and discard the prepared plaintext snapshot.
- Before replacement, the current logical database is encrypted with the same password and written atomically to app-private `noBackupFilesDir` as the latest pre-restore recovery. The backup menu can export this `.aabk` through Android SAF after a successful restore.
- Added a pure unit regression for preview counts. Targeted unit run compiled the implementation and passed **24 tests**.
- Real Android preview of the older verified backup showed: schema 2, 16 tables, 39 rows, one business, one project, zero budget items, and four transactions. No mutation occurred before the second confirmation. After tapping `Batal`, `PompaUjiEdit` and Rp2,000,000 were still present.
- Confirming that preview captured the current database into app-private `noBackupFilesDir/arina-before-restore.aabk` (14,126 bytes), then restored the older backup. The expected state appeared: empty budget plus cash Rp150,000, profit Rp350,000, and receivable Rp200,000.
- The backup dialog then exposed an enabled recovery-export action. Android SAF exported `arina-sebelum-pemulihan-2026-08-15.aabk` at the same 14,126 bytes; a host-side verification copy is retained at `E:\Android\temp\arina-recovery-export.aabk`.
- Trying that recovery with `WrongPass2026` was rejected during preparation with the authenticated-decryption error. No preview appeared and cash/profit/receivable remained Rp150,000/Rp350,000/Rp200,000.
- Preparing the same recovery with the correct password showed 16 tables, 62 rows, two projects, one budget item, and eight transactions. Confirming restored `PompaUjiEdit` and Rp2,000,000 exactly. Android's crash buffer remained empty through both restore cycles.
- Device evidence is tracked at `docs/evidence/v1-tablet-api30-restore-preview.png`. A final complete unit/lint/build gate remains before commit/push.
- The implementation currently awaits compilation, lint, a real Android SAF export, and independent PDF inspection. It is not yet part of the latest verified checkpoint `2af4a9d`.

## Risks and unknowns

- The legacy capability and visual inventory is now mapped, but individual formula ports still require native golden tests before acceptance.
- The original MVP commits before `3d141e1` were not reproducible from Git because the Kotlin data layer was ignored. This is closed for current `main`; do not use older commits as buildable handoff points without that repair.
- The existing schema is explicitly rebuildable because no owner data needs migration, but the replacement still needs migration/export safeguards before user testing begins.
- Schema 2 currently uses destructive migration from the technical MVP. This must be replaced by tested migrations before real user data or a public beta exists.
- Spreadsheet import formats outside canonical Arina export, the golden five-view corpus, and recognizable single-ledger headers still need a user-configurable mapping screen and saved-mapping UX.
- Encrypted backup is password-owned: a forgotten password cannot be recovered. Restore now authenticates and previews before mutation and retains an exportable encrypted pre-restore recovery copy; future work may add recovery rotation rather than retaining only the latest copy.
- Fresh API 30 compatibility AVDs now replace the broken legacy device. They are development evidence, not a substitute for later physical low/mid-range Android testing.

### 2026-08-15 presentation-layer refactor and web-aligned palette (build/test unverified — pending SDK-capable session)

- Owner assessment: the native UI "still loses badly" to the `arina-agri` web dashboard, both visually and structurally. Verified two concrete defects: `Theme.kt` colors (`#2D6A4F` Arina green) did not match the web palette (`app/globals.css` `--color-primary: #16a34a`), and several summary tiles (old `MetricCard`) called `Card(modifier)` with no explicit `colors`, silently falling back to Material3's flat `surfaceVariant` gray regardless of what the metric meant.
- `Theme.kt`: replaced the color scheme with `ArinaWebPalette` derived from `arina-agri/app/globals.css` (`Green600 #16A34A`, `Green700 #15803D`, `Green100 #DCFCE7`, `Amber500 #F59E0B`, `Red600 #DC2626`, plus the web's dark-mode counterparts). Added `ArinaSemanticColors` (success/danger/warning containers) exposed via `LocalArinaSemanticColors`, since Material3's `ColorScheme` has no dedicated slot for a domain-meaningful "receivable is good, payable is a liability" distinction that the web dashboard expresses constantly through colored icon chips.
- Structural refactor: `ArinaCalculatorApp.kt` was a single 1,831-line file holding every screen, every dialog, and the navigation shell — larger than intended and contrary to `docs/ARCHITECTURE.md`'s own module-boundary guidance. Split into:
  - `presentation/Theme.kt` — palette and semantic colors (updated in place).
  - `presentation/Formatting.kt` — `toRupiah()`/`toDisplayDate()` extensions, now shared instead of file-private duplicates.
  - `presentation/components/SharedComponents.kt` — cross-screen primitives (`NoticeCard`, `EmptyCard`, `SectionTitle`, `EventCard`, `ReportCard`, `ReportRow`, `ComparisonRow`, `SimpleMetricGrid`/`SimpleMetricCard`/`MetricTone`).
  - `presentation/screens/HomeScreen.kt`, `EntryScreen.kt`, `BudgetScreen.kt`, `ReportsScreen.kt`, `MoreScreen.kt` — one destination's screen composables and directly-owned dialogs each.
  - `presentation/ArinaCalculatorApp.kt` — reduced to the navigation shell (`MainScaffold`, `AppRail`, `ScopeHeader`, `ChoiceMenu`, `ScreenContent` dispatcher, `Destination` enum): approximately 460 lines, down from 1,831.
- Visual fixes applied during the split (not just a mechanical file move): metric tiles now carry an explicit, meaningful container/icon color (primary for neutral operating metrics, success container for receivables, danger container for payables) instead of the uniform gray; `ObligationCard`/`StatusChip` in Reports now color-code by obligation status (paid=success, overdue=danger, partial=warning) instead of every chip using default neutral styling; the feasibility verdict chip (LAYAK/BELUM LAYAK) is now success/danger colored; `HeroCard` uses the theme's `primary`→`tertiary` gradient instead of a hardcoded `Color(0xFF1B4332)` literal so it now follows the updated web-aligned palette automatically.
- Consistency check performed without a build (no Android SDK in this session's sandbox — `local.properties` points at `E:\Android\Sdk`, a Windows-only path): grepped every top-level declaration across `presentation/**/*.kt` for duplicate definitions (none found) and cross-checked every `import com.example.arinaagri.calculator.presentation.*` against actual symbol locations (all resolved). This is static consistency checking, not a compiler guarantee.
- **Not verified yet:** `gradlew.bat testDebugUnitTest lintDebug assembleDebug` has not been run against this refactor. No existing test references the old monolithic file directly (tests target `domain`/`data`/`importer`/`exporter`/`backup` packages, not `presentation`), so no test breakage is expected from the move itself, but this is an expectation, not evidence, until the gate runs.
- AR/AP scope note (unrelated to this session but relevant going forward): the owner confirmed `AUDIT_REVISI_MANAJEMEN_KEUANGAN.md` and the golden Padi 1 Ha workbook contain zero references to piutang/utang/receivable/payable — AR/AP was scope added by a prior AI collaborator, not evidence-backed. It remains implemented (not removed) but is no longer treated as part of the required "final" contract; a future schema pass may fold it into an optional template rather than the core ledger.

### 2026-08-15 manual compiler-simulation review (bugs found and fixed; still no real compiler run)

- The assistant's sandbox has no Android SDK and could not reach `dl.google.com`, `maven.google.com`, `services.gradle.org`, or `repo1.maven.org` (network egress was allowlist-restricted at the time). Rather than wait, a manual line-by-line review of every new/changed file was performed as a partial substitute for a real compiler pass.
- **Found and fixed a real defect that would have failed compilation:** three files used the wrong package for `TextOverflow` — `import androidx.compose.ui.style.TextOverflow` instead of the correct `androidx.compose.ui.text.style.TextOverflow`. Affected: `ArinaCalculatorApp.kt` (`ChoiceMenu`), `presentation/components/SharedComponents.kt` (`EventCard`), `presentation/screens/MoreScreen.kt` (`ImportBatchCard`). All three corrected to the right package; verified no other file repeats the mistake.
- Cross-checked every `AppViewModel` method signature invoked from the five new screen files against the actual method signatures in `AppViewModel.kt` (`record`, `createBudgetItem`, `updateBudgetItem`, `archiveBudgetItem`, `settleObligation`, `saveAssumptions`, `createBusiness`, `createProject`, `renameBusiness`, `renameProject`, `createCategory`, `archiveCategory`, `createUnit`, `archiveUnit`, `commitImport`, `reverseImportBatch`, `reverseEvent`) — all parameter counts and types match.
- Cross-checked every referenced data/domain class (`BudgetItemEntity`, `CategoryEntity`, `UnitEntity`, `ImportBatchEntity`, `ObligationView`, `ImportNoticeEntity`, `AnalysisAssumptions`, `BusinessAnalysis`, `MonthlyCashFlow`) against their actual field names in `Entities.kt`, `FinanceRepository.kt`, and `BusinessAnalysis.kt` — all fields used in the new screen files exist with matching names.
- Verified the `'\u0000'` password-buffer-clearing calls in `ArinaCalculatorApp.kt` are intact and correct (a `Read`-tool rendering quirk made a null character look like a plain space during review; confirmed via a raw byte dump that the actual file content is the correct null-byte literal, matching the same pattern already used in `AppViewModel.kt`). No action was needed here — this was a false alarm caught before any edit was made.
- This review is evidence of "no obvious defect found by inspection," not evidence of "compiles cleanly." Kotlin/Compose generic-type inference, missing `@Composable` context propagation, and Material3 API signature drift between versions are all classes of error a manual read will not reliably catch. A real `gradlew` run remains required before this checkpoint can be called verified.
- **Network update:** the owner enabled "Allow network egress" with domain allowlist set to "All domains" in Cowork capability settings (Settings → Capabilities → Code execution and file creation) partway through this session. This was not yet re-tested before the session was paused. The next session's first action should be to confirm `dl.google.com`/`maven.google.com`/`services.gradle.org`/`repo1.maven.org` are now reachable, then proceed with the Android SDK setup that was previously blocked.
- A scratch workspace was prepared for this purpose: `.claude-workspace/` at the root of the parent `Managed Projects` repository (sibling to `Arina/`, `ARSC/`, etc.), containing empty `android-sdk/` and `gradle-home/` subfolders. It is listed in the parent repository's `.gitignore` (`.claude-workspace/`) so it can never be accidentally committed. It is safe to delete and regenerate at any time; nothing of record lives there.
- Current uncommitted state in this repository (`git status --short`): modified `ArinaCalculatorApplication.kt`, `EncryptedBackupManager.kt`, `AppViewModel.kt`, `ArinaCalculatorApp.kt`, `Theme.kt`, `EncryptedBackupCodecTest.kt`, `docs/IMPLEMENTATION_PROGRESS.md`, `gradlew.bat`; new untracked `Formatting.kt`, `presentation/components/`, `presentation/screens/`, and `docs/evidence/v1-tablet-api30-restore-preview.png`. Nothing has been committed since the last pushed checkpoint (`6d4c8c6`, restore-preview hardening still pending its own gate — see the prior "restore preview and recovery-copy hardening" entry above, which was already pending before this session's presentation refactor started). Do not assume these two pending changesets are the same work; both are real and both are awaiting the same build gate.
- A stray `index.lock` file was observed under the parent repository's `.git/modules/Arina/arina-agri-calculator/` during a `git status` call in this sandbox, with an "Operation not permitted" unlink warning. This did not block the status read and no git write was attempted in this session, but if a future session's git command hangs or fails claiming the index is locked, check for and manually remove that stale lock file first.

### 2026-08-15 real build gate achieved for the presentation-layer refactor (code/test/lint green; device/visual check still owner-only)

- Re-verified network egress at session start: `dl.google.com`, `maven.google.com`, `services.gradle.org`, and `repo1.maven.org` all returned real content (HTTP 200/301/302, not an allowlist block page). The "All domains" Cowork capability change from the prior session is confirmed active.
- **Discovered and worked around a filesystem constraint that the prior session's plan did not anticipate:** the `E:\Managed Projects` mount (visible in the sandbox as a FUSE mount) allows file creation and in-place overwrite, but **rejects `unlink`/delete of any file or directory** with `Operation not permitted`, confirmed by a direct `rm` test. This is the same restriction the product layer states for the mounted workspace folder, but it turned out to be enforced at the raw filesystem level too, not just by the assistant's own Write/Edit tool policy. Consequences found the hard way:
  - Extracting the JDK tarball with `tar` into `.claude-workspace/` on `E:` failed midway: the JDK archive hard-links many per-module `LICENSE`/`ASSEMBLY_EXCEPTION` files to a shared inode, and `tar` needs `unlink` to lay down the second and later links.
  - Running Gradle with `app/build` living on `E:` failed outright on the very first task (`processDebugNavigationResources` / `checkDebugAarMetadata`) with `Unable to delete directory ... Operation not permitted`, because Gradle's stale-output cleanup step deletes and recreates task output directories on every run.
  - **Adaptation:** all toolchain state (JDK 17, Android SDK, Gradle home, project cache, and Gradle's own build output directories) now live under `/sessions/exciting-vibrant-einstein/.build-tools/` — a real ext4 path inside the sandbox VM itself (confirmed to support delete/rename), not under `E:\Managed Projects\.claude-workspace` as originally planned. This path does not persist across sandbox sessions (unlike the `E:` mount), so a future session must redo the JDK/SDK download+install steps below; that takes a few minutes and is not itself evidence of anything, so it is safe to repeat.
  - Build output redirection was done **without editing any tracked `build.gradle.kts` file**: an external Gradle init script (`/sessions/exciting-vibrant-einstein/.build-tools/init-buildpath.gradle.kts`, outside the repository entirely) sets `layout.buildDirectory` for every project to a path under `.build-tools/build-out/`, passed via `--init-script`. `--project-cache-dir` was also pointed at `.build-tools/project-cache` for the same reason. Neither flag nor the init script touched any file inside the repository.
  - `local.properties` (gitignored, normally `sdk.dir=E:\Android\Sdk` for the owner's Windows machine) was temporarily **overwritten in place** with the sandbox SDK path for the duration of the build, then restored byte-for-byte to `sdk.dir=E\:\\Android\\Sdk` immediately after the build finished. Overwrite-in-place works fine on the `E:` mount (only delete/rename is blocked), so this round trip is safe and leaves no residue. The owner's Windows `local.properties` value is unaffected.
- JDK: Eclipse Temurin 17.0.20+8 (Linux x64) downloaded from `api.adoptium.net`/GitHub releases, extracted to `.build-tools/jdk17`. Required because the sandbox's system Java is `openjdk-11-jre-headless` (JRE only, no compiler) and AGP 8.13.2/Gradle 8.13 need a JDK 17+; no `apt` JDK package was available and there is no root/sudo in this sandbox.
- Android SDK: cmdline-tools `9862592` (latest listed in Google's `repository2-3.xml` at the time), `platform-tools`, `platforms;android-30`, `platforms;android-36`, `build-tools;34.0.0`, `build-tools;36.0.0` installed to `.build-tools/android-sdk` via `sdkmanager`; all SDK licenses accepted non-interactively.
- **First real compiler run surfaced a genuine defect the prior session's manual line-by-line review did not catch:** `ArinaCalculatorApp.kt`'s file-header KDoc comment (opens `/**` at line 74) contains, in its own prose, the literal substring `presentation/screens/*Screen.kt` — and Kotlin, unlike Java, **nests block comments**. The `/*` inside that path-like text opened a second nested comment; the doc comment's own closing `*/` at line 80 only closed the inner nesting level, leaving the outer comment open. The comment then silently swallowed the `Destination` enum and the entire `ArinaCalculatorApp` composable as commented-out text, until an unrelated `"*/*"` MIME-type string literal at line 172 (`backupPicker.launch("*/*")`) accidentally supplied the `*/` that closed it — at which point the stray trailing characters produced a cascade of ~50 "unresolved reference" / "expecting a top level declaration" errors. This is exactly the class of error the prior session's own writeup predicted a manual read would not reliably catch. **Fix:** reworded the comment to `each \`*Screen.kt\` file under \`presentation/screens/\`` so no `/*` substring appears in the text; verified no other file in the same change set (`Theme.kt`, `Formatting.kt`, `components/SharedComponents.kt`, all five `screens/*.kt`, `AppViewModel.kt`) has an unbalanced `/* */` pair, using a small script that tracks nesting depth per file (all others: balanced).
- **Second real compiler error, unrelated to the first:** `BudgetScreen.kt` was the only one of the five new screen files missing `import androidx.compose.foundation.lazy.items`. Without it, the `items(state.budgetItems, key = { it.id }) { item -> ... }` call at line 92 resolved to `LazyListScope`'s built-in `items(count: Int, ...)` member instead of the intended `items(items: List<T>, ...)` extension, producing `Argument type mismatch: actual type is 'List<BudgetItemEntity>', but 'Int' was expected` and follow-on unresolved-reference errors. **Fix:** added the missing import, matching the pattern already correctly present in `EntryScreen.kt`, `HomeScreen.kt`, `MoreScreen.kt`, and `ReportsScreen.kt`.
- Complete quality command, run from `E:\Managed Projects\Arina\arina-agri-calculator` with `JAVA_HOME`/`ANDROID_HOME`/`ANDROID_SDK_ROOT`/`GRADLE_USER_HOME` pointed at the sandbox toolchain and the init-script/project-cache-dir overrides described above:
  `./gradlew --init-script .build-tools/init-buildpath.gradle.kts --project-cache-dir .build-tools/project-cache testDebugUnitTest lintDebug assembleDebug --stacktrace`
  **BUILD SUCCESSFUL in 2m 4s**; 58 tasks, 23 executed and 35 up-to-date.
- Test evidence (parsed from the generated JUnit XML under `.build-tools/build-out/app/test-results/testDebugUnitTest/`, ten suites): **24 tests, 0 failures, 0 errors, 0 skipped.** This is one more test than the previously-recorded 23-test checkpoint (`2af4a9d`), consistent with `EncryptedBackupCodecTest.kt` appearing in this session's modified-file list.
- Lint evidence (parsed from `.build-tools/build-out/app/reports/lint-results-debug.xml`): 11 issues, **0 with `severity="Error"`**, all `severity="Warning"` (e.g. the pre-existing "AGP/Gradle has a newer version available" advisory, unrelated to this change). `lintDebug` completed without failing the build.
- Build evidence: `app-debug.apk`, 19,620,275 bytes, at `.build-tools/build-out/app/outputs/apk/debug/app-debug.apk` inside the sandbox (not copied into the repository or the owner's `E:` folder — this checkpoint is code/test/lint evidence, not device/visual evidence, and the APK itself has not left the sandbox).
- **What this checkpoint does and does not prove:** the presentation-layer refactor now compiles, its and the rest of the suite's unit tests pass, and lint raises no errors — closing the "not verified yet" gap the prior session flagged. It does **not** yet prove the owner's specific visual/functional concern (that the native UI still visually loses to the `arina-agri` web dashboard) is resolved, because that requires human eyes on a running emulator/device, which this sandbox cannot do. Device/visual verification remains the owner's step per the existing next-safe-action plan below.
- Uncommitted state is otherwise unchanged from the prior session's list (same modified/untracked files), except for the two bug-fix edits recorded above (`ArinaCalculatorApp.kt` comment wording, `BudgetScreen.kt` import) and this document. `local.properties` is back to its original owner-Windows value and is not part of the diff (gitignored).

### 2026-08-15 owner visual review round 1 (FAILED — five concrete defects, gate not passed)

- The owner installed/viewed the build described above and rejected it on visual grounds before any device installation instruction was even needed — the feedback was given from looking at the design, confirming the "next safe action" plan above correctly gated commit/push on this review. Verbatim findings:
  1. Corner radius should be much more rounded, "lumayan ekstrim" ("fairly extreme"), matching the web.
  2. Color palette "masih aneh" ("still feels off") — asked for a re-check against the website.
  3. The app should show the Arina logo; check the `arina-agri` web repo for it.
  4. Layout should be cleaner and non-redundant — specifically called out having both a "Catat" nav destination and a "+" button doing the same thing.
  5. Overall, the design still feels inconsistent and unintuitive.
- **Commit/push remained blocked**, exactly per the existing next-safe-action gate — this round is corrective work, not a new checkpoint.

### 2026-08-15 root-cause research: the web palette source used in the prior refactor was wrong

- Dispatched a read-only research pass over the `arina-agri` web repository (`E:\Managed Projects\Arina\arina-agri`) to get ground truth before touching any native code, instead of guessing at "more rounded" / "better colors."
- **Finding that explains complaint #2 directly: `app/globals.css`'s `--color-primary`/`--color-accent` family — which the prior presentation-layer refactor treated as the web's design source of truth — is dead CSS.** Grepping the entire web repo for `var(--color-` matches only `globals.css` itself; zero component files consume it. The web dashboard is actually built on MUI (`@mui/material`), themed via `lib/theme.ts`, and every real component (`DashboardKPI.tsx`, `KeuanganView.tsx`, `Sidebar.tsx`, `StokView.tsx`, ...) reads MUI palette tokens (`success.main`, `error.main`, `theme.palette.primary`, ...) that resolve to `lib/theme.ts`'s values — which are materially different from `globals.css`'s unused variables.
- Confirmed by reading `lib/theme.ts` directly (light theme = "Day / Warm Off-White", dark theme = "Slate Garden"):
  - Real primary: `#2D6A4F` (dark `#1B4332`, light `#D8F3DC`) — **not** the bright generic `#16A34A` the prior session pulled from `globals.css`.
  - Real success/income color: `#52B788` (sage green).
  - Real error/expense color: **`#E07A5F` — a coral/terracotta, not a red at all.** The prior session's `#DC2626` red was a plausible-looking but wrong guess.
  - Real warning: `#F4E285` (pale gold), not amber `#F59E0B`.
  - Real background/surface: `#FAFAF8` default / `#FFFFFF` paper — not slate-tinted `#F8FAFC`.
  - Real text: `#2C2A29` primary / `#6B6866` secondary (warm charcoal/taupe) — not slate `#0F172A`/`#64748B`.
  - Dark theme values (`#52B788` primary, `#68C99A` success, `#D4C060` warning, `#1E2620`/`#262D28` background/paper) happened to already match what the prior session pulled from `globals.css`'s dark-mode block, which is why dark mode looked closer to correct than light mode — `globals.css`'s dark block, unlike its light block, happens to mirror `lib/theme.ts`'s real dark palette. Only dark-mode `error` was still wrong (`#E08585` red-ish instead of the real `#D4836A` coral).
  - Confirms and refines a pre-existing project record: the "native visual source of truth" bullet already in this document (Arina green `#2D6A4F`, deep green `#1B4332`, pale green `#D8F3DC`, terracotta `#E07A5F`, charcoal `#2C2A29`) — established during Phase 3, before this session's presentation-layer refactor — was already correct. The prior refactor pass overwrote a correct palette with an incorrect one sourced from dead CSS, which is exactly what the owner's "masih aneh" feedback caught.
  - Border radius: web's default `MuiCard` override is a flat, literal `borderRadius: 24` (px) applied to nearly every card (`lib/theme.ts` light/dark `components.MuiCard.styleOverrides.root`), but the flagship dashboard/KPI hero cards go further with `sx={{ borderRadius: 4 }}`, which MUI resolves against `theme.shape.borderRadius: 16` to **64px** — a genuinely extreme corner radius for a wide card (`DashboardKPI.tsx:64`, `DashboardHomeView.tsx:233`, `StokView.tsx:303`). Buttons/dialogs frequently go further still via `sx={{ borderRadius: 8 }}` (→128px, effectively full pill). Icon tiles (~40px boxes) commonly use `borderRadius: 2` (→32px, near-circle). This substantiates the owner's "ekstrim" characterization with real numbers rather than adjusting radius on a guess.
  - Logo: found at `public/logo arina.svg` — a single flat-fill vector path (CorelDRAW export, `viewBox="0 0 8669.86 8669.86"`, solid `#029049` green), used as the web app's favicon/app icon (`app/layout.tsx:29-31`). No separate logotype exists; it is an icon-only mark.
- Also read `MainScaffold` in `ArinaCalculatorApp.kt` directly (not just the Home screen) and found the redundancy was worse than a single screen: a `floatingActionButton` inside `Scaffold` (lines 290-304 before this session's edit) rendered a "+" `FloatingActionButton`/`ExtendedFloatingActionButton` navigating to `Destination.ENTRY` on **every screen except Entry itself**, simultaneously with the bottom `NavigationBar`'s (phone) or `NavigationRail`'s (tablet) own "Catat" item pointing at the same destination. That is a persistent, app-wide duplicate control, not just the Beranda hero card's own "Catat transaksi" button (which is a single-screen, contextual dashboard CTA and was judged a materially different, more defensible pattern — left in place; flagged here explicitly in case the owner's next review still calls it out).

### 2026-08-15 design-fix round 1 implemented against verified web ground truth

- `Theme.kt` — replaced `ArinaWebPalette` and both `ColorScheme`s with the values read directly from `lib/theme.ts` above (light: primary `#2D6A4F`/`#1B4332`/`#D8F3DC`, secondary+error `#E07A5F`, success `#52B788`, warning `#F4E285`, info/tertiary `#74A57F`, background `#FAFAF8`, text `#2C2A29`/`#6B6866`, divider `#EBEBE6`; dark: primary `#52B788`, secondary+error `#D4836A`, success `#68C99A`, warning `#D4C060`, background `#1E2620`/`#262D28`, text `#DDE8DF`/`#8BA08E`). `ArinaSemanticColors` (success/danger/warning containers used across every screen) updated to match. Comments in the file now explicitly record that `globals.css` is not the real source, to stop a future session from repeating the mistake.
- `Theme.kt` shapes — raised the `Shapes` used as Material3's defaults (`extraSmall` 8→12dp, `small` 10→20dp, `medium` 16→24dp, `large` 24→28dp, `extraLarge` 28→32dp). `medium` is the default unstyled `Card()` shape in Material3, so this single change alone rounds every plain card across `NoticeCard`, `EmptyCard`, `EventCard`, `ReportCard`, and `SimpleMetricCard` without touching each composable — deliberately mirroring the web's own approach of one global default card radius plus a few explicit larger exceptions, rather than hand-tuning dozens of call sites.
- `screens/HomeScreen.kt` — the hero card's gradient background now uses `MaterialTheme.shapes.extraLarge` instead of a hardcoded `RoundedCornerShape(24.dp)`, so the app's single most prominent surface gets the most rounded treatment and stays in sync with future theme tuning instead of drifting from a stray literal.
- Icon-tile corner radii bumped for visual consistency with the web's near-circle icon tiles: `SharedComponents.kt` (`NoticeCard` 12→16dp, `SimpleMetricCard` 14dp was 10dp), `EntryScreen.kt` (event-kind picker tile 10→14dp, selected-kind icon tile 12→16dp), `ReportsScreen.kt` (obligation status icon tile 10→14dp), `MoreScreen.kt` (section icon tile 10→14dp).
- `app/src/main/res/drawable/ic_arina_logo.xml` — new vector drawable converted from the web repo's `public/logo arina.svg` (same path data, same `#029049` fill, same square viewport), added as a real app asset instead of inventing a placeholder mark.
- `ArinaCalculatorApp.kt`:
  - Removed the app-wide floating action button entirely (and its now-unused `FloatingActionButton`/`ExtendedFloatingActionButton` imports) — "Catat" is already a first-class destination in the bottom nav/rail, so the FAB was a second, simultaneously-visible control doing the same thing on every non-Entry screen. A code comment records the reasoning at the removal site.
  - `AppRail`'s tablet header no longer shows a placeholder "AA" text-in-a-circle; it now renders the real `ic_arina_logo` vector inside a neutral `surface`-colored circle (not `primaryContainer`, so the fixed-color brand mark keeps consistent contrast in both light and dark mode rather than sitting on a theme-tinted background it wasn't designed against).
  - `ScopeHeader` (the top bar shown on every screen, phone and tablet) now shows the same logo mark next to the "Arina Agri / Calculator" title, so the brand mark is visible at all times, not only on tablet.
- **Not addressed in this round, on purpose:** the Beranda hero card's own "Catat transaksi" CTA button was left in place (see reasoning in the research entry above) since removing the app-wide FAB already resolves the clearest instance of the redundancy complaint; complaint #5 ("overall inconsistent and unintuitive") is broad and partially addressed by the above (one shared shape system, one verified palette, a real logo, one less duplicate control) but has no single concrete fix — it depends on the owner's next visual pass to say whether it still holds.
- Complete quality command re-run against this round's changes (same sandbox toolchain, init-script build-dir redirection, and temporary `local.properties` overwrite/restore as the previous checkpoint — see that entry above for why):
  `./gradlew --init-script .build-tools/init-buildpath.gradle.kts --project-cache-dir .build-tools/project-cache testDebugUnitTest lintDebug assembleDebug --stacktrace`
  First attempt **failed** — a self-inflicted typo in the new vector drawable's namespace declaration (`http://schemas.android.com/apis/res/android` instead of the correct `.../apk/res/android`) tripped lint's `NamespaceTypo` check as a hard error. Fixed the single attribute; re-ran.
  Second attempt: **BUILD SUCCESSFUL in 1m 16s**; 58 tasks, 15 executed and 43 up-to-date.
- Test evidence: **24 tests, 0 failures, 0 errors, 0 skipped** — unchanged from the prior checkpoint, as expected (this round touched only `presentation`, no `domain`/`data` logic).
- Lint evidence: 11 issues, **0 with `severity="Error"`**, all `severity="Warning"` (same pre-existing advisories as before, e.g. the AGP/Gradle version suggestion) — the drawable namespace error from the first attempt is gone after the fix.
- Build evidence: `app-debug.apk`, 20,096,593 bytes (up from 19,620,275 bytes in the prior checkpoint — expected, given the added vector drawable and Image composables), at `.build-tools/build-out/app/outputs/apk/debug/app-debug.apk` inside the sandbox.
- `local.properties` was again temporarily overwritten to the sandbox SDK path for this run and restored byte-for-byte to `sdk.dir=E\:\\Android\\Sdk` immediately afterward, confirmed via `git status --short` showing it absent from the diff (still gitignored).
- **What this proves and does not prove:** code compiles, tests and lint still pass, and every fix is traceable to a verified web-repo source rather than a guess. It does **not** prove the fixes actually read as "matching the web" or "consistent and intuitive" on a real screen — that judgment call is the owner's, same as the first round, and is exactly what failed last time despite an equally green code/test/lint gate. This round should not be assumed to have satisfied complaint #5 in particular.

### 2026-08-15 owner visual review round 2 (FAILED again — five new findings, deeper than tokens)

- The owner reviewed design-fix round 1 and rejected it again, this time with feedback that went beyond color/shape tokens into information architecture. Verbatim findings:
  1. Navigation could be cleaner — suggested a floating action button or floating navigation buttons instead of the standard edge-to-edge bottom bar.
  2. Wants an account-style button opening to settings/menus (theme, motion, etc.) — a concept the app did not have at all.
  3. "Lainnya" as a bottom-nav destination feels unintuitive, not a natural peer of Beranda/Catat/Anggaran/Laporan.
  4. Palette still "aneh banget" ("really off") — specifically called out orange and slightly purple tones appearing, despite round 1's palette being sourced directly from `lib/theme.ts`.
  5. Overall layout still doesn't feel intuitive; explicitly authorized a larger rework ("kalau kamu bisa rombak, rombak aja gapapa" — "if you can overhaul it, go ahead").
- Commit/push remained blocked, same gate as before.

### 2026-08-15 root-cause: Material3 ColorScheme has ~35 tokens, round 1 only set ~24 of them

- Investigated why a palette sourced correctly from `lib/theme.ts` could still look "off" with orange and purple. Root cause is specific to how Compose Material3 builds a `ColorScheme`, not a bad color value:
  - `lightColorScheme()`/`darkColorScheme()` accept roughly 35 named color parameters. Any parameter not explicitly passed silently falls back to Material3's own baseline scheme, which is purple-seeded (Google's default M3 sample palette, primary ≈ `#6750A4`). Round 1's `Theme.kt` explicitly set about 24 of them (primary/secondary/tertiary/background/surface/error families) but left `surfaceTint`, `inverseSurface`, `inverseOnSurface`, `inversePrimary`, and the entire `surfaceContainer`/`surfaceBright`/`surfaceDim` family (added to Material3 to back newer default component styling) at that purple baseline.
  - `surfaceTint` in particular is blended into the background of every elevated `Card`/`Surface`/`TopAppBar` by Material3's default elevation-overlay behavior — left at the baseline purple, it puts a faint purple wash on nearly every card in the app. This is almost certainly the owner-reported "agak keunguan."
  - Confirmed via the project's Compose BOM (`2026.06.00`, `gradle/libs.versions.toml:5`) that this version of Material3 is recent enough that `NavigationBar`'s default container color and other components resolve to the `surfaceContainer` family, which was also left unset (baseline purple-grey) — a second, more visible source of the same defect, since the bottom nav bar is on screen constantly.
  - Separately, and not a Material3 defect: round 1's `Theme.kt` mapped M3's `secondary`/`secondaryContainer` slot to the same coral (`#E07A5F`) used for `error`, mirroring the web's literal `secondary.main === error.main` in `lib/theme.ts`. That mapping is low-risk in MUI (secondary is rarely auto-applied), but Material3 automatically applies `secondaryContainer` to `FilterChip`'s selected state and to `NavigationBarItem`/`NavigationRailItem`'s selection-indicator pill — both frequently visible in this app (the Proyeksi/Realisasi toggle, the ledger direction filter, and the selected bottom-nav/rail item). That is almost certainly the owner-reported "ada oranye": the selected nav item and selected filter chips were rendering in a peachy-coral tone that reads as orange, in places where a brand-green accent was expected.
  - This is recorded as a lesson for future palette work on this project: matching the web's *token values* is necessary but not sufficient — M3's *auto-application rules* for each ColorScheme slot must also be accounted for, since M3 and MUI apply their "secondary"-equivalent tokens to different, non-overlapping sets of components.

### 2026-08-15 design-fix round 2: full ColorScheme token set, floating nav, account surface, functional theme preference

- `Theme.kt` — every one of the ~35 `lightColorScheme()`/`darkColorScheme()` parameters is now explicitly set (previously ~24). Added: `surfaceTint` (pinned to the real primary green in both modes — the single highest-impact fix for the purple wash), `inverseSurface`/`inverseOnSurface`/`inversePrimary` (for `Snackbar`'s default styling, also previously purple-baseline), and the full `surfaceBright`/`surfaceDim`/`surfaceContainerLowest/Low/(none)/High/Highest` family derived from the app's own warm-neutral background/card tones rather than left at Material's baseline.
- `Theme.kt` — `secondary`/`secondaryContainer` remapped away from the coral (`error`) family to the web's distinct sage-green `info` token (`#74A57F` light / `#7DB88A` dark), which the web itself barely surfaces but is real, on-brand, and not otherwise used elsewhere in this app's semantics. `danger`/`error` still renders the real coral exactly as the web does — coral now shows up only in genuine error/expense contexts, not in nav/chip selection chrome. `tertiary` (used by the Beranda hero card's gradient) changed from the sage `info` tone to the deep green (`#1B4332` light / a new near-background `#16241C` dark), so the hero gradient reads as a single hue family (mid-green → deep green) rather than introducing a third color — directly answering "warna ga ada macem-macem."
- Navigation: the phone bottom bar and tablet rail are no longer edge-to-edge/full-height flush bars. Both are now wrapped in a `Surface` with `MaterialTheme.shapes.extraLarge` (32dp) corners, margin from the screen edges, and a shadow, floating above content — addressing the "floating navigation buttons" request without introducing a new, less-proven navigation pattern (still the same `NavigationBar`/`NavigationRail` components, just visually floated).
- Information architecture: `Destination` enum reduced from five entries to four (`HOME`, `ENTRY`, `BUDGET`, `REPORTS` — dropped `MORE`). "Lainnya"'s former content (business/project administration, category/unit master data, workbook import, export/backup) now lives behind a new **Akun & Pengaturan** full-screen surface, reached via an account-circle icon added to `ScopeHeader` (phone, every screen) and as an extra item appended to `AppRail` (tablet) — not as a nav-tab peer of the four transactional destinations. This mirrors how most apps separate day-to-day content tabs from account-level administration, directly answering "Lainnya itu aneh kalau sebagai page."
  - Implementation: a new private `AccountScreen` composable in `ArinaCalculatorApp.kt` wraps the existing `MoreScreen` composable (unchanged internally, just re-labeled from "Lainnya" to "Akun & Pengaturan" in its own `SectionTitle`) inside a `Scaffold` with a back-arrow `TopAppBar`, opened/closed via a `showAccount` boolean instead of the `Destination` enum. `ScreenContent`'s dispatcher lost the `MORE` branch and, with it, eight parameters (`importState`, `transferState`, `onChooseWorkbook`, `onExportWorkbook`, `onExportPdf`, `onCreateBackup`, `onRestoreBackup`, `recoveryAvailable`, `onExportRecovery`) that were only ever used by that branch — a incidental simplification, not just a relocation.
  - The shared `SnackbarHostState` is now threaded into `AccountScreen`'s own `Scaffold` too (it has its own `Scaffold`, separate from `MainScaffold`'s). Missing this would have been a real regression: export/backup success or failure messages are triggered from actions that now live exclusively inside `AccountScreen`, and without a `SnackbarHost` mounted there, `snackbarHost.showSnackbar(...)` would silently queue with nothing on screen to show it. Caught and fixed before the build-gate run, not after.
- New feature, not previously present anywhere in the app: a working **Tampilan** (display) preference — System/Terang/Gelap (System/Light/Dark) — inside Akun & Pengaturan. Backed by `ThemePreference.kt`, a small class wrapping plain Android `SharedPreferences` (not a new Room table/migration, since this is a device-local display choice, not accounting data, and deliberately kept out of the audited financial schema). `ArinaCalculatorApp` now computes `darkTheme` from the stored mode instead of always trusting `isSystemInDarkTheme()`, so the choice actually changes rendering, not just a UI element that does nothing.
  - "Motion" (also requested) was deliberately **not** added: the native app doesn't currently have adjustable decorative animation the way the web dashboard does (weather-card sun/rain/cloud animations, etc. — those are web-only). Adding a "motion" toggle that controls nothing would be a fake control. This gap is recorded here rather than silently dropped, and can be revisited if/when the app grows animated surfaces worth gating.
- Two real compile errors found and fixed during this round's own build attempts (not caught by review, same "manual read misses real errors" lesson as round 1):
  1. `TopAppBar`/`TopAppBarDefaults.topAppBarColors` are `@ExperimentalMaterial3Api` in this Compose BOM version — first build failed with "This material API is experimental." Fixed by adding `@OptIn(ExperimentalMaterial3Api::class)` to `AccountScreen`.
  2. `ThemePreferenceState`'s `var mode: ThemeMode by mutableStateOf(...)` auto-generates a JVM `setMode(ThemeMode)` method (Kotlin property setter), which collided with an explicitly declared `fun setMode(...)` doing the same job — "Platform declaration clash." Renamed the explicit function to `updateMode` and updated its one call site.
- Complete quality command re-run three times against this round's changes (same sandbox toolchain/init-script/local.properties overwrite-restore pattern as prior checkpoints): first attempt failed on the experimental-API opt-in, second failed on the declaration clash, third — `./gradlew --init-script .build-tools/init-buildpath.gradle.kts --project-cache-dir .build-tools/project-cache testDebugUnitTest lintDebug assembleDebug --stacktrace` — **BUILD SUCCESSFUL in 1m 46s**; 58 tasks, 20 executed and 38 up-to-date.
- Test evidence: **24 tests, 0 failures, 0 errors, 0 skipped** — unchanged, expected (still no `domain`/`data` changes).
- Lint evidence after a small follow-up cleanup (switched `SharedPreferences.edit().putString(...).apply()` to the KTX `edit { }` form lint suggested, a one-line non-functional change): 11 issues, **0 `severity="Error"`**, all `severity="Warning"` — same pre-existing AGP/Gradle/dependency-version advisories plus one new, expected `VectorPath` performance note on the logo's long path data (inherent to using the exact brand asset; not something to "fix" by degrading the shape).
- Build evidence: `app-debug.apk`, 20,096,593 bytes, at `.build-tools/build-out/app/outputs/apk/debug/app-debug.apk` inside the sandbox.
- `local.properties` was again temporarily overwritten and restored byte-for-byte to `sdk.dir=E\:\\Android\\Sdk`, confirmed absent from `git status --short`.
- **What this proves and does not prove:** the specific, diagnosable causes of "orange" and "purple" (unset ColorScheme tokens, secondaryContainer auto-application) are fixed with a documented mechanism, not a re-guess; the navigation is genuinely floating now; "Lainnya" is no longer a bottom-nav destination; a real (if partial — no motion control) settings surface exists. It does **not** prove the result reads as intuitive or matches the owner's mental model — round 1 also passed an equally green gate and was still rejected on both palette and IA grounds, so this round's code/test/lint pass should be read as "ready for another visual look," not "done."

## Files changed in this design-fix round 2 (in addition to the round 1 list above)

- `app/src/main/java/com/example/arinaagri/calculator/presentation/ThemePreference.kt` — new; `ThemeMode` enum and `SharedPreferences`-backed `ThemePreferenceState`.
- `app/src/main/java/com/example/arinaagri/calculator/presentation/Theme.kt` — full ColorScheme token set, secondary/tertiary remap (see above).
- `app/src/main/java/com/example/arinaagri/calculator/presentation/ArinaCalculatorApp.kt` — `Destination` enum reduced to four; floating nav bar/rail; new `AccountScreen`; `ScopeHeader`/`AppRail` gained an account entry point; `ScreenContent` simplified (MORE branch and its eight now-unused parameters removed); theme mode wired into `ArinaTheme(darkTheme = ...)`.
- `app/src/main/java/com/example/arinaagri/calculator/presentation/screens/MoreScreen.kt` — gained `themeMode`/`onThemeModeChange` parameters and a "Tampilan" card; `SectionTitle` re-labeled from "Lainnya" to "Akun & Pengaturan"; otherwise unchanged (still the same business/project/master-data/import/export/backup content, just reframed).

### 2026-08-15 owner visual review round 3 (dark mode accepted; 16 further findings, scope widened by owner)

- Dark mode was explicitly accepted ("udah bagus banget, aku suka") — the first part of this UI work to pass. Everything else drew further findings, and the owner authorised a larger rework ("kalau kamu bisa rombak, rombak aja gapapa"). Verbatim list: (1) still not intuitive, no specific diagnosis offered; (2) dark mode good; (3) layout could be more uniform/consistent; (4) tools are in the wrong places — Excel import sitting in settings feels odd; (5) too technical — equity/expense/revenue are terms a farmer should not need, the reference point should be the owner's own spreadsheet or the arina-agri web app; (6) the money-in/money-out cards on Catat do not read as buttons; (7) light mode's palette feels wrong; (8) add animation; (9) add skeleton loading; (10) add a splash screen; (11) add motion settings; (12) mouse-wheel scrolling does not work — bug or intended?; (13) the logo vs "Arina Agri"/"Calculator" lockup is out of proportion; (14) drop the "Tersimpan lokal" text, the crossed-cloud icon is enough; (15) add multi-language; (16) build an account-management prototype; (17) prefer one button that expands into other buttons over a sticky navbar.
- Scope decisions taken with the owner before starting (via a short clarifying round, to avoid building the wrong thing at this size): account management should anticipate a future **Firebase** backend with local account + email + Google sign-in; languages are **Indonesian + English** with the i18n *structure* being the deliverable; and all items were to be done in **one pass** rather than staged.

### 2026-08-15 primary-source research: the owner's own workbook defines the vocabulary

- Read `arina-agri/references/CATATAN KEUANGAN PADI 1 Ha ADE.xlsx` directly (five sheets: RAB, Catatan Transaksi Harian, Laporan laba rugi, Arus kas, Arus Kas Pasca Pembiayaan) rather than inventing "simpler" wording. This is the artefact the owner actually keeps, so it is the authority on what a farmer already understands.
- The daily-transaction sheet's entire column set is: `Tanggal | Uraian Transaksi | Volume | Satuan | Harga Satuan (Rp) | Pengeluaran (Rp) | Pemasukan (Rp)`. There is no debit/credit, no account type, no receivable/payable column — money is simply in or out.
- The report sheets group spending into four buckets the owner wrote themselves: **Input Usaha Tani** (Benih, NPK, Urea, Herbisida, Dolomit, Pestisida, Karung), **Tenaga Kerja**, **Jasa Alsintan** (Olah Lahan TR2, Tanam Transplanter, Panen Combine, Pompa Air), and **Lain-lain** (Iuran Pengairan, Transport, Sewa Lahan). Income is "Pendapatan / Hasil Penjualan".
- Units in real use: `Kg, Liter, Sak, HOK, Paket, Lembar`.
- Important nuance this settles: **"Laba/Rugi" and "Arus Kas" are not jargon here** — they are the owner's own sheet names, so those report titles can stay. What is jargon is the English enum vocabulary (`REVENUE`, `EXPENSE`, `ASSET`, `EQUITY`, `LIABILITY`) that was reaching the screen. This distinction is why the fix is a presentation-layer mapping rather than a rename of the domain model.
- Confirmed the leak path: `MasterDataDialog` in `MoreScreen.kt` rendered `category.role` — the raw stored enum name — directly into a `Text`, and its category-role picker hardcoded a second, inconsistent Indonesian mapping ("Pendapatan/Biaya/Aset/Modal/Kewajiban"). Both are now replaced by one shared mapping.

### 2026-08-15 design round 3 implementation (all 17 items addressed or explicitly deferred)

**Terminology (5)** — new `presentation/FarmerWords.kt` is the single translation layer from the domain's precise enums to the owner's vocabulary. The domain model keeps `REVENUE`/`EXPENSE`/`ASSET`/`EQUITY`/`LIABILITY` because the journal/posting rules depend on them being unambiguous; only the presentation layer is de-jargonised (Pemasukan / Pengeluaran / Alat & Mesin / Modal Sendiri / Pinjaman). `FinanceRepository`'s seeded categories and units were rewritten to the workbook's own buckets and units. Seeded data only affects a *fresh* database; existing installs keep what they have, and categories stay user-editable.

**i18n (15)** — `res/values/strings.xml` (Indonesian, default) and `res/values-en/strings.xml` (English) now hold the app-shell, navigation, Beranda, Catat, Pengaturan, Akun, and Berkas & Data strings. Language is a real per-app setting: `MainActivity.attachBaseContext` wraps the base context with the saved locale (the reliable route for API 26+, since the framework `LocaleManager` is API 33+), and changing it calls `Activity.recreate()`. **Honest scope note:** Laporan and Rencana still contain hardcoded Indonesian strings; the structure and switcher are complete and proven, but string extraction for those two screens is outstanding and must not be described as finished.

**Navigation (17)** — new `components/ExpandingNav.kt`: a single bottom-right button that expands into a labelled vertical stack of destinations (Beranda, Catat, Rencana, Laporan, plus Berkas & Data) and collapses again, with a tap-anywhere-outside scrim. The persistent phone bottom bar is gone entirely. Tablets keep the rail, where a permanent side strip costs no content area and aids orientation. Every screen's `LazyColumn` gained ~96dp of bottom content padding so the last row can never hide under the floating button.

**Tool placement (4)** — `MoreScreen`'s composable is now `DataScreen` ("Berkas & Data"): Excel import, export, encrypted backup, farm/field administration, and category/unit master data. Appearance/language/motion/account moved to a new `SettingsScreen` in `screens/AccountSettingsScreen.kt`. Import is no longer inside settings. A duplicate `ImportCard` call (the card was being rendered twice on the old screen) was found and removed while restructuring.

**Catat buttons (6)** — the eleven equal-weight kind cards became two large, filled, colour-coded `Surface` buttons (green Uang masuk / coral Uang keluar) with elevation and a border, sized 112dp tall; the remaining nine kinds collapsed behind an "Jenis lain" expander. Helper text was rewritten from journal mechanics to field language ("Barang sudah diterima, bayar menyusul" rather than "Biaya sekarang, kas saat dilunasi").

**Light palette (7)** — root cause found: round 2 set `surface = BackgroundDefault` (`#FAFAF8`) in *both* schemes, so every card was exactly the same colour as the page behind it and nothing separated visually. Cards are now the web's `background.paper` (`#FFFFFF` light / `#262D28` dark) on the page colour (`#FAFAF8` / `#1E2620`), which is the two-tone relationship the web dashboard actually uses. Dark mode keeps its accepted look while gaining the same explicit separation.

**Motion, animation, skeleton, splash (8, 9, 10, 11)** — `ArinaMotion`/`LocalMotion` carry an app-wide animation budget driven by a real three-way Gerakan setting (Penuh / Dikurangi / Mati); durations, not just a boolean, so "Dikurangi" is meaningful. Screen changes cross-fade with a slight vertical offset via `AnimatedContent`. `components/Skeleton.kt` provides pulsing hero/metric-grid/list placeholders, shown while `AppUiState.isLoading` is true (a new flag that is only true for the pre-first-read placeholder emission). Splash uses the AndroidX `core-splashscreen` API with the brand mark on the warm off-white background, installed before `super.onCreate()`. Note that round 2 explicitly declined to add a motion toggle because nothing was animated yet; that reasoning no longer holds now that animation exists, so the setting was added together with the animation it controls.

**Header/logo/cloud text (13, 14)** — the lockup is now a 34dp mark (was 28dp) beside a single `titleLarge` "Arina Agri" over a small `bodySmall` subtitle, instead of a small mark beside a `labelLarge` line stacked over a heavier `titleLarge` line that competed with it. The "Tersimpan lokal" `AssistChip` is reduced to the crossed-cloud icon alone (with the text preserved as its content description and as a full sentence inside Pengaturan → Akun).

**Account prototype (16)** — `SettingsScreen` hosts a local profile (name, saved to `SharedPreferences`, with an initial-letter avatar) plus a deliberately **disabled** preview of Google sign-in, email sign-in, and "Sinkron ke Google", each visibly marked as not connected by an on-screen notice. Nothing touches the network. This anticipates the Firebase backend the owner described without implying it already works.

**Scroll wheel (12)** — **not fixed, and not reproducible here.** Every screen body and every dialog body already uses `LazyColumn`, which supports mouse-wheel (`ACTION_SCROLL`) input by default, and the sandbox has no emulator or physical device to reproduce mouse input on. Recorded as an open question rather than silently closed: the owner needs to say which screen it happened on before this can be diagnosed honestly.

**Verification** — complete quality command (same sandbox toolchain, init-script build-dir redirection, `local.properties` overwrite-then-restore as previous rounds): **BUILD SUCCESSFUL in 1m 57s**; 58 tasks, 16 executed and 42 up-to-date. **24 tests, 0 failures/errors/skipped** (unchanged; this round touched presentation, resources, and seed data, not the accounting engine). Lint: 32 issues, **0 `severity="Error"`**. Warning count rose from 11 to 32, all benign and reviewed: mostly `UnusedResources` for English/Indonesian strings not yet wired into Laporan/Rencana (the honest i18n gap noted above), plus one `AppBundleLocaleChanges` advisory about Play Core language splitting (not relevant to a locally-installed debug build) and the pre-existing `VectorPath` note on the brand logo. Debug APK: 20,027,877 bytes. Two self-inflicted errors were caught and fixed by the compiler during this round: a `Platform declaration clash` between `ThemePreferenceState`'s generated `setMode` property setter and an explicit `fun setMode`, and stray placeholder declarations accidentally left at the end of `ExpandingNav.kt`.
- `presentation/ThemePreference.kt` was deleted and replaced by `presentation/AppPreferences.kt` (theme + motion + language + account name). The sandbox mount initially refused the delete; it was completed through the workspace's own delete-permission path rather than left as a stale duplicate that would have clashed on `ThemeMode`.

### 2026-08-16 owner round 4: emulator-verified fixes, real device screenshots for the first time

- The owner installed the android-emulator MCP this session — the first session with real, controllable device access (screenshot, tap, scroll, install/uninstall) rather than manual code review or the owner's own eyes as the only visual gate. This directly closes the structural gap named at the end of every prior round: "device/visual check still owner-only."
- Verified the MCP end-to-end before any other work, per the owner's explicit instruction to stop if it didn't work: launched `com.example.arinaagri.calculator`, screenshotted Beranda, confirmed real interaction (tap/scroll/navigate). Proceeded only after this passed.
- The owner gave eight numbered fixes (light-mode contrast, settings-card width, pill-button overuse, workflow intuitiveness vs. the web, layout quality, loading screen, black-on-red text, plus a general "make it intuitive" framing) and two follow-ups: replace the app icon/splash with the real logo, and ultimately "one-run-fix-all" the whole app to a production-ready UI/UX/business-logic bar using the web app and the owner's own Excel workbook as the reference baseline ("mimic-and-fix").

**Root causes found (diagnosed from source before editing, in every case, not guessed):**

1. **Light-mode "jomplang" contrast** — `EmptyCard`, `NoticeCard`, `ReportCard` (`components/SharedComponents.kt`) and several screen-local `Card { }`/`OutlinedCard { }` blocks across `AccountSettingsScreen.kt`, `MoreScreen.kt`, `ReportsScreen.kt`, `EntryScreen.kt` used the bare constructor with no explicit `colors`. Confirmed on-device: these rendered visibly grayer than sibling cards that did pin `colorScheme.surface` (e.g. `SimpleMetricCard`), producing exactly the "some cards white, some cards gray" inconsistency the owner flagged — most visibly on Beranda's "Belum ada catatan" empty state sitting directly under white metric tiles.
2. **Non-uniform settings/data card widths** — same root defect, different symptom: a bare `Card()` with no `Modifier.fillMaxWidth()` sizes to intrinsic content width inside a `LazyColumn` item slot. `AccountCard` happened to render full-width because an inner `OutlinedTextField(Modifier.fillMaxWidth())` incidentally forced it; `SettingCard` (Tampilan/Bahasa/Gerakan & animasi) had no such lucky accident and rendered visibly narrower. Confirmed on-device pre-fix via screenshot: the Akun card ran edge-to-edge while the three cards below it stopped well short of the margin.
3. **Pill-button overuse / menu intuitiveness** — read `arina-agri/app/dashboard/keuangan/_components/KeuanganView.tsx` directly: the web uses MUI `Tabs` for its report/sub-view switcher and `Select`/`MenuItem` for filters, never a pill-chip row for page-level navigation. The native `ReportsScreen`'s seven report views (`Ringkasan…Perbandingan`) were a `FilterChip` row — a *filter* affordance borrowed for *navigation*, exactly the "not everything should be a pill button" complaint. `ChoiceMenu` (business/project picker, category picker) already matches the web's `Select` pattern correctly and was left alone; the `FilterChip` rows in Pengaturan (Tampilan/Bahasa/Gerakan) and the ledger direction filter are genuine 2–3-option toggles/filters, which is the correct use of that widget, not a misuse.
4. **Black-on-red text** — `Theme.kt`'s `ArinaLightSemanticColors.onDanger` and `ArinaLightColors.onError` were pinned to `ErrorOnMain` (`#2C2A29`, near-black) rather than white. This rendered as black text on the solid-coral "Uang keluar" button once selected (`EntryScreen.kt`'s `PrimaryKindButton`: `container = accent = semantic.danger`, `contentColor = onDanger`).
5. **App icon/splash "kaya altered"** — `AndroidManifest.xml` still pointed `android:icon`/`android:roundIcon` at `ic_launcher_foreground.xml`, a leftover hand-drawn MVP-era placeholder (green square, white circle, orange plus) that was never replaced when the real logo (`ic_arina_logo.xml`, converted from `public/logo arina.svg` — confirmed byte-identical to the copy the owner placed at `arina-agri-calculator/public/arina-logo.svg`) was added in design-fix round 1. Separately, the splash theme's `windowSplashScreenAnimatedIcon` pointed directly at the edge-to-edge `ic_arina_logo` vector with no safe-zone padding, so Android's own icon/splash masking clipped the circuit-board branches on-screen — the owner-reported "kaya altered."

**Fixes applied:**

- `Theme.kt`: light-mode `onError`/`onDanger` changed to `Color.White`. Dark mode intentionally untouched (already owner-accepted in round 3).
- `components/SharedComponents.kt`: `EmptyCard`, `NoticeCard`, `ReportCard`, `EventCard` now explicitly pin `containerColor = MaterialTheme.colorScheme.surface` (or the outlined equivalent) and `Modifier.fillMaxWidth()`.
- Same explicit `fillMaxWidth()` + pinned `surface` color applied to every other bare-`Card()`/`OutlinedCard()` instance found via a full `grep` sweep of `presentation/**` (not just the one card the owner happened to screenshot): `AccountSettingsScreen.kt` (`AccountCard`, `SettingCard`), `MoreScreen.kt` (three section cards, `ImportCard`, `ImportBatchCard`), `ReportsScreen.kt` (`Asumsi & kelayakan` card, `ObligationCard`, monthly cash-flow card), `EntryScreen.kt` (the kind-detail form card — a second, owner-unreported instance of the same gray-card bug, found only by clicking through the flow), `BudgetScreen.kt` (`BudgetItemCard`). `components/Skeleton.kt`'s placeholder cards were deliberately left unpinned: a pulsing gray block is the correct look for a loading skeleton, not a bug.
- `ReportsScreen.kt`: the seven-page `FilterChip` row replaced with `PrimaryScrollableTabRow` + `Tab`, matching the web's own `Tabs` pattern for page-level navigation.
- New `drawable/ic_launcher_foreground_inset.xml` (an `<inset>` wrapper, 20% margin per side, around the existing `ic_arina_logo`); new `mipmap-anydpi-v26/ic_launcher.xml` + `ic_launcher_round.xml` (a proper `<adaptive-icon>`: background = `splash_background`, foreground = the inset drawable). `AndroidManifest.xml` now points `android:icon`/`android:roundIcon` at `@mipmap/ic_launcher`/`@mipmap/ic_launcher_round`. `themes.xml`'s `windowSplashScreenAnimatedIcon` switched to the same inset drawable, so the splash and the launcher icon share identical, correctly-padded artwork. The old placeholder `ic_launcher_foreground.xml` is now fully unreferenced (left in place — the `E:` mount rejects delete, the same known constraint recorded in prior rounds).

**Verified on a real emulator for the first time in this project's history** (`sdk_gphone_x86_64`, Android 11/API 30, via the android-emulator MCP):

- Launcher icon: clean, centered, correctly padded circuit-leaf mark in the app drawer — no clipping.
- Beranda empty state ("Belum ada catatan"): now white, matching the metric tiles above it.
- Pengaturan: Akun / Tampilan / Bahasa / Gerakan & animasi cards render at identical width (before/after screenshots compared directly).
- Catat → Uang keluar selected: white text on solid coral confirmed; the form card beneath (the second, owner-unreported gray-card instance) is now white with visible text-field outlines.
- Laporan: tab strip renders as real Material tabs with an underline indicator instead of a chip row; card widths uniform.
- Berkas & Data: all section cards uniform width and white.
- Dark mode: spot-checked after the fixes (Pengaturan) — still renders correctly; the light-mode-only changes did not regress the owner-accepted dark theme.

**Quality gate**, sandbox toolchain rebuilt fresh this session (the previous session's `.build-tools` did not persist across sandbox restarts — expected and already documented as a known limitation in the prior round's own writeup):
`./gradlew --init-script .build-tools/init-buildpath.gradle.kts --project-cache-dir .build-tools/project-cache testDebugUnitTest lintDebug assembleDebug --stacktrace`
Ran three times across this round (initial fixes, then again after the second gray-card instance was found and fixed, then a final pass after a `ScrollableTabRow` → `PrimaryScrollableTabRow` deprecation cleanup). Final run: **BUILD SUCCESSFUL in 1m 42s**. **24 tests, 0 failures/errors, 0 skipped.** Lint: 36 warnings, **0 errors**. `local.properties` was overwritten to the sandbox SDK path before each build and restored byte-for-byte afterward every time, confirmed via `git status --short local.properties` showing no diff.

**A session interruption happened mid-round, worth recording accurately:** the sandbox's virtiofs mount failed with an I/O error while the Android SDK download was in progress. This turned out to be caused by the owner's actual Windows C: drive being nearly full (down to 422 MB free), not a transient sandbox fluke. Root-caused with the owner's help (WizTree scan) to two `.vhdx` files inside this Claude installation's own `vm_bundles` folder (`sessiondata.vhdx`, `rootfs.vhdx`) — meaning this and prior sessions' sandbox toolchain downloads (JDK, Android SDK, Gradle caches) likely did consume real host disk space, which directly contradicts an earlier in-session claim made to the owner that the sandbox was "fully isolated" from their machine. Confirmed via web search that Claude Desktop currently has no supported way to relocate this storage off C: on Windows (MSIX-packaged app; junction/symlink workarounds are documented as breaking the app outright). The owner freed space independently (disabling hibernation, shrinking the page file, disk cleanup — all outside this repository) before work resumed. Recorded here so a future session does not repeat the "should be fully isolated" claim without evidence, and treats sandbox disk usage as a real, owner-visible cost.

**Honest scope notes — what this round does and does not close:**

- The light-mode contrast/tone inconsistency, the settings/data card-width inconsistency, the black-on-red text, and the icon/splash clipping are concretely fixed and device-verified, each with a root cause identified rather than a symptom patch.
- The pill-button-overuse and layout-intuitiveness complaints received one real, device-verified fix (the report-page tab strip) plus confirmation — by reading the web's actual component code, not by assumption — that the app's existing dropdown/toggle usage elsewhere (`ChoiceMenu`, the Pengaturan `FilterChip` rows) already matches the web's own widget choices. This was **not** a ground-up redesign pass against every screen.
- The owner's broader "mimic-and-fix against the web and the Excel baseline, one-run-fix-all, production-ready from a UI/UX-intent and business-logic standpoint" instruction is **not** claimed as met by this round. In particular: the web app (`arina-agri`) was read as source code for this round's fixes but was **not run locally side-by-side** with the emulator for a live visual/flow comparison, which the owner explicitly offered and expected ("bisa kamu run local ya"); business logic was not re-audited against the Excel workbook this round (it was verified in design round 3 and is assumed, not re-confirmed, to still hold); and Anggaran/Budget and the full Catat entry flow were reviewed from source and spot-checked but not screenshot-compared screen-by-screen against the web the way Beranda/Pengaturan/Laporan/Berkas & Data were.
- Same two gaps as every prior round, still open: Laporan and Rencana retain some hardcoded Indonesian strings (so they will not fully switch to English), and the account screen remains a non-functional preview.

## Round 5 — one modal shape, scope management where you pick the scope, dead schema removed (2026-08-28)

Branch `feat/m3-standardisation-and-a11y`. Gate: `testDebugUnitTest assembleDebug lintDebug
connectedDebugAndroidTest` — 28 unit tests green, 1 instrumented test green, lint clean.

- **Every `AlertDialog` is gone.** Nine of them (budget item, categories and units, backup, restore
  preview, settlement, assumptions, rename, import-batch reverse, transaction reverse) now use one
  `presentation/components/ArinaSheet.kt`. See D-023.
- **Scope management moved into a sheet** (`ManageScopeSheet`), opened from the scope header itself
  and from Berkas dan Data; the `Overlay.SCOPE` route is deleted. See D-024. Device-verified: add
  usaha → add lahan → both appear in the header pickers immediately, and the sheet grows smoothly
  when the "Lahan pada …" section appears.
- **Dead schema deleted**: `saved_mappings` and `projects.status` / `templateKind` /
  `templateMetadata`. Database version 3, schema `3.json` exported, backup table list updated. D-026.
- **Dead sign-in buttons deleted** from the account card; the explanatory notice stays. D-027.
- **Two display bugs found by looking at the running app**, both fixed in the shared component
  rather than at one call site: `ReportRow` let a long label push "Rp1.150.000" onto three lines, and
  `ComparisonRow` coloured any positive delta green — so spending Rp1.150.000 over plan read as
  good news. `ComparisonRow` now takes `higherIsBetter`.
- **Entity mapping question answered** in `docs/DOMAIN_MODEL.md`: web `Proyek` ≈ Android `Lahan`;
  `Usaha` is the extra level Android adds under D-005.

Known gaps left open on purpose:

- Seeded category names stay Indonesian in the English UI. They are user data rows created when the
  usaha is created, not UI strings; translating stored records would rewrite the user's own ledger.
- The Android file-picker import path still needs one manual pass on real hardware (see the parity
  audit's verification notes); it is covered by automated tests, not by a manual click.


## Round 6 — one seed, one colour system (2026-08-28)

- The palette is generated, not transcribed: `docs/evidence/theme/generate_palette.py` runs Material
  Color Utilities over the brand green and prints both the Kotlin tokens and the WCAG audit. 96
  pairs measured, 0 failures. D-028.
- Money roles (income, expense, plan) and the six chart hues are harmonised toward the seed rather
  than borrowed from Material 2. `error` is Material's red again and means an error. D-029.
- Cards, sheets and the navigation bar moved onto surface-container tones; shadows are light-mode
  only, checked with `isDarkScheme()`. D-030.
- Sheets gained a half-expanded snap point, pinned actions and `imePadding()`. D-031.
- Fixed on the way: the Beranda hero drew white text on the gradient, which in dark mode sat at
  about 2:1 because dark `primary`/`tertiary` are light tones. Dark mode now fills with the
  container tones and writes with `onPrimaryContainer`.

Device-verified on the emulator in both themes: Beranda, the scope sheet at both snap points,
Pengaturan, and the entry form.

## Round 7 — the Padi 1 Ha case, end to end on device (2026-08-29)

Full manual pass from a cleared install: tour, manual scope creation with a real keyboard, import of
the reference workbook through the Android file picker, reports, both exports, batch reversal.

- **Every figure matches the workbook**: income 45.500.000, expense 22.159.000, profit 23.341.000,
  operating cash 23.341.000, 38 rows including the recovered land rent.
- **Imported rows are now categorised** (D-034) and the breakdown matches the workbook's own
  profit-and-loss grouping — Jasa Alsintan 8.150.000, Sewa Lahan 7.000.000, Tenaga Kerja 3.360.000,
  and the rest.
- **Gboard is stable** after the machine restart, which also confirmed the round-6 `imePadding()`
  work against a real keyboard rather than the voice IME.
- **The file picker is finally verified manually.** DocumentsUI ignores synthetic taps on list rows
  but responds to `DPAD_DOWN` + `ENTER`; the round-4 verification note is closed.
- **Excel and PDF exports were opened and read**, not merely produced: 10 sheets with the canonical
  marker and correct per-category rows, and a 4-page PDF containing both books and all 38 rows.
- **Two defects found by looking, not by testing**: wage rows were being absorbed into ingredient
  categories, and the cost chart kept showing reversed spending. Both fixed (D-034, D-035).
- **A silently skipped test** was found and fixed: the new test's fixture path was one level short,
  so `assumeTrue` skipped it and the build stayed green while the classifier was wrong. All 35 tests
  now run with zero skips.

Support address set to `main@arinahub.com` (D-032, closes O-012). Firebase Authentication chosen as
the identity layer with Drive holding only encrypted backup blobs (D-033); design in
`docs/CLOUD_SYNC_AND_AUTH.md`, nothing wired because the package identifier and signing certificate
do not exist yet.

## Round 8 — White-label multi-sector foundation and vocabulary abstraction (2026-09-05)

- Abstracted hardcoded agricultural vocabulary and master data into a decoupled multi-sector architecture (D-048).
- Introduced `BusinessTemplate` with 4 sector configurations: `GENERAL_MSME` (retail/dagang), `CULINARY_FB` (food & beverage), `SERVICES` (jasa & servis), and `AGRICULTURE` (pertanian & agribisnis).
- Extracted `BusinessWords` translating accounting operations into intuitive terms, while maintaining `FarmerWords` as a transparent backwards-compatible delegation layer.
- Enhanced `CategoryClassifier` keywords to support common MSME expenses (salaries, courier/shipping, packaging, ATK) and revenues (orders, catering, repair services) without disturbing agricultural keyword priority.
- Added sector selection in `ManageScopeScreen` when creating new business entities.
- Added comprehensive unit tests in `BusinessTemplateTest`.
- Quality verification: all 17 unit test suites (`testDebugUnitTest`, including golden workbook tests) passed 100% with zero regressions.

## Next safe action

1. **Run the web app locally** (`arina-agri`, `npm run dev` or equivalent) and use it as a live, side-by-side reference — not just source-code reading — for a full screen-by-screen pass: Beranda/dashboard, Catat/transaction entry, Anggaran/RAB, Laporan/reports, and account/settings. This is the piece of "mimic-and-fix" this round did not complete, and the owner named it explicitly.
2. Re-audit business logic and vocabulary against `arina-agri/references/CATATAN KEUANGAN PADI 1 Ha ADE.xlsx` one more time now that the presentation layer has changed substantially since round 3's audit, to confirm the "production-ready business logic" bar before declaring it met.
3. Two known gaps that should not be mistaken for finished: Laporan and Rencana still have hardcoded Indonesian strings (so they will not switch to English), and the account screen is a non-functional preview.
4. Owner visual sign-off on this round's specific fixes (light-mode consistency, card widths, tabs, icon/splash) is still worth a quick pass before the next larger redesign push, even though the code/test/lint/device gate is green.
5. After that, continue unfamiliar-workbook mapping UX, budget-link resolution, and production-safe Room migrations per the roadmap.

## Recovery after interruption

1. Open this file and read `README.md`, `AGENTS.md`, and the required project documents listed there.
2. Run `git status --short` in the calculator, parent, web-evidence, and fixture repositories. Do not overwrite unexpected user changes.
3. Confirm the calculator branch/commit against the latest commit recorded in this file and `git log -5 --oneline`.
4. Resume from **Next safe action**, not from chat memory.
5. After the next material milestone, update the timestamp, phase table, findings, files, verification log, risks, and next safe action here.
6. Commit and push a coherent checkpoint to the private calculator repository; then update and push the parent repository pointer/documentation when applicable.
