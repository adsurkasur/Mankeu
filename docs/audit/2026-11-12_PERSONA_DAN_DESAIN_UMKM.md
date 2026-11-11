# Persona UMKM dan Usulan Desain (2026-11-12)

> **Tujuan:** menguji Mankeu seperti pengguna UMKM dari berbagai bidang, lalu mengusulkan desain yang sesuai
> kenyataan lapangan, supaya perencana (Fable) menyusun rencana yang tepat sasaran.
> **Prinsip pemilik:** fleksibel tetapi tetap simpel; jangan membatasi UMKM; app harus **useful**.
> **Metode dan batas kejujuran:**
> - Dikerjakan oleh Sonnet 5.5 (effort rendah). Sebagian skenario dijalankan langsung di emulator (Gboard berfungsi)
>   dan sebagian dinilai dari membaca model data di kode.
> - Setiap temuan ditandai: `[dicoba]` (dijalankan di emulator), `[kode]` (dibuktikan dari kode), atau `[dugaan]`
>   (penalaran; perlu diuji dengan pengguna nyata).
> - Persona adalah rekaan penyusun berdasarkan pola umum usaha kecil di Indonesia. **Bukan pengganti uji dengan
>   pengusaha sungguhan**, yang tetap disarankan (paket baru di §6).

## 1. Fakta model yang menentukan semuanya `[kode]`

| Fakta | Akibat untuk pengguna |
|---|---|
| Baris nota **wajib** produk (`SaleLineDraft.productId: String`, tidak boleh kosong) | Tidak bisa menjual "ganti oli Rp50.000" atau "ongkos kirim" tanpa membuat produk dulu. |
| Stok produk hanya dari **produksi**, hitung stok, dan penjualan; belanja hanya menautkan **bahan** | Pedagang yang beli barang jadi lalu menjual lagi (kelontong, reseller) tidak punya jalur yang wajar. |
| Produk selalu punya "Resep 1 batch", "1 batch jadi N", dan stok | Jasa dan sewa dipaksa memakai kosakata produksi dan stok yang bisa minus. |
| Pilihan satuan = daftar tetap + "Lainnya" (tidak diingat) | Lihat audit menyeluruh §11 F1. |
| Satu kas, satu cara bayar; tidak ada retur, tidak ada uang muka/jaminan sebagai konsep | Konveksi, sewa, dan jual online terpaksa memakai akal-akalan. |
| Tidak ada pilihan jenis usaha saat mulai | Semua pengguna baru melihat kosakata dan langkah yang sama ("Buat produk dan resepnya"). |

## 2. Persona dan hasil uji

Skor kecocokan sekarang: ★★★ cocok, ★★ bisa dengan akal-akalan, ★ tidak cocok.

### P1. Bu Sri, produsen keripik rumahan ★★★
- Tugas: produksi per batch, jual per dus/bungkus ke toko, tagihan, titip jual.
- Hasil `[dicoba]` (sesi Fase 1–5): HPP per satuan, nota, tagihan, produksi berjalan sesuai angka acuan. Inilah kasus yang
  dirancang. Titip dan belanja tempo belum punya layar (Paket F).

### P2. Pak Joko, jasa sewa traktor dan bajak sawah ★
- Tugas: menyewakan traktor per hektar atau per hari; biaya BBM dan upah sopir per pekerjaan; pelanggan petani membayar
  setelah panen (tagihan panjang); mencatat solar yang dibeli.
- `[dicoba]` Membuka "Produk baru": layar langsung menampilkan "Resep 1 batch", "1 batch jadi", dan pill bungkus/pcs/
  porsi/botol/kg. Untuk jasa, semua itu tidak bermakna. Satuan "hektar" harus lewat "Lainnya". Dugaan awalku adanya bug fokus keyboard (teks "Bajak sawahhektar") **salah**: itu artefak skrip uji. Yang nyata: setelah memilih "Lainnya", kotak satuan terisi nilai pilihan awal ("bungkus") sehingga mengetik menyisip di tengah. Lihat `2026-11-12_AUDIT_BISNIS_PENGGUNA_AWAM.md` U5.
- `[kode]` Produk jasa punya stok yang bisa minus; "Produksi" dan "Hitung stok" tampil percuma; peringatan stok menipis
  bisa muncul.
- `[kode]` HPP jasa = biaya per kali kerja (BBM + upah), tetapi model memaksa "per batch" dengan hasil N satuan.
- Kebutuhan lain `[dugaan]`: satu pelanggan banyak pekerjaan bulanan dalam satu tagihan; harga berbeda per pelanggan.
- **Kebutuhan desain:** jenis produk "Jasa/sewa" tanpa stok dan tanpa batch.

### P3. Bu Tini, toko kelontong (beli barang jadi, jual eceran) ★
- Tugas: beli beras 1 karung 25 kg, jual per kg atau per liter; beli rokok dan sabun per bungkus; jual cepat tiap hari.
- `[kode]` Tidak ada jalur "barang dagangan": belanja menambah stok **bahan**, bukan stok produk yang dijual. Untuk menjual
  beras 25 kg per karung dalam satuan kg, ia harus membuat bahan, membuat produk beserta resep "1 karung = 25 kg", lalu
  mencatat "Produksi" setiap kali belanja. Itu tidak masuk akal bagi pedagang.
- `[kode]` Jual cepat 20 kali sehari = 20 nota @ 6 ketukan. Tidak ada mode kasir.
- `[dugaan]` Harga beli berubah sering; ia mau tahu untung per barang tanpa menghitung.
- **Kebutuhan desain:** jenis produk "Barang dagangan" (beli jadi): stok dari pembelian, HPP = harga beli terakhir (atau
  rata-rata), satuan beli dan satuan jual berbeda (karung ke kg), dan **jual cepat** (ketuk produk, +1, selesai).

### P4. Mbak Rina, warung makan ★★
- Tugas: menu per porsi, bahan harian, kas tiap hari, sisa makanan dibuang.
- `[kode]` Resep dengan "1 panci = 20 porsi" cocok untuk HPP. Tetapi makanan matang tidak diproduksi dan disimpan seperti
  keripik: stok produk minus dan "Produksi" harian terasa mengada-ada.
- `[dugaan]` Penjualan harian cenderung dicatat satu kali di malam hari ("hari ini Rp850.000"), bukan per nota.
- **Kebutuhan desain:** produk "dibuat sendiri tanpa stok" (jual langsung, HPP dari resep), **catat penjualan harian
  total** sebagai alternatif nota, dan pencatatan "bahan terbuang".

### P5. Pak Dedi, petani cabai (musiman) ★★
- Tugas: biaya sepanjang musim tanam (bibit, pupuk, upah), panen bertahap, jual ke pengepul per kg dengan harga harian.
- `[kode]` "1 batch = 1 musim, hasil N kg" dan biaya bebas bisa dipakai, tetapi HPP dari resep akan berbeda dari uang
  yang benar-benar keluar (muncul sebagai selisih belanja produksi, yang memang dirancang menjelaskannya).
- `[dugaan]` Harga jual berubah tiap hari: harga di nota sudah bisa diubah. Panen bertahap: produksi berkali-kali per
  batch tidak ada konsep "batch terbuka yang biayanya menumpuk".
- **Kebutuhan desain:** "siklus/proyek" dengan biaya berjalan dan hasil yang masuk bertahap; hitung untung per siklus.

### P6. Ibu Wati, konveksi/penjahit pesanan ★★
- Tugas: pesanan custom, uang muka (DP) 50 %, sisa saat selesai, bahan kain per pesanan, jarang ada stok jadi.
- `[kode]` DP bisa dicatat sebagai pembayaran sebagian pada nota. Tetapi nota baru terbit ketika menjual; tidak ada "pesanan
  yang sedang dikerjakan" (status, tanggal selesai) dan baris nota wajib produk, sehingga tiap pesanan custom harus
  dibuatkan produk dulu.
- **Kebutuhan desain:** baris nota bebas (nama + harga), status pesanan (diterima, dikerjakan, selesai), dan biaya per
  pesanan.

### P7. Pak Anton, bengkel motor ★★
- Tugas: satu nota berisi suku cadang (punya stok, dibeli dari toko) dan jasa (ganti oli, tanpa stok).
- `[kode]` Tidak bisa mencampur dalam satu nota tanpa membuat dua produk palsu; suku cadang tidak punya jalur stok barang
  dagangan (seperti P3).
- **Kebutuhan desain:** jenis produk campuran dalam satu nota (barang dagangan + jasa) dan baris bebas.

### P8. Mas Bayu, penjual online/reseller ★★
- Tugas: beli dari supplier, jual lewat marketplace, kadang dropship tanpa stok, ongkir dan biaya admin marketplace.
- `[dugaan]` Pencairan marketplace datang bulanan (piutang ke marketplace), dipotong biaya admin. Cara bayar transfer.
- **Kebutuhan desain:** barang dagangan (P3), pelanggan "Marketplace" dengan piutang, dan biaya yang dipotong dari
  pembayaran (potongan sebagai biaya, bukan diskon).

### P9. Bu Lina, laundry kiloan ★
- Tugas: harga per kg, antar-jemput, deterjen dan listrik sebagai biaya, langganan bulanan.
- Sama dengan P2 (jasa tanpa stok), ditambah pelanggan langganan dengan tagihan bulanan.

### P10. Pak Rudi, rental alat (tenda, sound system) ★
- Tugas: harga per hari, uang jaminan yang dikembalikan, denda keterlambatan.
- `[kode]` Jaminan (uang masuk yang akan dikembalikan) tidak punya konsep. Bila dicatat sebagai pendapatan, untung salah.
- **Kebutuhan desain:** uang titipan/jaminan sebagai kewajiban (bukan pendapatan), dan jasa per hari.

### Ringkasan kecocokan

| Persona | Skor | Penghalang utama |
|---|---|---|
| Keripik (P1) | ★★★ | (Paket F: titip, tempo) |
| Petani (P5) | ★★ | tidak ada siklus dengan biaya berjalan |
| Warung makan (P4) | ★★ | produk tanpa stok, penjualan harian total |
| Konveksi (P6), bengkel (P7), online (P8) | ★★ | baris nota bebas, barang dagangan, status pesanan |
| Jasa traktor (P2), laundry (P9), rental (P10) | ★ | tidak ada produk jasa tanpa stok; jaminan |
| Kelontong (P3) | ★ | tidak ada barang dagangan dan jual cepat |

Hampir separuh persona (4 dari 10) hampir tidak bisa memakai app ini dengan wajar. Itu berlawanan dengan prinsip pemilik.

## 3. Pola kebutuhan yang berulang

1. **Jenis item.** Pemakai butuh tiga jenis: **Dibuat sendiri** (resep, batch, stok), **Barang dagangan** (beli jadi, stok
   dari pembelian), **Jasa/sewa** (tanpa stok). Sekarang hanya jenis pertama yang ada.
2. **Baris bebas di nota.** "Ongkir", "ganti oli", "pesanan custom" tanpa membuat produk.
3. **Jual cepat dan penjualan harian.** Banyak usaha kecil mencatat banyak transaksi kecil.
4. **Satuan milik usaha** (audit §11 F1) dan satuan beli berbeda dari satuan jual (karung ke kg).
5. **Kosakata menyesuaikan jenis usaha.** "Produk/Resep/Produksi/Batch" untuk keripik; "Menu/Resep" untuk warung;
   "Layanan/Tarif" untuk jasa; "Barang" untuk kelontong. Satu mesin yang sama, label berbeda.
6. **Status pesanan dan uang muka**, serta **uang titipan/jaminan** sebagai kewajiban.
7. **Siklus/proyek** dengan biaya berjalan (tani, konveksi per pesanan, pekerjaan sewa).
8. **Pelanggan khusus** (marketplace) dengan potongan biaya dan pencairan.

## 4. Usulan desain: fleksibel tetapi simpel

### 4.1 Prinsip
- **Satu mesin, banyak bentuk.** Data inti tidak berubah (nota, stok, HPP, tagihan, untung dari `MonthlySummary`).
  Yang bertambah hanya *jenis item* dan *cara memasukkan*.
- **Mulai simpel, muncul saat dibutuhkan** (progressive disclosure). Pengguna baru hanya melihat Jual, Belanja, dan
  Untung. Resep, titip, utang, dan jenis lanjutan muncul bila ia memakainya atau memintanya.
- **Pengaturan awal = saran, bukan batas.** Saat mulai, pengguna memilih jenis usaha (atau "lainnya") hanya untuk
  mengisi saran awal (kosakata, satuan, kategori, contoh item). Semuanya bisa diubah, dan "lainnya" selalu ada.
- **Daftar milik pengguna.** Satuan, kategori, pelanggan, pemasok, bahan: belajar dari pemakaian, bisa dicari, bisa
  ditambah kapan saja (audit §11).

### 4.2 Perubahan model (rencana teknis ringkas)

| # | Perubahan | Isi | Catatan |
|---|---|---|---|
| D1 | `Product.kind`: `MADE` (dibuat sendiri), `TRADED` (barang dagangan), `SERVICE` (jasa/sewa) | MADE seperti sekarang. TRADED: stok dari belanja bertaut produk; HPP = harga beli terakhir (opsi rata-rata di tahap lanjut); satuan beli ke satuan jual (1 karung = 25 kg). SERVICE: tanpa stok, tanpa batch; HPP opsional = biaya per kali (BBM+upah). Editor menyembunyikan bagian yang tidak relevan. | schema 14 belum rilis, boleh diubah; DEC-012: kolom di lembar Produk |
| D2 | Belanja bisa menaut **produk** (TRADED) selain bahan | Belanja "Beras 1 karung" menambah stok beras (kg) lewat konversi. | memperluas U3 |
| D3 | Baris nota bebas | `SaleLine.productId` boleh kosong, dengan nama + jumlah + harga (+ HPP opsional). Tidak memengaruhi stok. | uang masuk ke penjualan; untung = harga − HPP opsional |
| D4 | Jual cepat | Layar "Jual" menampilkan produk favorit sebagai tombol besar; ketuk = +1; total tampil; satu ketukan "Lunas". Tersimpan sebagai nota lunas. | target 2 ketukan per transaksi kecil |
| D5 | Penjualan harian total | Alternatif "Catat penjualan hari ini: Rp…" tanpa rincian produk; masuk sebagai nota satu baris "Penjualan harian". | untung memakai HPP perkiraan atau 0 (ditandai) |
| D6 | Jenis usaha saat mulai (opsional, bisa dilewati) | Memilih jenis mengisi saran satuan, kategori, contoh produk, dan kosakata label. | konfigurasi, bukan logika berbeda |
| D7 | Label kosakata per usaha | "Produk/Menu/Layanan/Barang", "Resep/Bahan", "Produksi/Masak". Hanya teks tampilan. | disimpan per usaha |
| D8 | Satuan milik usaha | Lihat audit §11 F1. | |
| D9 | Uang muka, jaminan, dan status pesanan | DP sudah bisa. Tambah **kewajiban** "uang titipan" (jaminan) yang bukan pendapatan; status pesanan sederhana pada nota (baru, dikerjakan, selesai, tanggal janji). | tahap lanjut |
| D10 | Siklus/proyek | Kelompok biaya dan hasil dengan tanggal mulai-selesai, untung per siklus; panen bertahap. | tahap lanjut |
| D11 | Pelanggan khusus dan potongan | Kontak bertanda "marketplace" dengan potongan persen/biaya otomatis saat pencairan. | tahap lanjut |
| D12 | Retur penjualan dan cara bayar | Lihat audit §11 F3, F4. | keputusan pemilik |
| D13 | Bahan terbuang / penyesuaian stok bernama | "Buang", "Rusak", "Pakai sendiri" sebagai alasan penyesuaian stok. | biaya masuk ke untung |

### 4.3 Peta mulai: tiga jalur, satu layar pertama
Pengguna baru memilih dari kartu bergambar (tanpa kotak isian): "Saya membuat produk" (MADE), "Saya jual beli barang"
(TRADED), "Saya menjual jasa/sewa" (SERVICE), "Campuran/Lainnya". Pilihan ini hanya mengatur bentuk awal dan bisa
berubah per item. Beranda lalu menampilkan 3 langkah awal yang sesuai jalurnya.

### 4.4 Yang sengaja tidak diubah
- `MonthlySummary` tetap satu-satunya sumber angka; jenis item baru harus memberi masukan ke situ, bukan menghitung sendiri.
- Format Excel tetap milik Mankeu dan lossless (kolom jenis item, satuan beli, baris bebas masuk template v5 sebelum rilis).
- Tidak ada pinjaman/bunga/cicilan bank (keputusan #2).

## 5. Prioritas (usulan untuk Fable, urut dari paling mendesak ke usefulness)

| Gelombang | Isi | Alasan |
|---|---|---|
| **G1 (wajib sebelum rilis ke UMKM selain produsen)** | D1 (jenis item), D3 (baris bebas), D8 (satuan milik usaha), D2 (belanja barang dagangan) | membuka 6 dari 10 persona; semuanya mengubah model sehingga harus sebelum schema dirilis |
| **G2** | D4 (jual cepat), D5 (penjualan harian), D6+D7 (jenis usaha dan kosakata), D13 | membuat harian terasa cepat dan akrab |
| **G3** | D9 (jaminan, status pesanan), D10 (siklus), D11 (marketplace), D12 (retur, cara bayar) | bidang spesifik; tanyakan pemilik dulu |

**Catatan waktu:** G1 menyentuh schema dan template Excel. Schema 14 dan template v5 belum dirilis, jadi *saat ini adalah
waktu termurah* untuk mengubahnya. Setelah rilis ke internal testing, setiap perubahan butuh schema 15, migrasi, dan
BackupUpgrade baru. Rekomendasi kuat: **kerjakan G1 sebelum Paket G (template) dan sebelum merge/rilis.**

## 6. Cara menguji dengan pengguna sungguhan (disarankan, paket baru)

- 5 pengusaha dari bidang berbeda (minimal: produksi makanan, kelontong, jasa/sewa, warung makan, konveksi/bengkel).
- Satu sesi 30 menit, direkam (layar dan suara) dengan izin: tugas harian mereka sendiri, tanpa dibimbing. Catat: di mana
  berhenti, kata yang tidak dimengerti, dan apa yang mereka ingin catat tetapi tidak bisa.
- Hasilnya masuk ke dokumen ini sebagai bagian "hasil lapangan" dan mengubah prioritas §5.
- Pemilik yang menemui mereka; agen menyusun skrip tugas dan lembar catatan.

## 7. Tugas untuk Fable (ringkas)
1. Putuskan struktur paket untuk G1 sebelum Paket G dan merge; perbarui rencana dan register.
2. Tulis keputusan produk yang perlu pemilik (retur, cara bayar, jaminan, siklus) sebagai pertanyaan pilihan ganda.
3. Rinci tiap perubahan model (D1 sampai D13) dengan skema, migrasi, template Excel, tes golden per persona.
4. Susun skenario uji per persona sebagai tes otomatis (`PersonaScenarioTest`), supaya kecocokan tidak mundur.
5. Rancang cara mengukur "simpel": jumlah ketukan dan isian per tugas per persona, target di tabel audit 2026-10-30.
