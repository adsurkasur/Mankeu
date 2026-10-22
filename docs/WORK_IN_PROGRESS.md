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
- Isi tur berubah → naikkan `Tour.version` agar pengguna lama ditawari sekali.
- Tampilan berubah atau artikel/target baru → `scripts/capture_help_screenshots.ps1` (emulator menyala);
  `GuidesTest` gagal bila ada artikel tanpa gambar.

Merawat panduan penggunaan (`docs/panduan/`):
- Tampilan berubah → `scripts/capture_module_screenshots.ps1` (emulator menyala), lalu periksa gambar dan angka
  yang dikutip di naskah. Keputusan isi (grill Q1–Q11) ada di riwayat Git berkas ini, 2026-10-21.

---

## Pekerjaan aktif: Audit pra-rilis produksi (2026-10-22)

Permintaan pemilik: audit seluruh proyek sebelum rilis; semua docs diperbarui; logika bisnis distress-test dengan
simulasi UMKM nyata (mikro, kecil, menengah) dan pengguna dengan keahlian berbeda (paham vs awam); intuitif harus
benar-benar masuk akal; audit performa dan kode (ponytail-audit).

Keputusan grill (2026-10-22):
- Target: **rilis produksi publik** di Play (sekarang hanya internal testing).
- Sync multi-HP (DEC-013, belum tersambung): **dijadikan fitur rilis**. Desainnya di-grill tersendiri.
- Batas usaha menengah (piutang/utang, DEC-011 kas murni): **dicatat sebagai usulan fitur** untuk diputuskan setelah audit.
- Temuan: **yang jelas langsung diperbaiki + dites**; keputusan produk dikumpulkan untuk grill.

| Tahap | Isi | Status |
|---|---|---|
| 1 | Ponytail-audit (kode) | pindaian selesai, temuan di bawah |
| 2 | Grill desain sync | berjalan |
| 3 | Simulasi UMKM: persona mikro/kecil/menengah × awam/paham; angka dicek manual; uji nyata emulator | belum |
| 4 | Performa: data skala menengah (puluhan ribu transaksi), waktu buka layar, ukuran DB | belum |
| 5 | Perbaikan temuan jelas + test | belum |
| 6 | Sync sebagai fitur rilis (implementasi, test, uji dua emulator bila bisa) | belum |
| 7 | Kesiapan Play produksi (target SDK, Data Safety, listing, versi, ukuran) | belum |
| 8 | Semua docs diperbarui; laporan `docs/audit/2026-10-22_AUDIT_PRA_RILIS.md`; log; commit, push, pointer | belum |

### Catatan langkah

- Ponytail-audit: kode ramping. Temuan: `GetStockSummaryUseCase` tak terpakai (hapus); `RecordStockMovementUseCase`
  pembungkus satu pemanggil (lebur ke repository); dependensi `navigation-compose` tak dipakai (hapus); lapisan sync tak
  tersambung **dan `sync_outbox` tumbuh tanpa batas** (±4 baris per transaksi, tak pernah dikosongkan) → ditangani lewat
  tahap 6.
