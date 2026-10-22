# Sequence Diagrams - Mankeu

Diselaraskan dengan Room **schema 12** (DEC-012, DEC-019).

## 1. Mencatat atau mengubah catatan kas

```mermaid
sequenceDiagram
    autonumber
    actor Owner as Pemilik usaha
    participant Entry as EntryScreen
    participant VM as AppViewModel
    participant Repo as FinanceRepository
    participant Policy as LedgerPolicy
    participant Dao as FinanceDao
    participant Db as Room SQLite

    Owner->>Entry: Ketuk Simpan, Rp50.000 beli bahan
    Entry->>Entry: Abaikan bila isSaving atau justSaved
    Entry->>VM: record(type, amount, description, ..., editingEventId)
    VM->>VM: mutating, isSaving compareAndSet false ke true
    alt editingEventId kosong
        VM->>Repo: recordEvent(EventDraft)
    else Ubah catatan
        VM->>Repo: updateEvent(eventId, EventDraft)
    end
    Repo->>Db: withTransaction dimulai
    Repo->>Repo: requireValid: jumlah, lingkup, kategori
    Repo->>Policy: journalFor(event)
    Policy-->>Repo: JournalEntry seimbang
    Repo->>Repo: Periksa ulang debit sama dengan kredit

    alt Aturan dilanggar
        Repo-->>Db: Rollback
        Repo-->>VM: IllegalArgumentException
        VM-->>Entry: message berisi alasan penolakan
    else Semua aturan terpenuhi
        opt Ubah catatan
            Repo->>Dao: deleteEvent(id lama), jurnal ikut CASCADE
        end
        Repo->>Dao: insertEvent, id sama bila Ubah
        Repo->>Dao: insertJournal
        Repo->>Dao: insertPostings
        Repo-->>Db: Commit, trigger mengisi sync_outbox bila sync menyala
        Repo-->>VM: selesai
        VM-->>Entry: Tersimpan atau Catatan diperbarui
        Db-->>VM: Invalidation Room memicu Flow
        VM-->>Entry: uiState baru
    end
    VM->>VM: isSaving kembali false
```

## 2. Impor templat ke unit terpilih

```mermaid
sequenceDiagram
    autonumber
    actor Owner as Pemilik usaha
    participant Data as DataScreen
    participant VM as AppViewModel
    participant Importer as FinanceWorkbookImporter
    participant Repo as FinanceRepository
    participant Db as Room SQLite

    Owner->>Data: Pilih berkas
    Data->>VM: previewWorkbook(contentResolver, uri)
    VM->>Importer: Baca templat
    alt Bukan templat Mankeu
        Importer-->>VM: Daftar lembar atau kolom yang hilang
        VM-->>Data: ImportUiState.Failure dan tautan templat
    else Templat dikenali
        Importer-->>VM: WorkbookImportPreview
        VM->>Repo: reviewImport(businessId, projectId, preview)
        Repo-->>VM: ImportReview, kategori baru dan peringatan duplikat
        VM-->>Data: ImportUiState.Ready
        Owner->>Data: Setujui pratinjau
        Data->>VM: commitImport()
        VM->>Repo: commitImport(businessId, projectId, preview)
        Repo->>Db: withTransaction dimulai
        alt Ada konflik kategori atau produk
            Repo-->>Db: Rollback
            Repo-->>VM: Galat
            VM-->>Data: ImportUiState.Failure
        else Berhasil
            Repo->>Db: Batch, kategori, produk, transaksi, RAB, stok, notice
            Repo-->>Db: Commit
            Repo-->>VM: Jumlah baris yang ditambahkan
            VM-->>Data: ImportUiState.Committed
        end
    end
```

## 3. Batalkan impor

```mermaid
sequenceDiagram
    autonumber
    actor Owner as Pemilik usaha
    participant Data as DataScreen
    participant VM as AppViewModel
    participant Repo as FinanceRepository
    participant Db as Room SQLite

    Owner->>Data: Batalkan impor, lalu konfirmasi
    Data->>VM: undoImportBatch(id)
    VM->>Repo: undoImportBatch(batchId)
    Repo->>Db: withTransaction dimulai
    Repo->>Db: deleteEventsForImportBatch, jurnal ikut CASCADE
    Repo->>Db: Arsipkan RAB batch
    Repo->>Db: Hapus stok dan produk batch
    opt Periode unit masih persis yang diisi impor
        Repo->>Db: Kosongkan periode rencana
    end
    Repo->>Db: markImportBatchUndone
    alt Batch sudah dibatalkan
        Repo-->>Db: Rollback
        Repo-->>VM: Impor ini sudah dibatalkan
    else Berhasil
        Repo-->>Db: Commit
        Repo-->>VM: ImportUndo(records, periodCleared)
        VM-->>Data: Impor dibatalkan, N catatan dihapus
    end
```

## Jaminan

1. **Atomik.** Semua penulisan dan pemeriksaan satu aksi berada dalam satu transaksi SQLite. Penolakan tidak meninggalkan baris setengah jadi. Ubah adalah satu pertukaran atomik: tidak pernah ada saat catatan lama sudah hilang dan yang baru belum tertulis.
2. **Tidak ada ketukan ganda.** `compareAndSet` di `mutating` menolak aksi kedua yang tiba sebelum aksi pertama selesai, termasuk ketukan dalam satu frame (D-072).
3. **Reaktif.** Setelah commit, invalidation Room memancarkan ulang Flow, dan `uiState` memperbarui layar tanpa pemanggilan manual.
