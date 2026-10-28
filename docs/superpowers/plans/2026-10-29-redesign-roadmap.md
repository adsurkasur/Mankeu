# Peta Fase Redesign Mankeu

Spec: `docs/superpowers/specs/2026-10-29-redesign-spec.md` (20 keputusan, nomor `#n` di bawah merujuk ke sana).

Setiap fase menghasilkan app yang bisa dipakai, diuji, dan dirilis ke internal testing sebelum fase berikutnya (#12).
Rencana rinci (tugas bite-sized dengan kode dan tes) ditulis **saat fase itu dimulai**, karena bergantung pada kode
hasil fase sebelumnya. Hanya Fase 0 yang sudah rinci.

| Fase | Isi | Keputusan | Rencana rinci |
|---|---|---|---|
| 0 | Pembersihan dan bug sheet | #1, #8 (Firebase), #13 (sheet pendek), #16 (tur) | `2026-10-29-fase0-pembersihan.md` |
| 1 | Model produk: satuan dasar + satuan jual, bahan, resep per batch, HPP, stok produk, Produksi, peringatan harga bahan | #5, #6, #9, #20 | ditulis saat mulai |
| 2 | Penjualan nota multi-produk, status bayar, piutang, utang pemasok, konsinyasi, pelunasan; laba saat terjual | #2, #3, #7, #17 | ditulis saat mulai |
| 3 | Navigasi baru, form halaman penuh, Rencana tanpa periode, unit tersembunyi, Beranda baru | #4, #13, #14, #15 | ditulis saat mulai |
| 4 | Laporan: laba dengan HPP, arus kas terpisah, rencana vs realisasi, ringkas (tabel/lipat), grafik Canvas, bahasa awam, PDF formal | #3, #19 | ditulis saat mulai |
| 5 | Template Excel v2, backup/sinkron untuk schema baru, backup Drive digabung ke sinkron | #8, #10 | ditulis saat mulai |
| 6 | Kunci app, notifikasi jatuh tempo | #11, #18 | ditulis saat mulai |
| 7 | Pusat Bantuan ditulis ulang, screenshot, panduan, rilis | #16 | ditulis saat mulai |

## Kriteria selesai per fase

- **F0:** sheet tidak tertutup karena geser isi dan tidak memantul saat keyboard; schema hanya 12; tanpa Firebase;
  tanpa tur; semua tes hijau; uji emulator alur CP1–CP6 tetap jalan.
- **F1:** produk keripik (batch Rp2,4 jt → 400 bungkus, dus isi 40) menampilkan HPP Rp6.000/bungkus dan
  Rp240.000/dus tanpa hitungan manual; belanja minyak dengan harga baru memunculkan peringatan HPP; Produksi 1 batch
  menambah stok 400 bungkus.
- **F2:** nota ke Toko A berisi 2 produk, status "belum dibayar", tampil di piutang; pelunasan sebagian dan penuh;
  titip 10 dus → laku 6 → setor → tarik 4; laba bulan itu = penjualan − HPP − biaya, dicocokkan dengan hitungan tangan
  di tes golden.
- **F3:** pengguna baru dari instal bersih bisa membuat usaha, produk, dan nota pertama tanpa bertemu kata "unit",
  "realisasi", atau saklar mode; Rencana Usaha tanpa input tanggal.
- **F4:** laporan 24 bulan data tetap muat dalam ringkasan + tabel tanpa 24 kartu besar; Vico dihapus.
- **F5:** ekspor → impor ulang tanpa kehilangan data untuk semua entitas baru; template lama ditolak dengan pesan.
- **F6:** kunci app menutup isi di daftar app terbaru; notifikasi muncul pada tanggal jatuh tempo.
- **F7:** setiap artikel bantuan punya screenshot dari alur baru.
