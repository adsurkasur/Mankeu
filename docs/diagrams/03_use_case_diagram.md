# Use Case Diagram - Mankeu

Mankeu adalah aplikasi satu pengguna tanpa peran atau hak akses bertingkat: satu pemilik usaha mengelola seluruh buku di perangkatnya. Aktor lain adalah sistem eksternal. Diselaraskan dengan kode pada Room **schema 11** (DEC-011 sampai DEC-019).

```mermaid
flowchart LR
    owner["Pemilik usaha kecil"]
    workbookFile["Berkas Excel templat Mankeu"]
    googleDrive["Google Drive appDataFolder"]
    firebaseAuth["Firebase Auth"]

    subgraph mankeuSystem ["Aplikasi Mankeu"]
        subgraph scopeModule ["1. Lingkup dan buku"]
            ucScope["Kelola usaha dan unit"]
            ucMode["Beralih buku Proyeksi atau Realisasi"]
            ucCategory["Kelola kategori dan sifat biaya"]
        end

        subgraph entryModule ["2. Catatan kas"]
            ucCash["Catat uang masuk atau keluar<br/>jenis ditentukan kategori"]
            ucEdit["Ubah catatan"]
            ucDelete["Hapus catatan"]
            ucValidate["Periksa jumlah, lingkup, dan kategori"]
            ucSearch["Cari dan saring Semua, Masuk, Keluar"]
        end

        subgraph planModule ["3. Rencana usaha"]
            ucProduct["Kelola produk dan komponen biaya"]
            ucPlanLine["Kelola biaya tetap, biaya sekali, aset, modal"]
            ucPeriod["Atur periode rencana"]
        end

        subgraph stockModule ["4. Stok"]
            ucStockItem["Tambah barang untuk unit atau semua unit"]
            ucStockMove["Catat masuk, keluar, atau opname"]
        end

        subgraph analysisModule ["5. Laporan dan kelayakan"]
            ucDashboard["Pantau kas, BEP, dan stok di Beranda"]
            ucChart["Jelajahi grafik arus kas bulanan"]
            ucFeasibility["Hitung BEP, kebutuhan modal, dan kelayakan"]
            ucCompare["Bandingkan realisasi dengan target"]
        end

        subgraph dataModule ["6. Data"]
            ucImport["Impor templat ke unit terpilih"]
            ucUndoImport["Batalkan impor"]
            ucExport["Ekspor Excel, PDF, atau templat kosong"]
            ucBackup["Buat cadangan terenkripsi"]
            ucRestore["Pulihkan cadangan"]
        end

        subgraph helpModule ["7. Bantuan"]
            ucTour["Ikuti panduan halaman yang ditawarkan"]
            ucHelp["Baca Pusat Bantuan dan Tunjukkan di aplikasi"]
        end
    end

    owner --> ucScope
    owner --> ucMode
    owner --> ucCategory
    owner --> ucCash
    owner --> ucEdit
    owner --> ucDelete
    owner --> ucSearch
    owner --> ucProduct
    owner --> ucPlanLine
    owner --> ucPeriod
    owner --> ucStockItem
    owner --> ucStockMove
    owner --> ucDashboard
    owner --> ucChart
    owner --> ucCompare
    owner --> ucImport
    owner --> ucUndoImport
    owner --> ucExport
    owner --> ucBackup
    owner --> ucRestore
    owner --> ucTour
    owner --> ucHelp

    ucCash -.->|"include"| ucValidate
    ucEdit -.->|"include"| ucValidate
    ucProduct -.->|"masukan"| ucFeasibility
    ucPlanLine -.->|"masukan"| ucFeasibility
    ucDashboard -.->|"include"| ucFeasibility
    ucUndoImport -.->|"menghapus catatan batch"| ucDelete
    workbookFile -.->|"dibaca"| ucImport
    ucExport -.->|"menghasilkan"| workbookFile
    ucBackup -.->|"opsional, ciphertext"| googleDrive
    googleDrive -.->|"unduh"| ucRestore
    firebaseAuth -.->|"identitas untuk Drive"| ucBackup
```

| Use case | Tujuan bagi pemilik usaha |
| --- | --- |
| Catat uang masuk atau keluar | Mengganti buku kas tulis dengan beberapa ketukan. Kategori menentukan jenisnya (pemasukan, pengeluaran, setoran modal, ambil pribadi, beli aset), tanpa istilah debit dan kredit. |
| Ubah dan Hapus | Membetulkan catatan yang salah langsung di tempat, atau menghapusnya, tanpa baris pembalik tambahan (DEC-019). |
| Rencana usaha | Menjawab "berapa modal yang harus disiapkan" dan "berapa unit harus terjual agar tidak rugi" dari produk, resep biaya, dan biaya tetap. |
| Stok | Mengetahui sisa barang per unit dan kapan harus belanja lagi. |
| Impor templat | Memasukkan banyak catatan sekaligus ke unit terpilih. Batalkan impor menghapus catatan batch itu, dan berkas yang sama boleh diimpor ulang. |
| Cadangan terenkripsi | Memindahkan atau menyelamatkan data tanpa pihak mana pun dapat membaca isinya. |

Sinkronisasi multi-perangkat (DEC-013) sudah ada sebagai kerangka di `sync/`, tetapi belum dapat dijalankan pemilik dari UI, sehingga tidak muncul sebagai use case.
