# Lampiran A: spek layar alur awam (2026-11-12)

Induk: `2026-11-12-rencana-induk-mvp.md`. Standar yang mengikat setiap layar: cek-lis 12 butir di
`docs/audit/2026-11-12_AUDIT_BISNIS_PENGGUNA_AWAM.md` §2.

Semua teks di bawah adalah usulan untuk `values/strings.xml` (Indonesia). Padanan Inggris di `values-en` ditulis
pelaksana dengan makna yang sama. Sapaan di layar: **kamu**, konsisten.

## 1. Aturan yang berlaku di semua layar

| # | Aturan | Cara mengeceknya |
|---|---|---|
| S1 | Satu layar menjawab satu pertanyaan. Paling banyak 4 isian terlihat | Tes UI menghitung isian |
| S2 | Setiap isian punya nilai bawaan atau contoh. Tidak ada kotak kosong tanpa contoh | Tinjauan spek ini per layar |
| S3 | Tombol utama yang belum bisa ditekan menampilkan satu kalimat alasan tepat di atasnya | `FullPageForm(saveBlockedReason = …)`, PK-0.7 |
| S4 | Keadaan kosong selalu punya tombol yang menyelesaikannya, dengan `testTag("empty_action")` | `NoDeadEndTest`, PK-0.8 |
| S5 | Kata dari daftar terlarang tidak muncul di layar (Lampiran C §1.1) | `ScreenLanguageTest` |
| S6 | Pengguna tidak menghitung atau mengonversi. App yang mengalikan, membagi, dan mengubah gram ke kg | Tinjauan spek; golden konversi |
| S7 | Sesudah menyimpan, layar menyebut apa yang terjadi dan menawarkan langkah berikut dan "Urungkan" | Tes UI per alur |
| S8 | Target sentuh minimal 48 dp; tombol utama selebar layar; tanpa gestur tersembunyi | Tinjauan; uji HP murah |
| S9 | Back tidak pernah membuang isian tanpa bertanya, dan pertanyaannya menyebut pilihan dengan kata kerja | Dialog keluar, §9 |
| S10 | Angka ringkasan dibaca dari `MonthlySummary` | `OneNumberTest` |

## 2. Kosakata per jenis usaha

Jenis usaha (`businesses.kind`): `MAKER` buat sendiri, `TRADER` jual beli, `SERVICE` jasa atau sewa, `FARM` tani atau
ternak, `MIXED` campuran. Hanya kata di tabel ini yang berbeda per jenis; semua teks lain netral.

Pelaksana membuat satu objek `Vocabulary` (perluasan `presentation/BusinessWords.kt`) yang mengembalikan id string
menurut jenis usaha. Layar tidak boleh bercabang `when (kind)` sendiri.

| Kunci | MAKER | TRADER | SERVICE | FARM | MIXED |
|---|---|---|---|---|---|
| `item` (yang dijual) | Barang | Barang | Layanan | Hasil | Barang atau layanan |
| `items_tab` (nama tab) | Barang | Barang | Layanan | Hasil | Jualan |
| `sell_what_hint` | contoh: keripik singkong | contoh: beras 5 kg | contoh: bajak sawah | contoh: cabai merah | contoh: keripik, ganti oli |
| `buy_what_hint` | contoh: tepung, minyak, gas | contoh: beras 1 karung | contoh: solar, deterjen | contoh: bibit, pupuk | contoh: bahan, bensin |
| `unit_hint` | bungkus | pcs | kali | kg | pcs |
| `unit_suggestions` | bungkus, pcs, porsi, botol, kg, dus | pcs, kg, liter, bungkus, dus, karung | kali, jam, hari, kg, hektar | kg, ikat, karung, ekor, butir | pcs, bungkus, kali, kg |
| `recipe` (daftar bahan) | Bahan sekali buat | (tidak tampil) | Biaya sekali kerja | Biaya satu musim | Bahan sekali buat |
| `batch` | sekali buat | (tidak tampil) | sekali kerja | satu musim | sekali buat |
| `batch_yield_question` | Sekali buat jadi berapa? | (tidak tampil) | (tidak tampil) | Satu musim panen berapa? | Sekali buat jadi berapa? |
| `produce_action` | Catat hasil buat | (tidak tampil) | (tidak tampil) | Catat panen | Catat hasil buat |
| `default_item_kind` | MADE | TRADED | SERVICE | MADE | ditanyakan |
| `calc_door_title` | Hitung modal resep | Hitung untung barang | Hitung biaya layanan | Hitung biaya musim | Hitung modal |

Kata netral yang dipakai semua jenis:

| Konsep di kode | Kata di layar | Catatan |
|---|---|---|
| HPP | Modal barang (SERVICE: Biaya layanan) | "HPP" hanya di PDF dan Rincian analisis |
| profit | Untung | |
| receivable, nota belum lunas | Belum bayar | Judul kartu: "Siapa belum bayar" |
| payable | Belum kubayar | |
| sale note | Catatan jual (di daftar); "Nota" hanya pada teks yang dikirim ke pembeli | |
| purchase | Beli | Tombol: "Aku beli" |
| stock | Sisa barang | |
| contact customer | Pembeli | |
| contact supplier | Tempat beli | |
| business unit, project | Cabang | Tersembunyi sampai ada cabang kedua (spec #15) |
| fixed cost | Biaya rutin tiap bulan | |
| startup capital | Modal awal | |
| break-even | Supaya tidak rugi | |
| payback | Modal kembali | |
| cash on hand | Uang di tangan | |
| category | Jenis pengeluaran | Hanya muncul di "Aku beli" langkah 3 |

## 3. Layar pertama (PK-2.1)

**Pertanyaan:** usahamu seperti apa, dan apa namanya. Satu halaman penuh, bukan sheet. Tanpa kata "unit", "cabang",
"buku".

```
Halo! Usahamu seperti apa?

[ Bikin sendiri lalu dijual      ]   keripik, kue, warung makan, jahitan
[ Jual beli barang               ]   kelontong, pulsa, toko online
[ Jasa atau sewa                 ]   bengkel, laundry, traktor, tenda
[ Tani, ternak, atau kebun       ]   cabai, ayam, lele
[ Campuran atau belum tahu       ]

Nama usahamu
[ contoh: Keripik Bu Sri        ]

[            Mulai            ]
```

| Hal | Spek |
|---|---|
| Kartu jenis | Lima kartu besar berikon dan satu baris contoh. Satu dipilih. `testTag("kind_MAKER")` dst. |
| Nama usaha | Satu kotak. Contoh mengikuti jenis terpilih. Bila kosong saat "Mulai" ditekan, lihat baris berikut |
| Nilai bawaan | Tidak ada kartu terpilih. Nama kosong dengan contoh |
| Tombol mati | Belum pilih kartu: "Pilih dulu salah satu jenis usaha di atas." Nama kosong: "Tulis nama usahamu, boleh nama panggilan." |
| Sesudah Mulai | `createBusinessWithUnit(nama, "Utama")` dengan jenis usaha; cabang pertama tidak pernah ditanyakan. Langsung ke Beranda kosong |
| Punya cadangan | Tautan kecil di bawah: "Sudah pernah pakai Mankeu? Ambil data lama". Membuka alur pulih (PK-4.7) |
| Tidak ada | Sheet "Usaha dan Cabang / Unit" bertumpuk (L02), pilihan mata uang, tawaran kunci app (ditunda sampai sesudah catatan pertama) |
| Jumlah ketukan | 1 pilih, 1 ketik nama, 1 Mulai |

## 4. Beranda

### 4.1 Beranda kosong (PK-2.3)

Tampil selama belum ada satu pun catatan jual atau beli.

```
Keripik Bu Sri

[   Aku jual   ]   [   Aku beli   ]

Belum ada catatan. Mulai dari yang baru saja terjadi.

Mau hitung-hitung dulu?
[ Hitung modal resep        › ]
[ Layak nggak usahaku?      › ]
```

| Hal | Spek |
|---|---|
| Dua tombol besar | Tinggi minimal 96 dp, berikon, berdampingan. `testTag("door_sell")`, `testTag("door_buy")`, keduanya juga `empty_action` |
| Pintu Hitung | Dua baris. Judul baris pertama dari `Vocabulary.calc_door_title` |
| Tidak ada | Tiga langkah "Buat produk dan resepnya", deret Rp0, kartu tagihan kosong, kartu utang pemasok |

### 4.2 Beranda berisi

Urutan kartu tetap. Kartu muncul hanya bila ada isinya (progressive disclosure).

| Urutan | Kartu | Muncul bila | Isi | Sumber |
|---|---|---|---|---|
| 1 | Dua tombol | selalu | "Aku jual", "Aku beli" | |
| 2 | Untung bulan ini | ada catatan bulan ini | Satu angka besar. Di bawahnya satu baris cara hitung (§4.3) | `MonthlySummary.profitRupiah` |
| 3 | Uang di tangan | ada catatan | Satu angka | `cashOnHandRupiah` |
| 4 | Siapa belum bayar | ada tagihan | Tiga nama teratas dengan jumlah dan umur; tombol "Terima bayar" | `receivableRupiah`, `customerBills` |
| 5 | Ajakan | syarat §4.4 | Satu kartu ajakan saja pada satu waktu | |
| 6 | Belum kubayar | ada utang ke tempat beli | Nama, jumlah, tombol "Bayar" | `payableRupiah` |
| 7 | Hitung-hitung | selalu, kecil di bawah | Dua baris pintu Hitung | |

### 4.3 Baris cara hitung di kartu untung

| Keadaan | Teks |
|---|---|
| Cara Sederhana | "Jualan Rp… dikurangi belanja Rp…" |
| Cara Modal barang, semua modal diketahui | "Jualan Rp… dikurangi modal barang Rp… dan biaya lain Rp…" |
| Cara Modal barang, ada penjualan tanpa modal | Tambahan berwarna perhatian: "3 penjualan belum dihitung modalnya." dengan tombol "Isi modalnya" |
| Bulan ini rugi | Angka merah, kata "Rugi bulan ini", baris yang sama |

Ketuk kartu membuka Laporan bulan itu.

### 4.4 Kartu ajakan (satu pada satu waktu, bisa ditutup "Nanti saja")

| Urutan prioritas | Syarat | Teks | Aksi |
|---|---|---|---|
| 1 | Nama jual yang sama dipakai 2 kali dan belum jadi barang | "Kamu sudah 2 kali jual keripik. Simpan sebagai barang supaya tinggal pilih?" | "Simpan" membuat barang (PK-1.5) |
| 2 | Ada barang, cara hitung Sederhana, minimal 3 catatan jual | "Mau tahu untung tiap barang? Isi modalnya, cukup satu angka." | Membuka daftar barang tanpa modal (PK-2.10) |
| 3 | Semua barang yang dijual bulan ini punya modal, cara hitung masih Sederhana | "Modal semua barangmu sudah terisi. Hitung untung dari modal barang mulai sekarang?" | Dialog penjelasan lalu pindah cara (§10) |
| 4 | 7 hari sejak catatan pertama dan belum pernah mencadangkan | "Catatanmu baru ada di HP ini. Simpan cadangan?" | Membuka cadangan (PK-4.7) |

Kartu yang ditutup tidak muncul lagi selama 7 hari (disimpan di `AppPreferences`).

## 5. "Aku jual" (PK-2.4)

Halaman penuh, satu halaman bergulir, empat bagian. Semua bagian terlihat sejak awal, tetapi hanya "Jual apa?" yang
belum terisi.

```
‹  Aku jual

Jual apa?
[ contoh: keripik singkong                 ]
( Keripik )  ( Kue lapis )  ( Jualan hari ini )      <- yang pernah, paling sering dulu

Berapa?                       Harganya (satu)
[ − ]  1  [ + ]   bungkus     [ Rp 10.000   ]

Sudah dibayar?
( Lunas )  ( Belum )  ( Sebagian )

+ Tambah barang lain

Total Rp10.000
[            Simpan            ]
```

| Bagian | Isian | Nilai bawaan | Catatan |
|---|---|---|---|
| Jual apa | Teks bebas dengan saran | Kosong, contoh dari `sell_what_hint` | Saran: barang tersimpan dan nama yang pernah diketik, cocok awalan, tanpa beda huruf besar kecil. Memilih saran mengisi harga dan satuan terakhir. Chip "Jualan hari ini" selalu ada sebagai chip terakhir |
| Berapa | Stepper | 1 | Ketuk angka untuk mengetik; menerima "2,5". Satuan: milik barang, atau `unit_hint`; ketuk untuk ganti (pemilih satuan PK-2.9). Untuk "Jualan hari ini" bagian ini disembunyikan |
| Harganya | Uang | Harga terakhir nama itu; kosong bila baru | Label "Harganya (satu)"; untuk "Jualan hari ini" label menjadi "Dapat berapa hari ini?" |
| Sudah dibayar | Tiga pilihan | Lunas | "Belum" atau "Sebagian" membuka bagian Pembeli di bawahnya |
| Pembeli | Chip pembeli terakhir, lalu kotak "Nama pembeli" | Kosong | Mengetik nama baru membuat pembeli saat disimpan, tanpa pindah layar. Hanya nama; nomor HP bisa ditambah nanti |
| Dibayar berapa | Uang | Kosong | Hanya untuk "Sebagian" |
| Kapan bayar | Chip 7 hari / 14 hari / 30 hari / Belum tahu | Belum tahu | Hanya untuk "Belum" dan "Sebagian" |
| Tambah barang lain | Tautan | | Menambah baris kedua dengan tiga isian yang sama; baris sebelumnya menjadi ringkas ("1 bungkus Keripik · Rp10.000") |
| Tanggal | Baris "Hari ini ›" di paling bawah | Hari ini | |

Pesan saat Simpan belum bisa ditekan (satu saja, menurut urutan):

| Keadaan | Pesan |
|---|---|
| "Jual apa" kosong | "Tulis dulu apa yang kamu jual." |
| Harga kosong atau nol | "Isi harganya dulu." |
| Jumlah nol | "Jumlahnya minimal 1." |
| Belum atau Sebagian tanpa pembeli | "Siapa yang belum bayar? Tulis namanya." |
| Sebagian tanpa jumlah, atau jumlah melebihi total | "Isi berapa yang sudah dibayar, kurang dari Rp{total}." |

Sesudah Simpan: kembali ke asal, dengan pesan "Tersimpan: 1 bungkus Keripik Rp10.000" dan tombol "Urungkan" (10
detik) serta "Kirim ke pembeli" bila ada pembelinya.

Aturan data: nama yang cocok dengan barang tersimpan menjadi baris berproduk; selain itu baris bebas. Modal baris
berproduk dari `unitCost`; baris bebas tanpa modal. Pengguna tidak ditanya modal di layar ini.

Ketukan sasaran, penjualan ulang barang yang pernah: buka (1), pilih chip (1), Simpan (1) = **3**. Penjualan
pertama di instal bersih: buka (1), ketik nama (1), ketik harga (1), Simpan (1) = 4, ditambah 3 di layar pertama.

## 6. "Aku beli" (PK-2.5)

```
‹  Aku beli

Beli apa?
[ contoh: tepung, minyak, gas              ]
( Tepung )  ( Minyak )  ( Listrik )

Bayar berapa?
[ Rp                                        ]

Ini untuk apa?
( Bahan atau barang jualan )  ( Keperluan usaha lain )  ( Alat yang tahan lama )

+ Catat jumlahnya juga  (supaya sisa barang dan modal ikut terhitung)

[            Simpan            ]
```

| Bagian | Isian | Nilai bawaan | Catatan |
|---|---|---|---|
| Beli apa | Teks bebas dengan saran | Kosong, contoh `buy_what_hint` | Saran: bahan, barang dagangan, dan nama belanja yang pernah. Memilih bahan atau barang menautkannya |
| Bayar berapa | Uang | Kosong | Total yang dibayar, bukan harga satuan |
| Ini untuk apa | Tiga pilihan | Mengikuti pilihan terakhir untuk nama itu; nama baru: "Bahan atau barang jualan" (MAKER, TRADER, FARM, MIXED) atau "Keperluan usaha lain" (SERVICE) | Menentukan jenis pengeluaran: pilihan pertama memakai jenis bertanda "sudah dihitung di modal barang"; kedua "Biaya Lain-lain" dengan pemilih kecil (sewa, listrik, gaji, bensin, lainnya); ketiga "Beli Peralatan" dan menanyakan "Kira-kira dipakai berapa lama?" dengan chip 1 / 2 / 5 tahun |
| Catat jumlahnya | Lipatan | Tertutup; terbuka sendiri bila nama cocok dengan bahan atau barang dagangan | Stepper jumlah dan satuan. Harga satuan dihitung app dan ditampilkan ("Rp13.000 per kg") |
| Belum dibayar | Tidak tampil di MVP bila K-9 = A | | |
| Tanggal | Baris "Hari ini ›" | Hari ini | |

Pesan tombol mati: "Tulis dulu apa yang kamu beli." lalu "Isi berapa yang kamu bayar."

Bila harga satuan bahan berubah dari belanja terakhir dan bahan itu dipakai resep, sesudah Simpan tampil pesan yang
sudah ada (dampak ke modal barang), dengan kata "modal".

## 7. Siapa belum bayar dan terima bayar (PK-2.6)

Layar dan logika sudah ada (`BillsCard`, `CustomerScreen`, `PaySheet`, `recordCustomerPayment`). Yang diubah hanya
kata dan jalan masuk.

| Hal | Spek |
|---|---|
| Jalan masuk | Kartu Beranda; filter "Belum bayar" di Catatan |
| Daftar | Per pembeli: nama, total belum bayar, "sudah 12 hari" atau "lewat 3 hari" |
| Terima bayar | Sheet pendek: "Terima berapa dari Toko A?" dengan nilai bawaan seluruh sisa. Tombol "Terima". Pesan tombol mati: "Isi jumlahnya, paling banyak Rp{sisa}." |
| Sesudahnya | "Toko A sudah lunas." atau "Toko A masih belum bayar Rp…" dengan "Urungkan" |
| Ingatkan | Tombol "Ingatkan lewat WhatsApp" (teks pengingat yang sudah ada) |
| Kosong | "Tidak ada yang belum bayar." Tanpa tombol tambahan, karena ini bukan jalan buntu |

## 8. Barang (PK-2.8) dan satuan (PK-2.9)

### 8.1 Editor barang bertahap

Tahap pertama hanya tiga isian:

```
‹  Barang baru

Namanya
[ contoh: keripik singkong ]

Dijual per          Harganya
( bungkus ▾ )       [ Rp        ]

Barang ini …   (hanya untuk MIXED, atau saat pengguna menekan "ganti")
( kubuat sendiri )  ( kubeli lalu kujual lagi )  ( jasa atau sewa )

Atur lebih lanjut ›
[            Simpan            ]
```

"Atur lebih lanjut" membuka bagian menurut jenis barang:

| Bagian | MADE | TRADED | SERVICE |
|---|---|---|---|
| Modal satu angka ("Modalnya kira-kira berapa per bungkus?") | ya, bila belum ada resep | ya, sampai ada belanja | ya, berjudul "Biayanya kira-kira berapa per kali?" |
| Hitung modal dari bahan (membuka kalkulator, §11) | ya | tidak | ya, berjudul "Rinci biayanya" |
| Dijual juga per … (satuan lain: dus isi 40) | ya | ya, juga dipakai saat beli ("karung isi 25 kg") | ya |
| Hitung sisa barang | bawaan nyala | bawaan nyala | tidak tampil |
| Catat hasil buat | ya, bila hitung sisa nyala | tidak tampil | tidak tampil |

Isian "Isi" pada satuan lain ditulis sebagai kalimat: "1 dus isi berapa bungkus?".

Pesan tombol mati: "Tulis nama barangnya." lalu "Isi harganya." Tidak ada isian lain yang menghalangi Simpan.

### 8.2 Pemilih satuan

Sheet berpencarian (`PickerPage`). Isi: satuan yang sudah dipakai usaha ini, paling sering dulu, lalu
`unit_suggestions`. Mengetik "karung" yang belum ada menampilkan "+ Pakai 'karung'". Mengetik "Karung" saat "karung"
sudah ada memilih yang sudah ada. Kotak pencarian selalu kosong saat dibuka (memperbaiki U5).

## 9. Dialog keluar form (PK-2.2, bila K-7 = A)

Muncul saat Back ditekan dan ada isian.

| Keadaan | Judul | Tombol |
|---|---|---|
| Sudah bisa disimpan | "Catatan ini belum disimpan." | "Simpan" (utama), "Lanjut isi", "Hapus isian" |
| Belum bisa disimpan | "Catatan ini belum lengkap: {alasan tombol mati}" | "Lanjut isi" (utama), "Hapus isian" |

## 10. Pindah cara hitung untung (PK-2.10, bila K-1 = A)

Dialog saat pengguna menerima ajakan 3 di §4.4, atau dari menu Usaha.

> **Hitung untung dari modal barang?**
> Sekarang: untung = jualan dikurangi semua belanja.
> Sesudah ini: untung = jualan dikurangi modal barang yang terjual dan biaya lain. Belanja bahan tidak lagi langsung
> mengurangi untung, karena sudah dihitung lewat modal barang.
> Angka bulan-bulan lalu ikut dihitung ulang. Bulan ini: Rp350.000 menjadi Rp410.000.
> ( Pakai ) ( Nanti saja )

Dua angka di kalimat terakhir dihitung dari data nyata lewat `MonthlySummary` dengan kedua cara. Bisa dikembalikan
dari menu Usaha.

## 11. Kalkulator resep (PK-3.3)

Jalan masuk: pintu Hitung di Beranda, dan "Hitung modal dari bahan" di editor barang. Bisa dibuka tanpa satu pun
catatan. Halaman penuh bergulir dengan hasil hidup menempel di bawah.

```
‹  Hitung modal resep

Bikin apa?
[ contoh: keripik singkong ]

Bahan sekali buat
  Singkong   50 kg      Rp1.500.000   ›
  Minyak     20 liter   Rp500.000     ›
  Gas                   Rp100.000     ›
+ Tambah bahan atau biaya

Sekali buat jadi berapa?
[ 400 ]  ( bungkus ▾ )
Biasanya ada yang gagal atau susut?   [ 0 ] bungkus

──────────────────────────────
Modal sekali buat      Rp2.400.000
Modal per bungkus      Rp6.000

Mau untung berapa?
( 20% )  ( 30% )  ( 50% )  ( ketik sendiri )
Harga jual disarankan  Rp7.800 per bungkus
[ Pakai harga ini ]   atau ketik hargamu: [ Rp      ]

Kalau harga bahan naik?   ›
[        Simpan sebagai barang        ]
```

Sheet "Tambah bahan atau biaya":

| Isian | Nilai bawaan | Catatan |
|---|---|---|
| Namanya | Kosong, contoh "tepung terigu" | Saran dari daftar bahan; memilih bahan yang ada mengisi harga terakhirnya |
| Jenis | "Bahan yang ditakar" atau "Biaya sekali buat (gas, upah, kemasan)" | Bawaan: bahan. Biaya hanya meminta satu angka rupiah |
| Dipakai berapa | Kosong; satuan bawaan mengikuti satuan harga | Menerima "0,25", "250" dengan satuan gram |
| Harga belinya | "Rp … untuk … satuan" | Contoh terisi abu-abu: "Rp12.000 untuk 1 kg". Jumlah bawaan 1 |
| Hasil | Kalimat hidup: "250 gram dari Rp12.000 per kg = Rp3.000" | Dihitung app |

Konversi yang dikenal app: gram, ons, kg; ml, liter. Bila satuan pakai dan satuan harga tidak bisa dikonversi
(pakai "sendok", harga per "kg"), sheet menampilkan satu pertanyaan tambahan: "1 kg itu berapa sendok?".

Pesan tombol mati di sheet: "Tulis nama bahannya." "Isi dipakai berapa." "Isi harga belinya."
Pesan tombol mati "Simpan sebagai barang": "Tulis dulu nama yang kamu bikin." "Tambah minimal satu bahan." "Isi
sekali buat jadi berapa." "Yang gagal tidak boleh sebanyak atau melebihi hasilnya."

Aturan hitung (golden di Lampiran C §4):

| Hal | Aturan |
|---|---|
| Biaya satu bahan | harga × dipakai ÷ "untuk berapa", dibulatkan sekali ke rupiah |
| Modal per satuan | modal sekali buat ÷ (hasil − gagal) |
| Mau untung X% | dihitung dari modal: harga = modal × (1 + X%). Dibulatkan ke atas ke Rp100 terdekat, dan layar menyebut "dibulatkan" |
| Satuan jual lain | Bila ada "dus isi 40": tampil juga "Modal per dus Rp240.000 · disarankan Rp312.000" |
| Kalau naik | Sheet: pilih satu bahan atau "Semua", pilih 5% / 10% / 20% / ketik. Hasil: "Modal per bungkus naik dari Rp6.000 ke Rp6.125. Dengan harga Rp7.800 untungmu per bungkus turun dari Rp1.800 ke Rp1.675." Tidak mengubah data |

"Simpan sebagai barang" membuat barang MADE dengan resep, satuan, harga, dan bahan-bahan baru di daftar bahan. Sesudah
itu: "Tersimpan. Keripik sekarang bisa dipilih di Aku jual, modalnya Rp6.000 per bungkus." dengan tombol "Aku jual".

Untuk SERVICE judul dan kata mengikuti §2 ("Biaya sekali kerja": solar, upah), dan pertanyaan hasil menjadi "Sekali
kerja dihitung berapa?" dengan bawaan 1 kali. Untuk TRADED pintu ini membuka versi pendek: harga beli, isi, harga
jual, untung per satuan.

## 12. Kelayakan usaha (PK-3.5)

Jalan masuk: pintu Hitung "Layak nggak usahaku?". Menggantikan halaman Rencana berperiode. Tiga langkah, satu
pertanyaan per halaman, dengan "Lanjut" dan indikator "1 dari 3". Semua jawaban tersimpan dan bisa diubah kapan saja.

| Langkah | Pertanyaan | Isian | Nilai bawaan dan contoh |
|---|---|---|---|
| 1 | "Sebulan kira-kira laku berapa?" | Baris per barang: nama, jumlah per bulan, satuan, harga, modal per satuan | Barang yang sudah ada tampil dengan harga dan modalnya; jumlah kosong dengan contoh "50". "+ Tambah barang" membuat barang sederhana (nama, harga, modal satu angka) |
| 2 | "Apa saja biaya rutin tiap bulan?" | Baris: nama dan rupiah per bulan | Chip contoh yang tinggal diketuk: Sewa tempat, Gaji, Listrik dan air, Pulsa dan internet, Lainnya |
| 3 | "Modal awal: perlu beli apa sebelum mulai?" | Baris: nama dan rupiah | Chip contoh: Alat, Perlengkapan, Renovasi, Izin. Boleh dilewati ("Sudah punya semua") |

Halaman hasil, dengan kalimat, bukan tabel:

```
Untung kira-kira Rp1.800.000 per bulan
Jualan Rp15.000.000 − modal barang Rp12.000.000 − biaya rutin Rp1.200.000

Supaya tidak rugi, jual minimal 20 dus per bulan.
Modal awal Rp3.600.000 kembali dalam 2 bulan.

LAYAK                                   (atau BELUM LAYAK / BELUM BISA DINILAI)

Rincian analisis ›      Simpan sebagai PDF ›
```

| Keadaan | Putusan dan kalimat (bila K-14 = A) |
|---|---|
| Biaya rutin kosong | "Belum bisa dinilai. Isi dulu biaya rutin tiap bulan." dengan tombol ke langkah 2 |
| Untung per bulan > 0 | "Layak" |
| Untung per bulan ≤ 0 | "Belum layak. Tiap bulan kurang Rp…" dan saran: "Supaya tidak rugi, jual minimal … per bulan." |
| Ada barang dengan harga di bawah modal | "Keripik dijual di bawah modalnya. Naikkan harga atau turunkan modal." |
| Modal awal kosong | Kalimat modal kembali tidak tampil |
| Beberapa barang | Titik tidak rugi disebut per barang menurut perbandingan jumlah di langkah 1 |

Pesan tombol mati "Lanjut" di langkah 1: "Isi dulu minimal satu barang dengan jumlah, harga, dan modalnya."

"Rincian analisis" (terlipat) memuat istilah formal: R/C, B/C, penyusutan. Hanya di sini dan di PDF kata itu boleh
muncul (spec #19).

Tidak ada: pemilih periode, tanggal mulai dan selesai, kata "Rencana Anggaran", "Proyeksi", "Realisasi".

## 13. Catatan (PK-2.7)

| Hal | Spek |
|---|---|
| Kosong | "Belum ada catatan." dengan dua tombol "Aku jual" dan "Aku beli" (`empty_action`). Tanpa "0 catatan pada unit usaha dan buku yang aktif" |
| Tombol + | Sheet pilihan: "Aku jual", "Aku beli", lalu di bawah garis "Lainnya": uang masuk lain, modal dari kantong sendiri, ambil untuk pribadi |
| Baris | Tetap daftar per tanggal dari Paket B. Baris bebas menampilkan nama yang diketik |

## 14. Excel: unduh, isi, ambil (PK-4.5, PK-4.6)

| Hal | Spek |
|---|---|
| Jalan masuk | Menu Lainnya, "Excel": tiga baris berurutan "1. Unduh template kosong", "2. Ambil dari Excel", "3. Simpan semua catatan ke Excel" |
| File ditolak | Judul: "File ini belum bisa diambil." Daftar masalah, tiap baris: "Lembar Nota, baris 5, kolom Tanggal: tertulis 'kemarin'. Tulis seperti 2026-11-12." Tombol: "Unduh template kosong", "Pilih file lain" |
| Pratinjau | "File ini berisi 12 catatan jual, 3 barang, 5 pembeli." Jenis pengeluaran baru disebut satu per satu |
| Mode (bila K-13 = A) | Dua tombol: "Tambahkan ke catatan yang ada" dan "Ganti isi usaha ini dengan isi file". Di bawah "Ganti": "Catatan usaha ini sekarang (48 catatan) diganti isi file. Cadangan dibuat dulu dan bisa dibatalkan." |
| File pernah diambil | Peringatan sebelum "Tambahkan": "File ini sudah pernah diambil tanggal …. Menambahkannya lagi membuat catatan ganda." |
| Sesudahnya | "12 catatan ditambahkan." dengan "Batalkan" yang juga tersedia di daftar riwayat ambil |

Lembar Petunjuk di dalam file: enam langkah berbahasa sehari-hari, arti tiap lembar dalam satu kalimat, aturan isi
(tanggal `2026-11-12`, uang tanpa titik dan tanpa "Rp", jangan mengubah judul kolom). Lembar "Contoh" berisi tiga
baris contoh per lembar dan diabaikan saat diambil.
