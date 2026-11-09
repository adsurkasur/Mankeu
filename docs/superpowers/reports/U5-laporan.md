# Task U5 report: forms rebuilt with the new patterns

Branch `fase2-nota-piutang`, four commits (not pushed, per brief):

| Commit | What |
|---|---|
| `53ccd09` | fix(ui): money sen (×100), date row for TalkBack, stock prompt Back keeps the quantity, Tambah bahan |
| `92f00b1` | feat(ui): note form without boxes, Kirim nota and reminder, customer page |
| `fe50f1b` | feat(ui): Belanja and the other money pages as full pages, entry sheet removed |
| (part 4, see bottom) | feat(ui): product editor rows, Produksi stepper, defaults from screenshot review, orphaned strings |

## What changed, per form

**Carry-overs**
- **U1 M5 money ×100.** `sanitizeRupiah` drops trailing sen: a separator followed by 1–2 digits and nothing but non-digits. "1.250.000,50" → 1250000, ".5" likewise; "1,250,000" and "1.250" stay thousands groups. One shared function, so every money field is fixed.
- **Date row.** `DateField` itself is now a `ListRowField` ("Tanggal … Hari ini ›", role Button, × beside a clearable date) opening the shared `MankeuDatePicker`. Every date in the app changes with it (note, payment, production, stock count, plan, unit dates). "Hari ini" / "Kemarin" / the date.
- **U3 M-6.** Tambah keeps the stock sheet open under "Catat juga uangnya?". The dialog has "Kembali" (and tap-outside), which returns to the sheet with the typed quantity. Ya opens the new Belanja page filled; Tidak records stock only. Both close the sheet.
- **Back / Buang perubahan?** Every rebuilt full page (note, the four money pages, product editor) uses `FullPageForm`. `FullPageForm` got an optional `snackbarHost`, so repository refusals show on the page while it stays open.

**Nota** (`SaleNoteEditorScreen`)
- Opens with no text box. It shows the product line, the customer chips, the payment segment, "+ Tambah detail", and Total · untung above Simpan.
- **Lines.** Each line is a compact row "2 dus Keripik · Rp600.000" with untung below (rugi in red words) and − / + beside it. Tapping a row opens a 3-field sheet: unit chips, `QuantityStepper` (new: text quantity with decimal comma), and price filled from the unit with a "Harga biasa" chip. The sheet also has Hapus with confirmation. "Tambah produk" opens a `PickerPage`.
- **Starting line.** A new note starts on the product and unit last sold. While the line is untouched, picking a customer restarts it on what that customer last bought: Toko A gives "1 dus", the audit L12 "grosir sells dus" point.
- **Customer.** Chips for Umum and the last 3 customers, then "Cari / tambah". That opens a `PickerPage` where "Tambah 'Toko Baru'" creates and picks the customer (audit L13). When the note is unpaid with no customer, the hint "Belum lunas? Pilih pelanggannya." appears.
- **Payment and due date.** Payment is a segment (Lunas / Belum dibayar / Sebagian, with an amount when Sebagian). Due-date chips (7 / 14 / 30 hari / Pilih tanggal) show only while the note is unpaid.
- **Details.** Date, discount and note sit behind "+ Tambah detail".
- **Note detail.** Kirim / Ubah / Bayar sit at the bottom, Hapus nota at the end of the page. The customer row opens the customer page. Pay sheet (`PaySheet`, shared): "Jumlah dibayar" (was "Nominal"). A "Lunas Rp…" chip appears only after the amount is changed.

**Kirim (U-3)**
- `ShareText` is pure and JVM-tested. It builds:
  - the note text: lines, total, sudah dibayar, sisa, jatuh tempo, thanks;
  - the reminder: open notes with sisa and due date, and their total;
  - `whatsAppLink`: `https://wa.me/62…?text=…` for a usable number (08… → 628…, +62/62 kept, spaces and dashes dropped, under 8 digits → null).
- `SendChoice`: with a phone, a sheet offers WhatsApp (showing the number) or Aplikasi lain; without a phone, it goes straight to the share sheet.
- Screens only call `onSend(text, link)` (the seam). `MankeuApp.shareText` builds `ACTION_VIEW` wa.me and falls back to the `ACTION_SEND` chooser on `ActivityNotFoundException`. Checked on the emulator: the share sheet opens with "Nota dari Keripik Bu Sri / Untuk Toko A…".

**Customer page** (`Overlay.CUSTOMER`, `CustomerScreen`)
- The page shows Sisa tagihan (figure, warning colour), Terima bayar and Kirim pengingat, the notes (open first, oldest open on top, then paid), and "Ubah data" (the contact sheet, with Arsipkan).
- **Terima bayar** pays one amount over the open notes, oldest first. It goes through the new `FinanceRepository.recordCustomerPayment` (one transaction; notes dated after the payment are left out; above everything owed is refused) and the pure `SalesMath.allocate`. Repository validation stays the source of truth.
- **Routing.** Beranda's named Tagihan rows open it (`CustomerBill` now carries `contactId`), and so do Kontak rows. A note opened from it returns to it.

**Belanja and the other money pages** (`MoneyFormScreen`, `MoneyFormMode`)
- **Belanja**:
  - "Beli apa?" row on the material last bought. The picker lists the materials with their price, plus "Bukan bahan".
  - Quantity box with the unit as suffix; the keyboard opens on it.
  - Price filled from the material, with a "Harga terakhir" chip after a change.
  - Live "Total Rp260.000 · Stok Minyak goreng +10 liter".
  - The category follows the material: last category used for it, else the usual expense.
  - Description defaults to "Minyak goreng 10 liter" (audit L17: the list title).
  - After saving, the snackbar says "Belanja dicatat. Stok … +10 liter".
  - No Sifat biaya, reference or plan link.
  - "Bukan bahan" gives category chips and one amount.
- **Uang masuk lain / Uang keluar lain**: amount, category chips (+ Baru), note, date row (4 fields). For a business with products, Uang masuk lain starts on the last non-note income category, else "Pendapatan Lain", not "Penjualan & Jasa" (audit L16; found in the screenshot review).
- **Modal & ambil pribadi**: a Modal masuk / Ambil pribadi segment, amount, note, date. The category is implied.
- **Ubah on a Catatan card** opens the matching page. Fields the page does not show (reference, plan link, cost nature, product, asset life) are kept as stored.
- **Closing.** The page closes only once the record is stored (`AppViewModel.record` gained `onSuccess` and `savedMessage`).
- **Old sheet removed.** `EntryScreen` is now the list only; the old bottom-sheet form (≈600 lines) is deleted.
- **Speed dial.** It reads Nota penjualan / Uang masuk lain / Belanja / Uang keluar lain / Modal & ambil pribadi until U6 makes it one + menu.

**Product editor**
- On open, only two boxes: name and "1 batch jadi" (number with the base unit as suffix). The base unit is chips (bungkus, pcs, porsi, botol, kg, Lainnya).
- **Recipe.** Rows "Singkong · 50 kg … Rp150.000", "Gas … Rp400.000". A 2-field sheet edits them: material chips + quantity, or name + amount. "+ Bahan" starts on an unused material; "+ Biaya lain" adds a cost.
- **Sale units.** Rows "dus · isi 40 bungkus · untung Rp193.000 … Rp300.000". A 3-field sheet edits them (name, isi, harga) with Hapus. "+ Satuan jual" adds one.
- `ResultBar` shows "HPP Rp2.675 per bungkus · Biaya 1 batch Rp1.070.000".
- **Plan fields.** Target and sell-from month show only when the editor is opened from Rencana usaha (`showPlan = productFrom == PLAN`). Elsewhere they are kept as stored.
- **Produksi sheet.** "Berapa batch?" is now a `QuantityStepper` (audit L10).

**Tambah bahan**
- Name and price ("Harga beli per kg", follows the chip) are the only boxes. The unit is chips (kg, liter, gram, pcs, bungkus, ikat, Lainnya → a name box).
- Stok awal and batas menipis sit behind "+ Stok awal dan batas menipis". The owner choice shows only with 2+ units.

**Strings.** All new strings are ID + EN. 54 strings orphaned by the removed sheet and old editor were deleted with the Edit tool (lint `UnusedResources` back to baseline).

## Tests

**RED first**
- JVM:
  - `MoneyFieldInputTest.trailingSenAreDroppedNotGluedOnAsDigits` failed on the old sanitiser.
  - `ShareTextTest` 4/4 and `SalesMathTest.aCustomerPaymentPaysOldestNotesFirst` failed on `TODO()` stubs.
- Compose, with `FormFields.kt` stashed (old code):
  - `pastedSenDoNotMultiplyTheAmountByAHundred` failed: expected 1250000, was 125000050.
  - `theDateRowIsAButtonThatOpensThePicker` failed: no Role Button.
- The screen-level Compose tests (note, money pages, product editor, customer page, Kirim) were written before the screens were run, but not seen red. Their first runs found one test-timing issue (Back sent before the typed amount recomposed; fixed with `waitForIdle`).

**New / rewritten tests**

| Test | Covers |
|---|---|
| `FormComponentsTest` +2 | ×100 paste; date row is a Button that opens the picker (TalkBack's double-tap = semantics OnClick) |
| `MaterialStockScreenTest` +2, 1 updated | Kembali keeps "10"; Tambah bahan has 2 boxes and saves Gula kg Rp15.000; prompt → Belanja filled → Simpan → stock 10 + stock message |
| `SaleNoteFlowTest` (rewritten 2, +2) | `theDailySaleIsFiveTapsWithNoTyping`: 0 text boxes and no due field on open, "1 dus Keripik"; chip Toko A, +, Belum dibayar, 14 hari, Simpan → Rp600.000 unpaid, due today+14. Two products via line sheet / picker / ±, discount behind "+ Tambah detail", unpaid-needs-customer hint and repository refusal. Duplicate name not offered as new, "Tambah 'Toko Baru'" creates + picks. Terima bayar Rp400.000 → older note fully paid (300.000), newer 100.000 |
| `SaleNoteSendTest` (new) | no phone → share text with "Keripik Bu Sri", "2 dus Keripik", 600.000, due date, no WhatsApp; phone → WhatsApp link `https://wa.me/628123456789?text=` or another app (link null) |
| `MoneyFormsTest` (new, 6) | Belanja: 2 boxes, no Sifat biaya, live total and stock line, saved entry (material, 10, 26.000, category, "Minyak 10 liter"); Bukan bahan: 1 box + chip; Uang masuk lain 2 boxes + chips + date row; Ambil pribadi = segment, OWNER_DRAWING; clean Back closes; typed Back asks Buang perubahan? |
| `ProductEditorTest` (rewritten, 4) | 2 boxes on open; rows and sheets give HPP Rp6.000 per bungkus, Rp240.000 per dus; draft saved; unused-material default; plan fields only with `showPlan` |
| `HomeScreenTest` (updated) | a Tagihan name opens the customer page |
| `CustomerBillsTest` (updated) | rows carry `contactId` |

Deleted `EntryFormFallbackTest` and `EntrySheetSwipeTest`: they tested the removed bottom sheet; their concerns (no product on money in, swipe closing the form) no longer exist.

**Numbers**
- Unit: 139/0 (133 + 6).
- lintDebug: 0 errors, 85 warnings (82 before).
- assembleDebug: OK.
- Full connected: see the bottom of this report (before: 118/0/2).

## Taps on emulator-5554 (seeded Keripik Bu Sri), counted by hand

| Daily job | Path | Taps | Target |
|---|---|---|---|
| Jual 2 dus to Toko A, unpaid, due in 14 days | Beranda Jual → chip Toko A (line becomes "1 dus") → + → Belum dibayar → 14 hari → Simpan | **6** | 6–7 |
| Belanja minyak 10 liter | Belanja (Minyak goreng already picked, keyboard on Jumlah) → type 10 → Simpan = **3** at the known price; with a new price: + tap price, retype → **5** | 3 / 5 | 5 |
| Terima bayar from Toko A | Beranda Tagihan "Toko A" → Terima bayar (amount = everything left) → Simpan | **3** | 4 |
| Produksi 1 batch | Produksi → Keripik → Simpan | **3** | 2–3 |
| Kirim nota (bonus) | Note → Kirim → WhatsApp / Aplikasi lain | 2–3 | 2 |

## Screenshots (`.superpowers/sdd/2026-10-30-fase-ux/`)

| File | What |
|---|---|
| `u5-1-nota-buka.png` | New note on open: one line, customer chips, segment, detail link, Total · untung; no box |
| `u5-2-nota-isi.png` | 2 dus Keripik to Toko A, Belum dibayar, 14 hari selected |
| `u5-3-pelanggan.png` | Customer page Toko A: Rp900.000, Terima bayar / Kirim pengingat, notes, Ubah data |
| `u5-4-belanja-keyboard.png` | Belanja with keyboard: Minyak goreng, 10 liter, Rp26.000, Harga terakhir chip, Total + Stok +10 liter above Simpan |
| `u5-5-produk.png` | Product editor: recipe rows, 1 batch jadi, unit chips, sale-unit rows, HPP bar |
| `u5-6-produk-satuan-sheet.png` | Sale-unit sheet (3 fields + Hapus) |
| `u5-7-tambah-bahan.png` | Tambah bahan: 2 boxes + unit chips + link |
| `u5-8-modal.png` | Modal & ambil pribadi |
| `u5-9-uang-masuk-lain-keyboard.png` | Uang masuk lain with keyboard (default Pendapatan Lain) |
| `u5-10-kirim.png` | Kirim choice: WhatsApp 0812 3456 789 / Aplikasi lain |
| `u5-11-share-sheet.png` | Android share sheet with the note text |
| `u5-12-produksi-tanggal-baris.png` | Production sheet with the date row (before the batch stepper change) |

**P2/P3 review of the screenshots, and what was fixed:**
- Uang masuk lain defaulted to "Penjualan & Jasa" for a business with products. It now starts on Pendapatan Lain.
- The pay sheet showed a "Lunas Rp900.000" chip equal to the amount already filled. It now shows only after a change.
- A new note always started on the last product's unit (bungkus after a Warung sale). Picking a customer now restarts the untouched line on that customer's last unit.
- Produksi batches was a plain box. It is now − n +.

What remains by eye:
- The note page has a lot of white space below the fields on a tall phone. That is intended: everything sits above the fold.
- The "Belum lunas? Pilih pelanggannya." hint shifts the segment down by one line when it appears.
- The customer page title uses the old `OverlayScaffold` Black weight; the forms use `MankeuType.Title`.
- The sale-unit sheet label "Isi (bungkus)" repeats its own suffix.

## Concerns / open
- **System Back by key** is still not checked by hand (emulator voice IME, U1). Tests use the activity dispatcher. ← and "Buang perubahan?" were checked by hand.
- **Catatan list.** It is still the old cards and two filter sets (U6). Payment rows read "Bayar nota Toko A" and the speed dial is a temporary 5-entry dial.
- **Help and walkthrough are stale.** Help articles and `ModuleScreenshotTest`'s walkthrough still describe the old entry sheet ("Catat Pemasukan", "Simpan catatan"), the 19-box editor and the old note. The walkthrough only runs from its script and is not in the connected run. Both are left for U8.
- **Material stock in the seed went negative** (–33 liter) because production consumes 20 liter per batch. That is seed data, not a bug (U3 behaviour).
- **WhatsApp itself is not installed** on the emulator. The ACTION_VIEW → chooser fallback is covered by code review and the "Aplikasi lain" path by hand; the wa.me launch is not.
- **Quantity steppers step by 1.** A "2,5 kg" line steps to "3,5". That is intended.
