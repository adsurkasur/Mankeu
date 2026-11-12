# PK-0.8: penghitung interaksi dan kontrak jalan keluar

Tanggal: 2026-11-12, 08.32 WIB. Agen: Codex, paket PK-0.8. Acuan: brief PK-0.8, Lampiran C §1.3–1.4, Lampiran A §§4–8 dan 11–13. Cabang saat pengerjaan: `fase2-nota-piutang`, dasar `09b9835`.

**verified:** `TapCounter` menjalankan aksi Compose yang sebenarnya dan menghitung ketukan serta panggilan pengisian secara terpisah. Suite paket menghasilkan 4 lulus dan 8 diabaikan, tanpa kegagalan. Delapan perjalanan kosong masih berupa kontrak penerimaan untuk paket berikutnya. Hasil ini tidak membuktikan kedelapan perjalanan sudah bekerja.

## Perubahan

- `app/src/androidTest/java/com/mankeu/usability/TapCounter.kt`: pembungkus `performClick` dan `performTextInput`; penghitung per objek; `reset`; jumlah gabungan melalui `interactionCount`. Penghitung bertambah sesudah aksi berhasil. `typedFieldCount` menghitung panggilan pengisian, termasuk dua panggilan pada kotak yang sama, sesuai definisi Lampiran C §1.4. Penghitung ini tidak mengukur durasi atau jumlah karakter.
- `app/src/androidTest/java/com/mankeu/usability/TapCounterTest.kt`: empat tes atas tombol dan kotak teks Compose nyata. Tes memeriksa perubahan UI, pemisahan hitungan, kegagalan aksi, pengukuran ulang, dan kemandirian dua objek.
- `app/src/androidTest/java/com/mankeu/usability/NoDeadEndTest.kt`: delapan metode perjalanan sesuai Lampiran C §1.3. Setiap metode membuka aplikasi asli dengan Room di memori dan `AppViewModel`, lalu menuntut aksi `empty_action` yang menuju formulir penyelesaian. Kasus penjualan bebas juga menuntut nota lunas Rp10.000 tersimpan, nama terlihat di Catatan, dan tidak ada produk yang dibuat otomatis.

Fixture hanya membuat usaha sintetis beserta unit, buku, dan kategori wajib. Tidak ada barang, bahan, kontak, nota, atau transaksi awal. Konfigurasi bahasa Indonesia dipaksakan untuk teks masa depan yang belum punya ID resource; teksnya berasal dari Lampiran A. Navigasi yang sudah ada memakai ID resource, dibaca dari resource Activity agar cocok dengan bahasa UI. Ini bukan penambahan rute aplikasi. ViewModel dibersihkan melalui `ViewModelStore`; basis data ditutup; preferensi dan locale dipulihkan sesudah setiap metode yang dijalankan.

Tidak ada perubahan kode produksi, layar, alur, tag UI, dependensi, skema, atau keputusan produk. Log bersama dan commit ditangani agen induk.

## Bukti RED dan GREEN

Semua perintah Gradle dijalankan dari root repositori melalui PowerShell dengan awalan berikut:

```powershell
$env:Path = ($env:Path -split ';' | Where-Object { $_ -notmatch '"' }) -join ';'
```

| Perintah | Hasil terverifikasi |
|---|---|
| `./gradlew.bat connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.mankeu.usability.TapCounterTest'` | RED: 4 dijalankan, 3 gagal karena hitungan tetap 0 meskipun UI berubah; 1 kasus aksi gagal lulus. Baru sesudahnya penghitung diimplementasikan. |
| `./gradlew.bat connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.mankeu.usability.NoDeadEndTest'` | Diagnostik dengan delapan anotasi `@Ignore` dilepas sementara: 8 dijalankan, 8 gagal, 0 diabaikan. Keenam perjalanan gagal pada aksi `empty_action` yang belum ada; dua kalkulator gagal pada pintu masuk yang belum ada. |
| `./gradlew.bat connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.package=com.mankeu.usability'` | GREEN akhir: XML mencatat **12 kasus, 4 lulus, 8 diabaikan, 0 gagal, 0 error**. Seluruh badan metode, termasuk yang diabaikan, dikompilasi. |
| `./gradlew.bat testDebugUnitTest` | Sukses tetapi `UP-TO-DATE`; tidak dihitung sebagai eksekusi JVM baru. |
| `./gradlew.bat testDebugUnitTest --rerun` | Eksekusi JVM baru: **28 suite, 164 kasus, 164 lulus, 0 gagal/error/diabaikan**. |
| `git diff --check` | Tidak melaporkan kesalahan whitespace pada perubahan terlacak; berkas baru juga diperiksa terpisah. |

Perangkat: `Medium_Phone(AVD) - 11`. XML akhir berada di `app/build/outputs/androidTest-results/connected/debug/TEST-Medium_Phone(AVD) - 11-_app-.xml`; XML JVM di `app/build/test-results/testDebugUnitTest/`. Konsol Gradle akhir mencetak `Finished 20 tests` karena delapan peristiwa skip ikut dihitung lagi. Jumlah kasus yang dipakai laporan ini berasal dari 12 elemen `testcase` pada XML, bukan angka konsol tersebut.

Percobaan awal penghitung sempat gagal kompilasi karena `performTextInput` mengembalikan `Unit`; tanda tangan pembungkus diperbaiki sebelum RED perilaku. Diagnostik perjalanan pertama juga menemukan resource targetContext berbahasa Inggris sementara Activity berbahasa Indonesia. Pengambilan string diperbaiki ke Activity dan seluruh delapan metode dijalankan ulang. Hanya diagnostik ulang dengan kegagalan pada kemampuan yang memang belum tersedia yang menjadi bukti tabel di atas.

Peringatan yang terlihat: API Compose rule lama ditandai deprecated, mengikuti fixture `CatatanUnifiedLedgerTest`; Gradle JVM menampilkan peringatan versi SDK XML. Tidak ada kegagalan dari peringatan itu. Lint dan suite instrumentasi seluruh repositori tidak diulang karena paket ini hanya menambah infrastruktur tes.

## Delapan metode yang diabaikan

Jumlah tepat: **8 metode `@Ignore`, satu per tujuan**. Tidak ada `@Ignore` pada kelas, asumsi yang mengubah kegagalan menjadi lulus, atau penegasan TODO.

| Metode / tujuan | Paket dan alasan yang dicatat pada anotasi |
|---|---|
| `emptyHomeOpensBothSolvingForms` / Beranda | PK-2.3 + PK-2.4 + PK-2.5: belum ada dua `empty_action` Aku jual/Aku beli dan formulir cepatnya. |
| `emptyCatatanOpensBothSolvingForms` / Catatan | PK-2.7 + PK-2.4 + PK-2.5: CTA kosong lama hanya satu; belum ada dua aksi ke formulir cepat. |
| `emptyItemsOpensThreeFieldEditor` / tab barang | PK-2.8: belum ada `empty_action` ke editor Barang tiga isian. Kontrak meminta nama dan harga yang dapat diketik, serta pemilih satuan yang dapat diklik. |
| `saleWithoutItemsSavesTypedNameWithoutCreatingAProduct` / Aku jual tanpa barang | PK-2.4, membutuhkan PK-1.3: formulir nota lama meminta produk; belum dapat menyimpan nama bebas. |
| `recipeWithoutMaterialsOpensIngredientOrCostForm` / kalkulator resep tanpa bahan | PK-3.3: layar kalkulator mandiri dan sheet bahan/biaya belum ada. |
| `feasibilityWithoutItemsOpensSimpleItemForm` / kelayakan tanpa barang | PK-3.5, membutuhkan PK-3.4/K-14: rencana lama belum mempunyai aksi ke formulir barang kelayakan sederhana. |
| `emptyBuyersOpensBuyerForm` / pembeli kosong | PK-2.6 + PK-2.2: Kontak lama belum memenuhi aksi `Tambah pembeli` bertag `empty_action`. |
| `emptyReportsOpensSaleForm` / laporan kosong | PK-2.2 + PK-2.4: CTA laporan lama mengarah ke Catatan, belum langsung membuka Aku jual. |

**verified:** penelusuran `MankeuApp`, `HomeScreen`, `EntryScreen`, `ProductsScreen`, dan `ReportsScreen` memperlihatkan pintu lama; pencarian sumber tidak menemukan tag produksi `empty_action`. Diagnostik delapan metode mengonfirmasi kontrak baru belum dipenuhi. Kegagalan pertama menghentikan metode: penegasan formulir dan simpan sesudah titik itu belum dapat diverifikasi sampai paket pemilik kemampuan diterapkan.

**inferred:** PK-2.6 bersama kosakata PK-2.2 adalah lokasi terdekat untuk kontrak pembeli kosong; Lampiran B belum menyebut perbaikan `ContactsScreen` secara khusus. Penanggung jawab paket berikutnya perlu menjaga cakupan ini. Keputusan K yang masih kosong tetap merupakan penghalang; fixture masa depan harus mengikuti pilihan jenis usaha yang nanti disahkan, tanpa mengubah keputusan itu dalam PK-0.8.

**unknown:** ketentuan yang meminta seluruh `NoDeadEndTest` aktif di PK-2.11 bertabrakan dengan daftar prasyaratnya yang hanya PK-2.3–PK-2.7. Delapan tujuan Lampiran C juga membutuhkan editor PK-2.8 serta kalkulator/kelayakan PK-3.3 dan PK-3.5. Paket ini mempertahankan ketiga kontrak tersebut beserta alasan paket aslinya. Pertanyaan penyelarasan (belum dipilih): A. Jadwalkan gerbang semua delapan tujuan setelah PK-2.8/PK-3.3/PK-3.5 dan lengkapi prasyaratnya. B. Bedakan gerbang Pintu A pada tahap 2 dari gerbang seluruh delapan tujuan sesudah tahap 3, dengan kriteria masing-masing tertulis. PK-0.8 tidak memilih jawabannya atau mengubah syarat penerimaan. Pemilik/agen induk perlu menyelaraskan dependensi dan letak gerbang.

## Pemulihan data dan batas klaim

**verified:** arsip asli di direktori TEMP privat sudah tersedia sebelum runner pertama dan SHA-256 cocok dengan `9de082faddebf467370189b829a4463cc08a4e4e9c98e21bdec20d101768150f`. Sesudah instrumentasi, perintah berikut memasang kembali APK debug terbaru dan menjalankan helper pemulihan yang telah diperiksa:

```powershell
& E:/Android/Sdk/platform-tools/adb.exe install -r app/build/outputs/apk/debug/app-debug.apk
python C:/Users/super/AppData/Local/Temp/mankeu-spacing-audit/restore.py
```

Hasil: instalasi `Success`; helper menghasilkan `RESTORED VERIFIED 4 file SHA256 values equal`. Helper membandingkan seluruh himpunan nama berkas beserta SHA-256 di `databases`, `files`, `shared_prefs`, dan `no_backup`, sehingga tidak ada berkas tambahan dalam lingkup itu. Aplikasi ditinggalkan dalam keadaan force-stop agar SQLite tidak mengubah berkas setelah pemeriksaan. Arsip, isi data asli, label usaha asli, dan keluaran privat tidak dimasukkan ke repositori.

**reported:** agen induk menyatakan data asli tidak berubah sejak pemulihan PK-0.5. PK-0.8 memeriksa ulang fingerprint arsip dan kesamaan empat berkas hasil pemulihan secara langsung.

**unverified:** kemudahan pakai oleh manusia, durasi perjalanan, anggaran ketukan perjalanan penjualan pertama, perangkat fisik, formulir masa depan setelah titik kegagalan, serta delapan perjalanan yang diabaikan. Tidak ada klaim `user-validated`, migrasi, atau kesiapan rilis dari hasil ini.
