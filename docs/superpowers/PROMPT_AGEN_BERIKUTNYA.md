# Prompt untuk agen berikutnya

> **DIGANTIKAN (2026-11-12):** pakai `docs/superpowers/plans/2026-11-12-lampiran-d-prompt-eksekusi.md`. Prompt di
> bawah merujuk rencana 2026-11-09 yang urutannya sudah tidak berlaku.

Salin blok di bawah ini ke agen AI baru (Claude Code, Antigravity, Gemini, Cursor, dan lain-lain). Ganti bagian
`[PAKET]` bila ingin agen mengerjakan paket tertentu; bila dibiarkan, agen mengambil paket berikutnya yang belum
selesai menurut bagian Status rencana.

---

```text
Kamu melanjutkan pengembangan Mankeu, app Android offline (Kotlin, Jetpack Compose, Room) untuk pencatatan kas dan
rencana usaha pengusaha kecil Indonesia. Repo: E:\managed-projects-handoff\mankeu (submodule dari
E:\managed-projects-handoff). Branch kerja: fase2-nota-piutang (sudah di-push; belum di-merge ke main).

Prinsip pemilik: "APP INI HARUS USEFUL." Bukan kompetisi; buat yang logis dan benar-benar membantu. Pemilik baru
mencoba build terakhir: masih banyak bug, banyak jitter (layar bergetar/melompat), dan redesainnya terasa terlalu
brutal. Stabilkan dulu sebelum menambah fitur.

LANGKAH WAJIB SEBELUM MENYENTUH KODE
1. Baca docs/superpowers/plans/2026-11-09-rencana-lanjutan-lengkap.md sampai habis. Itu satu-satunya rencana kerja.
   Bagian 0 (cara kerja, lingkungan, aturan) mengikat.
2. Baca AGENTS.md di root repo mankeu dan di superproject (aturan log, commit, privasi).
3. Baca docs/WORK_IN_PROGRESS.md bagian "Pekerjaan aktif" dan bagian 8 "Status" rencana, untuk tahu paket mana yang
   sudah dan belum selesai.
4. Jalankan `git status` dan `git log --oneline -10`; pertahankan perubahan yang bukan milikmu.

APA YANG DIKERJAKAN
- Kerjakan [PAKET] — bila kosong: paket berikutnya menurut urutan di bagian 3 rencana yang belum berstatus selesai
  (saat ini: Paket A review U5, lalu Paket N stabilitas: jitter + bug bash).
- Untuk Paket N: mulai dengan bertanya ke pemilik daftar bug dan layar yang bergetar (pertanyaan singkat, boleh
  pilihan ganda), sambil menjalankan bug bash sendiri di emulator sesuai langkah di rencana.
- Ikuti alur di bagian 0.1: kerjakan dengan TDD (tes terlihat gagal dulu), tulis laporan ke
  docs/superpowers/reports/<paket>-laporan.md, minta review oleh agen/sesi lain bila tersedia (atau tulis self-review
  jujur yang menyebut apa yang belum diverifikasi), perbaiki temuan yang perlu diperbaiki — jangan diparkir kecuali
  di luar lingkup, dan catat alasannya di register bagian 5.

ATURAN YANG SERING DILANGGAR (sudah pernah merusak sesuatu)
- Gradle hanya dari PowerShell dengan PATH dibersihkan:
  $env:Path = ($env:Path -split ';' | Where-Object { $_ -notmatch '"' }) -join ';'; ./gradlew.bat <tasks>
- Edit log, dokumen, dan strings.xml hanya dengan alat edit atau Python UTF-8; jangan menulis ulang file teks lewat
  PowerShell/heredoc (pernah jadi mojibake dan pernah membuang `\n`). String bantuan diubah di
  docs/guide/help_strings.py lalu `python -I docs/guide/help_strings.py`.
- Uang = Long rupiah bulat; jumlah/kuantitas selalu di-parse dengan TransactionCalculation.parseQuantity /
  sanitizeRupiah (menerima "2,5"; tempel "1.250.000,50" = 1.250.000). Jangan menghitung untung/HPP/tagihan di layar:
  semua angka dari MonthlySummary (aturan "satu angka").
- Ekspor Excel harus bisa diimpor ulang tanpa kehilangan (DEC-012). Perubahan schema: schema 14 belum dirilis, boleh
  diubah lewat Migration13To14 + 14.json + BackupUpgrade + tes.
- Emulator: AVD Medium_Phone; keyboard emulator hanya ketik suara dan kemungkinan menelan tombol Back — jangan
  simpulkan bug Back dari situ; jangan ubah pengaturan emulator tanpa izin pemilik. Tes instrumented menghapus data app.
- `git add` path eksplisit; pesan commit `type(scope): pesan`; jam di log = jam nyata (Get-Date), jangan dikarang;
  setiap commit membawa entri logs/ACTIVITY_LOG.md (paling atas) dan baris Status di rencana; CHANGELOG untuk
  perubahan yang terlihat pengguna; DEC baru di logs/DECISION_LOG.md untuk keputusan arsitektur.
- Push branch setelah progres. Setelah merge ke main: push dan naikkan pointer superproject (git add mankeu + entri
  log root). Rilis hanya ke internal testing dan unggah Play Console dilakukan pemilik.
- Keputusan arah produk (yang mengubah perilaku yang sudah diputuskan pemilik di spec) ditanyakan ke pemilik, tidak
  diputuskan sendiri.

HEMAT KUOTA (pemilik sering kena limit)
- Tes terfokus selama bekerja; suite penuh sekali di akhir paket.
- Jangan membaca screenshot atau file besar berulang; baca diff sebagai file sekali.
- Bila mendekati limit: commit apa pun yang hijau, tulis di Status "berhenti di …", push.

SELESAI BILA
- Matriks verifikasi (bagian 6 rencana) hijau untuk paketmu: unit, lint, build, instrumented, ditambah cek khusus paket.
- Status rencana, WORK_IN_PROGRESS, log, dan CHANGELOG diperbarui; branch di-push.
- Laporan akhir ke pemilik dalam bahasa Indonesia sehari-hari: apa yang berubah, bukti tes, apa yang belum
  terverifikasi, dan keputusan yang perlu pemilik ambil.
```
