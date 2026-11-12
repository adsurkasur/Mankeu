# Laporan PK-1.1: jenis barang dan modal per satuan

Tanggal: 2026-11-12. Agen: Codex, pelaksana PK-1.1. Dasar kerja: branch `fase2-nota-piutang`, HEAD awal `b4ce447`.

Claim: **verified / unit-verified**. Domain PK-1.1 memenuhi brief `.superpowers/sdd/2026-11-12-rencana-induk-mvp/PK-1.1-brief.md` dan rencana induk §3.1 serta §3.5. Klaim ini terbatas pada fungsi domain dan kompilasi aplikasi debug.

Business evidence: **verified** dari brief dan rencana induk. Modal per satuan dasar mengikuti resep, lalu harga beli yang diberikan pemanggil untuk TRADED, lalu modal ketikan, lalu belum diketahui. Susut mengurangi hasil batch sebelum pembagian.

Changed behavior:

- `app/src/main/java/com/mankeu/domain/ProductCosting.kt`: menambah `ItemKind` MADE/TRADED/SERVICE; parameter akhir `ProductSpec` dengan bawaan MADE, susut nol, dan modal manual null; `UnitCostSource`; `UnitCostResult`; serta `ProductCosting.unitCost`.
- Resep selalu didahulukan. Resep yang belum lengkap tetap mengembalikan biaya parsial dengan `known = false` dan sumber RECIPE; harga beli atau modal manual tidak menyembunyikan bahan yang belum berharga.
- `cost()` membagi biaya batch dengan hasil bersih BigDecimal. HPP tiap satuan jual dihitung langsung dari biaya batch × isi ÷ hasil bersih, dengan HALF_UP di akhir pembagian.
- Validasi menolak susut negatif atau hasil bersih nol/negatif dengan `IllegalArgumentException` dan pesan. `unitCost` menolak modal manual/harga beli negatif. Null berarti belum tersedia; nol yang diberikan secara eksplisit merupakan biaya diketahui. Aturan harga bahan nol yang membuat resep tidak lengkap tetap dipertahankan.
- `app/src/test/java/com/mankeu/domain/ProductCostingTest.kt`: menambah 15 tes. Sepuluh metode tes lama dipertahankan tanpa perubahan, dibuktikan dengan perbandingan isi terhadap HEAD awal setelah normalisasi akhir baris.

Tests and exact result: **verified**.

Semua perintah Gradle dijalankan melalui PowerShell dengan penyesuaian PATH:

```powershell
$env:Path = ($env:Path -split ';' | Where-Object { $_ -notmatch '"' }) -join ';'
./gradlew.bat testDebugUnitTest --tests com.mankeu.domain.ProductCostingTest
./gradlew.bat testDebugUnitTest assembleDebug
```

1. RED awal: tes baru gagal dikompilasi karena API belum tersedia. Ini merupakan compile-red, bukan kegagalan perilaku.
2. Setelah hanya deklarasi API dan stub `unitCost` yang mengembalikan UNKNOWN ditambahkan, RED perilaku menunjukkan 21 tes, 9 gagal. Algoritme belum ditulis.
3. Empat tes batas tambahan dijalankan terhadap stub yang sama: 25 tes, 13 gagal. Kegagalan berasal dari cabang biaya, hasil bersih, dan validasi yang belum diterapkan.
4. GREEN terfokus setelah implementasi: 25 tes, 0 gagal, 0 error, 0 dilewati.
5. Verifikasi akhir `testDebugUnitTest assembleDebug`: BUILD SUCCESSFUL, exit 0. XML hasil JVM memuat 28 suite, 179 tes, 0 gagal, 0 error, 0 dilewati. Jumlah tersebut adalah baseline 164 ditambah 15 tes PK-1.1.
6. `git diff --check`: exit 0.

Acceptance evidence: **verified** melalui tes nyata tanpa mock. Golden resep tetap Rp6.000 per bungkus dan Rp240.000 per dus. Susut 20 dari 400 menghasilkan Rp6.316 dan Rp252.632, bukan perkalian biaya dasar yang sudah dibulatkan. TRADED tanpa resep memakai Rp13.000 dari parameter harga beli. Modal manual Rp5.000 dipakai bila sumber sebelumnya tidak ada. Tanpa sumber menghasilkan Rp0 dengan `known = false`. Susut 400 dan 401 ditolak dengan pesan. Tes tambahan mencakup urutan sumber pada ketiga jenis barang, resep parsial dan seluruhnya belum berharga, hasil pecahan, hasil bersih di bawah satu satuan, biaya nol eksplisit, input negatif, dan presisi Rp9.007.199.254.740.993.

Manual/device evidence: **unknown / tidak diuji**. Tidak ada interaksi emulator, instalasi APK, ataupun perubahan data perangkat. Artefak debug tersedia di `app/build/outputs/apk/debug/app-debug.apk`.

Data migration/rollback impact: **verified dari diff**. Perubahan hanya pada dua berkas Kotlin di atas dan laporan ini. Tidak ada perubahan skema, repository, persistensi, impor/ekspor, sinkronisasi, atau UI. Pemanggil lama tetap dapat memakai konstruktor `ProductSpec` dengan parameter sebelumnya karena parameter tambahan berada di akhir dan mempunyai bawaan.

Privacy/security impact: **verified dari diff**. Tidak ada dependensi atau akses jaringan baru; fixture sintetis digunakan untuk tes. Tidak ada data keuangan nyata, cadangan, atau kredensial yang ditambahkan.

Not verified: jalur pengguna lengkap, migrasi, sinkronisasi perangkat, build rilis bertanda tangan, dan penerbitan. `assembleDebug` membuktikan integrasi kompilasi, bukan alur UI.

Known limitations: harga beli terakhir diterima sebagai parameter opsional; pencarian transaksi pembelian, kebijakan K-8, dan penyimpanan field baru berada di luar PK-1.1. API lama `cost()` tetap menghitung resep saja; fallback tersedia melalui `unitCost`. Peringatan deprecation Google Sign-In yang muncul pada kompilasi RED berada pada kode yang tidak diubah. Delapan kasus masa depan NoDeadEnd yang diabaikan tidak diubah dan bukan bagian dari 179 tes JVM ini.

Handoff: peninjau induk menangani pembaruan log bersama, ADR/changelog/panduan domain/rencana induk, commit atomik, dan push. Pelaksana ini tidak membuat commit atau mengubah log bersama.
