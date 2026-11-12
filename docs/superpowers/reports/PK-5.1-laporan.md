# PK-5.1 — Bahasa awam layar lama

**Status: selesai untuk gerbang kosakata PK-5.1.**

## Hasil

- [terverifikasi] Daftar dasar `app/src/test/resources/kata_terlarang_dasar.txt` berubah dari 242 pasangan pelanggaran yang tercatat di HEAD menjadi kosong (0 byte, 0 baris).
- [terverifikasi] `ScreenLanguageTest` memindai nilai `strings.xml` Bahasa Indonesia dan Inggris. Setelah daftar dasar dikosongkan, tidak ada kata terlarang yang tersisa di luar pengecualian eksplisit tes.
- [terverifikasi] Istilah layar lama diganti mengikuti Lampiran A §2 dan P4 audit UI/UX: antara lain HPP menjadi “Modal barang”, BEP menjadi penjelasan penjualan yang menutup biaya, nominal menjadi “Jumlah”, dan bahasa impor/ekspor menjadi tindakan yang menyebut Excel.
- [terverifikasi] Teks bantuan yang ikut dipindai diperbarui pada sumber `docs/guide/help_strings.py`; generator dijalankan kembali sehingga blok bantuan pada kedua `strings.xml` tetap dibuat dari sumber yang benar.
- [terverifikasi] Tidak ada perubahan kode domain, penyimpanan, hitungan uang, kapabilitas, schema, atau tata letak.

## Verifikasi

- [terverifikasi] `python -I docs/guide/help_strings.py` selesai dan menghasilkan 103 string bantuan.
- [terverifikasi] Perintah terfokus `$env:Path = ($env:Path -split ';' | Where-Object { $_ -notmatch '"' }) -join ';'; ./gradlew.bat :app:testDebugUnitTest --tests com.mankeu.presentation.ScreenLanguageTest` lulus. Hasilnya 7 tes selesai, 0 gagal; ini bukan suite unit penuh.
- [terverifikasi] Berkas daftar dasar kosong; `git diff --check` lulus.
- [terverifikasi] Gradle memproses sumber daya Android pada tes terfokus tanpa galat XML.

## Batas pemeriksaan

- [belum] Tidak menjalankan suite penuh, lint, assemble, emulator, atau tangkapan layar; paket ini memerlukan gerbang kosakata saja.
- [belum] Tidak memeriksa pembungkusan teks pada berbagai ukuran layar/font, TalkBack, maupun uji pemahaman pengguna.
- [belum] Artikel bantuan belum diaudit ulang sebagai alur panduan lengkap. Penulisan ulang isi bantuan untuk alur baru tetap ruang PK-5.2.
- [belum] Perubahan belum dinilai oleh pemilik usaha atau pengguna lapangan.

PK-5.1 menutup T5 pada tingkat gerbang otomatis kosakata. Klaim ini tidak menyatakan seluruh teks sudah lolos uji kegunaan dengan pengguna.
