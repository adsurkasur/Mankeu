# Arsitektur Sistem Mankeu

Aplikasi native Android yang **offline-first**: semua data keuangan tinggal di Room SQLite **schema 12** di perangkat. Firebase Auth dan Google Drive hanya dipakai untuk cadangan terenkripsi opsional dan sinkronisasi antar-HP terenkripsi (DEC-013, DEC-024).

## 1. Lapisan dan dependensi

Dependensi dirakit secara manual di `MankeuApplication` (pola *manual dependency injection* yang direkomendasikan Android untuk satu grafik kecil) dan diteruskan ke `AppViewModel` lewat `viewModelFactory { initializer { } }` dari Jetpack Lifecycle.

```mermaid
flowchart TB
    subgraph wiring ["Perakitan aplikasi"]
        appContainer["MankeuApplication<br/>kontainer lazy"]
        mainActivity["MainActivity<br/>viewModelFactory dan initializer"]
    end

    subgraph presentation ["Presentasi: Jetpack Compose dan Material 3"]
        shell["MankeuApp<br/>collectAsStateWithLifecycle"]
        screens["Beranda, Catatan, Stok, Laporan<br/>Overlay Data, Pengaturan, Pusat Bantuan"]
        vicoChart["CashFlowChart<br/>Vico 3.2.3"]
    end

    subgraph stateLayer ["State"]
        viewModel["AppViewModel<br/>uiState StateFlow AppUiState<br/>isSaving dan mutating"]
    end

    subgraph domainLayer ["Domain: Kotlin murni, tanpa androidx"]
        ledgerPolicy["LedgerPolicy<br/>journalFor, jurnal seimbang"]
        calculators["PostingReportCalculator<br/>MonthlyCashFlowCalculator<br/>PlanCalculator, ActualBepCalculator<br/>StockCalculation"]
        txCalc["TransactionCalculation<br/>kuantitas BigDecimal, rupiah Long"]
    end

    subgraph dataLayer ["Data: Room"]
        repository["FinanceRepository<br/>satu corong semua penulisan<br/>recordEvent, updateEvent, deleteEvent<br/>commitImport, undoImportBatch"]
        dao["FinanceDao<br/>proyeksi sempit dan agregasi SQL"]
        database[("AppDatabase schema 12")]
    end

    subgraph ioLayer ["Subsistem I/O"]
        importer["FinanceWorkbookImporter<br/>hanya templat Mankeu, DEC-012<br/>XlsxWorkbookReader: XXE mati, ZIP terbatas"]
        exporters["FinanceWorkbookExporter<br/>templat kosong sama dengan ekspor<br/>FinancePdfExporter"]
        backupManager["EncryptedBackupManager<br/>PBKDF2-HMAC-SHA256 dan AES-256-GCM"]
        driveClient["GoogleDriveBackupClient<br/>HttpURLConnection dan org.json"]
        authManager["FirebaseAuthManager"]
        syncFramework["SyncController, SyncEngine, SyncCoordinator<br/>trigger ke sync_outbox saat sync menyala<br/>otomatis + tombol (DEC-024)"]
    end

    appContainer --> mainActivity
    mainActivity --> viewModel
    shell --> screens
    screens --> vicoChart
    shell --> viewModel
    viewModel --> repository
    viewModel --> importer
    viewModel --> backupManager
    viewModel --> driveClient
    viewModel --> authManager
    viewModel --> calculators
    repository --> ledgerPolicy
    repository --> calculators
    repository --> txCalc
    repository --> exporters
    repository --> dao
    dao --> database
    backupManager --> database
    syncFramework --> database
    syncFramework -.-> driveClient
```

## 2. Pohon komponen Compose

```mermaid
flowchart TD
    setContent["MainActivity.setContent"] --> mankeuApp["MankeuApp"]
    mankeuApp --> mankeuTheme["MankeuTheme<br/>token warna tanpa animasi"]
    mankeuTheme --> overlayContent["AnimatedContent overlay"]
    overlayContent -->|"DATA"| dataOverlay["OverlayScaffold<br/>DataScreen: impor, Batalkan impor,<br/>ekspor, cadangan, kategori"]
    overlayContent -->|"SETTINGS"| settingsOverlay["OverlayScaffold<br/>SettingsScreen"]
    overlayContent -->|"HELP"| helpOverlay["OverlayScaffold<br/>HelpCenter"]
    overlayContent -->|"NONE"| widthCheck{"Lebar layar 840dp atau lebih?"}
    widthCheck -->|"ya"| appRail["AppRail"]
    widthCheck -->|"ya"| mainScaffoldWide["MainScaffold"]
    widthCheck -->|"tidak"| mainScaffold["MainScaffold<br/>NavigationBar 4 destinasi<br/>RecordSpeedDial di Beranda dan Catatan"]
    mainScaffold --> scopeHeader["ScopeHeader<br/>ScopeSummary dan tombol mode buku"]
    mainScaffold --> screenContent["ScreenContent"]
    mainScaffoldWide --> screenContent
    screenContent --> homeScreen["HomeScreen"]
    screenContent --> entryScreen["EntryScreen di Realisasi<br/>formulir, cari, saring Semua Masuk Keluar"]
    screenContent --> planScreen["PlanScreen di Rencana<br/>produk, biaya, periode"]
    screenContent --> stockScreen["StockScreen<br/>barang unit dan bersama"]
    screenContent --> reportsScreen["ReportsScreen<br/>Laba Rugi dan Arus Kas, Kelayakan dan BEP,<br/>Uang vs rencana"]
    homeScreen --> metricCards["SimpleMetricCard<br/>read-out tanpa klik"]
    homeScreen --> planCards["PlanFeasibilityCard<br/>ActualMonthCard, StockCard"]
    entryScreen --> ledgerCard["EventCard<br/>Ubah dan Hapus, 48dp"]
    reportsScreen --> monthlyChart["MonthlyCashFlowChart"]
    mankeuApp --> scopeSheet["ManageScopeSheet"]
    mankeuApp --> restoreDialog["RestorePreviewDialog"]
    mankeuApp --> spotlightTour["SpotlightTour"]
    mankeuApp --> feedbackProvider["CompositionLocal LocalFeedback<br/>MankeuFeedback"]

    subgraph tooltipLayer ["MankeuTooltip: TooltipBox dan PlainTooltip"]
        tooltipAnchor["Anchor tekan lama"]
    end
    scopeHeader --> tooltipAnchor
    ledgerCard --> tooltipAnchor
    metricCards --> tooltipAnchor
    planCards --> tooltipAnchor
    tooltipAnchor -.->|"LONG_PRESS"| feedbackProvider
```

## 3. Aliran data reaktif

```mermaid
flowchart LR
    roomTables[("Tabel Room")] -->|"invalidation"| daoFlows["Flow FinanceDao<br/>observeEventsForBook LIMIT<br/>observeCashFlowRows<br/>observeCategoryTotals<br/>observePostingRows"]
    daoFlows --> repoFlows["FinanceRepository<br/>pemetaan ke model domain"]

    subgraph selectionChain ["Rantai pemilihan: SharingStarted.Eagerly"]
        selBusinesses["businesses"] --> selProjects["projects"]
        selProjects --> selBooks["books"]
        selBooks --> selBookId["selectedBookId"]
    end

    subgraph bookQueries ["Kueri per buku: WhileSubscribed 5000 ms"]
        perBook["events, eventCount, cashFlowRecords,<br/>categoryTotals, budgetItems, products,<br/>stockItems, summary, importBatches"]
    end

    repoFlows --> selBusinesses
    selBookId -->|"flatMapLatest"| perBook
    repoFlows --> perBook
    perBook --> combined["combine bertingkat"]
    selBookId --> combined
    combined --> uiState["uiState: StateFlow AppUiState"]
    uiState -->|"collectAsStateWithLifecycle"| composeUi["Compose UI"]
    composeUi -->|"aksi pengguna"| mutatingGuard["mutating<br/>isSaving compareAndSet"]
    mutatingGuard --> repoWrite["FinanceRepository<br/>withTransaction"]
    repoWrite --> roomTables
```

## Prinsip desain

1. **Satu sumber kebenaran.** UI hanya membaca `AppUiState`. Setiap penulisan melewati `AppViewModel.mutating` lalu `FinanceRepository`, dan setiap aturan bisnis ditegakkan di repository di dalam transaksi yang sama dengan penulisannya.
2. **Domain murni.** Kalkulator dan `LedgerPolicy` tidak mengimpor paket Android sehingga seluruhnya diuji dengan unit test JVM.
3. **Uang presisi.** Rupiah disimpan sebagai `Long` bulat. Rasio, margin, kuantitas, dan stok memakai `BigDecimal` dengan `RoundingMode.HALF_UP`.
4. **Primitif standar.** `org.json` bawaan Android untuk JSON Drive, `viewModelFactory` dari Jetpack, parser DOM platform untuk XLSX, dan `NumberFormat` yang di-cache per thread (D-082). Hilt, Apache POI, DataStore, dan AppCompat dipertimbangkan lalu ditolak dengan alasan di D-082.
5. **Cadangan tanpa server.** Berkas `.aabk` dienkripsi AES-256-GCM dengan kunci PBKDF2-HMAC-SHA256 310.000 iterasi. Drive hanya menyimpan ciphertext di `appDataFolder`.
