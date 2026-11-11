# PK-0.7: alasan tombol utama belum bisa ditekan

Tanggal: 2026-11-12. Pelaksana: Codex, subagen PK-0.7. Cabang: `fase2-nota-piutang`.

## Perubahan

**verified:** `FullPageForm` menerima `saveBlockedReason: String?`, `MankeuSheet` menerima
`confirmBlockedReason: String?`, dan `LineEditSheet` meneruskan `saveBlockedReason` ke sheet.
`null` mengaktifkan tombol. Alasan bukan `null` menonaktifkan tombol dan ditampilkan tepat sebelum
tombol utama, dengan tag `save_blocked_reason`. Alasan kosong ditolak dengan `require`.
Sheet informasi tanpa tindakan konfirmasi tidak menampilkan alasan konfirmasi.

**verified:** semua pemanggil API Boolean lama sudah diganti. Alasan mengikuti isian pertama yang
belum memenuhi syarat: nama, satuan, hasil sekali buat, jumlah, harga, kategori, tanggal, kata
sandi, atau berkas cadangan. Nama kontak ganda, diskon berlebih, pembayaran berlebih, tanggal
bayar terlalu awal, dan urutan periode memakai penjelasan spesifik. Penghapusan yang masih
berjalan menyebut bahwa pengguna perlu menunggu. Tombol tambah kategori di dalam sheet dan
tombol penyimpanan hasil pembacaan Excel juga diberi alasan.

**verified:** alasan baru tersedia dalam bahasa Indonesia dan Inggris. Daftar dasar PK-0.6 tidak
diubah. Syarat penyimpanan, perhitungan uang, data tersimpan, skema, dan dialog buang perubahan
tidak diubah.

## Tes dan bukti

**verified, RED pertama:** perintah berikut dijalankan sebelum kode produksi diubah. Ketiga tes
masih memakai API Boolean lama dan gagal pada assertion karena tag `save_blocked_reason` tidak
ditemukan. Kompilasi berhasil; ini kegagalan perilaku, bukan kegagalan tanda tangan API.

```powershell
./gradlew.bat connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.mankeu.presentation.components.form.FormComponentsTest#fullPageFormExplainsBlockedSaveAndEnablesWhenReasonClears,com.mankeu.presentation.components.form.FormComponentsTest#sheetExplainsBlockedConfirmAndEnablesWhenReasonClears,com.mankeu.presentation.components.form.FormComponentsTest#lineEditSheetExplainsBlockedSaveAndEnablesWhenReasonClears'
```

Hasil: 3 tes, 3 gagal, `BUILD FAILED in 18s`; alasan kegagalan ketiganya:
`Expected exactly '1' node but could not find any node that satisfies: (TestTag = 'save_blocked_reason')`.

**verified, RED alasan kosong:** sebelum `require` ditambahkan, kedua tes berikut gagal dengan
`Expected exception: java.lang.IllegalArgumentException`. Hasil: 2 tes, 2 gagal, 1 menit 18 detik.

```powershell
./gradlew.bat connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.mankeu.presentation.components.form.FormComponentsTest#fullPageFormRejectsBlankBlockedReason,com.mankeu.presentation.components.form.FormComponentsTest#sheetRejectsBlankBlockedReason'
```

Semua perintah Gradle memakai PATH yang sudah dibersihkan dari entri bertanda kutip:

```powershell
$env:Path = ($env:Path -split ';' | Where-Object { $_ -notmatch '"' }) -join ';'
```

**verified, GREEN:** perintah berikut lulus pada `Medium_Phone`, Android 11/API 30:

```powershell
./gradlew.bat connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.mankeu.presentation.components.form.FormComponentsTest,com.mankeu.presentation.components.MankeuSheetTest'
./gradlew.bat testDebugUnitTest lintDebug
git diff --check
```

Hasil: 16 tes komponen, 0 gagal, 0 dilewati (1 menit 28 detik); 155 tes JVM, 0 gagal,
0 error, 0 dilewati; lint 0 error dan 89 peringatan (gabungan JVM/lint 1 menit 28 detik).
`ScreenLanguageTest` termasuk dalam 155 tes JVM. Pemeriksaan whitespace lulus. Kompilasi masih
menyebut deprecation Google Sign-In dan aturan Compose Test yang sudah dipakai sebelumnya.

**verified, bukti alur nyata:** aplikasi dibuka melalui `MainActivity`, dibuat usaha sintetis,
lalu Produk → Tambah produk. Nama produk diisi `Keripik_Uji_PK07`, satuan bawaan `bungkus`
dipertahankan, dan harga jual diisi Rp5.000. Hasil sekali buat sempat diisi 10 lalu dikosongkan.
Alasan `Isi hasil sekali buat lebih dari 0 dulu.` tampil tepat di atas Simpan yang mati.

- [Screenshot](../evidence/PK-0.7/product-empty-yield.png), 1080 × 2400; sudah ditinjau visual.
- [Pohon UI](../evidence/PK-0.7/product-empty-yield-ui.xml). Assertion Python atas XML memastikan
  nama sintetis dan Rp5.000 ada, hasil masih kosong, alasan tampil sekali, posisi alasan sebelum
  tombol, dan induk teks Simpan mempunyai `enabled="false"`.

Alur ini tidak menyimpan produk yang belum lengkap. Tes komponen secara terpisah membuktikan
alasan `null` mengaktifkan tombol dan kliknya memanggil tindakan simpan.

## Tinjauan semua `enabled =` yang tersisa di layar

Pencarian: `rg -n 'saveEnabled|confirmEnabled|enabled\s*=' app/src/main/java/com/mankeu/presentation/screens`.
API lama tidak ditemukan. Dua belas kemunculan `enabled =` tersisa:

| Berkas dan baris | Kontrol | Alasan dipertahankan |
|---|---|---|
| `AccountSettingsScreen.kt:344` | Buka penghapusan data | Mati saat penghapusan berjalan; alasan menunggu sekarang tepat di atas tombol. |
| `ManageScopeScreen.kt:318` | Chip status aktif | Penanda pilihan aktif, bukan tindakan penyimpanan. Label `Aktif` menjelaskan statusnya. |
| `MoreScreen.kt:230` | Unduh Excel | Tindakan unduh, bukan form; `TransferUiState.Working` sudah menampilkan progres dan nama pekerjaan. |
| `MoreScreen.kt:237` | Unduh PDF | Penjelasan sama dengan tombol Excel. |
| `MoreScreen.kt:244` | Unduh templat | Penjelasan sama dengan tombol Excel. |
| `MoreScreen.kt:249` | Buka cadangan | Dinonaktifkan saat pemindahan berkas berjalan, dengan progres dan nama pekerjaan yang sama. |
| `MoreScreen.kt:485` | Simpan hasil pembacaan Excel | `blockedReason` menjelaskan konflik pertama, masalah berkas, atau berkas kosong tepat di atas tombol. |
| `MoreScreen.kt:519` | Implementasi `ExportAction` | Meneruskan status tiga tindakan unduh yang disebutkan di atas. |
| `MoreScreen.kt:617` | Tambah kategori | Nama kosong sekarang dijelaskan tepat di atas tombol. |
| `MoreScreen.kt:842` | Simpan pemulihan darurat | Tindakan tambahan pada pilihan cadangan; teks `backup_recovery_none` yang sudah ada menjelaskan tidak ada data pemulihan. |
| `SaleNoteScreens.kt:463` | Kurangi jumlah | Kontrol angka tambahan; minimum jumlah membatasi pengurangan. Bukan tombol utama form. |
| `SaleNoteScreens.kt:731` | Buka terima pembayaran | Tindakan pada rincian nota; status lunas dan sisa tagihan sudah ditampilkan. Form pembayaran memakai alasan jumlah/tanggal tersendiri. |

## Batas verifikasi

**reported:** PK-0.4 menyerahkan emulator sesudah memulihkan empat berkas data asli secara identik.
Cadangan asli: `C:/Users/super/AppData/Local/Temp/pk04-review/original-app-data.tar`.

**verified:** sesudah bukti alur, aplikasi dihentikan dan empat berkas asli dipulihkan dari
cadangan tersebut. Arsip hasil pemulihan dibandingkan dengan arsip asli: seluruh SHA-256 sama
dan tidak ada berkas tambahan. APK dengan perubahan PK-0.7 tetap terpasang. Data asli mempunyai
usaha yang sudah ada dengan data keuangan kosong; usaha sintetis tidak dipertahankan. Nama dan
isi pribadi data asli tidak dimasukkan ke bukti yang dilacak Git. Pemeriksaan tambahan yang
sempat mengira keadaan asli tanpa usaha gagal karena asumsi itu salah; kesamaan empat hash
tetap lulus dan menjadi bukti pemulihan.

**unverified:** belum mengklaim seluruh pemanggil diuji manual, tablet, TalkBack, atau ukuran font
200%. Tidak ada perubahan impor/ekspor, skema, jaringan, atau kebijakan privasi. Kegagalan kunci
nama ganda di `PickerPage` yang ditemukan PK-0.4 berada di paket PK-0.5, di luar perubahan ini.

Log aktivitas, keputusan API, changelog, dan commit atomik dikelola agen induk bersama berkas
kode, tes, dan laporan ini. Subagen tidak melakukan commit atau push.

Usulan entri aktivitas: mengganti tiga API tombol utama, menambahkan alasan dua bahasa pada
semua pemanggil dan dua tombol penyimpanan di dalam layar, membuktikan RED/GREEN, mencatat
hasil 16 tes komponen serta 155 tes JVM/lint, menyimpan bukti hasil kosong, dan memulihkan
data emulator. Usulan changelog: tombol Simpan/konfirmasi yang belum bisa ditekan sekarang
menjelaskan isian yang perlu dilengkapi tepat di atas tombol.
