# Audit Pra-Rilis Produksi — 22 Oktober 2026

Permintaan pemilik: audit seluruh proyek sebelum rilis produksi publik di Play; perbarui semua dokumen;
pastikan logika bisnis tepat dengan simulasi UMKM nyata dari skala mikro, kecil, dan menengah, oleh
pengguna yang paham maupun yang awam; pastikan aplikasi benar-benar intuitif; audit performa dan kode
(ponytail-audit).

Status pernyataan mengikuti `AGENTS.md`: **terverifikasi** (ada bukti yang bisa diulang),
**dikonfirmasi pemilik**, **disimpulkan**, **diusulkan**, **belum diketahui**.

Perangkat uji: emulator Android API 34 (`emulator-5554`, AVD Medium_Phone), build debug dan build
release yang diperkecil R8. Semua data uji fiktif.

## 1. Keputusan pemilik (grill)

| Topik | Keputusan |
|---|---|
| Arti "full release" | Rilis produksi publik di Play (sekarang hanya jalur internal testing) |
| Versi | 0.1.0 (versionCode 7) |
| Sync multi-HP (DEC-013, belum tersambung) | Dijadikan fitur rilis: satu akun Google untuk semua HP; kata sandi sync sekali per HP, disimpan terkunci Keystore; otomatis saat dibuka dan setelah menyimpan + tombol; bentrok digabung otomatis lalu diberitahukan |
| Batas usaha menengah (piutang, utang) | Dicatat sebagai usulan fitur, diputuskan setelah audit |
| Penanganan temuan | Yang jelas langsung diperbaiki dan dites; keputusan produk lewat grill |
| UX hasil uji awam | Unit pertama otomatis; satu sapaan panduan saja; keterangan boleh kosong; tombol buku disembunyikan sampai ada unit, "Proyeksi" → "Rencana"; aplikasi tetap "kamu"; keterangan saat untung/rugi kas minus |

## 2. Simulasi skala UMKM (logika bisnis)

Generator: `docs/evidence/2026-10-22_simulasi_skala_umkm/make_sims.py` (data setahun, seed tetap;
angka pembanding dihitung dari baris, bukan dari kode aplikasi). Test: `ScaleSimulationTest`
(instrumented) mengimpor setiap berkas lewat parser XLSX asli dan mencocokkan hasilnya.

| Simulasi | Skala (PP 7/2021) | Baris | Penjualan setahun | Laba kas | Kas akhir |
|---|---|---:|---:|---:|---:|
| M1 Warung Bu Ani | Mikro | 547 | Rp258.284.000 | Rp45.453.000 | Rp28.185.000 |
| K1 Bengkel Jaya Motor | Kecil | 2.119 | Rp2.717.174.000 | Rp670.593.000 | Rp572.593.000 |
| N1 Penggilingan — Pabrik Klaten | Menengah | 8.975 | Rp23.615.285.000 | Rp2.708.187.000 | Rp3.038.187.000 |
| N1 — Gudang Solo | Menengah | 5.513 | Rp14.102.045.000 | Rp1.976.848.000 | Rp2.876.848.000 |
| N1 — Gudang Yogya (merugi) | Menengah | 4.252 | Rp10.450.115.000 | −Rp273.202.000 | Rp401.798.000 |

**Terverifikasi:** untuk kelima berkas, jumlah baris, kas akhir, pendapatan operasional, biaya
operasional, dan laba kas di aplikasi sama persis dengan pembanding.

## 3. Uji sebagai pengguna

### 3.1 Bu Ani — warung mikro, awam teknologi

Dari instalasi kosong, tujuan: mencatat jualan hari ini dan kulakan, lalu melihat sisa uang.

| # | Temuan | Dampak | Penanganan |
|---|---|---|---|
| U1 | "Tambah Usaha" di Beranda (juga kartu kosong Stok, Rencana, banner Catatan) membuka Berkas dan Data, yang diawali "Impor workbook / Pilih file .xlsx" | Pemula mengira harus punya Excel; formulir usaha tidak pernah muncul | **Diperbaiki**: semua kartu itu membuka lembar Usaha dan Unit |
| U2 | Usaha satu tempat wajib membuat "usaha" lalu "unit usaha"; tiga nama untuk hal yang sama | Dua formulir, dua lapis lembar, istilah asing | **Diperbaiki** (DEC-022): satu formulir, unit "Utama" otomatis, lembar menutup sendiri |
| U3 | Lima kartu "Lihat panduan?" sebelum catatan pertama; kartu menutupi tombol + (ketukan memulai tur) dan pesan "tersimpan" | Mengganggu, membuat salah ketuk | **Diperbaiki** (DEC-022): satu sapaan di Beranda yang menunjuk tombol ? |
| U4 | **Catat Pemasukan memilih "Pendapatan Lain"** (urutan abjad) | Semua jualan pemula tercatat sebagai pendapatan lain; BEP bulan ini dan unit terjual salah | **Diperbaiki**: kategori terakhir dipakai, lalu urutan daftar awal |
| U5 | Keterangan wajib di setiap catatan | Catatan harian berulang ditolak | **Diperbaiki** (DEC-022): boleh kosong, diisi nama kategori |
| U6 | Kas minus merah setelah jualan + kulakan karena uang awal tidak dicatat | Pemula mengira rugi atau aplikasi salah | **Diperbaiki**: kartu "Uang kas minus?" + "Catat uang awal" (Setoran Modal terpilih); hilang begitu modal tercatat |
| U7 | Untung/rugi kas minus di hari kulakan | Wajar untuk pembukuan kas, tetapi menakutkan | **Diperbaiki** (DEC-022): satu kalimat penjelasan saat minus |
| U8 | "Laba Bersih" di Laporan untuk angka yang di Beranda bernama "Untung / Rugi kas" | Dua nama, dan "laba bersih" keliru (belum dikurangi penyusutan) | **Diperbaiki**: disamakan |
| U9 | Kolom cari menyebut "nama orang" | Sisa fitur utang-piutang yang sudah dihapus | **Diperbaiki** |
| U10 | Proyeksi/Realisasi tampil sebelum ada usaha | Dua istilah asing di layar pertama | **Diperbaiki** (DEC-022): disembunyikan sampai ada unit; "Rencana" |
| U11 | Persen komposisi biaya "100.0%" | Format desimal Inggris | **Diperbaiki**: locale aplikasi |
| — | Dugaan "Kembali tidak menutup lembar" | — | **Bukan bug aplikasi**: emulator hanya punya input suara Google, yang menyadap tombol Kembali; setelah dimatikan, lembar tertutup normal |

### 3.2 Mbak Rina — admin penggilingan menengah, mahir

| # | Temuan | Penanganan |
|---|---|---|
| R1 | Tombol "Batal" saat impor tidak membatalkan: hasil baca tetap menimpa; saat menyimpan, data tetap masuk lalu tampil "Impor selesai" | **Diperbaiki**: baca benar-benar dibatalkan; menyimpan menampilkan "Menyimpan N baris…" tanpa Batal (satu transaksi) |
| R2 | Pratinjau tidak menyebut unit tujuan, padahal ia punya tiga unit | **Diperbaiki**: baris "Masuk ke: usaha · unit" |
| R3 | Impor lewat layar tidak bisa dicoba di emulator: pemilih file tidak merespons ketukan (juga untuk berkas lama) | Kendala alat uji; jalur impor dibuktikan oleh `ScaleSimulationTest` |

## 4. Performa

Diukur di emulator dengan `ScaleSimulationTest` (log `ScaleSimulation`).

| Langkah (Pabrik Klaten, 8.975 baris) | Sebelum | Sesudah |
|---|---:|---:|
| Ringkasan Beranda (`accountingSummary`) | 556 ms | 29 ms |
| Arus kas bulanan | 57 ms | 10 ms |
| Impor (baca + simpan), emulator hangat | — | ±10,6 s |

Akar masalah: Beranda memuat **semua** posting buku Realisasi ke memori pada setiap perubahan
database, dua kali, padahal yang dihitung hanya jumlah per (peran arus kas, akun, sisi). Kini
dijumlahkan oleh SQLite (`observePostingTotals`); arus kas bulanan juga dijumlahkan per bulan di SQL.
`sync_outbox` tumbuh tanpa batas selama sync tidak dipakai (±4 baris per transaksi) — ditangani di
bagian sync.

## 5. Kode (ponytail-audit)

Kode tergolong ramping. Temuan dan tindakan:

- `delete` `GetStockSummaryUseCase` tidak dipakai — **dihapus**.
- `delete` dependensi `navigation-compose` tidak dipakai — **dihapus** (beserta aturan ProGuard-nya).
- `delete` status tawaran tur `later/notNow/never/offerHidden` — **dihapus** bersama perubahan DEC-022.
- `yagni` `RecordStockMovementUseCase` pembungkus satu pemanggil — **dipertahankan**: punya unit test sendiri dengan DAO tiruan.

## 6. Kesiapan rilis

| Butir | Status |
|---|---|
| Versi | 0.1.0 (code 7) — DEC-023 |
| R8 + shrinkResources | Aktif; APK release 4,2 MB (APK debug 27 MB); build release diuji di emulator tanpa crash |
| Target SDK | 36 (compileSdk 36) |
| Penandatanganan | Lewat variabel lingkungan `KEYSTORE_*` (tidak berubah); kunci rilis di tangan pemilik |
| "Hapus semua data" | Kini juga mematikan sync dan menghapus kata sandi sync tersimpan |

## 7. Sinkronisasi antar-HP (DEC-024)

Sebelum audit, kerangka sync sudah ada tetapi tidak tersambung, dan triggernya mengisi `sync_outbox`
di setiap HP tanpa pernah dibaca. Keputusan pemilik: jadikan fitur rilis.

| Butir | Status |
|---|---|
| Satu akun Google untuk semua HP, folder `drive.appdata` | **Terverifikasi** di kode; belum diuji dengan akun nyata |
| Kata sandi sync sekali per HP, terkunci Android Keystore | **Terverifikasi** (`SyncPassphraseStoreTest`) |
| Kata sandi berbeda di HP kedua ditolak sebelum menulis apa pun | Kode ada (`SyncController.enable`); belum diuji dengan Drive nyata |
| Otomatis saat dibuka, 5 detik setelah perubahan, tombol "Sinkronkan sekarang" | Kode ada; kartu Pengaturan diperiksa di emulator |
| Nama sama dari dua HP digabung, rujukan ikut berpindah, pemberitahuan | **Terverifikasi** (`SyncEngineTest`: kategori dari dua HP, kas −Rp65.000 di keduanya) |
| Antrean hanya diisi saat sync menyala (skema 12) | **Terverifikasi** (`MigrationTest.migrate11To12…`, `SyncEngineTest`) |
| Kebijakan privasi dan catatan Data Safety | Diperbarui (ID/EN) |

**Belum diketahui:** perilaku dengan akun Google sungguhan di dua HP fisik. Uji ini perlu pemilik
masuk dengan akunnya sendiri; agen tidak boleh memasukkan kredensial. Langkah uji ada di bagian 10.

## 8. Usulan fitur untuk usaha menengah (diputuskan setelah audit)

Simulasi penggilingan (Rp48 M/tahun, tiga unit) berjalan benar secara angka, tetapi usaha sebesar itu
hampir pasti punya kebutuhan yang Mankeu (kas murni, DEC-011) belum tampung. Ini **usulan**, bukan
keputusan:

| # | Usulan | Alasan dari simulasi | Catatan |
|---|---|---|---|
| P1 | **Catatan tagihan** (piutang/utang sebagai daftar pengingat, bukan jurnal): siapa, berapa, jatuh tempo; saat dibayar menjadi catatan kas biasa | Penggilingan menjual ke toko dengan tempo dan membeli gabah dengan bayar belakang | Menjaga DEC-011: kas tetap satu-satunya buku; tagihan tidak masuk laporan sampai dibayar |
| P2 | **Laporan per periode** (pilih bulan/kuartal/tahun) | Admin menengah perlu tutup buku bulanan, bukan akumulasi sejak awal | Laporan sekarang dari awal buku |
| P3 | **Peran pengguna** (kasir hanya mencatat, pemilik melihat semua) | Sync memakai satu akun Google; kasir melihat laporan pemilik | Butuh identitas per orang; besar |
| P4 | **Perkiraan PPh Final UMKM** (0,5% omzet, PP 55/2022) | Usaha kecil-menengah wajib menyetor; angka omzet sudah ada | Harus jelas "perkiraan", bukan nasihat pajak |
| P5 | **Stok terhubung penjualan** (jual produk mengurangi stok bahan sesuai resep) | Bengkel dan penggilingan mencatat stok dan penjualan terpisah dua kali | Resep sudah ada di Rencana Usaha |
| P6 | **Progres impor** (persen) untuk berkas besar | Impor 9 ribu baris ±10 detik dengan spinner saja | Sekarang sudah menyebut jumlah baris |

## 9. Ringkasan bukti

| Gate | Hasil |
|---|---|
| Unit test, lint | Lulus |
| Instrumented test | 45 test, 0 gagal (2 dilewati: test screenshot yang dijalankan lewat skrip), setelah review |
| `ScaleSimulationTest` | 5 berkas, semua angka sama persis |
| Jalan-panduan screenshot (bantuan + panduan) | Lulus; `check_guide_flow.py` OK |
| Build release R8 di emulator | Tanpa crash, juga setelah sync ditambahkan (APK 4,25 MB) |

## 10. Daftar periksa sebelum menekan "Rilis ke produksi"

Dikerjakan pemilik (butuh akun, kunci, atau Play Console):

1. **Uji sync dengan dua HP sungguhan**: HP A dan B masuk dengan akun Google usaha yang sama; nyalakan
   sync di A (buat kata sandi), catat satu jualan; nyalakan sync di B dengan kata sandi yang sama;
   pastikan jualan A muncul di B; catat di B, buka A, pastikan muncul. Coba kata sandi salah di HP
   ketiga: harus ditolak.
2. Bangun AAB release dengan kunci rilis (`KEYSTORE_*`), unggah ke jalur internal dulu, pasang dari
   Play, ulangi uji asap (buat usaha, catat, laporan, cadangan Drive, sync).
3. Perbarui formulir Data Safety sesuai `docs/STORE_LISTING_AND_POLICIES.md` (sync ditambahkan).
4. Pastikan halaman kebijakan privasi di adsurkasur.my.id sudah memuat bagian sync (dari repo ini).
5. Promosikan ke produksi dengan peluncuran bertahap (mis. 20%), pantau ANR/crash di Play Console.


## 11. Review menyeluruh dan perbaikannya (22 Oktober 2026, sore)

Review dua sumbu (standar kode + kesesuaian dengan permintaan dan DEC-022–024) atas seluruh run ini menemukan
lima bug nyata dan beberapa kekurangan. Semua diperbaiki dan diberi test nyata:

| Temuan | Perbaikan | Bukti |
|---|---|---|
| Putaran sync yang sedang mengunggah bisa dibatalkan oleh catatan baru; nomor kiriman dipakai ulang sehingga HP lain melewatkan perubahan | Nomor dipesan sebelum unggah; unggah + pencatatan tak bisa dibatalkan; hanya jeda yang di-debounce | `SyncControllerTest`; uji mutasi: perilaku lama membuat test gagal |
| Setelah modal awal, jualan berikutnya otomatis berkategori modal | Hanya kategori pendapatan/biaya yang dipilih otomatis | `EntryDefaultsTest`; uji emulator |
| Pratinjau impor lama menimpa yang baru | Pratinjau yang dibatalkan tidak menulis status | `ViewModelFlowsTest` (berkas 9 ribu vs 547 baris) |
| Gangguan jaringan disebut "kata sandi salah"; sync pertama gagal mematikan sync | Hanya gagal dekripsi = kata sandi beda; putaran pertama terpisah dari penyalaan | `SyncControllerTest` |
| Kata sandi tidak selalu dihapus dari memori | Dimuat setelah kunci; tanpa salinan String | Kode |
| Usaha tanpa unit bila langkah kedua gagal | Satu transaksi | `LedgerIntegrityTest` |
| Keterangan kosong belum berlaku untuk impor | Kosong → nama kategori | `TemplateImportTest` |
| Kata sandi beda di HP yang menyala lebih dulu | Gagal dekripsi kiriman HP lain dilaporkan jelas | `SyncControllerTest` |
| "Uang kas proyeksi", sakelar sapaan yang tak berefek, teks "tawaran" lama | Diganti; sakelar menunjukkan keadaan nyata | Uji emulator |
| Pemulihan cadangan tidak memicu sync | Memicu putaran | Kode |
| **Screenshot Play Store basi** (layar yang sudah dihapus) dan rasio 2,22 (di atas batas 2:1 Play) | 7 screenshot baru 1:2 dari aplikasi saat ini, lewat skrip | `scripts/make_store_screenshots.py` |
| Dokumen basi (skema 11, sync "belum tersambung", path aset lama) | Diperbarui | `check_doc_links.py` |

Catatan proses: commit `075cde9` di run ini tidak menyertakan `logs/`; dicatat di log aktivitas.
