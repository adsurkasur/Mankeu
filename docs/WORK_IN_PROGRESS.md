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
| 2 | Grill desain sync | selesai |
| 3 | Simulasi UMKM: persona mikro/kecil/menengah × awam/paham; angka dicek manual; uji nyata emulator | jalur impor (paham) berjalan; walk awam belum |
| 4 | Performa: data skala menengah (puluhan ribu transaksi), waktu buka layar, ukuran DB | diukur; 2 kueri dipindah ke agregasi SQL |
| 5 | Perbaikan temuan jelas + test | selesai, di-commit (bagian 1) |
| 6 | Sync sebagai fitur rilis (implementasi, test, uji dua emulator bila bisa) | belum |
| 7 | Kesiapan Play produksi (target SDK, Data Safety, listing, versi, ukuran) | versi + R8 selesai; Data Safety/kebijakan privasi menunggu sync |
| 8 | Semua docs diperbarui; laporan `docs/audit/2026-10-22_AUDIT_PRA_RILIS.md`; log; commit, push, pointer | belum |

### Catatan langkah

- Ponytail-audit: kode ramping. Temuan: `GetStockSummaryUseCase` tak terpakai (hapus); `RecordStockMovementUseCase`
  pembungkus satu pemanggil (lebur ke repository); dependensi `navigation-compose` tak dipakai (hapus); lapisan sync tak
  tersambung **dan `sync_outbox` tumbuh tanpa batas** (±4 baris per transaksi, tak pernah dikosongkan) → ditangani lewat
  tahap 6.
- Grill sync (2026-10-22): satu akun Google untuk semua HP (appDataFolder; HP kasir masuk dengan akun usaha);
  kata sandi sync diketik sekali per HP, disimpan terkunci Android Keystore; sync otomatis saat aplikasi dibuka dan
  beberapa detik setelah menyimpan (bila online + menyala) plus tombol "Sinkronkan sekarang" dan status terakhir;
  bentrok otomatis (nama kategori/produk sama digabung, perubahan terbaru menang) lalu diberitahukan. Default agen:
  pemadatan change set lama (usulan no. 4 di SYNC_ARCHITECTURE) dan last-writer-wins jam perangkat diterima.
- Simulasi skala (`docs/evidence/2026-10-22_simulasi_skala_umkm/make_sims.py`, aset test `androidTest/assets/simulasi/`):
  M1 warung mikro Rp258 jt/th (547 baris), K1 bengkel kecil Rp2,7 M (2.119), N1 penggilingan menengah 3 unit Rp48 M
  (8.975 / 5.513 / 4.252 baris; Gudang Yogya merugi). `ScaleSimulationTest` mengimpor lewat XLSX asli dan mencocokkan
  kas, pendapatan, biaya, laba dengan skrip. Waktu (emulator, sebelum perbaikan): impor 3,9 s / 8,9 s / 31 s (Klaten);
  ringkasan 556 ms pada 9k baris.
- Perbaikan performa: `observePostingTotals` (SUM per peran×akun×sisi di SQL, bukan semua posting ke memori, dua kali
  per perubahan) dan `observeCashFlowRows` (total per bulan×jenis×peran di SQL).
- Ponytail diterapkan: `GetStockSummaryUseCase` dihapus; dependensi `navigation-compose` dihapus. `RecordStockMovementUseCase`
  tetap (punya unit test sendiri).
- Rilis: R8/minify belum aktif di build release (APK debug 27 MB); versi 0.0.6 → perlu keputusan versi rilis.
- Walk awam (Bu Ani, warung, emulator dari instalasi kosong):
  - **Bug** "Tambah Usaha" di Beranda (juga kartu kosong Stok, Rencana, banner Catatan) membuka Berkas dan Data (impor Excel),
    bukan formulir usaha → diperbaiki: semua kartu kosong usaha/unit membuka sheet Usaha dan Unit (`onManageScope`).
  - Teks layar pertama menjelaskan alasan desain → diganti langkah konkret (nama usaha, lalu satu tempat usaha).
  - Untuk grill: Proyeksi/Realisasi tampil sebelum ada usaha; usaha mikro satu lokasi wajib buat usaha + unit; tiga nama untuk
    hal sama (cabang/unit, unit usaha, proyek/unit usaha); sheet tidak menutup sendiri setelah unit pertama dibuat; tawaran
    panduan muncul di setiap halaman/sheet baru (3 kali sebelum mencatat apa pun); aplikasi "kamu" vs panduan "Anda".
  - Dugaan "Kembali tidak menutup sheet" **bukan bug aplikasi**: emulator hanya punya input suara Google, yang menyadap
    tombol Kembali; setelah dimatikan (`ime disable`) sheet tanpa perubahan pun tertutup normal. Probe dihapus.
  - **Bug logika** "Catat Pemasukan" memilih "Pendapatan Lain" (urutan abjad) → semua jualan pemula jadi pendapatan lain,
    BEP/penjualan salah. Diperbaiki: kategori terakhir dipakai untuk arah itu, kalau belum ada ikut urutan daftar awal.
  - Kartu tawaran panduan menutupi tombol "+" (ketukan memulai tur) dan snackbar "tersimpan" → dinaikkan 168 dp.
  - Kolom cari menyebut "nama orang" (sisa fitur utang-piutang) → "Cari keterangan atau kategori".
  - Kas minus merah setelah jualan + kulakan karena uang awal belum dicatat → kartu "Uang kas minus?" + "Catat uang awal"
    (formulir uang masuk, kategori Setoran Modal Pemilik terpilih); hilang begitu modal tercatat. Diuji di emulator: kas
    −1,3 jt → 700 rb, kartu hilang.
  - Persen komposisi biaya memakai Locale.US ("100.0%") → locale aplikasi.
  - Untuk grill: keterangan wajib di setiap catatan; "Untung/Rugi kas" negatif di hari kulakan; chip "Hapus" (kosongkan
    angka) berbagi kata dengan hapus catatan.
  - "Laba Bersih" di Laporan untuk angka yang di Beranda bernama "Untung / Rugi kas" (dan belum dikurangi penyusutan) →
    disamakan "Untung / Rugi kas".
- Walk paham (Mbak Rina, penggilingan menengah): impor lewat layar terhambat pemilih file emulator (tak merespons ketukan,
  juga untuk berkas lama) → jalur impor dibuktikan oleh `ScaleSimulationTest`. Dari kode:
  - **Bug** tombol Batal saat impor palsu: saat membaca, hasil tetap menimpa; saat menyimpan, data tetap masuk lalu tampil
    "Impor selesai". Diperbaiki: baca dibatalkan lewat `previewJob.cancel()`; simpan punya status `Committing` tanpa Batal
    ("Menyimpan N baris… semua atau tidak sama sekali").
  - Pratinjau tidak menyebut unit tujuan → baris "Masuk ke: Usaha · Unit".
- Grill UX (2026-10-22, hasil uji awam): (1) unit pertama dibuat otomatis "Utama" saat usaha dibuat dan langsung dipilih;
  (2) tawaran panduan **hanya satu sapaan di awal** (Beranda), dan harus jelas bahwa bantuan ada di tombol ?; pemilik:
  "Intrusif ya setelah diuji tadi"; (3) keterangan boleh kosong → diisi nama kategori; (4) Proyeksi/Realisasi disembunyikan
  sampai ada unit, label "Proyeksi" → "Rencana"; (5) aplikasi tetap "kamu", panduan tetap "Anda" (sengaja);
  (6) keterangan singkat saat Untung/Rugi kas minus (belanja stok dihitung saat dibayar); (7) versi rilis **0.1.0**
  (versionCode 7).
- Diterapkan (keputusan grill UX): `createBusiness(name, firstUnitName)` membuat unit "Utama"/"Main" dan memilihnya, sheet
  menutup sendiri untuk usaha pertama, sheet langsung membuka formulir bila belum ada usaha; `TourController.offer` hanya
  `Tour.HOME`, kartu sapaan "Selamat datang! … ketuk tombol ?" dengan Tutup/Lihat (Tutup = selesai), tur di sheet tidak
  ditawarkan lagi (kode `later/notNow/never/offerHidden` dihapus), `Tour.HOME` v3; keterangan kosong → nama kategori
  (`EventDraft.toEvent`); tombol buku tersembunyi tanpa unit, "Proyeksi" → "Rencana" di semua teks + bantuan
  ("Projection" → "Plan" EN); catatan "belanja stok dihitung saat dibayar" di bawah metrik bila untung/rugi kas minus.
- Rilis: versi 0.1.0 (code 7); R8 + shrinkResources aktif; APK release 4,2 MB. Uji build release (ditandatangani kunci debug)
  di emulator: buat usaha (unit Utama otomatis), catat tanpa keterangan, Laporan 3 tab, Stok, Pusat Bantuan → tanpa crash.
  Aturan proguard `androidx.navigation` dibuang bersama dependensinya.
- Test: unit + lint lulus; instrumented 29 test, 0 gagal (2 dilewati = screenshot). `ModuleScreenshotTest` disesuaikan
  (shot 02_tambah_usaha, 03_beranda_unit_baru menggantikan 02–06; "Rencana"; sapaan di Beranda; `newBusiness` mengganti
  nama unit Utama lewat ViewModel). Panduan: bagian 3 ditulis ulang (unit Utama otomatis), 6.2 (keterangan opsional, kartu
  kas minus), 6.3, 9.2 (Masuk ke, menyimpan), 10.3 (sapaan tunggal), "Proyeksi" → "Rencana", "Gerai Utama" → "Utama".
- Docs pass 1: README (v0.1.0, Rencana), PRODUCT_REQUIREMENTS, PROJECT_GUIDE, UX_AND_ACCESSIBILITY, V1 spec,
  MANUAL_TEST_GUIDE, PLAY_CONSOLE_ENTRY_GUIDE, STORE_LISTING. DEC-022 (UX) dan DEC-023 (0.1.0 + R8) ditulis.
- Bagian 1 di-commit (screenshot bantuan/panduan diambil ulang, gambar 02–06 lama dihapus, check_guide_flow OK). Lanjut tahap 6 (sync).
