# Audit Menyeluruh Mankeu (2026-11-12)

> **Tujuan:** bahan serah terima ke agen perencana (Fable) untuk menyusun rencana berikutnya.
> **Sifat:** read-only. Tidak ada kode yang diubah. Disusun oleh Sonnet 5.5 (effort rendah) atas permintaan pemilik.
> **Status fakta:** `terverifikasi` = dijalankan atau dibaca langsung pada audit ini. `dilaporkan` = hanya dari laporan
> agen sebelumnya. `belum diverifikasi` = tidak bisa dicek tanpa HP nyata atau tindakan pemilik.

## 1. Ringkasan

Kondisi teknis **sehat**. Tes otomatis hijau, build release lulus, dan tidak ada utang teknis yang menonjol di kode baru.
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

**T4. Tidak ada bukti pada perangkat nyata.**
Semua uji dijalankan di emulator dengan keyboard suara. Belum terverifikasi: tombol Back, Gboard, dropdown, Kirim ke
WhatsApp, masuk Google dan token Drive, sinkron dua HP lintas versi schema, dan baterai/memori. Ini `belum diverifikasi`
(register R1, R17).

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

## 5. Yang sudah baik (jangan dibuang)

- Satu sumber angka (`MonthlySummary`) dan tes `OneNumberTest`; angka acuan Oktober keripik stabil.
- Kaidah engineering yang terbukti: tes ditulis dulu sampai gagal, review terpisah, log di commit yang sama.
- Migrasi dan backup berantai 12→13→14 dengan tes, lantai sinkron schema 14, dan pemberitahuan data dilewati.
- Komponen form bersama dan token desain (tanpa warna hard-code di layar baru).
- Dokumentasi serah terima: rencana lanjutan, prompt agen, register 20 temuan, ledger status.

## 6. Keputusan yang masih menunggu pemilik

1. Izin menyalakan Gboard di emulator agar tombol Back dan keyboard bisa diuji (atau uji langsung di HP nyata).
2. Kapan merge branch Fase 2 ke main: sebelum atau sesudah Paket G (saran audit: **sesudah** G, karena DEC-012).
3. Teks privasi, listing toko, Data Safety (masih menyebut Firebase).
4. Unggah AAB bertanda tangan ke internal testing (tindakan pemilik).

## 7. Saran urutan untuk perencana (bukan keputusan)

1. **Konfirmasi dengan pemilik** di HP nyata: apakah jitter dan bug yang dilaporkan masih terjadi di build `b217794`?
   Ini menentukan apakah Paket N dianggap selesai. Minta daftar bug dan screenshot dari pemilik.
2. **Review independen** Paket B dan N (read-only, model kuat), dengan fokus aturan serap pembayaran hari yang sama
   dan ukuran jank sheet 32,69 %.
3. Lanjut paket C (Rencana), D (bahasa + bantuan), F (titip + tempo), G (template), lalu E (uji ulang), H (review
   akhir + merge), sesuai `docs/superpowers/plans/2026-11-09-rencana-lanjutan-lengkap.md`.
4. Pertimbangkan satu paket baru: **uji keterpakaian dengan pengusaha** (satu sesi 30 menit, tugas harian, direkam)
   karena sumber keluhan pemilik adalah rasa pakai, bukan hasil tes.
5. Pertimbangkan satu paket baru: **pemecahan file besar** hanya setelah merge, bukan sebelum.
6. Tambah pengaman kualitas yang bisa dijalankan agen murah: daftar kata terlarang di string layar non-laporan
   (`ScreenLanguageTest`), batas lint peringatan, dan pemeriksaan `gfxinfo` otomatis pada alur utama.

## 8. Cara mengulang audit ini

```
./gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease   # PowerShell, PATH dibersihkan
./gradlew.bat :app:connectedDebugAndroidTest                                                    # emulator menyala
grep -c "OutlinedTextField(" per berkas; wc -l file besar; grep jargon di strings.xml
```
