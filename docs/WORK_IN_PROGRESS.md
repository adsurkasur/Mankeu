# Work in Progress (living document)

Dokumen ini diperbarui **setiap kali satu langkah selesai**, sebelum langkah berikutnya dimulai, supaya
pekerjaan bisa dilanjutkan bila sesi agen terputus (kejadian 2026-10-21: sesi hilang saat limit,
perubahan schema 10 tertinggal tanpa catatan). Agen berikutnya: baca bagian "Status" dan "Catatan
langkah" dulu, lalu lanjutkan dari langkah pertama yang belum `selesai`.

Setelah pekerjaan di-commit, ringkasannya pindah ke `logs/ACTIVITY_LOG.md`; dokumen ini dikosongkan
untuk pekerjaan berikutnya (riwayat langkah demi langkah tetap ada di Git). Setiap progres yang di-commit
langsung di-push dan pointer superproject diperbarui (instruksi pemilik 2026-10-21).

Merawat panduan dalam aplikasi:
- Ubah teks di `docs/guide/help_strings.py`, lalu `python -I docs/guide/help_strings.py`.
- Tampilan berubah atau artikel/tempat baru (`ShowPlace`) → `scripts/capture_help_screenshots.ps1` (emulator menyala);
  `GuidesTest` gagal bila ada artikel tanpa gambar.

Merawat panduan penggunaan (`docs/panduan/`):
- Tampilan berubah → `scripts/capture_module_screenshots.ps1` (emulator menyala), lalu periksa gambar dan angka
  yang dikutip di naskah. Keputusan isi (grill Q1–Q11) ada di riwayat Git berkas ini, 2026-10-21.

---

## Pekerjaan aktif

**Uji alur sebagai pengusaha baru (2026-10-29)**, dicatat per checkpoint di
`docs/audit/2026-10-29_UJI_ALUR_PENGUSAHA.md`. Semua checkpoint (CP1–CP7) selesai.

**Redesign disepakati (DEC-026)**: spec `docs/superpowers/specs/2026-10-29-redesign-spec.md` (20 keputusan),
peta fase `docs/superpowers/plans/2026-10-29-redesign-roadmap.md`. **Fase 0 selesai** dan sudah di main (DEC-027).
**Rencana Fase 1 tertulis**: `docs/superpowers/plans/2026-10-29-fase1-produk-hpp.md` (7 tugas, schema 13). Bagian
"Keputusan rencana" berisi sembilan keputusan penulis rencana yang menunggu tinjauan pemilik; keputusan itu dicatat
sebagai DEC-028 di Task 2. Eksekusi di branch `fase1-produk-hpp`: Task 1 (HPP/stok domain) dan Task 2 (schema 13,
migrasi, backup, lapisan data, DEC-028) selesai. Berikutnya: Task 3 (stok dari penjualan, harga bahan dari belanja).

Sebelumnya: review run audit dan perbaikannya selesai (laporan: `docs/audit/2026-10-22_AUDIT_PRA_RILIS.md` bagian 11).
Sisa untuk pemilik: bagian 10 laporan itu (uji sync dua HP dengan akun Google nyata, AAB bertanda tangan, Data Safety,
peluncuran bertahap) dan usulan fitur usaha menengah (bagian 8).
