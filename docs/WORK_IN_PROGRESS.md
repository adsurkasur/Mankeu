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

**Progres otomatis (2026-11-12 02:30):** Keputusan total kas (DEC-033) selesai. [terverifikasi] Dokumentasi kontrak; belum ada perubahan angka/kode pada keputusan ini. Implementasi serta tes rekonsiliasi pada PK-0.5; keputusan K lain yang kosong tetap terhalang. Lingkup pemilik: lanjut seluruh paket MVP yang tidak terhalang keputusan; tetap di fase2-nota-piutang, tanpa main/superproject.

**Progres otomatis (2026-11-12 02:27):** PK-0.6 selesai. [terverifikasi] RED/GREEN/probe; 155 tes JVM/0 gagal, assembleDebug dan diff --check hijau. Pemeriksaan XML tidak menggantikan uji pengguna. PK-0.4 sedang mengukur emulator; PK-0.7 berikutnya. Daftar dasar 243 pasangan harus dikosongkan pada PK-5.1. Lingkup pemilik: lanjut seluruh paket MVP yang tidak terhalang keputusan; tetap di fase2-nota-piutang, tanpa main/superproject.

**Mulai di sini (2026-11-12 02:05):** rencana kerja sekarang `docs/superpowers/plans/2026-11-12-rencana-induk-mvp.md`
(lampiran A spek layar, B paket kerja, C rencana uji, D prompt eksekusi). Rencana 2026-11-09 di bawah ini digantikan
urutannya. Keadaan: rencana ditulis, belum ada paket yang dikerjakan. Menunggu pemilik: tabel 4.16 (K-1 sampai K-15);
K-1 sampai K-4 menghalangi Tahap 1. Bisa dikerjakan tanpa jawaban: PK-0.3, PK-0.4, PK-0.6, PK-0.7, PK-0.8,
PK-3.1. PK-0.1 dan PK-0.2 (hotfix INTERNET, `f8a9bee` di `main`) selesai; K-1 sampai K-4 dan K-15a sudah dijawab. PK-0.3 selesai ditinjau (2026-11-12 02:14): [terverifikasi] review statis di docs/superpowers/reports/PK-0.3-review.md, 4 Important; tanpa perubahan kode atau eksekusi tes. F2 menunggu pilihan pemilik tentang total kas nyata versus operasional. Paket berikutnya sesuai urutan: **PK-0.4**; perbaikan temuan B/N pada PK-0.5 setelah kedua review.

**Mulai di sini (2026-11-09 20:30):** `docs/superpowers/plans/2026-11-09-rencana-lanjutan-lengkap.md` adalah rencana serah terima
lengkap untuk semua sisa pekerjaan. Paket A (review U5), Paket N (stabilitas jitter + bug bash), dan Paket B (U6 Catatan terpadu) selesai. Pekerjaan aktif berikutnya:
**Paket C — U7: Rencana Usaha tanpa periode (spec #14)**.

**Riwayat Fase UX:** U1, U2, U3, U4 selesai. U5 direview/diperbaiki di Paket A. Paket N (stabilitas jitter & bug bash) selesai. Paket B (U6 Catatan terpadu) selesai. Berikutnya U7 (Paket C).

**Sekarang (2026-10-29):** perapihan temuan Fase 1 (P1–P10) selesai, direview, dan sudah di main
(`docs/superpowers/plans/2026-10-29-fase1-perapihan.md`, laporan `...-perapihan-laporan.md`). **Fase 2:** keputusan pemilik F2-1..F2-7 dicatat di spec (bagian "Keputusan Fase 2"); rencana tertulis di
`docs/superpowers/plans/2026-10-29-fase2-nota-piutang.md` (8 tugas, schema 14; 12 keputusan penulis rencana menunggu
tinjauan pemilik, masuk DEC-029). Eksekusi di branch `fase2-nota-piutang`: Task 1 (matematika domain) selesai dan
direview; Task 2 (schema 14, migrasi F2-4, jurnal piutang/utang, BackupUpgrade 13→14, DEC-029) selesai dan direview;
Task 3 (repository kontak, nota, pembayaran, belanja tempo, titip; stok dari `sale_lines`; aturan M3/M4 di DEC-029)
selesai dan direview; Task 4 (untung saat terjual di ringkasan, `actualMonth`, Beranda/Laporan, PDF/xlsx) selesai,
ditambah fix round F2-8 (kategori "Biaya produksi (sudah dihitung di HPP)") dan fix round 2 (Komparasi tetap basis kas,
BEP memuat biaya variabel non-HPP), selesai dan direview. Task 5 (layar Kontak/pemilih pelanggan, Nota penjualan,
daftar nota + detail di Catatan, kartu Tagihan di Beranda, form uang masuk tanpa produk) selesai, menunggu review
(laporan `.superpowers/sdd/2026-10-29-fase2-nota-piutang/task-5-report.md`). Berikutnya: Task 6.

**Uji alur sebagai pengusaha baru (2026-10-29)**, dicatat per checkpoint di
`docs/audit/2026-10-29_UJI_ALUR_PENGUSAHA.md`. Semua checkpoint (CP1–CP7) selesai.

**Redesign disepakati (DEC-026)**: spec `docs/superpowers/specs/2026-10-29-redesign-spec.md` (20 keputusan),
peta fase `docs/superpowers/plans/2026-10-29-redesign-roadmap.md`. **Fase 0 selesai** dan sudah di main (DEC-027).
**Fase 1 selesai (2026-10-29, DEC-028)** dan sudah di main: produk dengan satuan dasar, satuan jual dan resep
per batch, HPP per satuan jual, stok produk (Produksi, Hitung stok), harga bahan dari belanja terakhir dengan peringatan
HPP, template Excel v5, tab Produk dan editor halaman penuh. Uji emulator CP1 sampai CP9 dan angka verifikasi:
`docs/audit/2026-10-29_UJI_FASE1.md`. Satu crash ditemukan dan diperbaiki saat uji (Rencana Usaha dengan produk dua
satuan). Berikutnya: **tulis rencana Fase 2** (nota multi-produk, piutang, konsinyasi, snapshot HPP per baris nota, laba
saat terjual) di `docs/superpowers/plans/`. Review akhir branch: I-1 sampai I-5 dan M-3, M-4, M-6, M-11, M-12 sudah
diperbaiki (lembar baru **Produksi & Stok** di template v5, DEC-028 §9 diamandemen). Diparkir: bahan arsip kembali
aktif setelah impor ulang; harga bahan bersama dihitung dari belanja seluruh usaha; Back tidak menutup sheet Usaha/Unit
(sudah ada sebelum Fase 1); entri Produksi/Hitung stok yang salah belum bisa dihapus (koreksi lewat Hitung stok;
kandidat Fase 2); nama produk kembar di satu file. Terbuka dari Fase 1: tidak ada tes otomatis untuk dua cabang form catat (produk tanpa
satuan jual, bahan terpilih lalu pindah ke Uang masuk; diperiksa tangan di CP), generator `docs/evidence/make_sims.py`
masih menulis template v4, Gboard asli dan sinkron dua HP lintas versi belum diuji. Rilis internal testing adalah
tindakan pemilik; catatan rilis: "perbarui semua HP yang memakai sinkron" (DEC-028 §7).

Sebelumnya: review run audit dan perbaikannya selesai (laporan: `docs/audit/2026-10-22_AUDIT_PRA_RILIS.md` bagian 11).
Sisa untuk pemilik: bagian 10 laporan itu (uji sync dua HP dengan akun Google nyata, AAB bertanda tangan, Data Safety,
peluncuran bertahap) dan usulan fitur usaha menengah (bagian 8).
