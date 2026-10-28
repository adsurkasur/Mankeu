# Changelog

All notable changes to this project will be documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]
### Changed
- Products now have a base unit, sale units with their own price and a per-batch recipe (database schema 13, DEC-028). Existing products and schema-12 backups are converted automatically: each recipe line "Rp X for N units" becomes Rp X ÷ N per unit, rounded to the rupiah, so a product's variable cost can move by Rp1.

### Fixed
- Bottom sheets no longer close when you scroll a long form, and the keyboard should no longer make them bounce (checked on the emulator only).

### Removed
- Spotlight tours (the help center and the ? help sheet stay).
- Firebase Auth: sign-in uses the Google account directly.
- Support for data and backups from before 0.1.0 (schema 12 is the minimum; older backups are refused).

## [0.1.0] - 2026-10-27
### Added
- **Sinkronisasi antar-HP** in Pengaturan (DEC-024): phones signed in with the same Google account share one set of books through the app's Drive folder, end-to-end encrypted with a sync passphrase typed once per phone (kept by the Android Keystore). Runs when the app opens, a few seconds after a change, and from "Sinkronkan sekarang"; categories, stock items and products created with the same name on two phones are merged and reported on Beranda. Help article and user guide section 9.6.
- Beranda explains a negative balance ("Uang kas minus?") and offers "Catat uang awal", which opens money in with Setoran Modal Pemilik selected; a negative cash profit gets one line on why restocking days look like a loss.
- Import preview names where the rows go ("Masuk ke: usaha · unit"); saving shows "Menyimpan N baris…".
- **Hapus semua data** in Pengaturan: deletes every business, record, plan, stock item, import history, the automatic recovery copy and the local name on this phone after typing HAPUS; when signed in with Google it can also delete every Mankeu file on Google Drive, the sign-in account, and the Drive permission (DEC-020).
- **Ubah** on every record card: the entry form opens filled in and saving corrects the record in place (DEC-019).
- User guide for small-business owners (`docs/panduan/PANDUAN_PENGGUNAAN_MANKEU.md`): step-by-step use of every feature with 47 screenshots generated from the real app, quick and full reading paths, appendices for seasonal farming and multi-branch stock.
- Pusat Bantuan: 27 task-based articles (ID/EN) with search, numbered steps, a screenshot and "Tunjukkan di aplikasi", which opens the right page and highlights the place. The ? button now opens a help sheet for the current page (DEC-018).
- Page guides for every page that may confuse (Beranda, Catatan, form catat, Rencana Usaha, Stok, Laporan, Berkas & Data, Usaha/Unit, Pengaturan), rewritten for the current app, short, with "Pelajari lebih lanjut". They are offered with a small card instead of starting on their own; "Jangan tawarkan lagi" and a switch in Pengaturan turn offers off.
- Empty screens link to the matching help article ("Pelajari caranya").
- Seasonal products: "Mulai dijual bulan ke-" on each product (template column "Mulai Dijual (bulan ke-)", template v4; v3 files still import). Months before the start have no sales, so capital need includes the waiting months; the plan card, PDF and Excel say when figures are period averages and BEP is per selling month (DEC-017).
- Stock per unit: a new item belongs to the selected unit or, with "Semua unit", to every unit of the business; shared items are labelled. Imported stock goes to the selected unit (DEC-017).
- The app reopens the usaha and unit that were open last.
- Import history shows money in and money out separately; export file names include the unit (`mankeu-<unit>-<tanggal>.xlsx`, `laporan-keuangan-<unit>-<tanggal>.pdf`).
- Plan card shows capital needed, planned capital and what is still missing; breakdown shows the break-even base when it differs from fixed cost.
- The Excel template carries the plan period ("Periode rencana" on Petunjuk); importing it fills a unit that has no period yet.
- **Rencana Usaha** page (Catatan in Proyeksi mode, DEC-014): plan period, products with cost recipes (per batch with yield), monthly fixed costs, other one-off costs and income, assets with useful life, and capital contributions. Break-even, profit per month, R/C, B/C and capital need update as you type.
- Multi-product break-even by sales mix, with BEP per product and a warning for a product sold below its variable cost.
- Laporan → Kelayakan & BEP: itemised monthly breakdown in the lecturer's format (each fixed cost, one-off costs averaged, depreciation, variable cost, total cost, revenue, profit, R/C, B/C, unit economics per product).
- "Status BEP Bulan Ini" on Beranda and Laporan in Realisasi: revenue against this month's break-even, units sold per product, profit, and ingredient spend against the recipe.
- Uang masuk can name a product; unit and price fill in and the amount follows quantity × price. Uang keluar can override the category's cost nature. Asset purchases take a useful life (default 36 months).
- Kelola kategori: choose Tetap/Variabel for each expense category.
- Template v3: sheets Produk and Resep Produk; Transaksi gains Produk, Jumlah, Sifat Biaya, Umur Pakai; Rencana Anggaran gains Berulang, Sifat Biaya, Umur Pakai.
### Changed
- Database schema 12: changes are queued for sync only while sync is on; the queue every phone had been filling without use is emptied on upgrade. "Hapus semua data" also turns sync off and forgets its passphrase.
- A new business starts with one unit, "Utama", already selected; the first business closes the sheet so recording can start at once. With no business yet, the sheet opens straight on its form (DEC-022).
- One welcome card on Beranda at first launch instead of a guide offer on every page and sheet; every page's tour stays behind the ? button (DEC-022).
- The description is optional; a blank one becomes the category name. The entry form preselects the category last used.
- "Proyeksi" is now "Rencana", and the Rencana/Realisasi switch appears once a unit exists. Laporan says "Untung / Rugi kas" like Beranda.
- Version 0.1.0; release builds are minified with R8 (DEC-023).
### Fixed
- Sync: a record saved while a round was uploading could cancel it and make other phones skip later changes; a dropped connection while switching sync on was reported as a wrong passphrase; a failed first round turned sync off and forgot the passphrase. A phone with a different passphrase now says so instead of failing vaguely.
- After recording opening money (or buying equipment) the next entry preselected that category, so a sale could be booked as capital in one tap; only sales and cost categories are preselected now.
- Choosing a second Excel file while the first was still being read could replace the new preview with an error.
- A Keterangan cell left empty in the Excel template is accepted and takes the category name, like in the entry form.
- A business and its first unit are created together or not at all; restoring a backup starts a sync round.
- The "Tampilkan sapaan panduan" switch shows whether the welcome will appear and brings it back with one tap; "Uang kas proyeksi" read "Uang kas rencana".
- Play Store screenshots showed removed screens and were taller than Play accepts (2.22:1); seven new 1:2 screenshots are made from the current app by `scripts/make_store_screenshots.py`.
- "Tambah Usaha" (and the other empty-state buttons for a business or unit) opened Berkas dan Data instead of the business form.
- Catat Pemasukan preselected "Pendapatan Lain", so sales of a new owner were recorded as other income and the break-even status was wrong.
- The import "Batal" buttons did not cancel; reading now stops for real and saving no longer offers a cancel it cannot honour.
- Beranda summary and monthly cash flow loaded every posting of the book on each change; both are now summed by the database (9k-row book: 556 ms → 29 ms).
- Cost-composition percentages used an English decimal point; the search hint still mentioned people's names.
- Texts that still named removed features now describe the app as it is: the Proyeksi tooltip ("rencana usaha, produk, dan biaya" instead of "asumsi"), the financing note in Laporan, and unit errors ("Unit tidak ditemukan", "Nama unit wajib diisi" instead of "lahan").
- "Balik Transaksi" is gone: a wrong record is fixed with **Ubah** or removed with **Hapus**. On upgrade (schema 11), records that were reversed earlier are deleted together with their reversals; cash and reports do not change, and a notice says how many went. Old backups are cleaned the same way on restore (DEC-019).
- "Balik batch" is now **Batalkan impor**: undoing an import deletes the records it added; the import stays in the history as "dibatalkan".
- The four-number BEP calculator, "Saldo kas awal" and "Modal sendiri" are gone. Existing calculator values are converted on upgrade into a product, a monthly fixed cost and a capital line, with a notice; opening cash is named in the notice to be recorded as Setoran Modal if real.
- Proyeksi reports (Beranda, Laporan, comparison) are computed from the business plan. The record button is hidden in Proyeksi.
### Fixed
- Guides no longer describe removed features (Riwayat tab, "Belum Lunas", "Ubah Asumsi"); owners who saw the old guides are offered the updated ones once.
- A guide step pointing at something further down a page now scrolls to it instead of highlighting nothing.
- Reversing an import also clears the plan period that import filled in (unless it was changed since), and says so.
- Import counts (preview button, result message, batch history) count only rows actually added, not stock or products skipped because the name exists.
- Editing a product no longer resets its selling start month; deleting a unit that holds its own stock archives it instead of orphaning the stock.
- Break-even now includes variable costs outside recipes and other income, so profit is zero at break-even (was a loss for seasonal costs such as seed and fertiliser).
- Multi-product break-even revenue is exact to the rupiah (was off by a few rupiah).
- Realisasi vs Target compares averages per month instead of a 12-month plan against one recorded month; the meaningless RAB total row is gone.
- A month with no sales shows "Belum Dapat Dinilai" instead of "Tidak Layak".
- Import notices show only on the unit they belong to; an open import preview is cleared when the unit changes.
- Excel and PDF exports report Proyeksi from the business plan (was Rp0), write counts and ratios as numbers / Indonesian decimals, format negative rupiah as "-Rp…", and use local time.
- Labels say which profit is cash and which is after depreciation; import preview says "uang masuk/keluar" instead of "pendapatan".
- Upgrading straight from schema 7 to 9 no longer fails creating sync triggers for tables that do not exist yet (DEC-015).
- CI runs the instrumented test suite (migrations, import commit, ledger integrity, sync) on an Android emulator for every push and pull request.
- Single Mankeu Excel template for import and export (DEC-012): "Unduh template Mankeu", strict reading with exact sheet/cell errors, import preview with new categories, skipped stock items and a duplicate-file warning.
- Owner capital, owner drawings (prive) and equipment purchases can now be recorded, chosen by category (DEC-011).
- Basic default categories (11) for new businesses.
- Multi-device sync framework (DEC-013): change capture, encrypted change sets, apply with last-writer-wins, Drive transport. Not yet switched on in the UI.
- New **Stok** bottom-nav tab (Beranda · Catatan · Stok · Laporan): inventory value, item count, low-stock alert, add item, and record stock in / out / opname per item. Beranda shows a compact stock summary card linking to the tab.
- PDF export: "Titik Impas (BEP) & Kelayakan Usaha" subsection per book (BEP unit, BEP omzet, R/C, B/C, LAYAK / BELUM LAYAK).
- Excel export: "Anggaran Proyeksi/Realisasi" sheets are written again, so RAB lines survive an export → import round trip.
- English string `reports_rc_ratio`.
- Explicit dual BEP presentation: **BEP Volume (Unit)** and **BEP Nilai (Uang / Nominal Rupiah)** side-by-side metric tiles in `FeasibilityCard` and `ReportsScreen`.
- Dedicated **Kalkulator BEP & Parameter** dialog with real-time **Live Calculation Preview Card** showing dynamic BEP Unit, BEP Rupiah, and Contribution Margin % as inputs change.
- Automated unit test suite additions in `BusinessAnalysisTest` covering BEP calculations without target quantity, loss-making unit margins, and fixed cost fallbacks.
- Consolidate navigation to 3 clean tabs (`Beranda`, `Catatan`, `Laporan`), eliminating the redundant `Riwayat` tab.
- Integrated full, searchable & filterable transaction ledger directly inside `EntryScreen` (Catatan) with search bar, direction filter chips (`Semua`, `Uang Masuk`, `Uang Keluar`), and empty state feedback card (`EmptyCard`) with immediate `[Catat Sekarang]` CTA.
- Added `SingleChoiceSegmentedButtonRow` directly at the top of the entry `ModalBottomSheet` to toggle between `Uang Masuk` and `Uang Keluar` on the fly.
- Interactive foreground launch task via Windows Task Scheduler (`schtasks /it`), established as the permanent default mechanism to prevent sandbox isolation issues.

### Fixed
- Businesses created before the basic categories now get Setoran Modal / Prive / Aset categories, and an imported basic category name keeps its meaning (prive is no longer imported as an expense).
- BEP in rupiah is computed exactly (FC × P ÷ (P − VC)); it was off by a few rupiah from rounding the volume first.
- On-screen texts no longer mention debts, receivables, loans or credit sales; 116 unused strings removed.
- The transaction form's Batal/Simpan buttons stay visible above the keyboard; before, an open keyboard made saving impossible.
- Profit and net cash no longer disagree, and Biaya Operasional now equals Komposisi Biaya: credit/loan rows that caused both are gone.
- Restoring a backup made on v0.0.4–v0.0.6 (schemas 3–5) was refused because the stock tables were missing.
- `LedgerIntegrityTest` (androidTest) no longer references removed obligation APIs; `compileDebugAndroidTestSources` builds again.

### Changed
- Stock opname may record a physical count of zero; stock movements and new items are saved atomically.
- Renamed Tab 2 in `ReportsScreen` to **"Kelayakan & BEP"** (`reports_tab_feasibility`) and action button to `[ 🧮 Kalkulator & Parameter BEP ]`.
- Decoupled BEP display from `targetQuantity > 0`, rendering BEP Unit and BEP Rupiah whenever unit price, variable cost, and fixed cost are available.
- `Destination.ENTRY` bottom navigation icon changed from `+` (`Icons.Default.Add`) to `Icons.AutoMirrored.Filled.ReceiptLong` (receipt/ledger).
- Removed giant static buttons for "Uang Masuk" and "Uang Keluar" and the "Jenis lain" dropdown from `EntryScreen`, strictly enforcing the pure cash-only model.
- Eradicated redundant 4 summary metric cards from `FinancialsSection` in `ReportsScreen.kt`. Laporan now immediately displays formal accounting statements (Income Statement, Cash Flow, and Monthly Cash Flow Chart) without echoing Beranda's cards.
- Interactive `HeroCard` on `HomeScreen`: clicking the top cash balance navigates directly to Cash Flow Reports (`Destination.REPORTS`).
- Embedded recent transactions history in `EntryScreen` (`state.events.take(5)`), enabling immediate confirmation and undo/reverse actions right below input buttons.
- Inline custom category creation directly in `EntryScreen`'s category picker (`ChoiceMenu`), allowing on-the-fly creation of user-defined operational expense categories reflected in `ExpenseDistributionCard`.
- R/C Ratio ($TR / TC$) metric card alongside Net B/C Ratio in `FeasibilityCard` and `ReportsScreen`, with dynamic fallback to recorded projection revenue when assumption volume and price are omitted.
- Standard CVP (Cost-Volume-Profit) calculations in `BusinessAnalysisCalculator.calculateCvp`:
  - Unit Contribution Margin ($CM = P - VC$)
  - Contribution Margin Ratio ($CMR = CM / P$)
  - BEP Unit ($FC / CM$) and BEP Rupiah ($FC / CMR$)
  - Revenue-to-Cost Ratio ($R/C = TR / TC$)
  - Benefit-Cost Ratio ($B/C = NetProfit / TC$)
- Minimal Stock Management domain models, calculations, and Room schema (`StockManagement.kt`):
  - `StockItemEntity`, `StockMovementEntity`, and `StockDao`
  - `RecordStockMovementUseCase` (Stok Awal + Masuk - Keluar & Opname)
  - `GetStockSummaryUseCase` (Low stock alert & total inventory valuation in exact rupiah)
- Automated unit test suite verifying real micro-MSME benchmark (Kedai Kopi Berkah: exact BEP 400 units).
- Room database schema version 7 with `MIGRATION_6_7` enforcing pure Cash-Only architecture.

### Changed
- `FeasibilityCard.kt`: Fixed feasibility threshold bug where B/C ratio was checked against R/C ratio threshold ($\ge 1.0$), correctly assessing R/C ratio $\ge 1.0$ and Net B/C ratio $\ge 0.0$.
- `SharedComponents.kt`: Updated transaction reversal icon from `Redo` to `Icons.AutoMirrored.Filled.Undo`.
- `BusinessAnalysisCalculator.calculate` now supports explicit `fixedCostRupiah` and `variableCostPerUnitRupiah` for genuine CVP break-even analysis while preserving backward compatibility for legacy imports.
- `HomeScreen.kt` and `ReportsScreen.kt` re-anchored to 4 primary pure cash metrics: Operating Cash Flow, Net Profit, Cash Income, Cash Expense.
- `EntryScreen.kt` simplified to 5 pure cash transaction types: `CASH_INCOME`, `CASH_EXPENSE`, `OWNER_CAPITAL`, `OWNER_DRAWING`, and `ASSET_PURCHASE_CASH`.
- `EncryptedBackupManager` updated to backup `stock_items` and `stock_movements` while dropping obligation tables.

### Removed
- All debt, receivable, loan, instalment and interest concepts (types, accounts, categories, assumption field, form fields). Schema 8 deletes existing non-cash rows and leaves a notice that lists them (DEC-011).
- Reading arbitrary workbooks: fuzzy column matching, the five-sheet reference format and automatic enrichment (DEC-012).
- Dead obligation stubs: `ObligationView`, `FinanceRepository.obligations()/obligationBalances()/settleObligation()`, `AppViewModel.settleObligation`, `ReportsScreen(onSettle)`, `msg_settlement_*` strings.
- Removed redundant recent transaction records preview (`home_recent`, `EmptyCard`, `EventCard` list, and `Lihat Semua Catatan` button) from `HomeScreen.kt`, leaving transaction history exclusively to `EntryScreen` (Catatan).
- Removed dead "UNPAID" filter chip from `LedgerScreen.kt`.

### Removed
- Fully purged all debt, loan, interest, installment, and credit/obligation concepts:
  - Dropped database tables `obligations` and `settlements` (`MIGRATION_6_7`).
  - Removed `ObligationEntity`, `SettlementEntity`, and all DAO queries to obligation tables.
  - Removed due date reminder cards and debt settlement dialogs (`SettlementDialog`) from presentation layer.
  - Purged loan amount and interest calculation fields (`loanAmountRupiah`, `interestRateBasisPoints`, `loanDisbursementEpochDay`, `loanPaymentEpochDay`) from domain, Room entities, repository, PDF and Excel exporters, and UI screens.

### Added (Initial)
- Modular logging protocol (`logs/` directory with activity, decision, and changelog files).
- Mandatory `AGENTS.md` guidelines for AI agents and contributors.