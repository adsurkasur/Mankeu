# Review independen PK-0.3: Paket B

Status: selesai ditinjau; Paket B belum memenuhi seluruh penerimaan PK-0.3.
Reviewer: Codex (GPT-6). Tanggal: 2026-11-12 02:14 WIB.
Lingkup: diff `337dde4..b217794`; checkout `d88fb09`, branch `fase2-nota-piutang`.

## Dasar dan batas bukti

- [terverifikasi] Status Git awal bersih. Diff disimpan di `C:/Users/super/AppData/Local/Temp/mankeu-PK-0.3.diff`. Empat berkas ledger/tes utama tidak berbeda antara b217794 dan HEAD.
- [terverifikasi] Dibaca: AGENTS.md, rencana induk §3/4.16/7/8, PK-0.3, Lampiran A §13, bagian uji Catatan di Lampiran C, B-laporan, DEC-031, diff, kedua tes ledger, serta jalur pemuatan, ringkasan, ubah dan hapus yang disebut di bawah. PK-0.3 tidak membutuhkan jawaban K.
- [terverifikasi] Bukti di laporan ini adalah penelusuran statis dan contoh aritmetika, bukan hasil menjalankan aplikasi. Dua peninjau pembantu memeriksa standar dan spesifikasi secara hanya baca.
- [dilaporkan] B-laporan menyatakan unit 140/140, instrumented ledger 4/4, SaleNoteFlow 8/8 dan lint lulus. Hasil tersebut tidak dijalankan ulang atau dianggap bukti penerimaan skenario yang belum diuji.

## Temuan standar

### F1 · Important · Batas 200 transaksi memotong hari dan menyembunyikan pembayaran

[terverifikasi] Lokasi utama: `app/src/main/java/com/mankeu/presentation/UnifiedLedger.kt:113` dan `:184`; sumber batas: `data/FinanceRepository.kt:182`, `data/FinanceDao.kt:201`; pemakai: `presentation/AppViewModel.kt:273`, `screens/EntryScreen.kt:90`.

Pemicu: simpan 201 pengeluaran operasional masing-masing Rp1.000 pada satu tanggal. Query LIMIT 200 memasok Rp200.000 ke judul hari, sedangkan uang tercatat dan MonthlySummary bulan itu Rp201.000. Notice pemangkasan di EntryScreen:242 tidak melengkapi total hari. Kasus pembayaran: nota pada tanggal D, pembayaran pada D+1, lalu tambahkan 200 event lebih baru. Nota tetap termuat, tetapi pembayaran D+1 lenyap pada semua chip; hanya pembayaran bertanggal nota yang dibangun dari SaleRecord.payments.

Akibat: judul hari tampak lengkap padahal kurang; pembayaran yang sah tidak dapat ditemukan dalam daftar/filter. Ini mematahkan butir wajib 2, 3, dan 4. LIMIT sudah ada sebelumnya; Paket B memakai subset itu sebagai total harian baru dan gagal memenuhi penerimaan baru.

Perbaikan konkret [usulan]: muat halaman berdasarkan batas tanggal, sertakan seluruh event pada tanggal terakhir, dan gabungkan pembayaran dari sumber lengkap dengan deduplikasi id. Gunakan agregasi harian dari data lengkap. Jangan menampilkan total lengkap dari subset.

Tes yang harus menangkap: integrasi Room → state → ledger dengan 201 pengeluaran satu hari (Rp201.000); batas yang jatuh di tengah hari; pembayaran D+1 di luar 200 event harus tetap terjangkau melalui pemuatan hari berikutnya. Rekonsiliasi sebulan setelah semua halaman dimuat.

### F2 · Important · Definisi total harian berbeda dari MonthlySummary

[terverifikasi] Lokasi: `app/src/main/java/com/mankeu/presentation/UnifiedLedger.kt:53`, `:184`; `domain/MonthlySummary.kt:12`, `:46`; `domain/PostingReportCalculator.kt:32`; `domain/FinanceModels.kt:103`.

Pemicu: satu bulan hanya berisi modal Rp1.000.000 dan pembelian aset tunai Rp200.000. Ledger menghitung total harian masuk Rp1.000.000 dan keluar Rp200.000. MonthlySummary.moneyInRupiah dan moneyOutRupiah bernilai Rp0/Rp0 karena hanya kas operasional; modal merupakan FINANCING dan aset INVESTING. Ini tetap berbeda walau seluruh data termuat dan filter Semua tanpa pencarian.

Akibat: butir wajib 3 salah. Aturan uang nyata harian dan kesamaan dengan ringkasan operasional saling bertentangan untuk modal/prive/aset. Ledger juga menghitung ringkasan sendiri tanpa kontrak agregasi bersama. Tidak ditemukan perhitungan ulang untung, uang di tangan, atau belum bayar dalam Paket B.

Perbaikan konkret [usulan]: setelah pemilik memilih kontrak di bawah, sediakan agregasi bersama untuk angka harian dan bulanan sesuai kontrak itu; pertahankan nilai operasional MonthlySummary yang sekarang sampai keputusan tercatat. Tambahkan tes rekonsiliasi yang memasukkan modal, prive, aset, penjualan, pengeluaran operasional, dan belanja tempo tanpa kas.

## Temuan spesifikasi

### F3 · Important · Metadata pembayaran terserap hilang dari pencarian

[terverifikasi] Lokasi: `app/src/main/java/com/mankeu/presentation/UnifiedLedger.kt:93`, `:113`, `:160`.

Pemicu: nota lunas pada hari penerbitan, pelanggan dan produk tidak bernama Penjualan. Cari kategori pembayaran `Penjualan` atau keterangan `Bayar nota` pada Semua/Uang masuk. Event pembayaran sudah diserap, tetapi pencarian Sale hanya membaca pelanggan, ringkasan produk, dan catatan nota; hasil kosong. Pembayaran hari berikutnya cocok melalui kategori/keterangan SalePayment. Referensi pembayaran juga tidak dibaca pada kedua representasi nota/pembayaran.

Akibat: pembayaran hilang dari seluruh hasil chip saat pencarian aktif, meskipun data tetap tersimpan. Tanpa pencarian dan dengan event lengkap, pembayaran positif terserap tetap muncul pada Semua dan Uang masuk; tidak tampil pada Belum lunas setelah lunas adalah penyaringan yang sesuai.

Perbaikan konkret [usulan]: cocokkan metadata pembayaran sebelum penyerapan, lalu tampilkan representasi nota yang menyerapnya; sertakan keterangan, kategori, dan referensi seluruh pembayaran yang diwakili. Tes: pembayaran sama hari dan beda hari dicari dengan setiap metadata tersebut pada Semua/Uang masuk; jumlah kas tidak berubah dan tidak ganda.

### F4 · Important · Filter lewat jatuh tempo di Catatan terhapus

[terverifikasi] Lokasi: `app/src/main/java/com/mankeu/presentation/MankeuApp.kt:248`; `screens/EntryScreen.kt:81`, `:175`. Pembanding: EntryScreen pada 337dde4 memanggil saleNoteItems; `screens/SaleNoteScreens.kt:509`, `:529` menyediakan predikat dan pilihan OVERDUE.

Pemicu: satu nota belum lunas sudah lewat tempo dan satu belum tempo. Sebelum Paket B, chip lewat jatuh tempo menyaring nota pertama. Sesudah Paket B chip itu tidak tersedia; noteFilter diterima tetapi tidak dibaca dan OVERDUE dipetakan ke UNPAID. Home saat ini memanggil OPEN, jadi tidak diklaim ada tombol Home lewat tempo yang rusak.

Akibat: kapabilitas penyaringan yang sudah ada hilang, bertentangan dengan V-4 yang ditegaskan pemilik.

Perbaikan konkret [usulan]: pertahankan pilihan/predikat lewat tempo pada ledger terpadu (boleh melalui kontrol lanjutan), serta teruskan intent OVERDUE tanpa mengubahnya menjadi semua belum lunas. Tes Compose: dua nota di atas, pilih lewat tempo dan pastikan hanya nota terlambat muncul; uji intent OVERDUE terpisah.

## Putusan tiap skenario wajib 1

Semua contoh memakai nota Rp600.000, tanpa transaksi lain, data sumber lengkap dan konsisten. Putusan benar bersifat statis; bukan klaim alur emulator sudah lulus. F1 membatalkan jaminan umum saat daftar terpangkas.

| Skenario | Putusan dan bukti |
|---|---|
| Lunas hari nota D | [terverifikasi] **Benar**: sumOf pembayaran D memberi Rp600.000 pada Sale, id event diserap, total masuk D Rp600.000 sekali (UnifiedLedger:93–99,113,184). Tes absorbsInitialNotePaymentOnSameDayWithoutDuplicateRowOrDoubleCounting mencakup bentuk ini, tetapi fixture memakai id pembayaran berbeda di SaleRecord dan events; belum membuktikan integrasi sumber nyata. |
| Uang muka D, pelunasan D+1 | [terverifikasi] **Benar**: Rp200.000 diserap ke D dan event Rp400.000 menjadi SalePayment D+1 (:93–95,115–124). Tes unpaidNoteShowsZeroCashAndLaterPaymentAppearsOnPaymentDate hanya menguji uang muka nol, sehingga kombinasi uang muka positif belum diuji. |
| Dua pembayaran pada D | [terverifikasi] **Benar**: Rp200.000 + Rp400.000 dijumlahkan dari payments, kedua id diserap; D Rp600.000 (:93–99). Belum ada tes khusus dua pembayaran. |
| Nota awal D dibayar D lalu tanggal diubah | [terverifikasi] **Benar** jika dimundurkan ke D−1: pembayaran tetap D, tidak lagi diserap, D−1 Rp0 dan D Rp600.000. Memajukan nota ke D+1 ditolak sebelum menulis (FinanceRepository:1097–1099); transaksi lama tetap. saveSale mempertahankan payments (:1086–1089). Belum ada tes ledger perubahan tanggal. |
| Nota dihapus sesudah dibayar | [terverifikasi] **Benar terhadap data tercatat setelah penghapusan**: FinanceRepository:1145–1149 menghapus pembayaran dan nota dalam withTransaction; kontribusi keduanya menjadi Rp0. SaleNoteScreens:733–743 dan strings.xml:715 memperingatkan pembayaran ikut terhapus dan tidak bisa dibatalkan. Kas fisik tidak berubah akibat menghapus catatan; tidak diklaim Rp0 mewakili uang fisik yang masih ada. Perilaku ini sudah ada sebelum Paket B, bukan kehilangan pembayaran diam-diam karena penyerapan. Belum diuji ulang melalui Room/UI. |

## Putusan pemeriksaan lain

- [terverifikasi] Butir 2 **salah secara umum**: F1 menghilangkan pembayaran dari semua chip; F3 menghilangkannya dari pencarian pada semua chip. Penyerapan sendiri tidak menghilangkan pembayaran positif dari Semua/Uang masuk bila sumber lengkap dan pencarian kosong.
- [terverifikasi] Butir 3 **salah**: F1 dan F2. Untuk event operasional lengkap, konsisten, filter Semua dan pencarian kosong, penyerapan menjaga penjumlahan kas secara aljabar. Belum ada tes lintas MonthlySummary dalam UnifiedLedgerTest.
- [terverifikasi] Butir 4 **salah**: LIMIT per baris, bukan per tanggal (F1).
- [terverifikasi] Uang pada jalur ledger menggunakan Long dan sumOf; tidak ada Double/Float untuk uang atau pembulatan baru. Float pada alpha UI bukan uang. Ledger memakai total/sisa SaleRecord dan tidak mengalikan ulang jumlah × harga.
- [belum] Audit pembulatan seluruh jalur pembentukan nota/HPP, overflow Long, kondisi emisi Flow yang sesaat tidak konsisten, dan penyimpanan di luar operasi nota yang ditelusuri. Tidak ada perubahan perhitungan domain/persistensi dalam diff Paket B yang ditinjau.
- [terverifikasi] B-laporan menyebut benchmark kurang dari 50 ms; tes buildsScaleLedgerUnderFiftyMilliseconds sebenarnya mengasersi kurang dari 500 ms. [belum] Waktu runtime kini. Ini koreksi bukti laporan, bukan bukti masalah kas atau hasil benchmark baru.

## Pertanyaan keputusan pemilik

[belum] Kontrak mana yang harus dipakai untuk modal/prive/aset pada total harian?
A. Total tetap seluruh kas nyata; tambahkan angka seluruh kas terpisah di MonthlySummary dan ubah kriteria rekonsiliasi agar memakai angka tersebut.
B. Total harian hanya operasional agar sama dengan moneyInRupiah/moneyOutRupiah; beri label operasional dan tampilkan modal/prive/aset sebagai baris tanpa memasukkannya ke total itu.

Kedua pilihan [usulan], belum diputuskan reviewer. Jangan mengganti arti angka ringkasan sekarang secara diam-diam.

## Yang tidak diperiksa dan penyerahan

[belum] Tidak menjalankan Gradle, unit test, lint, emulator, tes instrumented, suite penuh, pengukuran jank/FAB, ekspor/impor, migrasi, backup, sinkron, perangkat nyata, atau seluruh alur aplikasi. Tidak mengaudit paket lain. Lampiran A §13 dan Lampiran C yang dibaca mengatur pekerjaan lanjutan; perubahan bahasa/tombol untuk paket itu tidak dijadikan temuan PK-0.3.

[terverifikasi] Tidak ada kode yang diubah. Review selesai dengan 4 temuan Important, 0 Critical, 0 Minor pada daftar temuan; kontradiksi kontrak F2 menunggu pemilik. Perbaikan diserahkan ke PK-0.5 setelah PK-0.4; paket tersebut tidak dikerjakan di sesi ini. Bukti penerimaan baru yang diperlukan sudah dirinci per temuan.
