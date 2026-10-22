**Berlaku sejak:** 22 Oktober 2026 · **Aplikasi:** Mankeu (`com.mankeu`) · **Pengembang:** Ade Surya Ananda (AdsurKasur)

> **Prinsip utama.** Mankeu bekerja *offline* dan menyimpan data di HP Anda sendiri. Catatan kas, rencana usaha, stok, dan laporan Anda tidak pernah dikirim ke server kami. Aplikasi ini tidak memuat iklan, tidak memakai pelacak, dan tidak mengumpulkan telemetri.

## 1. Ringkasan

Mankeu adalah buku kas dan rencana usaha untuk usaha mikro, kecil, dan menengah. Kebijakan ini menjelaskan data apa yang ada di aplikasi, di mana data itu disimpan, dan bagaimana Anda menghapusnya.

## 2. Data yang disimpan di HP Anda

Semua yang Anda masukkan disimpan di basis data lokal (SQLite/Room) di HP Anda, antara lain:

- usaha, cabang, dan unit usaha;
- catatan uang masuk dan keluar, rencana usaha (produk, biaya, modal), dan stok barang;
- riwayat impor dan pengaturan aplikasi.

Data ini **tidak pernah dikirim, disimpan, atau dianalisis di server milik pengembang**. Pencadangan otomatis Android untuk data aplikasi juga dimatikan.

## 3. Masuk dengan Google, cadangan, dan sinkronisasi Google Drive (opsional)

- **Masuk dengan Google** hanya dipakai bila Anda memilih mencadangkan atau menyinkronkan data lewat Google Drive Anda sendiri. Nama dan email dipakai untuk menampilkan status akun di Pengaturan. Masuk dengan Google membuat akun masuk di Firebase Authentication milik pengembang; akun itu hanya berisi identitas Google Anda, tanpa data keuangan.
- **Enkripsi di HP.** Berkas cadangan (`.aabk`) dienkripsi di HP Anda dengan AES-256-GCM memakai kata sandi yang Anda tentukan, sebelum diunggah. Kata sandi tidak disimpan aplikasi dan tidak bisa dipulihkan.
- **Sinkronisasi antar-HP (bila Anda nyalakan).** Perubahan data dikirim ke Google Drive Anda dan diambil oleh HP lain yang masuk dengan akun Google yang sama. Setiap kiriman dienkripsi di HP dengan AES-256-GCM memakai kata sandi sync yang Anda tentukan. Kata sandi itu disimpan di HP dalam bentuk terenkripsi oleh Android Keystore, tidak pernah dikirim, dan tidak bisa dipulihkan. Setiap HP diberi pengenal acak yang hanya dipakai untuk menamai berkas sync di Drive Anda.
- **Folder aplikasi terpisah (`drive.appdata`).** Cadangan dan berkas sync disimpan di folder khusus aplikasi di Google Drive Anda. Aplikasi tidak bisa membaca berkas lain di Drive Anda, dan pengembang maupun pihak lain tidak punya akses ke cadangan atau kata sandinya.

## 4. Berkas Excel (.xlsx) dan PDF

- Impor dan ekspor Excel serta ekspor PDF diproses **sepenuhnya di HP Anda**, tanpa mengunggah berkas ke internet.
- Aplikasi hanya membuka berkas yang Anda pilih sendiri lewat pemilih berkas Android.

## 5. Tanpa iklan dan tanpa pelacak

- Aplikasi bebas iklan.
- Aplikasi **tidak memuat SDK analitik atau pelacak** (misalnya Google Analytics, Firebase Analytics, atau Facebook SDK).
- Data penggunaan Anda tidak dijual atau dibagikan kepada pihak mana pun.

## 6. Izin perangkat

| Izin | Kegunaan |
| --- | --- |
| `INTERNET` | Hanya saat Anda masuk dengan Google, membuat dan memulihkan cadangan di Google Drive, atau saat sinkronisasi antar-HP menyala. |
| `ACCESS_NETWORK_STATE` | Dipakai pustaka Google (Sign-In dan Firebase) untuk memeriksa koneksi. |
| Pemilih berkas Android | Memilih berkas Excel atau cadangan untuk impor, ekspor, dan pemulihan. Tidak perlu izin penyimpanan. |

## 7. Penghapusan data

- **Hapus satu catatan:** ketuk Hapus pada kartu catatan.
- **Hapus semua data:** Pengaturan › *Hapus semua data*, lalu ketik HAPUS. Semua usaha, unit, catatan, rencana usaha, stok, riwayat impor, salinan pemulihan otomatis, dan nama lokal di HP dihapus. Bila Anda masuk dengan Google, opsi yang sama juga menghapus semua cadangan dan berkas sinkronisasi Mankeu di folder aplikasi Google Drive Anda, menghapus akun masuk Mankeu (Firebase Authentication), dan mencabut izin Google Drive. Berkas lain di Google Drive Anda tidak tersentuh.
- **Tanpa membuka aplikasi:** hapus data aplikasi lewat Setelan Android › Aplikasi › Mankeu › Penyimpanan, atau copot aplikasi. Cadangan di Google Drive dapat dihapus lewat Google Drive di web: Setelan › Kelola aplikasi › Mankeu › Hapus data aplikasi tersembunyi.
- **Permintaan lewat email:** bila cara di atas tidak bisa dipakai, kirim email ke alamat kontak di bawah dari alamat Google yang Anda pakai untuk masuk. Kami akan menghapus akun masuk Anda (Firebase Authentication). Data keuangan Anda tidak pernah sampai ke server kami, jadi tidak ada data lain yang perlu kami hapus.
- **Mencabut akses Google:** keluar dari akun di Pengaturan kapan saja, atau cabut izin Mankeu di halaman keamanan Akun Google Anda.

## 8. Privasi anak

Mankeu ditujukan untuk pelaku usaha dan pengguna dewasa (18 tahun ke atas). Aplikasi tidak dengan sengaja mengumpulkan informasi dari anak-anak.

## 9. Perubahan kebijakan

Perubahan kebijakan ini diumumkan dengan memperbarui tanggal di bagian atas halaman. Riwayat lengkapnya tersimpan di repositori proyek.

## 10. Kontak

- **Email:** [adsurkasur.dev@gmail.com](mailto:adsurkasur.dev@gmail.com)
- **Pengembang:** Ade Surya Ananda (AdsurKasur)
- **Repositori:** [github.com/adsurkasur/Mankeu](https://github.com/adsurkasur/Mankeu)
