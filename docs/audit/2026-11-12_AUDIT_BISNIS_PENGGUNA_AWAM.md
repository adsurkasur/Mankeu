# Audit Logika Bisnis dari Sudut Pengguna Awam dan Rancangan App yang Seharusnya (2026-11-12)

> **Permintaan pemilik:** app harus benar-benar berguna bagi banyak UMKM (bahkan usaha besar). Penilaian intuitif harus dari
> pengguna yang **tidak mahir teknologi dan tidak paham istilah keuangan**, bukan dari orang yang sudah mengerti alurnya.
> Latar: presentasi ke mitra calon pengguna menunjukkan mereka tidak berminat karena app sulit dan tidak mewakili kebutuhan mereka.
>
> **Pembaca:** Fable (menyusun rencana) lalu agen pelaksana (Sonnet, Gemini).
> **Penyusun:** Sonnet 5.5 (effort rendah).
>
> **Batas kejujuran.** Persona adalah rekaan penyusun. Sebagian skenario dijalankan langsung di emulator dengan data kosong
> (`[dicoba]`), sebagian dinilai dari kode (`[kode]`), sebagian dugaan (`[dugaan]`). Ini **tidak menggantikan uji dengan
> pengguna awam sungguhan** (§8). Kesimpulan besarnya sendiri didukung bukti kuat dari uji ini; angka dan detail sebaiknya
> divalidasi lapangan.

## 1. Kesimpulan satu paragraf

Mankeu sekarang dirancang dari **sudut pandang akuntan/pengelola** (produk, resep, batch, satuan terkecil, HPP, unit usaha,
buku) dan menuntut pengguna **menyiapkan struktur dulu sebelum bisa mencatat apa pun**. Pengguna awam berpikir sebaliknya:
"tadi saya jual 3 kantong dapat Rp30.000, beli bahan Rp100.000, siapa yang belum bayar, untung berapa?" Mereka ingin
**mencatat dulu, merapikan nanti**. Selama urutan ini terbalik, tidak ada perbaikan tampilan (warna, jarak, tombol) yang
akan membuat app ini "intuitif". Kesimpulan ini sejalan dengan reaksi mitra saat presentasi.

## 2. Standar "intuitif untuk pengguna awam" (cek-lis mengikat)

Setiap layar dan alur harus lolos semuanya. Ini juga menjadi dasar tes keterpakaian (§8).

1. **Bahasa sehari-hari.** Tanpa istilah keuangan atau teknis (HPP, batch, resep, unit usaha, buku, rasio, persediaan,
   piutang, kategori pembukuan). Bila istilah tak terhindar, ada kata awam di sebelahnya ("modal barang (HPP)").
2. **Nilai dulu, struktur nanti.** Pengguna bisa mencatat transaksi pertamanya **dalam kurang dari 1 menit tanpa menyiapkan
   apa pun** (produk, bahan, satuan, pelanggan).
3. **Satu layar, satu tugas, satu pertanyaan.** Tidak ada layar yang meminta 5 jenis informasi sekaligus.
4. **Tidak ada jalan buntu.** Bila ada syarat yang belum terpenuhi, ada tombol yang langsung menyelesaikannya, bukan hanya teks petunjuk.
5. **Tombol mati harus menjelaskan.** Bila "Simpan" tidak bisa ditekan, layar menyebut apa yang kurang, di tempatnya.
6. **Contoh di setiap isian yang tidak sudah jelas** (placeholder berisi contoh, bukan label abstrak).
7. **Pilih, jangan ketik, jangan menghitung.** Pengguna tidak melakukan pembagian atau konversi sendiri.
8. **Selalu jelas langkah berikutnya** dan **bisa dibatalkan dengan mudah** (Urungkan, bukan peringatan menakutkan).
9. **Tidak perlu membaca.** Ikon besar berteks pendek, urutan konsisten; cocok untuk membaca cepat dan mata lelah.
10. **Ramah HP murah dan layar kecil:** target sentuh besar, kontras tinggi, tanpa gestur tersembunyi.
11. **Tidak menghukum kesalahan:** salah ketik jumlah mudah diperbaiki; tidak ada kehilangan data saat Back.
12. **Bantuan di tempatnya**, singkat, berbahasa awam, dan bisa dibaca lewat gambar atau suara bila perlu.

## 3. Hasil uji: pengguna awam mencoba app (build `b217794` + manifest, emulator dengan Gboard)

| # | Skenario pengguna awam | Hasil | Bukti | Cek-lis yang gagal |
|---|---|---|---|---|
| U1 | Baru buka app, ingin mulai | Layar pertama meminta **menambah usaha**, lalu muncul dua sheet bertumpuk bernama "Usaha dan Cabang / Unit" | `[dicoba]` | 1, 3 |
| U2 | Setelah usaha dibuat: apa yang harus dilakukan? | Beranda menampilkan 3 langkah: "Buat produk dan resepnya", "Catat penjualan pertama", "Lihat untung bulan ini". **Langkah 1 adalah pekerjaan terbesar** | `[dicoba]` | 2 |
| U3 | Pengguna tukang bajak: "Buat produk" | Layar menampilkan **Resep 1 batch**, **+ Biaya lain**, **1 batch jadi**, deretan pill bungkus/pcs/porsi/botol/kg/Lainnya, **Satuan terkecil**, **Dijual per**, **+ Satuan jual**. Tidak ada satu pun kata yang cocok dengan "sewa traktor per hektar" | `[dicoba]` | 1, 3, 6 |
| U4 | Mengisi lalu menekan Simpan | **Tombol Simpan abu-abu tanpa penjelasan.** Penyebabnya "1 batch jadi" belum diisi; app tidak menyebutnya. Pengguna terjebak tanpa tahu apa yang salah | `[dicoba]` (screenshot) | 4, 5 |
| U5 | Memilih satuan sendiri ("hektar") lewat "Lainnya" | Kotak "Satuan terkecil" **terisi "bungkus"** dari pilihan awal; mengetik langsung menyisip di tengah ("bungkhektarus"). Pengguna harus menghapus dulu | `[dicoba]` | 7, 11 |
| U6 | Satuan jual: sheet "Satuan jual" | Ada isian **"Isi"** (angka 1). Tidak ada penjelasan "isi apa" | `[dicoba]` | 1, 6 |
| U7 | Menekan "Kembali" di editor | Dialog "Buang perubahan? Isian yang belum disimpan akan hilang." dengan "Lanjut isi / Buang" yang sulit dipahami konteksnya; tombol sistem Back dan tombol atas menghasilkan dialog yang sama | `[dicoba]` | 8, 11 |
| U8 | Langsung ingin mencatat penjualan (langkah 2) tanpa produk | Nota baru terbuka dengan teks **"Belum ada produk. Tambahkan produk dulu di tab Produk."** **Tidak ada tombol** untuk melakukannya. Jalan buntu | `[dicoba]` | 2, 4 |
| U9 | Tab Catatan kosong | Judul "**0 catatan pada unit usaha dan buku yang aktif**" (istilah unit usaha dan buku). Tombol "Catat sekarang" membuka **"Uang masuk lain"**, bukan Jual atau Belanja | `[dicoba]` | 1, 2 |
| U10 | Pemilik yang menjual ingin mencatat "dapat Rp50.000" lewat jalur yang terlihat | Jalur terbuka adalah "Uang masuk lain" dengan kategori **"Penjualan & Jasa"** atau **"Pendapatan Lain"**. Jalur ini **tidak menghitung modal barang**, jadi untung bulan itu terlihat terlalu besar (masalah yang sama dengan audit demo awal) | `[dicoba]` + `[kode]` | 2 (nilai salah) |
| U11 | Jual beli barang jadi (kelontong): beli beras karungan, jual eceran | Tidak ada jalur. Stok produk hanya dari produksi; belanja hanya menambah stok bahan; baris nota wajib produk | `[kode]` | 2, 3 |
| U12 | Menyebut "jasa" tanpa stok | Produk jasa tetap punya stok yang bisa minus, tombol Produksi, dan peringatan menipis | `[kode]` | 1, 4 |
| U13 | Pengguna dengan banyak transaksi kecil per hari | Satu nota = sekitar 6 ketukan; tidak ada penjualan harian total atau jual cepat | `[dicoba]`+`[kode]` | 2, 7 |
| U14 | Pertama kali melihat Beranda berisi angka | "Untung bulan ini", "Uang di tangan", Tagihan: baik **setelah ada data**. Tetapi untuk mencapai data itu butuh U1–U4 | `[dicoba]` | — |

Catatan: skenario yang baik (bukan masalah): Back kini bekerja urut dan benar dengan Gboard; Beranda berisi angka dengan
kata sehari-hari; Tagihan bernama; tombol Terima bayar. Itu nilai yang harus dipertahankan.

## 4. Akar masalah logika bisnis

| # | Akar | Penjelasan |
|---|---|---|
| A1 | **Urutan terbalik** | App meminta pengguna mendefinisikan produk, resep, batch, satuan, kategori sebelum bisa mencatat satu rupiah pun. Pengguna awam tidak tahu jawabannya, apalagi manfaatnya. |
| A2 | **Satu model untuk semua usaha** | Jasa, dagang, dan produksi dipaksa memakai kosakata produksi. 4 dari 10 persona tidak bisa memakai app dengan wajar (lihat dokumen persona). |
| A3 | **Konsep akuntan dibuka di depan** | HPP, batch, "1 batch jadi", "Satuan terkecil", "Isi", "unit usaha", "buku" terlihat sejak langkah pertama. |
| A4 | **Nilai terbesar (untung sebenarnya) bergantung pada pekerjaan terberat (resep)** | Untung benar baru muncul bila HPP terisi, padahal mengisi resep adalah bagian tersulit. Tidak ada jalur menengah ("perkiraan modal per barang"). |
| A5 | **Kegagalan tak dijelaskan** | Simpan mati tanpa alasan; sheet bertumpuk; dialog "buang perubahan" tanpa konteks. |
| A6 | **Tidak ada pemahaman tujuan pengguna** | App belum menanyakan "usahamu apa?" dan "apa yang paling ingin kamu ketahui?" sehingga tidak bisa menyesuaikan bentuk. |

## 5. Rancangan: app ini seharusnya seperti apa

### 5.1 Prinsip
1. **Catat dulu, rapikan nanti.** Transaksi bisa dicatat tanpa produk, tanpa pelanggan, tanpa resep. Struktur lahir dari
   kebiasaan pemakaian, bukan dari formulir awal.
2. **Tiga tingkat, naik bila pengguna mau:**
   - **Tingkat 1 (Catat):** uang masuk dan uang keluar dengan keterangan bebas; hasilnya "uang di tangan" dan "masuk vs
     keluar bulan ini". Cukup untuk warung dan jasa kecil.
   - **Tingkat 2 (Rapikan):** barang dan harga (daftar barang terisi sendiri dari catatan: "kamu sering menjual *keripik*,
     jadikan barang?"), pelanggan dan utang, stok sederhana.
   - **Tingkat 3 (Hitung untung sebenarnya):** modal per barang (HPP), resep, produksi, rencana usaha. Hanya muncul bila
     pengguna memilih "Mau tahu untung sebenarnya?".
3. **Jenis usaha menentukan bentuk awal, bukan membatasi.** Satu pertanyaan sehari-hari saat mulai: "Usahamu seperti
   apa?" dengan kartu bergambar: *Jual makanan/minuman buatan sendiri*, *Jual beli barang*, *Jasa atau sewa*, *Tani/
   ternak/kebun*, *Campuran*. Pilihan mengubah kosakata dan contoh, bisa diganti kapan saja.
4. **Bahasa dan bentuk mengikuti pengguna.** "Barang", "Menu", "Layanan"; "Modal barang" bukan HPP; "Dibuat per
   panci/kali masak" bukan batch.
5. **Satu mesin di dalam** (nota, stok, tagihan, untung dari `MonthlySummary`) tetap sama, sehingga laporan, ekspor, dan sinkron
   tidak berubah. Yang berubah adalah apa yang ditanyakan dan kapan.

### 5.2 Alur pertama kali yang diusulkan (target < 1 menit sampai transaksi pertama)
1. Buka app → "Halo! Usahamu apa?" (kartu bergambar, satu ketukan). Nama usaha diisi dengan satu kotak.
2. Langsung layar **dua tombol besar:** "**Tadi aku jual**" dan "**Tadi aku beli**" (plus "Ada yang belum bayar?").
3. Jual: "Jual apa?" (ketik bebas atau pilih dari yang pernah) → "Berapa?" (stepper) → "Harga" (terisi dari yang terakhir)
   → "Sudah dibayar?" (Lunas/Belum) → selesai. **Tanpa produk dan resep.**
4. Setelah 2–3 catatan, kartu ramah: "Mau tahu untungmu? Isi modal per barang" membuka mode sederhana (satu angka modal per
   barang dulu; resep lengkap hanya bila ingin).
5. Setelah ada utang pelanggan: kartu "Siapa belum bayar" muncul sendiri.

### 5.3 Perubahan konkret pada app sekarang (dari audit ini)

| # | Perubahan | Mengatasi |
|---|---|---|
| R1 | Baris nota bebas (nama + jumlah + harga) dan produk yang lahir otomatis dari catatan | U8, U10, U11, A1 |
| R2 | Beranda kosong: dua tombol besar "Aku jual" / "Aku beli", bukan langkah 1 "Buat produk dan resepnya" | U2, A1 |
| R3 | Editor produk menjadi **bertahap**: nama + harga dulu; satuan, resep, batch di balik "Atur lebih lanjut"; kosakata sesuai jenis | U3, U4, U6 |
| R4 | Setiap tombol Simpan mati menyebut alasannya di tempat ("Isi 'hasil 1 kali masak' dulu") | U4, A5 |
| R5 | Pilihan "Lainnya" untuk satuan menghapus isi awal dan fokus langsung; daftar satuan milik usaha (audit §11 F1) | U5 |
| R6 | Jenis item: dibuat sendiri / barang dagangan / jasa; jasa tanpa stok | U11, U12, A2 |
| R7 | Satu sheet saja saat membuat usaha; kata "unit/cabang" disembunyikan sampai dibutuhkan | U1 |
| R8 | Catatan kosong: tombol "Aku jual / Aku beli" yang jelas; judul tanpa "unit usaha dan buku" | U9 |
| R9 | Jual cepat dan penjualan harian total | U13 |
| R10 | Estimasi modal per barang (satu angka) sebagai jalan tengah menuju untung yang benar | A4, U10 |
| R11 | Dialog "Buang perubahan?" diganti simpan otomatis sebagai draf | U7 |

Detail teknis perubahan model (jenis item, baris bebas, dst.) ada di `docs/audit/2026-11-12_PERSONA_DAN_DESAIN_UMKM.md` §4.

## 6. Skala usaha: kecil sampai besar (jujur tentang batasnya)

| Tingkat | Ciri | Cocok sekarang? | Yang perlu |
|---|---|---|---|
| Mikro (warung, jasa kecil, 1 orang) | 1 pemilik, catat harian | Sebagian; terlalu rumit di awal | Tingkat 1 (Catat) yang sangat sederhana |
| Kecil (produksi rumahan, toko, 1–5 orang) | produk, stok, utang, beberapa pelanggan | Ya untuk produksi; tidak untuk dagang/jasa | Jenis item, baris bebas, jual cepat |
| Menengah (10–50 orang, beberapa cabang) | cabang, beberapa pengguna, laporan rutin | Cabang ada; **pengguna ganda dan hak akses tidak ada** | Multi-pengguna, peran, tutup buku bulanan |
| Besar | akuntansi penuh, pajak (PPN), faktur bernomor, rekening bank, persetujuan, integrasi | **Tidak** | Arsitektur berbeda (server, banyak pengguna, audit trail) |

**Pernyataan jujur:** Mankeu berarsitektur offline, satu pengguna, tanpa server. Itu kekuatan untuk usaha mikro sampai kecil
(privasi, tanpa internet, murah) tetapi **bukan fondasi untuk usaha besar**. Keinginan "juga untuk usaha besar" adalah
keputusan arah produk yang perlu dipilih pemilik, bukan sesuatu yang bisa ditambahkan sebagai fitur:
- Pilihan A: **fokus mikro sampai menengah kecil** dan jadi terbaik di situ (rekomendasi untuk 6–12 bulan ke depan).
- Pilihan B: jalur kedua untuk menengah/besar (multi-pengguna, server opsional). Proyek terpisah dan jauh lebih besar.
Rekomendasi: A dulu; catat B sebagai fase masa depan setelah ada pengguna nyata yang memintanya.

## 7. Aspek teknis (ringkas; rinci di audit menyeluruh)

Rujukan: `docs/audit/2026-11-12_AUDIT_MENYELURUH.md`. Intinya:
- Tes otomatis hijau (unit 148, instrumented 134, lint 0 error, build release lulus) **tetapi** cacat serius lolos
  (izin INTERNET hilang) dan pengalaman pengguna awam gagal. Tes belum mengukur apa yang penting.
- Perlu **tes keterpakaian otomatis** yang meniru pengguna awam: alur "kurang dari 1 menit sampai transaksi pertama",
  "tombol mati menyebut alasan", "tidak ada jalan buntu", kata terlarang di layar, dan jumlah ketukan per tugas.
- Perlu pengukuran jank (`gfxinfo`) otomatis pada alur utama (sheet masih 32,69 % jank di emulator).
- Perlu uji di HP nyata murah (RAM kecil) dengan layar kecil dan pengguna awam sungguhan.
- File besar (`FinanceRepository.kt` 2.129 baris, `MankeuApp.kt` 1.503) dipecah **setelah** model baru stabil.

Temuan teknis baru dari uji ini:
1. Kotak satuan "Lainnya" terisi nilai pilihan sebelumnya (U5).
2. Dialog "Buang perubahan?" muncul untuk dua pemicu berbeda dan sulit ditutup bila terjadi berturut-turut (perlu dicek
   ulang sebelum disimpulkan bug).
3. Tombol "Catat sekarang" di Catatan kosong membuka form yang salah untuk penjual (U9, U10).

**Koreksi atas dokumen sebelumnya:** di `PERSONA_DAN_DESAIN_UMKM.md` saya menulis ada "bug fokus keyboard" saat mengetik satuan.
Setelah diulang dengan benar, itu **artefak skrip uji** (saya mengetik tanpa mengetuk kotaknya). Yang nyata adalah U5
(kotak terisi nilai awal).

## 8. Uji keterpakaian dengan pengguna awam sungguhan (wajib sebelum rilis)

- 6 orang dari bidang berbeda: penjual makanan rumahan, pemilik warung/kelontong, penyedia jasa/sewa, petani/peternak,
  penjahit/bengkel, dan satu orang yang jarang memakai aplikasi (mis. berusia 50+).
- 30 menit, direkam (layar, suara) dengan izin. **Tanpa bantuan.** Tugas: "Catat penjualan pertamamu", "Catat belanja",
  "Cek siapa belum bayar", "Lihat untung bulan ini".
- Catat: detik sampai transaksi pertama, titik berhenti, kata yang tidak dimengerti, apa yang ingin dicatat tetapi tidak bisa.
- Lulus bila **5 dari 6** mencatat transaksi pertama < 2 menit tanpa bantuan. Jika tidak, desain dirombak, bukan dilatih.
- Pemilik yang menemui pengguna; agen menyiapkan skrip, lembar catatan, dan membuat prototipe tiap iterasi.

## 9. Tugas untuk Fable (urutan usulan)

1. **Putuskan arah (dengan pemilik):** Pilihan A atau B (§6). Putuskan juga apakah "catat dulu, rapikan nanti" menjadi
   prinsip utama (rekomendasi: ya).
2. **Susun ulang rencana** agar **perombakan alur awal (R1–R4, R7–R8) dan model (jenis item, baris bebas)** didahulukan
   dari Paket C, D, F, G, sebelum schema 14 dan template v5 dirilis. Paket F (titip/tempo) dan G (template) mengikuti
   model baru.
3. **Rinci Tingkat 1–3** sebagai spesifikasi layar dengan kosakata awam per jenis usaha.
4. **Rencana uji keterpakaian** (§8) sebagai paket tersendiri di tengah pembangunan, bukan di akhir: dua putaran
   (prototipe kasar lalu build) dengan perbaikan sesudahnya.
5. Tulis **tes otomatis keterpakaian** (§7) dan masukkan ke matriks verifikasi.
6. Tetap jalankan hotfix INTERNET (`main` terdampak), review B dan N, serta perbaikan teknis dari audit menyeluruh.
7. Hitung risiko jadwal: perubahan ini lebih besar dari sisa Fase 2; mungkin perlu memecah rilis internal menjadi
   "Mankeu Lite" (Tingkat 1–2) dulu dan Tingkat 3 sesudahnya.
