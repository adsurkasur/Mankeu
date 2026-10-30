# User Flow Diagram - Mankeu

Alur navigasi sesuai `MankeuApp` pada Room **schema 12**: empat destinasi di `NavigationBar` (atau `AppRail` pada layar lebar), tiga overlay yang dibuka dari bar atas, dan satu bottom sheet lingkup.

```mermaid
flowchart TD
    launch(["Buka aplikasi"]) --> workspace{"currentWorkspace menemukan usaha dan unit?"}
    workspace -->|"belum ada"| addBusiness["Tambah usaha<br/>kategori dasar ikut dibuat"]
    addBusiness --> addProject["Unit Utama dibuat otomatis<br/>buku Rencana dan Realisasi ikut dibuat"]
    addProject --> home
    workspace -->|"ada, pilihan terakhir diingat"| home["Beranda<br/>kas, arus kas, BEP, stok"]

    home --> scopeHeader["ScopeHeader"]
    scopeHeader -->|"ketuk ringkasan"| scopeSheet["ManageScopeSheet<br/>ganti, tambah, ubah, hapus atau arsipkan"]
    scopeHeader -->|"tombol mode"| modeChoice{"Buku aktif"}
    modeChoice -->|"Realisasi"| actualBook["Catatan kas nyata<br/>warna primaryContainer"]
    modeChoice -->|"Rencana"| planBook["Rencana usaha<br/>warna secondaryContainer"]

    home --> navBar{"NavigationBar"}
    navBar -->|"Catatan, Realisasi"| entry["Catatan<br/>formulir dan daftar catatan"]
    navBar -->|"Catatan, Rencana"| planPage["Rencana Usaha<br/>produk, resep biaya, biaya tetap, aset, periode"]
    home -->|"speed dial Masuk atau Keluar"| entry
    entry --> entryForm["Formulir<br/>jumlah, keterangan, tanggal, kategori"]
    entryForm --> validate{"Valid?"}
    validate -->|"tidak"| fieldError["Pesan galat di kolom"]
    fieldError --> entryForm
    validate -->|"ya"| saving["Simpan<br/>tombol nonaktif dan indikator berputar"]
    saving --> saved["Snackbar Tersimpan atau Diperbarui<br/>formulir dikosongkan"]

    entry --> recordList["Daftar catatan<br/>cari dan saring Semua, Masuk, Keluar"]
    recordList --> eventAction{"Aksi pada kartu catatan"}
    eventAction -->|"Ubah"| editForm["Formulir terisi data catatan<br/>label Mengubah catatan"]
    editForm --> entryForm
    eventAction -->|"Hapus"| confirmDelete{"Sheet konfirmasi Hapus"}
    confirmDelete -->|"batal"| recordList
    confirmDelete -->|"Hapus"| deleted["Catatan terhapus<br/>jurnal ikut terhapus"]

    navBar -->|"Stok"| stock["Stok<br/>barang unit ini dan barang bersama"]
    stock --> stockSheet["Sheet barang<br/>Masuk, Keluar, Opname"]

    navBar -->|"Laporan"| reports["Laporan"]
    reports --> reportTabs{"Tab"}
    reportTabs -->|"Laba Rugi dan Arus Kas"| summaryTab["Laba rugi, arus kas, grafik bulanan"]
    reportTabs -->|"Kelayakan dan BEP"| feasibilityTab["BEP unit dan rupiah, kebutuhan modal"]
    reportTabs -->|"Uang vs rencana"| compareTab["Uang masuk/keluar rata-rata per bulan, basis kas"]

    home -->|"ikon Data"| dataOverlay["Data<br/>impor, ekspor, cadangan, kategori"]
    dataOverlay --> importFlow["Pilih berkas templat, pratinjau, simpan"]
    importFlow --> batchCard["Kartu riwayat impor"]
    batchCard -->|"Batalkan impor"| undoImport["Catatan batch dihapus<br/>riwayat impor tetap, ditandai dibatalkan"]
    dataOverlay --> backupFlow["Cadangan terenkripsi<br/>berkas lokal atau Google Drive"]
    home -->|"ikon Pengaturan"| settingsOverlay["Pengaturan<br/>tema, getar, bahasa, akun, panduan"]
    home -->|"tombol tanda tanya"| helpOverlay["Pusat Bantuan<br/>artikel dan Tunjukkan di aplikasi"]

    classDef screen fill:#E3F2FD,stroke:#1565C0,stroke-width:2px
    classDef blocked fill:#FFEBEE,stroke:#C62828,stroke-width:2px
    classDef success fill:#E8F5E9,stroke:#2E7D32,stroke-width:2px
    class home,entry,planPage,stock,reports,dataOverlay,settingsOverlay,helpOverlay screen
    class fieldError blocked
    class saved,deleted,undoImport success
```

## Catatan kegunaan

1. **Ubah di tempat (DEC-019).** Catatan yang salah dibetulkan di formulir yang sama dengan saat mencatat, tanpa baris tambahan atau tanda khusus pada kartu.
2. **Hapus selalu dikonfirmasi.** Sheet konfirmasi menampilkan keterangan dan jumlah catatan dengan warna galat sebelum penghapusan.
3. **Warna buku berbeda** agar catatan kas nyata tidak tercatat ke buku rencana, dan Catatan berganti menjadi Rencana Usaha di mode Rencana.
