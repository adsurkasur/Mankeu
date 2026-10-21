# MASTER PROMPT HANDOFF: FINALISASI & RILIS APLIKASI "MANKEU"

> **Dokumen riwayat.** Prompt serah terima 2026-10-20. Tugasnya sudah dikerjakan atau digantikan oleh DEC-011 s.d. DEC-019. **Jangan dijalankan lagi.** Keadaan sekarang (Room schema 11): `README.md`, `docs/DOMAIN_MODEL.md`, `docs/V1_IMPLEMENTATION_SPEC.md`, `docs/diagrams/`, dan keputusan di `logs/DECISION_LOG.md`.

Salin seluruh teks di bawah garis pemisah ini dan berikan langsung ke AI Agent baru (Claude Code, Cursor, Gemini CLI, Wilson, dll.) atau pengembang penerus tugas untuk mengeksekusi penyelesaian final aplikasi Mankeu.

---

```text
Kamu adalah Senior Android & Financial Architect yang ditugaskan untuk menyelesaikan rilis final aplikasi "Mankeu" (aplikasi pembukuan dan analisis kelayakan keuangan UMKM berbasis Android Jetpack Compose dan Room Database).

Aplikasi ini ditargetkan final hari ini. Kode dasar telah diaudit secara menyeluruh dan seluruh temuan kesenjangan telah dipetakan.

================================================================================
1. REPOSITORI & INVARIAN BISNIS
================================================================================
- Repositori: Lokal di direktori saat ini.
- Arsitektur: 100% Offline-first, Zero-server dependency, Single Activity dengan Jetpack Compose Material 3, Room Database v7.
- Prinsip Finansial:
  1. Pure Cash Model: Tidak ada utang, piutang, pinjaman, bunga, atau termin kredit. Transaksi hanya 5 jenis kas murni (CASH_INCOME, CASH_EXPENSE, OWNER_CAPITAL, OWNER_DRAWING, ASSET_PURCHASE_CASH).
  2. Nilai Uang: Menggunakan exact integer rupiah (Long). Dilarang keras menggunakan floating point (Float/Double) untuk penyimpanan rupiah.
  3. Formula CVP:
     - Margin Kontribusi Satuan: CM = P - VC
     - Rasio Margin Kontribusi: CMR = CM / P
     - BEP Satuan: BEP_unit = FC / CM
     - BEP Omzet: BEP_rupiah = FC / CMR = BEP_unit * P
     - R/C Ratio = Total Revenue / Total Cost (ambang batas >= 1.0)
     - B/C Ratio = Net Profit / Total Cost (ambang batas >= 0.0)
  4. Navigasi: Tepat 3 Tab utama (Beranda, Catatan, Laporan). Tab Riwayat telah dihapus.
  5. Kerapian Root: File simulasi berada di docs/archive/evidence/verification-workbook/simulasi_kelayakan_umkm_verifikasi.xlsx.

================================================================================
2. DOKUMEN RUJUKAN UTAMA
================================================================================
Sebelum mengeksekusi kode, baca file-file ini di root:
1. `FINAL_AUDIT_REPORT.md`: Laporan audit menyeluruh atas perubahan dan gap yang ada.
2. `TODO_RELEASE_CHECKLIST.md`: Daftar tugas teknis dengan lokasi baris kode dan draf solusi.
3. `AGENTS.md`: Panduan aturan agen dan kewajiban modular logging di folder logs/.
4. `paket_audit_agentic_ai_mankeu.md`: Standar evaluasi dosen terkait BEP dan stok minimal.

================================================================================
3. TUGAS EKSEKUSI FINALISASI (URUTAN PRIORITAS)
================================================================================

TUGAS 1 (P0 - Perbaiki Broken AndroidTest):
- Buka `app/src/androidTest/java/com/mankeu/data/LedgerIntegrityTest.kt`.
- Hapus ketergantungan pada `dao.getObligationBalances()` dan `repository.settleObligation()` yang menyebabkan error kompilasi skema 7.
- Pastikan `./gradlew compileDebugAndroidTestSources` berhasil 100%.

TUGAS 2 (P0 - Kompatibilitas Restore Backup Lama):
- Buka `app/src/main/java/com/mankeu/backup/EncryptedBackupManager.kt`.
- Pada method `validate(snapshot)` dan `restore()`, izinkan pemulihan backup dari aplikasi versi lama (skema 3..5) yang tidak memuat tabel `stock_items` dan `stock_movements`.
- Jangan melempar exception jika tabel stok tidak ditemukan di backup versi 3-5 (lewati tabel stok jika null). Pastikan unit test backup tetap lolos.

TUGAS 3 (P0 - Tambahkan Hasil BEP & CVP ke Ekspor PDF):
- Buka `app/src/main/java/com/mankeu/exporter/FinancePdfExporter.kt`.
- Di bagian ringkasan asumsi per buku, hitung `BusinessAnalysisCalculator.calculate` dan cetak subseksi "Titik Impas (BEP) & Kelayakan Usaha":
  * BEP Volume (Unit)
  * BEP Nilai (Omzet Rupiah)
  * R/C Ratio & B/C Ratio
  * Status Kelayakan (LAYAK / BELUM LAYAK)

TUGAS 4 (P0 - Hubungkan Sheet Anggaran di Ekspor Excel):
- Buka `app/src/main/java/com/mankeu/exporter/FinanceWorkbookExporter.kt`.
- Di fungsi `export()`, panggil `budgetTable(mode, book, categories)` ke dalam daftar `tables` jika `book.budgets.isNotEmpty()`, agar rincian anggaran (RAB) diekspor ke sheet Excel.

TUGAS 5 (P0 - Tambahkan String Bahasa Inggris yang Hilang):
- Buka `app/src/main/res/values-en/strings.xml`.
- Tambahkan: `<string name="reports_rc_ratio">Revenue-to-Cost Ratio (R/C Ratio): %1$s</string>`.

TUGAS 6 (P1 - Hubungkan Fitur Manajemen Stok Minimal ke UI):
- Periksa `app/src/main/java/com/mankeu/domain/StockManagement.kt` (Use case dan entity sudah ada).
- Di `AppViewModel.kt`, observasi `GetStockSummaryUseCase(repository.stockDao)` dan sediakan action untuk `recordStockMovement`.
- Di `HomeScreen.kt` (atau sheet khusus), tampilkan kartu ringkas "Persediaan & Stok Barang":
  * Total Nilai Persediaan (Rp)
  * Jumlah Barang & Peringatan Stok Menipis (*Low Stock Alert*) jika ada item <= minAlertStock.
  * Tombol aksi untuk melihat stok / mencatat barang masuk / penyesuaian stok opname.
- Pastikan alur input stok ini intuitif bagi UMKM sesuai arahan dosen.

TUGAS 7 (P2 - Pembersihan Kode Basi & Sinkronisasi Docs):
- Bersihkan parameter tidak terpakai `onSettle` di `ReportsScreen.kt`.
- Perbarui `docs/DATABASE_MIGRATIONS.md` dengan entri MIGRATION_5_6 dan MIGRATION_6_7.

================================================================================
4. VERIFIKASI AKHIR & STANDAR SELESAI (DEFINITION OF DONE)
================================================================================
Pekerjaan dinyatakan SELESAI jika dan hanya jika:
1. `./gradlew testDebugUnitTest` lolos 100% tanpa kegagalan (34+ tasks).
2. `./gradlew compileDebugAndroidTestSources` lolos 100% tanpa error kompilasi.
3. `./gradlew assembleDebug` sukses membangun APK.
4. Aplikasi terpasang dan diverifikasi secara visual pada emulator Android (jalankan `run_emulator.bat` jika perlu meluncurkan emulator).
5. File log diperbarui sesuai AGENTS.md:
   - `logs/ACTIVITY_LOG.md` (Catatan sesi)
   - `logs/DECISION_LOG.md` (ADR baru jika ada)
   - `logs/CHANGELOG.md` (Update Unreleased)
```
