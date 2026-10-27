# Peta Dokumentasi Mankeu

Dokumen di folder ini menggambarkan aplikasi sebagaimana adanya (Room schema 12, versi 0.1.0). Dokumen lama yang sudah tidak berlaku ada di [`archive/`](archive/README.md).

## Mulai dari sini

| Dokumen | Isi |
| --- | --- |
| [`../README.md`](../README.md) | Ringkasan produk, fitur, dan cara build |
| [`../AGENTS.md`](../AGENTS.md) | Aturan wajib untuk agen dan kontributor |
| [`PROJECT_GUIDE.md`](PROJECT_GUIDE.md) | Tujuan produk, pengguna, keadaan sekarang |
| [`WORK_IN_PROGRESS.md`](WORK_IN_PROGRESS.md) | Pekerjaan yang sedang berjalan (dokumen hidup) |

## Produk dan domain

| Dokumen | Isi |
| --- | --- |
| [`PRODUCT_REQUIREMENTS.md`](PRODUCT_REQUIREMENTS.md) | Kebutuhan fungsional dan non-fungsional |
| [`V1_IMPLEMENTATION_SPEC.md`](V1_IMPLEMENTATION_SPEC.md) | Cakupan V1: alur, laporan, kontrak Excel dan cadangan |
| [`DOMAIN_MODEL.md`](DOMAIN_MODEL.md) | Entitas, aturan koreksi, kebijakan jurnal |
| [`diagrams/`](diagrams/README.md) | ERD, arsitektur, use case, alur, state, sequence (Mermaid) |
| [`design/`](design/) | Dokumen desain fitur (rencana usaha dan BEP) |
| [`UX_AND_ACCESSIBILITY.md`](UX_AND_ACCESSIBILITY.md) | Prinsip UX, warna, sentuhan, tooltip, getar |
| [`ROADMAP.md`](ROADMAP.md) | Status workstream dan langkah berikutnya |

## Teknis

| Dokumen | Isi |
| --- | --- |
| [`ARCHITECTURE.md`](ARCHITECTURE.md) | Stack, lapisan, cadangan, sinkronisasi |
| [`DATABASE_MIGRATIONS.md`](DATABASE_MIGRATIONS.md) | Riwayat skema dan kompatibilitas cadangan |
| [`SYNC_ARCHITECTURE.md`](SYNC_ARCHITECTURE.md) | Sinkronisasi antar-HP (fitur rilis, DEC-024) |
| [`CLOUD_SYNC_AND_AUTH.md`](CLOUD_SYNC_AND_AUTH.md) | Firebase Auth dan cadangan Google Drive |
| [`QUALITY_GATES.md`](QUALITY_GATES.md) | Standar bukti dan pengujian |
| [`MANUAL_TEST_GUIDE.md`](MANUAL_TEST_GUIDE.md) | Skenario uji manual dengan angka yang diharapkan |

## Keputusan

- `DEC-001` dan seterusnya: [`../logs/DECISION_LOG.md`](../logs/DECISION_LOG.md).
- `D-001` sampai `D-087` (2026-08 s.d. 2026-09): [`DECISION_LOG.md`](DECISION_LOG.md), dengan daftar yang sudah digantikan di bagian atasnya.

## Rilis dan Play Store

| Dokumen | Isi |
| --- | --- |
| [`PLAY_STORE_DEPLOYMENT.md`](PLAY_STORE_DEPLOYMENT.md) | Pipeline rilis dan penandatanganan |
| [`PLAY_CONSOLE_ENTRY_GUIDE.md`](PLAY_CONSOLE_ENTRY_GUIDE.md) | Isian Play Console langkah demi langkah |
| [`STORE_LISTING_AND_POLICIES.md`](STORE_LISTING_AND_POLICIES.md) | Teks listing, data safety, aset |
| [`privacy-policy/`](privacy-policy/README.md) | Kebijakan privasi (ID/EN), sumber halaman adsurkasur.my.id/mankeu/privacy-policy |
| [`releases/`](releases/) | Catatan rilis |
| [`playstore/`](playstore/), [`assets/store/`](assets/store/) | Grafik dan tangkapan layar toko |

## Pengguna dan bantuan

- [`panduan/PANDUAN_PENGGUNAAN_MANKEU.md`](panduan/PANDUAN_PENGGUNAAN_MANKEU.md): panduan penggunaan untuk pemilik usaha.
- [`simulasi/SKENARIO_SIMULASI_DEMO.md`](simulasi/SKENARIO_SIMULASI_DEMO.md): naskah simulasi demo dengan angka yang harus muncul di layar.
- [`guide/help_strings.py`](guide/help_strings.py): sumber teks tur dan Pusat Bantuan (ID/EN).

## Riset dan bukti

| Dokumen | Isi |
| --- | --- |
| [`RESEARCH_AND_VALIDATION.md`](RESEARCH_AND_VALIDATION.md) | Rencana validasi dan riset |
| [`EVIDENCE_INDEX.md`](EVIDENCE_INDEX.md) | Indeks bukti |
| [`GOVERNANCE.md`](GOVERNANCE.md) | Tata kelola kontribusi |
| [`audit/`](audit/) | Audit dan uji 2026-10 beserta rencana perbaikannya; audit pra-rilis produksi: [`audit/2026-10-22_AUDIT_PRA_RILIS.md`](audit/2026-10-22_AUDIT_PRA_RILIS.md) |
| [`evidence/`](evidence/) | Simulasi lintas usaha, skema 10, dan skala UMKM mikro–kecil–menengah; generator palet warna |

## Merawat dokumen

- Dokumen yang tidak lagi menggambarkan aplikasi dipindah ke `archive/` dengan `git mv`, diberi spanduk "Dokumen riwayat", dan dicatat di [`archive/README.md`](archive/README.md).
- `py -I scripts/check_doc_links.py` gagal bila ada tautan Markdown ke berkas yang tidak ada.
