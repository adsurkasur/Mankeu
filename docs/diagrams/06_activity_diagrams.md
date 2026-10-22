# Activity Diagrams - Alur Bisnis Kritis Mankeu

Lima alur yang menulis, mengubah, atau menghapus uang pada Room **schema 12**. Semua pemeriksaan di bawah ini berada di `FinanceRepository`, di dalam `withTransaction`: satu penolakan membatalkan seluruh penulisan.

## 1. Mencatat transaksi

```mermaid
flowchart TD
    tap(["Ketuk Simpan"]) --> uiGuard{"isSaving atau justSaved?"}
    uiGuard -->|"ya"| ignoreTap(["Ketukan diabaikan"])
    uiGuard -->|"tidak"| formValid{"Formulir valid?"}
    formValid -->|"tidak"| formError(["Galat di kolom"])
    formValid -->|"ya"| typeFromCategory["CategoryRole.cashTypeFor<br/>Masuk atau Keluar menjadi jenis kas"]
    typeFromCategory --> mutating["AppViewModel.mutating<br/>isSaving compareAndSet"]
    mutating --> reconcile["TransactionCalculation.reconcile<br/>total, kuantitas, harga satuan"]
    reconcile --> openTx["recordEvent: withTransaction"]
    openTx --> ruleAmount{"Jumlah lebih dari 0?"}
    ruleAmount -->|"tidak"| rollback["Rollback dan snackbar penolakan"]
    ruleAmount -->|"ya"| ruleScope{"Usaha, unit, dan buku konsisten?"}
    ruleScope -->|"tidak"| rollback
    ruleScope -->|"ya"| ruleCategory{"Kategori milik usaha yang sama?"}
    ruleCategory -->|"tidak"| rollback
    ruleCategory -->|"ya"| journal["LedgerPolicy.journalFor<br/>requireBalanced"]
    journal --> recheck{"Debit sama dengan kredit dan semua posting positif?"}
    recheck -->|"tidak"| rollback
    recheck -->|"ya"| insertRows["Tulis event, jurnal, dan posting"]
    insertRows --> commitTx["Commit<br/>trigger menulis sync_outbox"]
    commitTx --> savedDone(["Snackbar tersimpan, Flow Room memperbarui layar"])
    rollback --> failDone(["Tidak ada baris yang tertulis"])
```

## 2. Mengubah catatan (Ubah)

```mermaid
flowchart TD
    askEdit(["Pengguna memilih Ubah pada kartu"]) --> fillForm["Formulir diisi dari catatan<br/>editingId disimpan"]
    fillForm --> editSave(["Ketuk Simpan"])
    editSave --> recordCall["AppViewModel.record<br/>editingEventId terisi"]
    recordCall --> updateTx["updateEvent: withTransaction"]
    updateTx --> loadOld{"Catatan lama masih ada?"}
    loadOld -->|"tidak"| refuseEdit["Tolak: Transaksi tidak ditemukan"]
    loadOld -->|"ya"| validDraft{"requireValid lolos?<br/>sama dengan saat mencatat"}
    validDraft -->|"tidak"| refuseEdit
    validDraft -->|"ya"| keepIdentity["Pertahankan id, importBatchId,<br/>dan createdAtEpochMillis"]
    keepIdentity --> swapRow["Hapus baris lama, jurnal ikut CASCADE"]
    swapRow --> rebuild["Tulis event baru dengan id sama<br/>jurnal dari LedgerPolicy.journalFor"]
    rebuild --> editDone(["Commit, snackbar Catatan diperbarui"])
    refuseEdit --> editFail(["Rollback dan pesan penjelasan"])
```

## 3. Menghapus catatan (Hapus)

```mermaid
flowchart TD
    askDelete(["Pengguna memilih Hapus"]) --> warnFeedback["Getar WARN<br/>sheet konfirmasi dengan keterangan dan jumlah"]
    warnFeedback --> confirm{"Pengguna menekan Hapus?"}
    confirm -->|"tidak"| keep(["Tidak ada perubahan"])
    confirm -->|"ya"| deleteCall["AppViewModel.deleteTransaction"]
    deleteCall --> dropEvent["FinanceRepository.deleteEvent<br/>DELETE financial_events"]
    dropEvent --> cascade["CASCADE: jurnal dan posting"]
    cascade --> deleteDone(["Snackbar terhapus, saldo dan laporan dihitung ulang"])
```

## 4. Impor templat dan Batalkan impor

```mermaid
flowchart TD
    pickFile(["Pilih berkas"]) --> readBytes["Baca maksimal 25 MB, hitung SHA-256"]
    readBytes --> unzip["XlsxWorkbookReader<br/>maks 1.000 entri, berhenti begitu batas terlewati,<br/>DOCTYPE dan entitas eksternal ditolak"]
    unzip --> template{"Penanda templat, lembar, dan kolom cocok?"}
    template -->|"tidak"| rejectFile(["Tolak dengan daftar masalah<br/>dan tautan unduh templat"])
    template -->|"ya"| review["reviewImport<br/>kategori baru, konflik peran, stok dan produk yang sudah ada,<br/>peringatan bila berkas pernah diimpor"]
    review --> confirmImport{"Pengguna menyetujui pratinjau?"}
    confirmImport -->|"tidak"| cancelImport(["Pratinjau dibuang"])
    confirmImport -->|"ya"| importTx["commitImport: withTransaction<br/>ke usaha dan unit terpilih"]
    importTx --> conflicts{"Ada konflik kategori atau produk?"}
    conflicts -->|"ya"| importRollback(["Rollback, tidak ada baris tertulis"])
    conflicts -->|"tidak"| batchRow["Tulis import_batches COMMITTED<br/>isi periode rencana bila unit belum punya"]
    batchRow --> categories["Buat kategori baru yang tercantum di pratinjau"]
    categories --> products["Produk dan komponen biaya ke unit"]
    products --> events["Transaksi ke buku Realisasi<br/>event dan jurnal seimbang"]
    events --> budgets["Rencana Anggaran ke buku Rencana"]
    budgets --> stockRows["Barang stok ke unit terpilih"]
    stockRows --> notice["Notice: apa yang ditambahkan"]
    notice --> importCommit(["Commit"])

    undoBatch(["Batalkan impor"]) --> undoTx["undoImportBatch: withTransaction"]
    undoTx --> deleteEvents["Hapus semua event batch,<br/>termasuk yang sudah diubah"]
    deleteEvents --> archiveBudgets["Arsipkan RAB batch"]
    archiveBudgets --> removeRest["Hapus stok dan produk batch"]
    removeRest --> periodCheck{"Periode unit masih persis yang diisi impor?"}
    periodCheck -->|"ya"| clearPeriod["Kosongkan periode rencana"]
    periodCheck -->|"tidak"| markBatch
    clearPeriod --> markBatch{"Tandai batch REVERSED<br/>tepat satu baris berubah?"}
    markBatch -->|"tidak"| undoRefused(["Rollback: impor ini sudah dibatalkan"])
    markBatch -->|"ya"| undoDone(["Commit: riwayat impor tetap, tampil dibatalkan<br/>berkas yang sama boleh diimpor ulang"])
```

## 5. Cadangan dan pemulihan terenkripsi

```mermaid
flowchart TD
    startBackup(["Buat cadangan"]) --> passwordLength{"Kata sandi minimal 8 karakter?"}
    passwordLength -->|"tidak"| backupRefused(["Tolak"])
    passwordLength -->|"ya"| capture["Snapshot semua DATA_TABLES dalam satu transaksi baca"]
    capture --> encode["PortableBackupCodec<br/>MANKEU-PORTABLE-DB"]
    encode --> derive["Salt 16 byte, IV 12 byte<br/>PBKDF2-HMAC-SHA256 310.000 iterasi"]
    derive --> encrypt["AES-256-GCM, AAD MANKEU-BKP"]
    encrypt --> destination{"Tujuan"}
    destination -->|"berkas"| safFile["Tulis lewat Storage Access Framework"]
    destination -->|"Drive"| driveUpload["Unggah ke appDataFolder<br/>sisakan 5 cadangan terbaru"]
    safFile --> wipePassword(["Kata sandi dihapus dari memori"])
    driveUpload --> wipePassword

    startRestore(["Pulihkan"]) --> decrypt["Dekripsi dan dekode, galat bila sandi salah"]
    decrypt --> schemaCheck{"Schema cadangan antara 3 dan 11?"}
    schemaCheck -->|"tidak"| restoreRefused(["Tolak"])
    schemaCheck -->|"ya"| previewRestore["Pratinjau jumlah usaha, unit, RAB, transaksi"]
    previewRestore --> recoveryCopy["Salinan pemulihan otomatis terenkripsi"]
    recoveryCopy --> replaceTx["Satu transaksi, trigger sinkronisasi dibisukan:<br/>kosongkan tabel, isi hanya kolom yang masih ada"]
    replaceTx --> legacy{"Schema cadangan lebih lama?"}
    legacy -->|"di bawah 11, 8, atau 9"| upgrade["Pembersihan dan konversi yang sama<br/>dengan migrasi ke schema 11"]
    legacy -->|"tidak"| resetSync
    upgrade --> resetSync["Kosongkan sync_outbox<br/>requires_full_snapshot = 1"]
    resetSync --> restoreDone(["Pilih ulang usaha dan unit"])
```
