# Lampiran C: rencana uji (2026-11-12)

Induk: `2026-11-12-rencana-induk-mvp.md`. Lima lapis uji, dari yang paling murah sampai yang hanya bisa dilakukan
manusia. Lapis 1, 2, dan 4.1 sampai 4.3 otomatis; lapis 3, 4.4, dan 5 dilakukan pemilik dengan bahan dari agen.

Batas jujur: lapis otomatis mengukur **pengganti** keterpakaian (jumlah ketukan, kata, jalan buntu). Hanya lapis 3
yang mengukur keterpakaian sungguhan. Lulus lapis 1 tidak boleh dilaporkan sebagai "mudah dipakai".

## 1. Uji otomatis keterpakaian

### 1.1 Kata terlarang (`ScreenLanguageTest`, PK-0.6)

Diperiksa: semua string di `res/values/strings.xml` dan `res/values-en/strings.xml`, tanpa beda huruf besar kecil,
sebagai kata utuh.

| Bahasa | Kata |
|---|---|
| Indonesia | HPP, batch, unit usaha, buku, rasio, persediaan, piutang, kategori pembukuan, realisasi, proyeksi, nominal, opname, prive, operasional, konsinyasi, arus kas, aset, variabel, workbook, impor, ekspor, debit, kredit, jurnal, R/C, B/C, BEP, RAB, anggaran |
| Inggris | COGS, batch, business unit, ledger, ratio, inventory, receivable, payable, projection, realization, drawing, consignment, cash flow, asset, variable cost, workbook, debit, credit, journal, break-even |

Pengecualian:

1. String yang hanya dipakai PDF, Laporan formal, dan "Rincian analisis". PK-0.6 mendaftar awalan namanya dari
   `strings.xml` yang ada dan menuliskannya sebagai konstanta di tes, dengan komentar asalnya.
2. "HPP" di dalam kurung sesudah kata "modal", misalnya "modal barang (HPP)".
3. "buku" sebagai bagian "buku resep" tidak dikecualikan; hindari saja.
4. "impor" dan "ekspor": di layar dipakai "ambil dari Excel" dan "simpan ke Excel".

Daftar dasar `kata_terlarang_dasar.txt` hanya boleh menyusut dan harus kosong di PK-5.1.

### 1.2 Tombol mati menyebut alasan (PK-0.7)

Dijamin oleh bentuk kode, bukan oleh tes per layar: `FullPageForm` dan `MankeuSheet` tidak punya parameter
`enabled` tanpa alasan. Satu tes komponen menjaga tampilannya. Tiap tes layar baru (PK-2.x, PK-3.x) wajib punya
satu kasus yang menegaskan teks alasan dari Lampiran A.

Pemeriksaan tambahan di tiap review: `grep -rn "enabled = " presentation/screens` dan tiap hasil dijelaskan.

### 1.3 Tak ada jalan buntu (`NoDeadEndTest`, PK-0.8)

Pada basis data kosong (hanya usaha), tiap tujuan harus punya node `empty_action` yang bisa diklik dan membuka form
yang menyelesaikan kekosongan itu:

| Tujuan | Aksi yang diharapkan |
|---|---|
| Beranda | "Aku jual", "Aku beli" |
| Catatan | "Aku jual", "Aku beli" |
| Tab barang | "Tambah" membuka editor barang 3 isian |
| "Aku jual" tanpa barang | Bisa disimpan dengan nama ketikan (tidak ada pesan "buat produk dulu") |
| Kalkulator resep tanpa bahan | "Tambah bahan atau biaya" |
| Kelayakan tanpa barang | "+ Tambah barang" |
| Pembeli kosong | "Tambah pembeli" |
| Laporan kosong | "Aku jual" |

Pemeriksaan teks: string yang memuat pola "dulu di tab", "terlebih dahulu di", "buka menu … untuk" ditinjau di
tiap review; teks petunjuk tanpa tombol dianggap jalan buntu.

### 1.4 Jumlah ketukan dan perjalanan pertama (`FirstSaleJourneyTest`, PK-2.11)

Ketukan = `performClick`; isian = `performTextInput`. Dihitung `TapCounter`.

| Tugas | Mulai dari | Batas ketukan | Batas isian diketik |
|---|---|---|---|
| Penjualan pertama | instal bersih | 8 (termasuk isian) | 3 (nama usaha, nama barang, harga) |
| Jual lagi barang yang pernah | Beranda | 3 | 0 |
| Jual belum bayar ke pembeli lama | Beranda | 5 | 0 |
| Aku beli | Beranda | 4 | 2 |
| Terima bayar penuh | Beranda | 3 | 0 |
| Lihat untung bulan ini | Beranda | 0 | 0 |
| Lihat siapa belum bayar | Beranda | 1 | 0 |
| Resep 3 bahan sampai modal per satuan | Beranda | 20 | 11 |

**Pengganti "di bawah 1 menit":** anggaran di atas. Dengan 5 detik per ketukan dan 8 detik per isian, penjualan
pertama memakan kira-kira 50 detik. Angka 5 dan 8 detik adalah anggapan penyusun [belum]; uji pengguna putaran 1
mengukur yang sebenarnya, dan anggaran ini disesuaikan dari hasilnya.

### 1.5 Jank (PK-0.4, PK-5.3)

`dumpsys gfxinfo com.mankeu` pada: buka "Aku jual", ketik harga, simpan, gulir Catatan 200 baris, buka kalkulator.
Batas di emulator: di bawah 15 % janky tiap aksi. Batas ini pilihan penyusun; putusan sebenarnya di HP murah (§5).

## 2. Skenario persona (`PersonaScenarioTest`, PK-1.7)

Tingkat repository, basis data di memori. Satu fungsi tes per persona. Semua angka rupiah.

| # | Persona | Cara hitung | Langkah | Yang ditegaskan |
|---|---|---|---|---|
| P1 | Bu Sri, keripik | Modal barang | Data golden `GoldenData.kt` | Modal Rp6.000 per bungkus, Rp240.000 per dus; Oktober: untung −310.000, modal terjual 4.440.000 |
| P2 | Pak Joko, traktor | Sederhana | Layanan "Bajak sawah" per hektar Rp1.200.000. Jual 2 hektar ke Pak Hadi, belum bayar. Beli solar 300.000 dan upah 500.000 (keperluan lain) | Belum bayar 2.400.000 atas nama Pak Hadi; untung 1.600.000; uang di tangan −800.000; tidak ada gerakan stok; tidak ada peringatan menipis |
| P3 | Bu Tini, kelontong | Modal barang | Beras, barang dagangan, kg, "karung" isi 25. Beli 2 karung 650.000. Jual 3 kg @ 15.000 lunas | Sisa 47 kg; modal 13.000 per kg; modal baris 39.000; untung 6.000; selisih belanja 611.000; uang di tangan −605.000. Dengan cara Sederhana pada data yang sama: untung −605.000 |
| P4 | Mbak Rina, warung | Sederhana | Baris bebas "Jualan hari ini" 850.000 lunas. Beli bahan 500.000. Beli gas 50.000 | Untung 300.000; uang di tangan 300.000; tidak ada barang yang tercipta |
| P5 | Pak Dedi, cabai | Sederhana | Bulan 1 beli bibit dan pupuk 2.000.000. Bulan 3 jual 100 kg @ 30.000 | Bulan 1 untung −2.000.000; bulan 3 untung 3.000.000. **Untung per musim: `@Ignore("K-12")`** |
| P6 | Ibu Wati, penjahit | Sederhana | Baris bebas "Jahit seragam 10 pcs" 1.500.000, dibayar 750.000, atas nama Bu Ani. Beli kain 600.000. Lalu terima 750.000 | Sesudah nota: belum bayar 750.000. Sesudah terima: lunas. Untung bulan itu 900.000. **Status pesanan: `@Ignore("K-3")`** |
| P7 | Pak Anton, bengkel | Modal barang | Oli, barang dagangan, botol. Beli 10 botol 400.000. Satu nota: Oli 1 @ 55.000 dan baris bebas "Jasa ganti oli" 20.000, lunas | Total 75.000; modal 40.000; untung 35.000; sisa oli 9; tidak ada penjualan "tanpa modal" yang ditandai |
| P8 | Mas Bayu, toko online | Modal barang | Barang dagangan dijual 10 @ 50.000 ke pembeli "Marketplace", belum bayar | Belum bayar 500.000. **Potongan admin saat pencairan: `@Ignore("D11, sesudah MVP")`** |
| P9 | Bu Lina, laundry | Sederhana | Layanan "Cuci kiloan" per kg 7.000. Jual 5 kg lunas. Beli deterjen 50.000 | Total 35.000; untung −15.000; tanpa stok |
| P10 | Pak Rudi, rental | Sederhana | Layanan "Sewa tenda" per hari 300.000. Jual 2 hari lunas | Total 600.000. **Jaminan yang dikembalikan: `@Ignore("K-11")`** |

Tambahan lintas persona:

- Tiap skenario diakhiri: `MonthlySummary` sama dengan yang dibaca Beranda dan ekspor (`OneNumberTest` pola).
- Sesudah PK-4.4, tiap skenario diekspor dan diimpor ke usaha kosong, dan semua penegasan diulang.

## 3. Uji pengguna awam sungguhan (PK-2.14 dan PK-5.5)

### 3.1 Peserta dan aturan

Enam orang per putaran: pembuat makanan rumahan, pemilik warung atau kelontong, penyedia jasa atau sewa, petani atau
peternak, penjahit atau bengkel, dan satu orang berusia 50 tahun ke atas yang jarang memakai aplikasi. Putaran 2
sebaiknya memakai orang yang berbeda, supaya yang diukur adalah app, bukan ingatan.

Aturan sesi: 30 menit, di HP peserta sendiri bila bisa (APK debug), direkam layar dan suara dengan izin. Pemilik
tidak membantu, tidak menunjuk, tidak menjelaskan istilah. Bila peserta macet 60 detik, catat "macet" lalu boleh
beri satu petunjuk dan catat petunjuknya. Data yang dimasukkan peserta dihapus sesudah sesi; nama peserta tidak
masuk repo (pakai kode A1 sampai A6).

### 3.2 Skrip (dibacakan pemilik)

> "Terima kasih mau mencoba. Yang diuji aplikasinya, bukan Bapak/Ibu. Kalau bingung, itu salah aplikasinya dan
> justru itu yang ingin saya tahu. Tolong ucapkan apa yang Bapak/Ibu pikirkan sambil mencoba."

| # | Tugas (dibacakan persis) | Yang diukur |
|---|---|---|
| T1 | "Ini aplikasi catatan usaha. Silakan buka dan mulai." | Detik sampai Beranda; kartu jenis yang dipilih; ragu di mana |
| T2 | "Tadi pagi ada yang beli dagangan Bapak/Ibu. Tolong catat." | **Detik dari awal T2 sampai tersimpan**; macet atau tidak |
| T3 | "Kemarin Bapak/Ibu belanja keperluan usaha. Tolong catat." | Detik; pilihan "ini untuk apa" yang diambil |
| T4 | "Ada langganan yang ambil barang tapi belum bayar. Tolong catat." | Detik; apakah menemukan "Belum" dan menulis nama |
| T5 | "Bulan ini untungnya berapa?" | Jawaban yang diucapkan; cocok dengan layar atau tidak |
| T6 | "Siapa saja yang belum bayar, dan berapa?" | Sama |
| T7 | "Orang tadi sudah bayar. Tolong catat." | Detik |
| T8 (pembuat makanan saja) | "Coba hitung modal satu resep yang biasa Bapak/Ibu buat." | Selesai atau tidak; bahan yang sulit dimasukkan |
| T9 | "Kalau HP ini hilang, catatannya bagaimana?" | Apakah menemukan cadangan |

Pertanyaan penutup: "Kata mana yang tidak dimengerti?", "Apa yang ingin dicatat tapi tidak bisa?", "Mau pakai ini
besok? Kenapa?"

### 3.3 Lembar catatan (satu per peserta)

```
Kode peserta: ____   Jenis usaha: ____________   Umur kira-kira: ____   HP: ____________   Tanggal: ______

Tugas | Detik | Selesai sendiri? (Y / dengan petunjuk / T) | Macet di layar apa | Kata yang ditanyakan
T1    |       |                                            |                    |
…
T9    |       |                                            |                    |

T5 jawaban peserta: Rp________   Angka di layar: Rp________   Cocok? Y / T
T6 jawaban peserta: ____________________________________   Cocok? Y / T

Kata tidak dimengerti: ________________________________________________
Ingin dicatat tapi tidak bisa: _________________________________________
Mau pakai besok? Y / T   Alasan: _______________________________________
Catatan pengamat: _____________________________________________________
```

### 3.4 Kriteria

| Putaran | Lulus bila | Bila tidak |
|---|---|---|
| 1 (sesudah Tahap 2) | Tidak ada syarat lulus; tujuannya menemukan titik macet. Tiap titik macet yang dialami 2 orang atau lebih menjadi paket perbaikan | |
| 2 (Tahap 5), syarat MVP V-5 | 5 dari 6 menyelesaikan T2 di bawah 120 detik tanpa petunjuk, **dan** 5 dari 6 menjawab T5 dan T6 dengan benar | Desain dirombak di titik macet, lalu putaran 3 dengan minimal 3 orang baru. Tidak dirilis sebagai MVP |

Hasil ditulis agen ke `docs/audit/2026-11-XX_UJI_PENGGUNA_n.md`: tabel per peserta, daftar titik macet dengan jumlah
orang, daftar kata, dan usulan perbaikan. Rekaman tidak masuk repo.

## 4. Uji kecocokan hitungan

### 4.1 Golden kalkulator (`RecipeCalculatorTest`, PK-3.1)

Resep dasar keripik: singkong 50 kg @ 30.000, minyak 20 liter @ 25.000, bumbu dan kemasan 300.000, gas 100.000; hasil
400 bungkus; dus isi 40.

| Kasus | Hasil |
|---|---|
| Modal sekali buat | 2.400.000 |
| Modal per bungkus, per dus | 6.000; 240.000 |
| Susut 20 bungkus | 6.316 per bungkus (2.400.000 ÷ 380 = 6.315,79); 252.632 per dus |
| Susut 400 atau lebih | ditolak |
| Untung 30 % dari modal 6.000 | harga 7.800; dus 312.000 |
| Untung 30 % dari modal 6.316 | 8.210,8 dibulatkan ke atas ke 8.300 |
| Minyak naik 10 % | modal sekali buat 2.450.000; 6.125 per bungkus; 245.000 per dus |
| Semua naik 10 % | 2.640.000; 6.600 per bungkus |
| Untung per bungkus pada harga 7.800 sesudah minyak naik | dari 1.800 menjadi 1.675 |

### 4.2 Golden konversi dan harga per kemasan (`UnitConversionTest`, PK-3.1, PK-3.2)

| Kasus | Hasil |
|---|---|
| Tepung 12.000 untuk 1 kg, dipakai 250 gram | 3.000 |
| Tepung 12.000 untuk 1 kg, dipakai 1,5 ons | 1.800 |
| Gula 13.500 untuk 1 kg, dipakai 500 gram | 6.750 |
| Minyak 25.000 untuk 1 liter, dipakai 500 ml | 12.500 |
| Telur 28.000 untuk 30 butir, dipakai 4 butir | 3.733 (28.000 × 4 ÷ 30 = 3.733,33) |
| Garam 5.000 untuk 500 gram, dipakai 10 gram | 100 |
| Dipakai "sendok", harga per "kg" | tidak bisa dikonversi; app bertanya |
| "Kg", "KG", "kg " | dianggap sama |

### 4.3 Golden kelayakan (`FeasibilityTest`, PK-3.4)

| Kasus | Hasil |
|---|---|
| 50 dus per bulan, harga 300.000, modal 240.000, biaya rutin 1.200.000, modal awal 3.600.000 | Untung 1.800.000; tidak rugi mulai 20 dus; modal kembali 2 bulan; LAYAK |
| Sama, biaya rutin 0 | BELUM BISA DINILAI |
| Sama, 10 dus per bulan | Untung −600.000; BELUM LAYAK; tidak rugi mulai 20 dus |
| Untung 1.700.000, modal awal 3.600.000 | Modal kembali 3 bulan (dibulatkan ke atas) |
| Dua barang: 50 dus (selisih 60.000) dan 100 pcs (selisih 2.000), biaya rutin 1.200.000 | Tidak rugi mulai 19 dus dan 38 pcs (18,75 dan 37,5 dibulatkan ke atas) |
| Harga di bawah modal | Barang itu ditandai "dijual di bawah modal" |
| Modal awal 0 | Kalimat modal kembali tidak ada |

### 4.4 Uji praktisi: satu resep nyata (PK-3.8)

Peserta: satu pembuat makanan yang biasa menghitung sendiri (boleh salah satu peserta §3).

1. Sebelum membuka app, minta ia menuliskan hitungannya sendiri di kertas: bahan, jumlah, harga beli, hasil, modal
   per satuan, harga jual. Foto kertasnya (tanpa nama).
2. Ia memasukkan resep yang sama ke kalkulator, tanpa bantuan.
3. Catat tiap baris bahan: angka kertas, angka app, selisih.

Lembar:

```
Resep: ____________   Hasil: ____ ____   Tanggal: ______

Bahan | Dipakai | Harga beli (Rp untuk berapa) | Biaya di kertas | Biaya di app | Selisih
…
Modal sekali buat:  kertas Rp______  app Rp______
Modal per satuan:   kertas Rp______  app Rp______
Bahan yang sulit dimasukkan: _______________
Hal yang ia hitung tetapi app tidak (listrik, tenaga, penyusutan alat): _______________
```

Lulus bila modal per satuan di app sama dengan di kertas, atau selisihnya bisa dijelaskan sebagai kesalahan hitung
di kertas yang ia akui. Selisih pembulatan app harus Rp0 per baris bila K-4 = A. Selisih lain menjadi paket
perbaikan sebelum Tahap 5.

## 5. Uji di HP nyata murah (PK-5.4)

HP sasaran: Android 8 sampai 11, RAM 2 sampai 3 GB, layar sekitar 5,5 inci atau lebih kecil, papan ketik bawaan
pabrik. Agen menulis `docs/audit/CHECKLIST_HP_NYATA.md` dari daftar ini; pemilik mencentang.

| # | Cek | Lulus bila |
|---|---|---|
| H1 | Pasang dan buka pertama kali | Layar pertama tampil di bawah 5 detik; tidak ada teks terpotong |
| H2 | Perjalanan penjualan pertama | Tombol utama tidak tertutup papan ketik; semua target mudah disentuh ibu jari |
| H3 | Ketik harga dan jumlah dengan papan ketik bawaan | Titik ribuan muncul; koma desimal diterima |
| H4 | Back di tiap form | Menutup papan ketik, lalu dialog keluar; tidak ada isian hilang diam-diam |
| H5 | 200 catatan lalu gulir Catatan | Tidak tersendat yang mengganggu (penilaian pemilik, catat ya atau tidak) |
| H6 | Ukuran huruf sistem "besar" | Tidak ada tombol atau angka terpotong |
| H7 | Mode gelap | Semua teks terbaca |
| H8 | Kirim nota dan pengingat ke WhatsApp | Terbuka di WhatsApp dengan teks benar (R17) |
| H9 | Masuk Google, cadangan ke Drive, pulih di HP kedua | Angka Beranda sama di HP kedua (menutup T0 di lapangan) |
| H10 | Sinkron dua HP | Catatan dari HP A muncul di HP B |
| H11 | Simpan cadangan ke berkas, hapus app, pasang lagi, pulih | Data kembali |
| H12 | Unduh template, isi 3 baris di aplikasi lembar kerja HP, ambil | Terambil; atau pesan galat menunjuk sel yang salah |
| H13 | App dibuka lagi sesudah sehari di latar | Tidak mulai dari layar kosong; tidak crash |

H9 dan H10 belum pernah diverifikasi siapa pun [belum]. Kegagalannya menghalangi rilis.

## 6. Matriks verifikasi (akhir tiap paket yang mengubah kode)

| Cek | Cara | Harus |
|---|---|---|
| Unit | `:app:testDebugUnitTest` | 0 gagal (dasar 148 di `b217794` [audit]) |
| Lint | `:app:lintDebug` | 0 error; jumlah peringatan dicatat, tidak naik dari 89 |
| Build | `:app:assembleDebug`; sebelum merge juga `:app:assembleRelease` | hijau |
| Instrumented | `:app:connectedDebugAndroidTest` | 0 gagal; yang dilewati hanya Help/ModuleScreenshotTest dan `@Ignore` bernomor |
| Satu angka | `OneNumberTest` | Beranda = Laporan = ekspor, untuk dua cara hitung |
| Golden lama | `SalesFlowTest`, `MonthlySummaryTest`, `ProductCostingTest` | Oktober keripik tidak berubah |
| Persona | `PersonaScenarioTest` | hijau |
| Migrasi | `Migration12To13Test`, `Migration13To14Test`, `BackupUpgradeTest` | hijau; `createSql` = `14.json` |
| DEC-012 | `TemplateImportTest`, `ImportCommitTest` | round trip identik |
| Manifest | `ManifestTest` | INTERNET ada |
| Bahasa | `ScreenLanguageTest`, `VocabularyTest`, `GuidesTest` | hijau |
| Keterpakaian | `NoDeadEndTest`, `FirstSaleJourneyTest` | hijau |
| Encoding | cari `â€`, `Â§`, `Ã—` di `*.md`, `*.xml`, `*.kt` | kosong |
| Bukti nyata | butir "Bukti nyata" paket | tertulis di laporan dengan screenshot atau angka |

Suite penuh dijalankan sekali di akhir paket. Selama bekerja, jalankan tes terfokus:
`./gradlew.bat :app:testDebugUnitTest --tests "com.mankeu.domain.ProductCostingTest"` atau
`:app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.mankeu.QuickSaleTest"`.
