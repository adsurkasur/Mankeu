# Class Diagram - Domain, Data, dan Presentasi Mankeu

Nama kelas, properti, dan metode di bawah ini sama dengan kode sumber pada Room **schema 12**. Hanya anggota yang relevan bagi alur bisnis yang ditampilkan.

```mermaid
classDiagram
    direction LR

    class BookMode {
        <<enumeration>>
        PROJECTION
        REALIZATION
    }
    class FinancialEventType {
        <<enumeration>>
        CASH_INCOME
        CASH_EXPENSE
        OWNER_CAPITAL
        OWNER_DRAWING
        ASSET_PURCHASE_CASH
    }
    class CategoryRole {
        <<enumeration>>
        REVENUE
        EXPENSE
        CAPITAL_IN
        DRAWING
        ASSET
        +cashTypeFor(incoming) FinancialEventType
    }
    class CashFlowRole {
        <<enumeration>>
        OPERATING
        INVESTING
        FINANCING
    }

    class MoneyEvent {
        +String id
        +String bookId
        +FinancialEventType type
        +Long amountRupiah
        +LocalDate date
        +String description
        +String categoryId
        +String productId
        +String importBatchId
        +CashFlowRole cashFlowRole
        +CostNature costNature
    }
    class JournalEntry {
        +String id
        +String sourceEventId
        +List~Posting~ postings
        +requireBalanced() JournalEntry
    }
    class Posting {
        +LedgerAccount account
        +PostingSide side
        +Long amountRupiah
    }
    class CashFlowRecord {
        +String id
        +LocalDate date
        +FinancialEventType type
        +CashFlowRole cashFlowRole
        +Long amountRupiah
    }
    class FinancialSummary {
        +Long operatingIncomeRupiah
        +Long operatingExpenseRupiah
        +Long profitRupiah
        +Long cashBalanceRupiah
    }
    class PlanProduct {
        +String name
        +Long priceRupiah
        +BigDecimal targetPerMonth
        +List~CostComponent~ components
        +Int sellFromMonth
        +BigDecimal contributionMargin
    }
    class CostComponent {
        +String name
        +Long amountRupiah
        +BigDecimal yield
        +BigDecimal perUnit
    }
    class PlanAnalysis {
        +List~ProductBep~ products
        +BigDecimal breakEvenUnits
        +Long breakEvenRupiah
        +Long profitRupiah
        +Long breakEvenBaseRupiah
    }
    class StockItem {
        +String name
        +BigDecimal currentStock
        +BigDecimal minAlertStock
        +Boolean shared
        +Boolean isLowStock
    }

    class LedgerPolicy {
        <<object>>
        +journalFor(event) JournalEntry
    }
    class PostingReportCalculator {
        <<object>>
        +calculate(records) FinancialSummary
    }
    class MonthlyCashFlowCalculator {
        <<object>>
        +calculateFromRecords(records, openingCash) List~MonthlyCashFlow~
    }
    class PlanCalculator {
        <<object>>
        +analyse(inputs) PlanAnalysis
        +schedule(months, inputs) List~MonthlyCashFlow~
    }
    class ActualBepCalculator {
        <<object>>
        +analyse(products, actual) ActualMonthAnalysis
    }
    class StockCalculation {
        <<object>>
        +summarize(items) StockSummary
    }

    class FinanceRepository {
        +recordEvent(draft) String
        +updateEvent(eventId, draft)
        +deleteEvent(eventId)
        +createCategory(businessId, name, role, aliases) String
        +saveProduct(...)
        +createBudgetItem(draft) String
        +createStockItem(businessId, projectId, shared, ...) String
        +recordStockMovement(stockItemId, type, quantity, date, notes) BigDecimal
        +reviewImport(businessId, projectId, preview) ImportReview
        +commitImport(businessId, projectId, preview) Int
        +undoImportBatch(batchId) ImportUndo
        +eraseAllData()
        +eventsForBook(bookId) Flow
        +cashFlowRecords(bookId) Flow
        +accountingSummary(bookId) Flow
    }
    class FinanceDao {
        <<interface>>
        +getEvent(id) FinancialEventEntity
        +deleteEvent(id)
        +deleteEventsForImportBatch(batchId) Int
        +markImportBatchUndone(batchId, undoneAt) Int
        +hasImported(sourceSha256) Boolean
        +observeEventsForBook(bookId, limit) Flow
        +observeCashFlowRows(bookId) Flow
        +findCategory(businessId, role, normalizedName) CategoryEntity
    }
    class StockDao {
        <<interface>>
        +observeStockItems(businessId, projectId) Flow
        +insertStockMovement(movement)
        +deleteImportedItems(batchId) Int
    }
    class AppDatabase {
        <<abstract>>
        +financeDao() FinanceDao
        +DATA_TABLES
        +MIGRATION_9_10
        +MIGRATION_10_11
    }
    class EncryptedBackupManager {
        +create(password) ByteArray
        +prepareRestore(envelope, password) PreparedRestore
        +restore(prepared, recoveryPassword) RestoreResult
        +deleteLatestRecovery()
    }
    class SyncEngine {
        +collectLocalChanges() LocalChanges
        +applyRemote(changeSet) ApplyResult
        +acknowledge(changes)
    }

    class AppViewModel {
        +StateFlow~AppUiState~ uiState
        +MutableStateFlow~Boolean~ isSaving
        +record(type, amount, ..., editingEventId)
        +deleteTransaction(eventId)
        +saveProduct(...)
        +savePlanLine(...)
        +recordStockMovement(itemId, type, quantity, notes)
        +commitImport()
        +undoImportBatch(id)
        +eraseAllData(driveAccessToken, onErased)
        -mutating(block) Job
    }
    class AppUiState {
        +String selectedBookId
        +BookMode selectedBookMode
        +List~MoneyEvent~ events
        +List~CashFlowRecord~ cashFlowRecords
        +List~StockItem~ stockItems
        +FinancialSummary summary
        +PlanAnalysis plan
        +Boolean isLoading
    }

    JournalEntry "1" *-- "2..*" Posting : berisi
    PlanProduct "1" *-- "0..*" CostComponent : resep
    MoneyEvent --> FinancialEventType
    MoneyEvent --> CashFlowRole
    CategoryRole ..> FinancialEventType : menentukan
    LedgerPolicy ..> MoneyEvent : membaca
    LedgerPolicy ..> JournalEntry : membuat
    PostingReportCalculator ..> FinancialSummary : membuat
    MonthlyCashFlowCalculator ..> CashFlowRecord : membaca
    PlanCalculator ..> PlanProduct : membaca
    PlanCalculator ..> PlanAnalysis : membuat
    StockCalculation ..> StockItem : membaca
    FinanceRepository --> FinanceDao : kueri
    FinanceRepository --> StockDao : stok
    FinanceRepository --> AppDatabase : withTransaction
    FinanceRepository ..> LedgerPolicy : jurnal
    EncryptedBackupManager --> AppDatabase : snapshot DATA_TABLES
    SyncEngine --> AppDatabase : sync_outbox
    AppViewModel --> FinanceRepository : delegasi
    AppViewModel --> EncryptedBackupManager : cadangan
    AppViewModel --> AppUiState : memancarkan
```

## Catatan desain

1. **Keseimbangan dua lapis.** `LedgerPolicy.journalFor` memanggil `JournalEntry.requireBalanced()`, lalu `FinanceRepository.insertEventAndJournal` memeriksa ulang debit sama dengan kredit tepat sebelum baris ditulis.
2. **Satu jalur untuk catat dan ubah.** `AppViewModel.record` memanggil `updateEvent` bila `editingEventId` terisi dan `recordEvent` bila tidak. Keduanya memakai `requireValid` dan `LedgerPolicy.journalFor` yang sama.
3. **Kategori menentukan jenis.** Formulir hanya punya Masuk dan Keluar. `CategoryRole.cashTypeFor` mengubahnya menjadi salah satu dari lima jenis kas (DEC-011 K-2).
4. **Proyeksi sempit.** `CashFlowRecord` hanya membawa kolom yang dibutuhkan jadwal kas, sehingga buku berisi ribuan transaksi tidak memuat seluruh deskripsi ke memori.
