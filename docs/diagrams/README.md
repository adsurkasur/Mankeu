# Katalog Diagram Sistem Mankeu

Diagram arsitektur, basis data, alur pengguna, proses bisnis, dan siklus hidup Mankeu dalam Mermaid. Terakhir diselaraskan dengan kode pada **2026-10-22** (Room schema 12, DEC-011 sampai DEC-024).

| No | Dokumen | Isi | Jenis |
| :-: | :-- | :-- | :-: |
| 1 | [01_entity_relationship_diagram.md](01_entity_relationship_diagram.md) | Schema 12 (tabel sama dengan 11): 17 tabel, foreign key sungguhan dibedakan dari referensi logis | ERD |
| 2 | [02_architecture_diagram.md](02_architecture_diagram.md) | Lapisan dan dependensi, pohon komponen Compose, aliran data reaktif, kerangka sinkronisasi | Flowchart |
| 3 | [03_use_case_diagram.md](03_use_case_diagram.md) | Pemilik usaha dan sistem eksternal terhadap tujuh modul | Use case |
| 4 | [04_class_diagram.md](04_class_diagram.md) | Model domain, kalkulator, repository, DAO, ViewModel | Class |
| 5 | [05_user_flow_diagram.md](05_user_flow_diagram.md) | Navigasi empat destinasi, tiga overlay, Ubah dan Hapus, Batalkan impor | Flowchart |
| 6 | [06_activity_diagrams.md](06_activity_diagrams.md) | Catat, Ubah, Hapus, impor dan Batalkan impor, cadangan dan pemulihan | Activity |
| 7 | [07_state_diagrams.md](07_state_diagrams.md) | Catatan kas, batch impor, buku aktif, barang stok | State |
| 8 | [08_sequence_diagram.md](08_sequence_diagram.md) | Catat atau ubah catatan, impor templat, Batalkan impor | Sequence |
| 9 | [09_interaction_feedback.md](09_interaction_feedback.md) | Taksonomi haptic, siklus tooltip, kartu catatan dengan Ubah dan Hapus | Flowchart, sequence, state |

## Melihat diagram

- **Browser:** buka [`preview.html`](preview.html). Halaman merender semua diagram dan menampilkan laporan validasi di bagian atas: jumlah diagram yang lolos parser Mermaid dan pesan galat untuk yang tidak.
- **Membangun ulang pratinjau** setelah mengedit berkas `.md`:

  ```bash
  py docs/diagrams/generate_html_preview.py
  ```

- **Editor:** VS Code dengan ekstensi Markdown Preview Mermaid Support, Android Studio dengan plugin Mermaid, atau [mermaid.live](https://mermaid.live/).

## Aturan penulisan Mermaid

1. Label node yang berisi spasi atau tanda baca selalu diberi tanda kutip ganda, misalnya `repo["FinanceRepository<br/>withTransaction"]`.
2. Pemisah baris memakai `<br/>` di flowchart, bukan `\n`.
3. Setiap `subgraph` memiliki id eksplisit dan `end` yang cocok.
4. Id node tidak memakai kata kunci Mermaid (`end`, `graph`, `subgraph`, `class`, `style`, `click`).
5. Hindari `;`, `#`, dan tanda kutip ganda di dalam label.

## Aturan pemeliharaan

1. Perubahan `Entities.kt` atau migrasi baru wajib memperbarui diagram 1 dan 4.
2. Aturan koreksi baru wajib memperbarui diagram 6, 7, dan tabel "Correction rules" di `docs/DOMAIN_MODEL.md`.
3. Diagram hanya boleh memuat nama kelas dan metode yang benar-benar ada di kode.
