# Spec Redesign Mankeu (2026-10-29)

Status: disepakati pemilik lewat grill 2026-10-29 (20 keputusan). Arah produk: DEC-025.
Bukti masalah: `docs/audit/2026-10-29_UJI_ALUR_PENGUSAHA.md` (uji emulator per checkpoint).

## Masalah yang diselesaikan

Pengusaha berpikir dari sisi **produk dan pesanan** ("jual 25 dus ke Toko A, belum bayar, untung berapa"),
sedangkan Mankeu dibangun dari sisi akuntansi (buku Rencana/Realisasi, jurnal, arus kas, kelayakan).
Akibatnya HPP harus dihitung sendiri, laba terlihat lebih besar dari kenyataan, penjualan kredit dan titip jual
tidak bisa dicatat, dan alur Rencana/Catatan membingungkan.

## Keputusan (user-confirmed, grill 2026-10-29)

| # | Keputusan |
|---|---|
| 1 | **Mulai bersih.** Migrasi schema 3→11 dan pembersih data lama dihapus. Tester instal ulang atau impor Excel. |
| 2 | **Buku utang-piutang:** piutang pelanggan (bayar belakangan / sebagian), konsinyasi, utang pemasok (beli tempo). Tanpa pinjaman, bunga, cicilan bank. Menggantikan DEC-011 K-1. |
| 3 | **Laba diakui saat terjual** = penjualan − HPP produk terjual − biaya. Arus kas ditampilkan terpisah. |
| 4 | **Navigasi:** Beranda · Catat · Produk · Laporan. Saklar global Rencana/Realisasi dihapus. Rencana Usaha dibuka dari Beranda. Laporan menampilkan rencana vs realisasi berdampingan. |
| 5 | **Satuan:** satu satuan dasar (bungkus) + beberapa satuan jual dengan isi (dus = 40 bungkus, bal = 10 dus). Harga per satuan jual boleh berbeda. App yang mengonversi. |
| 6 | **HPP dari resep per batch:** biaya 1 batch dan hasilnya (N satuan dasar). HPP per satuan dasar, per satuan jual, dan per nota dihitung otomatis. Penjualan menyimpan HPP pada saat itu (snapshot). |
| 7 | **Konsinyasi:** titip N ke toko (stok pindah ke "dititipkan") → toko lapor laku X (jadi penjualan + piutang) → toko setor (piutang lunas). Sisa bisa ditarik kembali ke stok. |
| 8 | **Firebase Auth dibuang.** Akun Google dipakai langsung (Google Sign-In yang sudah ada). Dua jalur salinan data: file backup terenkripsi + sinkron Drive terenkripsi. Backup Drive terpisah digabung ke sinkron. |
| 9 | **Stok produk otomatis** dalam satuan dasar. Tombol **Produksi** menambah stok (1 batch = +hasil). Penjualan dan titip mengurangi. Bahan baku = daftar terpisah, opsional, tidak dikurangi otomatis oleh produksi. |
| 10 | **Template Excel versi baru saja** (Produk + satuan jual + resep, Penjualan, Piutang/Utang, Konsinyasi). Template lama ditolak dengan pesan jelas + tombol unduh template baru. Ekspor tetap bisa diimpor ulang tanpa kehilangan data (DEC-012 tetap berlaku untuk format baru). |
| 11 | **Kunci app opsional** (PIN / sidik jari), ditawarkan sekali setelah usaha pertama dibuat, diatur di Pengaturan. Saat aktif, isi app disembunyikan di daftar app terbaru; screenshot tetap boleh. |
| 12 | **Bertahap:** setiap fase selesai, diuji, dan dirilis ke internal testing sebelum fase berikutnya. Tidak ada tenggat. |
| 13 | **Form panjang = halaman penuh** dengan Simpan tetap di bawah. Bottom sheet hanya untuk pilihan pendek dan konfirmasi. |
| 14 | **Rencana Usaha tanpa periode:** "sebulan normal" (penjualan per produk, biaya tetap) + "modal awal" (peralatan + biaya persiapan). Hasil: untung per bulan, titik impas (satuan jual per bulan), modal dibutuhkan, balik modal N bulan. R/C dan B/C di "Rincian analisis" + PDF. "Layak" hanya bila biaya tetap terisi. Input tanggal periode dihapus. |
| 15 | **Unit/cabang disembunyikan** sampai pengguna menambah cabang kedua lewat menu Usaha. |
| 16 | **Tur spotlight dihapus.** Pusat Bantuan dipertahankan; artikel ditulis ulang di fase terakhir. |
| 17 | **Nota multi-produk:** satu penjualan berisi beberapa baris produk; status bayar (lunas / belum / sebagian / titip) untuk seluruh nota. |
| 18 | **Jatuh tempo:** kartu di Beranda + notifikasi HP (AlarmManager bawaan Android, tanpa library baru; izin `POST_NOTIFICATIONS` di Android 13+). |
| 19 | **Bahasa sehari-hari di layar** (untung, modal, HPP, uang masuk/keluar, tagihan). Istilah formal (arus kas operasi/investasi/pembiayaan, R/C, B/C, debit/kredit) hanya di PDF dan "Rincian analisis". |
| 20 | **Harga bahan berfluktuasi:** komponen resep = bahan dari daftar bahan + jumlah per batch (atau baris biaya bebas seperti gas, ongkos). Harga bahan = harga beli terakhir yang tercatat. Saat belanja bahan dengan harga baru, HPP produk yang memakainya ikut berubah dan muncul peringatan dampaknya ke untung per satuan jual. Penjualan lama tetap memakai HPP snapshot. |

## Batasan global (berlaku untuk semua fase)

- Uang selalu `Long` rupiah bulat; tidak ada floating point untuk uang tersimpan. Pembagian HPP dibulatkan ke rupiah
  dengan aturan yang sama di semua tempat (setengah ke atas) dan diuji.
- Kuantitas boleh pecahan (kg, liter) dan memakai `BigDecimal` seperti `TransactionCalculation.parseQuantity`.
- Operasi lokal tidak bergantung pada login, jaringan, Firebase, atau server milik app.
- Tidak ada dependensi baru tanpa alasan tertulis di DEC. Target pengurangan: Firebase Auth, Vico.
- Laporan adalah turunan; tidak boleh jadi tempat input kedua.
- Setiap fase: tes unit domain, tes instrumented untuk alur utama, uji emulator alur pengusaha, log
  (`ACTIVITY_LOG`, `DECISION_LOG`, `CHANGELOG`) di commit yang sama, push + pointer superproject.

## Hasil ponytail-audit 2026-10-29 (masukan untuk Fase 0)

1. `delete:` migrasi 3→11, `StrictCashCleanup`, `ReversalCleanup`, `LegacyPlanConversion`, cabang konversi backup lama
   (`data/AppDatabase.kt:62-368`, `backup/EncryptedBackupManager.kt:286-306`). `StrictCashCleanup` menghapus baris utang
   saat restore, bertentangan dengan keputusan #2.
2. `native:` Firebase Auth hanya menyimpan nama/email; token Drive sudah lewat `DriveTokenProvider` (Google Sign-In).
3. `yagni:` tur spotlight (`components/SpotlightTour.kt`, `guide/TourHost.kt`, target di 10 layar) menjelaskan alur yang
   akan dirombak.
4. `native:` Vico (2 dependensi) untuk satu grafik batang → Compose `Canvas` (Fase 4).
5. ~~`delete:` `SkeletonMetricGrid` tidak dipakai.~~ Temuan ditarik: `SkeletonMetricGrid` dipakai di `HomeScreen.kt:139` (keadaan memuat).
6. `yagni:` tiga jalur salinan data → dua (keputusan #8, Fase 5).
7. `shrink:` `MankeuApp.kt` 1.368 baris, menyusut saat saklar global dihapus (Fase 3).

## Keputusan Fase 2 (user-confirmed, grill 2026-10-29, sebelum rencana Fase 2 ditulis)

| # | Keputusan |
|---|---|
| F2-1 | **Untung dan belanja produksi:** belanja berkategori biaya variabel (bahan, kemasan, gas; sifat biaya `VARIABLE`) tidak dikurangkan langsung dari untung; yang dikurangkan HPP produk terjual. Biaya tetap tetap biaya. Laporan menampilkan baris selisih "belanja variabel bulan ini vs HPP terjual" supaya tidak ada selisih tak terjelaskan. Arus kas tetap mencatat semua uang keluar. |
| F2-2 | **Daftar kontak** (satu layar Kontak): pelanggan dan pemasok dalam satu daftar (bisa keduanya), dengan nama, no. HP, alamat, catatan. Nota memilih pelanggan; belanja tempo memilih pemasok. |
| F2-3 | **Pelanggan di nota** wajib hanya bila nota belum lunas, dibayar sebagian, atau titip; nota lunas boleh tanpa pelanggan (tampil "Umum"). |
| F2-4 | **Data lama:** saat update, setiap uang masuk berproduk menjadi nota lunas satu baris; HPP baris diisi HPP saat update dan ditandai perkiraan. Semua penjualan produk lewat satu jalur (nota). |
| F2-5 | **Beli tempo ke pemasok:** belanja berstatus belum dibayar dengan jatuh tempo opsional; biaya diakui tanggal beli (untung), uang keluar dicatat saat dibayar (arus kas); bayar sebagian boleh; sisa tampil di daftar utang. |
| F2-6 | **Konsinyasi:** harga setor per satuan ditetapkan saat titip; laporan laku membuat nota otomatis dengan harga itu (bisa diubah di nota). |
| F2-7 | **Diskon:** satu diskon rupiah per nota; total = jumlah baris − diskon; untung memakai total setelah diskon. |
| F2-8 | **Belanja yang diwakili HPP (mengoreksi F2-1, grill 2026-10-30):** bukan semua biaya bersifat variabel, melainkan kategori pengeluaran yang bertanda **"Biaya produksi (sudah dihitung di HPP)"**. Bawaan menyala hanya untuk "Bahan Baku & Stok"; pemilik bisa menyalakannya untuk kategori lain (gas, kemasan). Belanja berkategori itu diganti HPP (dengan baris selisih); semua belanja lain, termasuk transport dan usaha jasa tanpa produk, tetap mengurangi untung. Alasan: uji Task 4 menunjukkan Transportasi (variabel bawaan) ikut hilang dari untung. |

## Keputusan setelah audit UI/UX (user-confirmed, 2026-10-30)

Dasar: `docs/audit/2026-10-30_AUDIT_UI_UX.md` (32 layar, pola P1–P7).

| # | Keputusan |
|---|---|
| U-1 | **Rombak tampilan dulu**, sebelum sisa Fase 2: pola form baru, Beranda baru, satu angka untung di semua layar, saklar Rencana/Realisasi dihapus (isi Fase 3 ditarik maju). Titip dan belanja tempo (F2 Task 6) dibuat sesudahnya dengan pola baru. |
| U-2 | **Stok bahan nyata:** belanja yang menautkan bahan menambah stok bahan; Produksi mengurangi bahan sesuai resep (jumlah per batch × batch). Koreksi lewat "Hitung ulang stok". Menggantikan spec #9 bagian "tidak dikurangi otomatis oleh produksi" dan DEC-028 §3. |
| U-3 | **Kirim nota / pengingat tagihan** lewat menu bagikan Android (teks rapi, tanpa izin atau akun baru) dan **jatuh tempo cepat** (7 / 14 / 30 hari) saat nota belum lunas. |
| U-4 | **Tab Catatan satu daftar per tanggal**: nota, pembayaran, belanja, uang masuk lain, dengan filter (Semua / Belum lunas / Uang masuk / Uang keluar). |
