# Uji Alur sebagai Pengusaha Baru (2026-10-29)

Dokumen handoff. Diisi **per checkpoint** selama pengujian, jadi bagian di bawah yang belum diisi berarti
belum dikerjakan. Agen berikutnya: lanjutkan dari checkpoint pertama yang berstatus `belum`.

Klasifikasi pernyataan: `verified` (dilihat di kode/emulator), `user-confirmed`, `inferred`, `proposed`, `unknown`.

## Latar (user-confirmed, 2026-10-29)

- Demo ke pengusaha 2026-10-28 berdampingan dengan alat Apps Script buatan dosen. Pemilik menilai Mankeu
  membingungkan, terasa belum selesai, dan tidak bisa menghitung HPP sesuai cara pengusaha.
- Arah produk sejak 2026-10-29: **pemilik yang menentukan**. Ini bukan kompetisi; ukurannya kegunaan nyata bagi
  pengusaha yang sudah fasih Excel. Arahan lama yang membuat app lebih buruk boleh ditinggalkan lewat DEC baru.
- Masukan pengusaha:
  - "Saya jualnya tidak eceran, saya perlu HPP bukan untuk satu bungkus" → butuh HPP per batch produksi,
    per satuan jual (dus/kg/karung), dan per pesanan.
  - "Data saya tidak ada yang bisa buka kan?" → privasi penting.
  - Butuh utang-piutang: konsinyasi (titip jual), pembeli bayar belakangan.
- Fitur Apps Script yang terlihat saat demo: buku utang, pencatatan, daftar produk, sinkron Drive dan
  Spreadsheet, dashboard.

## Temuan dari kode sebelum uji emulator (verified)

1. **Privasi**
   - Data lokal di HP; tidak ada server milik app; tidak ada analytics; `allowBackup="false"`.
   - Backup/sinkron Drive ke `appDataFolder` (tersembunyi), dienkripsi AES-GCM dengan kata sandi pemilik
     (PBKDF2 ≥100.000 iterasi, `EncryptedBackupManager.kt`).
   - Celah: tidak ada kunci app (PIN/biometrik), tidak ada `FLAG_SECURE`, database lokal tidak terenkripsi.
2. **"Batas waktu" (kalender dari–sampai)**: satu data (`ProjectEntity.startDateEpochDay/endDateEpochDay`)
   yang diedit di dua tempat dengan nama berbeda: "Mulai operasional / Perkiraan selesai (opsional)" di
   Usaha/Unit (`ManageScopeScreen.kt:440`) dan "Periode rencana" di Rencana (`PlanScreen.kt:549`).
   Hanya dipakai untuk jumlah bulan rencana/BEP (`PlanAssembly.kt:45`), tidak memengaruhi catatan atau
   laporan realisasi. Memilih tanggal per hari padahal yang dipakai hanya bulan.
3. **Utang-piutang**: pernah ada (D-042, D-077), dihapus total oleh DEC-011 (strict cash, 2026-10-20).
4. **HPP**: resep biaya per satuan (`CostComponent` dengan "untuk berapa unit"); tidak ada konsep batch
   produksi, satuan jual berbeda, atau HPP per pesanan. HPP di Rencana tidak terhubung ke penjualan di Catatan.
5. **Struktur navigasi**: 4 tab (Beranda, Catatan, Stok, Laporan) + saklar global Rencana/Realisasi di
   header (`MankeuApp.kt:1047`) yang mengubah isi tab. Hierarki Usaha → Unit → Buku sebelum bisa mencatat.

## Checkpoint uji emulator

| # | Checkpoint | Status |
|---|---|---|
| 1 | Pemasangan bersih dan layar pertama | selesai |
| 2 | Membuat usaha dan unit | selesai |
| 3 | Rencana usaha: produk, resep/HPP, biaya, periode | selesai |
| 4 | Mencatat penjualan grosir (per dus) dan pengeluaran | selesai |
| 5 | Stok | selesai |
| 6 | Laporan | selesai |
| 7 | Ringkasan dan urutan perbaikan | selesai |

Skenario: usaha keripik pisang. Satu batch produksi menghabiskan Rp2.400.000 dan menghasilkan 400 bungkus.
Dijual ke toko per dus isi 40 bungkus seharga Rp320.000, dan sebagian dititipkan (konsinyasi).

## Catatan per checkpoint

Lingkungan: emulator `Medium_Phone` (1080×2400), APK debug dari commit `ddad1c6` (v0.1.0), data dikosongkan.

### CP1–2. Layar pertama dan membuat usaha (verified)

Yang sudah baik:
- Tidak perlu login. Layar kosong langsung mengarahkan ke satu tombol "Tambah Usaha". Tur ditawarkan, tidak dipaksa.
- Cukup isi nama usaha, unit "Utama" dibuat otomatis, dan Beranda langsung terbuka.

Masalah:
- **Lembar bertumpuk.** "Tambah Usaha" membuka lembar "Usaha dan Cabang / Unit", lalu lembar "Tambah usaha" di
  atasnya. Pengguna baru langsung bertemu konsep cabang/unit sebelum sempat mencatat apa pun.
- **Saklar Rencana | Realisasi langsung muncul tanpa penjelasan.** Kata "Realisasi" dan chip "Mode Aktual" adalah
  istilah akuntansi/proyek, bukan bahasa pedagang.
- **Beranda kosong berisi enam angka Rp0** dengan nama yang tumpang tindih: "Uang kas realisasi", "Uang masuk bersih",
  "Untung / Rugi kas", "Pendapatan Operasional", "Biaya Operasional". Bagi pengguna baru, ini tidak memberi tahu
  langkah berikutnya. Seharusnya Beranda kosong menunjukkan langkah awal: daftar produk, catat transaksi pertama.
- Toast pembuatan usaha menutupi kartu dan tombol +.
- Ikon kanan atas (folder, orang) tanpa label; tidak jelas bahwa folder adalah "Berkas dan Data".

### CP3. Rencana usaha (verified)

Langkah: saklar ke **Rencana**, lalu tab **Catatan** (isinya berubah jadi "Rencana Usaha"). Ditambahkan produk
"Keripik pisang original" (satuan dus, target 50/bulan, harga Rp320.000/dus, resep "Bahan produksi 1 batch"
Rp2.400.000 untuk 10 dus) dan biaya tetap "Gaji 2 karyawan" Rp3.000.000/bulan.

Yang sudah baik:
- Label "Untuk berapa unit" ikut berubah jadi "Untuk berapa dus" setelah satuan diisi.
- Ringkasan biaya per satuan dan margin dihitung langsung di form dan berwarna merah bila rugi.
- Kartu Kelayakan & BEP langsung diperbarui setelah simpan.

Masalah:
- **Rencana tersembunyi di tab bernama "Catatan".** Untuk membuka rencana, pengguna harus tahu bahwa saklar di
  header mengubah isi tab Catatan. Tidak ada jalan yang terlihat dari Beranda.
- **HPP hanya bisa dalam satu satuan.** Pengusaha berpikir "1 batch Rp2,4 juta jadi 400 bungkus, dijual per dus
  isi 40". App memaksa dia menghitung sendiri 400 ÷ 40 = 10 dus lalu mengisi "untuk 10 dus". Inilah keluhan
  "sama aja ngitung lagi". Default "untuk berapa dus = 1" juga menghasilkan biaya Rp2,4 juta per dus (merah)
  sebelum pengguna sadar harus mengubahnya.
- **Kata "HPP" tidak muncul sama sekali.** App memakai "biaya variabel" dan "margin kontribusi", istilah kuliah
  ekonomi. Pengusaha mencari kata HPP dan untung per dus.
- **Tidak ada HPP per pesanan.** Tidak ada cara bertanya "pesanan 25 dus, modalnya berapa, untungnya berapa".
- **"Layak" muncul terlalu dini.** Dengan satu produk dan tanpa biaya tetap, kartu langsung berlabel **Layak**
  dengan "Omzet impas Rp0" dan "BEP 0 dus". Seharusnya "Belum lengkap: isi biaya tetap dulu".
- **R/C Ratio dan B/C Ratio** tampil besar tanpa penjelasan; bagi pedagang angka "1,07" tidak bermakna.
- **Periode default 1 bulan** (bulan berjalan). Rencana usaha biasanya 12 bulan; laba "per bulan" pada periode
  1 bulan tidak memberi gambaran.
- Input uang tanpa pemisah ribuan saat diketik ("2400000"); rawan salah jumlah nol.
- Kategori biaya tetap default "Biaya Lain-lain" walaupun nama berisi "Gaji"; harus dipilih manual.
- Field "Mulai dijual bulan ke-" selalu tampil walaupun hanya relevan untuk usaha musiman.

### Bug: bottom sheet "balapan" dengan keyboard (user-confirmed, sebagian verified)

Laporan pemilik: sheet naik-turun dan keyboard berkedip saat mengisi form.

Terlihat di emulator (verified):
- Saat keyboard muncul, area isi sheet menyusut dan field yang difokus berada di tepi bawah; field di bawahnya
  (Biaya, Untuk berapa dus) terpotong sampai digulir.
- **Dropdown Kategori saat keyboard terbuka:** keyboard menutup, sheet turun ke bawah, tetapi menu dropdown
  muncul di posisi lama sehingga menutupi field di atasnya.

Dugaan penyebab (inferred, belum diuji perbaikannya), `presentation/components/MankeuSheet.kt`:
- `imePadding()` dipasang pada kolom di dalam `ModalBottomSheet`, sedangkan isi kolom memakai
  `animateContentSize(tween(...))`. Saat keyboard animasi naik/turun, tinggi kolom berubah tiap frame, lalu
  `animateContentSize` ikut menganimasikan perubahan itu dengan tween sendiri. Dua animasi mengejar satu sama lain
  sehingga sheet memantul.
- Komentar di berkas menyebut `skipPartiallyExpanded = false` (ada snap setengah), tetapi kodenya `true`.
  Dokumentasi dan kode tidak sesuai.
- Keyboard emulator adalah input suara Google, jadi kedipan keyboard sungguhan (Gboard) belum bisa diamati di sini.
  Perlu diuji di HP nyata setelah perbaikan.

### Bug: menggeser untuk scroll malah menutup sheet (user-confirmed, verified)

Terulang di emulator pada form "Uang masuk": isi digulir ke bawah, lalu pengguna menggeser ke bawah untuk kembali ke
atas. Begitu isi sudah di posisi paling atas, gerakan yang sama langsung menyeret sheet turun dan menutupnya.
Isian tidak hilang karena form transaksi menyimpan draf, tetapi pengguna kaget dan harus membuka ulang.

Penyebab (inferred dari kode `MankeuSheet.kt`): semua form memakai `ModalBottomSheet` dengan seret-untuk-tutup aktif.
Scroll isi dan seret sheet berbagi satu gerakan (nested scroll), jadi sisa gerakan scroll ke atas diteruskan ke sheet.
Ini perilaku bawaan Material 3 dan cocok untuk lembar pendek, bukan form panjang.

Arah perbaikan (proposed): form panjang (transaksi, produk, rencana) tidak boleh tertutup oleh seret isi. Pilihannya
layar penuh, atau sheet dengan `confirmValueChange` yang menolak `Hidden` dari seret selama form berisi perubahan
(tutup hanya lewat Batal/Back, dengan konfirmasi bila ada isian). Lembar pendek (konfirmasi, pilihan) tetap boleh
diseret tutup.

### CP4. Mencatat penjualan per dus dan pengeluaran (verified)

Langkah: saklar ke **Realisasi**, tab Catatan, "Catat sekarang". Uang masuk: pilih produk "Keripik pisang original",
buka rincian, isi volume 5. Simpan. Lalu dibuka "Catat Pengeluaran" untuk melihat formnya (tidak disimpan).

Yang sudah baik:
- Memilih produk mengisi satuan (dus) dan harga satuan (Rp320.000). Volume 5 langsung menghitung jumlah
  Rp1.600.000 dengan keterangan "Dihitung otomatis: 5 dus × Rp320.000".
- Form transaksi menyimpan draf: setelah sheet tertutup tak sengaja, isian kembali saat dibuka ulang.
- Tombol cepat +50 rb … +5 jt.

Masalah:
- **Urutan form terbalik untuk penjual.** Yang pertama diminta "Jumlah uang", sedangkan produk dan volume ada di
  bawah, sebagian tersembunyi di "Tambah rincian". Penjual berpikir "jual 5 dus keripik", bukan "terima Rp1,6 juta".
- **Penjualan tidak menunjukkan HPP atau untung.** Padahal produk sudah punya biaya Rp240.000/dus di Rencana.
  Seharusnya tampil "modal Rp1.200.000, untung Rp400.000".
- **Hanya tunai.** Teks form sendiri berkata "yang langsung dibayar". Tidak ada pilihan "belum dibayar",
  "dititipkan (konsinyasi)", atau "bayar sebagian". Penjualan ke toko yang bayar belakangan tidak bisa dicatat
  dengan benar.
- **Kartu di daftar hanya menampilkan keterangan dan kategori** ("Tes · Penjualan & Jasa"), bukan "5 dus Keripik
  pisang original".
- **Bahasa akuntan:** toast "Transaksi tersimpan; jurnal debit dan kredit seimbang", pilihan "Sifat biaya: Ikut
  kategori / Tetap / Variabel" di form pengeluaran.
- Pengeluaran "Bahan Baku & Stok" tidak menawarkan pilihan untuk menambah stok bahan (mungkin baru muncul bila sudah
  ada barang di Stok; diperiksa di CP5).
- Tanggal default hari ini sudah benar; kalender dibuka dengan dialog terpisah di atas sheet.

### CP5. Stok (verified)

- **Produk dari Rencana tidak ada di Stok.** Setelah produk "Keripik pisang original" dibuat dan 5 dus terjual, tab
  Stok tetap kosong ("Belum ada barang"). Produk (Rencana), barang (Stok), dan penjualan (Catatan) adalah tiga daftar
  yang tidak saling kenal. Pengguna harus membuat barang yang sama sekali lagi di Stok.
- Penjualan 5 dus tidak mengurangi stok apa pun.
- Saklar Rencana | Realisasi tetap tampil di Stok walaupun stok tidak punya versi rencana. Ini membingungkan
  karena pengguna tidak tahu apakah stok ikut berubah saat saklar dipindah.

### CP6. Laporan (verified)

Tab Laporan punya tiga sub-tab: "Laba Rugi dan Arus Kas", "Kelayakan & BEP", "Realisasi vs Target".

Yang sudah baik:
- "Realisasi vs Target" berguna: rencana dan realisasi berdampingan beserta selisihnya. Isi ini seharusnya jadi
  pusat perhatian, bukan sub-tab ketiga.
- Grafik batang kas bulanan jelas.

Masalah:
- **Laba Rugi menyesatkan tanpa HPP.** Menjual 5 dus menghasilkan "Untung / Rugi kas Rp1.600.000". Modal produksi
  5 dus (Rp1.200.000 menurut resep) tidak dikurangi karena laporan murni basis kas. Pengusaha yang membaca ini
  mengira untung Rp1,6 juta. Di "Realisasi vs Target", laba realisasi (Rp1,6 juta) bahkan tampil hijau lebih tinggi
  dari rencana (Rp1 juta).
- **Scroll tanpa ujung (verified dari kode):** `ReportsScreen.kt:267` membuat satu kartu besar penuh per bulan
  (`monthlyCashFlow.forEach`), masing-masing lima baris. Dua tahun data berarti 24 kartu besar berurutan, tanpa
  ringkasan, lipatan, atau tabel.
- Istilah laporan keuangan formal: "Arus Kas Operasi / Investasi / Pembiayaan", "Saldo Kas Kumulatif", "basis kas,
  sebelum penyusutan". Benar secara akuntansi, tetapi bukan bahasa pedagang.
- Subjudul "Dihitung otomatis dari catatan transaksi dan anggaran" memakai kata "anggaran", padahal di layar lain
  disebut "Rencana".

### CP7. Ringkasan dan urutan perbaikan (proposed)

Akar masalah: app dibangun dari sisi akuntansi (buku, jurnal, arus kas, kelayakan), sedangkan pengusaha berpikir
dari sisi **produk dan pesanan** ("jual 25 dus ke toko A, belum bayar, untung berapa"). Excel terasa jelas karena
satu sheet untuk satu keperluan dengan nama yang sesuai isinya.

Urutan usulan:

1. **Bug sheet** (kecil, sangat terasa): form panjang tidak tertutup oleh seret; perbaiki pantulan saat keyboard
   (`MankeuSheet.kt`). Diuji ulang di HP nyata.
2. **Produk sebagai pusat:** satu daftar produk yang dipakai Rencana, Stok, dan penjualan. HPP diisi per batch
   produksi (biaya batch dan hasilnya, misal 400 bungkus) dengan satuan jual dan isinya (dus isi 40). App yang
   mengonversi. Kata "HPP" dan "untung per dus" dipakai.
3. **Penjualan berbasis produk:** pilih produk dan jumlah lebih dulu; jumlah uang, HPP, dan untung keluar sendiri.
   Laporan Laba Rugi mengurangi HPP barang yang terjual (laba kotor), terpisah dari arus kas.
4. **Utang-piutang dan konsinyasi:** penjualan "belum dibayar / titip jual / bayar sebagian", daftar tagihan per
   pelanggan, pelunasan otomatis jadi uang masuk. Perlu DEC baru yang menggantikan DEC-011.
5. **Navigasi tanpa saklar global:** Rencana jadi halaman sendiri; Laporan menampilkan rencana dan realisasi
   berdampingan. Unit/cabang disembunyikan sampai diperlukan. Beranda kosong menunjukkan langkah awal.
6. **Laporan ringkas:** ringkasan di atas, rincian per bulan dalam tabel atau dapat dilipat.
7. **Bahasa pedagang:** ganti jurnal, debit-kredit, biaya variabel, margin kontribusi, R/C, B/C, arus kas investasi
   dengan istilah sehari-hari; istilah formal tetap ada di ekspor/PDF bila perlu.
8. **Privasi:** kunci app (PIN/biometrik) dan `FLAG_SECURE`.
9. **Rencana:** periode "mulai bulan X, selama N bulan" (default 12) hanya di Rencana; label "Layak" baru muncul
   bila biaya tetap sudah diisi; "Mulai dijual bulan ke-" jadi opsi lanjutan.

Belum diuji di sesi ini: ekspor/impor Excel, backup/sinkron Drive, PDF, mode gelap, multi-unit.
