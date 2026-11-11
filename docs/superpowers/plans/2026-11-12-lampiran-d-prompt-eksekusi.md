# Lampiran D: prompt eksekusi (2026-11-12)

Induk: `2026-11-12-rencana-induk-mvp.md`. Salin §1 (aturan tetap) **ditambah** satu blok tahap dari §2 ke agen
baru. Ganti `[PAKET]` dengan nomor paket; bila dikosongkan, agen mengambil paket pertama di tahap itu yang belum
selesai menurut bagian 8 rencana induk dan tidak terhalang keputusan.

Berkas ini menggantikan `docs/superpowers/PROMPT_AGEN_BERIKUTNYA.md` untuk pekerjaan sesudah 2026-11-12.

## 1. Aturan tetap (ditempel di depan setiap prompt)

```text
Kamu melanjutkan Mankeu: app Android offline (Kotlin, Jetpack Compose, Room) pembukuan dan kalkulator usaha untuk
UMKM Indonesia. Repo: E:\managed-projects-handoff\mankeu (submodule dari E:\managed-projects-handoff). Branch kerja:
fase2-nota-piutang. Jangan merge ke main kecuali paketmu memintanya.

BACA DULU (hanya ini, jangan seluruh repo):
1. docs/superpowers/plans/2026-11-12-rencana-induk-mvp.md bagian 3, 4.16, 7, 8.
2. Paketmu di docs/superpowers/plans/2026-11-12-lampiran-b-paket-kerja.md dan berkas yang disebutnya.
3. Bagian Lampiran A (spek layar) dan Lampiran C (uji) yang dirujuk paketmu.
4. AGENTS.md repo. Jalankan `git status` dan `git log --oneline -5`; pertahankan perubahan yang bukan milikmu.

BATAS:
- Kerjakan SATU paket. Jangan mengerjakan paket lain walaupun terlihat dekat.
- Paket bertanda "Butuh K-n": periksa tabel 4.16 rencana induk. Bila kolom "Jawaban pemilik" kosong, BERHENTI,
  laporkan, dan ambil paket lain yang tidak terhalang. Jangan memakai rekomendasi sebagai jawaban.
- Jangan memutuskan arah produk. Hal yang tidak diatur rencana: tulis sebagai pertanyaan pilihan ganda di laporan.
- Jangan menghapus kapabilitas yang sudah ada (spec V-4). Yang diatur ulang hanya kapan ditampilkan.

ATURAN YANG PERNAH MERUSAK SESUATU:
- Gradle hanya dari PowerShell dengan PATH dibersihkan:
  $env:Path = ($env:Path -split ';' | Where-Object { $_ -notmatch '"' }) -join ';'; ./gradlew.bat <tasks>
  Bila macet: ./gradlew.bat --stop
- Log, dokumen, dan strings.xml hanya diubah dengan alat Edit atau Python UTF-8. Jangan menulis ulang berkas teks
  lewat PowerShell atau heredoc (pernah menjadi mojibake dan pernah membuang \n). String bantuan diubah di
  docs/guide/help_strings.py lalu `python -I docs/guide/help_strings.py`.
- Uang selalu Long rupiah. Tidak ada Double atau Float untuk uang. Uang ketikan lewat sanitizeRupiah
  (presentation/components/FormFields.kt). Jumlah lewat TransactionCalculation.parseQuantity (menerima "2,5").
  Pembulatan setengah ke atas, satu kali, di akhir hitungan.
- Satu angka: untung, uang di tangan, belum bayar hanya dari MonthlySummary. Layar dan eksportir tidak menghitung
  sendiri. Butuh angka baru: tambahkan ke MonthlySummary.
- Excel: format milik Mankeu, ketat, lossless (DEC-012). Judul kolom hanya di MankeuTemplate. Tidak menebak kolom.
- Schema: versi tetap 14 (belum dirilis). Perubahan kolom masuk ke Migration13To14, 14.json (dibuat build),
  BackupUpgrade, sinkron, dan tesnya. Jangan membuat schema 15.
- Kata di layar dari Lampiran A dan lewat Vocabulary. Jangan menambah kata dari daftar terlarang (Lampiran C 1.1).
- Tombol utama yang mati wajib punya alasan (saveBlockedReason). Keadaan kosong wajib punya tombol (empty_action).
- Emulator: AVD Medium_Phone, adb di E:/Android/Sdk/platform-tools/adb.exe. Tes instrumented menghapus data app.
  Jangan memakai emulator untuk uji tangan saat tes instrumented berjalan.

CARA KERJA:
1. Tulis tes di butir "Tes dulu" dan lihat gagal. Baru tulis kode.
2. Selama bekerja jalankan tes terfokus saja. Suite penuh (Lampiran C bagian 6) SEKALI di akhir paket.
3. Lakukan butir "Bukti nyata" di emulator. Tes hijau saja belum selesai: tes hijau pernah meloloskan cacat serius.
4. Tulis docs/superpowers/reports/PK-x.y-laporan.md: apa yang diubah, keluaran tes, bukti nyata, apa yang BELUM
   diverifikasi, temuan di luar lingkup. Tandai tiap klaim: terverifikasi / dilaporkan / belum.
5. Perbarui docs/WORK_IN_PROGRESS.md sesudah tiap langkah selesai.
6. Commit: `git add` dengan path eksplisit (jangan -A); pesan `type(scope): pesan`; sertakan logs/ACTIVITY_LOG.md
   (entri baru di atas, jam nyata dari `Get-Date -Format "yyyy-MM-dd HH:mm"`), logs/DECISION_LOG.md bila ada
   keputusan arsitektur, logs/CHANGELOG.md bila terlihat pengguna; akhiri pesan dengan baris Co-Authored-By modelmu.
7. Tambah satu baris di bagian 8 rencana induk. Push branch. Naikkan pointer superproject (git add mankeu + entri
   logs/ACTIVITY_LOG.md root; jangan menyentuh perubahan pemilik yang lain di superproject).
8. Bila kuota hampir habis: commit yang sudah hijau, tulis Status "berhenti di …" dengan langkah berikutnya, push.
```

## 2. Blok per tahap

### Tahap 0: pengaman

```text
TAHAP 0. Paket: [PAKET] (PK-0.1 sampai PK-0.8).
Tujuan tahap: menutup lubang yang sudah diketahui sebelum membangun. Tidak ada layar baru.
Khusus:
- PK-0.2 mengubah main: hanya bila K-15a dijawab A. Commit kecil baru di main, jangan cherry-pick e96144c.
- PK-0.3 dan PK-0.4 adalah review: HANYA BACA. Jangan mengubah kode. Kamu harus sesi atau agen yang berbeda dari
  pembuat Paket B dan N. Tulis temuan dengan berkas:baris, bukti, dan perbaikan konkret.
- PK-0.7 menghapus parameter saveEnabled. Biarkan kompilator menunjukkan setiap pemanggil; beri alasan satu per satu.
```

### Tahap 1: model

```text
TAHAP 1. Paket: [PAKET] (PK-1.1 sampai PK-1.9).
Tujuan tahap: jenis barang, baris nota bebas, modal satu angka, dua cara hitung untung. Belum ada layar baru.
Baca tambahan: rencana induk bagian 3; logs/DECISION_LOG.md DEC-028, DEC-029; docs/DATABASE_MIGRATIONS.md.
Khusus:
- Golden lama adalah pagar: SalesFlowTest, MonthlySummaryTest, ProductCostingTest, OneNumberTest tidak boleh
  berubah angkanya. Bila harus mengubah tes lama, berhenti dan tulis alasannya di laporan.
- FinanceRepository.kt 2.129 baris. Jangan dibaca seluruhnya; cari nama fungsi yang disebut paket.
- sale_lines.productId menjadi boleh kosong. Cari SEMUA pemakainya dan daftar di laporan.
- Usaha lama tetap profitBasis ITEM_COST. Hanya usaha baru yang SIMPLE.
- Catat keputusan arsitektur sebagai DEC-033 (model barang dan baris bebas) dan DEC-034 (cara hitung untung).
```

### Tahap 2: Pintu A

```text
TAHAP 2. Paket: [PAKET] (PK-2.1 sampai PK-2.14).
Tujuan tahap: pengguna awam mencatat penjualan pertama di bawah satu menit tanpa menyiapkan apa pun.
Baca tambahan: docs/audit/2026-11-12_AUDIT_BISNIS_PENGGUNA_AWAM.md bagian 2 (cek-lis 12 butir, mengikat) dan 3.
Khusus:
- Teks, isian, nilai bawaan, pesan tombol mati, dan testTag persis dari Lampiran A. Bila spek kurang, tulis
  pertanyaan di laporan; jangan mengarang kata.
- Layar baru memakai komponen yang ada: FullPageForm, Stepper, MoneyInput, PickerPage, LineRow, MankeuSheet.
  Tidak ada warna di luar Theme.kt.
- Layar lama (SaleNoteScreens, MoneyForms) tetap untuk membuka dan mengubah catatan lama. Jangan dihapus.
- Tiap paket layar: satu kasus tes untuk alasan tombol mati, satu untuk jumlah ketukan (TapCounter).
- Bukti nyata selalu dari instal bersih: adb shell pm clear com.mankeu.
```

### Tahap 3: Pintu B

```text
TAHAP 3. Paket: [PAKET] (PK-3.1 sampai PK-3.8).
Tujuan tahap: kalkulator resep dan kelayakan usaha yang bisa dipakai tanpa satu transaksi, dengan angka yang cocok
dengan hitungan tangan.
Khusus:
- PK-3.1 dan PK-3.4 fungsi murni tanpa Android. Semua golden di Lampiran C bagian 4 harus menjadi tes, angka persis.
- Layar tidak menghitung. Semua angka dari RecipeCalculator, ProductCosting, Feasibility.
- PlanCalculator lama tetap dipakai Rincian analisis dan PDF. Jangan dihapus.
- Kata R/C, B/C, penyusutan hanya di Rincian analisis yang terlipat dan di PDF.
- Bukti nyata PK-3.3: tulis hitungan tangan di laporan, bandingkan dengan layar, baris demi baris.
```

### Tahap 4: Excel dan data aman

```text
TAHAP 4. Paket: [PAKET] (PK-4.1 sampai PK-4.9).
Tujuan tahap: ekspor yang bisa diimpor ulang tanpa kehilangan dan tanpa ganda; template yang dipahami pengguna awam;
pindah HP yang teruji.
Baca tambahan: DEC-012 di logs/DECISION_LOG.md; "Import and export rules" di AGENTS.md; importer/ImportModels.kt.
Khusus:
- Versi template tetap 5. Lembar dan judul kolom hanya di MankeuTemplate.
- Impor tidak menebak. File tanpa penanda, lembar atau kolom hilang atau berganti nama: ditolak dengan daftar
  masalah persis. Pesan per sel: "Lembar X, baris n, kolom Y: tertulis '…'. <cara memperbaiki>."
- Impor tetap satu transaksi, satu batch yang bisa dibatalkan, dengan pemberitahuan.
- PK-4.5 mengubah DEC-012: hanya bila K-13 dijawab. Tulis amandemennya di DECISION_LOG.
- Jangan memasukkan data keuangan nyata ke aset uji.
```

### Tahap 5: rapikan, uji, rilis

```text
TAHAP 5. Paket: [PAKET] (PK-5.1 sampai PK-5.7).
Tujuan tahap: membuktikan kriteria lulus MVP (spec V-5) dan merilis ke internal testing.
Khusus:
- Uji pengguna dan HP nyata dilakukan pemilik. Tugasmu: menyiapkan APK, skrip, lembar (Lampiran C bagian 3 dan 5),
  lalu merangkum hasil ke docs/audit/ tanpa nama peserta dan tanpa rekaman.
- Jangan mengunggah ke Play Console, jangan membuat kunci tanda tangan. AAB dan unggah adalah tindakan pemilik.
- Rilis hanya track internal testing.
- PK-5.6 review akhir: HANYA BACA, oleh model terkuat yang tersedia, sesi berbeda dari para pelaksana.
- Bila kriteria lulus tidak tercapai, laporkan apa adanya. Jangan menyebut MVP lulus.
```

### Blok reviewer (untuk PK-0.3, PK-0.4, PK-1.8, PK-2.12, PK-3.6, PK-4.8, PK-5.6)

```text
PERANMU: reviewer independen, HANYA BACA. Kamu bukan sesi yang menulis kode yang ditinjau.
1. Simpan diff sebagai berkas di folder sementara (git diff BASE..HEAD > berkas) dan baca sekali.
2. Periksa butir "Wajib diperiksa" paketmu, lalu: uang tanpa floating point; satu pembulatan; satu angka; DEC-012;
   migrasi dan cadangan; kehilangan data saat ubah atau hapus; kata terlarang; tombol mati tanpa alasan; jalan buntu.
3. Untuk tiap temuan: tingkat (Critical / Important / Minor), berkas:baris, cara memicunya, akibatnya, perbaikan
   konkret, dan tes yang seharusnya menangkapnya.
4. Jangan menjalankan ulang seluruh suite. Boleh menjalankan satu tes untuk membuktikan satu temuan.
5. Tulis docs/superpowers/reports/PK-x.y-review.md. Sebut juga apa yang TIDAK sempat kamu periksa.
6. Commit hanya berkas review dan log. Jangan mengubah kode.
```
