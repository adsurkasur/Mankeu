# Handoff progres Mankeu — 13 November 2026

## Status kerja

- Branch kerja: `fase2-nota-piutang`; jangan merge ke `main` dan jangan mengubah superproject.
- Paket terakhir yang selesai dan sudah di-push: PK-1.6, commit `5f81c45`.
- Pekerjaan aktif sesudahnya adalah menutup pemeriksaan lanjutan PK-1.2. Belum selesai dan belum di-commit.
- `git status` terakhir sebelum dokumen ini: 10 berkas berubah (7 tes instrumentasi, 2 sumber data, 1 resource). Perubahan itu milik sesi sebelumnya dan harus dipertahankan, diverifikasi dengan `git diff`, lalu diputuskan/di-commit sesuai aturan paket. Jangan reset atau checkout berkasnya.
- Paket 1.2 checkpoint terdahulu mencatat K-1 sampai K-4 dijawab; lihat tabel 4.16 yang kini otoritatif sebelum kerja lanjutan. K-8 historis disebut belum dijawab dan membatasi PK-1.4. Jangan menebak keputusan produk.

## Berkas yang berubah tetapi belum di-commit

- `app/src/main/java/com/mankeu/data/FinanceDao.kt`: pencocokan kategori untuk biaya tunai lama yang `categoryId`-nya null memakai nama snapshot persis (case-insensitive, trim), dibatasi pada business yang sama.
- `app/src/main/java/com/mankeu/data/FinanceRepository.kt`: biaya manual yang diketahui tidak lagi dianggap unknown hanya karena `ProductCost.complete` adalah penanda kelengkapan resep.
- `app/src/androidTest/java/com/mankeu/data/SalesFlowTest.kt`: tes integrasi biaya lama tanpa categoryId.
- `app/src/androidTest/java/com/mankeu/data/ImportCommitTest.kt`, `ScaleSimulationTest.kt`: fixture menandai basis ITEM_COST secara eksplisit agar sesuai asumsi golden lama setelah usaha baru memakai SIMPLE.
- `app/src/androidTest/java/com/mankeu/presentation/HomeScreenTest.kt`, `OneNumberTest.kt`, `ProductEditorTest.kt`, `SaleNoteFlowTest.kt`: selector UI diselaraskan sebagian dengan label sekarang.
- `app/src/main/res/values/strings.xml`: format placeholder riwayat produksi `%1$d` diubah menjadi `%1$s` untuk menerima teks jumlah.

Perubahan ini bukan bagian dari PK-1.6 commit. Telusuri diff mentah dan sejarah sebelum menganggap semuanya siap. `git status` pada 07:40 WIB menunjukkan hanya daftar tersebut.

## Bukti verifikasi yang sudah ada

- Suite JVM penuh setelah PK-1.6: 193 tes, 0 gagal (dilaporkan dan tercatat pada sesi sebelumnya).
- Instrumentasi penuh pada AVD Gradle terisolasi `Mankeu_PK12_Proof(AVD) - 11`: 192 tes dilaporkan, 10 gagal, 10 terlewati. XML saat uji penuh mencatat kegagalan awal antara lain fixture basis lama, label UI usang, dan `IllegalFormatConversionException` pada riwayat produksi. Ini terjadi sebelum sebagian perbaikan di atas.
- Tes fokus data sesudah perbaikan: `LedgerWindowTest#manualCostAndUnknownCostStayDistinct` lulus; `ImportCommitTest` dan `ScaleSimulationTest` fokus lulus 2/2; migrasi dan data kategori-null lulus 2/2 (semua hasil tersebut dilaporkan dari sesi sebelumnya).
- Uji fokus terakhir tujuh kasus UI berjalan pada 13 Nov; hasil XML tersimpan di `app/build/outputs/androidTest-results/connected/debug/TEST-Mankeu_PK12_Proof(AVD) - 11-_app-.xml`. 4/7 lulus, 3/7 gagal:
  1. `OneNumberTest.keripikBuSriHasOneProfitAndOneGap`, assertion `scrollTo("Uang vs rencana")`/tap gagal di baris 158 (lihat stack penuh XML; baris sumber setelah perubahan 158).
  2. `ProductEditorTest.recipePerBatchGivesHppPerSaleUnitAndSaves`, masih mencari input lama `Biaya per batch` pada baris 91; selector itu belum diperbarui.
  3. `StockHistoryScreenTest.historyListsEveryLineAndOnlyProductionAndCountsCanBeDeleted`, masih mengharapkan teks lama `Produksi 1 batch`.
- UI emulator tersedia sebagai `emulator-5556` pada `adb devices`, tetapi `cua.getState()` gagal (`failed to write kernel assets: The system cannot find the path specified`). Tidak ada screenshot atau pemeriksaan layar langsung pada sesi terakhir. AVD tes terisolasi bukan emulator pengguna.
- Jangan jalankan instrumentasi yang menghapus data pada `emulator-5556`. Pakai AVD terisolasi Medium_Phone/Gradle; aturan repo menyebut tes instrumented dapat menghapus data app.

## Audit tambahan yang diminta pemilik

### Bahasa Inggris dalam app

- Pemilik meminta seluruh app berbahasa Indonesia untuk saat ini setelah melihat English saat tes.
- Bukti sumber: `AppPreferences.kt` mendukung `AppLanguage.INDONESIAN` dan `ENGLISH`, default baru Indonesia, tetapi menyimpan pilihan device dan menerapkannya melalui `MainActivity.attachBaseContext`; jadi English dapat tampil jika preference sebelumnya tersimpan.
- `AccountSettingsScreen.kt` masih menampilkan LanguagePicker dan pilihan English. `values-en/strings.xml` berisi terjemahan penuh (sekitar 899 baris string). Karena preferensi Indonesia hanya default, perilaku saat ini tidak menjamin semua pengguna selalu berbahasa Indonesia.
- Pencarian literal sumber Compose menemukan setidaknya satu teks produk literal yang perlu ditinjau: `MoreScreen.kt` “Pencadangan Google Drive” (sudah Indonesia, nama produk wajar). Literal teknis lain seperti `label="screen"`, `overlay`, `meter` adalah label internal/test semantics, bukan teks layar.
- Belum dilakukan inventaris runtime menyeluruh, belum dibuktikan bahasa preference emulator, belum diputuskan apakah English disembunyikan untuk sementara atau tetap dipertahankan sebagai kapabilitas tidak terlihat. Jangan menghapus kapabilitas tanpa memeriksa spec V-4/aturan paket; bila tidak diatur, ajukan pilihan eksplisit.

### Dialog tiga aksi stok

- Sumber `StockSection.kt` menunjukkan `confirmButton` berisi Column dua TextButton yang ditumpuk dan rata kanan (`Ya, catat belanja`, `Tidak, tambah stok saja`); `dismissButton` `Kembali` berada di slot terpisah. String berada di `values/strings.xml` sekitar baris 501–504.
- Ini bukti layout kode yang mendukung masukan pemilik bahwa susunan terasa janggal. Screenshot nyata tidak didapat pada sesi ini, sehingga dampak visual di ukuran layar tertentu belum terverifikasi.
- Rekomendasi audit untuk agent selanjutnya: reproduksi dialog di AVD dan screenshot/XML; evaluasi hierarki tiga pilihan agar dua jalur data terlihat sebagai pilihan utama dan kembali/cancel sebagai aksi sekunder, sambil mempertahankan perilaku dan input quantity/notes. Jangan memodifikasi sebagai bagian PK-1.2 jika tidak termasuk paket aktif; catat ke paket layar/audit sesuai rencana. Pemilik sudah memberi prinsip “pilih terbaik bagi pengguna” untuk keputusan UX sebelumnya, tetapi tetap dokumentasikan alasan dan tes.

## Batas dan urutan kerja yang disarankan

1. Baca `AGENTS.md`, README/guide/requirements/domain/quality gates yang disyaratkan repo, lalu paket dan spec yang tepat saja. Perintah pemilik yang lebih sempit mungkin membatasi berkas yang dibaca.
2. Jalankan `git status --short`, `git diff --` untuk 10 berkas, dan `git log --oneline -5`. Pertahankan perubahan.
3. Selesaikan kegagalan tes fokus: periksa stack XML untuk OneNumber, ganti selector ProductEditor dari teks lama dengan label resource aktual, dan cocokkan label StockHistory dengan app strings. Ulangi hanya tes terarah dulu.
4. Jalankan JVM penuh dan lint hanya bila kriteria PK-1.2 membutuhkannya; jangan menyatakan paket lengkap hanya dari tes terarah.
5. Lanjutkan verifikasi kategori-null: pastikan nama snapshot aman terhadap variasi spasi/case, kategori duplikat, kategori kustom, dan business lain; cek apakah cocokannya dapat menggandakan biaya bila nama kategori duplikat. Tambahkan tes yang mengunci perilaku yang dipilih oleh dokumen/keputusan.
6. Selidiki penyebaran `costKnown` ke MonthlySummary/laporan/ekspor dan perbedaan `PlanAssembly` yang dicatat di `PK-1.2-laporan.md`, tetapi jangan mengambil paket lain diam-diam. PK-1.2 checkpoint mungkin memerlukan paket integrasi berikut.
7. Tangani permintaan audit Indonesia dan dialog stok terpisah, berdasarkan scope paket yang benar; simpan hasil dan pertanyaan produk sebagai pilihan ganda di laporan.
8. Dokumentasikan hasil di laporan paket, `docs/WORK_IN_PROGRESS.md`, `logs/ACTIVITY_LOG.md`, satu baris rencana induk §8, dan changelog/decision log jika relevan. Commit path eksplisit, lalu push branch; jangan sentuh superproject.

## Prompt siap salin untuk agent penerus

```text
Lanjutkan progres Mankeu pada repo E:\managed-projects-handoff\mankeu, branch fase2-nota-piutang. Baca dulu docs/superpowers/reports/2026-11-13-handoff-progress.md, lalu verifikasi status/diff/log Git; pertahankan semua perubahan yang ada. Paket selesai terakhir PK-1.6 commit 5f81c45. Fokus aktif tertunda adalah PK-1.2: selesaikan diagnosis kategori-null dan tes gagal yang tercatat di handoff tanpa mengerjakan paket lain. Jangan merge main atau sentuh superproject.

Setelah fokus PK-1.2 stabil, lanjutkan permintaan audit pemilik: app harus Bahasa Indonesia seluruhnya untuk saat ini (investigasi preferensi English tersimpan vs string literal; jangan hapus kapabilitas tanpa memeriksa spec V-4 dan scope), serta audit layout dialog tambah stok dengan tiga aksi “catat belanja”, “stok saja”, “kembali”. Emulator pengguna emulator-5556 jangan di-clear atau dipakai tes instrumentasi; gunakan AVD terisolasi. Catat bukti yang terverifikasi/dilaporkan/belum, termasuk keterbatasan screenshot bila ada.

Ikuti AGENTS.md: dokumentasikan semua perubahan, timestamp nyata, staging path eksplisit, commit/push branch. Aturan Gradle: PowerShell dengan PATH dibersihkan lalu ./gradlew.bat. Teks hanya dengan Edit/Python UTF-8 dan jaga newline. Jangan menjalankan suite penuh kecuali kriteria paket; tes terarah dulu. Teruskan sampai blocker/selesai, jangan berhenti hanya dengan status naratif. Pertanyaan produk yang tidak ditentukan tulis sebagai pilihan ganda dalam laporan.
```
