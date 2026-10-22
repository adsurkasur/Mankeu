# State Diagrams - Siklus Hidup Mankeu

Diselaraskan dengan Room **schema 12**. Sejak DEC-019 catatan kas tidak punya kolom status: catatan ada, diubah di tempat, atau dihapus.

## 1. Catatan kas (`financial_events`)

Tidak ada status draf di basis data: formulir yang belum disimpan hanya ada di UI.

```mermaid
stateDiagram-v2
    [*] --> Recorded : recordEvent atau commitImport
    Recorded --> Recorded : updateEvent, id dan createdAt tetap
    Recorded --> Deleted : deleteEvent
    Recorded --> Deleted : undoImportBatch untuk catatan batch

    state Deleted {
        [*] --> Removed
        Removed : jurnal dan posting ikut terhapus lewat CASCADE
    }

    note right of Recorded
        Setiap simpan membangun ulang jurnal
        dengan LedgerPolicy.journalFor.
        Trigger mencatat UPSERT atau DELETE
        ke sync_outbox.
    end note
```

## 2. Batch impor dan pratinjau (`ImportUiState`)

```mermaid
stateDiagram-v2
    state "Pratinjau di UI" as PreviewFlow {
        [*] --> Idle
        Idle --> Loading : berkas dipilih
        Loading --> Ready : templat dikenali, reviewImport selesai
        Loading --> Failure : bukan templat Mankeu atau ada galat
        Ready --> Loading : simpan
        Loading --> Committed : commitImport berhasil
        Failure --> Idle : reset
        Committed --> Idle : reset
    }

    state "Baris import_batches" as BatchRow {
        [*] --> COMMITTED
        COMMITTED --> REVERSED : undoImportBatch, tampil dibatalkan
    }

    Committed --> BatchRow : batch tertulis
    REVERSED --> Idle : berkas yang sama boleh diimpor ulang

    note right of BatchRow
        REVERSED hanya ejaan tersimpan untuk dibatalkan.
        Catatan batch sudah dihapus, baris batch tetap sebagai riwayat.
    end note
```

## 3. Buku aktif (`BookMode`)

```mermaid
stateDiagram-v2
    state "REALIZATION, primaryContainer" as Realization
    state "PROJECTION, secondaryContainer" as Projection

    [*] --> Realization : bawaan saat aplikasi dibuka
    Realization --> Projection : pilih Rencana
    Projection --> Realization : pilih Realisasi

    note right of Projection
        Catatan menjadi Rencana Usaha: produk, resep biaya,
        biaya tetap, aset, modal, dan periode rencana.
    end note

    note left of Realization
        Catatan kas nyata, saldo kas,
        BEP realisasi bulan berjalan.
    end note
```

## 4. Barang stok (`stock_items`)

```mermaid
stateDiagram-v2
    state "Aman" as Normal
    state "Stok menipis" as Low

    [*] --> Normal : createStockItem, stok awal di atas batas
    [*] --> Low : stok awal di bawah atau sama dengan batas
    Normal --> Low : keluar atau opname sampai batas
    Low --> Normal : masuk atau opname di atas batas
    Normal --> Normal : recordStockMovement
    Low --> Low : recordStockMovement
    Normal --> [*] : undoImportBatch untuk barang impor
    Low --> [*] : undoImportBatch untuk barang impor

    note right of Low
        isLowStock: currentStock kurang dari
        atau sama dengan minAlertStock.
        Keluar melebihi stok tersedia ditolak.
    end note
```
