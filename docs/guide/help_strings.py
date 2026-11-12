"""Source of the in-app guide text (Pusat Bantuan), Indonesian and English.

Edit here, then run `python -I docs/guide/help_strings.py` from the repo root: it rewrites the
generated block between the GUIDE markers in `app/src/main/res/values{,-en}/strings.xml`.
Steps of an article are one per line ("\n").
"""
import os
import re

S = {}


def t(key, id_, en):
    S[key] = (id_, en)


# --- Mechanism -------------------------------------------------------------------------------
t("help_center_title", "Pusat Bantuan", "Help Center")
t("help_search_hint", 'Cari bantuan, mis. stok, penjualan untuk menutup biaya, ambil data dari Excel', 'Search help, e.g. stock, sales needed to cover costs, load an Excel file')
t("help_search_empty", "Tidak ada topik yang cocok. Coba kata lain.", "No matching topic. Try another word.")
t("help_all_topics", "Semua topik", "All topics")
t("help_screenshot_desc", "Contoh tampilan: %1$s", "Example screen: %1$s")
t("help_show_in_app", "Tunjukkan di aplikasi", "Show me in the app")
t("help_related", "Topik terkait", "Related topics")
t("help_sheet_title", "Bantuan", "Help")
t("help_for_this_page", "Untuk halaman ini", "About this page")
t("help_open_center", "Buka Pusat Bantuan", "Open Help Center")
t("help_learn_how", "Pelajari caranya", "Learn how")
t("help_show_needs_unit", 'Buat usaha dan cabang dulu. Setelah itu tombol Tunjukkan bisa membawamu ke sana.', "Create a business and a unit first. Then Show me can take you there.")
t("help_group_start", "Mulai", "Getting started")
t("help_group_record", "Mencatat", "Recording")
t("help_group_plan", "Merencanakan", "Planning")
t("help_group_stock", "Stok", "Stock")
t("help_group_report", "Laporan", "Reports")
t("help_group_data", "Data dan cadangan", "Data and backup")
t("help_group_faq", "Kenapa ...?", "Why ...?")

# --- Articles ---------------------------------------------------------------------------------


def art(key, title, body, steps):
    t(f"help_{key}_title", *title)
    t(f"help_{key}_body", *body)
    t(f"help_{key}_steps", "\n".join(steps[0]), "\n".join(steps[1]))


art("start_scope",
    ("Membuat usaha dan cabang/unit", "Creating a business and a branch/unit"),
    ("Usaha adalah bisnismu, misalnya Toko Sembako Berkah. Usaha baru langsung punya satu unit bernama Utama, jadi kamu bisa langsung mencatat. Punya cabang, kebun, atau musim tanam yang ingin dipisah? Tambahkan unit lain. Catatan, rencana, dan stok selalu milik satu unit.",
     "A business is your company, for example Berkah Grocery. A new business starts with one unit called Main, so you can record right away. Have branches, fields or seasons to keep apart? Add more units. Records, plans and stock always belong to one unit."),
    (["Ketuk nama usaha di bagian atas layar.",
      "Ketuk Tambah Usaha, tulis namanya, lalu Simpan. Unit Utama dibuat otomatis.",
      'Untuk cabang lain: buka lagi bagian atas, ketuk Tambah cabang, lalu beri nama cabangnya.',
      "Untuk pindah, ketuk Pakai ini pada unit yang ingin dipakai."],
     ["Tap the business name at the top of the screen.",
      "Tap Add Business, type its name, then Save. The Main unit is created for you.",
      'For another branch: open the top again, tap Add business branch, and name it.',
      "To switch, tap Use this on the unit you want."]))
art("start_books",
    ('Rencana dan catatan nyata', "Plan and Actual"),
    ('Setiap unit punya dua catatan. Rencana adalah rencana: produk, target, biaya, dan modal. catatan nyata adalah kenyataan: uang yang benar-benar masuk dan keluar. Keduanya terpisah, lalu dibandingkan di Laporan.',
     "Every unit has two books. Plan is the plan book: products, targets, costs and capital. Actual is reality: money that really came in and went out. They stay separate and are compared in Reports."),
    (["Pilih Rencana di bagian atas untuk membuat atau mengubah rencana usaha.",
      'Pilih catatan nyata untuk mencatat transaksi harian.',
      "Buka Laporan, tab Uang vs rencana, untuk melihat seberapa jauh kenyataan dari rencana."],
     ["Choose Plan at the top to build or change the business plan.",
      "Choose Actual to record daily transactions.",
      "Open Reports, the Money vs plan tab, to see how far reality is from the plan."]))
art("start_home",
    ("Membaca Beranda", "Reading the Home screen"),
    ('Beranda merangkum bulan ini di unit yang dipilih: uang kas, untung, pendapatan, serta uang masuk dan keluar. Di catatan Rencana, Beranda menampilkan ringkasan rencana usaha.',
     "Home sums up this month in the selected unit: cash, profit, income, and money in and out. In Plan it shows a summary of the business plan."),
    (["Uang kas adalah sisa uang dari semua catatan di unit ini.",
      'Untung bulan ini adalah pendapatan bulan ini dikurangi modal barang barang terjual dan biaya lain. Angkanya sama dengan di Laporan.',
      "Komposisi biaya menunjukkan ke mana uang paling banyak keluar."],
     ["Cash is the money left from every record in this unit.",
      "Profit this month is this month's income minus the cost per item of goods sold and other costs. It is the same figure as in Reports.",
      "Cost breakdown shows where most of the money goes."]))
art("record_income",
    ("Mencatat penjualan", "Recording a sale"),
    ('Setiap uang yang masuk dari pembeli dicatat sebagai Pemasukan berkategori penjualan. Jika produkmu sudah ada di Rencana Usaha, pilih produk dan jumlahnya agar jumlah minimum penjualan bulan ini bisa dihitung.',
     "Money received from customers is recorded as income in a sales category. If your products are in the business plan, pick the product and quantity so the app can work out how much must sell to cover costs."),
    (['Pastikan catatan nyata dipilih, lalu ketuk tombol Catat.',
      "Pilih Catat Pemasukan.",
      "Isi jumlah uang, keterangan, dan kategori Penjualan & Jasa.",
      "Bila ada, pilih produk dan isi jumlah terjual. Jumlah uang mengikuti harga x jumlah.",
      "Ketuk Simpan catatan."],
     ["Make sure Actual is selected, then tap the Record button.",
      "Choose Record money in.",
      "Fill in the amount, a description and the category “Penjualan & Jasa”.",
      "If listed, pick the product and the quantity sold. The amount follows price x quantity.",
      "Tap Save record."]))
art("record_expense",
    ("Mencatat pengeluaran dan sifat biaya", "Recording an expense and its cost type"),
    ('Belanja bahan, gaji, sewa, dan listrik dicatat sebagai Pengeluaran. Biaya rutin tetap sama saat penjualan berubah. Biaya lain bisa naik atau turun mengikuti penjualan; pembagian ini dipakai untuk menghitung penjualan yang menutup biaya.',
     'Ingredients, wages, rent and electricity are recorded as expenses. Regular costs stay the same when sales change. Other costs may rise or fall with sales; this distinction is used to calculate the sales needed to cover costs.'),
    (["Ketuk tombol Catat, lalu Catat Pengeluaran.",
      "Isi jumlah, keterangan, dan kategorinya.",
      'Jika pengeluaran ini berbeda dari biasanya, ubah sifat biayanya menjadi Tetap atau berubah mengikuti penjualan.',
      "Ketuk Simpan catatan."],
     ["Tap the Record button, then Record money out.",
      "Fill in the amount, a description and its category.",
      "If needed, choose whether this cost is regular or changes with sales.",
      "Tap Save record."]))
art("record_capital",
    ("Setoran modal, ambil pribadi, dan beli alat", "Capital, personal withdrawals and equipment"),
    ('Setoran uang pribadi, uang usaha yang dipakai untuk keperluan pribadi, dan pembelian alat bukan pendapatan atau biaya. Ketiganya mengubah uang di tangan, tetapi tidak mengubah untung.',
     'Personal money added to the business, business money used for personal needs, and equipment purchases are not income or costs. They change cash on hand but not profit.'),
    (["Uang pribadi yang dimasukkan ke usaha: Pemasukan, kategori Setoran Modal Pemilik.",
      'Uang usaha yang dipakai untuk keperluan pribadi: catat sebagai uang keluar dan pilih kategori ambil pribadi.',
      'Membeli alat yang dipakai bertahun-tahun: catat sebagai uang keluar dan isi lama pemakaiannya dalam bulan.'],
     ["Personal money added to the business: record money in and choose the capital category.",
      "Business money used for personal needs: record money out and choose the personal use category.",
      "Equipment used for years: record money out and enter how many months it will be used."]))
art("record_fix",
    ("Memperbaiki catatan yang salah", "Fixing a wrong record"),
    ("Setiap kartu catatan punya dua tombol: Ubah (ikon pensil) dan Hapus (ikon tempat sampah). Ubah membuka formulir yang sudah terisi; setelah disimpan, catatan lama diganti dengan yang benar. Hapus membuang catatan itu untuk selamanya.",
     "Every record card has two buttons: Edit (pencil icon) and Delete (bin icon). Edit opens the form already filled in; once saved, the old record is replaced by the correct one. Delete removes the record for good."),
    (["Buka Catatan dan cari catatan yang salah.",
      "Ketuk Ubah pada kartunya. Formulir terbuka dengan isi catatan itu.",
      'Perbaiki jumlah, tanggal, kategori, atau keterangannya, lalu ketuk Simpan.',
      "Catatan yang sama sekali tidak perlu: ketuk Hapus, lalu konfirmasi."],
     ["Open Records and find the wrong record.",
      "Tap Edit on its card. The form opens with that record filled in.",
      "Fix the amount, date, category or description, then tap Save.",
      "A record that should not exist at all: tap Delete, then confirm."]))
art("plan_create",
    ("Membuat Rencana Usaha", "Building a business plan"),
    ("Rencana Usaha ada di Catatan saat Rencana dipilih. Dari isian ini aplikasi menghitung titik impas, laba per bulan, dan modal yang dibutuhkan.",
     'The business plan lives in Records when Plan is selected. From it the app calculates the minimum sales needed to avoid a loss, profit per month and the capital you need.'),
    (["Pilih Rencana di bagian atas, lalu buka Catatan.",
      "Atur periode rencana, misalnya 12 bulan.",
      "Tambahkan produk dengan harga, target per bulan, dan resep biayanya.",
      'Tambahkan biaya tetap bulanan, biaya lain, alat, dan setoran modal.',
      "Lihat kartu Kelayakan di atas untuk hasilnya."],
     ["Choose Plan at the top, then open Records.",
      "Set the plan period, for example 12 months.",
      "Add products with price, monthly target and cost recipe.",
      "Add monthly fixed costs, other costs, assets and capital.",
      "Check the Feasibility card at the top for the result."]))
art("plan_products",
    ("Produk dan resep biaya", "Products and cost recipes"),
    ('Resep diisi untuk sekali produksi: bahan dan biaya lain untuk satu kali produksi, dan berapa satuan terkecil yang dihasilkan. Mankeu membagi biaya sekali produksi itu menjadi modal barang per bungkus, per dus, atau satuan jual lain.',
     'A recipe is entered per production run: the materials and other costs of one production run, and how many of the smallest unit it makes. Mankeu divides that production run cost into the cost per pack, per box or any other selling unit.'),
    (["Buka Produk, lalu ketuk Tambah produk (atau Tambah pada bagian Produk di Rencana Usaha).",
      'Isi nama, satuan terkecil, dan berapa satuan yang dihasilkan satu kali produksi.',
      'Tambahkan bahan dan biaya lain untuk satu kali produksi.',
      'Isi satuan jual dengan isi dan harganya. modal barang dan untung tiap satuan tampil di bawahnya.'],
     ["Open Products, then tap Add product (or Add in the Products section of the business plan).",
      'Fill in the name, the smallest unit and how many of it one production run makes.',
      'Add the materials and other costs of one production run.',
      "Fill in the selling units with their content and price. Cost and profit per unit show below each."]))
art("plan_seasonal",
    ("Produk musiman", "Seasonal products"),
    ("Hasil panen yang baru dijual beberapa bulan setelah tanam diisi dengan Mulai dijual bulan ke-. Bulan sebelumnya tanpa penjualan, sehingga kebutuhan modal ikut menghitung masa menunggu panen.",
     "A harvest that only sells some months after planting uses Sells from month. Earlier months have no sales, so the capital you need includes the wait for the harvest."),
    (["Buka produknya di Rencana Usaha.",
      "Isi Mulai dijual bulan ke-, misalnya 4 bila panen di bulan keempat.",
      'Simpan. Kartu Kelayakan menyebut bahwa angka per bulan adalah rata-rata periode dan jumlah minimum penjualan produk dihitung per bulan berjualan.'],
     ["Open the product in the business plan.",
      "Fill in Sells from month, for example 4 if you harvest in the fourth month.",
      "Save. The Feasibility card notes that monthly figures are period averages and the minimum for each product is counted only in selling months."]))
art("plan_costs",
    ('Biaya rutin, biaya lain, alat, dan modal', 'Regular costs, other costs, equipment and capital'),
    ('Biaya rutin dibayar setiap bulan. Biaya sekali bayar dibagi rata selama periode rencana. Harga alat dibagi menjadi biaya bulanan. Setoran modal tidak mengubah untung, tetapi menutup kebutuhan uang.',
     'Regular costs are paid every month. One-time costs are spread across the plan period. Equipment costs are counted monthly. Capital contributions do not change profit but cover the cash needed.'),
    (["Biaya Tetap per Bulan: sewa, gaji, listrik.",
      "Biaya dan Pendapatan Lain: biaya atau pendapatan sekali waktu, beserta bulannya.",
      'Alat: harga beli dan lama pemakaian.',
      "Setoran Modal: uang yang akan kamu setor ke usaha."],
     ["Monthly fixed costs: rent, wages, electricity.",
      "Other costs and income: one-off amounts and their month.",
      "Equipment: purchase price and how long it will be used.",
      "Capital: money you will put into the business."]))
art("plan_feasibility",
    ('Membaca hasil usaha dan kebutuhan modal', 'Reading business results and capital needed'),
    ('Penjualan minimum adalah jumlah yang perlu dijual agar pendapatan menutup biaya. Perbandingan di atas 1 berarti pendapatan lebih besar dari biaya. Kebutuhan modal adalah kekurangan uang terbesar sebelum ada setoran modal.',
     'Minimum sales are the amount you need to sell for income to cover costs. A comparison above 1 means income is greater than costs. Capital needed is the largest cash shortfall before a contribution.'),
    (["Omzet impas per bulan: bila target omzet di atasnya, rencana layak.",
      'jumlah minimum penjualan per produk: jumlah yang harus terjual tiap bulan.',
      'Laba per bulan dan penjualan dibanding biaya: hasil pada target penjualan.',
      "Kebutuhan modal, setoran direncanakan, dan masih kurang: tambah Setoran Modal bila masih kurang."],
     ['minimum sales revenue needed to avoid a loss per month: if the revenue target is above it, the plan works.',
      'minimum units per product to avoid a loss: how many must sell each month.',
      'Profit per month and sales-to-cost comparison: the result at the sales target.',
      "Capital need, planned capital and still missing: add Capital if something is still missing."]))
art("stock_owner",
    ("Stok milik unit dan stok bersama", "Unit stock and shared stock"),
    ("Barang bisa milik satu unit saja atau dipakai semua unit dalam usaha. Barang milik unit tidak tampil di cabang lain. Barang bersama, misalnya isi gudang pusat, tampil di semua unit dengan label dipakai semua unit.",
     "An item belongs to one unit or is shared by every unit of the business. A unit's own items do not show in other branches. Shared items, like a central store, show in every unit labelled shared."),
    (["Buka Produk, lalu di bagian Bahan & barang ketuk Tambah Barang.",
      "Isi nama, satuan, harga beli, stok awal, dan batas minimum.",
      "Di Barang ini milik, pilih unit ini atau Semua unit.",
      "Simpan."],
     ["Open Products, then under Materials & goods tap Add item.",
      "Fill in name, unit, purchase price, opening stock and minimum level.",
      "Under This item belongs to, choose the unit or All units.",
      "Save."]))
art("stock_move",
    ('Barang masuk, keluar, dan stok hitung ulang stok', "Stock in, out and recounts"),
    ('Setiap perubahan jumlah barang dicatat sebagai mutasi. hitung ulang stok dipakai saat menghitung ulang barang di rak dan hasilnya berbeda dari catatan.',
     "Every change in quantity is recorded as a movement. A recount is used when you count the shelf and it differs from the records."),
    (["Ketuk barang di bagian Bahan & barang pada tab Produk.",
      'Pilih Masuk, Keluar, atau hitung ulang stok.',
      "Isi jumlah dan catatan, lalu simpan.",
      "Barang di bawah batas minimum diberi tanda peringatan."],
     ["Tap an item under Materials & goods on the Products tab.",
      "Choose In, Out or Count.",
      "Fill in the quantity and a note, then save.",
      "Items below their minimum level are flagged."]))
art("report_profit",
    ('Laba rugi dan pergerakan uang', 'Profit and money movement'),
    ('Laba adalah pendapatan dikurangi biaya. pergerakan uang menunjukkan uang yang benar-benar bergerak, dibagi tiga: operasi (jual beli), investasi (beli alat), dan pembiayaan (modal dan ambil pribadi).',
     'Profit is income minus costs. money movement shows money that actually moved, in three parts: operations (trading), investing (equipment) and financing (capital and withdrawals).'),
    (['Buka Laporan, tab Laba Rugi dan pergerakan uang.',
      "Laporan Laba Rugi menunjukkan bulan ini: pendapatan, biaya, dan untung bulan ini (setelah penyusutan alat).",
      'pergerakan uang menjelaskan kenapa uang kas berbeda dari laba.'],
     ['Open Reports, the Profit, Loss and money movement tab.',
      "The profit and loss card shows this month: income, costs and profit this month (after equipment wear).",
      'money movement explains why cash differs from profit.']))
art("report_bep",
    ('Penjualan minimum bulan ini', 'Minimum sales this month'),
    ('Aplikasi membandingkan penjualan bulan ini dengan jumlah yang diperlukan untuk menutup biaya, menghitung jumlah terjual per produk, dan membandingkan belanja produksi dengan modal barang yang terjual.',
     "The app compares this month's sales with the amount needed to cover costs, counts items sold per product, and compares production spending with the cost of items sold."),
    (['Pastikan catatan nyata dipilih, lalu buka Laporan atau Beranda.',
      'Lihat apakah penjualan bulan ini sudah menutup biaya.',
      'Belanja produksi yang lebih besar dari modal barang terjual bisa berarti stok bertambah atau modal barang terlalu rendah. Selisihnya sama dengan di Laporan.',
      "Bila belum ada penjualan bulan ini, statusnya Belum Dapat Dinilai."],
     ["Make sure Actual is selected, then open Reports or Home.",
      "See whether this month's sales cover the costs.",
      'Production spending above the cost of items sold can mean stock grew or item costs are too low. The difference matches Reports.',
      "With no sales this month the status is Not yet assessable."]))
art("report_target",
    ("Uang vs rencana", "Money vs plan"),
    ("Tab ini membandingkan uang masuk dan keluar rata-rata per bulan dari rencana dengan catatan nyata, sehingga kamu tahu pos mana yang meleset. Ini uang, bukan untung.",
     "This tab compares the plan's average money in and out per month with the actual records, so you see which items are off. This is money, not profit."),
    (['Buat Rencana Usaha di catatan Rencana lebih dulu.',
      'Catat transaksi di catatan nyata.',
      "Buka Laporan, tab Uang vs rencana."],
     ["Build the business plan in Plan first.",
      "Record transactions in Actual.",
      "Open Reports, the Money vs plan tab."]))
art("report_export",
    ('simpan ke Excel Excel dan PDF', "Exporting to Excel and PDF"),
    ("Excel berisi seluruh data unit ini dalam format template Mankeu, sehingga bisa diimpor lagi tanpa kehilangan data. PDF berisi laporan yang siap dibaca atau dicetak.",
     "Excel holds all of this unit's data in the Mankeu template format, so it can be imported again without loss. PDF holds a report ready to read or print."),
    (["Ketuk ikon map di bagian atas untuk membuka Berkas dan Data.",
      'Di simpan ke Excel dan cadangan, pilih Excel, PDF, atau Template Excel.',
      "Pilih tempat menyimpan berkas. Nama berkas memuat nama unit dan tanggal."],
     ["Tap the folder icon at the top to open Files and Data.",
      "Under Export and backup, choose Excel, PDF or Excel template.",
      "Choose where to save. The file name includes the unit and the date."]))
art("data_import",
    ('ambil dari Excel dari template Excel', "Importing from the Excel template"),
    ("Mankeu hanya membaca template-nya sendiri. Isinya ditambahkan ke unit yang sedang dipilih setelah kamu melihat pratinjau. Kategori baru dibuat otomatis, barang dan produk yang namanya sudah ada dilewati.",
     "Mankeu only reads its own template. The content is added to the selected unit after you see a preview. New categories are created, and items and products whose names already exist are skipped."),
    (['Di Berkas dan Data, simpan ke Excel Template lalu isi di Excel.',
      "Pilih unit tujuan, lalu ketuk Pilih file .xlsx.",
      "Periksa pratinjau: jumlah baris, uang masuk dan keluar, kategori baru, dan peringatan.",
      'Ketuk Setujui dan ambil dari Excel.'],
     ["In Files and Data, export the Template and fill it in Excel.",
      "Select the target unit, then tap Choose an .xlsx file.",
      "Check the preview: row count, money in and out, new categories and warnings.",
      "Tap Approve and import."]))
art("data_undo",
    ('Membatalkan ambil dari Excel', "Undoing an import"),
    ('Setiap ambil dari Excel tercatat di riwayat. Membatalkannya menghapus semua transaksi yang ditambahkan ambil dari Excel itu, mengarsipkan baris rencananya, menghapus produk dan stok yang ditambahkan, dan mengosongkan periode rencana yang diisinya.',
     "Every import is listed in the history. Undoing it deletes all the transactions it added, archives its plan lines, removes the products and stock it added, and clears the plan period it filled in."),
    (["Buka Berkas dan Data, bagian Riwayat pengambilan.",
      'Ketuk Batalkan ambil dari Excel pada berkas yang salah, lalu konfirmasi.',
      "Riwayatnya tetap ada dengan status dibatalkan."],
     ["Open Files and Data, the import history section.",
      "Tap Undo import on the wrong file, then confirm.",
      "The history stays, marked as undone."]))
art("data_sync",
    ("Sinkronisasi antar-HP", "Sync between phones"),
    ('Sync membuat beberapa HP memakai catatan yang sama, misalnya HP pemilik dan HP kasir. Semua HP masuk dengan akun Google yang sama, dan memakai kata sandi sync yang sama. Data dikunci kata sandi itu sebelum dikirim ke folder aplikasi di Google Drive. Sync berjalan sendiri saat aplikasi dibuka dan beberapa detik setelah ada perubahan. Kategori, barang, atau produk dengan nama sama yang dibuat di dua HP digabung, lalu aplikasi memberi tahu di Beranda.',
     "Sync lets several phones use the same books, for example the owner's and the cashier's. Every phone signs in with the same Google account and uses the same sync passphrase. Data is locked with that passphrase before it goes to the app's folder on Google Drive. Sync runs by itself when the app opens and a few seconds after a change. Categories, stock items or products with the same name created on two phones are merged, and the app says so on Beranda."),
    (["Buka Pengaturan, lalu masuk dengan akun Google usaha.",
      "Di Sinkronisasi antar-HP, ketuk Nyalakan sync dan buat kata sandi sync minimal 8 karakter.",
      "Di HP lain, masuk dengan akun Google yang sama dan ketik kata sandi sync yang sama.",
      "Untuk menyinkronkan saat itu juga, ketuk Sinkronkan sekarang."],
     ["Open Settings and sign in with the business Google account.",
      "In Sync between phones, tap Turn on sync and choose a sync passphrase of at least 8 characters.",
      "On the other phone, sign in with the same Google account and type the same sync passphrase.",
      "To sync right away, tap Sync now."]))
art("data_backup",
    ("Cadangan terenkripsi dan Google Drive", "Encrypted backup and Google Drive"),
    ("Cadangan menyimpan seluruh data di HP ini dalam satu berkas yang dikunci kata sandimu. Kata sandi tidak disimpan aplikasi, jadi catat di tempat aman. Cadangan bisa disimpan di HP atau di Google Drive pribadimu.",
     "A backup stores all data on this phone in one file locked with your password. The app does not keep the password, so write it down somewhere safe. Backups can be stored on the phone or in your own Google Drive."),
    (["Buka Berkas dan Data, lalu ketuk Cadangan terenkripsi.",
      "Buat kata sandi minimal 8 karakter.",
      "Simpan berkasnya di HP, atau pilih Google Drive setelah masuk akun Google di Pengaturan."],
     ["Open Files and Data, then tap Encrypted backup.",
      "Choose a password of at least 8 characters.",
      "Save the file on the phone, or choose Google Drive after signing in with Google in Settings."]))
art("data_restore",
    ("Pindah HP atau memulihkan data", "Moving phones or restoring data"),
    ("Pemulihan mengganti semua data di HP ini dengan isi cadangan. Sebelum itu aplikasi otomatis membuat salinan data sekarang, sehingga bisa dikembalikan bila salah berkas.",
     "Restoring replaces all data on this phone with the backup. Before that the app automatically copies the current data, so it can be brought back if it was the wrong file."),
    (["Di HP lama, buat cadangan dan simpan berkasnya atau unggah ke Google Drive.",
      "Di HP baru, buka Berkas dan Data, Cadangan terenkripsi, lalu Pulihkan dari berkas.",
      "Pilih berkasnya, masukkan kata sandi, periksa pratinjau, lalu konfirmasi."],
     ["On the old phone, make a backup and keep the file or upload it to Google Drive.",
      "On the new phone, open Files and Data, Encrypted backup, then Restore from a file.",
      "Pick the file, enter the password, check the preview, then confirm."]))
art("faq_cash",
    ("Kenapa kas saya minus?", "Why is my cash negative?"),
    ("Biasanya karena modal awal belum dicatat. Aplikasi hanya tahu uang yang kamu catat, jadi belanja pertama terlihat seperti uang yang belum pernah ada.",
     "Usually the starting capital has not been recorded. The app only knows the money you record, so the first purchases look like money that never existed."),
    (["Catat Pemasukan dengan kategori Setoran Modal Pemilik sebesar uang awal usaha.",
      "Gunakan tanggal sebelum transaksi pertama.",
      "Kas akan naik, sedangkan laba tidak berubah."],
     ["Record money in, category “Setoran Modal Pemilik”, for the money you started with.",
      "Use a date before the first transaction.",
      "Cash goes up while profit stays the same."]))
art("faq_bep",
    ('Kenapa penjualan minimum tidak muncul?', 'Why is the minimum sales amount missing?'),
    ('Jumlah penjualan minimum untuk seluruh usaha perlu target bulanan pada setiap produk dan harga jual di atas biaya barang. Tanpa produk, jumlahnya belum bisa dihitung.',
     'The minimum sales amount for the whole business needs a monthly target for every product and a price above its item cost. Without products, it cannot be calculated.'),
    (['Buka Rencana Usaha di catatan Rencana.',
      "Pastikan setiap produk punya target per bulan.",
      'Periksa hasil di form produk: bila merah, harga jual lebih rendah dari biaya barang.'],
     ["Open the business plan in Plan.",
      "Make sure every product has a monthly target.",
      'Check the result in the product form: red means the selling price is below the item cost.']))
art("faq_import",
    ('Kenapa berkas ambil dari Excel ditolak?', "Why was my import file rejected?"),
    ("Mankeu hanya membaca template Mankeu dengan nama lembar dan judul kolom yang persis sama. Pesan penolakan menyebut lembar dan sel yang salah.",
     "Mankeu only reads its own template, with exactly the same sheet names and column headers. The rejection message names the sheet and cell that is wrong."),
    (["Baca daftar masalah di pratinjau, misalnya Transaksi!A3 tanggal salah.",
      "Perbaiki sel itu di Excel. Tanggal ditulis YYYY-MM-DD.",
      'Jangan ganti nama lembar atau judul kolom. Bila ragu, simpan ke Excel template baru dan salin isinya.'],
     ["Read the problem list in the preview, for example Transaksi!A3 wrong date.",
      "Fix that cell in Excel. Dates are written YYYY-MM-DD.",
      "Do not rename sheets or column headers. If unsure, export a fresh template and copy your data in."]))
art("faq_stock",
    ("Kenapa barang tidak tampil di cabang lain?", "Why doesn't an item show in another branch?"),
    ("Barang yang ditambahkan sebagai milik satu unit hanya tampil di unit itu. Ini disengaja agar stok cabang tidak tercampur.",
     "An item added as belonging to one unit only shows in that unit. This keeps branch stock apart on purpose."),
    (["Buka unit tempat barang itu dibuat.",
      "Untuk barang yang memang dipakai bersama, tambahkan sebagai Semua unit.",
      "Nama barang bersama tidak boleh sama dengan barang milik unit."],
     ["Open the unit where the item was created.",
      "For items really used by everyone, add them as All units.",
      "A shared item cannot have the same name as a unit's own item."]))
art("faq_profit",
    ("Kenapa laba berbeda dengan uang kas?", "Why is profit different from cash?"),
    ("Setoran modal menambah kas tetapi bukan laba. Ambil pribadi dan beli alat mengurangi kas tetapi bukan biaya. Penyusutan alat mengurangi laba tetapi bukan uang keluar.",
     "Capital adds cash but is not profit. Personal withdrawals and equipment reduce cash but are not costs. Equipment depreciation reduces profit but is not money going out."),
    (['Buka Laporan, tab Laba Rugi dan pergerakan uang.',
      'Bandingkan pergerakan uang Operasi dengan Investasi dan Pembiayaan.',
      "Selisih kas dan laba dijelaskan oleh dua bagian terakhir."],
     ['Open Reports, the Profit, Loss and money movement tab.',
      'Compare operating money movement with investing and financing.',
      "The difference between cash and profit is explained by the last two."]))


def xml_escape(text):
    text = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    text = text.replace("\\", "\\\\").replace("'", "\\'").replace('"', '\\"').replace("\n", "\\n")
    return text


def write(path, lang):
    s = open(path, encoding="utf-8").read()
    start, end = "    <!-- GUIDE:BEGIN generated by docs/guide/help_strings.py -->\n", "    <!-- GUIDE:END -->\n"
    s = re.sub(re.escape(start) + ".*?" + re.escape(end), "", s, flags=re.S)
    body = "".join(f'    <string name="{k}">{xml_escape(v[lang])}</string>\n' for k, v in S.items())
    s = s.replace("</resources>", start + body + end + "</resources>")
    open(path, "w", encoding="utf-8").write(s)


if __name__ == "__main__":
    root = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "app", "src", "main", "res")
    write(os.path.join(root, "values", "strings.xml"), 0)
    write(os.path.join(root, "values-en", "strings.xml"), 1)
    print(len(S), "strings")
