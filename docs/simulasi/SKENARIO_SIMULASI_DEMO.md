# Skenario Simulasi Demo Mankeu

Panduan untuk menampilkan simulasi Mankeu di depan audiens (±25 menit, bisa dipendekkan ke ±12 menit
dengan babak bertanda **Inti**). Setiap langkah berisi **yang diketuk**, **yang diisi**, dan **yang harus
terlihat di layar**, sehingga Anda bisa mengecek angka sambil berbicara.

Semua angka di dokumen ini sudah dicocokkan dengan hitungan manual dan dengan aplikasi
(lihat `docs/panduan/PANDUAN_PENGGUNAAN_MANKEU.md` bagian 5 dan 11, serta
`docs/evidence/2026-10-22_simulasi_skala_umkm/expected.json`). Data seluruhnya fiktif.

---

## 1. Ringkasan alur

| Babak | Isi | Waktu | Inti |
|---|---|---|---|
| A | Mankeu bekerja tanpa internet | 1 menit | ✓ |
| B | Memulai usaha: satu formulir | 1 menit | ✓ |
| C | Menyusun Rencana Usaha dan membaca kelayakan | 6 menit | ✓ |
| D | Mencatat bulan pertama, termasuk memperbaiki salah ketik | 6 menit | ✓ |
| E | Membaca Beranda dan Laporan | 3 menit | ✓ |
| F | Untuk pengguna awam: kas minus dan "Catat uang awal" | 2 menit | |
| G | Skala menengah: impor setahun data penggilingan (9 ribu baris) | 3 menit | |
| H | Stok, bantuan "?", dan "Tunjukkan di aplikasi" | 2 menit | |
| I | Penutup: privasi, cadangan, sinkronisasi | 1 menit | ✓ |

Cerita yang dibawa: **Bu Sari** membuka *Kedai Kopi Senja* dengan modal Rp15.000.000. Ia ingin tahu
berapa yang harus terjual agar tidak rugi, berapa modal yang dibutuhkan, dan apakah bulan pertamanya
sudah untung.

---

## 2. Persiapan (paling lambat 1 jam sebelum tampil)

**Perangkat.** Gunakan **HP Android sungguhan** bila bisa, ditampilkan ke layar dengan `scrcpy` atau
fitur cermin layar. Emulator bisa dipakai, tetapi di emulator pengujian kami pemilih berkas tidak
merespons ketukan (babak G tidak bisa diperagakan), dan input suara emulator menyadap tombol Kembali
(gunakan panah ← di layar).

**Aplikasi.**

1. Pasang build terbaru (versi 0.1.0):
   ```bash
   ./gradlew assembleDebug
   ```
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
2. Mulai dari keadaan kosong agar sapaan dan alur awal tampil:
   ```bash
   adb shell pm clear com.mankeu
   ```
3. Bahasa HP Indonesia, mode terang, kecerahan penuh, notifikasi dimatikan (mode Jangan Ganggu).
4. Untuk babak G, salin berkas simulasi ke folder Download HP:
   ```bash
   adb push docs/evidence/2026-10-22_simulasi_skala_umkm/N1_penggilingan_Pabrik_Klaten.xlsx /sdcard/Download/
   ```
   Berkas `.xlsx` dibuat oleh `py -I docs/evidence/2026-10-22_simulasi_skala_umkm/make_sims.py`
   (tidak disimpan di Git). Salinannya juga ada di `app/src/androidTest/assets/simulasi/`.
5. Siapkan dokumen ini dan `docs/panduan/gambar/` (screenshot setiap langkah) di layar kedua, untuk
   cadangan bila HP bermasalah.

**Latihan.** Jalankan babak B–E sekali sebelum tampil, lalu kosongkan lagi dengan `pm clear`.

---

## 3. Skenario

### Babak A — Tanpa internet (Inti)

1. Nyalakan **mode pesawat** di HP.
2. Buka Mankeu.

**Terlihat:** layar Beranda dengan kartu *Mulai dengan menambah usaha* dan kartu sapaan
*Selamat datang! … ketuk tombol ? di kanan atas*.

**Poin bicara:** semua data tinggal di HP. Tidak perlu login, tidak ada iklan, tidak ada pelacak.
Ketuk **Tutup** pada sapaan.

### Babak B — Memulai usaha (Inti)

1. Ketuk **Tambah Usaha**. Formulir langsung terbuka.
2. Nama usaha: `Kedai Kopi Senja`, ketuk **Simpan**.

**Terlihat:** Beranda siap dipakai, di kiri atas *Kedai Kopi Senja · Utama*, tombol **Rencana | Realisasi**
muncul, semua angka Rp0.

**Poin bicara:** satu formulir. Unit "Utama" dibuat otomatis; usaha bercabang tinggal menambah unit.

### Babak C — Rencana Usaha dan kelayakan (Inti)

1. Ketuk **Rencana** di atas, lalu **Catatan** di bilah bawah. Halaman *Rencana Usaha* terbuka.
2. **Periode rencana** → **Ubah** → bulan ini sampai 11 bulan ke depan (12 bulan) → **Simpan**.
3. **Produk → Tambah**:

   | Isian | Kopi Susu | Es Teh |
   |---|---|---|
   | Satuan | cup | gelas |
   | Harga jual | 10000 | 5000 |
   | Target per bulan | 700 | 400 |
   | Resep | Susu Rp18.000 untuk 6 cup; Kopi dan cup Rp2.000 untuk 1 cup | Teh, gula, es Rp1.500 untuk 1 gelas |

   **Terlihat** di bawah resep Kopi Susu: biaya variabel Rp5.000 per cup, margin Rp5.000.
4. **Biaya Tetap per Bulan → Tambah** tiga kali: Sewa lapak Rp1.000.000 (*Sewa Tempat*), Gaji Rp800.000
   (*Gaji & Upah*), Listrik dan air Rp200.000 (*Listrik, Air & Internet*).
5. **Aset dan Penyusutan → Tambah**: Mesin espresso Rp7.200.000, umur pakai 36 bulan.
   **Terlihat:** penyusutan Rp200.000 per bulan.
6. **Setoran Modal → Tambah**: Modal awal Rp15.000.000, bulan pertama.

**Terlihat** di kartu *Kelayakan & Titik Impas (BEP)*:

| Baris | Angka |
|---|---|
| Status | **Layak** |
| Omzet impas per bulan | **Rp4.040.816** |
| BEP Kopi Susu / Es Teh | **314,286 cup** / **179,592 gelas** |
| Laba per bulan (pada target) | **Rp2.700.000** |
| R/C · B/C | **1,43 · 0,43** |
| Kebutuhan modal | **Rp4.300.000** |
| Setoran modal direncanakan / Masih kurang | **Rp15.000.000 / Rp0** |

**Poin bicara:**
- Biaya tetap Rp2.200.000 (termasuk penyusutan), margin pada target Rp4.900.000, jadi laba
  Rp2.700.000.
- Kebutuhan modal datang dari bulan pertama: mesin dibeli di depan, jadi kas sempat −Rp4.300.000.
  Modal Rp15 juta menutupnya.
- BEP adalah batas bawah, bukan target: target Rp9.000.000, impas Rp4.040.816.

### Babak D — Mencatat bulan pertama (Inti)

Ketuk **Realisasi** di atas. Semua catatan lewat tombol **+** lalu **Catat Pemasukan** atau
**Catat Pengeluaran**.

| # | Jenis | Jumlah | Kategori | Isian lain |
|---|---|---|---|---|
| 1 | Pemasukan | 15000000 | Setoran Modal Pemilik | keterangan *Modal awal* |
| 2 | Pemasukan | (otomatis 4.600.000) | Penjualan & Jasa | Produk *Kopi Susu*, Volume `460` |
| 3 | Pemasukan | (otomatis 900.000) | Penjualan & Jasa | Produk *Es Teh*, Volume `180`, keterangan dikosongkan |
| 4 | Pengeluaran | 2350000 | Bahan Baku & Stok | *Belanja susu, kopi, cup* |
| 5 | Pengeluaran | 7200000 | Beli Peralatan & Aset | *Mesin espresso*, umur pakai 36 |
| 6 | Pengeluaran | 1000000 | Sewa Tempat | |
| 7 | Pengeluaran | 800000 | Gaji & Upah | |
| 8 | Pengeluaran | **1500000** (sengaja salah) | Listrik, Air & Internet | *Listrik bulan ini* |

**Poin bicara saat mencatat:**
- Catatan 2: pilih produk, isi volume, jumlah uang terisi sendiri (460 × Rp10.000).
- Catatan 3: keterangan dikosongkan, aplikasi memakai nama kategori. Produk dan volume tetap diisi,
  karena Status BEP bulan ini menghitung jumlah terjual per produk.
- Setelah catatan 1, formulir pemasukan berikutnya **kembali ke Penjualan & Jasa**, bukan Setoran
  Modal. Aplikasi tidak membiarkan jualan tercatat sebagai modal karena kebiasaan.

**Memperbaiki salah ketik:** buka **Catatan**, ketuk ikon **pensil** pada *Listrik bulan ini*, ganti jumlah
ke `150000`, **Simpan catatan**.

**Terlihat:** kartu listrik kini −Rp150.000. **Poin bicara:** salah ketik diperbaiki di tempat; laporan
ikut menyesuaikan.

### Babak E — Beranda dan Laporan (Inti)

**Beranda** (Realisasi):

| Kartu | Angka |
|---|---|
| Uang kas realisasi | **Rp9.000.000** |
| Untung / Rugi kas | **Rp1.200.000** |
| Pendapatan / Biaya Operasional | **Rp5.500.000** / **Rp4.300.000** |

Gulir ke **Status BEP Bulan Ini**: omzet bulan ini Rp5.500.000 **sudah melewati** titik impas bulan ini
(Rp4.035.836).

**Laporan**: tab *Laba Rugi dan Arus Kas* (laba setelah penyusutan Rp1.000.000: mesin tidak dihitung
biaya sekaligus, melainkan Rp200.000 per bulan), tab *Kelayakan & BEP*, tab *Realisasi vs Target*.

**Poin bicara:** kas Rp9.000.000 = 15.000.000 + 4.600.000 + 900.000 − 2.350.000 − 7.200.000 − 1.000.000 −
800.000 − 150.000. Laba berbeda dari kas karena modal dan pembelian mesin bukan pendapatan atau biaya
bulan ini.

### Babak F — Untuk pengguna awam (opsional)

Menunjukkan bagaimana aplikasi menolong orang yang lupa mencatat uang awal.

1. Ketuk nama usaha di kiri atas → **Tambah Usaha** → `Warung Bu Ani` → **Simpan**.
2. Catat Pemasukan Rp650.000 (Penjualan & Jasa), lalu Catat Pengeluaran Rp1.950.000 (Bahan Baku & Stok).

**Terlihat:** kas merah −Rp1.300.000, di bawahnya kartu **Uang kas minus?** dengan tombol
**Catat uang awal**, dan satu kalimat bahwa belanja stok dihitung sebagai biaya pada hari dibayar.

3. Ketuk **Catat uang awal**: formulir terbuka dengan kategori *Setoran Modal Pemilik* sudah terpilih.
   Isi 2000000, **Simpan**.

**Terlihat:** kas **Rp700.000**, kartu peringatan hilang sendiri.

4. Kembali ke *Kedai Kopi Senja* lewat nama usaha di kiri atas → **Pakai ini**.

### Babak G — Skala menengah: impor setahun data (opsional, butuh HP sungguhan)

1. Nama usaha → **Tambah Usaha** → `Penggilingan Sumber Rejeki` → **Simpan**.
2. Ikon **map** (Berkas dan Data) → **Pilih file .xlsx** → `N1_penggilingan_Pabrik_Klaten.xlsx`.

**Terlihat di pratinjau:** 8.975 transaksi, satu produk, baris **Masuk ke: Penggilingan Sumber Rejeki ·
Utama**.

3. Ketuk **Setujui dan impor**. **Terlihat:** *Menyimpan 8975 baris…* beberapa detik, lalu
   *Impor selesai*.
4. Buka Beranda (Realisasi):

| Kartu | Angka |
|---|---|
| Uang kas realisasi | **Rp3.038.187.000** |
| Pendapatan Operasional | **Rp23.708.896.000** |
| Biaya Operasional | **Rp21.000.709.000** |
| Untung / Rugi kas | **Rp2.708.187.000** |

**Poin bicara:** setahun data penggilingan menengah (sekitar Rp24 miliar penjualan) dari Excel, setiap
angka cocok dengan hitungan independen. Impor bisa dibatalkan dari riwayat impor di Berkas dan Data.

### Babak H — Stok dan bantuan (opsional)

1. Kembali ke *Kedai Kopi Senja*. Tab **Stok → Tambah Barang**: Susu UHT, liter, Rp18.000, stok 24,
   batas minimum 6.
2. Ketuk **?** di kanan atas → **Buka Pusat Bantuan** → *Mencatat penjualan* → **Tunjukkan di aplikasi**.

**Terlihat:** aplikasi pindah ke halaman yang tepat dan menyorot tombolnya.

### Babak I — Penutup (Inti)

- **Privasi:** data tetap di HP; cadangan dienkripsi dengan kata sandi milik pengguna (AES-256-GCM),
  boleh ke berkas atau ke Google Drive pribadi.
- **Sinkronisasi antar-HP:** HP pemilik dan HP kasir memakai buku yang sama, terenkripsi ujung ke ujung
  (Pengaturan → *Sinkronisasi antar-HP*). **Tunjukkan kartunya saja**; jangan menjanjikan peragaan
  langsung, karena uji dengan akun Google sungguhan di dua HP belum dilakukan.
- Matikan mode pesawat.

---

## 4. Lembar contekan angka

| Yang ditanyakan | Jawaban |
|---|---|
| Omzet impas per bulan | Rp4.040.816 |
| BEP per produk | 314,286 cup kopi · 179,592 gelas teh |
| Laba per bulan pada target | Rp2.700.000 |
| R/C · B/C | 1,43 · 0,43 |
| Kebutuhan modal | Rp4.300.000 |
| Kas akhir bulan pertama | Rp9.000.000 |
| Untung/rugi kas bulan pertama | Rp1.200.000 |
| Laba setelah penyusutan | Rp1.000.000 |
| Titik impas bulan ini | Rp4.035.836 (sudah terlewati) |
| Penggilingan, kas setelah impor | Rp3.038.187.000 |

## 5. Bila ada yang tidak sesuai

| Gejala | Tindakan |
|---|---|
| Angka berbeda dari tabel | Biasanya ada catatan dobel atau tanggal di luar bulan ini. Buka **Catatan**, periksa, **Ubah** atau **Hapus**. Bila waktu mepet, lanjutkan dengan screenshot di `docs/panduan/gambar/` (nomor 14, 22, 23, 30). |
| Formulir tertutup sebelum disimpan | Ketuk **+** lagi; isian yang belum disimpan memang tidak tercatat. |
| Pemilih berkas tidak merespons (emulator) | Lewati babak G, tunjukkan `docs/panduan/gambar/33_pratinjau_impor.jpg`. |
| Tombol Kembali tidak bekerja (emulator) | Gunakan panah ← di kiri atas atau ketuk di luar lembar. |
| Ingin mengulang dari awal | `adb shell pm clear com.mankeu`, buka aplikasi lagi. |

## 6. Pertanyaan yang mungkin muncul

- **Apakah harus online?** Tidak. Internet hanya untuk cadangan Google Drive dan sinkronisasi, dan
  keduanya opsional.
- **Bisa untuk piutang atau utang?** Belum. Mankeu sengaja mencatat kas saja (DEC-011). Catatan tagihan
  sederhana ada di daftar usulan untuk usaha menengah.
- **Bagaimana kalau HP hilang?** Pulihkan dari cadangan terenkripsi (berkas atau Google Drive) di HP baru.
- **Apakah data dikirim ke pengembang?** Tidak. Tidak ada server pengembang, iklan, atau pelacak.
- **Sudah di Play Store?** Versi 0.1.0 disiapkan untuk rilis produksi; versi sebelumnya beredar di jalur
  pengujian internal.
