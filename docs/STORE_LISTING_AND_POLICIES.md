# Google Play Store Listing, Policy Declarations, and Compliance Guide

This guide provides the complete, authoritative compliance declarations, store listing copy (Indonesian and English), privacy policy hosting instructions, and visual asset inventory for **Mankeu** (`com.mankeu`).

---

## 1. Privacy Policy Hosting (GitHub Pages)

### Live URL
- **Public Privacy Policy URL:** `https://adsurkasur.my.id/mankeu/privacy-policy`
*(Content source: `docs/privacy-policy/id.md` and `en.md` in this repository; the page reads them from GitHub and refreshes within about 15 minutes of a push to `main`.)*

---

## 2. Google Play Console Compliance & Policy Declarations

Follow these exact steps and answers in Google Play Console under **Policy and programs** > **App content**:

### A. Privacy Policy
* **Privacy Policy URL:** `https://adsurkasur.my.id/mankeu/privacy-policy`

---

### B. App Access (Akses Aplikasi)
* **Question:** Is all or part of your app restricted based on login credentials, memberships, or other authentication?
* **Answer:** Select **"All functionality is available without special access"** (*Semua fungsi tersedia tanpa batasan khusus*).
* **Rationale:** The entire core financial functionality (budgeting, actual expense recording, business plan and break-even, stock, Excel template import, PDF export) operates 100% locally on device without registration or login. Google Sign-In is strictly optional for Drive backup.

---

### C. Ads (Iklan)
* **Question:** Does your app contain ads?
* **Answer:** Select **"No, my app does not contain ads"** (*Tidak, aplikasi saya tidak berisi iklan*).
* **Rationale:** The application is 100% ad-free, with zero third-party advertising SDKs or banner frameworks per project invariants (`O-011`).

---

### D. Content Rating (Kuesioner Rating Konten IARC)
Fill in the IARC Content Rating questionnaire as follows:
* **Category:** **Utility, Productivity, Communication, or Other** (*Utilitas, Produktivitas, atau Lainnya*).
* **Violence:** No
* **Sexuality / Adult Material:** No
* **Offensive Language:** No
* **Controlled Substance (Drugs/Alcohol):** No
* **User Interaction / Social Features:** No (Users do not exchange text or media with unknown third parties).
* **Physical Location Sharing:** No
* **Digital Goods / Purchases:** No
* **Expected Result:** **Rated for 3+ / Everyone** (PEGI 3, USK 0, ESRB Everyone).

---

### E. Target Audience and Content (Audiens Target dan Konten)
* **Target Age Group:** Select **"18 and over"** (*18 tahun ke atas*).
* **Could your store listing appeal to children?** Select **"No"** (*Tidak*).
* **Rationale:** The application is designed for adult MSME owners, small business managers, entrepreneurs, and operators managing business finances.

---

### F. Government Apps (Aplikasi Pemerintah)
* **Question:** Is this app developed by or on behalf of a government entity?
* **Answer:** Select **"No"** (*Bukan*).

---

### G. Financial Features Declaration (Fitur Keuangan)
* **Question:** What financial features does your app provide?
* **Answer:** Select **"Personal/Business accounting and expense tracking"** (*Akuntansi pribadi/bisnis dan pelacakan pengeluaran*).
* **Important Guardrails:**
  * **DO NOT** select: Banking, P2P Lending, Crypto, Digital Wallet, Money Transfer, or Investment Brokerage.
  * **Declaration Note:** Mankeu is an on-device MSME cost bookkeeping tool, business plan and break-even calculator (BEP, R/C, B/C, capital need), and stock tracker. It records cash only and has no lending, credit, or payment features. It does not process financial transactions, loan applications, banking, or money transfers.

---

### H. Health & Medical Apps (Aplikasi Kesehatan)
* **Question:** Does your app provide human health, medical diagnosis, or clinical features?
* **Answer:** Select **"No"** (*Bukan*).

---

### I. Data Safety Form (Keamanan Data)

Fill in the Google Play Data Safety form with the following answers:

#### 1. Data Collection & Sharing Overview:
* **Does your app collect or share any user data?**
  * Select **"Yes"** (Only for optional Google account metadata during Drive backup).
* **Is all of the user data collected by your app encrypted in transit?**
  * Select **"Yes"** (HTTPS / OAuth 2.0).
* **Do you provide a way for users to request that their data be deleted?**
  * Select **"Yes"**. In the app: Pengaturan → **Hapus semua data** deletes all local data and, when signed in, every Mankeu file in the Drive app folder, the Firebase sign-in account, and the Drive grant (DEC-020). Outside the app: section 7 of the privacy policy (clear app storage, Drive "Delete hidden app data", or an email request).
  * **Delete account URL** (asked because Google Sign-In creates a Firebase account): `https://adsurkasur.my.id/mankeu/privacy-policy` (section 7, "Penghapusan Data").

#### 2. Data Types & Purposes:
* **Personal Info > Name & Email Address:**
  * **Collected?** Yes (Optional, collected only if the user explicitly signs in with Google).
  * **Shared?** No (Never shared with third parties or developer servers).
  * **Ephemeral?** No (Cached locally by Firebase Auth on the device).
  * **Required or Optional?** **Optional** (Users can use 100% of calculation/import features offline without signing in).
  * **Purpose:** **Account Management** & **App Functionality** (Drive backup sync identity).
* **Financial Info (User payment info, Purchase history, Credit score):**
  * **Collected?** **No** (Cash records, plans, and stock are stored exclusively on device in the local SQLite Room database).
* **Files and Docs:**
  * **Collected?** **No** (Excel workbooks and backups are processed on-device; cloud backups are uploaded directly to the user's personal Google Drive without developer server intermediation).
* **Device or Other IDs:**
  * **Collected?** **No** (No advertising IDs, no device trackers).

---

## 3. Store Listing Copy (Teks Listing Toko)

### Bahasa Indonesia (Utama / Default)

#### Nama Aplikasi (Maks 30 Karakter)
```text
Mankeu
```

#### Deskripsi Singkat (Maks 80 Karakter)
```text
Buku kas UMKM, rencana usaha & titik impas (BEP), dan stok. Offline & privat.
```

#### Deskripsi Lengkap (Maks 4000 Karakter)
```text
Mankeu adalah aplikasi buku kas dan rencana usaha untuk pelaku UMKM (Usaha Mikro, Kecil, dan Menengah), pedagang, wirausaha kuliner, penyedia jasa, dan petani di Indonesia.

Catat uang masuk dan keluar, susun rencana usaha, hitung titik impas, dan pantau stok. Semua 100% offline!

📊 FITUR UTAMA MANKEU:

1. BUKU KAS YANG SEDERHANA
• Dua tombol saja: Uang Masuk dan Uang Keluar. Kategori menentukan apakah itu penjualan, biaya, setoran modal, ambil pribadi, atau beli aset.
• Salah isi? Ketuk Ubah untuk membetulkan, atau Hapus.
• Mencatat uang yang benar-benar berpindah. Tanpa utang, piutang, atau pinjaman.

2. RENCANA USAHA & TITIK IMPAS (BEP)
• Buku Proyeksi terpisah dari Buku Realisasi.
• Produk dengan resep biaya per unit, biaya tetap bulanan, aset, dan modal rencana.
• BEP unit dan rupiah (juga untuk banyak produk), laba per bulan, R/C, B/C, dan kebutuhan modal.
• Bandingkan realisasi dengan target per bulan.

3. STOK BARANG
• Barang masuk, keluar, dan opname, dengan peringatan stok menipis.
• Stok per cabang atau dipakai bersama.

4. EXCEL (.XLSX) & PDF
• Templat Excel Mankeu: isi sekali, impor banyak catatan sekaligus, dan batalkan bila salah.
• Ekspor Excel yang bisa diimpor kembali, dan laporan PDF siap cetak.

5. 100% OFFLINE & PRIVAT
• Semua data tersimpan di HP Anda (SQLite Room).
• Bebas iklan, tanpa pelacak, tanpa wajib login.

6. CADANGAN TERENKRIPSI
• Cadangkan ke berkas atau Google Drive pribadi Anda (opsional).
• Dienkripsi AES-256-GCM dengan kata sandi Anda.

💼 COCOK UNTUK BERBAGAI SEKTOR UMKM:
• Usaha Dagang / Ritel: Toko kelontong, kios, warung sembako, agen, reseller.
• Kuliner & F&B: Kafe, warung makan, resto, katering, bakery, kedai kopi.
• Jasa & Servis: Bengkel, laundry, barbershop/salon, rental/sewa alat, jasa servis.
• Pertanian & Produksi: Budidaya, pengolahan pangan, kerajinan, dan manufaktur kecil.

Kelola arus kas dan kembangkan usaha Anda bersama Mankeu!
```

---

### English (United States)

#### App Name (Max 30 Characters)
```text
Mankeu
```

#### Short Description (Max 80 Characters)
```text
Small-business cash book, business plan & break-even, and stock. Offline.
```

#### Full Description (Max 4000 Characters)
```text
Mankeu is a cash book and business planner for small businesses: shops, food stalls, service providers, rentals, and farms.

Record money in and out, plan your business, find your break-even point, and track stock. 100% offline!

📊 KEY FEATURES:

1. A SIMPLE CASH BOOK
• Two buttons: Money In and Money Out. The category tells whether it is a sale, an expense, owner capital, an owner drawing, or an asset purchase.
• Made a mistake? Tap Edit to fix it, or Delete.
• Records money that actually moved. No debts, receivables, or loans.

2. BUSINESS PLAN & BREAK-EVEN
• A Projection book kept apart from the Actual book.
• Products with per-unit cost recipes, monthly fixed costs, assets, and planned capital.
• Break-even in units and rupiah (also for several products), monthly profit, R/C, B/C, and capital needed.
• Compare actual results with the plan month by month.

3. STOCK
• Stock in, stock out, and stock counts, with low-stock warnings.
• Stock per branch or shared.

4. EXCEL (.XLSX) & PDF
• The Mankeu Excel template: fill it once, import many records at a time, and undo if needed.
• Excel export that imports back, and a printable PDF report.

5. 100% OFFLINE & PRIVATE
• All data stays on your phone (SQLite Room).
• No ads, no trackers, no required login.

6. ENCRYPTED BACKUP
• Back up to a file or your personal Google Drive (optional).
• Encrypted with AES-256-GCM under your password.

Take control of your business finances with Mankeu!
```

---

## 4. Visual Store Assets Inventory (`docs/assets/store/`)

The following visual assets are prepared and cataloged in `docs/assets/store/`:

| Asset Name | Resolution / Format | Purpose | File Location |
| :--- | :--- | :--- | :--- |
| **App Icon** | 512 x 512 px PNG (32-bit) | Launcher & Google Play Store Icon | `app/src/main/res/mipmap-xxxhdpi/ic_launcher.png` |
| **Feature Graphic** | 1024 x 500 px JPEG/PNG | Google Play Store Banner Header | `docs/assets/store/feature-graphic.png` |
| **Screenshot 01** | High-Res PNG Phone | Beranda & Ringkasan Keuangan | `docs/assets/store/screenshot-01-beranda.png` |
| **Screenshot 02** | High-Res PNG Tablet/Phone | Kelayakan & BEP | `docs/assets/store/screenshot-02-kelayakan-bep-hpp.png` |
| **Screenshot 03** | High-Res PNG Phone | Impor Templat Excel Mankeu | `docs/assets/store/screenshot-03-impor-excel.png` |
| **Screenshot 04** | High-Res PNG Phone | Laporan Rincian Arus Kas & Biaya Operasional | `docs/assets/store/screenshot-04-laporan-keuangan.png` |
| **Screenshot 05** | High-Res PNG Tablet/Phone | Buku Kas Terisolasi (Realisasi vs Proyeksi Anggaran) | `docs/assets/store/screenshot-05-buku-kas-anggaran.png` |
| **Screenshot 06** | High-Res PNG Phone | Pengaturan Akun & Cadangan Terenkripsi AES-256-GCM | `docs/assets/store/screenshot-06-cadangan-terenkripsi.png` |

---

## 5. Contact & Support Information

* **Developer Name:** Mankeu
* **Support Email:** `adsurkasur.dev@gmail.com`
* **Website / Privacy Policy:** `https://adsurkasur.my.id/mankeu/privacy-policy`
* **Target Category:** Productivity / Business / Tools
* **Primary Tags:** Agriculture, Farm Finance, Calculator, Bookkeeping, Agribusiness, Feasibility