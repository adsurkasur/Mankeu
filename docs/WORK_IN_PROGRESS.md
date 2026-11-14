# Work in Progress (living document)

**Modal manual rencana (2026-11-14 16:26 WIB):** [terverifikasi] Modal manual non-dagang tanpa resep, termasuk nol, dihitung per isi satuan jual dan ditampilkan pada Rencana. PlanAssemblyTest serta 5/5 PlanProductsTest dan build lulus. [belum] Harga beli terakhir barang dagangan, UI modal manual, sinkron dan seluruh MVP.


**Kelayakan dan modal belum lengkap (2026-11-14 16:23 WIB):** [terverifikasi] Status biaya tidak lengkap mencegah kesimpulan rencana dan perbandingan pada layar serta angka turunan Excel/PDF; titik impas aktual ditahan bila biaya standar produk terjual belum lengkap. Lima tes layar/PDF pada emulator-5558, 199/199 JVM dan build lulus; teks PDF sintetis diperiksa. Laporan `docs/superpowers/reports/2026-11-14-kelayakan-modal-belum-lengkap.md`. [belum] Verifikasi visual PDF, dukungan modal manual/harga dagangan di rencana, sinkronisasi dan seluruh MVP.


**Modal pada daftar rencana (2026-11-14 16:14 WIB):** [terverifikasi] Nominal modal yang belum lengkap diganti tanda kosong dengan peringatan, sementara resep lengkap tetap menampilkan nominal; PlanProductsTest 2/2 pada emulator-5558 dan build lulus. [belum] Telusuri agregasi kelayakan rencana saat modal belum lengkap dan seluruh gerbang MVP.


**Modal di editor produk (2026-11-14 16:12 WIB):** [terverifikasi] Pratinjau modal dan untung ditahan bila resep kosong atau belum lengkap; dua tes baru dan kontrol resep lengkap 3/3 pada emulator-5558, build dan JVM 196/196 lulus. [belum] `PlanScreen` masih menyertakan nominal HPP parsial dalam baris meski ada peringatan; perbaiki sebelum menyatakan perhitungan rencana aman. Audit visual dan MVP belum selesai.


**Satuan pada nota (2026-11-14 16:08 WIB):** [terverifikasi] Dropdown menampilkan satuan produk dan mempertahankan harga otomatis; dua alur nota dan dua regresi produk/satuan lulus pada emulator-5558, build debug/androidTest berhasil. [belum] Audit visual seluruh layar, perangkat dan gerbang MVP.


**Pesan batas stok (2026-11-14 16:05 WIB):** [terverifikasi] Produk menyatakan bila belum ada batas menipis; jika ada batas tetapi tidak ada peringatan, teks tidak lagi mengklaim semua barang di atas minimum. Tes stok nol/tanpa batas dan stok minus 2/2 lulus pada emulator-5558; build debug/androidTest lulus. [belum] Audit visual menyeluruh, semua perangkat dan gerbang MVP.


**Validasi satuan jual (2026-11-14 16:02 WIB):** [terverifikasi] Nama, isi dan harga pada sheet satuan jual mengikuti validasi blur dan fokus saat Simpan. ProductEditorTest 9/9 dan tes fokus isi/harga tambahan 1/1 lulus pada emulator-5558; build debug dan androidTest lulus. [belum] Form lain, audit visual dan seluruh gerbang MVP.


**Validasi kontak (2026-11-14 15:59 WIB):** [terverifikasi] Nama kosong dan duplikat mengikuti validasi blur, koreksi dan fokus saat Simpan; tiga tes instrumentasi lulus pada emulator-5558, build debug dan tes berhasil. [belum] Form lain, audit visual dan seluruh gerbang MVP.


**Bukti kategori historis (2026-11-14 15:56 WIB):** [terverifikasi] Migrasi 13→14 mempertahankan dua belanja sintetis dengan ID kategori null, nilai, label dan tautan stok; pada database terpisah koreksi melalui Catatan menghilangkan ketidakpastian ringkasan tanpa menggandakan uang keluar. Kedua tes lulus bersama pada emulator-5558. [belum] Rangkaian migrasi hingga koreksi pada database yang sama, laporan/ekspor, backup/sync dan seluruh gerbang MVP. Lihat `docs/superpowers/reports/2026-11-14-kategori-validasi-checkpoint.md`.


**Validasi form uang (2026-11-14 15:04 WIB):** [terverifikasi] Jumlah, kuantitas dan harga menampilkan kesalahan setelah blur dan memfokuskan kolom saat Simpan; MoneyFormsTest 11/11 pada AVD terisolasi, JVM 196/196, build dan lint lulus. Laporan `docs/superpowers/reports/2026-11-14-validasi-form-uang.md`. [belum] Validasi form lain dan kondisi lintas kolom, audit seluruh layar dan gerbang MVP.


Dokumen ini diperbarui **setiap kali satu langkah selesai**, sebelum langkah berikutnya dimulai, supaya
pekerjaan bisa dilanjutkan bila sesi agen terputus (kejadian 2026-10-21: sesi hilang saat limit,
perubahan schema 10 tertinggal tanpa catatan). Agen berikutnya: baca bagian "Status" dan "Catatan
langkah" dulu, lalu lanjutkan dari langkah pertama yang belum `selesai`.

Setelah pekerjaan di-commit, ringkasannya pindah ke `logs/ACTIVITY_LOG.md`; dokumen ini dikosongkan
untuk pekerjaan berikutnya (riwayat langkah demi langkah tetap ada di Git). Setiap progres yang di-commit
langsung di-push dan pointer superproject diperbarui (instruksi pemilik 2026-10-21).

Merawat panduan dalam aplikasi:
- Ubah teks di `docs/guide/help_strings.py`, lalu `python -I docs/guide/help_strings.py`.
- Tampilan berubah atau artikel/tempat baru (`ShowPlace`) → `scripts/capture_help_screenshots.ps1` (emulator menyala);
  `GuidesTest` gagal bila ada artikel tanpa gambar.

Merawat panduan penggunaan (`docs/panduan/`):
- Tampilan berubah → `scripts/capture_module_screenshots.ps1` (emulator menyala), lalu periksa gambar dan angka
  yang dikutip di naskah. Keputusan isi (grill Q1–Q11) ada di riwayat Git berkas ini, 2026-10-21.

---

## Pekerjaan aktif

**Validasi produk dan stok (2026-11-14 14:58 WIB):** [terverifikasi] Nama/satuan kustom barang dan nama/satuan terkecil/hasil produk menampilkan kesalahan setelah blur dan mengarahkan fokus saat Simpan; tes terarah barang 4/4 dan produk/komponen 6/6, JVM 196/196, lint/build lulus. Laporan `docs/superpowers/reports/2026-11-14-validasi-produk-stok.md`. [belum] Form uang/nota lain, fokus satuan jual, audit visual seluruh aplikasi dan gerbang MVP. Lanjutkan dari `docs/superpowers/plans/2026-11-13-konsistensi-antarmuka.md`; jangan merge main atau menyentuh superproject.


**Checkpoint kategori dan validasi (2026-11-14 14:50 WIB):** [terverifikasi] Belanja lama tanpa ID kategori yang bukan pelunasan menahan laba ITEM_COST/BEP sampai dipilih; edit meminta kategori eksplisit, termasuk belanja bahan. Validasi blur baru pada jumlah gerakan stok dan nama kategori; kontrol stok/satuan/kategori diselaraskan. JVM 196/196, lint/build dan tes terarah lulus. Rincian serta batas: `docs/superpowers/reports/2026-11-14-kategori-validasi-checkpoint.md`. [belum] Validasi form lain, seluruh audit layar, koreksi lengkap lewat Catatan setelah upgrade 13→14, PDF visual, sync/backup dan gerbang MVP. Lanjut `docs/superpowers/plans/2026-11-13-konsistensi-antarmuka.md`; jangan merge main atau sentuh superproject.


**Rencana konsistensi antarmuka (2026-11-13 12:22 WIB):** [user-confirmed] Pemilik memilih audit seluruh aplikasi dengan Produk, Pengaturan dan Pusat Bantuan sebagai acuan awal; karakter visual dipertahankan, pilihan mengikuti fungsi, rincian penting tetap terlihat, dan pelaksanaan dapat berlanjut tanpa persetujuan contoh tambahan. Validasi: setelah blur atau saat Simpan untuk kolom yang belum disentuh; hilang segera saat valid (DEC-042). Rencana `docs/superpowers/plans/2026-11-13-konsistensi-antarmuka.md`. [terverifikasi] Perubahan kode/tes kategori-null dan UI masih belum di-commit; tes kosakata terakhir gagal karena istilah Variabel yang dipilih pemilik dan `break-even` di resource English. [belum] Implementasi aturan blur, audit seluruh layar dan seluruh gerbang MVP. Pertahankan perubahan tersebut; jangan merge main atau sentuh superproject.


**Checkpoint modal tidak diketahui (2026-11-13 11:51 WIB):** [terverifikasi] ITEM_COST menahan laba/BEP pada layar dan ekspor jika ada penjualan tanpa modal; SIMPLE tetap dihitung. Tes UI/xlsx dan PDF smoke lulus, golden Bu Sri lulus, JVM 194/194 dan lint berhasil. Laporan `docs/superpowers/reports/2026-11-13-modal-belum-diketahui.md`. [belum] kategori-null historis, koreksi modal historis, PDF visual, sync/backup dan seluruh MVP; jangan klaim siap produksi.


**Checkpoint audit UI (2026-11-13 11:44 WIB):** [terverifikasi] Margin dialog, pilihan stok simetris, dropdown satuan dengan tombol custom terpisah, aksi tambah berbingkai, dan validasi setelah Simpan; satu AVD 1080 × 2400/420 dpi tersisa setelah cleanup atas izin pemilik. Lihat `docs/superpowers/reports/2026-11-13-audit-layout-pilihan.md`. [belum] PK-1.2 kategori historis, audit seluruh pilihan, font 200%, TalkBack, tablet, HP nyata dan seluruh paket MVP. Lanjutkan paket sesuai rencana tanpa merge main atau mengubah superproject.


**Checkpoint (2026-11-13 11:21 WIB):** [terverifikasi] Perbaikan penggandaan kategori-null, tes fokus, bahasa Indonesia sementara dan susunan dialog stok; lihat `docs/superpowers/reports/2026-11-13-pk12-audit-pemilik.md`. JVM 193/193 dan lint lulus; AVD sintetis `emulator-5558` berjendela. [belum] PK-1.2 belum selesai: nama kategori bebas/ambigu masih menghasilkan klasifikasi tanpa peringatan dan seed upgrade bebas belum lulus. Jangan klaim hasil finansial lengkap atau rilis; jangan merge main atau ubah superproject.


**Handoff sesi (2026-11-13 07:40 WIB):** Laporan handoff lengkap: `docs/superpowers/reports/2026-11-13-handoff-progress.md` (termasuk prompt siap salin untuk agent penerus). [terverifikasi] Branch `fase2-nota-piutang`, PK-1.6 terakhir selesai di `5f81c45`; sesudahnya terdapat 10 berkas kode/tes/resource belum commit dan wajib dipertahankan. JVM sebelumnya 193/193; tes fokus terbaru 4/7 lulus, 3 gagal (OneNumber, ProductEditor selector, StockHistory label); rincian ada di laporan. [terverifikasi] Sumber mengizinkan English tersimpan melalui LanguagePicker meski default Indonesia; susunan dialog stok menumpuk dua aksi di kanan dan Kembali terpisah. [belum] Screenshot/runtime emulator, inventaris semua English, dan solusi UX belum selesai; CUA gagal init, `emulator-5556` tidak disentuh. Berikutnya verifikasi diff mentah, perbaiki kegagalan terarah dalam scope PK-1.2, baru tangani audit bahasa/layout sesuai paket layar. Jangan merge main atau ubah superproject.

**Progres otomatis (2026-11-13 07:12):** PK-1.6 selesai; laporan `docs/superpowers/reports/PK-1.6-laporan.md`. [terverifikasi] Dua basis laba lewat ProfitInputs dan MonthlySummary; usaha baru SIMPLE, golden/legacy ITEM_COST; kartu BEP dan ekspor konsisten; empat uji instrumentasi dan 16 tes domain terarah hijau; assemble debug/androidTest lulus. Emulator menangkap lalu memverifikasi perbaikan kartu BEP yang semula memakai HPP mentah dan tabel businesses yang belum diobservasi. [belum] Suite penuh, lint, PDF, sinkron/backup, seluruh edit/hapus, HP nyata. Pertanyaan pembagian diskon untuk nilai penjualan unknown dicatat di laporan. Berikutnya pilih paket yang dependensinya tidak menunggu K kosong; PK-1.2 tetap checkpoint. Branch `fase2-nota-piutang`; jangan merge ke `main` atau sentuh superproject.

**Progres otomatis (2026-11-13 06:34):** PK-5.1 selesai untuk gerbang kosakata. [terverifikasi] Daftar dasar turun dari 242 pasangan menjadi kosong; `ScreenLanguageTest` lulus 7/7; sumber dan blok 103 string bantuan dibuat ulang; `git diff --check` bersih. [belum] Suite penuh, lint, emulator/HP, pembungkusan teks, aksesibilitas, dan uji pengguna. Laporan: `docs/superpowers/reports/PK-5.1-laporan.md`. PK-5.2 membutuhkan Tahap 2 dan 3; periksa paket yang masih dapat dikerjakan tanpa jawaban pemilik kosong. Jangan merge ke `main` atau ubah superproject.

**Progres otomatis (2026-11-13 05:56):** PK-4.7 selesai pada alur lokal repository/Room; uji UI dan instal bersih belum terbukti. [terverifikasi] `BackupMoveFlowTest` lulus 1/1: file terenkripsi dibuat, DB in-memory dihapus dan dipulihkan, MonthlySummary serta pembeli sama persis. [terverifikasi] Kode Beranda diarahkan ke chooser backup dan panduan tiga langkah ada dalam dua bahasa. [belum] Compose UI, pemilih Android, `pm clear`, Drive, HP nyata. Laporan lengkap: `docs/superpowers/reports/PK-4.7-laporan.md`. PK-2.1 perlu membawa CTA ini jika WelcomeScreen baru dibuat. Belum menjalankan lint atau seluruh suite. Berikutnya PK-5.1 tidak bergantung pada K; PK-5.2 menunggu Tahap 2 dan 3; PK-4.8 menunggu paket prasyarat Tahap 4 selesai.

**Progres otomatis (2026-11-13 05:32):** PK-1.3 selesai pada lapisan domain/repository; layar input belum dibangun. [terverifikasi] RED API tidak mendukung nullable ID/nama. GREEN FreeLineSaleTest 4/4, SalesFlowTest 9/9 dan ImportCommitTest 20/20 instrumentasi di AVD terpisah; MonthlySummaryTest 4/4, SalesMathTest 8/8, BackupUpgradeTest 5/5 JVM. Lint 0 error/87 warning dan assembleDebug berhasil; sesudah satu tes guard ditambahkan, FreeLineSaleTest diulang 4/4. git diff --check bersih. [belum] Suite penuh, layar input, editor nota bebas, dan ekspor lossless PK-4.1. Lanjut paket yang tidak bergantung pada K kosong. PK-1.4 menunggu K-8; PK-2.1/2.2 menunggu K-6/K-7. Lanjut PK-4.7 yang tak punya K, catat bahwa WelcomeScreen belum ada dan tautan awal baru bisa ditambahkan saat PK-2.1 membangunnya. Jangan klaim laporan finansial lengkap sebelum propagasi costKnown PK-1.6 dan penjelasan seed kategori-null PK-1.2. Lingkup pemilik: lanjut seluruh paket MVP yang tidak terhalang keputusan; tetap di fase2-nota-piutang, tanpa main/superproject.

**Progres otomatis (2026-11-13 05:09):** PK-1.2 (bukti upgrade lanjutan) berhenti di checkpoint schema dan bukti golden. [terverifikasi] Upgrade install -r schema 13→14 lulus untuk seed kategori baku; integrity_check lulus, foreign_key_check kosong, Beranda Rp30.000 sebelum/sesudah. [terverifikasi] Kontrol tanpa categoryId menghasilkan Rp30.000→-Rp20.000 dan dicatat sebagai batas yang belum dijelaskan terhadap perilaku produk. [belum] Sync dengan APK lama, suite JVM/lint terbaru, dan jalur data tanpa kategori belum beres. PK-1.2 tetap checkpoint dan belum release-ready. Telusuri perilaku laba untuk data lama tanpa categoryId, jalankan JVM/lint setelah perubahan terakhir, dan selesaikan propagasi costKnown/rencana/stok sebelum klaim angka finansial lengkap. Lingkup pemilik: lanjut seluruh paket MVP yang tidak terhalang keputusan; tetap di fase2-nota-piutang, tanpa main/superproject.

**Progres otomatis (2026-11-13 00:14):** PK-3.1 selesai. [terverifikasi] RED pecahan/status/konversi lalu GREEN 189 tes JVM, 6 instrumented, lint 0 error/87 warning, assembleDebug. Data emulator pulih 4 hash. [belum] Seluruh MVP dan PK-1.2 tetap checkpoint. Lanjut pekerjaan yang tidak terhalang; selesaikan integrasi PK-1.2/1.3/1.6 sebelum klaim finansial lengkap. Lingkup pemilik: lanjut seluruh paket MVP yang tidak terhalang keputusan; tetap di fase2-nota-piutang, tanpa main/superproject.

**Progres otomatis (2026-11-12 23:48):** PK-1.2 berhenti di checkpoint schema. [terverifikasi] 37/37 tes instrumentasi terarah lulus; data emulator dipulihkan dengan 4 hash cocok. [belum] JVM/lint setelah perubahan terakhir, upgrade APK nyata, dan integrasi angka unknown/rencana/stok. Lanjut PK-3.1 yang independen; selesaikan PK-1.2/1.3/1.6 dan jawab K-8 sebelum klaim finansial lengkap atau rilis. Lingkup pemilik: lanjut seluruh paket MVP yang tidak terhalang keputusan; tetap di fase2-nota-piutang, tanpa main/superproject.

**Progres otomatis (2026-11-12 13:10):** PK-1.1 selesai. [terverifikasi] RED perilaku 9/21 dan 13/25; GREEN 25/25 terfokus, 179/179 JVM dan assembleDebug lulus. UI, migrasi, sinkron dan perangkat belum diuji pada paket domain ini. Review independen dan PK-1.2 schema 14; K-8 tetap belum dijawab. Lingkup pemilik: lanjut seluruh paket MVP yang tidak terhalang keputusan; tetap di fase2-nota-piutang, tanpa main/superproject.

**Progres otomatis (2026-11-12 08:34):** PK-0.8 selesai. [terverifikasi] RED penghitung4/3gagal dan diagnostik8/8gagal; GREEN paket12kasus=4lulus/8Ignore/0gagal, JVM164/164. Data emulator pulih4hash/noextras; diff --check lulus. Delapan perjalanan belum dinyatakan bekerja. PK-1.1 model biaya barang; keputusan K kosong tetap penghalang. Selaraskan gerbang X19 sebelum PK-2.11. Lingkup pemilik: lanjut seluruh paket MVP yang tidak terhalang keputusan; tetap di fase2-nota-piutang, tanpa main/superproject.

**Progres otomatis (2026-11-12 08:24):** PK-0.5 selesai. [terverifikasi] RED tiap temuan lalu GREEN164 tes JVM/0 gagal;31 instrumented terfokus/0 gagal sebelum penghapusan label usang; setelah cleanup164 JVM+lint0error/87warning+assembleDebug lulus, diff --check lulus. APK akhir dipasang; data emulator pulih4/4hash identik. PK-0.8 kemudian PK-1.1 dan paket model yang tidak terhalang. Kinerja X3/HP nyata tetap belum; daftar dasar bahasa242. Lingkup pemilik: lanjut seluruh paket MVP yang tidak terhalang keputusan; tetap di fase2-nota-piutang, tanpa main/superproject.

**Progres otomatis (2026-11-12 07:59):** Audit ruang kosong (permintaan tambahan pemilik) selesai. [terverifikasi] Skrip bukti PASS; celah form 202–413 dp terutama akibat footer tetap; ruang pesan sekitar44 dp, sela resep berlebih12 dp. [belum] Uji pengguna, tablet, font besar, mode gelap; kode layout tidak diubah. PK-0.5 terus berjalan; pertimbangkan temuan spacing pada paket layar yang dirujuk laporan, jangan mengecilkan target sentuh. Lingkup pemilik: lanjut seluruh paket MVP yang tidak terhalang keputusan; tetap di fase2-nota-piutang, tanpa main/superproject.

**Progres otomatis (2026-11-12 02:42):** PK-0.7 selesai. [terverifikasi] RED 3 perilaku + 2 alasan kosong; GREEN 16 tes komponen, 155 tes JVM, lint 0 error/89 peringatan. Screenshot/XML: alasan tampil di atas Simpan mati; pemulihan hash 4/4 sama. PK-0.5 berikutnya; audit ruang kosong tambahan diminta pemilik, tanpa mengubah batas kapabilitas. Lingkup pemilik: lanjut seluruh paket MVP yang tidak terhalang keputusan; tetap di fase2-nota-piutang, tanpa main/superproject.

**Progres otomatis (2026-11-12 02:34):** PK-0.4 selesai ditinjau. [terverifikasi] 12/12 sampel, jank 57,14–96,67 %, p90 25–53 ms; crash duplicate key direproduksi; hash pemulihan 4/4 sama. [belum] Akar tingkat fungsi dan HP nyata; bukan klaim kinerja lulus. PK-0.5 memperbaiki crash dan temuan B, membetulkan klaim N; PK-0.7 sedang dikerjakan. Kinerja tetap terbuka di X3/PK-5.4. Lingkup pemilik: lanjut seluruh paket MVP yang tidak terhalang keputusan; tetap di fase2-nota-piutang, tanpa main/superproject.

**Progres otomatis (2026-11-12 02:30):** Keputusan total kas (DEC-033) selesai. [terverifikasi] Dokumentasi kontrak; belum ada perubahan angka/kode pada keputusan ini. Implementasi serta tes rekonsiliasi pada PK-0.5; keputusan K lain yang kosong tetap terhalang. Lingkup pemilik: lanjut seluruh paket MVP yang tidak terhalang keputusan; tetap di fase2-nota-piutang, tanpa main/superproject.

**Progres otomatis (2026-11-12 02:27):** PK-0.6 selesai. [terverifikasi] RED/GREEN/probe; 155 tes JVM/0 gagal, assembleDebug dan diff --check hijau. Pemeriksaan XML tidak menggantikan uji pengguna. PK-0.4 sedang mengukur emulator; PK-0.7 berikutnya. Daftar dasar 243 pasangan harus dikosongkan pada PK-5.1. Lingkup pemilik: lanjut seluruh paket MVP yang tidak terhalang keputusan; tetap di fase2-nota-piutang, tanpa main/superproject.

**Mulai di sini (2026-11-12 02:05):** rencana kerja sekarang `docs/superpowers/plans/2026-11-12-rencana-induk-mvp.md`
(lampiran A spek layar, B paket kerja, C rencana uji, D prompt eksekusi). Rencana 2026-11-09 di bawah ini digantikan
urutannya. Keadaan: rencana ditulis, belum ada paket yang dikerjakan. Menunggu pemilik: tabel 4.16 (K-1 sampai K-15);
K-1 sampai K-4 menghalangi Tahap 1. Bisa dikerjakan tanpa jawaban: PK-0.3, PK-0.4, PK-0.6, PK-0.7, PK-0.8,
PK-3.1. PK-0.1 dan PK-0.2 (hotfix INTERNET, `f8a9bee` di `main`) selesai; K-1 sampai K-4 dan K-15a sudah dijawab. PK-0.3 selesai ditinjau (2026-11-12 02:14): [terverifikasi] review statis di docs/superpowers/reports/PK-0.3-review.md, 4 Important; tanpa perubahan kode atau eksekusi tes. F2 menunggu pilihan pemilik tentang total kas nyata versus operasional. Paket berikutnya sesuai urutan: **PK-0.4**; perbaikan temuan B/N pada PK-0.5 setelah kedua review.

**Mulai di sini (2026-11-09 20:30):** `docs/superpowers/plans/2026-11-09-rencana-lanjutan-lengkap.md` adalah rencana serah terima
lengkap untuk semua sisa pekerjaan. Paket A (review U5), Paket N (stabilitas jitter + bug bash), dan Paket B (U6 Catatan terpadu) selesai. Pekerjaan aktif berikutnya:
**Paket C — U7: Rencana Usaha tanpa periode (spec #14)**.

**Riwayat Fase UX:** U1, U2, U3, U4 selesai. U5 direview/diperbaiki di Paket A. Paket N (stabilitas jitter & bug bash) selesai. Paket B (U6 Catatan terpadu) selesai. Berikutnya U7 (Paket C).

**Sekarang (2026-10-29):** perapihan temuan Fase 1 (P1–P10) selesai, direview, dan sudah di main
(`docs/superpowers/plans/2026-10-29-fase1-perapihan.md`, laporan `...-perapihan-laporan.md`). **Fase 2:** keputusan pemilik F2-1..F2-7 dicatat di spec (bagian "Keputusan Fase 2"); rencana tertulis di
`docs/superpowers/plans/2026-10-29-fase2-nota-piutang.md` (8 tugas, schema 14; 12 keputusan penulis rencana menunggu
tinjauan pemilik, masuk DEC-029). Eksekusi di branch `fase2-nota-piutang`: Task 1 (matematika domain) selesai dan
direview; Task 2 (schema 14, migrasi F2-4, jurnal piutang/utang, BackupUpgrade 13→14, DEC-029) selesai dan direview;
Task 3 (repository kontak, nota, pembayaran, belanja tempo, titip; stok dari `sale_lines`; aturan M3/M4 di DEC-029)
selesai dan direview; Task 4 (untung saat terjual di ringkasan, `actualMonth`, Beranda/Laporan, PDF/xlsx) selesai,
ditambah fix round F2-8 (kategori "Biaya produksi (sudah dihitung di HPP)") dan fix round 2 (Komparasi tetap basis kas,
BEP memuat biaya variabel non-HPP), selesai dan direview. Task 5 (layar Kontak/pemilih pelanggan, Nota penjualan,
daftar nota + detail di Catatan, kartu Tagihan di Beranda, form uang masuk tanpa produk) selesai, menunggu review
(laporan `.superpowers/sdd/2026-10-29-fase2-nota-piutang/task-5-report.md`). Berikutnya: Task 6.

**Uji alur sebagai pengusaha baru (2026-10-29)**, dicatat per checkpoint di
`docs/audit/2026-10-29_UJI_ALUR_PENGUSAHA.md`. Semua checkpoint (CP1–CP7) selesai.

**Redesign disepakati (DEC-026)**: spec `docs/superpowers/specs/2026-10-29-redesign-spec.md` (20 keputusan),
peta fase `docs/superpowers/plans/2026-10-29-redesign-roadmap.md`. **Fase 0 selesai** dan sudah di main (DEC-027).
**Fase 1 selesai (2026-10-29, DEC-028)** dan sudah di main: produk dengan satuan dasar, satuan jual dan resep
per batch, HPP per satuan jual, stok produk (Produksi, Hitung stok), harga bahan dari belanja terakhir dengan peringatan
HPP, template Excel v5, tab Produk dan editor halaman penuh. Uji emulator CP1 sampai CP9 dan angka verifikasi:
`docs/audit/2026-10-29_UJI_FASE1.md`. Satu crash ditemukan dan diperbaiki saat uji (Rencana Usaha dengan produk dua
satuan). Berikutnya: **tulis rencana Fase 2** (nota multi-produk, piutang, konsinyasi, snapshot HPP per baris nota, laba
saat terjual) di `docs/superpowers/plans/`. Review akhir branch: I-1 sampai I-5 dan M-3, M-4, M-6, M-11, M-12 sudah
diperbaiki (lembar baru **Produksi & Stok** di template v5, DEC-028 §9 diamandemen). Diparkir: bahan arsip kembali
aktif setelah impor ulang; harga bahan bersama dihitung dari belanja seluruh usaha; Back tidak menutup sheet Usaha/Unit
(sudah ada sebelum Fase 1); entri Produksi/Hitung stok yang salah belum bisa dihapus (koreksi lewat Hitung stok;
kandidat Fase 2); nama produk kembar di satu file. Terbuka dari Fase 1: tidak ada tes otomatis untuk dua cabang form catat (produk tanpa
satuan jual, bahan terpilih lalu pindah ke Uang masuk; diperiksa tangan di CP), generator `docs/evidence/make_sims.py`
masih menulis template v4, Gboard asli dan sinkron dua HP lintas versi belum diuji. Rilis internal testing adalah
tindakan pemilik; catatan rilis: "perbarui semua HP yang memakai sinkron" (DEC-028 §7).

Sebelumnya: review run audit dan perbaikannya selesai (laporan: `docs/audit/2026-10-22_AUDIT_PRA_RILIS.md` bagian 11).
Sisa untuk pemilik: bagian 10 laporan itu (uji sync dua HP dengan akun Google nyata, AAB bertanda tangan, Data Safety,
peluncuran bertahap) dan usulan fitur usaha menengah (bagian 8).
