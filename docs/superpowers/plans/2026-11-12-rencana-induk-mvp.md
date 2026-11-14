# Rencana induk menuju MVP Mankeu (2026-11-12)

> **Prioritas pemilik 2026-11-15:** selesaikan fitur dan alur fungsional MVP terlebih dahulu; pemolesan UI/UX menyusul, lalu penyempurnaan multi-skala. Tes penerimaan fungsional tetap dijalankan sesuai perubahan (DEC-052).


> **Pembaruan pemilik 2026-11-13 12:22 WIB:** audit konsistensi seluruh aplikasi dan aturan validasi baru mengikuti `2026-11-13-konsistensi-antarmuka.md` dan DEC-042. Keputusan baru menggantikan waktu validasi lama PK-0.7/DEC-034/040; riwayat paket dan hasil tes lama tetap historis. Pekerjaan finansial dan gerbang MVP pada rencana ini tetap berlaku.


> **Kedudukan.** Berkas ini menggantikan urutan paket A sampai O di `2026-11-09-rencana-lanjutan-lengkap.md`.
> Rencana lama tetap dibaca untuk §0.3 (lingkungan), §5 (register R1 sampai R22), dan isi rinci paket F, J, K, L, M.
> Bila berkas ini bertentangan dengan spec (`../specs/2026-10-29-redesign-spec.md`, V-1 sampai V-6), spec yang menang.
>
> **Penyusun:** Fable 5.2, hanya membaca kode dan menulis rencana. Tidak ada kode produksi yang diubah.
>
> **Pembaca:** pemilik (bagian 1 sampai 4 dan 8) dan agen pelaksana (bagian 5 sampai 7 dan lampiran).

Lampiran:

| Berkas | Isi |
|---|---|
| `2026-11-12-lampiran-a-spek-layar.md` | Spek layar alur awam, kosakata per jenis usaha, pesan tombol mati |
| `2026-11-12-lampiran-b-paket-kerja.md` | Paket kerja kecil, satu sesi agen per paket |
| `2026-11-12-lampiran-c-rencana-uji.md` | Uji otomatis keterpakaian, skenario persona, uji pengguna, uji praktisi, uji HP murah |
| `2026-11-12-lampiran-d-prompt-eksekusi.md` | Prompt siap salin per tahap |

Tanda status fakta di seluruh berkas: **[kode]** dibaca penyusun di HEAD `7fe55ee`; **[audit]** dilaporkan tiga audit
2026-11-12 dan tidak diulang; **[belum]** belum diverifikasi siapa pun.

## 1. Ringkasan untuk pemilik

Mesin hitung Mankeu sudah kuat: HPP per batch, nota, tagihan, satu angka untung. Yang gagal adalah urutannya. App
meminta produk, resep, dan satuan sebelum pengguna boleh mencatat satu rupiah, dan empat dari sepuluh persona tidak
punya jalur yang wajar [audit]. Rencana ini membalik urutan itu dalam enam tahap:

| Tahap | Isi | Hasil yang bisa dilihat pemilik |
|---|---|---|
| 0 Pengaman | Hotfix izin INTERNET, review Paket B dan N, alat ukur keterpakaian | `main` bisa cadangan Drive; aturan uang Paket B ditinjau orang kedua |
| 1 Model | Jenis barang, baris nota bebas, modal satu angka, dua cara hitung untung | Belum ada layar baru; tes persona hijau di tingkat data |
| 2 Pintu A | Layar pertama, "Aku jual", "Aku beli", Beranda, siapa belum bayar | Instal bersih sampai penjualan pertama di bawah satu menit |
| 3 Pintu B | Kalkulator resep dan kelayakan usaha | Resep nyata dihitung tanpa satu transaksi pun |
| 4 Excel dan data aman | Template v5 lengkap, mode impor, pindah HP | Ekspor, ubah, impor lagi tanpa kehilangan dan tanpa ganda |
| 5 Uji dan rilis | Bahasa, uji ulang, uji pengguna putaran 2, merge, rilis internal | Kriteria lulus MVP terukur |

Sesudah MVP: titip jual dan belanja tempo (layar), jual cepat, laporan per bulan, kunci app, notifikasi, jaminan,
retur, cara bayar, siklus tani, pemecahan berkas besar. Tidak ada kapabilitas yang dibuang (V-4).

Tiga hal yang perlu pemilik ketahui sebelum membaca lebih jauh:

1. **Ada 15 keputusan yang menunggu pemilik** (bagian 4). Empat di antaranya mengubah schema, jadi harus dijawab
   sebelum Tahap 1 dimulai: K-1, K-2, K-3, K-4. Tahap 0 dan dua paket hitung murni bisa jalan tanpa jawaban.
2. **Temuan logika baru dari penyusunan rencana ini:** "catat dulu" tidak bisa memakai rumus untung yang sekarang.
   Rumus sekarang tidak mengurangkan belanja bahan dari untung karena diwakili modal barang terjual (F2-8). Pengguna
   yang belum mengisi modal barang akan melihat untung sebesar seluruh penjualannya. Usulannya di K-1.
3. **Ukuran pekerjaan lebih besar dari sisa Fase 2 lama.** Perkiraan 45 paket kecil. Perkiraan ini belum diuji;
   paket pertama tiap tahap akan mengoreksinya.

## 2. Mengapa urutannya begini

1. **Tahap 0 lebih dulu karena tidak bergantung pada keputusan apa pun** dan menutup dua lubang yang sudah
   diketahui: `main` tanpa izin INTERNET [kode: manifest `main` tidak memuat INTERNET, branch memuatnya], dan
   aturan uang Paket B yang belum pernah ditinjau orang kedua [audit T2].
2. **Model sebelum layar.** Layar "Aku jual" butuh baris nota tanpa produk. Sekarang `SaleLineEntity.productId`
   wajib dan `saveSale` menolak nota tanpa baris produk [kode]. Membangun layar di atas model lama berarti membangun
   dua kali.
3. **Model sekarang, bukan nanti.** Schema 14 dan template v5 belum pernah dirilis [kode: `version = 14`,
   `MankeuTemplate.VERSION = 5`; rilis terakhir dari `main` schema 13]. Kolom baru cukup ditambahkan ke
   `Migration13To14`, `14.json`, dan `BackupUpgrade`. Setelah rilis internal, setiap kolom berarti schema 15 dengan
   migrasi, BackupUpgrade, dan lantai sinkron baru.
4. **Pintu A sebelum Pintu B** karena Pintu A memuat risiko terbesar (model, alur pertama) dan paling lama diuji
   pengguna. Dua paket hitung Pintu B (PK-3.1 dan PK-3.4) murni domain dan boleh dikerjakan kapan saja, termasuk
   saat menunggu jawaban pemilik.
5. **Uji pengguna putaran 1 di tengah (akhir Tahap 2), bukan di akhir.** Audit menegaskan bahwa bila pengguna awam
   gagal, desain yang dirombak, bukan penggunanya yang dilatih. Merombak di tengah lebih murah.
6. **Template Excel sesudah model dan layar stabil** supaya kolomnya ditulis sekali. Template tetap sebelum merge
   karena DEC-012 mensyaratkan ekspor yang bisa diimpor ulang tanpa kehilangan.
7. **Merge dan rilis paling akhir.** Begitu dirilis, schema 14 membeku.

Peta paket lama ke rencana ini:

| Paket lama | Nasib |
|---|---|
| A, N, B | Selesai. B dan N mendapat review independen di Tahap 0 |
| O (fleksibilitas) | Dipecah ke Tahap 1 (model) dan PK-2.9 (satuan milik usaha) |
| C (Rencana tanpa periode) | Menjadi kelayakan usaha, PK-3.4 dan PK-3.5 |
| D (bahasa dan bantuan) | Sebagian masuk tiap layar baru; sisanya PK-5.1 dan PK-5.2 |
| G (template) | Tahap 4, diperluas dengan kolom model baru dan mode impor |
| E, H, I | PK-5.3, PK-5.6, PK-5.7 |
| F (titip, tempo), J, K, L, M | Sesudah MVP, isi rincinya tetap dari rencana lama (lihat K-9) |

## 3. Rancangan inti yang mengikat semua paket

### 3.1 Satu mesin, tiga jenis barang

`products` mendapat `kind`: `MADE` (dibuat sendiri), `TRADED` (barang dagangan), `SERVICE` (jasa atau sewa), dan
`tracksStock` (hitung sisa barang atau tidak). Bawaan: MADE dan TRADED menghitung stok, SERVICE tidak. Warung makan
memakai MADE tanpa stok. Semua jenis lewat nota, tagihan, dan `MonthlySummary` yang sama.

Modal per satuan dasar diputuskan satu fungsi (`ProductCosting.unitCost`), dengan urutan:

1. ada resep: biaya resep dibagi hasil bersih (hasil dikurangi susut);
2. TRADED yang pernah dibeli: harga beli terakhir per satuan dasar (K-8);
3. ada "modal per satuan" yang diketik pengguna: angka itu;
4. selain itu: belum diketahui.

### 3.2 Baris nota bebas dan "jadikan barang"

Baris nota boleh tanpa produk: nama, jumlah, harga. Baris bebas tidak menyentuh stok. Nama yang pernah diketik muncul
sebagai saran berikut harga terakhirnya. Setelah nama yang sama dipakai dua kali, app menawarkan "Simpan sebagai
barang" (K-5). Penjualan lama bernama sama ikut ditautkan ke barang itu.

"Jualan hari ini" (total sehari tanpa rincian) adalah baris bebas dengan nama itu. Tidak perlu mesin terpisah.

### 3.3 Dua cara hitung untung (usulan, menunggu K-1)

| Cara | Rumus "untung bulan ini" | Untuk siapa |
|---|---|---|
| Sederhana | uang jualan dikurangi semua belanja usaha bulan itu | bawaan pengguna baru |
| Modal barang | jualan dikurangi modal barang terjual dikurangi biaya lain (F2-8, seperti sekarang) | pengguna yang sudah mengisi modal semua barang yang dijualnya |

Cara disimpan per usaha dan dibaca `ProfitCalculator`, jadi Beranda, Laporan, PDF, dan Excel tetap satu angka.
Pindah ke "Modal barang" ditawarkan app saat syaratnya terpenuhi, dengan penjelasan bahwa angka bulan-bulan lalu ikut
dihitung ulang. Usaha yang sudah ada (data keripik) tetap "Modal barang".

Pada cara "Modal barang", penjualan barang yang modalnya belum diketahui ditandai (`costKnown = 0`) dan Beranda
menyebut jumlahnya dengan tombol untuk mengisinya. Tidak ada angka yang diam-diam terlalu besar.

### 3.4 Jenis usaha hanya mengatur bentuk

Layar pertama bertanya "Usahamu seperti apa?" (K-6). Jawabannya memilih kosakata, contoh isian, dan jenis barang
bawaan. Logika hitung tidak bercabang menurut jenis usaha. Bisa diganti kapan saja di menu Usaha.

### 3.5 Yang tidak berubah

- Uang `Long` rupiah. Pembulatan setengah ke atas, satu kali, di akhir.
- Kuantitas lewat `TransactionCalculation.parseQuantity`; uang ketikan lewat `sanitizeRupiah`.
- Semua angka ringkasan dari `MonthlySummary`. Layar tidak menghitung sendiri.
- Format Excel milik Mankeu, ketat, lossless (DEC-012).
- Tanpa pinjaman, bunga, cicilan, pajak, multi-pengguna (V-1, spec #2).

## 4. Keputusan untuk pemilik

Cara menjawab: tulis huruf pilihan di kolom "Jawaban" tabel 4.16, atau jawab di obrolan dan agen yang mencatatnya.
Paket yang bergantung pada sebuah keputusan menuliskan "Butuh K-n" dan **tidak dikerjakan sebelum dijawab**.
"Batas jawab" adalah titik terakhir sebelum jawaban menjadi mahal.

### K-1. Untung bulan ini bagi pengguna yang belum mengisi modal barang

Latar: lihat 3.3. Tanpa keputusan ini, pengguna baru yang menjual Rp30.000 dan belanja bahan Rp100.000 melihat untung
Rp30.000.

- **A (rekomendasi).** Dua cara hitung per usaha: "Sederhana" sebagai bawaan, "Modal barang" ditawarkan saat siap.
  Akibat: angka pengguna baru jujur sejak hari pertama; satu kolom baru di `businesses`; angka bulan lalu berubah
  saat pindah cara (dijelaskan di layar).
- B. Selalu "Modal barang" seperti sekarang, dengan peringatan "untung belum dikurangi modal barang". Akibat: tanpa
  kolom baru; angka pertama yang dilihat pengguna awam salah dan hanya dikoreksi oleh teks peringatan.
- C. Sembunyikan untung sampai modal diisi. Akibat: jujur, tetapi Pintu A kehilangan jawaban "untungku bulan ini?",
  yang merupakan syarat lulus MVP.

Batas jawab: sebelum PK-1.2.

### K-2. Cara bayar

- A. Tetap satu kas tanpa pilihan. Akibat: tanpa perubahan; transfer dan QRIS tercatat sebagai uang di tangan.
- **B (rekomendasi).** Label "Dibayar lewat: Tunai / Transfer / Lainnya" pada tiap uang masuk dan keluar, tanpa
  memecah saldo. Kolomnya disiapkan sekarang, layarnya sesudah MVP. Akibat: satu kolom nullable; nanti bisa
  menjawab "berapa yang masuk lewat transfer" tanpa schema 15.
- C. Beberapa dompet dengan saldo masing-masing. Akibat: mendekati pembukuan bank, berlawanan dengan V-1.

Batas jawab: sebelum PK-1.2.

### K-3. Status pesanan untuk usaha pesanan (penjahit, katering)

- **A (rekomendasi).** Tunda. Pesanan dicatat sebagai nota dengan uang muka; status dan tanggal janji ditulis di
  catatan nota. Akibat: tanpa perubahan schema; bila nanti dibangun sesudah rilis, butuh schema 15.
- B. Siapkan kolom sekarang (`status pesanan`, `tanggal janji` pada nota), layar sesudah MVP. Akibat: dua kolom
  nullable yang mungkin tidak pernah dipakai.
- C. Bangun di MVP. Akibat: menambah kira-kira tiga paket ke Tahap 2.

Batas jawab: sebelum PK-1.2.

### K-4. Harga bahan "per kemasan"

Latar: pembuat makanan berkata "telur Rp28.000 serak isi 30, kupakai 4". Sekarang harga bahan disimpan per satu
satuan dalam rupiah bulat (Rp933 per butir), sehingga 4 butir terhitung Rp3.732, padahal hitungan tangan Rp3.733.
Selisih kecil, tetapi syarat lulus MVP adalah angka resep cocok dengan hitungan manual praktisi.

- **A (rekomendasi).** Simpan "harga untuk berapa" pada bahan (Rp28.000 untuk 30 butir) dan hitung sekali di akhir.
  Akibat: satu kolom baru di `stock_items` dan satu kolom di lembar Stok Barang; hitungan tepat sama dengan tangan.
- B. Biarkan; terima selisih paling banyak setengah rupiah kali jumlah pakai per bahan. Akibat: tanpa perubahan;
  risiko uji praktisi menemukan selisih yang harus dijelaskan.

Batas jawab: sebelum PK-1.2.

### K-5. Nama yang diketik saat "Aku jual"

- **A (rekomendasi).** Tetap catatan bebas. Setelah nama yang sama muncul dua kali, tawarkan "Simpan sebagai
  barang". Akibat: salah ketik tidak menjadi barang; daftar barang hanya berisi yang disetujui pengguna.
- B. Setiap nama baru langsung menjadi barang. Akibat: daftar cepat terisi, juga oleh salah ketik ("kripik",
  "keripik"); perlu fitur gabung barang.

Batas jawab: sebelum PK-1.5.

### K-6. Pertanyaan jenis usaha di layar pertama

- **A (rekomendasi).** Wajib satu ketukan dari lima kartu: buat sendiri, jual beli, jasa atau sewa, tani atau
  ternak, campuran. "Campuran" untuk yang ragu. Akibat: satu ketukan tambahan; kosakata dan contoh langsung cocok.
- B. Boleh dilewati. Akibat: yang melewati mendapat kosakata umum, yang menurut audit paling tidak membantu.
- C. Tidak ditanyakan; pengguna memilih jenis per barang. Akibat: layar pertama lebih pendek; kosakata tidak bisa
  menyesuaikan.

Batas jawab: sebelum PK-2.1.

### K-7. Dialog "Buang perubahan?"

- **A (rekomendasi).** Ganti dengan tiga pilihan jelas: "Simpan", "Lanjut isi", "Hapus isian". Bila belum bisa
  disimpan, dialog menyebut apa yang kurang. Akibat: murah, satu komponen.
- B. Simpan otomatis sebagai draf yang bisa dilanjutkan. Akibat: tidak ada dialog; butuh penyimpanan draf per form,
  kira-kira tiga paket.

Batas jawab: sebelum PK-2.2.

### K-8. Modal barang dagangan

- **A (rekomendasi).** Harga beli terakhir, sama dengan aturan bahan sekarang (spec #20). Akibat: mudah dijelaskan
  ("modalnya ikut harga belanja terakhir"); untung bergeser saat harga beli berubah.
- B. Rata-rata harga beli. Akibat: lebih halus; sulit dicocokkan pengguna dengan hitungan tangan.

Batas jawab: sebelum PK-1.4.

### K-9. Titip jual dan belanja tempo (layar)

Latar: logikanya sudah ada di kode, layarnya belum. MVP menurut V-5 tidak menyebut keduanya.

- **A (rekomendasi).** Sesudah MVP. Kartu "Utang ke pemasok" di Beranda disembunyikan sampai ada datanya.
  Template Excel tetap membawa lembar Titip dan kolom tempo sejak sekarang. Akibat: MVP lebih cepat; Bu Sri
  (keripik) belum bisa mencatat titip dari layar.
- B. Masuk MVP (Paket F lama). Akibat: menambah kira-kira empat paket sebelum rilis.

Batas jawab: sebelum Tahap 5.

### K-10. Retur penjualan

- **A (rekomendasi).** Tunda. Sementara, nota diubah atau dihapus. Akibat: tanpa perubahan; retur sebagian sesudah
  pembayaran butuh dua langkah manual.
- B. Retur sederhana sesudah MVP: tombol "Barang dikembalikan" pada nota, yang mengurangi tagihan atau mengembalikan
  uang dan mengembalikan stok. Akibat: tidak butuh kolom baru (baris nota bernilai minus), bisa dibangun kapan saja.
- C. Masuk MVP. Akibat: menambah kira-kira tiga paket dan golden test baru.

Batas jawab: tidak mendesak (tanpa dampak schema).

### K-11. Uang jaminan (rental) dan uang titipan

- **A (rekomendasi).** Sesudah MVP, sebagai jenis catatan baru "Uang titipan" yang bukan pendapatan dan muncul di
  "harus dikembalikan". Akibat: tidak butuh kolom baru (jenis catatan disimpan sebagai teks); sampai dibangun,
  Pak Rudi (rental) mencatat jaminan di catatan nota saja.
- B. Masuk MVP. Akibat: menambah kira-kira tiga paket.
- C. Tidak dibangun. Akibat: persona rental tetap tidak terlayani.

Batas jawab: tidak mendesak.

### K-12. Siklus tanam atau proyek

Latar: unit usaha di kode sudah punya nama, keterangan, tanggal mulai, dan tanggal selesai [kode: `ProjectEntity`].

- **A (rekomendasi).** Sesudah MVP, unit usaha ditampilkan sebagai "Musim tanam" atau "Proyek" untuk jenis usaha
  tani, dengan untung per musim. Akibat: tanpa tabel baru; di MVP petani mencatat harian seperti yang lain.
- B. Tabel siklus tersendiri. Akibat: konsep kedua yang mirip unit usaha; butuh schema.

Batas jawab: tidak mendesak bila A.

### K-13. Mode impor Excel

Latar: V-6. Impor sekarang hanya menambah baris, jadi ekspor lalu ubah lalu impor menggandakan catatan.

- **A (rekomendasi).** Pratinjau menawarkan dua tombol: "Tambahkan ke catatan yang ada" dan "Ganti isi usaha ini
  dengan isi file". "Ganti" menyimpan cadangan otomatis lebih dulu dan bisa dibatalkan; pembatalan mengembalikan
  seluruh data ke keadaan sebelum impor (catatan yang dibuat sesudahnya ikut hilang, dengan peringatan). Bila file
  tampak berasal dari ekspor usaha ini, "Ganti" yang disorot. Akibat: alur (b) V-6 berjalan; DEC-012 perlu
  amandemen karena "impor hanya menambah" tidak lagi mutlak.
- B. Hanya "Tambahkan", dengan peringatan duplikat yang lebih keras. Akibat: tanpa perubahan aturan; alur (b)
  tetap menggandakan data.
- C. Cocokkan per baris lewat kolom ID tersembunyi (ubah baris yang sama, tambah yang baru). Akibat: paling halus;
  paling rumit, dan baris yang dihapus pengguna di Excel tidak terhapus di app.

Batas jawab: sebelum PK-4.5.

### K-14. Kapan disebut "layak"

- **A (rekomendasi).** "Layak" bila biaya rutin terisi dan untung per bulan lebih dari nol; lama balik modal selalu
  ditampilkan di sebelahnya. Bila biaya rutin kosong: "Belum bisa dinilai, isi biaya rutin dulu" dengan tombol.
  Akibat: sederhana, sesuai spec #14.
- B. "Layak" hanya bila modal kembali dalam 24 bulan. Akibat: lebih hati-hati; angka 24 bulan adalah pilihan
  penyusun tanpa dasar lapangan.

Batas jawab: sebelum PK-3.4.

### K-15. Jadwal merge, rilis, dan hotfix

Tiga pertanyaan pendek:

- **K-15a Hotfix INTERNET ke `main`.** **A (rekomendasi):** sekarang, satu commit kecil di `main` berisi manifest
  dan tesnya. B: menunggu merge MVP; build dari `main` tetap tidak bisa cadangan ke Drive sampai saat itu.
- **K-15b Bentuk rilis internal.** **A (rekomendasi):** satu rilis MVP sesudah Tahap 5. B: rilis "Lite" sesudah
  Tahap 2 (Pintu A saja); schema 14 membeku lebih awal, sehingga kolom template dan Pintu B sesudahnya berisiko
  schema 15.
- **K-15c Uji pengguna awam.** **A (rekomendasi):** dua putaran, enam orang, pemilik yang menemui (sesudah Tahap 2
  dan di Tahap 5). B: satu putaran di akhir; lebih hemat waktu pemilik, tetapi kegagalan baru ketahuan saat
  merombak paling mahal.

### 4.16 Tabel jawaban

| Keputusan | Rekomendasi | Jawaban pemilik | Tanggal |
|---|---|---|---|
| K-1 cara hitung untung | A | **A** (spec V-7) | 2026-11-12 |
| K-2 cara bayar | B | **B** (spec V-8) | 2026-11-12 |
| K-3 status pesanan | A | **A** (spec V-9) | 2026-11-12 |
| K-4 harga per kemasan | A | **A** (spec V-10) | 2026-11-12 |
| K-5 nama ketikan | A | | |
| K-6 jenis usaha | A | | |
| K-7 dialog buang | A | | |
| K-8 modal barang dagangan | A | | |
| K-9 titip dan tempo | A | | |
| K-10 retur | A | | |
| K-11 jaminan | A | | |
| K-12 siklus | A | | |
| K-13 mode impor | A | | |
| K-14 layak | A | | |
| K-15a hotfix | A | **A** | 2026-11-12 |
| K-15b bentuk rilis | A | | |
| K-15c uji pengguna | A | | |

Setelah dijawab, agen menyalin jawabannya ke spec sebagai V-7 dan seterusnya, dan menulis DEC bila menyentuh arsitektur.

## 5. Tahap dan paket

Rincian tiap paket ada di Lampiran B. Ukuran: K (kecil, sampai kira-kira 150 baris diff), S (sedang, sampai 400),
B (besar, harus dipecah bila ternyata lebih). Semua ukuran adalah perkiraan penyusun [belum].

### Tahap 0: pengaman (tanpa keputusan pemilik, kecuali PK-0.2)

| Paket | Isi | Ukuran | Butuh |
|---|---|---|---|
| PK-0.1 | Tes manifest: izin INTERNET wajib ada | K | |
| PK-0.2 | Hotfix INTERNET ke `main` | K | K-15a, PK-0.1 |
| PK-0.3 | Review independen Paket B (uang) | S, hanya baca | |
| PK-0.4 | Review independen Paket N (jank), ukur ulang | S, hanya baca | |
| PK-0.5 | Perbaikan temuan PK-0.3 dan PK-0.4 | S | PK-0.3, PK-0.4 |
| PK-0.6 | Tes kata terlarang dengan daftar dasar yang hanya boleh menyusut | K | |
| PK-0.7 | Tombol mati wajib beralasan (`FullPageForm`, `MankeuSheet`) | S | |
| PK-0.8 | Alat hitung ketukan dan tes "tak ada jalan buntu" (kerangka) | K | |

Selesai bila: `main` memuat INTERNET (bila K-15a = A); dua review tertulis dan temuan Critical/Important beres;
tiga tes keterpakaian berjalan di suite.

### Tahap 1: model (butuh K-1 sampai K-4)

| Paket | Isi | Ukuran | Butuh |
|---|---|---|---|
| PK-1.1 | Domain: jenis barang dan `unitCost` | K | |
| PK-1.2 | Schema 14 diubah di tempat: semua kolom baru, migrasi, backup, sinkron | S | K-1..K-4, PK-1.1 |
| PK-1.3 | Baris nota bebas di repository dan untung | S | PK-1.2 |
| PK-1.4 | Stok menurut jenis; belanja barang dagangan | S | PK-1.2, K-8 |
| PK-1.5 | "Simpan sebagai barang" dan nama mirip | S | PK-1.3, K-5 |
| PK-1.6 | Dua cara hitung untung | S | PK-1.3, K-1 |
| PK-1.7 | Skenario persona sebagai tes (tingkat data) | S | PK-1.3..1.6 |
| PK-1.8 | Review independen Tahap 1 | hanya baca | PK-1.7 |
| PK-1.9 | Perbaikan temuan PK-1.8 | S | PK-1.8 |

Selesai bila: `PersonaScenarioTest` hijau untuk sepuluh persona (bagian yang menunggu keputusan diabaikan dengan
nomor keputusannya, Lampiran C §2); golden lama tidak berubah; migrasi
13 ke 14 dan BackupUpgrade lulus; review tertulis.

### Tahap 2: Pintu A, catat harian (butuh K-5, K-6, K-7)

| Paket | Isi | Ukuran | Butuh |
|---|---|---|---|
| PK-2.1 | Layar pertama: jenis usaha dan nama, satu halaman | S | K-6, PK-1.2 |
| PK-2.2 | Kosakata per jenis usaha; dialog keluar form | S | K-7, PK-2.1 |
| PK-2.3 | Beranda kosong dan Beranda berisi | S | PK-2.2, PK-1.6 |
| PK-2.4 | "Aku jual" | S | PK-1.3, PK-2.2 |
| PK-2.5 | "Aku beli" | S | PK-1.4, PK-2.2 |
| PK-2.6 | Siapa belum bayar dan terima bayar: uji alur awam, pelanggan baru di tempat | K | PK-2.4 |
| PK-2.7 | Catatan kosong dan tombol tambah | K | PK-2.4, PK-2.5 |
| PK-2.8 | Editor barang bertahap menurut jenis | S | PK-1.4, PK-2.2 |
| PK-2.9 | Satuan milik usaha | S | PK-2.8 |
| PK-2.10 | Kartu "Isi modal barang" dan pindah cara hitung | S | PK-1.6, PK-2.3 |
| PK-2.11 | Tes perjalanan pertama: ketukan dan jalan buntu | K | PK-2.3..2.7 |
| PK-2.12 | Review independen Tahap 2 dan uji tangan U1 sampai U14 ulang | hanya baca dan emulator | PK-2.11 |
| PK-2.13 | Perbaikan temuan PK-2.12 | S | PK-2.12 |
| PK-2.14 | Uji pengguna putaran 1 (pemilik) dan paket perbaikannya | pemilik, lalu S | PK-2.13, K-15c |

Selesai bila: dari instal bersih, penjualan pertama tersimpan dalam paling banyak 8 ketukan dan 3 isian diketik
(tes); 14 skenario audit diulang dan tidak ada yang gagal pada cek-lis 2, 4, 5; hasil putaran 1 tercatat.

### Tahap 3: Pintu B, hitung (butuh K-4, K-14)

| Paket | Isi | Ukuran | Butuh |
|---|---|---|---|
| PK-3.1 | Domain kalkulator resep: susut, saran harga, "kalau naik", konversi satuan | S | |
| PK-3.2 | Harga bahan per kemasan di semua jalur hitung | S | K-4 = A, PK-1.2 |
| PK-3.3 | Layar kalkulator resep | S | PK-3.1, PK-2.8 |
| PK-3.4 | Domain kelayakan usaha | K | K-14 |
| PK-3.5 | Layar kelayakan usaha (menggantikan Rencana berperiode) | S | PK-3.4, PK-2.2 |
| PK-3.6 | Review independen Tahap 3 | hanya baca | PK-3.5 |
| PK-3.7 | Perbaikan temuan PK-3.6 | S | PK-3.6 |
| PK-3.8 | Uji praktisi: satu resep nyata (pemilik) | pemilik | PK-3.7 |

Selesai bila: golden kalkulator dan kelayakan hijau; resep bisa dihitung di instal bersih tanpa transaksi; barang
hasil kalkulator langsung punya modal di "Aku jual"; lembar uji praktisi terisi dan angkanya cocok.

### Tahap 4: Excel dan data aman (butuh K-13)

| Paket | Isi | Ukuran | Butuh |
|---|---|---|---|
| PK-4.1 | Kontrak template v5 final dan ekspor: Kontak, Nota, Baris Nota, Titip, kolom model baru | S | Tahap 1 selesai |
| PK-4.2 | Impor lembar baru dengan pesan kesalahan per sel | S | PK-4.1 |
| PK-4.3 | Kategori dibaca kembali; pembayaran dan tempo di Transaksi | S | PK-4.2 |
| PK-4.4 | Round trip golden semua persona | S | PK-4.3 |
| PK-4.5 | Mode impor "Tambahkan" dan "Ganti" | S | K-13, PK-4.4 |
| PK-4.6 | Template ramah awam: Petunjuk, lembar Contoh, tombol unduh yang terlihat | S | PK-4.1 |
| PK-4.7 | Cadangan dan pindah HP: alur satu tombol dan uji pulih di instal bersih | S | |
| PK-4.8 | Review independen Tahap 4 | hanya baca | PK-4.7 |
| PK-4.9 | Perbaikan temuan PK-4.8 | S | PK-4.8 |

Selesai bila: ekspor lalu impor ke usaha kosong menghasilkan data identik (tes); file rusak ditolak dengan lembar,
baris, kolom, dan cara memperbaikinya; pulih dari file cadangan di instal bersih mengembalikan angka Beranda.

### Tahap 5: rapikan, uji, rilis

| Paket | Isi | Ukuran | Butuh |
|---|---|---|---|
| PK-5.1 | Bahasa awam di layar lama (sisa Paket D): daftar dasar kata terlarang menjadi kosong | S | |
| PK-5.2 | Bantuan: artikel yang salah ditulis ulang untuk alur baru | S | Tahap 2 dan 3 |
| PK-5.3 | Uji ulang seluruh app di emulator dan jank | emulator | PK-5.1 |
| PK-5.4 | Uji di HP nyata murah (pemilik, dengan cek-lis) | pemilik | PK-5.3 |
| PK-5.5 | Uji pengguna putaran 2 (pemilik) dan perbaikannya | pemilik, lalu S | PK-5.3, K-15c |
| PK-5.6 | Review akhir branch, merge ke `main` | hanya baca, lalu K | semua di atas |
| PK-5.7 | Persiapan rilis internal: versi, catatan rilis, AAB oleh pemilik | K | PK-5.6 |

Selesai bila: kriteria lulus MVP (V-5) terpenuhi dan tercatat; matriks verifikasi (Lampiran C §6) hijau.

### Sesudah MVP (urutan usulan, dirinci saat dimulai)

1. Titip jual dan belanja tempo, layar (Paket F lama), bila K-9 = A.
2. Jual cepat: barang favorit sebagai tombol besar (D4).
3. Laporan per bulan dan grafik `Canvas`, Vico dibuang (Paket J lama).
4. Kunci app dan notifikasi jatuh tempo (Paket K lama).
5. Layar cara bayar (K-2), jaminan (K-11), retur (K-10), musim tanam (K-12), status pesanan (K-3), potongan
   marketplace (D11), bahan terbuang (D13).
6. Sisa sinkron (Paket L lama), bantuan lengkap (Paket M lama).
7. Pemecahan `FinanceRepository.kt`, `MankeuApp.kt`, `AppViewModel.kt`.

Di luar rencana dan diputuskan terpisah: impor pintar (V-6), jalur usaha menengah dan besar (V-1).

## 6. Register risiko

| # | Risiko | Akibat bila terjadi | Penangkal |
|---|---|---|---|
| X1 | `main` tanpa izin INTERNET [kode] | Cadangan dan sinkron Drive gagal di HP nyata | PK-0.1, PK-0.2 |
| X2 | Aturan "pembayaran hari yang sama diserap" (Paket B) belum ditinjau [audit T2] | Uang harian dihitung dua kali atau hilang dari daftar | PK-0.3 |
| X3 | PK-0.4: 12 sampel emulator 57,14–96,67 % janky; akar tingkat fungsi belum terisolasi, HP nyata belum diuji | Ambang 15 % belum terpenuhi; klaim stabil belum dapat diterima | PK-0.5 membetulkan klaim bukti; method/system trace dan PK-5.3, PK-5.4 |
| X4 | Tes hijau meloloskan cacat (sudah terjadi: INTERNET, alur awam) | Rasa aman palsu | Tiap paket punya "verifikasi nyata" di emulator; review independen tiap tahap |
| X5 | Baris nota tanpa produk mengubah asumsi di banyak tempat (stok, ekspor, sinkron, migrasi lama) | Crash atau angka salah pada nota campuran | PK-1.3 mencari semua pemakai `productId`; golden lama harus tetap sama |
| X6 | Dua cara hitung untung membingungkan atau dipakai tidak konsisten | Angka Beranda berbeda dari Laporan | Cara dibaca di satu tempat (`ProfitInputs`); `OneNumberTest` dijalankan untuk kedua cara |
| X7 | Pindah cara hitung mengubah angka bulan lalu | Pengguna mengira data rusak | Penjelasan di layar sebelum pindah; uji pengguna putaran 2 menanyakan ini |
| X8 | Pengguna mencatat belanja bahan sebagai "keperluan lain" pada cara Modal barang | Modal terhitung dua kali, untung terlalu kecil | "Aku beli" menyarankan dari daftar bahan dan barang; baris selisih F2-8 tetap tampil di Laporan |
| X9 | Schema 14 terlanjur dirilis sebelum kolom lengkap | Schema 15, migrasi, BackupUpgrade, lantai sinkron baru | Rilis hanya di PK-5.7; K-1 sampai K-4 dijawab sebelum PK-1.2 |
| X10 | Mode "Ganti" menghapus data yang tidak ada di file | Kehilangan data | Cadangan otomatis sebelum ganti; pratinjau menyebut apa yang hilang; tes pulih |
| X11 | Persona rekaan, bukan pengguna nyata [audit] | Desain cocok di kertas, gagal di lapangan | Dua putaran uji pengguna; hasilnya boleh mengubah rencana ini |
| X12 | Paket terlalu besar untuk agen hemat | Sesi habis di tengah, pekerjaan setengah jadi | Tiap paket punya titik simpan; aturan "commit yang hijau lalu tulis Status" |
| X13 | Agen pelaksana memutuskan arah produk sendiri | Pekerjaan dibuang | Paket bertanda "Butuh K-n" dilarang mulai tanpa jawaban |
| X14 | Kosakata per jenis usaha menggandakan jumlah string | Terjemahan dan tes bahasa membengkak | Hanya kira-kira 12 kata yang berbeda per jenis (Lampiran A §2); sisanya netral |
| X15 | Uji hanya di emulator [audit T4] | Masalah keyboard, memori, WhatsApp, Drive baru muncul di pengguna | PK-5.4 dengan cek-lis HP nyata |
| X16 | Berkas besar (`FinanceRepository.kt` 2.129 baris) menghabiskan konteks agen [kode] | Agen membaca ulang ribuan baris tiap paket | Paket menyebut fungsi dan rentang baris; pemecahan berkas sesudah MVP |
| X17 | Bantuan dan panduan menjelaskan app lama [audit T6] | Pengguna dibimbing ke layar yang tidak ada | PK-5.2; sampai saat itu tombol bantuan di layar baru tidak menaut artikel lama |
| X18 | Pemilik merasa banyak ruang kosong; audit 2026-11-12 mengukur celah form pendek 202–413 dp serta ruang pesan tersembunyi dan daftar kosong | Hubungan isian dan aksi dapat terasa jauh [inferred]; perubahan sembarang berisiko merusak jangkauan tombol/keyboard | Laporan audit ruang kosong; tinjau pada PK-2.3/2.4/2.5/2.8, pertahankan target 48 dp |
| X19 | PK-2.11 meminta semua Ignore NoDeadEnd hilang, tetapi prasyarat belum mencakup editor PK-2.8 dan kalkulator PK-3.3/3.5 | Gerbang uji lintas tahap tidak dapat dipenuhi dengan prasyarat tertulis saja | Pertanyaan pilihan gerbang dicatat di PK-0.8-laporan.md; selaraskan sebelum PK-2.11 tanpa menghapus delapan kontrak |

### Yang sengaja ditunda, dengan alasan

| Hal | Alasan | Kapan |
|---|---|---|
| Layar titip dan belanja tempo | Di luar V-5; logikanya aman di kode | Sesudah MVP (K-9) |
| Jual cepat (grid favorit) | "Aku jual" sudah memenuhi syarat lulus; grid adalah percepatan | Sesudah MVP |
| Simpan otomatis sebagai draf | Dialog tiga pilihan menyelesaikan masalah yang sama lebih murah | Bila uji pengguna menunjukkan perlu (K-7) |
| Retur, jaminan, status pesanan, musim tanam, marketplace | Bidang khusus; sebagian besar tanpa dampak schema | Sesudah MVP, menurut jawaban K |
| Laporan per bulan, grafik Canvas, hapus Vico | Tidak menghalangi syarat lulus | Sesudah MVP |
| Kunci app, notifikasi jatuh tempo | Sama | Sesudah MVP |
| Pemecahan berkas besar | Berisiko regresi saat model masih berubah | Sesudah merge |
| Peringatan lint (89) | Tidak ada error; dipatok supaya tidak naik | PK-5.6 mencatat angkanya |
| Impor pintar | Bertentangan dengan DEC-012 | Keputusan terpisah |
| Review independen Paket B dan N | **Tidak ditunda lagi**: PK-0.3 dan PK-0.4 | Tahap 0 |
| Hotfix INTERNET ke `main` | **Tidak ditunda**, menunggu K-15a saja | Tahap 0 |

## 7. Aturan kerja pelaksana (ringkas; lengkapnya di Lampiran D)

1. Satu paket per sesi. Baca paketnya di Lampiran B dan berkas yang disebutnya saja.
2. Tes ditulis dulu dan terlihat gagal. Tes terfokus selama bekerja; suite penuh sekali di akhir paket.
3. Verifikasi nyata di emulator sesuai butir "Bukti nyata" paket itu. Tes hijau saja belum selesai.
4. Laporan di `docs/superpowers/reports/PK-x.y-laporan.md`; review di `PK-x.y-review.md`.
5. Commit dengan path eksplisit, log di commit yang sama, lalu satu baris di bagian 8 berkas ini, lalu push.
6. Paket "Butuh K-n" tanpa jawaban di tabel 4.16: berhenti, kerjakan paket lain yang tidak terhalang.
7. Temuan di luar lingkup paket: catat di laporan dan di register bagian 6, jangan dikerjakan diam-diam.

## 8. Status

- 2026-11-14 15:04 · Validasi form uang · checkpoint · [terverifikasi] blur/fokus jumlah, kuantitas dan harga; MoneyFormsTest 11/11, JVM 196/196 dan lint/build. [belum] form lain dan MVP. `docs/superpowers/reports/2026-11-14-validasi-form-uang.md`.

- 2026-11-14 14:58 · Validasi produk dan stok · checkpoint · lihat commit · [terverifikasi] blur/fokus untuk nama dan satuan barang serta nama, satuan dan hasil produk; tes terarah 4/4 dan 6/6, JVM 196/196, lint/build. [belum] form lain dan MVP. `docs/superpowers/reports/2026-11-14-validasi-produk-stok.md`.


- 2026-11-14 14:50 · Kategori lama dan contoh validasi · checkpoint · lihat commit · [terverifikasi] penanda kategori tidak pasti dan koreksi eksplisit; blur jumlah stok/nama kategori; JVM 196/196, lint/build dan tes terarah. [belum] koreksi Catatan pada upgrade 13→14, validasi semua form, PDF visual dan MVP. `docs/superpowers/reports/2026-11-14-kategori-validasi-checkpoint.md`.


Format: `- YYYY-MM-DD HH:MM · PK-x.y · selesai / berhenti di … · commit · bukti · catatan`.

- 2026-11-13 11:51 · Modal penjualan belum diketahui · checkpoint · lihat commit · [terverifikasi] laba/BEP ITEM_COST ditahan di layar/ekspor, SIMPLE tidak terpengaruh; JVM 194/194, lint, uji UI/xlsx dan PDF smoke lulus. [belum] kategori-null ambigu, koreksi modal historis dan PDF visual; `docs/superpowers/reports/2026-11-13-modal-belum-diketahui.md`.

- 2026-11-13 11:44 · Audit layout dan pilihan · checkpoint · lihat commit · [terverifikasi] margin ringkas, dropdown dan satuan kustom, pilihan stok simetris, validasi setelah Simpan; satu AVD 1080 × 2400/420 dpi. [belum] audit semua pilihan, font 200%, TalkBack, HP nyata dan PK-1.2. Laporan `docs/superpowers/reports/2026-11-13-audit-layout-pilihan.md`.

- 2026-11-13 11:21 · PK-1.2 dan audit pemilik · checkpoint, belum selesai · lihat commit · [terverifikasi] kategori ganda dicegah, JVM 193/193, lint, tes UI dan AVD terisolasi; bahasa Indonesia sementara dan dialog stok diperbarui. [belum] kategori bebas/ambigu belum ditandai dan seed upgrade bebas belum lulus; laporan `docs/superpowers/reports/2026-11-13-pk12-audit-pemilik.md`.

- 2026-11-13 07:40 · Handoff audit progres PK-1.2 + bahasa/layout · berhenti di checkpoint · belum commit · [terverifikasi] JVM 193/193 pada putaran sebelumnya, tes fokus UI terakhir 4/7 dengan tiga kegagalan selector; 10 berkas implementasi/test/resource tetap belum commit. [terverifikasi] English dapat dipilih/disimpan dan dialog stok menumpuk dua aksi kanan, Kembali terpisah. [belum] screenshot emulator dan audit runtime menyeluruh. Laporan + prompt agent: `docs/superpowers/reports/2026-11-13-handoff-progress.md`. Jangan merge main atau sentuh superproject.

- 2026-11-12 02:05 · Rencana induk · ditulis · lihat log · tanpa kode; fakta [kode] dibaca di HEAD `7fe55ee` · menunggu jawaban K-1 sampai K-15 (DEC-032).
- 2026-11-12 02:03 · Keputusan pemilik · K-1 = A, K-2 = B, K-3 = A, K-4 = A (spec V-7 sampai V-10) · Tahap 1 tidak lagi terhalang keputusan; K-5 sampai K-15 masih menunggu.
- 2026-11-12 02:06 · PK-0.1 dan PK-0.2 · selesai · `f8a9bee` di `main`, digabung ke branch kerja · `ManifestTest` merah lalu hijau; unit 97/0 dan `assembleDebug` hijau di `main`; manifest gabungan debug memuat INTERNET · masuk Google dan Drive di HP nyata masih [belum] (PK-5.4).

- 2026-11-12 02:14 · PK-0.3 · selesai · commit docs(review) sesi ini · [terverifikasi] review statis lima skenario, 4 Important; lihat PK-0.3-review.md · F2 menunggu pilihan kontrak total kas; perbaikan PK-0.5 setelah PK-0.4, tes/emulator [belum].
- 2026-11-12 02:27 · PK-0.6 · selesai · commit paket ini · [terverifikasi] RED/GREEN/probe; 155 tes JVM/0 gagal, assembleDebug dan diff --check hijau. Pemeriksaan XML tidak menggantikan uji pengguna. · PK-0.4 sedang mengukur emulator; PK-0.7 berikutnya. Daftar dasar 243 pasangan harus dikosongkan pada PK-5.1.
- 2026-11-12 02:30 · Keputusan total kas (DEC-033) · selesai · commit paket ini · [terverifikasi] Dokumentasi kontrak; belum ada perubahan angka/kode pada keputusan ini. · Implementasi serta tes rekonsiliasi pada PK-0.5; keputusan K lain yang kosong tetap terhalang.
- 2026-11-12 02:34 · PK-0.4 · selesai ditinjau · commit paket ini · [terverifikasi] 12/12 sampel, jank 57,14–96,67 %, p90 25–53 ms; crash duplicate key direproduksi; hash pemulihan 4/4 sama. [belum] Akar tingkat fungsi dan HP nyata; bukan klaim kinerja lulus. · PK-0.5 memperbaiki crash dan temuan B, membetulkan klaim N; PK-0.7 sedang dikerjakan. Kinerja tetap terbuka di X3/PK-5.4.
- 2026-11-12 02:42 · PK-0.7 · selesai · commit paket ini · [terverifikasi] RED 3 perilaku + 2 alasan kosong; GREEN 16 tes komponen, 155 tes JVM, lint 0 error/89 peringatan. Screenshot/XML: alasan tampil di atas Simpan mati; pemulihan hash 4/4 sama. · PK-0.5 berikutnya; audit ruang kosong tambahan diminta pemilik, tanpa mengubah batas kapabilitas.
- 2026-11-12 07:59 · Audit ruang kosong (permintaan tambahan pemilik) · selesai · commit paket ini · [terverifikasi] Skrip bukti PASS; celah form 202–413 dp terutama akibat footer tetap; ruang pesan sekitar44 dp, sela resep berlebih12 dp. [belum] Uji pengguna, tablet, font besar, mode gelap; kode layout tidak diubah. · PK-0.5 terus berjalan; pertimbangkan temuan spacing pada paket layar yang dirujuk laporan, jangan mengecilkan target sentuh.
- 2026-11-12 08:24 · PK-0.5 · selesai · commit paket ini · [terverifikasi] RED tiap temuan lalu GREEN164 tes JVM/0 gagal;31 instrumented terfokus/0 gagal sebelum penghapusan label usang; setelah cleanup164 JVM+lint0error/87warning+assembleDebug lulus, diff --check lulus. APK akhir dipasang; data emulator pulih4/4hash identik. · PK-0.8 kemudian PK-1.1 dan paket model yang tidak terhalang. Kinerja X3/HP nyata tetap belum; daftar dasar bahasa242.
- 2026-11-12 08:34 · PK-0.8 · selesai · commit paket ini · [terverifikasi] RED penghitung4/3gagal dan diagnostik8/8gagal; GREEN paket12kasus=4lulus/8Ignore/0gagal, JVM164/164. Data emulator pulih4hash/noextras; diff --check lulus. Delapan perjalanan belum dinyatakan bekerja. · PK-1.1 model biaya barang; keputusan K kosong tetap penghalang. Selaraskan gerbang X19 sebelum PK-2.11.
- 2026-11-12 13:10 · PK-1.1 · selesai · commit paket ini · [terverifikasi] RED perilaku 9/21 dan 13/25; GREEN 25/25 terfokus, 179/179 JVM dan assembleDebug lulus. UI, migrasi, sinkron dan perangkat belum diuji pada paket domain ini. · Review independen dan PK-1.2 schema 14; K-8 tetap belum dijawab.
- 2026-11-12 23:48 · PK-1.2 · berhenti di checkpoint schema · commit paket ini · [terverifikasi] 37/37 tes instrumentasi terarah lulus; data emulator dipulihkan dengan 4 hash cocok. [belum] JVM/lint setelah perubahan terakhir, upgrade APK nyata, dan integrasi angka unknown/rencana/stok. · Lanjut PK-3.1 yang independen; selesaikan PK-1.2/1.3/1.6 dan jawab K-8 sebelum klaim finansial lengkap atau rilis.
- 2026-11-13 00:14 · PK-3.1 · selesai · commit paket ini · [terverifikasi] RED pecahan/status/konversi lalu GREEN 189 tes JVM, 6 instrumented, lint 0 error/87 warning, assembleDebug. Data emulator pulih 4 hash. [belum] Seluruh MVP dan PK-1.2 tetap checkpoint. · Lanjut pekerjaan yang tidak terhalang; selesaikan integrasi PK-1.2/1.3/1.6 sebelum klaim finansial lengkap.
- 2026-11-13 05:09 · PK-1.2 (bukti upgrade lanjutan) · berhenti di checkpoint schema dan bukti golden · commit paket ini · [terverifikasi] Upgrade install -r schema 13→14 lulus untuk seed kategori baku; integrity_check lulus, foreign_key_check kosong, Beranda Rp30.000 sebelum/sesudah. [terverifikasi] Kontrol tanpa categoryId menghasilkan Rp30.000→-Rp20.000 dan dicatat sebagai batas yang belum dijelaskan terhadap perilaku produk. [belum] Sync dengan APK lama, suite JVM/lint terbaru, dan jalur data tanpa kategori belum beres. · PK-1.2 tetap checkpoint dan belum release-ready. Telusuri perilaku laba untuk data lama tanpa categoryId, jalankan JVM/lint setelah perubahan terakhir, dan selesaikan propagasi costKnown/rencana/stok sebelum klaim angka finansial lengkap.
- 2026-11-13 05:32 · PK-1.3 · selesai pada lapisan domain/repository; layar input belum dibangun · commit paket ini · [terverifikasi] RED API tidak mendukung nullable ID/nama. GREEN FreeLineSaleTest 4/4, SalesFlowTest 9/9 dan ImportCommitTest 20/20 instrumentasi di AVD terpisah; MonthlySummaryTest 4/4, SalesMathTest 8/8, BackupUpgradeTest 5/5 JVM. Lint 0 error/87 warning dan assembleDebug berhasil; sesudah satu tes guard ditambahkan, FreeLineSaleTest diulang 4/4. git diff --check bersih. [belum] Suite penuh, layar input, editor nota bebas, dan ekspor lossless PK-4.1. · Lanjut paket yang tidak bergantung pada K kosong. PK-1.4 menunggu K-8; PK-2.1/2.2 menunggu K-6/K-7. Lanjut PK-4.7 yang tak punya K, catat bahwa WelcomeScreen belum ada dan tautan awal baru bisa ditambahkan saat PK-2.1 membangunnya. Jangan klaim laporan finansial lengkap sebelum propagasi costKnown PK-1.6 dan penjelasan seed kategori-null PK-1.2.
- 2026-11-13 05:56 · PK-4.7 · selesai pada alur lokal repository/Room; uji UI dan instal bersih belum terbukti · commit paket ini · [terverifikasi] BackupMoveFlowTest lulus 1/1: berkas terenkripsi dibuat, DB in-memory dihapus dan dipulihkan, MonthlySummary serta pembeli sama persis. [terverifikasi] Kode Beranda diarahkan ke chooser backup dan panduan 3 langkah ada dalam dua bahasa. [belum] Compose UI, pemilih Android, pm clear, Drive, HP nyata. Laporan PK-4.7-laporan.md menjelaskan bukti dan batasnya; PK-2.1 perlu membawa CTA jika WelcomeScreen dibuat.
- 2026-11-13 06:34 · PK-5.1 · selesai · commit paket ini · [terverifikasi] daftar dasar 242→0; ScreenLanguageTest 7/7, generator 103 string, diff --check bersih. [belum] keterbacaan visual/aksesibilitas/uji pengguna dan isi bantuan menyeluruh PK-5.2. · T5 ditutup pada tingkat gerbang kosakata; lanjutkan hanya paket tanpa keputusan pemilik kosong.
  - 2026-11-13 07:12 · PK-1.6 · selesai · commit paket ini · [terverifikasi] dua rumus dan golden ITEM_COST; 16 tes domain + 4 instrumentasi terarah; layar/BEP/ekspor sama untuk dua basis; tanpa schema baru. [belum] suite penuh, lint, PDF, sinkronisasi, backup/restore, semua edit/hapus, HP nyata. Nilai penjualan unknown saat ini bruto sebelum diskon; pertanyaan alokasi dicatat di laporan. · PK-1.2 tetap checkpoint untuk propagasi costKnown/rencana/stok; lanjut paket lain yang tidak menunggu keputusan.
