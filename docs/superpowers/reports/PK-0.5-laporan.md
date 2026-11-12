# PK-0.5: perbaikan buku kas dan identitas pemilih

Tanggal: 2026-11-12. Agen: Codex. Branch: `fase2-nota-piutang`; dasar implementasi `3564fdb`.

## Lingkup dan perilaku

[user-confirmed] Pemilik mendelegasikan pilihan F2; DEC-033 mempertahankan total seluruh kas pada
Catatan dan menambah angka kas bulanan tanpa mengganti ringkasan operasional. Implementasi ini
menangani F1 sampai F4 dari PK-0.3, N1 sampai N3 dari PK-0.4, serta F5 yang direproduksi saat review
lanjutan: status belanja tempo dapat berubah mengikuti halaman yang termuat.

- [verified] F1: jendela awal memakai anggaran 200 **baris**, dari gabungan tanggal event dan nota,
  lalu melengkapi seluruh tanggal batas. Ini tidak memuat 200 tanggal berbeda. `LedgerWindow`
  membaca event, nota, baris nota, pembayaran, kontak, batas tanggal, status pembayaran pemasok,
  dan penanda riwayat dalam satu transaksi Room. Tanggal yang hanya berisi nota ikut menentukan batas.
- [verified] Tombol **Muat catatan lebih lama** memperluas jendela, termasuk ketika pencarian atau
  filter tidak menemukan hasil. Nota di luar jendela tetap tersedia untuk detail/status dan tautan
  pembayaran yang lebih baru; baris nota lamanya baru tampil setelah tanggalnya termuat. Pergantian
  unit menghapus permintaan riwayat sebelumnya. Event yang telah dimuat tetap berada di `state.events`,
  sehingga editor menemukan ID asli, termasuk catatan di luar 200 baris awal.
- [verified] F2: `CashTotals` membaca akun CASH dan sisi posting dari kebijakan jurnal yang sama.
  `MonthlySummary.totalCashInRupiah/totalCashOutRupiah` mencakup operasi, pendanaan, dan investasi.
  `moneyInRupiah/moneyOutRupiah` serta laba mempertahankan arti sebelumnya. Uang tetap `Long`;
  tidak ada perubahan pembulatan, model penyimpanan, skema, atau dependensi.
- [verified] F3: event digabung menurut ID kanonis; pembayaran berbeda tetap dijumlahkan masing-masing.
  Nota mencari keterangan, kategori, dan referensi pembayaran pada tanggal yang diserapnya.
  Pembayaran pada tanggal lain mencari metadata event-nya sendiri. Pencarian tidak menggandakan kas.
- [verified] F4: chip lewat jatuh tempo dan intent `NoteFilter.OVERDUE` memakai predikat nota yang
  sudah ada. Nota belum tempo dan yang sudah lunas tidak lolos.
- [verified] F5: filter Belum lunas mengurangi total belanja tempo dengan seluruh pembayaran terkait.
  Agregat status ini diambil dari hubungan `settlesEventId`, tanpa batas tanggal tampilan, dalam
  transaksi yang sama. Kas tetap berasal dari event pada tanggal yang termuat. Data lama yang
  tanggal belanjanya pernah dimajukan tetap terbaca dengan benar; tidak ada migrasi atau koreksi data.
- [verified] N1: `PickerPage` mewajibkan kunci identitas entitas, dengan namespace terpisah dari aksi
  tambah. Semua pemanggil diperbarui: produk untuk produksi, produk nota, pelanggan, dan pemilih
  bahan pada `MoneyForms` yang memakai argumen generik. Nama yang sama tetap diizinkan.
- [verified] N2/N3: catatan koreksi bertanggal ditambahkan ke B-laporan, N-laporan, dan bug bash lama,
  sambil mempertahankan isi historis. Nama tes batas kasar JVM diselaraskan dengan assertion 500 ms;
  klaim lama kurang dari 50 ms tidak dipakai sebagai bukti benchmark.

## Bukti merah sebelum perbaikan

Semua Gradle memakai PowerShell dengan PATH yang dibersihkan:

```powershell
$env:Path = ($env:Path -split ';' | Where-Object { $_ -notmatch '"' }) -join ';'
```

| Pemeriksaan pada kode lama | Hasil terverifikasi |
|---|---|
| `:app:testDebugUnitTest --tests com.mankeu.presentation.UnifiedLedgerTest` | 11 tes, 2 gagal: metadata pembayaran terserap tidak ditemukan dan tanggal pembayaran berikutnya hilang bila event hanya tersedia dalam riwayat nota |
| `:app:testDebugUnitTest --tests com.mankeu.domain.MonthlySummaryTest` | 4 tes, 1 gagal: kontrak total CASH belum tersedia |
| Domain lewat tempo | 14 tes, 1 gagal: pilihan OVERDUE belum tersedia |
| Empat tes instrumented awal: LedgerWindowTest, LedgerEntryScreenTest, PickerPageTest | 4 tes, 4 gagal: 201 menjadi 200; intent lewat tempo menampilkan Besok; tombol riwayat tidak ditemukan saat filter kosong; exception kunci `Sama` dipakai dua kali |
| Domain belanja tempo lunas | 19 tes, 1 gagal: belanja yang sudah dibayar penuh masih masuk Belum lunas |
| `LedgerWindowTest#editedPaidPurchaseKeepsItsStatusBeforeAndAfterLoadingOlderPayment` | 1 tes, 1 gagal: pembayaran 10 November tidak menentukan status belanja yang diubah ke 12 November ketika batas daftar 12 November |

[verified] Setelah API kunci diwajibkan, fixture pemilih mengirim ID eksplisit. Fixture lama pembayaran
hari yang sama juga diperbaiki: satu pembayaran yang muncul di dua sumber sekarang memakai satu ID
yang sama, sesuai data tersimpan. Dua pembayaran yang benar-benar berbeda diuji terpisah. Kekurangan
fixture pelanggan wajib, matcher label lewat tempo yang juga muncul pada status baris, dan jalur
Produksi yang disembunyikan panduan awal diselesaikan pada tes; tidak ditutupi dengan perubahan domain.

## Penerimaan yang dapat dijalankan ulang

| Kontrak | Nama bukti utama |
|---|---|
| 201 event satu hari = Rp201.000; batas 199 + 3 event melengkapi tanggal; nota tanpa event ikut riwayat | `LedgerWindowTest.recentRowsIncludeTheEntireBoundaryDay`, `rowBudgetIncludesNoteOnlyDaysAndExtendsWholeBoundaryDate` |
| Pembayaran lama di luar 200 baris dapat dimuat; pembayaran baru ke nota lama tetap tertaut | `oldLaterPaymentLoadsWithItsOriginalIdAndCanBeEdited`, `newerPaymentLinksToNoteOutsideTheLoadedDateRange` |
| Nota lunas pada hari nota, sekali Rp600.000 | `fullyPaidSameDayNoteAbsorbsTheCanonicalPaymentExactlyOnce` |
| Uang muka D dan pelunasan D+1; dua pembayaran berbeda pada D; ID ganda antarsumber tidak dihitung dua kali | `UnifiedLedgerTest.depositThenSettlementAndMovedNoteKeepOriginalPaymentDates`, `twoCanonicalSameDayPaymentsAreAbsorbedOnceEach` dan tes Room perubahan/penghapusan di bawah |
| Tanggal nota dimundurkan menjaga tanggal kas; memajukannya melewati pembayaran ditolak; hapus nota menghapus seluruh pembayaran | `LedgerWindowTest.paymentsRemainOnTheirDatesAfterNoteEditThenDisappearOnDelete` |
| Kas bulanan semua peran sama dengan jumlah hari lengkap | `UnifiedLedgerTest.fullDailyCashReconcilesToMonthlyCashAcrossEveryRole`: masuk Rp1.650.000, keluar Rp430.000; operasional tetap Rp650.000/Rp130.000; laba tetap dari ProfitInputs |
| Metadata pembayaran sama hari dan beda hari pada Semua/Uang masuk | `absorbedPaymentRetainsDescriptionCategoryAndReferenceSearch`, `laterPaymentRetainsDescriptionCategoryAndReferenceSearch`; Compose `searchingAbsorbedPaymentReferenceShowsItsNote` |
| Filter lewat tempo dan intent masuk | `LedgerEntryScreenTest.overdueChipKeepsOnlyLateNotes`, `incomingOverdueIntentKeepsOnlyOverdueNotes` |
| Cari tanpa hasil lalu muat riwayat, ubah baris lama, Simpan mempertahankan ID dan jumlah 201 event | `CatatanUnifiedLedgerTest.oldFilteredCashRowLoadsAndSavesTheOriginalEventId` |
| Unit A diperluas, pindah B, kembali A mengulang 200 baris; setiap emisi tidak membawa baris unit lain | `LedgerWindowViewModelTest.historyResetsOnUnitSwitchAndNeverLeaksPreviousUnitRows` |
| Ganti nama kontak menyegarkan snapshot; unit hilang tidak mematikan aliran | `LedgerWindowTest.contactRenameRefreshesTheTransactionCoherentNoteSnapshot`, `missingUnitIsAnEmptyWindowInsteadOfADeadFlow` |
| Status belanja lunas tidak berubah sebelum/sesudah riwayat; metadata status tidak menambah kas pada tanggal yang belum termuat | `editedPaidPurchaseKeepsItsStatusBeforeAndAfterLoadingOlderPayment`: kas jendela Rp199.000, sesudah riwayat Rp249.000, status lunas tetap |
| Nama pemilih sama dapat dipilih menurut ID; ubah nama, urut ulang, cari/hapus pencarian, dan aksi tambah tidak bertabrakan | tiga tes `PickerPageTest`, serta `CatatanUnifiedLedgerTest.productionPickerAllowsDuplicateProductLabelsAndRecordsTheSelectedId`; alur nota menyimpan ID produk dan pelanggan yang dipilih |

## Hasil akhir dan pemulihan

[verified] Perintah integrasi berikut selesai `BUILD SUCCESSFUL` dalam 2 menit 39 detik pada 08:15 WIB:

```powershell
./gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.mankeu.data.LedgerWindowTest,com.mankeu.presentation.LedgerEntryScreenTest,com.mankeu.presentation.PickerPageTest,com.mankeu.presentation.LedgerWindowViewModelTest,com.mankeu.presentation.CatatanUnifiedLedgerTest,com.mankeu.presentation.SaleNoteFlowTest'
```

| Lapisan | Hasil terverifikasi dari XML |
|---|---|
| Seluruh suite JVM, dijalankan ulang sesudah pembersihan resource | 28 suite, **164 tes**, 0 gagal/error, 0 dilewati; termasuk 17 UnifiedLedgerTest, 4 MonthlySummaryTest, dan gate bahasa PK-0.6 |
| Instrumented terfokus, AVD Medium_Phone/API 30, sebelum pembersihan resource usang | **31 tes**, 0 gagal/error: LedgerWindowTest 9, LedgerEntryScreenTest 4, PickerPageTest 3, LedgerWindowViewModelTest 1, CatatanUnifiedLedgerTest 6, SaleNoteFlowTest 8 |
| `lintDebug`, sesudah pembersihan resource | **0 error, 87 warning**; tidak diklaim bebas warning |
| `git diff --check` | Lulus |

[verified] Review akhir menghapus hanya `ledger_truncated_title` dan `ledger_truncated_body` yang
sudah tidak dipakai, pada kedua bahasa. Daftar dasar PK-0.6 menyusut dari 243 menjadi 242 pasangan
setelah `ledger_truncated_body|ekspor` dihapus. Gate `ScreenLanguageTest` lulus 7/7 dan lint lulus.
Perintah akhir `./gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` kemudian
selesai `BUILD SUCCESSFUL` dalam 8 detik, dengan seluruh 164 tes JVM lulus kembali. Tes perangkat
31/31 tidak diulang untuk penghapusan label yang tidak dipakai ini. Hasil awal lint 91 warning
digantikan hasil akhir 87 warning, sesuai XML hasil pemeriksaan.

[verified] Runner melepas paket aplikasi setelah tes; APK debug hasil build dipasang kembali sebelum
pemulihan. APK yang menjalankan 31 tes mempunyai SHA-256
`40fecb3536624b8542f99da1dbf52e30da25c02cf6c85f1a3866409f5fa9d623`.
APK akhir sesudah pembersihan resource juga dipasang dan data awal dipulihkan ulang;
SHA-256 akhir: `4550b1c7ee83d85273223ce2f71e72587c88d8a74b24d15ca5c67bbbef756d7c`.
Emulator diserahkan dalam keadaan aplikasi berhenti. Cadangan awal berada hanya di direktori TEMP
audit lokal; SHA-256 arsip `9de082faddebf467370189b829a4463cc08a4e4e9c98e21bdec20d101768150f`.
Skrip pemulihan membandingkan isi seluruh berkas dan memastikan empat hash sebelum = sesudah serta
tidak ada berkas tambahan dalam cakupan cadangan:

| Berkas | SHA-256 sebelum = sesudah |
|---|---|
| `databases/mankeu.db` | `2ce762b12aa583fe8329f428a75c0b66a7f30eb06d9a6f987f4a5da4bc277615` |
| `databases/mankeu.db-wal` | `5150e7a3a6badd6b45b5bcc607a23183e0be6a1b114a024f4a6512b1415644b7` |
| `databases/mankeu.db-shm` | `8ffd50eb46a91fd7da8227c24a71248429d6b48eb2318f20ab8341cacd10abf0` |
| `shared_prefs/mankeu_ui_prefs.xml` | `d762e362c7cf3242aeac92f5c46229ad07f5e69faa2b85bd76cde425e25c6c1c` |

[verified] F1 sampai F5 dan N1 memiliki bukti integrasi di atas. N2/N3 ditangani sebagai koreksi
ketepatan laporan. Lingkup kode PK-0.5 dapat diteruskan ke sesi induk untuk commit bersama log;
status jank tetap mengikuti batas di bawah.

## Batas klaim

[unknown] Penyebab jank tinggi belum terisolasi. Tidak ada perubahan animasi/insets atau pengukuran
performa baru dalam PK-0.5. X3 tetap belum selesai; bukti dan 12 sampel PK-0.4 berlaku sebagai
pengukuran historis dengan batas yang dicatat di review tersebut. Keputusan pada HP pemilik tetap PK-5.4.
Hari yang sangat padat dapat melebihi 200 baris karena kelengkapan tanggal; ini keputusan ketepatan kas,
bukan klaim kecepatan atau pemakaian memori. Riwayat nota lengkap memang sudah tersedia sebelumnya.

[verified] Pengujian memakai data sintetis di database dalam memori. Tidak ada data keuangan asli,
cadangan perangkat, kredensial, atau nama usaha awal yang dimasukkan ke repo. Tidak ada perubahan skema
atau format Excel. Tidak menguji ulang seluruh instrumented suite aplikasi, ekspor/impor, migrasi,
cadangan/sinkronisasi produksi, perangkat pemilik, atau penerimaan pengguna; hasil terfokus tidak diberi
label release-ready. Commit beserta log bersama ditangani sesi induk.
