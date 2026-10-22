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

## 7. Belum selesai di dokumen ini

Bagian sync, usulan fitur usaha menengah, dan daftar periksa akhir ditambahkan setelah
pekerjaannya selesai.
