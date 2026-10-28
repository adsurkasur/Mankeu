# Entity Relationship Diagram - Basis Data Mankeu

Skema Room SQLite **versi 11** (`com.mankeu.data`), diselaraskan dengan `Entities.kt`, `domain/StockManagement.kt`, `sync/SyncEntities.kt`, dan `app/schemas/com.mankeu.data.AppDatabase/11.json`.

> Garis **tegas** adalah foreign key sungguhan (dengan aturan `CASCADE` atau `RESTRICT`). Garis **putus-putus** adalah referensi logis: kolomnya diindeks, tetapi tidak dijaga foreign key, sehingga integritasnya ditegakkan oleh `FinanceRepository`.

```mermaid
erDiagram
    businesses ||--o{ projects : "CASCADE"
    businesses ||--o{ categories : "CASCADE"
    projects ||--|{ books : "tepat dua buku"
    books ||--o{ budget_items : "CASCADE"
    categories ||--o{ budget_items : "RESTRICT"
    books ||--o{ financial_events : "CASCADE"
    financial_events ||--|| journal_entries : "CASCADE"
    journal_entries ||--|{ postings : "CASCADE"
    projects ||--o{ products : "CASCADE"
    products ||--o{ product_units : "CASCADE"
    products ||--o{ product_recipe_lines : "CASCADE"
    products ||--o{ product_movements : "CASCADE"
    stock_items ||..o{ product_recipe_lines : "stockItemId logis"
    stock_items ||..o{ financial_events : "stockItemId logis (bahan dibeli)"
    businesses ||--o{ stock_items : "CASCADE"
    stock_items ||--o{ stock_movements : "CASCADE"
    import_batches ||--o{ import_notices : "CASCADE"
    import_batches ||--o{ provenance_records : "CASCADE"
    categories ||..o{ financial_events : "categoryId logis"
    products ||..o{ financial_events : "productId logis"
    budget_items ||..o{ financial_events : "budgetItemId logis"
    projects ||..o{ stock_items : "projectId logis, NULL = semua unit"
    import_batches ||..o{ financial_events : "importBatchId logis"
    import_batches ||..o{ budget_items : "importBatchId logis"
    import_batches ||..o{ products : "importBatchId logis"
    import_batches ||..o{ stock_items : "importBatchId logis"
    financial_events |o..o{ stock_movements : "referenceEventId logis"

    businesses {
        string id PK
        string name
        string currency
        boolean archived
        long createdAtEpochMillis
        long updatedAtEpochMillis
    }

    projects {
        string id PK
        string businessId FK
        string name "unit usaha"
        boolean isGeneral
        boolean archived
        string description
        long startDateEpochDay "periode rencana"
        long endDateEpochDay
        long createdAtEpochMillis
        long updatedAtEpochMillis
    }

    books {
        string id PK
        string projectId FK "unik bersama mode"
        string mode "PROJECTION atau REALIZATION"
        long createdAtEpochMillis
        long updatedAtEpochMillis
    }

    categories {
        string id PK
        string businessId FK
        string role "REVENUE EXPENSE CAPITAL_IN DRAWING ASSET"
        string name
        string normalizedName "unik bersama businessId dan role"
        string aliases
        string templateOrigin
        boolean active "arsip = 0, dapat diaktifkan lagi"
        string costNature "FIXED atau VARIABLE"
        long createdAtEpochMillis
    }

    budget_items {
        string id PK
        string bookId FK "buku Rencana"
        string categoryId FK
        string name
        string normalizedName
        string quantityText
        string unit
        long unitPriceRupiah
        long plannedTotalRupiah
        long plannedDateEpochDay
        string aliases
        string amountSource
        long formulaDifferenceRupiah
        int sortOrder
        boolean archived
        string costNature "menimpa kategori"
        string recurrence "MONTHLY atau ONCE"
        int usefulLifeMonths "khusus aset"
        string importBatchId "logis"
        long createdAtEpochMillis
        long updatedAtEpochMillis
    }

    financial_events {
        string id PK
        string businessId FK
        string projectId FK
        string bookId FK
        string type "CASH_INCOME CASH_EXPENSE OWNER_CAPITAL OWNER_DRAWING ASSET_PURCHASE_CASH"
        long amountRupiah "rupiah bulat lebih dari 0"
        long dateEpochDay
        string description
        string category
        string categoryId "logis"
        string reference
        string quantityText
        string unit
        long unitPriceRupiah
        string budgetItemId "logis"
        string productId "logis"
        string costNature
        int usefulLifeMonths
        string stockItemId "logis, bahan yang dibeli"
        string importBatchId "logis"
        string cashFlowRole "OPERATING INVESTING FINANCING"
        long createdAtEpochMillis "tetap saat Ubah"
    }

    journal_entries {
        string id PK
        string sourceEventId FK, UK
        long dateEpochDay
        string description
    }

    postings {
        string id PK
        string journalId FK
        string account
        string side "DEBIT atau CREDIT"
        long amountRupiah
    }

    products {
        string id PK
        string projectId FK
        string name
        string unit "satuan dasar"
        string batchYieldText "hasil satu batch, default 1"
        string targetPerMonthText "dalam satuan jual utama"
        int sellFromMonth "1 = bulan pertama rencana"
        int sortOrder
        boolean archived
        string importBatchId "logis"
        long createdAtEpochMillis
        long updatedAtEpochMillis
    }

    product_units {
        string id PK "produk lama: <productId>-u0"
        string productId FK
        string name
        string contentText "isi dalam satuan dasar"
        long priceRupiah
        int sortOrder "0 = satuan utama"
    }

    product_recipe_lines {
        string id PK
        string productId FK
        string name
        string stockItemId "logis, bahan"
        string quantityText "per batch"
        long amountRupiah "biaya bebas per batch"
        int sortOrder
    }

    product_movements {
        string id PK
        string productId FK
        string movementType "PRODUCTION / ADJUSTMENT"
        string quantityText "satuan dasar"
        string batchesText
        long dateEpochDay
        string notes
        long createdAtEpochMillis
    }

    stock_items {
        string id PK
        string businessId FK
        string projectId "logis, unik bersama businessId dan name"
        string name
        string unit
        long basePriceRupiah
        string initialStockText
        string currentStockText
        string minAlertStockText
        boolean archived
        string importBatchId "logis"
        long createdAtEpochMillis
        long updatedAtEpochMillis
    }

    stock_movements {
        string id PK
        string stockItemId FK
        string movementType "IN OUT ADJUSTMENT"
        string quantityText
        long dateEpochDay
        string notes
        string referenceEventId "logis"
        long createdAtEpochMillis
    }

    import_batches {
        string id PK
        string sourceName
        string sourceSha256 "indeks biasa"
        string importerVersion
        string state "COMMITTED atau REVERSED = dibatalkan"
        long committedAtEpochMillis
        string businessId
        string projectId
        string bookId
        string mappingVersion
        int recordCount
        long totalAmountRupiah
        long incomingRupiah
        long appliedStartEpochDay
        long appliedEndEpochDay
        long reversedAtEpochMillis "waktu Batalkan impor"
    }

    import_notices {
        string id PK
        string batchId FK "NULL untuk notice SYSTEM"
        string severity
        string title
        string explanation
        long amountRupiah
        string sourceEvidence
        boolean reviewed
        string action
        long createdAtEpochMillis
    }

    provenance_records {
        string id PK
        string batchId FK
        string targetEventId
        string targetBudgetItemId
        string targetField
        string classification
        string sourceEvidence
        string sourceSheet
        string sourceCell
        string sourceValue
        string rationale
        string inferredFields
        int confidenceBasisPoints
        string ruleVersion
    }

    sync_outbox {
        long seq PK "autoincrement"
        string tableName
        string rowId
        string op "UPSERT atau DELETE"
        long changedAtEpochMillis
    }

    sync_meta {
        string key PK
        string value
    }
```

`sync_outbox` dan `sync_meta` tidak memiliki relasi: outbox diisi trigger SQLite pada setiap tabel `DATA_TABLES` (DEC-013, DEC-015), dan `sync_meta` menyimpan flag seperti `applying_remote` dan `requires_full_snapshot`.

## Aturan integritas

1. **Dua buku per unit.** Indeks unik `(projectId, mode)` dan `ensureBookPair` menjamin tepat satu buku `PROJECTION` dan satu `REALIZATION`.
2. **Jurnal selalu seimbang.** Setiap event memiliki tepat satu `journal_entries` (unik pada `sourceEventId`). Debit dan kredit diperiksa di `insertEventAndJournal` sebelum baris ditulis, di dalam transaksi yang sama.
3. **Kas murni (DEC-011).** Hanya lima jenis event kas: uang yang benar-benar berpindah.
4. **Ubah dan Hapus (DEC-019).** `updateEvent` mengganti event di tempat: id, `importBatchId`, dan `createdAtEpochMillis` dipertahankan, jurnal dan posting dibangun ulang. `deleteEvent` menghapus event, dan jurnal serta posting ikut terhapus lewat `CASCADE`. Tidak ada lagi status atau kolom pembalikan.
5. **Rencana usaha (DEC-014, DEC-017).** Produk dan komponen biaya per unit milik unit (`projects`). Biaya tetap, biaya sekali, aset, dan modal rencana adalah `budget_items` di buku Rencana.
6. **Stok per unit (DEC-017).** `stock_items.projectId` NULL berarti dipakai bersama semua unit. Nama unik di antara barang yang terlihat oleh satu unit.
7. **Impor dan pembatalannya.** Duplikat boleh diimpor ulang dengan peringatan (`hasImported` hanya menghitung batch `COMMITTED`). `undoImportBatch` menghapus event batch, mengarsipkan RAB-nya, menghapus produk dan stoknya, lalu menandai batch `REVERSED` sebagai riwayat.
8. **Migrasi.** `MIGRATION_10_11` menjalankan `ReversalCleanup` (menghapus setiap pasangan pembalikan beserta jurnalnya, meninggalkan satu notice SYSTEM), lalu membangun ulang `financial_events` dan `journal_entries` tanpa kolom pembalikan dan membuat ulang trigger sinkronisasi. Tidak ada migrasi destruktif.
