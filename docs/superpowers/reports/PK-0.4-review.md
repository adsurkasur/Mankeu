# Review PK-0.4: Paket N dan pengukuran jank

2026-11-12 02:31 · Codex, reviewer independen · branch `fase2-nota-piutang`.

## Putusan dan batas bukti

**[verified]** Review menemukan satu crash Important pada kunci pemilih produk. Dua belas pengukuran
`gfxinfo` selesai, masing-masing tiga kali untuk empat aksi. Semua sampel melebihi 15% jank.
Paket N belum memiliki bukti yang cukup untuk klaim stabil secara menyeluruh. Perbaikan diteruskan ke PK-0.5.

**[verified]** Reviewer membaca AGENTS.md, README, PROJECT_GUIDE, PRODUCT_REQUIREMENTS, DOMAIN_MODEL,
QUALITY_GATES, paket PK-0.4, rencana induk §3/4.16/7/8, N-laporan, dan bug bash 2026-11-09.
Diff `95d0062..337dde4` disimpan di `%TEMP%/pk04-paket-n.diff` dan diperiksa. Reviewer tidak mengubah kode
aplikasi atau tes. Baris kode di bawah merujuk HEAD yang diukur, `92686510aa23be7a14a4a038648c9c5ec9d89525`,
sebelum perubahan PK-0.7 dari sesi lain.

**[unknown]** Akar penyebab tingkat fungsi Kotlin atau biaya host emulator untuk seluruh jank belum
terisolasi. Penelusuran fase frame di bawah membatasi lokasi keterlambatan; angka itu bukan bukti bahwa
insets, HPP, atau daftar tanpa kunci menyebabkan jank yang tersisa. Tidak ada klaim perbaikan maupun regresi
performa dibanding benchmark lama karena build, beban, dan protokol benchmark lama tidak setara.

## Lingkungan dan data

- **[verified]** AVD `Medium_Phone`, `emulator-5554`, Android 11/API 30, 1080×2400, density 420,
  layar 60 Hz, pipeline Skia OpenGL, APK debug `com.mankeu` 0.1.0/code 7, schema 14.
- **[verified]** APK final berasal dari `:app:assembleDebug` branch oleh sesi PK-0.6 pada 02:25.
  SHA-256: `3820647a1eac1375bd468832ee2da99ba48eafea38e8240e327cd82ac2e210a3`.
  PK-0.6 juga melaporkan suite JVM 155/0. Reviewer ini tidak menjalankan ulang Gradle atau instrumented suite.
- **[verified]** File APK lokal 02:06 ternyata build `main` schema 13. Saat sempat dipasang, Room menolak
  downgrade 14→13. APK schema 14 dipasang kembali, lalu diganti artifact branch terverifikasi sebelum
  seluruh pengukuran final. Konflik provenance ini terselesaikan; penolakan downgrade bukan temuan Paket N.
- **[verified]** Sebelum manipulasi fixture, app dihentikan dan `databases`, `shared_prefs`, `files`
  dicadangkan sebagai tar di luar repo. Data awal mempunyai 0 financial event, 0 produk, dan 0 nota.
  Fixture sementara merupakan salinan DB awal dengan 200 event `CASH_INCOME` Rp10.000, tanggal hari ini,
  200 jurnal, 400 posting seimbang, serta satu produk sintetis Rp10.000. `foreign_key_check` kosong.
  UI Catatan menampilkan **200 catatan**, total harian Rp2.000.000. Ini fixture performa langsung ke SQLite,
  bukan bukti bahwa alur input/import 200 transaksi lulus.
- **[verified]** Pengukuran dilakukan berurutan; sesi lain menahan build berat sampai 12 sampel selesai.
  Perubahan source sesi lain tidak mengubah APK terpasang selama pengukuran. Tidak ada data sintetis disimpan
  melalui tombol Simpan. Tidak ada cadangan atau isi data awal masuk Git.

## Protokol dan hasil

Sebelum tiap sampel: `adb shell dumpsys gfxinfo com.mankeu reset`; lakukan hanya aksi target;
tunggu satu detik; baca `adb shell dumpsys gfxinfo com.mankeu framestats`.
Ambil jank dan p90 dari bagian ringkasan Android, bukan menghitung p90 sendiri.
Navigasi persiapan, pembersihan nominal, dan gulir balik berada di luar jendela ukur.
Tidak ada pengambilan UI tree atau screenshot di dalam jendela ukur final.

| Aksi | Ulangan | Frame | Janky | Janky % | p90 Android (ms) |
|---|---:|---:|---:|---:|---:|
| Buka sheet tambah dari FAB Catatan | 1 | 44 | 39 | 88.64 | 48 |
| Buka sheet tambah dari FAB Catatan | 2 | 42 | 36 | 85.71 | 38 |
| Buka sheet tambah dari FAB Catatan | 3 | 41 | 35 | 85.37 | 48 |
| Ketik 1234 pada Dibayar sekarang, form jual | 1 | 14 | 8 | 57.14 | 27 |
| Ketik 1234 pada Dibayar sekarang, form jual | 2 | 14 | 8 | 57.14 | 26 |
| Ketik 1234 pada Dibayar sekarang, form jual | 3 | 14 | 8 | 57.14 | 28 |
| Gulir Catatan berisi 200 event | 1 | 220 | 161 | 73.18 | 26 |
| Gulir Catatan berisi 200 event | 2 | 222 | 152 | 68.47 | 26 |
| Gulir Catatan berisi 200 event | 3 | 220 | 134 | 60.91 | 25 |
| Buka Beranda dari Catatan | 1 | 30 | 27 | 90.00 | 53 |
| Buka Beranda dari Catatan | 2 | 31 | 28 | 90.32 | 36 |
| Buka Beranda dari Catatan | 3 | 30 | 29 | 96.67 | 46 |

**[verified]** Typing memakai pilihan Sebagian pada nota baru. Keyboard sudah terbuka sebelum reset;
angka sebelumnya dihapus, lalu `1`, `2`, `3`, `4` dikirim satu per satu dengan jeda 100 ms ditambah biaya
perintah adb. Nilai akhir UI `1.234`. Sampel ini tidak mencakup animasi pembukaan keyboard atau penyimpanan nota.
Gulir memakai empat swipe `(540,1820)→(540,800)`, 400 ms, jeda 300 ms; daftar berisi 200 event, bukan klaim
bahwa seluruh 200 baris dilintasi pada setiap ulangan. Beranda berarti perpindahan tab hangat, bukan cold start.

### Penelusuran semua aksi di atas 15%

Counter berikut disalin dari dump per sampel. Counter Android saling bertumpang tindih, sehingga tidak dijumlahkan
untuk menjelaskan semua janky frame. Kolom `deadline` adalah `Number Frame deadline missed`, metrik berbeda
dari `Janky frames`; misalnya Beranda 1 mencatat 27 janky tetapi 2 deadline missed.

| Sampel | Slow UI | Slow draw commands | Missed vsync | High input latency | Deadline |
|---|---:|---:|---:|---:|---:|
| sheet-1 | 12 | 9 | 3 | 27 | 15 |
| sheet-2 | 11 | 8 | 2 | 25 | 12 |
| sheet-3 | 9 | 7 | 4 | 25 | 13 |
| type-1 | 4 | 0 | 0 | 4 | 4 |
| type-2 | 4 | 0 | 0 | 4 | 4 |
| type-3 | 4 | 0 | 0 | 4 | 4 |
| scroll-1 | 2 | 7 | 0 | 187 | 11 |
| scroll-2 | 2 | 11 | 0 | 198 | 12 |
| scroll-3 | 3 | 6 | 0 | 147 | 9 |
| home-1 | 2 | 1 | 0 | 27 | 2 |
| home-2 | 1 | 0 | 0 | 28 | 2 |
| home-3 | 3 | 2 | 0 | 26 | 3 |

Fase `framestats` memakai baris `Flags=0`. Angka berikut rentang p90 tiga ulangan dalam milidetik:

| Aksi | IntendedVsync→HandleInputStart | InputStart→TraversalsStart | TraversalsStart→SyncQueued | SyncQueued→SyncStart | SyncStart→FrameCompleted |
|---|---:|---:|---:|---:|---:|
| Sheet tambah | 14.45–20.64 | 8.98–11.60 | 2.15–3.40 | 11.53–21.45 | 19.68–22.18 |
| Ketik nominal | 7.27–9.29 | 15.00–16.59 | 4.60–4.98 | 0.67–0.86 | 17.38–19.50 |
| Gulir 200 event | 2.43–2.53 | 5.35–6.29 | 1.10–1.43 | 8.02–8.74 | 18.20–19.80 |
| Buka Beranda | 14.70–18.55 | 9.27–16.38 | 4.27–5.03 | 10.83–15.02 | 18.76–21.56 |

- **[verified]** Pada sheet dan Beranda, antrean ke RenderThread serta fase setelah SyncStart mengambil bagian
  besar dari latensi. Gulir menghabiskan lebih banyak waktu setelah SyncStart daripada traversal UI; counter
  Slow UI hanya 2–3 dari 220–222 frame. Menyalahkan perhitungan HPP dari hasil ini tidak didukung bukti.
- **[verified]** Pada typing, keempat keystroke berkaitan dengan empat Slow UI dan empat deadline missed per
  sampel. p90 sebelum traversal 15–16.59 ms menunjukkan biaya input/animasi; biaya setelah SyncStart juga
  mencapai 17.38–19.50 ms. Belum ada method trace yang mengidentifikasi fungsi aplikasi tertentu.
- **[unknown]** Fase setelah SyncStart menggabungkan kerja render, penantian, dan penyelesaian frame; belum
  dipisahkan menjadi biaya GPU, driver, atau penjadwalan host. Tidak diartikan sebagai waktu CPU fungsi Kotlin.
  Ring buffer frame hanya menyisakan 119 baris valid untuk sampel gulir; sheet mempunyai 28–31, typing 14,
  Beranda 26–27. Fase ini sampel diagnostik, bukan dekomposisi persentase jank global. p90 per fase tidak boleh
  dijumlahkan atau menggantikan p90 ringkasan Android.

**[proposed]** PK-0.5 memperbaiki crash deterministik di bawah. Penyelidikan performa berikutnya memakai
system/method trace serta APK release pada perangkat nyata sebelum memilih perubahan sumber. Tidak ada
dasar untuk mengulangi perubahan insets atau menghapus animasi secara menyeluruh dari angka ini saja.

## Temuan

### Important N1: nama produk sebagai key membuat pemilih crash

**Lokasi:** `presentation/components/form/PickerPage.kt:82` (API opsional pada baris 55),
`presentation/MankeuApp.kt:594`, `presentation/screens/SaleNoteScreens.kt:399` dan `:409`.

**[verified]** Paket N mengubah `items(shown)` menjadi `items(shown, key = itemKey ?: { itemLabel(it) })`.
Tiga pemanggil tidak memberi `itemKey`. `FinanceRepository.kt:358` (`saveProduct`) mewajibkan nama terisi
tetapi membolehkan dua ID produk mempunyai nama yang sama. Dengan dua produk sintetis ber-ID berbeda
dan nama persis `Produk sintetis PK04`, Beranda → Produksi langsung menutup aplikasi.

Logcat pada APK terukur, 02:29:45.810:

```text
FATAL EXCEPTION: main
Process: com.mankeu, PID: 24615
java.lang.IllegalArgumentException: Key "Produk sintetis PK04" was already used. If you are using LazyColumn/Row please make sure you provide a unique key for each item.
at androidx.compose.ui.layout.LayoutNodeSubcompositionsState.subcompose(SubcomposeLayout.kt:1592)
at androidx.compose.foundation.lazy.layout.LazyLayoutMeasureScopeImpl.compose(LazyLayoutMeasureScope.kt:94)
```

**[inferred]** Pemilih tambah produk pada nota mempunyai jalur crash yang sama; pemilih kontak juga memakai
label nama tanpa identitas unik. Kedua jalur tambahan tersebut belum direproduksi di perangkat pada sesi ini.

**[proposed]** Wajibkan `itemKey: (T) -> Any` dan kirim ID produk/kontak dari semua pemanggil. Jangan melarang
nama yang sama hanya untuk menutupi masalah identitas tampilan. Tes merah dulu: dua produk bernama sama
dan dua kontak bernama sama tampil serta dapat dipilih menurut ID tanpa exception; kunci tetap sama ketika
nama berubah atau pencarian difilter. Perbaikan di PK-0.5, tanpa perubahan reviewer.

### Important N2: klaim stabil Paket N melampaui bukti

**Lokasi:** `docs/superpowers/reports/N-laporan.md:15`, `:63`, `:64`;
`docs/audit/2026-11-09_BUG_BASH.md`, tabel Hasil Verifikasi.

**[verified]** Laporan lama menyatakan seluruh akar masalah diperbaiki dan menyebut p50 11 ms sebagai
“60 FPS stabil”, walaupun sampel yang sama mencatat 22.89% jank; sheet masih 32.69%. Pengukuran independen
di atas juga tidak memenuhi ambang 15%. p50 tidak menjelaskan frame lambat maupun lompatan geometri.
Tidak ada rekaman batas elemen per frame, method trace, atau konfirmasi HP pemilik di sesi ini.

**[proposed]** Ubah status klaim menjadi perbaikan kode yang masih memerlukan verifikasi; tautkan tabel
pengukuran dan batasnya, pertahankan PK-5.4 sebagai putusan HP nyata. Jangan menyebut peningkatan ataupun
kemunduran persentase dari dua protokol berbeda sebagai dampak perubahan kode. Ini temuan bukti/kelulusan,
bukan diagnosis satu fungsi aplikasi.

### Minor N3: tinggi minimum ResultBar tidak mencadangkan ruang ketika hasil kosong

**Lokasi:** `presentation/components/form/FullPageForm.kt:93`, `:97`, `:132` pada HEAD yang diukur;
penjelasan J-3 di laporan N dan bug bash.

**[verified]** `AnimatedVisibility(visible = result != null)` membungkus hasil; `heightIn(min = Space.Row)`
berada pada anak `ResultBar`. Ketika tidak terlihat, ruang anak tetap menghilang. Implementasi menghaluskan
perubahan tinggi lewat expand/shrink 150 ms, belum membuktikan tinggi bottom bar tetap.

**[proposed]** Selaraskan uraian dengan perilaku tersebut. Bila persyaratannya benar-benar tinggi tetap,
cadangkan ruang di parent dan verifikasi batas form dengan hasil null/ada/detail dua baris. Sesi ini tidak
mereproduksi lompatan geometri dari perbedaan tersebut; jangan menyatakannya sebagai penyebab jank terukur.

Tidak ada temuan Critical dari ruang lingkup yang diperiksa.

## Pertanyaan untuk pemilik pada PK-5.4

**[unknown]** Jawaban belum tersedia, dan tidak menghalangi pengerjaan paket MVP lain yang telah diizinkan.

1. Merek/model HP, versi Android, keyboard, dan apakah mode hemat daya aktif saat terasa bergetar?
2. Aksi tepatnya: membuka sheet, keyboard mulai muncul, mengetik nominal, atau menggulir? Apakah posisi
   tombol Simpan meloncat atau hanya gerak animasinya tersendat?
3. Apakah terjadi sejak buka pertama atau sesudah beberapa menit, dan pada data kosong atau banyak catatan?
4. Dapatkah pemilik mencoba empat aksi tabel ini tiga kali pada build yang sama serta menyimpan rekaman
   layar singkat? Catat jumlah catatan dan jangan menyertakan isi keuangan pribadi dalam laporan publik.

## Pemulihan dan bukti lokal

**[verified]** Pada 02:30 app dihentikan, seluruh anggota arsip awal dipulihkan, dan arsip ulang dibandingkan
berdasarkan hash isi. Keempat file identik, tidak ada file tambahan. Setelah app dibuka, Beranda kembali
ke panduan awal tanpa produk/catatan. APK branch terbaru tetap terpasang. Emulator diserahkan ke PK-0.7;
tidak ada klaim tentang keadaan perangkat sesudah serah terima itu.

| File | SHA-256 sebelum = sesudah pemulihan |
|---|---|
| databases/mankeu.db | `2ce762b12aa583fe8329f428a75c0b66a7f30eb06d9a6f987f4a5da4bc277615` |
| databases/mankeu.db-wal | `5150e7a3a6badd6b45b5bcc607a23183e0be6a1b114a024f4a6512b1415644b7` |
| databases/mankeu.db-shm | `a97f95fb9eede0ce9fc1f261f49f7e8aad79c101cbbe378f38ba8cc444d8b588` |
| shared_prefs/mankeu_ui_prefs.xml | `d762e362c7cf3242aeac92f5c46229ad07f5e69faa2b85bd76cde425e25c6c1c` |

Dump lengkap dan skrip diagnosis berada di `%TEMP%/pk04-review/`: `seed.py`, `metrics.py`,
`{sheet,type,scroll,home}-{1,2,3}-{reset.txt,gfxinfo.txt,summary.json}`, `duplicate-picker-crash.txt`.
Cadangan asli bersifat lokal, tidak boleh di-commit. Direktori temp tidak menjamin retensi antar-mesin;
tabel hasil, counter, hash, serta protokol dalam laporan ini adalah bukti yang disimpan di Git.

### Pengukuran ulang otomatis pada fixture yang sama

Kode berikut dapat dijalankan sebagai Python di host dengan `adb` pada path tersebut setelah fixture sintetis
aktif. Persiapkan Catatan di bagian paling atas dan keyboard tertutup. Setiap pengukuran menulis output mentah
ke direktori temp. Perintah navigasi memakai koordinat AVD yang diukur. Jangan menjalankannya pada data pemilik
tanpa mencadangkan dan memulihkan data. Fungsi `run` tidak menjalankan shell gabungan.

```python
import pathlib, subprocess, tempfile, time
adb = 'E:/Android/Sdk/platform-tools/adb.exe'
out = pathlib.Path(tempfile.mkdtemp(prefix='pk04-remeasure-'))
def run(*args):
    return subprocess.check_output([adb, *args], text=True, encoding='utf-8', errors='replace')
def tap(x, y):
    run('shell', 'input', 'tap', str(x), str(y))
def measure(label, action):
    run('shell', 'dumpsys', 'gfxinfo', 'com.mankeu', 'reset')
    action()
    time.sleep(1)
    (out / (label + '.txt')).write_text(
        run('shell', 'dumpsys', 'gfxinfo', 'com.mankeu', 'framestats'), encoding='utf-8')
for n in range(1, 4):
    measure(f'sheet-{n}', lambda: tap(965, 2033))
    tap(952, 2264)  # Batal
    time.sleep(1)
tap(965, 2033); time.sleep(1); tap(368, 1288)  # Nota penjualan
time.sleep(1); tap(871, 1110); time.sleep(1); tap(257, 1299)  # Sebagian, nominal
time.sleep(1)
def digits():
    for digit in '1234':
        run('shell', 'input', 'text', digit)
        time.sleep(.1)
for n in range(1, 4):
    run('shell', 'input', 'keyevent', 'KEYCODE_MOVE_END', *(['KEYCODE_DEL'] * 20))
    time.sleep(.8)
    measure(f'type-{n}', digits)
run('shell', 'input', 'keyevent', 'KEYCODE_BACK'); time.sleep(.5)
run('shell', 'input', 'keyevent', 'KEYCODE_BACK'); time.sleep(.5)
tap(911, 2264); time.sleep(1)  # Buang, tanpa Simpan
def swipes(down):
    for _ in range(4):
        a, b = ('1820', '800') if down else ('800', '1820')
        run('shell', 'input', 'swipe', '540', a, '540', b, '400')
        time.sleep(.3)
for n in range(1, 4):
    measure(f'scroll-{n}', lambda: swipes(True))
    swipes(False); time.sleep(1)
for n in range(1, 4):
    tap(403, 2295); time.sleep(1)
    measure(f'home-{n}', lambda: tap(127, 2295))
print(out)
```

**[unknown]** Jitter HP murah, TalkBack, tablet, release build, input sentuh fisik, dan animasi keyboard saat
mulai dibuka belum diverifikasi pada review ini. Pengujian berikutnya harus menyebut kondisi tersebut secara terpisah.
