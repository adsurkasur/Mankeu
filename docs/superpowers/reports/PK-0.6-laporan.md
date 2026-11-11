# PK-0.6: pemeriksaan bahasa layar

Tanggal: 2026-11-12. Agen: Codex (GPT-6.1). Branch: `fase2-nota-piutang`.

## Hasil dan lingkup

**verified:** `ScreenLanguageTest` memeriksa nilai seluruh elemen `string` dalam `res/values/strings.xml` dan `res/values-en/strings.xml`. Semua kata Indonesia dan Inggris dari Lampiran C §1.1 diperiksa sebagai kata utuh tanpa membedakan kapital. Entitas XML dan markup dibaca sebagai teks; komentar serta nama resource tidak dianggap teks layar. Spasi dan pergantian baris dinormalkan.

**verified:** daftar dasar berisi **243 pasangan unik** `nama_string|kata`, disalin dari keluaran tes merah awal. Format sesuai PK-0.6: pasangan yang sama pada kedua bahasa dihitung sekali. Tes gagal untuk pasangan baru serta pasangan daftar dasar yang tidak lagi ditemukan. Baris ganda dan format tidak sah juga ditolak. Tidak ada perubahan permanen pada string aplikasi, kode produksi, dependensi, schema, atau identifier paket.

**verified:** pengecualian HPP hanya mengabaikan HPP dalam kurung sesudah frasa modal, misalnya `Modal barang (HPP)`. HPP lain dalam kalimat tetap terdeteksi. `buku resep`, `impor`, dan `ekspor` tetap diperiksa.

## Pengecualian nama string

**verified:** 13 awalan berikut menjadi konstanta `formalStringPrefixes`. Asalnya ditelusuri ke resource yang ada dan pemakainya; tidak ada pengecualian seluruh berkas UI.

| Awalan | Pemakai dan alasan |
|---|---|
| `reports_operating_` | `ReportsScreen.FinancialsSection`: baris pendapatan dan biaya laporan |
| `reports_income_note` | `ReportsScreen.FinancialsSection`: catatan perhitungan laporan |
| `reports_cash_flow` | `ReportsScreen.FinancialsSection`: judul laporan arus kas |
| `reports_cf_` | `ReportsScreen.FinancialsSection` dan `FeasibilitySection`: baris arus kas |
| `reports_monthly_` | `ReportsScreen.FinancialsSection`: rincian kas bulanan |
| `reports_financing_` | `ReportsScreen.FeasibilitySection`: rincian pendanaan |
| `reports_working_capital` | `ReportsScreen.FeasibilitySection`: kebutuhan modal kerja dan penjelasannya |
| `reports_rc_ratio` | `PlanCards.PlanBreakdownCard` dan `ActualMonthCard` pada laporan: rasio formal |
| `reports_bc_ratio` | `PlanCards.PlanBreakdownCard`: rasio formal |
| `plan_bep_base` | `PlanCards.PlanBreakdownCard`: dasar titik impas dan penjelasannya |
| `plan_variable_total` | `PlanCards.PlanBreakdownCard`: rincian biaya variabel |
| `plan_vc_unit` | `PlanCards.PlanBreakdownCard`: biaya variabel per satuan |
| `feasibility_bep_` | `PlanCards.PlanBreakdownCard`: titik impas volume dan rupiah |

**verified:** `reports_`, `plan_`, `feasibility_`, dan `help_` tidak dikecualikan secara menyeluruh. Judul tab, subjudul laporan, ringkasan kelayakan pada layar rencana, bantuan, dan UI umum tetap masuk pemeriksaan. `PlanBreakdownCard` saat ini memiliki definisi tetapi tidak dipanggil; pengecualiannya terbatas pada keluarga label rincian formal tersebut. `FinancePdfExporter` memakai label literal sendiri sehingga tidak perlu awalan pengecualian PDF di `strings.xml` saat ini.

## Bukti penerimaan

Semua Gradle dijalankan lewat PowerShell dengan PATH dibersihkan sesuai perintah pemilik:

```powershell
$env:Path = ($env:Path -split ';' | Where-Object { $_ -notmatch '"' }) -join ';'
./gradlew.bat :app:testDebugUnitTest --tests com.mankeu.presentation.ScreenLanguageTest
```

| Langkah | Hasil terverifikasi |
|---|---|
| Daftar dasar kosong | Merah: 7 tes, 1 gagal; pesan menyebut 243 pasangan pelanggaran lama |
| Daftar dasar dari keluaran merah | Hijau: 7 tes, 0 gagal |
| Tambah sementara `<string name="pk06_probe">piutang</string>` pada resource Indonesia, dan `pk06_stale_probe|hpp` pada daftar dasar | Merah: 7 tes, 1 gagal; pesan memuat `pk06_probe|piutang` di bagian pelanggaran baru serta `pk06_stale_probe|hpp` di bagian baris usang |
| Kembalikan kedua berkas persis dari salinan byte | Resource aplikasi kembali sama dengan Git; kedua probe hilang |
| Tambah kasus `Modal HPP (HPP)` | Merah: pengecualian awal terlalu luas dan menyembunyikan HPP di luar kurung; diperbaiki agar hanya menghapus bagian kurung |
| `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug` | Hijau: **28 suite, 155 tes, 0 gagal, 0 error, 0 dilewati**; APK debug berhasil dibangun |
| `git diff --check` | Lulus |

**verified:** tujuh tes mencakup gate pada resource asli, batas kata/kapital/frasa, seluruh daftar kata, pengecualian HPP, batas awalan formal, pembacaan XML, serta penolakan pelanggaran baru dan baris usang. Tujuan tes lama untuk debit/kredit/credit/jurnal/journal tetap tercakup daftar kata baru.

**reported:** induk menerima pengalihan kunci Gradle dan lokasi APK hasil build untuk kebutuhan PK-0.4. Agen PK-0.6 tidak memasang APK atau mengakses emulator.

## Batas dan tindak lanjut

**unverified:** keterpakaian nyata, seluruh perjalanan UI di perangkat, PDF, serta teks hardcoded di Kotlin berada di luar gate dua resource XML ini. Hasil tes bahasa tidak membuktikan aplikasi mudah dipakai. Sebanyak 243 pasangan masih menjadi pekerjaan PK-5.1; daftar dasar harus kosong pada paket itu. Awalan formal perlu ditinjau kembali bila resource dipakai di UI umum. Tidak diperlukan migrasi data, perubahan privasi, ADR, atau changelog fitur untuk paket yang hanya menambah verifikasi ini.
