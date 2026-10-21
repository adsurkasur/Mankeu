# Kebijakan Privasi Mankeu

`id.md` dan `en.md` adalah **satu-satunya sumber** kebijakan privasi Mankeu (DEC-021).

Halaman publik <https://adsurkasur.my.id/mankeu/privacy-policy> (repo situs `adsurkasur-aura`, `src/app/mankeu/privacy-policy/`) membaca kedua berkas ini langsung dari cabang `main` repo ini di GitHub dan memperbaruinya paling lama sekitar **15 menit** setelah push (cache GitHub ±5 menit + jadwal situs 10 menit). Situs tidak perlu di-deploy ulang.

## Mengubah kebijakan

1. Ubah `id.md` dan `en.md` bersamaan; isinya harus setara.
2. Perbarui tanggal di baris pertama ("Berlaku sejak" / "Effective") bila isinya berubah.
3. Commit dan push ke `main`. Push ini **langsung tayang ke publik** dalam sekitar 15 menit, tanpa tahap review di situs. Perlu tayang saat itu juga? Redeploy proyek adsurkasur-aura di dashboard Vercel.
4. Bila kebijakan menyebut fitur baru (misalnya izin atau penghapusan data), samakan juga isian data safety di `../STORE_LISTING_AND_POLICIES.md` dan `../PLAY_CONSOLE_ENTRY_GUIDE.md`.

## Format

Markdown biasa (GitHub-flavored): paragraf, `##` judul bagian, daftar, tabel, kutipan, tautan. Judul halaman, tombol bahasa, dan kotak identitas aplikasi dibuat oleh situs, jadi tidak perlu ditulis di sini. Jangan memakai HTML mentah.
