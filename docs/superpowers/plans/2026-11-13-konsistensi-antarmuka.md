# Rencana konsistensi antarmuka Mankeu

**Status:** disepakati pemilik pada 13 November 2026; pelaksanaan dan pemeriksaan seluruh aplikasi belum selesai.

**Tujuan:** membuat seluruh layar mudah dipahami dan konsisten, sambil mempertahankan karakter Mankeu yang sudah ada dan setiap kemampuan yang masih berlaku.

**Acuan:** DEC-042, `docs/QUALITY_GATES.md`, `docs/WORK_IN_PROGRESS.md`, rencana induk `2026-11-12-rencana-induk-mvp.md`, dan aturan repo `AGENTS.md`. Bila aturan validasi lama bertentangan, DEC-042 menggantikan ketentuan waktunya dalam DEC-034 dan DEC-040. Rencana produk, domain dan keputusan pemilik lain tetap berlaku.

**Teknologi dan tempat kerja:** aplikasi Android Kotlin/Jetpack Compose Material 3; branch `fase2-nota-piutang`. Jangan merge ke `main` atau mengubah superproject. Jalankan Gradle hanya dari PowerShell dengan PATH bersih. Ubah teks dengan Edit atau Python UTF-8, pertahankan newline dan perubahan yang sudah ada. Gunakan staging dengan path eksplisit; catat, commit dan push setiap paket yang lolos pemeriksaan.

## Keputusan pemilik

- [user-confirmed] Seluruh aplikasi masuk audit. Produk, Pengaturan dan Pusat Bantuan menjadi rujukan awal; bila pola mereka berbeda untuk fungsi yang sama, pilih yang paling jelas setelah dibandingkan. Pertahankan karakter warna, tipografi dan bentuk; ubah hanya yang diperlukan.
- [user-confirmed] Hasilnya rapi dan tenang: ruang cukup, warna seperlunya, kartu untuk kelompok informasi, dan rincian penting tetap terlihat. Jangan menambahkan kartu hanya demi menyeragamkan tampilan.
- [user-confirmed] Pilihan sedikit memakai kontrol tersambung bila masuk akal; daftar panjang atau dapat bertambah memakai dropdown atau pencarian. Kontrol yang fungsinya berbeda boleh berbeda, dengan ukuran, label, status terpilih dan jarak yang konsisten.
- [user-confirmed] Pada satuan, dropdown memuat satuan yang tersedia. Tombol tambah yang terpisah berada di sebelah dropdown dan membuka input satuan kustom; tombol ikon memiliki nama aksesibilitas yang jelas. Penambahan dan penyimpanan satuan tidak boleh mengubah isian atau perilaku lain.
- [user-confirmed] Pilihan gerakan stok “Tambah | Pakai | Hitung ulang” menggunakan kontrol tersambung seperti pilihan pembayaran. Kategori menggunakan istilah “Tetap | Variabel”; jelaskan maknanya dan jelaskan pilihan modal barang di tempat pilihan itu berada.
- [user-confirmed] Pesan kesalahan merah muncul setelah kolom tidak valid ditinggalkan. Jika Simpan ditekan dengan kolom wajib yang belum disentuh, cegah penyimpanan, tampilkan kesalahan pada kolom yang perlu diisi dan arahkan fokus/scroll ke kolom tersebut. Pesan hilang segera setelah isian valid. Petunjuk netral boleh terlihat sejak awal. Untuk kesalahan lintas kolom atau transaksi, tampilkan alasan yang terkait dengan tindakan setelah percobaan; jangan menghilangkan konflik impor yang sudah diketahui pada pratinjau.
- [user-confirmed] Gunakan satu AVD bergantian untuk lebar ringkas dan ponsel umum, font besar, serta tema terang dan gelap. Uji HP fisik jika tersedia; simulasi tidak membuktikan hasil pada HP fisik. Contoh di emulator tidak membutuhkan persetujuan tambahan: lanjutkan ke kelompok berikut setelah contoh sesuai acuan dan pemeriksaannya lulus.
- [user-confirmed] Tinjau ulang perubahan stok, satuan dan kategori yang telah dibuat. Pertahankan yang cocok dengan acuan dan perbaiki yang bertentangan. Keputusan pemilik mengizinkan perubahan fondasi dan migrasi yang diperlukan untuk produk praproduksi, dengan tes dan penjelasan dampaknya; izin ini bukan persetujuan merge atau rilis.

## Kondisi awal dan prioritas

1. [terverifikasi pada sesi perencanaan] HEAD `97eb0bf` di `fase2-nota-piutang`; ada perubahan kode dan tes kategori-null serta UI yang belum di-commit. Jalankan `git status --short --branch` dan `git diff` lagi sebelum mulai; jangan reset atau memasukkan perubahan belum diverifikasi ke commit dokumentasi.
2. [terverifikasi dari putaran sebelumnya] `ScreenLanguageTest` gagal karena `Variabel` yang diminta pemilik masih dilarang, dan string English baru memuat `break-even`. Ini bukan bukti seluruh tes gagal atau lolos. Tambahkan pengecualian sempit hanya untuk resource `category_cost_nature_hint|variabel` dan `category_cost_nature_variable_short|variabel`, dengan tes bahwa resource lain tetap diperiksa; ubah teks English yang tidak perlu memakai istilah terlarang. Catat bahwa pengecualian ini adalah keputusan pemilik, bukan pelonggaran daftar secara umum.
3. [belum] Perubahan kategori-null yang sedang ada belum dibuktikan lengkap: penanganan kategori dan modal tidak pasti, perbaikan lewat alur pengguna, upgrade 13→14, ekspor/PDF serta SIMPLE versus ITEM_COST harus tetap diverifikasi. Jangan menyatakan PK-1.2 atau angka finansial selesai berdasarkan rencana UI ini.
4. Selesaikan perubahan terarah yang sudah ada, termasuk permintaan terakhir pemilik untuk kontrol stok, tombol tambah satuan, jarak kategori dan bantuan checkbox. Catat hasil sebagai checkpoint tersendiri sebelum memperluas perubahan lintas aplikasi.

## Peta berkas

- Rujukan gaya: `presentation/Theme.kt`, `screens/ProductsScreen.kt`, `screens/ProductEditorScreen.kt`, `screens/AccountSettingsScreen.kt`, `guide/HelpUi.kt` dan `guide/Guides.kt` di `app/src/main/java/com/mankeu/`.
- Komponen bersama yang harus ditelusuri sebelum perubahan: `presentation/components/MankeuSheet.kt`, `presentation/components/form/FullPageForm.kt`, `presentation/components/form/Rows.kt` dan `screens/UnitPicker.kt`.
- Contoh dan tindak lanjut: `screens/StockSection.kt`, `screens/MoreScreen.kt`, `screens/SaleNoteScreens.kt`, `screens/MoneyForms.kt`, lalu seluruh layar di `presentation/screens/`, navigasi dan bantuan.
- Tes terarah: `MaterialStockScreenTest`, `ProductEditorTest`, `HomeScreenTest`, `SaleNoteFlowTest`, `FormComponentsTest`, `ScreenLanguageTest`, serta tes domain/Room dan ekspor yang relevan pada perubahan finansial. Jangan mengasumsikan nama tes lain tanpa memeriksa sumbernya.
- Dokumentasi dan bukti: `docs/WORK_IN_PROGRESS.md`, rencana induk §8, laporan audit baru di `docs/superpowers/reports/`, screenshot sintetis yang aman, `docs/QUALITY_GATES.md`, panduan yang terdampak, dan `logs/`.

## Paket pelaksanaan dan bukti penerimaan

### 1. Audit acuan dan inventaris

- [ ] Baca tiga layar acuan dan komponen bersama; ambil screenshot sintetis dan catat ukuran layar, dpi, tema, ukuran font, keadaan dan tanggal. Bandingkan jarak, hierarki teks, bentuk, kartu, tombol, navigasi dan umpan balik.
- [ ] Inventarisasi semua layar dan dialog yang benar-benar ada. Catat untuk setiap temuan: lokasi, fungsi, perbedaan dari acuan, dampak pengguna, pola yang dipilih, dan status `terverifikasi`, `dilaporkan`, `disimpulkan`, `diusulkan`, atau `belum`.
- [ ] Hasil: satu daftar komponen bersama dan urutan kelompok layar; cek bahwa layar baru dan lama, termasuk bantuan, tidak hilang dari inventaris. Tidak perlu merombak warna atau tipografi hanya demi pembaruan visual.

### 2. Komponen bersama dan validasi

- [ ] Tulis atau perbarui tes yang membedakan kolom belum disentuh, tidak valid setelah blur, valid setelah perbaikan, dan Simpan dengan kolom belum disentuh. Pastikan penyimpanan ditolak untuk data invalid dan dijalankan sekali untuk data valid; uji juga keyboard, fokus dan scroll ke kesalahan pertama.
- [ ] Telusuri semua pemanggil `MankeuSheet`, `FullPageForm`, field dan form yang membuat validasi sendiri. Terapkan perilaku DEC-042 tanpa menyamakan kesalahan data dengan status proses, informasi netral atau konflik impor. Pertahankan pencegahan simpan ganda saat proses berjalan.
- [ ] Sesuaikan tes lama DEC-034/040 dengan kontrak baru. Pastikan teks merah tidak tampil pada form kosong yang belum disentuh, dan pembaca layar menerima label serta pesan yang terkait dengan kolom.

### 3. Layar perwakilan dan alur lengkap

- [ ] Pada stok, uji tiga pilihan, dialog dengan margin di lebar ringkas, stok saja, belanja, Kembali, kuantitas dan catatan. Pada satuan bahan dan produk, uji opsi daftar, tombol tambah di sebelah dropdown, satuan kustom dan penyimpanan.
- [ ] Pada kategori, uji tambah kategori, validasi, Tetap/Variabel, penjelasan modal barang serta perubahan pengaturan yang tersimpan. Pastikan perubahan kategori tidak diam-diam mengubah arti laporan tanpa bukti domain dan pengujian finansial.
- [ ] Pada nota, uji pilihan pembayaran dan penolakan jumlah berlebih; pada Produk, Pengaturan dan Pusat Bantuan pastikan pola acuan tidak rusak. Tinjau screenshot keadaan awal, pilihan aktif, form invalid, keyboard dan hasil tersimpan.
- [ ] Setelah bukti lulus, lanjutkan ke kelompok lain tanpa meminta persetujuan ulang. Bila hasil berlawanan dengan asumsi visual atau angka, perbaiki penyebab dan catat keterbatasan; jangan menutup audit hanya karena layar berhasil dikompilasi.

### 4. Seluruh aplikasi dan perangkat

- [ ] Terapkan pola yang disepakati per kelompok: Beranda dan navigasi; Catatan dan transaksi; Produk, resep dan stok; kategori dan kontak; nota dan tagihan; Rencana dan laporan; pengaturan, cadangan, impor/ekspor dan bantuan. Setiap kelompok mencakup keadaan kosong, isi, loading, error dan aksi relevan.
- [ ] Pada satu AVD, catat ukuran pixel, dpi, ukuran dp, tema, font dan screenshot untuk lebar ringkas dan ponsel umum. Uji font besar yang benar-benar didukung; bila target 200% tidak didukung sistem image, catat kegagalan dan gunakan konfigurasi sistem yang mendukung target, tanpa membuat banyak AVD tersisa. Kembalikan konfigurasi dan tampilkan emulator agar pemilik dapat memantau.
- [ ] Jangan jalankan instrumentasi atau penghapusan data pada `emulator-5556` atau perangkat yang berisi data pengguna. Gunakan serial AVD sintetis secara eksplisit. Uji HP fisik hanya bila tersedia dan tanpa menghapus datanya. Uji TalkBack, target sentuh, keyboard dan mode terang/gelap; catat tablet sebagai belum diverifikasi bila tidak diuji.

### 5. Penutupan per kelompok dan gerbang MVP

- [ ] Jalankan tes terarah dahulu, lalu tes JVM, lint, build dan tes workflow yang sesuai dengan perubahan; jelaskan setiap kegagalan atau pengecualian. Periksa impor/ekspor, pencatatan, stok dan laporan bila perilaku terkait berubah.
- [ ] Perbarui panduan terdekat dan screenshot bila alurnya berubah; catat keputusan material dalam ADR dan perubahan pengguna di changelog. Perbarui WIP dan rencana induk dengan bukti, batas dan pekerjaan berikutnya. Stage hanya path paket dan log yang terkait, commit secara atomik, lalu push branch.
- [ ] Audit UI selesai hanya jika inventaris seluruh layar telah ditinjau dan bukti tiap kelompok tersedia. MVP matang/siap produksi hanya jika seluruh gerbang produk, keuangan, migrasi, penyimpanan, aksesibilitas, perangkat dan keamanan dalam `docs/QUALITY_GATES.md` terpenuhi; audit UI atau screenshot saja tidak cukup.
