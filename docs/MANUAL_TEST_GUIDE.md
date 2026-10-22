---
status: current
last_verified: 2026-10-22
audience: owner-testers-and-maintainers
sensitivity: internal
---

# Panduan Tes Manual

Panduan ini menguji build debug Mankeu (Room schema 11), bukan rilis Play Store. Gunakan data rekaan; jangan jadikan build debug satu-satunya tempat catatan keuangan asli.

## 1. Jalankan aplikasi

1. Buka folder `mankeu` di Android Studio dan tunggu Gradle sync selesai.
2. Nyalakan emulator dari **Device Manager**, atau sambungkan ponsel dengan USB debugging.
3. Pilih konfigurasi `app`, lalu Run.

Alternatif: pasang `app/build/outputs/apk/debug/app-debug.apk`. Package `com.mankeu` dengan debug signing bukan identitas rilis.

## 2. Pencatatan kas dan laporan

Buat usaha `Usaha Uji` dan unit `Unit A`. Di buku **Realisasi**, buka **Catatan** dan simpan lima catatan berikut:

| Tombol | Keterangan | Kategori | Jumlah |
| --- | --- | --- | ---: |
| Uang masuk | Servis traktor | Penjualan & Jasa | Rp1.250.000 |
| Uang keluar | Suku cadang | Bahan Baku & Stok | Rp250.001 |
| Uang masuk | Setoran pemilik | Setoran Modal Pemilik | Rp5.000.000 |
| Uang keluar | Ambil untuk dapur | Ambil untuk Pribadi (Prive) | Rp200.000 |
| Uang keluar | Beli mesin bor | Beli Peralatan & Aset | Rp1.000.000 |

Buka **Laporan → Laba Rugi dan Arus Kas** dan cocokkan:

| Baris laporan | Hasil yang benar |
| --- | ---: |
| Pendapatan Operasional | Rp1.250.000 |
| Biaya Operasional | Rp250.001 |
| Laba / Rugi kas | Rp999.999 |
| Arus Kas Operasi | Rp999.999 |
| Arus Kas Investasi | −Rp1.000.000 |
| Arus Kas Pembiayaan | Rp4.800.000 |
| Saldo Kas Akhir | Rp4.799.999 |

Setoran modal dan prive tidak boleh mengubah pendapatan, beban, atau laba. Pembelian aset tidak menjadi beban.

## 3. Ubah dan Hapus

1. Pada kartu `Suku cadang`, ketuk **Ubah**. Formulir terisi dan bertuliskan "Mengubah catatan".
2. Ganti jumlah menjadi Rp300.000 lalu Simpan. Snackbar "Catatan diperbarui." muncul, kartu yang sama berubah (tidak ada kartu baru), dan laba menjadi Rp950.000.
3. Ketuk **Hapus** pada kartu `Ambil untuk dapur`, baca sheet konfirmasi, lalu Hapus. Saldo kas naik Rp200.000 dan Arus Kas Pembiayaan menjadi Rp5.000.000.

## 4. Persistensi dan isolasi lingkup

1. Tutup paksa aplikasi lalu buka lagi. Usaha dan unit terakhir terbuka kembali, semua angka sama.
2. Tambah usaha `Usaha Uji Kedua` dengan unit `Unit B` dan pindah ke sana. Laporan dimulai dari Rp0.
3. Kembali ke `Usaha Uji` → `Unit A`. Catatan awal muncul tanpa tercampur.

## 5. Rencana usaha

Pindah ke buku **Rencana** dan buka **Catatan** (menjadi Rencana Usaha):

1. Atur periode rencana 12 bulan.
2. Tambah produk dengan harga jual, target per bulan, dan satu komponen resep biaya.
3. Tambah satu biaya tetap bulanan.

Beranda dan **Laporan → Kelayakan & BEP** harus menampilkan BEP unit dan rupiah. Bila harga jual di bawah biaya variabel per unit, muncul peringatan bahwa setiap penjualan menambah rugi.

## 6. Stok

1. Buka **Stok**, tambah barang dengan stok awal 10 dan batas minimum 3.
2. Catat **Keluar** 8: stok menjadi 2 dan barang ditandai menipis.
3. Catat **Keluar** 5: ditolak karena stok tidak mencukupi.
4. Catat **Opname** 0: diterima.

## 7. Impor templat dan Batalkan impor

1. Di **Berkas & Data**, ekspor Excel dari `Unit A`.
2. Pindah ke `Unit B` lalu impor berkas tadi. Pratinjau menampilkan jumlah baris dan kategori baru, lalu Simpan.
3. Impor berkas yang sama sekali lagi: pratinjau memberi peringatan duplikat (impor tetap diizinkan). Batalkan pratinjau.
4. Di **Riwayat pengambilan**, ketuk **Batalkan impor** pada batch tadi. Catatan dari impor itu hilang, dan batch tetap tercatat sebagai dibatalkan.
5. Impor berkas Excel lain yang bukan templat Mankeu: harus ditolak dengan daftar lembar atau kolom yang bermasalah.

## 8. Cadangan dan pemulihan

1. Buat cadangan ke berkas dengan kata sandi minimal 8 karakter.
2. Tambah satu catatan baru, lalu pulihkan cadangan tadi. Pratinjau menampilkan jumlah data; setelah dipulihkan, catatan baru itu hilang dan data cadangan kembali.
3. Coba pulihkan dengan kata sandi salah: harus gagal tanpa mengubah data.

## 9. Tampilan tablet

Di perangkat virtual tablet, pastikan navigasi berpindah dari baris bawah ke rail di sisi kiri (lebar 840dp ke atas), kartu tidak saling menimpa, dan formulir, laporan, serta pratinjau impor tetap dapat dipakai.

## 10. Batas kegagalan

Jangan menyatakan build lolos bila terjadi salah satu hal berikut:

- angka berubah setelah restart;
- catatan usaha atau unit lain tercampur;
- setoran modal atau prive menaikkan pendapatan atau laba;
- Ubah membuat kartu baru alih-alih mengganti kartu yang sama;
- data berubah sebelum pratinjau impor disetujui;
- berkas bukan templat diterima;
- aplikasi crash, membeku, atau kehilangan data.

Catat perangkat/API, langkah, hasil yang diharapkan, hasil sebenarnya, tangkapan layar, dan apakah masalah dapat diulang.
