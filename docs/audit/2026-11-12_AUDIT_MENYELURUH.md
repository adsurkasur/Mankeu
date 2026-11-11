# Audit Menyeluruh Mankeu (2026-11-12)

> **Tujuan:** bahan serah terima ke agen perencana (Fable) untuk menyusun rencana berikutnya.
> **Sifat:** audit; satu perubahan kode kecil dilakukan karena menyangkut temuan T0 (izin INTERNET). Disusun oleh Sonnet 5.5 (effort rendah) atas permintaan pemilik.
> **Pembaca berikutnya:** Fable menyusun rencana; agen lain (Sonnet, Gemini) menjalankannya. Dokumen ini sengaja lengkap
> agar rencana bisa disusun sampai akhir (rilis internal testing) tanpa menebak.
> **Status fakta:** `terverifikasi` = dijalankan atau dibaca langsung pada audit ini. `dilaporkan` = hanya dari laporan
> agen sebelumnya. `belum diverifikasi` = tidak bisa dicek tanpa HP nyata atau tindakan pemilik.

## 1. Ringkasan

Kondisi teknis **sehat menurut tes**. Tes otomatis hijau, build release lulus, dan tidak ada utang teknis yang menonjol di kode baru.
Namun audit ini menemukan **satu cacat serius yang lolos dari semua tes** (T0: izin INTERNET hilang sejak Fase 0, sehingga
cadangan dan sinkron Google Drive tidak bisa berjalan di HP nyata). Itu menunjukkan batas tes yang dijalankan di emulator.
Tetapi produk **belum bisa dirilis**: ada 37 commit di branch `fase2-nota-piutang` yang belum di-merge, Fase 2 belum
lengkap (titip, belanja tempo, template Excel untuk nota), dan beberapa klaim kualitas lemah buktinya. Pemilik juga
melaporkan bahwa app masih terasa penuh bug dan bergetar saat dipakai walaupun tes hijau. Tes yang ada belum menangkap
apa yang dirasakan pengguna.

## 2. Bukti yang dijalankan pada audit ini (HEAD `b217794`)

| Cek | Hasil | Status |
|---|---|---|
| Unit test | **148 lulus, 0 gagal** | terverifikasi |
| `lintDebug` | **0 error**, 89 peringatan | terverifikasi |
| `assembleDebug`, `assembleRelease` (R8) | hijau | terverifikasi |
| Tes instrumented penuh | **134 tes, 0 gagal, 2 dilewati** | terverifikasi |
| Mojibake di `*.md`/`*.xml`/`*.kt` | tidak ada | terverifikasi |
| `TODO`/`FIXME` di kode utama | tidak ada | terverifikasi |
| Warna hard-code di layar baru | 0 di luar `Theme.kt` | terverifikasi |

### 2.1 Tes instrumented
134 tes, **0 gagal**, 2 dilewati (`HelpScreenshotTest`, `ModuleScreenshotTest`, yang memang tidak ikut run normal).

## 3. Posisi pekerjaan

| Bagian | Keadaan |
|---|---|
| `main` | Fase 0, Fase 1, perapihan Fase 1 (`9946f4f` dan dokumen). Aman, sudah di-push. |
| Branch `fase2-nota-piutang` | 37 commit, 214 berkas, +17.070 / −3.019 baris di atas main. Sudah di-push, **belum di-merge**. |
| Fase 2 | Task 1–5 selesai. Task 6 (titip + belanja tempo), 7 (template Excel nota/kontak/titip), 8 (uji alur) **belum**. |
| Fase UX | U1–U6 selesai. U7 (Rencana tanpa periode), U8 (bahasa + bantuan), U9 (uji ulang) **belum**. |
| Paket rencana 2026-11-09 | A, N, B **selesai** menurut Status. C, D, F, G, E, H–M belum. |

## 4. Temuan

Tingkat: **Berat** = menghalangi rilis atau berisiko data/uang. **Sedang** = merusak kegunaan. **Ringan** = kerapian.

### Berat

**T0. Izin INTERNET hilang sejak Fase 0 (terverifikasi, sudah diperbaiki di branch).**
Manifest app tidak pernah mendeklarasikan `INTERNET`; izin itu dulu ikut masuk lewat pustaka Firebase. Saat Firebase Auth
dibuang di Fase 0 (spec #8), izin ikut hilang. Manifest hasil gabungan (debug dan release) hanya berisi `DUMP`. Padahal
klien Drive memakai `HttpURLConnection`. Akibatnya masuk Google lalu cadangan dan sinkron Drive akan gagal di HP nyata.
Ini ada di `main` juga (`6dfeacc`), bukan hanya di branch Fase 2. Tidak tertangkap karena emulator tidak punya akun Google
uji dan tes Drive memakai tiruan; item "masuk Google dan token Drive" memang tercatat `belum diverifikasi` sejak DEC-027.
Perbaikan: `<uses-permission android:name="android.permission.INTERNET" />` di `app/src/main/AndroidManifest.xml`
(manifest hasil gabungan debug dan release kini berisi `INTERNET`). **Karena `main` terdampak, perbaikan ini perlu
masuk ke main lebih cepat dari merge Fase 2** (cherry-pick satu commit) bila ada pengguna internal yang memakai build main.
Pelajaran: perlu uji manifest otomatis (lihat §7) dan uji Drive sungguhan di HP.

**T1. Branch besar belum di-merge dan belum utuh (DEC-012).**
Template Excel belum membawa nota, kontak, titip, dan belanja tempo, serta tanda "Biaya produksi" pada kategori. Ekspor
lalu impor ulang kehilangan data itu. Aturan repo menyatakan ekspor harus bisa diimpor ulang tanpa kehilangan.
Merge ke main sebelum Paket G berarti melanggar aturan itu. Rujukan: rencana paket G, register R2, R3.

**T2. Paket B dan N tidak punya review independen.**
Tidak ada `B-review.md` atau `N-review.md` di `docs/superpowers/reports/` (hanya `U5-review.md`). Seluruh pola kerja
sebelumnya memakai review terpisah yang berulang kali menemukan bug nyata (desimal "2,5" yang membuang stok, urutan
sinkron, dan lain-lain). Dua paket besar yang menyentuh daftar Catatan dan performa lolos tanpa itu.
Paket B menggabungkan nota dan kas dengan aturan "pembayaran hari yang sama diserap" (hindari hitung ganda); aturan
ini menyentuh uang dan harus ditinjau.

**T3. Klaim perbaikan jitter belum menyelesaikan masalah.**
Laporan N sendiri mencatat membuka bottom sheet masih **32,69 %** frame janky (turun dari 57,9–65,3 %). Itu masih
buruk secara objektif. Ketik di sheet: 22,89 % janky. Pemilik melaporkan jitter dari build sebelum paket ini; belum ada
konfirmasi pemilik bahwa jitter hilang. Bug bash (`2026-11-09_BUG_BASH.md`) berisi satu tabel hasil dan analisis,
tetapi tidak ada daftar bug dari pemilik sendiri yang dikonfirmasi. Status: `dilaporkan`, bukan terbukti pada HP nyata.

**T4. Masih tidak ada bukti pada perangkat nyata.**
Semua uji dijalankan di emulator. Gboard di emulator sempat rusak (proses Gboard sendiri crash dengan
`NullPointerException`); pemilik memperbaruinya lewat Play Store pada 2026-11-12, lalu **tombol Back terverifikasi baik**
(lihat §4a). Masih `belum diverifikasi` (butuh HP nyata atau akun uji): masuk Google dan token Drive, sinkron dua HP
lintas versi schema, Kirim ke WhatsApp (tidak terpasang di emulator), dan baterai/memori (register R17).

### Sedang

**T5. Penamaan akuntansi masih ada di layar (bertentangan dengan spec #19).**
Ditemukan langsung di `strings.xml`: "Unit Usaha" di banyak tempat (padahal spec #15 menyembunyikannya sampai ada cabang
kedua), "Cabang / Unit", "Pendapatan Operasional", "Biaya Operasional", "Rasio Penerimaan atas Biaya (R/C Ratio)",
"Rasio Manfaat Bersih (B/C Ratio)", "Ambil Pribadi (Prive)", "Realisasi %1$s" (Komparasi), "proyek". Paket D belum
dikerjakan (register R5, R7).

**T6. Bantuan dan panduan menjelaskan app lama.**
`docs/guide/help_strings.py` masih memuat 11 rujukan ke "Realisasi", "Catat Pemasukan", "Simpan catatan". Panduan
`docs/panduan/` dan dua tes screenshot (`HelpScreenshotTest`, `ModuleScreenshotTest`) tidak ikut dijalankan di run
normal, sehingga tidak ada yang gagal saat artikel sudah salah.

**T7. Rencana Usaha masih versi lama.**
Halaman sementara memuat periode, R/C, BEP, dan "Layak" yang bisa muncul saat biaya tetap Rp0 (L27 audit). Masih ada
6 rujukan `BookMode.PROJECTION/REALIZATION` di lapisan presentasi, tanda bahwa konsep Rencana/Realisasi belum
sepenuhnya lepas dari UI. Paket C.

**T8. Fitur Fase 2 yang sudah dijanjikan ke pengguna belum ada di layar.**
Titip ke toko, laku, tarik, belanja "dibayar nanti", utang ke pemasok dengan tombol Bayar. Kodenya sudah ada di
repository (Task 3) tetapi tidak punya UI. Beranda sudah memajang kartu "Utang ke pemasok" yang belum bisa diisi
dari form. Paket F.

**T9. Bentuk form masih terpusat pada kotak isian di beberapa layar.**
Jumlah `OutlinedTextField` per berkas: Editor produk 9, Lainnya/Data 6, Kontak 5, Stok 4, Form uang 3. Editor produk
sudah dirombak ke baris ringkas, tetapi angka ini menunjukkan masih ada layar yang bergantung pada kotak (Kontak,
Stok/Bahan, Data/Berkas, Pengaturan). Perlu dipastikan lewat uji ulang (Paket E), bukan dari hitungan baris.

**T10. File besar menumpuk tanggung jawab.**
`FinanceRepository.kt` 2.129 baris, `MankeuApp.kt` 1.503, `AppViewModel.kt` 1.258, `MoreScreen.kt` 960. Belum
menyebabkan bug, tetapi menyulitkan review dan agen berbasis konteks terbatas; semuanya dibaca ulang setiap
perubahan. Pemecahan perlu rencana terpisah supaya tidak memicu regresi.

### Ringan

**T11. Dependensi Vico (2 artefak) masih ada** untuk satu grafik; rencana F4 menggantinya dengan `Canvas`.
**T12. `lintDebug` 89 peringatan** (80 sebelum U5). Tidak ada error, tetapi trennya naik tiap paket.
**T13. Dokumen yang masih menyebut Firebase:** ARCHITECTURE, CLOUD_SYNC_AND_AUTH, PLAY_CONSOLE_ENTRY_GUIDE,
PLAY_STORE_DEPLOYMENT, PRODUCT_REQUIREMENTS, README, ROADMAP (kecuali DECISION_LOG yang memang catatan sejarah).
Privasi, listing toko, dan Data Safety menunggu keputusan pemilik (DEC-027).
**T14. Ketidaksesuaian catatan:** Status paket B menyebut commit `3d02d84`; riwayat saat ini menunjukkan commit B
sebagai `b217794`. Objek `3d02d84` memang ada, tetapi tidak berada di jalur HEAD (kemungkinan sisa rebase).
Rujukan commit di Status perlu dibetulkan.
**T15. Uang dan parsing:** tidak ditemukan penggunaan `Double`/`Float` untuk uang tersimpan. `Double` hanya muncul
di pembaca Excel, backup (kolom REAL SQLite), grafik, dan persentase. Pembacaan jumlah masih memakai
`toBigDecimalOrNull()` setelah mengganti koma pada pengimpor dan `StockManagement.kt:283`. Pola ini tertulis aman
(koma diganti titik lebih dulu), tetapi pernah menjadi sumber bug (desimal "2,5"); satu fungsi parsing bersama akan
lebih aman daripada tiga salinan.

### 4a. Verifikasi tangan di emulator dengan Gboard berfungsi (2026-11-12, build `b217794` + manifest)

| Skenario | Hasil |
|---|---|
| Menu "Lainnya" lalu Back | Menu menutup, kembali ke Beranda. Baik. |
| Buat usaha: Back dengan keyboard terbuka | Back pertama menutup keyboard, kedua menutup sheet, ketiga menutup sheet di bawahnya. Berurutan dan benar. |
| Halaman Produk baru dari langkah awal Beranda lalu Back | Kembali ke Beranda. Baik. |
| Crash `com.mankeu` di log | Tidak ada. |
| **Masalah L02 masih ada:** "Tambah Usaha" pertama membuka **dua sheet bertumpuk** ("Usaha dan Cabang / Unit" lalu "Tambah usaha"), dengan kata unit/cabang di layar pertama pengguna baru. Bertentangan dengan spec #15 (unit disembunyikan). | UX sedang-berat; masuk paket D. |

Kesimpulan: dugaan "Back diabaikan" di audit 2026-10-30 (L04, L10) adalah **artefak keyboard emulator**, bukan bug app. Register R1 selesai.

### 4b. Klarifikasi laporan pemilik (2026-11-12)

- Layar "form1 sampai form12, belanja, singkong" yang tampak buruk dan menempel di pojok kiri atas adalah **fixture tes otomatis**
  (komponen mentah tanpa tema, mis. `MankeuSheetTest`), bukan halaman produksi. Tidak ada halaman debug di app (kode utama
  bersih dari `Form N`, `DebugScreen`, `playground`). Tes instrumented membuka, menutup, dan mereset app berulang kali,
  sehingga layar putih dan app tertutup saat tes berjalan adalah normal.
- **Tidak berarti UI produksi sudah siap.** Jawaban jujur untuk pertanyaan pemilik "apakah UI-nya production-ready":
  **belum** (T3, T5 sampai T9).
- Saran proses: jangan memakai emulator saat tes instrumented berjalan. Jalankan tes di AVD terpisah agar emulator pemakaian bersih.

## 5. Yang sudah baik (jangan dibuang)

- Satu sumber angka (`MonthlySummary`) dan tes `OneNumberTest`; angka acuan Oktober keripik stabil.
- Kaidah engineering yang terbukti: tes ditulis dulu sampai gagal, review terpisah, log di commit yang sama.
- Migrasi dan backup berantai 12→13→14 dengan tes, lantai sinkron schema 14, dan pemberitahuan data dilewati.
- Komponen form bersama dan token desain (tanpa warna hard-code di layar baru).
- Dokumentasi serah terima: rencana lanjutan, prompt agen, register 20 temuan, ledger status.

## 6. Keputusan yang masih menunggu pemilik

1. ~~Gboard di emulator~~ selesai (pemilik memperbarui; Back terverifikasi).
2. Kapan merge branch Fase 2 ke main: sebelum atau sesudah Paket G (saran audit: **sesudah** G, karena DEC-012).
3. ~~Teks privasi dan dokumen listing~~ sudah diperbarui di repo (2026-11-12, lihat §9). Tersisa untuk pemilik: **isi di Play Console** (Data Safety, URL penghapusan akun bila diminta, hosting halaman kebijakan) dan membaca ulang `docs/privacy-policy/id.md` dan `en.md` sebelum dipublikasikan.
3b. **Cherry-pick perbaikan INTERNET ke `main`** (T0) lebih awal, atau terima risiko bahwa build main tidak bisa cadangan/sinkron.
4. Unggah AAB bertanda tangan ke internal testing (tindakan pemilik).

## 7. Saran urutan untuk perencana (bukan keputusan)

0. **Kerjakan T0 dulu** (hotfix INTERNET dan tes manifest), karena memengaruhi `main`.
1. **Konfirmasi dengan pemilik** di HP nyata: apakah jitter dan bug yang dilaporkan masih terjadi di build `b217794`?
   Ini menentukan apakah Paket N dianggap selesai. Minta daftar bug dan screenshot dari pemilik.
2. **Review independen** Paket B dan N (read-only, model kuat), dengan fokus aturan serap pembayaran hari yang sama
   dan ukuran jank sheet 32,69 %.
3. Lanjut paket C (Rencana), D (bahasa + bantuan), F (titip + tempo), G (template), lalu E (uji ulang), H (review
   akhir + merge), sesuai `docs/superpowers/plans/2026-11-09-rencana-lanjutan-lengkap.md`.
4. Pertimbangkan satu paket baru: **uji keterpakaian dengan pengusaha** (satu sesi 30 menit, tugas harian, direkam)
   karena sumber keluhan pemilik adalah rasa pakai, bukan hasil tes.
5. Pertimbangkan satu paket baru: **pemecahan file besar** hanya setelah merge, bukan sebelum.
6. Tambah pengaman kualitas yang bisa dijalankan agen murah: tes manifest (izin INTERNET harus ada), daftar kata terlarang di string layar non-laporan
   (`ScreenLanguageTest`), batas lint peringatan, dan pemeriksaan `gfxinfo` otomatis pada alur utama.

## 9. Privasi dan dokumen (diperbarui 2026-11-12)

Diubah agar sesuai dengan app sekarang (tanpa Firebase, ada nota, pelanggan, stok, Kirim). Play Console tidak disentuh.

| Berkas | Perubahan |
|---|---|
| `docs/privacy-policy/id.md`, `en.md` | Tanggal berlaku 12 Nov 2026; tidak lagi menyebut akun Firebase; daftar data mencakup nota, pelanggan dan pemasok (nama, HP, alamat, hanya di HP), titipan, stok bahan, resep; bagian baru tentang **Kirim nota/pengingat** (teks keluar HP hanya bila pengguna memilih aplikasi tujuan, nomor HP hanya untuk membuka WhatsApp); baris izin `ACCESS_NETWORK_STATE` dihapus (tidak ada di manifest); penghapusan data dan permintaan lewat email disesuaikan. |
| `docs/STORE_LISTING_AND_POLICIES.md`, `docs/PLAY_CONSOLE_ENTRY_GUIDE.md` | Rujukan akun Firebase dibuang; URL penghapusan akun ditandai "hanya bila Play Console masih memintanya". |
| `docs/CLOUD_SYNC_AND_AUTH.md` | Banner "dokumen desain lama, Firebase sudah dibuang". |
| `docs/README.md`, `PRODUCT_REQUIREMENTS.md`, `ROADMAP.md`, `docs/diagrams/02`, `03` | Rujukan Firebase diganti Google Sign-In / Akun Google. |
| Tidak diubah (disengaja) | `AGENTS.md` (aturan jangan membuat proyek Firebase), `DECISION_LOG`/`CHANGELOG`/log (sejarah), `V1_IMPLEMENTATION_SPEC.md` (spec lama), `docs/diagrams/preview.html` (hasil render; buat ulang dari diagram bila dibutuhkan). |

Catatan jujur: pernyataan "tidak ada data ke server pengembang" benar menurut kode, tetapi fitur Drive baru benar-benar bisa berjalan
setelah T0. Pemilik perlu membaca ulang kedua berkas sebelum publikasi, karena ini dokumen hukum atas namanya.

## 10. Peta lengkap sisa pekerjaan sampai rilis internal testing (untuk perencana)

Sumber rinci: `docs/superpowers/plans/2026-11-09-rencana-lanjutan-lengkap.md` (paket, kriteria, register R1 sampai R22) dan
`docs/superpowers/PROMPT_AGEN_BERIKUTNYA.md`. Status paket: A, N, B selesai; sisanya di bawah.

| Urutan | Paket | Isi | Syarat selesai singkat |
|---|---|---|---|
| 0 | **T0 hotfix** | INTERNET ke main (cherry-pick) + tes manifest otomatis | manifest hasil gabungan berisi INTERNET; tes gagal bila hilang |
| 1 | Review B dan N | review independen (uang pembayaran hari yang sama; jank sheet 32,69 %) | review tertulis, perbaikan dengan tes merah dulu |
| 2 | Konfirmasi pemilik di HP | bug dan jitter yang masih ada di build terbaru | daftar bug dari pemilik masuk bug bash |
| 3 | C | Rencana Usaha tanpa periode | golden domain, "Layak" hanya bila biaya tetap terisi |
| 4 | D | bahasa awam, bantuan, sheet pembuka usaha bertumpuk (L02), R/C keluar dari Beranda | `ScreenLanguageTest` diperluas; GuidesTest hijau; pengguna baru melihat satu form saja |
| 5 | F | titip ke toko dan belanja tempo (UI) | golden F2 titip dan tempo |
| 6 | G | template Excel lengkap (nota, kontak, titip, tempo, kategori) | round trip identik (DEC-012) |
| 7 | E | uji ulang semua layar dan ketukan harian | tidak ada temuan Berat; target ketukan tercapai |
| 8 | H | review akhir branch, merge ke main, push, pointer | matriks verifikasi hijau |
| 9 | I | persiapan rilis internal (versi, catatan rilis, uji HP nyata, AAB oleh pemilik) | checklist HP nyata selesai |
| 10+ | J, K, L, M | laporan per bulan dan Canvas, kunci app dan notifikasi, backup/sinkron sisa, bantuan dan rilis | kriteria F4, F6, F5, F7 di roadmap |

Aturan kerja untuk agen pelaksana bereffort rendah: satu paket per sesi; tes terfokus dulu, suite penuh sekali di akhir;
commit dan Status diperbarui di setiap langkah; laporan di `docs/superpowers/reports/`.

## 11. Fleksibilitas untuk UMKM (permintaan pemilik 2026-11-12)

> Prinsip pemilik: sistem fleksibel tetapi tetap simpel; jangan membatasi UMKM karena kenyataan di lapangan sangat beragam.

| # | Temuan | Bukti | Usulan |
|---|---|---|---|
| F1 | Pilihan satuan adalah daftar tetap di kode (`COMMON_BASE_UNITS` di `ProductEditorScreen.kt:79`: bungkus, pcs, porsi, botol, kg; `COMMON_UNITS` di `StockSection.kt:261`: kg, liter, gram, pcs, bungkus, ikat) dengan satu tombol "Lainnya". Satuan yang disimpan teks bebas, jadi tidak ada batas data, tetapi satuan ketikan tidak diingat, harus diketik ulang di tiap produk/bahan, dan "karung"/"Karung"/"krg" menjadi tiga satuan (pencocokan stok dan penjualan memakai nama). | kode | **Satuan milik usaha**: pemilih dengan pencarian (komponen `PickerPage` U1) berisi satuan yang sudah dipakai di usaha itu (urut paling sering) + saran awal; "+ Tambah 'xyz'" bila belum ada; saran nama yang hampir sama (huruf besar/kecil, spasi) sebelum membuat satuan baru; tanpa tabel baru (dibaca dari data) atau tabel kecil `units` bila perlu penamaan resmi. Berlaku di editor produk, tambah bahan, satuan jual, form belanja, dan nota. |
| F2 | Tidak ada produk jasa tanpa stok. Sewa alat/jasa membajak tetap dianggap punya stok dan bisa minus; tombol Produksi dan resep tidak relevan. | pencarian kode: tidak ada `trackStock`/`isService`; "Jasa" hanya nama kategori | Pilihan "Produk ini tidak punya stok (jasa/sewa)" di editor produk: stok, Produksi, dan peringatan menipis disembunyikan; HPP dari biaya per kali jasa (mis. BBM, upah) tetap bisa diisi. Spec awal mewajibkan inti mendukung jasa pertanian dan sewa mesin. |
| F3 | Tidak ada retur penjualan (pelanggan mengembalikan barang). | pencarian kode kosong | Retur sebagai kebalikan nota: mengurangi piutang atau mengembalikan uang, mengembalikan stok (opsional), dan membalik untung. Perlu keputusan pemilik dan golden test. |
| F4 | Cara bayar hanya "tunai" (satu kas). Transfer/QRIS tidak dibedakan. | kode, rencana Fase 2 ruling 1 | Opsional: label "Dibayar lewat" (tunai, transfer, lainnya) tanpa memecah saldo; atau beberapa "dompet". Perlu keputusan pemilik. |
| F5 | Sudah fleksibel (jangan dibatasi): kategori bisa ditambah sendiri; komponen biaya resep bebas; jumlah produk dan satuan jual tidak dibatasi; kategori impor yang tidak dikenal menjadi kategori baru; kuantitas desimal. | kode | pertahankan |

Aturan desain yang diusulkan untuk semua pilihan berdaftar (satuan, kategori, pelanggan, pemasok, bahan): **daftar milik usaha, belajar dari pemakaian, ada pencarian, ada "+ Tambah", deteksi nama mirip**, bukan daftar tetap dari pengembang.

## 12. Uji persona UMKM

Lihat `docs/audit/2026-11-12_PERSONA_DAN_DESAIN_UMKM.md`: 10 persona, 4 dari 10 hampir tidak bisa memakai app dengan wajar (jasa/sewa, kelontong), dan usulan desain G1–G3. Temuan tambahan dari uji tangan: kotak satuan di editor produk bisa kehilangan fokus keyboard sehingga teks tercampur ke kolom nama (perlu direproduksi di HP nyata).

## 8. Cara mengulang audit ini

```
./gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease   # PowerShell, PATH dibersihkan
./gradlew.bat :app:connectedDebugAndroidTest                                                    # emulator menyala
grep -c "OutlinedTextField(" per berkas; wc -l file besar; grep jargon di strings.xml
```
