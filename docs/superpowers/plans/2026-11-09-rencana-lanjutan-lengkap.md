# Rencana Lanjutan Lengkap Mankeu (serah terima, 2026-11-09)

> **Untuk siapa:** agen atau orang yang melanjutkan pekerjaan tanpa konteks percakapan sebelumnya.
>
> **Peran yang diminta pemilik (2026-11-09):** penyusun rencana bekerja *read-only dan test-only*. Implementasi,
> pengujian, dan revisi dikerjakan agen lain mengikuti dokumen ini. Setiap agen yang selesai satu paket harus:
> - memperbarui baris statusnya di bagian **Status** paling bawah;
> - menulis `logs/ACTIVITY_LOG.md` di commit yang sama;
> - mendorong (push) branch-nya.
>
> Tujuannya supaya pekerjaan bisa dilanjutkan kapan saja bila sesi terputus atau kena limit.
>
> **Prinsip pemilik:** "APP INI HARUS USEFUL, intinya itu." Bukan kompetisi; buat yang benar-benar logis dan membantu
> pengusaha kecil Indonesia (produsen keripik, warung, jasa pertanian).

---

## 0. Cara bekerja (wajib dibaca dulu)

### 0.1 Alur per paket

1. Baca paket di bagian 4, berikut dokumen yang dirujuknya. Jangan membaca seluruh repo.
2. **Implementer** (satu agen): kerjakan dengan TDD (tes ditulis dan terlihat gagal dulu), commit kecil per kelompok,
   lalu tulis laporan `docs/superpowers/reports/<paket>-laporan.md`. Laporan disimpan di repo, bukan di
   `.superpowers/`, karena folder itu di-exclude lokal dan hilang saat serah terima.
3. **Reviewer** (agen lain, read-only): nilai kesesuaian spec dan kualitas, lalu tulis
   `docs/superpowers/reports/<paket>-review.md` dengan temuan Critical/Important/Minor, `file:line`, dan perbaikan konkret.
   Reviewer tidak menjalankan ulang seluruh suite.
4. **Perbaikan:** implementer yang sama memperbaiki semua Critical/Important dan Minor yang murah, dengan tes yang
   terlihat gagal dulu. Minor yang sengaja tidak dikerjakan ditulis di register §5 beserta alasannya. Ini sesuai
   arahan pemilik: temuan yang perlu diperbaiki memang diperbaiki, bukan diparkir.
5. **Re-review singkat** hanya untuk perbaikan yang menyentuh data, uang, sinkron, atau migrasi.
6. Perbarui Status (bagian 8), `docs/WORK_IN_PROGRESS.md`, CHANGELOG, lalu push.

### 0.2 Hemat kuota

Pemilik berulang kali kena limit, jadi efisiensi adalah syarat kerja, bukan saran:
- Satu agen per paket, bukan banyak putaran kecil.
- Jalankan tes terfokus selama bekerja. Suite penuh hanya sekali di akhir paket.
- Jangan membaca screenshot berulang. Satu set per paket, dan hanya yang dibutuhkan.
- Reviewer membaca diff sekali, sebagai file (`git diff BASE..HEAD > file`).
- Bila kena limit, commit dulu apa pun yang sudah hijau, lalu tulis Status "berhenti di …".

### 0.3 Lingkungan (sudah terbukti)

- Repo: `E:\managed-projects-handoff\mankeu`, submodule dari superproject `E:\managed-projects-handoff`.
- **Gradle dari PowerShell dengan PATH dibersihkan.** PATH Git Bash memuat entri `"` yang membuat tes unit gagal:
  `$env:Path = ($env:Path -split ';' | Where-Object { $_ -notmatch '"' }) -join ';'; ./gradlew.bat <tasks>`.
  Bila macet: `./gradlew.bat --stop`.
- Peringatan "SDK XML version 4" dari Gradle tidak berbahaya.
- **Emulator:** AVD `Medium_Phone`, SDK `E:\Android\Sdk`, adb `E:/Android/Sdk/platform-tools/adb.exe`.
  - Nyalakan di latar: `E:/Android/Sdk/emulator/emulator.exe -avd Medium_Phone -no-snapshot-load -no-boot-anim`.
  - Tunggu `adb wait-for-device` dan `getprop sys.boot_completed` = 1.
  - Tes instrumented menghapus data app (diterima).
  - **Keyboard emulator hanya ketik suara (Gboard dimatikan).** Kemungkinan besar keyboard ini menelan tombol Back
    sistem (DEC-030). Jangan menyimpulkan "Back diabaikan" dari emulator ini. Menyalakan Gboard adalah keputusan
    pemilik (belum diberikan); sampai ada izin, uji Back lewat `OnBackPressedDispatcher` di tes, dan di HP nyata.
  - Mengetik koma lewat alat input menghasilkan "%2C". Itu artefak alat, bukan bug.
- **Log dan dokumen hanya diedit dengan alat Edit atau Python UTF-8.**
  - Penulisan PowerShell pernah merusak log menjadi mojibake ("â€”").
  - Skrip shell pernah membuang `\n` di `strings.xml`.
  - String bantuan dibuat oleh `docs/guide/help_strings.py`. Ubah di sana, lalu `python -I docs/guide/help_strings.py`.
- **Commit:** `git add` dengan path eksplisit (jangan `-A`); format pesan `type(scope): pesan`; akhiri dengan baris
  kosong + `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>` bila dikerjakan Claude. Jam di log = jam nyata
  (`Get-Date -Format "yyyy-MM-dd HH:mm"`), jangan dikarang.
- **Push dan pointer:** setelah progres di main, push mankeu lalu naikkan pointer superproject. Di superproject:
  `git add mankeu`, tambah entri `logs/ACTIVITY_LOG.md` root, commit, push. Instruksi pemilik: tanpa perlu bertanya.
  Branch fitur boleh di-push sebagai cadangan.
- **Rilis hanya ke track internal testing Play Console.** Tidak ada rilis produksi. Unggah ke Play adalah tindakan pemilik.

### 0.4 Aturan produk yang mengikat (ringkas; sumber lengkap di dokumen)

- `AGENTS.md` (repo):
  - uang `Long` rupiah bulat;
  - kuantitas `BigDecimal` dengan `TransactionCalculation.parseQuantity` (menerima "2,5");
  - laporan adalah turunan;
  - operasi lokal tanpa jaringan;
  - DEC-012: Mankeu pemilik format Excel, ekspor harus bisa diimpor ulang tanpa kehilangan, impor menolak file asing
    dengan daftar masalah yang persis, atomik, dan bisa dibatalkan.
- Spec: `docs/superpowers/specs/2026-10-29-redesign-spec.md`:
  - keputusan #1–#20;
  - Keputusan Fase 2 F2-1..F2-8 (F2-8 mengoreksi F2-1: yang diganti HPP hanya kategori bertanda
    "Biaya produksi (sudah dihitung di HPP)");
  - Keputusan pasca-audit U-1..U-4.
- Aturan desain form P2 dan P3 serta daftar ganti kata P4: `docs/audit/2026-10-30_AUDIT_UI_UX.md`.
  - Maksimal 4 isian terlihat; pilih, jangan ketik; item berulang = baris ringkas; uang bertitik ribuan;
    hasil hidup di atas Simpan.
  - Halaman penuh bila lebih dari 3 isian.
  - Peran warna: biru = aksi, hijau = masuk/untung, merah = keluar/rugi, kuning-oranye = perlu perhatian.
  - Satu angka utama per kartu.
- P7: **satu angka**. Semua layar dan ekspor membaca `MonthlySummary`; tidak ada layar yang menghitung untung sendiri.
- Keputusan arsitektur: `logs/DECISION_LOG.md` DEC-026 (redesign), DEC-027 (Fase 0), DEC-028 (Fase 1 + addendum
  perapihan), DEC-029 (Fase 2 + addendum), DEC-030 (Fase UX + addendum U3).

---

## 1. Status saat ini (diverifikasi penyusun, 2026-11-09)

| Hal | Keadaan |
|---|---|
| `main` | Fase 0, Fase 1, perapihan Fase 1 (P1–P10). Terakhir `9946f4f` (dan commit dokumen sesudahnya). Sudah di-push, pointer superproject naik. |
| Branch `fase2-nota-piutang` | 31 commit di atas main, HEAD `b1b865b`, sudah di-push sebagai cadangan. **Belum di-merge**, karena template Excel belum membawa nota/kontak/titip sehingga DEC-012 belum terpenuhi (paket G). |
| Fase 2 | Task 1–5 selesai dan direview: matematika nota/untung/titip, schema 14 + migrasi 13→14 + BackupUpgrade, repository nota/bayar/tempo/titip, untung saat terjual (F2-8), layar Kontak/Nota/Tagihan. **Task 6–8 belum** (paket F, G, H). |
| Fase UX (audit 2026-10-30) | U1 komponen form + token, U2 `MonthlySummary`, U3 stok bahan nyata, dan U4 Beranda + navigasi baru (saklar Rencana/Realisasi dihapus, menu "Lainnya") selesai dan direview. **U5 (semua form dirombak) selesai tetapi BELUM direview** (paket A). U6–U9 belum. |
| Tes (dijalankan penyusun di HEAD `b1b865b`) | Unit **139 lulus, 0 gagal**. `lintDebug` dan `assembleDebug` hijau. Instrumented **130 tes, 0 gagal, 2 dilewati**. Catatan: tes hijau tetapi pemilik masih menemukan banyak bug dan jitter saat memakai app (Paket N). |
| Ukuran ketukan tugas harian (U5, emulator) | Jual 2 dus ke Toko A belum dibayar, jatuh tempo 14 hari: **6** (target 6–7). Belanja minyak 10 liter: **3** (harga sama) / **5** (harga baru). Terima bayar: **3**. Produksi: **3**. |

Laporan agen sebelumnya ada di `.superpowers/sdd/2026-10-30-fase-ux/` dan `.superpowers/sdd/2026-10-29-fase2-nota-piutang/`.
Folder itu **lokal saja** (di-exclude git). Isi pentingnya sudah dirangkum di dokumen ini dan di bagian Status
rencana masing-masing fase.

---

## 2. Peta dokumen

| Dokumen | Isi |
|---|---|
| `docs/WORK_IN_PROGRESS.md` | Dokumen hidup. Perbarui di setiap langkah. |
| `docs/superpowers/specs/2026-10-29-redesign-spec.md` | Semua keputusan pemilik. |
| `docs/superpowers/plans/2026-10-29-redesign-roadmap.md` | Fase F0–F7 dan kriteria selesai. |
| `docs/superpowers/plans/2026-10-29-fase2-nota-piutang.md` | Rencana Fase 2 (Task 6–8 belum) beserta Status. |
| `docs/superpowers/plans/2026-10-30-fase-ux.md` | Rencana Fase UX (U5 review, U6–U9 belum) beserta Status. |
| `docs/audit/2026-10-30_AUDIT_UI_UX.md` + `docs/audit/img/ui-20261030-*.png` | Audit 32 layar; P1–P7; target ketukan. |
| `docs/audit/2026-10-29_UJI_ALUR_PENGUSAHA.md`, `docs/audit/2026-10-29_UJI_FASE1.md` | Uji alur sebelumnya. |
| `docs/DATABASE_MIGRATIONS.md`, `docs/DOMAIN_MODEL.md` | Migrasi 12→13→14, model domain. |

---

## 3. Urutan paket

```
A (review U5) → N (stabilitas: jitter + bug bash) → B (U6 Catatan) → C (U7 Rencana) → D (U8 bahasa+bantuan) → F (F2 Task 6 titip+tempo)
→ G (F2 Task 7 template) → E (U9 uji ulang seluruh app) → H (review akhir branch, merge, rilis internal)
→ I (persiapan rilis oleh pemilik) → J (F4 laporan) → K (F6 kunci app + notifikasi) → L (F5 sisa) → M (F7 bantuan + rilis)
```

Urutan ini punya alasan. F (titip, tempo) dibuat sesudah B–D supaya langsung memakai pola form dan daftar yang baru.
G sebelum H karena merge ke main mensyaratkan DEC-012 utuh. E dijalankan sesudah F dan G supaya uji ulang mencakup
semua layar akhir.

---

## 4. Paket kerja

### Paket A — Review U5 (form dirombak)

- **Diff:** `git diff 50b3c7e..b1b865b`. Empat commit: `53ccd09`, `92f00b1`, `fe50f1b`, `b1b865b`.
- **Bahan:** laporan U5 (lokal `.superpowers/sdd/2026-10-30-fase-ux/task-U5-report.md`; salin ke
  `docs/superpowers/reports/U5-laporan.md` bila masih ada) dan screenshot `u5-*.png` di folder yang sama.
- **Yang wajib diperiksa:**
  1. Setiap form dibuka dengan ≤ 4 isian terlihat (P2). Tes `theDailySaleIsFiveTapsWithNoTyping`,
     `ProductEditorTest` (2 kotak saat dibuka), dan `MoneyFormsTest` benar-benar menegaskan hal itu.
  2. **Uang:** "1.250.000,50" → 1.250.000 (sen dibuang). Tidak ada jalur lain yang mem-parse uang tanpa `sanitizeRupiah`.
  3. **Terima bayar per pelanggan** (`FinanceRepository.recordCustomerPayment`, `SalesMath.allocate`): satu transaksi,
     nota terlama dulu, nota bertanggal sesudah pembayaran dikecualikan, lebih bayar ditolak, jurnal Kas/Piutang seimbang.
  4. **Kirim:** teks nota dan pengingat benar; `wa.me` hanya bila ada no. HP, dengan cadangan `ACTION_SEND`; tanpa izin baru.
  5. **"Ubah" catatan lama** membuka halaman yang cocok dan **tidak menghilangkan** field yang tidak ditampilkan
     (referensi, tautan rencana, sifat biaya, produk, umur aset). Ini risiko kehilangan data.
  6. Editor produk: field rencana hanya tampil dari Rencana usaha; data rencana tetap tersimpan saat diedit dari tab Produk.
  7. Baris tanggal "Hari ini ›" bisa diaktifkan TalkBack; target sentuh ≥ 48 dp.
  8. Tes yang dihapus (`EntryFormFallbackTest`, `EntrySheetSwipeTest`) memang hanya menguji sheet lama yang sudah tiada.
- **Sisa "by eye" dari U5 yang harus diperbaiki dalam putaran perbaikan:**
  - judul halaman pelanggan memakai bobot lama;
  - label "Isi (bungkus)" berulang;
  - petunjuk "Belum lunas? Pilih pelanggannya." menggeser segmen.
- **Selesai bila:** review tertulis, perbaikan dengan tes merah dulu, suite penuh hijau, Status diperbarui.

### Paket N — Stabilitas: jitter dan bug bash (laporan pemilik 2026-11-09)

Pemilik mencoba build ini dan menemukan bahwa masih banyak bug, banyak jitter (layar bergetar atau melompat), dan
redesainnya terasa terlalu brutal. Paket ini menstabilkan dulu sebelum fitur baru (B dan seterusnya).

1. **Kumpulkan bug nyata dulu.**
   - Tanyakan ke pemilik daftar bug dan layar yang bergetar (screenshot atau rekaman layar bila ada).
   - Jalankan sendiri semua tugas harian di emulator dengan data "Keripik Bu Sri" (§E), lalu catat setiap bug dan
     jitter di `docs/audit/2026-11-XX_BUG_BASH.md`: langkah reproduksi, hasil, harapan, dan tingkat (Berat/Sedang/Ringan).
   - Simpan setelah setiap temuan.
2. **Jitter: telusuri penyebab dulu, jangan menebak.**
   - Rekam layar: `adb shell screenrecord /sdcard/x.mp4`, lalu tarik.
   - Ukur frame: `adb shell dumpsys gfxinfo com.mankeu reset`, jalankan aksinya, lalu `dumpsys gfxinfo com.mankeu`
     (lihat janky frames).
   - Hitung rekomposisi: Layout Inspector di Android Studio, bila tersedia untuk pemilik. Alternatifnya, log sementara
     di composable yang dicurigai.
   - Tersangka kuat yang sudah diketahui dari riwayat:
     - pengecekan jam sekali per menit yang menyebabkan `MonthlySummary`/state memancarkan ulang (U2 M-4), sehingga
       seluruh Beranda/Laporan berkomposisi ulang;
     - `combine` flow yang memancarkan objek baru walaupun nilainya sama (perlu `distinctUntilChanged`);
     - `LazyColumn` tanpa `key`;
     - petunjuk yang muncul/hilang dan menggeser isi, misalnya "Belum lunas? Pilih pelanggannya." menggeser segmen;
     - `animateContentSize` atau animasi warna di kartu besar;
     - `imePadding` yang bertabrakan dengan inset lain (masalah lama sheet dan keyboard memantul);
     - ResultBar yang tingginya berubah;
     - Stepper atau MoneyInput yang memformat ulang saat mengetik.
   - Perbaiki di akar (state stabil, `remember`/`derivedStateOf`, ruang dicadangkan untuk petunjuk, kunci daftar,
     animasi dihapus bila tidak berguna).
   - Bukti sebelum/sesudah: angka janky frames dan rekaman.
3. **Redesain terasa "brutal".**
   - Catat di bug bash bagian mana yang membuat pemilik kehilangan orientasi: fitur yang hilang, tempat yang pindah,
     label yang berubah.
   - Usulan perbaikannya bisa berupa jalan pintas ke tempat lama, teks pengantar satu kali "Yang baru", atau
     mengembalikan sesuatu yang ternyata dibutuhkan.
   - Keputusan yang mengubah arah produk ditanyakan ke pemilik dengan pertanyaan pilihan ganda, bukan diputuskan sendiri.
4. Setiap bug Berat/Sedang diperbaiki dengan tes yang gagal dulu (bila bisa diuji otomatis) atau bukti emulator, dalam
   commit kecil dengan log.
5. **Selesai bila:** daftar bug bash berstatus beres atau dijadwalkan dengan alasan; jitter yang dilaporkan hilang
   (dibuktikan dengan angka gfxinfo dan rekaman); suite penuh hijau.

### Paket O — Fleksibilitas UMKM (permintaan pemilik 2026-11-12)

> **PERINGATAN PRIORITAS (2026-11-12):** `docs/audit/2026-11-12_AUDIT_BISNIS_PENGGUNA_AWAM.md` menyimpulkan app gagal bagi pengguna awam karena alurnya meminta struktur (produk, resep, batch) sebelum bisa mencatat. Perencana harus **mengurutkan ulang seluruh paket** agar perombakan alur awal dan model ("catat dulu, rapikan nanti", jenis item, baris bebas) mendahului C, D, F, G, dan semua rilis. Paket di bawah ini berlaku sampai perencana menyusun ulang.

> **Diperluas:** `docs/audit/2026-11-12_PERSONA_DAN_DESAIN_UMKM.md` memuat 10 persona UMKM, hasil uji, dan usulan desain D1–D13 dengan prioritas G1–G3. Paket O mencakup G1 (jenis item, baris nota bebas, satuan milik usaha, belanja barang dagangan) dan harus selesai **sebelum Paket G (template) dan sebelum merge/rilis**, karena menyentuh schema 14 dan template v5 yang belum dirilis. G2 dan G3 dijadwalkan oleh perencana.

Rujukan: audit `docs/audit/2026-11-12_AUDIT_MENYELURUH.md` §11 (F1 sampai F5). Kerjakan sesudah Paket C dan sebelum Paket F (supaya titip dan tempo memakai pemilih baru).

1. **F1 satuan milik usaha** (wajib): pemilih berpencarian + "+ Tambah" + saran nama mirip, dibaca dari satuan yang sudah dipakai usaha; ganti daftar tetap di editor produk, tambah bahan, satuan jual, belanja, nota. Tes: satuan ketikan muncul di produk berikutnya; "Karung" vs "karung" menawarkan satuan yang sudah ada; tanpa batas jumlah satuan; migrasi tidak perlu bila dibaca dari data.
2. **F2 produk tanpa stok** (wajib): opsi di editor produk; sembunyikan stok, Produksi, dan peringatan menipis; stok tidak dihitung untuk produk itu; nota tidak memeriksa stok; ekspor/impor membawa opsi ini (DEC-012; kolom di lembar Produk). Perlu kolom di tabel `products` (schema 14 belum dirilis, ubah migrasi 13→14, `14.json`, `BackupUpgrade`, tes).
3. **F3 retur** dan **F4 cara bayar**: tanyakan pemilik dengan pilihan ganda sebelum membangun; tulis keputusan di spec.
4. Tes lintas jenis usaha: skenario warung (jual per porsi, bahan per kg/liter), jasa pertanian (tanpa stok, biaya BBM+upah), sewa mesin (tanpa stok, tagihan per sewa), dan keripik (produksi).

### Paket B — U6: Satu daftar Catatan (keputusan U-4)

- **Tujuan:** tab Catatan menampilkan satu daftar menurut tanggal, dengan:
  - item: nota (dengan status Lunas/Sebagian/Belum dan sisa), pembayaran nota ("Bayar nota Toko A"), belanja,
    uang masuk lain, uang keluar lain, modal/ambil pribadi, produksi (opsional; putuskan dan catat);
  - filter chip: Semua / Belum lunas / Uang masuk / Uang keluar;
  - satu tombol + yang membuka pilihan Jual / Belanja / Uang masuk lain / Uang keluar lain / Modal & ambil pribadi
    (ganti speed dial 5 entri sementara);
  - daftar polos ber-pemisah, aksi lewat ketuk;
  - judul hari ("Hari ini", "Kemarin", tanggal);
  - total kecil per hari (uang masuk/keluar).
- **Data:** gabungan dari `sales` dan `financial_events`.
  - Pembayaran nota tampil sebagai sub-baris atau baris sendiri. Putuskan, tapi jangan dua kali menghitung uang di total harian.
  - Pagination: tetap pakai batas yang ada (`RECENT_EVENT_LIMIT`) atau muat bertahap; jangan memuat ribuan baris
    sekaligus (`ScaleSimulationTest` punya 8.975 baris).
- **Tes:**
  - urutan dan pengelompokan per tanggal;
  - filter;
  - total harian = jumlah uang nyata;
  - setelah menyimpan nota, daftar tidak lagi berkata "Belum ada catatan" (L14 audit);
  - performa dengan data simulasi (< 1 detik render awal di emulator; ukur dan catat).
- **Bahasa:** "Uang masuk lain", bukan "Penjualan", untuk usaha berproduk (L16).

### Paket C — U7: Rencana Usaha tanpa periode (spec #14)

- **Tujuan:** halaman Rencana usaha (sekarang halaman sementara berisi konten lama dengan periode, R/C, BEP) diganti dengan:
  - **Sebulan normal:**
    - per produk: berapa terjual per bulan, dalam satuan jual pilihan (stepper/angka) dengan harga dan HPP dari produk;
    - biaya tetap per bulan (baris ringkas: sewa, gaji, listrik …).
  - **Modal awal:** alat (beli alat) + biaya persiapan.
  - **Hasil:**
    - untung per bulan = Σ(jual × (harga − HPP)) − biaya tetap;
    - "Supaya tidak rugi, jual minimal N dus per bulan", memakai satuan utama dan campuran penjualan yang benar;
    - modal dibutuhkan;
    - balik modal N bulan, hanya bila untung > 0.
  - "Layak" hanya bila biaya tetap terisi (L27).
  - Rincian analisis (R/C, B/C, penyusutan) di lipatan.
  - Kolom periode tidak tampil. Data periode lama dibiarkan di database; tidak perlu migrasi.
- **Data:** pakai buku PROJECTION dan `budget_items` yang ada bila cocok, atau tabel kecil baru bila lebih bersih.
  Bila schema berubah, **schema 14 belum dirilis**, jadi ubah `Migration13To14` + `14.json` + `BackupUpgrade` + tes, dan catat di DEC-030.
- **Komparasi** "Uang masuk/keluar vs rencana" di Laporan disesuaikan ke rencana bulanan (per bulan, bukan per periode).
- **Tes:**
  - golden domain: keripik 50 dus/bulan × (300.000 − 240.000) − biaya tetap 1.200.000 = 1.800.000/bulan;
    BEP 20 dus; modal 3.600.000 → balik modal 2 bulan;
  - "Layak" tidak muncul bila biaya tetap 0;
  - UI ≤ 4 isian per lembar.
- **Template Excel:** lembar Rencana Anggaran mengikuti bentuk baru dan tetap lossless (masuk ke paket G).

### Paket D — U8: Bahasa dan bantuan

- Terapkan tabel P4 audit di semua layar non-laporan (ID + EN). Contoh:
  - "Realisasi" dan "Mode Aktual" dihapus;
  - "Nominal" → "Jumlah";
  - "Opname" → "Hitung ulang stok";
  - "Kategori Pembukuan" → "Kategori";
  - "Impor workbook" → "Ambil dari Excel";
  - "Total nilai persediaan" → "Nilai bahan di gudang";
  - "Arus Kas …" hanya di Laporan, dengan kata awam.

  Perluas `ScreenLanguageTest`: daftar kata terlarang di string non-laporan.
- **Bantuan:**
  - Tulis ulang artikel yang kini salah lewat `docs/guide/help_strings.py`. Artikel itu menjelaskan sheet catat lama,
    saklar Rencana/Realisasi, editor 19 kotak, nota lama, "Penjualan" di uang masuk, dan stok bahan manual.
  - Pastikan setiap artikel sesuai alur U4–U7.
  - `GuidesTest` tetap hijau (artikel tanpa gambar gagal).
- **Screenshot:** jalankan `scripts/capture_help_screenshots.ps1` dan `scripts/capture_module_screenshots.ps1`
  (emulator menyala). Perbarui gambar panduan `docs/panduan/` yang menyebut tab lama, misalnya `31_laporan_target.jpg`.
  `ModuleScreenshotTest` hanya jalan dari skripnya: jalankan, perbaiki langkah-langkahnya untuk UI baru, dan catat hasilnya.
- **Rasio R/C di Beranda/kartu BEP** (melanggar #19, sudah dicatat sejak Fase 2): pindahkan ke Rincian analisis.

### Paket E — U9: Uji ulang seluruh app (sesudah F dan G)

- Ulangi audit L01–L32 dengan data "Keripik Bu Sri", disusun sama seperti audit 2026-10-30:
  - usaha;
  - bahan Singkong (kg Rp30.000) dan Minyak (liter Rp25.000);
  - produk Keripik (bungkus, 1 batch = 400, resep Singkong 50 + Minyak 20 + Bumbu & kemasan 300.000 + Gas 100.000,
    satuan bungkus Rp8.000 dan dus isi 40 Rp300.000);
  - produksi;
  - kontak Toko A, B, Warung C;
  - nota belum/sebagian/lunas;
  - belanja tunai dan tempo;
  - titip ke Toko B lalu laku dan tarik.
- Tulis `docs/audit/2026-11-XX_UJI_ULANG_UX.md` per checkpoint (simpan setelah setiap layar). Untuk tiap layar:
  - tautan screenshot sebelum (audit) dan sesudah;
  - status: beres / masih masalah;
  - jumlah ketukan tugas harian dibandingkan target.
- **Kriteria lulus:**
  - tidak ada temuan "Berat" dari audit yang tersisa;
  - satu angka (P7) di semua layar dan ekspor;
  - target ketukan tercapai;
  - mode gelap rapi;
  - Back dan "Buang perubahan?" bekerja lewat tombol panah.
- Temuan baru diperbaiki dalam paket ini, atau dicatat di §5 dengan alasan.

### Paket F — Fase 2 Task 6: Titip ke toko dan belanja tempo (dengan pola form baru)

- **Rujukan:** `docs/superpowers/plans/2026-10-29-fase2-nota-piutang.md` Task 6, keputusan F2-5 dan F2-6, dan API
  repository yang sudah ada (Task 3):
  - `sendConsignment`;
  - `reportConsignmentSold`;
  - `returnConsignment`;
  - `consignments`;
  - `recordSupplierPayment`;
  - `payables`;
  - belanja `CREDIT_PURCHASE`.
- **UI:**
  - **Titip** (halaman penuh, pola U5):
    - toko dipilih dari kontak (chip terakhir + pemilih);
    - baris produk ringkas "10 dus Keripik · Rp280.000/dus", diedit di lembar (produk, satuan, stepper jumlah, harga setor);
    - Simpan.
  - **Bagian "Dititipkan"** di tab Produk dan di halaman pelanggan (toko):
    - sisa per produk dalam satuan jual terbesar + sisa;
    - **Laku** (lembar: jumlah laku per baris ≤ sisa, dibayar sekarang) → nota;
    - **Tarik** (lembar: jumlah ≤ sisa) → stok kembali.
  - **Belanja:** pilihan "Dibayar nanti" (segmen) → pemasok (chip/pemilih, wajib) + jatuh tempo (chip 7/14/30) →
    `CREDIT_PURCHASE`. Belanja tempo bertaut bahan juga menambah stok bahan (U3).
  - **Beranda:** kartu "Utang ke pemasok" bernama (pola kartu Tagihan U4: 3 teratas, sisa, umur) dengan **Bayar**.
    Halaman pemasok mirip halaman pelanggan.
  - **Detail stok produk:** "Di gudang N · Dititipkan M".
- **Bawaan dari review sebelumnya (wajib):**
  - Form uang lama tidak boleh mengubah `CREDIT_PURCHASE` menjadi tunai diam-diam. Bila sudah ada pembayaran, jenisnya tidak bisa diubah.
  - Titip yang salah bisa dihapus hanya bila belum ada laku atau tarik darinya. Tarik yang salah bisa dihapus.
  - Batas di UI: laku ≤ sisa, tarik ≤ sisa, bayar utang ≤ sisa, tanggal bayar tidak sebelum tanggal belanja.
- **Tes:**
  - golden F2 di emulator dan tes: titip 10 dus → stok −400, dititipkan 400; laku 6 → nota, stok tidak turun lagi;
    setor → lunas; tarik 4 → stok +160, titipan 0;
  - belanja kemasan tempo 300.000 → utang; bayar 100.000 → sisa 200.000, kas hanya −100.000.

### Paket G — Fase 2 Task 7: Template Excel v5 lengkap (DEC-012)

- **Rujukan:** rencana Fase 2 Task 7. Format v5 belum pernah dirilis, jadi lembar dan kolom baru tidak menaikkan versi.
- **Lembar baru** (dalam `MankeuTemplate.REQUIRED`, juga ditulis oleh template kosong):
  - **Kontak:** Nama, Pelanggan, Pemasok, No. HP, Alamat, Catatan, Diarsipkan.
  - **Nota:** No. Nota, Tanggal, Pelanggan, Jatuh Tempo, Diskon, Catatan, Titip, HPP Perkiraan.
  - **Baris Nota:** No. Nota, Produk, Satuan, Jumlah, Harga Satuan, HPP Baris.
  - **Titip:** No. Titip, Tanggal, Toko, Produk, Satuan, Jumlah, Harga Setor.
- **Kolom Transaksi baru:** No. Nota (pembayaran), Kontak, Jatuh Tempo, Melunasi (rujukan belanja tempo), Jenis "Dibayar nanti".
- **Gerakan:** tarik titip masuk ke lembar Produksi & Stok ("Tarik titip"), begitu juga gerakan bahan dan pemakaian
  produksi dari U3, yang sudah ada.
- **Bawaan wajib:**
  - Lembar **Kategori** harus dibaca kembali saat impor: tanda "Biaya produksi (sudah dihitung di HPP)" dan sifat biaya.
    Sekarang lembar itu hanya referensi, sehingga tanda yang diubah tangan hilang saat diimpor ulang (celah DEC-012 yang tercatat).
  - Nota dari form nota harus terekspor. Saat ini hanya pembayarannya, sehingga impor ulang kehilangan nota.
  - Rencana usaha bentuk baru dari paket C.
  - Representasi rujukan (nomor nota/titip/belanja) harus lossless dan bisa divalidasi; catat pilihannya di DEC-029.
- **Validasi:** rujukan harus ada, jumlah laku ≤ titip, pembayaran ≤ total. Pesan per sel.
- **Tes:**
  - round trip data golden (nota, status, sisa titip, utang, untung bulan, stok bahan dan produk) → ekspor → impor ke
    unit baru → semuanya sama;
  - penolakan rujukan rusak;
  - aset `app/src/androidTest/assets/simulasi/*.xlsx` dan generator `docs/evidence/2026-10-22_simulasi_skala_umkm/make_sims.py`
    diperbarui, dengan `expected.json` tetap lulus.

### Paket H — Review akhir branch, merge, push

- Review seluruh branch `fase2-nota-piutang` terhadap main oleh model terkuat, read-only. Fokus:
  - integritas data: migrasi 12→13→14, BackupUpgrade berantai, sinkron (DATA_TABLES urutan induk dulu, `REFERENCES`,
    kunci alami, lantai schema 14, lewati set lama dengan pemberitahuan);
  - hapus semua data;
  - undo impor;
  - satu angka;
  - DEC-012;
  - uang tanpa floating point.
- Satu gelombang perbaikan, lalu re-review singkat.
- Uji alur pengusaha Fase 2 di emulator. Ini Task 8 rencana Fase 2, CP1–CP10:
  - kontak;
  - nota belum dibayar;
  - bayar sebagian, lalu lunas;
  - titip 10 dus, laku 6, setor, tarik 4;
  - belanja tempo dan bayar;
  - laporan untung = hitungan tangan;
  - ekspor dan impor ulang;
  - data Fase 1 tampil sebagai nota lunas "HPP perkiraan".

  Tulis hasilnya di `docs/audit/2026-11-XX_UJI_FASE2.md`.
- Perbarui DOMAIN_MODEL, PRODUCT_REQUIREMENTS, roadmap (F2 dan F3 sebagian besar selesai, sisanya ditulis), DEC-029 dan
  DEC-030 → Implemented, CHANGELOG, WORK_IN_PROGRESS.
- Merge `fase2-nota-piutang` ke main (fast-forward bila bisa), push, naikkan pointer superproject, hapus branch lokal.

### Paket I — Persiapan rilis internal (sebagian tindakan pemilik)

- `versionCode`/`versionName` dinaikkan.
- **Catatan rilis** (Indonesia, bahasa awam):
  1. Perbarui semua HP yang memakai sinkron. HP versi lama menolak data dari versi baru, dan kiriman dari HP lama
     dilewati dengan pemberitahuan.
  2. Stok bahan sekarang dihitung otomatis dari belanja dan produksi. Hitung ulang stok sekali; ada pemberitahuan satu kali.
  3. Penjualan berproduk lama menjadi nota lunas dengan "HPP perkiraan".
  4. Stok produk bisa minus sampai produksi atau hitung stok dicatat.
  5. Template Excel lama ditolak dengan daftar kolom yang berbeda; unduh template baru.
- **Dokumen yang masih menyebut Firebase** (terbuka sejak DEC-027): kebijakan privasi, listing toko, Data Safety,
  `CLOUD_SYNC_AND_AUTH`. Diagram 02/03 dan panduan tur juga basi. Penyusun hanya menyiapkan draf perubahan; keputusan
  dan unggah Play Console oleh pemilik.
- **AAB bertanda tangan** dibuat oleh pemilik (kunci tidak ada di repo).
- **Uji di HP nyata** oleh pemilik:
  - masuk Google dan token Drive;
  - sinkron dua HP;
  - Gboard asli, tombol Back, dan dropdown;
  - Kirim ke WhatsApp (WhatsApp tidak terpasang di emulator).

  Sediakan checklist singkat di `docs/audit/CHECKLIST_HP_NYATA.md`.

### Paket J — Fase 4: Laporan

- Pemilih bulan di Laporan. Kartu arus kas per bulan (sekarang sepanjang waktu).
- Tabel bulanan ringkas: 24 bulan muat tanpa 24 kartu besar (kriteria F4).
- Grafik batang dengan Compose `Canvas`, lalu hapus dependensi Vico (dua artefak).
- Bahasa awam: "Uang dari jualan / beli alat / modal". Rekonsiliasi (selisih belanja produksi vs HPP) di lipatan.
- PDF formal memakai istilah akuntansi (#19) dan angka dari `MonthlySummary`. PDF dibuka dengan data nyata.
- **Tes:** golden laporan per bulan; P7 tetap (OneNumberTest diperluas ke bulan lain).

### Paket K — Fase 6: Kunci app dan notifikasi jatuh tempo

- **Kunci opsional** (PIN / sidik jari lewat `BiometricPrompt`; PIN memakai penyimpanan aman yang sudah ada):
  - ditawarkan sekali setelah usaha pertama dibuat, diatur di Pengaturan;
  - saat aktif, isi disembunyikan di daftar app terbaru; screenshot tetap boleh (#11).
- **Notifikasi jatuh tempo** (#18):
  - `AlarmManager` bawaan dan izin `POST_NOTIFICATIONS` di Android 13+, ditanyakan saat pertama ada jatuh tempo;
  - satu notifikasi per hari berisi daftar tagihan dan utang yang jatuh tempo;
  - ketuk → halaman pelanggan/pemasok.
- **Tes:** penjadwalan (unit dengan jam yang disuntikkan), konten notifikasi, izin ditolak tanpa crash.

### Paket L — Fase 5 sisa: Backup dan sinkron

- Backup Drive terpisah digabung ke sinkron (#8): dua jalur salinan, yaitu file backup terenkripsi dan sinkron Drive terenkripsi.
- Template sudah v5 (paket G). Pastikan pesan penolakan file lama ramah, dengan tombol unduh template.
- Uji sinkron dua HP nyata (pemilik).

### Paket M — Fase 7: Bantuan, panduan, rilis

- Pusat Bantuan ditulis ulang menyeluruh dengan screenshot alur baru (kriteria F7: setiap artikel punya screenshot alur baru).
- Panduan `docs/panduan/` diperbarui.
- Rilis ke internal testing (pemilik).

---

## 5. Register temuan terbuka (dikonsolidasi dari ledger lokal yang tidak ikut git)

| # | Temuan | Asal | Rencana |
|---|---|---|---|
| R1 | ~~Back sistem diabaikan~~ **Selesai 2026-11-12**: penyebabnya keyboard emulator yang rusak; setelah Gboard diperbarui Back terverifikasi baik (audit 4a) | U1, DEC-030 | selesai |
| R21 | **Izin INTERNET hilang sejak Fase 0** (cadangan/sinkron Drive tidak bisa jalan di HP nyata); diperbaiki di branch; `main` terdampak | audit 2026-11-12 T0 | Hotfix ke main dan tes manifest otomatis (paket 0) |
| R22 | Tombol "Tambah Usaha" pertama membuka dua sheet bertumpuk dengan kata unit/cabang (L02, spec #15) | audit 4a | Paket D |
| R2 | Kategori: tanda "Biaya produksi" dan sifat biaya hilang saat ekspor → impor ulang | F2 T4 | G |
| R3 | Nota dari form nota tidak terekspor (hanya pembayarannya) | F2 T3 | G |
| R4 | Form uang lama bisa mengubah belanja tempo menjadi tunai; titip/tarik salah belum bisa dihapus | F2 T3 | F |
| R5 | Rasio R/C tampil di Beranda/kartu BEP (melanggar #19) | F2 T4 | D |
| R6 | Kartu arus kas Laporan masih sepanjang waktu, bukan per bulan | U2 | J |
| R7 | Bantuan dan panduan basi (sheet lama, saklar, editor lama, nota lama, stok bahan manual); gambar `31_laporan_target.jpg` | U2, U3, U4, U5 | D |
| R8 | "Layak" dengan biaya tetap Rp0 (L27) dan konten Rencana lama (periode, R/C) | U2, U4 | C |
| R9 | Stok bahan data lama berubah setelah update (belanja lama ikut menambah, produksi lama tanpa pemakaian); tanpa backfill, ada pemberitahuan satu kali | U3 | I (catatan rilis) |
| R10 | Impor: hitung ulang stok di hari yang sama dengan belanja/penjualan dibaca sesudahnya (Excel tanpa jam) | Fase 1, U3 | Batasan tercatat; tidak diubah |
| R11 | Ekspor satu unit tidak membawa belanja/pemakaian bahan bersama dari unit lain | U3 | Batasan tercatat; tinjau di G |
| R12 | Penghapusan pemakaian bahan lewat pemanggilan `deleteOrphanConsumption` di setiap jalur hapus produksi, bukan FK | U3 | Setiap jalur hapus baru wajib memanggilnya; reviewer H memeriksa |
| R13 | Sinkron lintas versi: penghapusan di set lama yang dilewati tidak diputar ulang; HP yang tidak pernah diperbarui datanya dilewati | F2 T2 | Batasan tercatat (DEC-029); catatan rilis |
| R14 | Sinkron: dua baris resep bernama sama pada produk kembar tergabung; pemeriksaan "dipakai kemudian" saat undo hanya tahu data lokal | Perapihan P3/P4 | Batasan tercatat (DEC-028) |
| R15 | Memperbaiki tanggal nota lunas perlu beberapa langkah (tanggal bayar dulu) | F2 T4 m-5 | Opsional; tinjau di E |
| R16 | Stepper jumlah naik 1 (2,5 kg → 3,5 kg) | U5 | Disengaja |
| R17 | WhatsApp `wa.me` tidak teruji (tidak terpasang di emulator) | U5 | I (HP nyata) |
| R18 | `ModuleScreenshotTest` hanya jalan dari skrip; HelpScreenshotTest dilewati di run normal | sejak Fase 0 | D |
| R19 | Dokumen privasi, listing, Data Safety, `CLOUD_SYNC_AND_AUTH` masih menyebut Firebase | DEC-027 | I |
| R20 | `0ab3ce1` (U1) dan `087e66a` (U3) pesan commit-nya kurang tepat atau tanpa baris co-author | U1, U3 | Hanya catatan; log adalah catatan yang benar |

---

## 6. Matriks verifikasi (jalankan di akhir setiap paket)

| Cek | Cara | Harus |
|---|---|---|
| Unit | `./gradlew.bat :app:testDebugUnitTest` | 0 gagal (baseline 139) |
| Lint | `:app:lintDebug` | 0 error |
| Build | `:app:assembleDebug`; sebelum merge juga `:app:assembleRelease` (R8) | hijau |
| Instrumented | `:app:connectedDebugAndroidTest` (emulator menyala) | 0 gagal; yang dilewati hanya Help/ModuleScreenshotTest |
| Satu angka | `OneNumberTest` | angka Beranda = Laporan = kartu BEP = ekspor |
| Golden untung | `SalesFlowTest`, `MonthlySummaryTest` | Oktober keripik: untung −310.000, HPP 4.440.000, belanja produksi 5.200.000, selisih +760.000 |
| Golden HPP | `ProductCostingTest` | Rp6.000/bungkus, Rp240.000/dus; minyak Rp27.500 → Rp6.125 / Rp245.000 |
| Migrasi | `Migration12To13Test`, `Migration13To14Test`, `BackupUpgradeTest` | lulus; `createSql` = `14.json` |
| DEC-012 | `TemplateImportTest`, `ImportCommitTest` round trip | ekspor → impor ulang identik |
| Bahasa | `ScreenLanguageTest`, `GuidesTest` | tanpa kata terlarang; setiap langkah bantuan > 1 |
| Ketukan | tangan di emulator | jual ≤ 7, belanja ≤ 5, terima bayar ≤ 4, produksi ≤ 3 |
| Log | `grep "â€\|Â§\|Ã—"` pada `*.md`/`*.xml` | kosong (kecuali entri log yang mengutip polanya) |

---

## 7. Risiko yang harus diingat setiap agen

- **Data pengguna nyata di internal testing.** Setiap perubahan schema harus punya migrasi, BackupUpgrade, dan tes.
  Schema 14 belum dirilis, jadi masih boleh diubah; setelah rilis, perubahan berikutnya wajib schema 15.
- **Uang:** satu aturan pembulatan setengah ke atas; parse jumlah selalu lewat `TransactionCalculation.parseQuantity` /
  `sanitizeRupiah`. Bug ×100 dan koma pernah terjadi dua kali.
- **Satu angka:** jangan menambah perhitungan untung, HPP, atau tagihan di layar. Tambahkan field ke `MonthlySummary`.
- **Encoding:** jangan menulis ulang file teks seluruhnya lewat PowerShell atau heredoc shell.
- **Emulator:** reboot bila tombol tidak sampai. Keyboard suara menelan Back.

---

## 8. Status

Format baris: `- YYYY-MM-DD HH:MM — Paket X — selesai/berhenti di … — commit — bukti tes — catatan`.

- 2026-11-09 20:30 — Paket B — selesai — commit 3d02d84 — unit 140/0, lintDebug hijau (0 error), instrumented 12 tes (CatatanUnifiedLedgerTest 4/4, SaleNoteFlowTest 8/8) di Medium_Phone — Catatan satu linimasa tanggal terpadu (nota + kas), pembayaran hari yang sama diserap tanpa double counting kas, filter chip (Semua, Belum lunas, Masuk, Keluar), single FAB + choice sheet (Nota, Belanja, Masuk lain, Keluar lain, Modal/ambil pribadi), fix bug L14, non-cash produksi diisolasi ke modul stok (DEC-031); laporan docs/superpowers/reports/B-laporan.md.
- 2026-11-09 17:18 — Paket N — selesai — commit 337dde4 — unit 139/0, lintDebug hijau (0 error), instrumented 132 tes (0 gagal) di Medium_Phone — eliminasi jitter bottom sheet & form bottom bar (insets windowing), ResultBar height stabil, key stabil pada semua LazyList (Beranda, Catatan, Rencana, Laporan, Produk, Kontak), komputasi berat di-remember; laporan docs/superpowers/reports/N-laporan.md, register audit docs/audit/2026-11-09_BUG_BASH.md.
- 2026-11-09 15:32 — Paket A — selesai — commit 95d0062 — unit 139/0, lintDebug hijau (0 error) — review U5 lengkap (docs/superpowers/reports/U5-review.md), 3 temuan (M-1 bobot judul OverlayScaffold, M-2 label isi satuan, M-3 jitter pergeseran segmen nota) diperbaiki.
- 2026-11-09 — Penyusun (read-only/test-only): dokumen ini ditulis. Di HEAD `b1b865b`: unit 139 lulus / 0 gagal,
  lintDebug dan assembleDebug hijau. Instrumented penuh: 130 tes, 0 gagal, 2 dilewati (Help/ModuleScreenshotTest, seperti biasa).

- 2026-11-12 — Audit menyeluruh (`docs/audit/2026-11-12_AUDIT_MENYELURUH.md`) — selesai — temuan T0 INTERNET diperbaiki di manifest, privasi dan dokumen Firebase diperbarui, Back diverifikasi dengan Gboard berfungsi — unit 148/0, instrumented 134/0/2.
