"""Pre-release scale simulations (2026-10-22): one micro, one small and one medium business.

Each is a full year of realistic cash records, written as a Mankeu template (v4) for the import
path an experienced owner or admin would use. Expected figures are computed here from the rows
themselves, not by calling the app:
- cash in, cash out, closing cash;
- operating revenue, operating cost and cash profit (capital, drawings and assets excluded);
- cash per month (net) and the deepest month.

Scale follows PP 7/2021 (annual sales): micro <= Rp2 M, small Rp2-15 M, medium Rp15-50 M.
Run: python -I make_sims.py  (needs openpyxl). The .xlsx files are generated, not committed.
"""
import json
import os
import random
from datetime import date, timedelta

from openpyxl import Workbook

OUT = os.path.dirname(os.path.abspath(__file__))
TX = ["Tanggal", "Keterangan", "Kategori", "Pemasukan (Rp)", "Pengeluaran (Rp)", "Produk", "Jumlah", "Sifat Biaya", "Umur Pakai (bulan)"]
BUDGET = ["Bulan", "Kategori", "Item", "Jumlah", "Satuan", "Harga Satuan (Rp)", "Total (Rp)", "Berulang", "Sifat Biaya", "Umur Pakai (bulan)"]
PRODUCT = ["Nama", "Satuan", "Harga Jual (Rp)", "Target per Bulan", "Mulai Dijual (bulan ke-)"]
RECIPE = ["Produk", "Komponen", "Biaya (Rp)", "Untuk Berapa Unit"]
STOCK = ["Nama", "Satuan", "Harga Beli (Rp)", "Jumlah Stok", "Batas Minimum"]

JUAL, LAIN = "Penjualan & Jasa", "Pendapatan Lain"
BAHAN, GAJI, SEWA, LISTRIK, TRANSPORT, LAINLAIN = (
    "Bahan Baku & Stok", "Gaji & Upah", "Sewa Tempat", "Listrik, Air & Internet", "Transportasi", "Biaya Lain-lain")
MODAL, PRIVE, ASET = "Setoran Modal Pemilik", "Ambil untuk Pribadi (Prive)", "Beli Peralatan & Aset"
INCOMING = {JUAL, LAIN, MODAL}
NON_OPERATING = {MODAL, PRIVE, ASET}
START = date(2025, 11, 1)
DAYS = 365


def days():
    return [START + timedelta(d) for d in range(DAYS)]


def round_to(x, step):
    return int(round(x / step) * step)


def warung(rng):
    """Mikro: warung kelontong Bu Ani. Daily totals, as a novice would write them."""
    rows = [(START, "Modal awal dari tabungan", MODAL, 5_000_000, None)]
    rows.append((START, "Rak dan etalase bekas", ASET, None, 1_200_000, None, None, None, 36))
    for d in days():
        weekend = d.weekday() >= 5
        rows.append((d, "Jualan hari ini", JUAL, round_to(rng.gauss(850_000 if weekend else 650_000, 90_000), 500), None, None, None))
        if d.weekday() in (0, 3):
            rows.append((d, "Kulakan di grosir", BAHAN, None, round_to(rng.gauss(1_950_000, 200_000), 1000)))
        if d.weekday() == 6:
            rows.append((d, "Ambil untuk dapur", PRIVE, None, round_to(rng.gauss(400_000, 60_000), 1000)))
        if d.day == 5:
            rows.append((d, "Listrik token dan air", LISTRIK, None, 180_000))
            rows.append((d, "Sewa kios", SEWA, None, 600_000))
    return dict(jenis="Mikro: warung kelontong, catatan harian ringkas, pemilik awam", tx=rows, units=["Warung Bu Ani"])


def bengkel(rng):
    """Kecil: bengkel motor dua montir, per servis + suku cadang."""
    rows = [(START, "Modal awal", MODAL, 60_000_000, None),
            (START + timedelta(2), "Kompresor dan alat bengkel", ASET, None, 38_000_000, None, None, None, 60)]
    jobs = [("Servis ringan", 95_000, 18), ("Servis besar", 320_000, 5), ("Ganti oli", 70_000, 22), ("Tambal ban", 20_000, 10)]
    for d in days():
        if d.weekday() == 6:
            continue
        for name, price, per_day in jobs:
            n = max(0, int(rng.gauss(per_day, per_day * 0.25)))
            if n:
                rows.append((d, f"{name} ({n} motor)", JUAL, n * price, None, name, str(n)))
        rows.append((d, "Oli dan bahan habis servis", BAHAN, None, round_to(rng.gauss(1_700_000, 200_000), 1000)))
        rows.append((d, "Penjualan suku cadang", JUAL, round_to(rng.gauss(3_900_000, 500_000), 1000), None))
        if d.weekday() in (0, 2, 4):
            rows.append((d, "Belanja suku cadang dan oli", BAHAN, None, round_to(rng.gauss(7_400_000, 600_000), 1000)))
        if d.weekday() == 5:
            rows.append((d, "Upah montir dan kasir mingguan", GAJI, None, 5 * 1_150_000))
        if d.day == 1:
            rows.append((d, "Sewa ruko", SEWA, None, 4_500_000))
            rows.append((d, "Listrik dan internet", LISTRIK, None, round_to(rng.gauss(850_000, 80_000), 1000)))
            rows.append((d, "Ambil gaji pemilik", PRIVE, None, 12_000_000))
    products = [(n, "servis", p, per * 26, 1, [("Oli dan bahan habis", round_to(p * 0.25, 500), 1)]) for n, p, per in jobs]
    return dict(jenis="Kecil: bengkel motor empat montir, pemilik paham, mencatat per servis", tx=rows, units=["Bengkel Jaya Motor"], products=products)


def penggilingan(rng):
    """Menengah: penggilingan dan distribusi beras, admin mencatat setiap faktur. One file per unit."""
    units = {}
    for unit, scale in (("Pabrik Klaten", 1.0), ("Gudang Solo", 0.6), ("Gudang Yogya", 0.45)):
        rows = [(START, "Setoran modal kerja", MODAL, round_to(1_500_000_000 * scale, 1_000_000), None)]
        if unit == "Pabrik Klaten":
            rows.append((START + timedelta(3), "Mesin penggiling (RMU) dan pengering", ASET, None, 850_000_000, None, None, None, 120))
            rows.append((START + timedelta(4), "Truk engkel", ASET, None, 320_000_000, None, None, None, 96))
        for d in days():
            if d.weekday() == 6:
                continue
            for i in range(max(1, int(rng.gauss(26 * scale, 4)))):
                karung = rng.choice((10, 20, 40, 60, 100))
                price = rng.choice((62_000, 64_500, 68_000))
                rows.append((d, f"Faktur {d:%y%m%d}-{i:03d} beras {karung} karung", JUAL, karung * price, None, "Beras 5 kg", str(karung)))
            for i in range(max(1, int(rng.gauss(2.6 * scale, 0.6)))):
                ton = rng.choice((3, 4, 5, 6))
                rows.append((d, f"Beli gabah petani {ton} ton", BAHAN, None, ton * round_to(rng.gauss(6_300_000, 150_000), 1000)))
            rows.append((d, "Solar dan bongkar muat", TRANSPORT, None, round_to(rng.gauss(1_100_000 * scale, 150_000), 1000)))
            if d.weekday() == 5:
                rows.append((d, "Upah buruh harian mingguan", GAJI, None, round_to(28 * 600_000 * scale, 1000)))
            if d.day == 25:
                rows.append((d, "Gaji karyawan tetap", GAJI, None, round_to(42_000_000 * scale, 1000)))
                rows.append((d, "Listrik pabrik/gudang", LISTRIK, None, round_to(rng.gauss(18_000_000 * scale, 1_000_000), 1000)))
            if d.day == 1 and unit != "Pabrik Klaten":
                rows.append((d, "Sewa gudang", SEWA, None, round_to(15_000_000 * scale, 1000)))
            if d.day == 28:
                rows.append((d, "Penjualan dedak dan sekam", LAIN, round_to(rng.gauss(9_000_000 * scale, 900_000), 1000), None))
        units[unit] = rows
    products = [("Beras 5 kg", "karung", 64_800, 25_000, 1, [("Gabah 8,5 kg", 52_000, 1), ("Karung dan jahit", 1_400, 1)])]
    return dict(jenis="Menengah: penggilingan + distribusi beras, 3 unit, admin terlatih, setiap faktur dicatat",
                units=list(units), tx_by_unit=units, products=products)


def normalize(row):
    d, desc, cat, inc, out, *rest = row
    rest = (list(rest) + [None] * 4)[:4]
    return [d.isoformat(), desc, cat, inc, out] + rest


def expected(rows):
    inc = sum(r[3] or 0 for r in rows)
    out = sum(r[4] or 0 for r in rows)
    revenue = sum(r[3] or 0 for r in rows if r[2] not in NON_OPERATING)
    cost = sum(r[4] or 0 for r in rows if r[2] not in NON_OPERATING)
    by_month = {}
    for r in rows:
        m = r[0][:7]
        by_month[m] = by_month.get(m, 0) + (r[3] or 0) - (r[4] or 0)
    cum, low = 0, 0
    for m in sorted(by_month):
        cum += by_month[m]
        low = min(low, cum)
    return dict(rows=len(rows), cash_in=inc, cash_out=out, closing_cash=inc - out, revenue=revenue, operating_cost=cost,
                cash_profit=revenue - cost, net_by_month=dict(sorted(by_month.items())), lowest_cumulative=low,
                annual_sales=sum(r[3] or 0 for r in rows if r[2] == JUAL))


def write(name, rows, products):
    wb = Workbook()
    ws = wb.active
    ws.title = "Petunjuk"
    ws.append(["Kunci", "Nilai"])
    ws.append(["Format", "MANKEU_TEMPLATE"])
    t = wb.create_sheet("Transaksi")
    t.append(TX)
    for r in rows:
        t.append(r)
    wb.create_sheet("Rencana Anggaran").append(BUDGET)
    p = wb.create_sheet("Produk")
    p.append(PRODUCT)
    rc = wb.create_sheet("Resep Produk")
    rc.append(RECIPE)
    for pname, unit, price, target, start, comps in products:
        p.append([pname, unit, price, target, start])
        for c, cost, y in comps:
            rc.append([pname, c, cost, y])
    wb.create_sheet("Stok Barang").append(STOCK)
    path = os.path.join(OUT, f"{name}.xlsx")
    wb.save(path)
    return os.path.getsize(path)


rng = random.Random(20261022)
result = {}
for key, sim in (("M1_warung", warung(rng)), ("K1_bengkel", bengkel(rng)), ("N1_penggilingan", penggilingan(rng))):
    by_unit = sim.get("tx_by_unit") or {sim["units"][0]: sim["tx"]}
    for unit, raw in by_unit.items():
        rows = [normalize(r) for r in raw]
        name = f"{key}_{unit.replace(' ', '_')}"
        size = write(name, rows, sim.get("products", []))
        result[name] = dict(jenis=sim["jenis"], unit=unit, file_bytes=size, **expected(rows))
json.dump(result, open(os.path.join(OUT, "expected.json"), "w", encoding="utf-8"), indent=1, ensure_ascii=False)
for k, v in result.items():
    print(f"{k:40} rows={v['rows']:6} sales={v['annual_sales']:>16,} profit={v['cash_profit']:>15,} cash={v['closing_cash']:>15,} low={v['lowest_cumulative']:>14,} {v['file_bytes']:>9,}B")
