# Audit ruang kosong Android Mankeu

Tanggal: 12 November 2026. Acuan kode saat pengambilan bukti: `3564fdb`, cabang `fase2-nota-piutang`. Audit baca-saja terhadap aplikasi; tidak mengubah tata letak, menjalankan Gradle, atau membuat commit. Mode penilaian: Operate, mempertahankan Compose/Material yang sudah digunakan.

**Ruang kosong besar memang terlihat**, terutama pada formulir pendek setelah keyboard ditutup. Penyebab utamanya adalah tombol Simpan yang sengaja dipasang di bawah layar. Ada pula dua sumber jarak yang dapat dipangkas secara terarah: tempat pesan pelanggan yang tetap disediakan saat pesannya tidak tampil dan kolom resep kosong yang masih mendapat jarak antaranak. Bukti belum mendukung pengurangan seluruh jarak aplikasi atau ukuran target sentuh.

## Dasar dan batas bukti

- `user-confirmed`: pemilik merasa banyak ruang kosong; pernyataan ini memicu audit.
- `verified`: ukuran dan keadaan layar di bawah dibaca dari screenshot emulator dan pasangan XML di `../evidence/spacing/`, lalu ditelusuri ke kode. Screenshot PK-0.7 `product-empty-yield.png` juga dibaca sebagai pembanding.
- `inferred`: dampak pada pemilik UMKM yang dijelaskan di bawah merupakan penilaian UX, belum hasil uji pengguna.
- `proposed`: rekomendasi dan pemetaan paket merupakan usulan, belum diterapkan.
- `unknown`: keterpakaian pada tablet, layar kecil, ukuran huruf 200%, mode gelap, landscape, serta pendapat pengguna sesudah mencoba alternatif jarak.

Perangkat: emulator `Medium_Phone`, 1080 × 2400 px, density 420 dpi. Konversi `1 dp = 2,625 px`; lebar logis sekitar 411 dp. Selisih satu piksel dapat muncul karena pembulatan Compose. Jarak memakai **batas seluruh kontrol** dari XML, bukan hanya batas tulisan, kecuali disebutkan. XML tidak memuat setiap node layout Compose; penjelasan node kosong dikonfirmasi dengan kode.

Data bukti memakai nama sintetis `Usaha Contoh Audit` dan `Utama Contoh`. Keadaan awal memiliki usaha tetapi nol catatan keuangan dan nol produk. Satu pemasukan sintetis Rp100.000 dibuat untuk melihat Beranda/Laporan berisi. Cadangan asli disimpan hanya di TEMP, lalu dipulihkan sebelum emulator diserahkan ke pelaksana PK-0.5. SHA256 empat berkas asli (database, WAL, SHM, preferensi) sesudah pemulihan sama dengan sebelum audit. Tidak ada cadangan atau label asli yang dimasukkan ke bukti repo.

## 1. Simpan terpisah jauh dari akhir formulir pendek

Status `verified`; observasi prioritas P2 untuk penilaian alur formulir berikutnya, **belum ditetapkan sebagai bug tata letak**. Penyelesaian tugas tetap tersedia. Dampak kebingungan masih `inferred`.

| Keadaan dan screenshot | Akhir kontrol terakhir | Awal alasan Simpan | Ruang di antaranya |
|---|---:|---:|---:|
| [Uang masuk, keyboard tutup](../evidence/spacing/money-no-keyboard.png) | y1043, baris Tanggal | y2126 | 1083 px / 412,6 dp |
| [Uang masuk, keyboard tampil](../evidence/spacing/money-keyboard.png) | y1043, baris Tanggal | y1285 | 242 px / 92,2 dp |
| [Produk baru, keyboard tutup](../evidence/spacing/product-empty.png) | y1596, tombol Satuan jual | y2126 | 530 px / 201,9 dp |
| [Nota baru tanpa produk](../evidence/spacing/sale-no-keyboard.png) | y1131, tombol Tambah detail | y2126 | 995 px / 379,0 dp |
| [Belanja, keyboard tutup](../evidence/spacing/buy-no-keyboard.png) | y1380, tombol Tambah detail | y2126 | 746 px / 284,2 dp |

Akar terverifikasi: `FullPageForm.kt:85` menyediakan `bottomBar` tersendiri, sedangkan kolom isian di `FullPageForm.kt:112` sampai `116` mengisi viewport dan menggulir. Tidak ada spacer setinggi 200 sampai 400 dp dalam formulir-formulir ini. Ketika isi pendek, sisa viewport terlihat kosong. Alasan Simpan mengikuti footer, sesuai S3; tombol selebar halaman setinggi minimal 56 dp memenuhi S8.

Pada pasangan uang masuk, footer naik tepat 841 px ketika keyboard muncul. Tombol tetap berada di atas keyboard. `FullPageForm.kt:89` memakai gabungan inset navigasi dan IME, bukan penjumlahan keduanya. Bukti ini tidak menunjukkan inset ganda. Angka jarak dalam tabel mencakup sisa viewport dan jarak footer; angka tersebut tidak seluruhnya merupakan padding yang bisa dihapus.

Konsekuensi `inferred`: pemilik yang baru mengisi Tanggal harus memindahkan perhatian jauh ke bawah untuk menemukan alasan mengapa Simpan belum aktif. Sebaliknya, posisi tetap di bawah memudahkan jangkauan ibu jari dan tidak bergerak mengikuti panjang daftar. Kedua kebutuhan harus diuji bersama.

Rekomendasi `proposed`: pertahankan kontrak Simpan tetap terlihat dan alasan tepat di atasnya. Pada PK-2.4, PK-2.5, dan PK-2.8, tinjau formulir pendek di keyboard terbuka/tertutup; uji apakah footer perlu batas visual halus agar terasa sebagai kelompok aksi. Bila pengguna masih gagal menemukan aksi, buat tindak lanjut terpisah untuk varian formulir pendek dengan aksi lebih dekat ke isi. Jangan memindahkan semua footer hanya untuk mengisi layar. Terima perubahan hanya setelah aksi, hasil hitungan, pesan alasan, dan keyboard tetap terbaca tanpa mengecilkan target 48 dp.

## 2. Nota tetap menyediakan ruang pesan pelanggan yang sedang tersembunyi

Status `verified`, P2. [Bukti nota tanpa produk, pilihan Lunas](../evidence/spacing/sale-no-keyboard.png): batas chip pelanggan berakhir y729; kelompok pembayaran dimulai y846. Jarak 117 px / 44,6 dp terlihat walaupun tidak ada pesan pelanggan.

Akar: `SaleNoteScreens.kt:318` mempertahankan `Box.heightIn(min = 20.dp)`; hanya teks di dalamnya yang bersyarat pada `needsCustomer` (`:321`). Dua jarak `Space.Field` di induk menambah 24 dp. Jadi sekitar 44 dp itu terdiri atas tempat pesan 20 dp dan dua sela 12 dp. Ini berbeda dari sisa viewport pada temuan 1. Ruang tersebut dapat mengurangi pergeseran saat pesan muncul, tetapi tetap dibayar ketika Lunas dipilih dan pesan tidak dibutuhkan.

Konsekuensi `inferred`: pelanggan dan status pembayaran tampak kurang berhubungan. Pada layar pendek atau ketika keyboard tampil, bagian bawah formulir lebih cepat keluar dari area terlihat.

Rekomendasi `proposed`: pada pekerjaan PK-2.4, kelompokkan pesan dengan pemilih pelanggan dan tampilkan tempatnya hanya saat diperlukan, atau tempatkan pesan di ruang pendukung yang menjadi bagian kelompok itu. Untuk editor nota lama yang tetap dipakai, tindak lanjut kecil tersendiri dapat mengubah kotak ini. Uji perpindahan Lunas → Belum dibayar → Sebagian agar pesan tetap terlihat, fokus stabil, dan tidak muncul loncatan yang mengganggu. Tinggi sentuh chip/segmen dipertahankan.

## 3. Kolom resep kosong menggandakan sela setelah judul

Status `verified`, P3. [Produk baru tanpa resep](../evidence/spacing/product-empty.png): tulisan Resep 1 batch berakhir y506, tombol Biaya lain mulai y571. Jarak 65 px / 24,8 dp. Sebagai pembanding pada formulir uang masuk, label Untuk apa berakhir y506 dan chip dimulai y538: 32 px / 12,2 dp.

Akar: `ProductEditorScreen.kt:214` selalu memasukkan `Column` untuk daftar resep, walaupun `lines` kosong (`:215`). Kolom itu tetap menjadi anak dari `FullPageForm`, yang memasang `spacedBy(Space.Field)` (`FullPageForm.kt:116`, Field = 12 dp). Ada dua sela 12 dp yang mengapit anak tanpa isi. **Tambahan yang tidak perlu sekitar 12 dp; total sela sekitar 24 dp.** Nota tanpa produk memiliki pola kolom daftar kosong yang sama di `SaleNoteScreens.kt:281`.

Konsekuensi `inferred`: tombol menambah biaya terlihat lebih jauh dari judul yang menjelaskan fungsinya. Ini penyumbang kecil, tidak menjelaskan sendiri ruang kosong 200 dp sebelum Simpan.

Rekomendasi `proposed`: pada PK-2.8, jangan menambahkan kolom daftar saat resep kosong, atau satukan judul/daftar/aksi sebagai satu kelompok dengan jarak internal yang jelas. Untuk nota lama, lakukan pola yang sama saat `lines` kosong. Pertahankan area sentuh tombol. Verifikasi keadaan nol/satu/banyak baris agar pengelompokan tidak rusak setelah baris pertama ditambahkan.

## 4. Sisa layar Beranda dan Catatan: teramati, bukan bukti ruang yang dicadangkan

Status `verified`; tanpa tingkat bug. Dampak keterpakaian `unknown`.

| Keadaan | Ukuran sisa viewport | Penelusuran dan penilaian |
|---|---|---|
| [Beranda kosong](../evidence/spacing/home-empty.png) | Baris Rencana selesai sekitar y1093; batas area isi y2148: sekitar 1055 px / 402 dp | `HomeScreen.kt:136` sampai `145` menampilkan FirstSteps dan Rencana, lalu keluar dari LazyColumn. Tidak ada kartu kosong untuk utang atau stok yang tetap mengambil tempat. |
| [Beranda dengan satu pemasukan](../evidence/spacing/home-populated.png) | Rencana selesai sekitar y1138; area isi sampai y2148: sekitar 1010 px / 385 dp, sebagian sementara tertutup snackbar | `HomeScreen.kt:175` sampai `203` menambahkan tagihan/utang/stok hanya jika ada datanya. Tidak ada placeholder tersembunyi untuk kartu tersebut. |
| [Catatan kosong](../evidence/spacing/catatan-empty.png) | Kartu selesai y1228; area isi sampai y2148: 920 px / 350,5 dp; FAB menempati sebagian bawah | `SharedComponents.kt:116` memakai padding 22 dp dan sela 10 dp di dalam EmptyCard. Kartu membungkus isi; tidak dipaksa setinggi layar. |

Beranda kosong saat ini mengutamakan tiga langkah di atas dan tidak menyediakan dua pintu jual/beli yang sudah direncanakan Lampiran A §4.1. Ini sudah menjadi cakupan PK-2.3, sehingga audit ini tidak mengusulkan kartu pengisi ruang tambahan. Catatan kosong masuk PK-2.7. Ukur kembali sesudah paket tersebut selesai; pertahankan pengungkapan bertahap agar kartu tanpa data tidak muncul hanya untuk mengisi layar.

## Bagian yang tidak menunjukkan kelebihan ruang pada sampel

- [Laporan berisi](../evidence/spacing/reports-populated.png) memakai viewport untuk tabel laba/rugi dan arus kas; isi berlanjut ke bawah. `ReportsScreen.kt:130` menggunakan daftar dengan sela 14 dp. Sampel awal tidak menunjukkan bidang kosong besar. Bagian laporan di bawah viewport dan tab lain belum diperiksa.
- [Sheet Biaya lain](../evidence/spacing/product-line-sheet.png) membungkus dua isian, alasan, dan aksi; tidak membesar menjadi halaman penuh. Saat [keyboard muncul](../evidence/spacing/product-line-sheet-keyboard.png), awal sheet berpindah y1552 → y711, selisih 841 px, sama dengan perpindahan footer. `MankeuSheet.kt:107` memakai `weight(1f, fill = false)` untuk isi, dan `:99` menggabungkan inset. Ini pola yang layak dipertahankan untuk input pendek.
- Bukti bernama [buy-choice-sheet](../evidence/spacing/buy-choice-sheet.png) sebenarnya **halaman pemilih**, bukan ModalBottomSheet. Ia berisi pencarian dan satu pilihan karena belum ada bahan; ruang bawah adalah daftar yang belum berisi. Bukti [more-choice-sheet](../evidence/spacing/more-choice-sheet.png) adalah menu dropdown Lainnya, juga bukan sheet. Keduanya tidak membuktikan masalah tinggi `MankeuSheet`.

## Cakupan dan penerimaan tindak lanjut

Sudah diperiksa secara visual: Beranda kosong/berisi satu pemasukan, Catatan kosong, produk baru tanpa resep dan dengan keyboard, sheet biaya dua isian tanpa/dengan keyboard, nota baru tanpa produk, Belanja awal tanpa/dengan keyboard, uang masuk tanpa/dengan keyboard, pemilih pembelian kosong, dropdown Lainnya, dan bagian awal Laporan berisi. Total 15 pasangan PNG/XML. Screenshot produk PK-0.7 dipakai sebagai pembanding tambahan.

Belum diperiksa: nota berisi banyak baris atau keyboard nota, pembeli panjang, sheet pilihan dengan banyak entri, usaha tanpa cabang, data finansial banyak, seluruh tab laporan, tablet/foldable, landscape, font besar, TalkBack, mode gelap, serta HP fisik. Tablet stretching tetap `unknown`; tidak ditetapkan sebagai temuan dari kode `fillMaxWidth` saja.

Pemeriksaan ulang bukti tanpa emulator:

```powershell
python docs/superpowers/evidence/spacing/verify_measurements.py
```

Skrip memeriksa pasangan bukti dan batas XML untuk jarak formulir, sela resep kosong, serta perpindahan footer ketika keyboard tampil. Ia tidak menguji perilaku aplikasi saat ini. Setelah perbaikan, ambil bukti baru pada ukuran yang sama, tambahkan layar kecil/font besar, dan pastikan S1 (maksimal empat isian), S3 (alasan dekat Simpan), serta S8 (target minimal 48 dp dan aksi utama selebar halaman) tetap terpenuhi. Tidak ada skor kesehatan aplikasi atau klaim validasi pengguna dari audit ruang ini.
