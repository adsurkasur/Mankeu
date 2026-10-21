#!/usr/bin/env python3
"""Generates the Mankeu end-to-end verification workbook.

Case study: "Dapur Sambal Nusantara", a culinary (F&B) MSME producing bottled
sambal over one three-month production cycle (February - April 2026).

The workbook has five coordinated sheets, in the order the reference workbook
contract expects them:

  1. Petunjuk dan Parameter          - assumptions the app cannot derive
  2. RAB (Proyeksi)                  - the plan, imported into the projection book
  3. Transaksi Harian (Realisasi)    - the actual ledger, imported into the realization book
  4. Laporan Laba Rugi               - derived, formula-driven
  5. Arus Kas dan Analisis Kelayakan - derived, formula-driven

Why this file exists rather than a hand-built .xlsx: every derived figure is
computed here with the *same* arithmetic the Kotlin domain uses (exact integer
rupiah, BigDecimal with HALF_UP for ratios), written into the workbook as a
live Excel formula *and* as the cached value that formula evaluates to. The
Mankeu importer reads cached values, Excel shows the formula, and both are the
same number by construction.

Two artefacts are produced:

  simulasi_kelayakan_umkm_verifikasi.xlsx                     (repo root)
  docs/evidence/verification-workbook/expected.json           (golden values)

`app/src/test/java/com/mankeu/importer/VerificationWorkbookTest.kt` imports the
first through the real importer and asserts it reproduces the second.

No third-party dependency: the OOXML is written with `zipfile` and string
templates, mirroring `app/src/main/java/com/mankeu/exporter/XlsxWorkbookWriter.kt`.

Usage:
    py scripts/generate_verification_workbook.py
"""

from __future__ import annotations

import json
import zipfile
from dataclasses import dataclass, field
from decimal import Decimal, ROUND_HALF_UP
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
XLSX_PATH = REPO / "docs" / "evidence" / "verification-workbook" / "simulasi_kelayakan_umkm_verifikasi.xlsx"
EXPECTED_PATH = REPO / "docs" / "evidence" / "verification-workbook" / "expected.json"

# --------------------------------------------------------------------------------------
# Domain mirror. These four helpers reproduce the Kotlin rules exactly; if one of them
# drifts, VerificationWorkbookTest fails rather than the workbook quietly lying.
# --------------------------------------------------------------------------------------

# FinancialEventType groupings, copied from ReportCalculator.kt / MonthlyCashFlowCalculator.
CASH_IN_TYPES = {"CASH_INCOME", "RECEIVABLE_COLLECTION", "OWNER_CAPITAL", "LOAN_RECEIVED"}
CASH_OUT_TYPES = {
    "CASH_EXPENSE",
    "PAYABLE_SETTLEMENT",
    "OWNER_DRAWING",
    "LOAN_PRINCIPAL_REPAID",
    "LOAN_INTEREST_PAID",
    "ASSET_PURCHASE_CASH",
}
REVENUE_TYPES = {"CASH_INCOME", "CREDIT_SALE"}
EXPENSE_TYPES = {"CASH_EXPENSE", "CREDIT_PURCHASE_EXPENSE"}
OPERATING_IN_TYPES = {"CASH_INCOME", "RECEIVABLE_COLLECTION"}
OPERATING_OUT_TYPES = {"CASH_EXPENSE", "PAYABLE_SETTLEMENT", "LOAN_INTEREST_PAID"}
FINANCING_PLUS = {"OWNER_CAPITAL", "LOAN_RECEIVED"}
FINANCING_MINUS = {"OWNER_DRAWING", "LOAN_PRINCIPAL_REPAID"}
NON_CASH_TYPES = {"CREDIT_SALE", "CREDIT_PURCHASE_EXPENSE", "CREDIT_PURCHASE_ASSET"}


def money_div(numerator: int, denominator: int) -> int:
    """Integer rupiah division, HALF_UP — matches BigDecimal.divide(scale=0, HALF_UP)."""
    return int((Decimal(numerator) / Decimal(denominator)).quantize(Decimal("1"), ROUND_HALF_UP))


def ratio(numerator: int, denominator: int, scale: int = 3) -> Decimal:
    quantum = Decimal(1).scaleb(-scale)
    return (Decimal(numerator) / Decimal(denominator)).quantize(quantum, ROUND_HALF_UP)


def percent(numerator: int, denominator: int) -> Decimal:
    return (Decimal(numerator) * 100 / Decimal(denominator)).quantize(Decimal("0.01"), ROUND_HALF_UP)


# --------------------------------------------------------------------------------------
# Parameters (sheet 1)
# --------------------------------------------------------------------------------------

BUSINESS_NAME = "Dapur Sambal Nusantara"
PROJECT_NAME = "Siklus Produksi Februari - April 2026"
SECTOR = "Kuliner / F&B"
OUTPUT_UNIT = "botol"
OUTPUT_PRICE = 28_000
PLAN_OUTPUT_QUANTITY = 6_000
OWNER_CAPITAL_PLAN = 15_000_000
LOAN_PLAN = 20_000_000
INTEREST_BPS = 1_200  # 12,00% per siklus
OTHER_FINANCING_COST = 400_000
OPENING_CASH = 0

# --------------------------------------------------------------------------------------
# RAB / plan (sheet 2)
#
# `section` is written into the Uraian column on its own row with no figures beside it;
# that is how FinanceWorkbookImporter.parseRabBudget picks up a category. Every section
# name is one of the app's own Kuliner template categories, so the imported lines land on
# categories that already carry the right role (ASSET / EXPENSE / REVENUE) instead of the
# EXPENSE default the importer falls back to for unknown names.
# --------------------------------------------------------------------------------------


@dataclass(frozen=True)
class BudgetLine:
    """One RAB line for one month.

    A budget is written per period, not as a lump: "sewa dapur, 3 bulan, Rp2.500.000" spent as a
    single February figure would be a different plan from the same money spread across the cycle,
    and it is the *timing* that decides how much working capital the owner has to raise. Every
    recurring line is therefore emitted once per month it is planned for, and `Bulan Rencana`
    carries that month into the app — see `BudgetPlanCalculator.schedule`.
    """

    name: str
    volume: Decimal
    unit: str
    unit_price: int
    section: str
    month: str

    @property
    def total(self) -> int:
        return int((self.volume * Decimal(self.unit_price)).quantize(Decimal("1"), ROUND_HALF_UP))


def once(name: str, volume, unit: str, unit_price: int, section: str, month: str) -> list[BudgetLine]:
    return [BudgetLine(name, Decimal(volume), unit, unit_price, section, month)]


def monthly(name: str, volume_per_month, unit: str, unit_price: int, section: str,
            months: list[str] | None = None) -> list[BudgetLine]:
    return [
        BudgetLine(name, Decimal(volume_per_month), unit, unit_price, section, month)
        for month in (months or PLAN_MONTHS)
    ]


PLAN_MONTHS = ["2026-02", "2026-03", "2026-04"]


ASSET_SECTION = "Peralatan Dapur dan Kafe"
REVENUE_SECTION = "Penjualan Makanan dan Minuman"

BUDGET_SECTIONS: list[tuple[str, str, list[BudgetLine]]] = [
    (
        "A. Peralatan Dapur dan Kafe",
        ASSET_SECTION,
        # Bought once, up front, which is exactly why the plan needs working capital in month one.
        once("Mesin giling cabai", 1, "unit", 4_500_000, ASSET_SECTION, "2026-02")
        + once("Kompor industri dua tungku", 2, "unit", 1_750_000, ASSET_SECTION, "2026-02")
        + once("Mesin sealer botol", 1, "unit", 2_300_000, ASSET_SECTION, "2026-02")
        + once("Wajan dan panci stainless", 1, "set", 1_200_000, ASSET_SECTION, "2026-02")
        + once("Etalase dan rak simpan", 1, "unit", 1_500_000, ASSET_SECTION, "2026-02"),
    ),
    (
        "B. Bahan Baku Makanan",
        "Bahan Baku Makanan",
        monthly("Cabai rawit merah", 400, "kg", 45_000, "Bahan Baku Makanan")
        + monthly("Bawang merah dan bawang putih", 100, "kg", 30_000, "Bahan Baku Makanan")
        + monthly("Minyak goreng curah", 200, "liter", 18_000, "Bahan Baku Makanan"),
    ),
    (
        "C. Bahan Minuman dan Bumbu",
        "Bahan Minuman dan Bumbu",
        monthly("Gula, garam, dan penyedap", 1, "paket", 2_200_000, "Bahan Minuman dan Bumbu"),
    ),
    (
        "D. Kemasan dan Label",
        "Kemasan dan Label",
        monthly("Botol kaca dan tutup", 2_000, "pcs", 2_500, "Kemasan dan Label")
        + monthly("Label dan segel", 2_000, "pcs", 500, "Kemasan dan Label"),
    ),
    (
        "E. Gaji Koki dan Pegawai",
        "Gaji Koki dan Pegawai",
        monthly("Gaji dua karyawan produksi", 2, "orang-bulan", 2_000_000, "Gaji Koki dan Pegawai"),
    ),
    (
        "F. Sewa Tempat",
        "Sewa Tempat",
        monthly("Sewa dapur produksi", 1, "bulan", 2_500_000, "Sewa Tempat"),
    ),
    (
        "G. Gas, Listrik dan Air",
        "Gas, Listrik dan Air",
        monthly("Gas, listrik, dan air dapur", 1, "bulan", 1_150_000, "Gas, Listrik dan Air"),
    ),
    (
        "H. Perizinan dan Administrasi",
        "Perizinan dan Administrasi",
        once("Perizinan PIRT dan sertifikasi halal", 1, "paket", 1_750_000, "Perizinan dan Administrasi", "2026-02")
        + monthly("Administrasi dan alat tulis kantor", 1, "bulan", 250_000, "Perizinan dan Administrasi"),
    ),
    (
        "I. Pemasaran dan Promosi",
        "Pemasaran dan Promosi",
        monthly("Konten, iklan, dan bazar UMKM", 1, "bulan", 800_000, "Pemasaran dan Promosi"),
    ),
    (
        "J. Transport dan Pengiriman",
        "Transport dan Pengiriman",
        monthly("Ongkos kirim pesanan luar kota", 1, "bulan", 600_000, "Transport dan Pengiriman"),
    ),
    (
        "K. Penjualan Makanan dan Minuman",
        REVENUE_SECTION,
        # Sales ramp as the product finds its market, which is what leaves month one short.
        once("Target penjualan sambal kemasan", 1_500, "botol", 28_000, REVENUE_SECTION, "2026-02")
        + once("Target penjualan sambal kemasan", 2_000, "botol", 28_000, REVENUE_SECTION, "2026-03")
        + once("Target penjualan sambal kemasan", 2_500, "botol", 28_000, REVENUE_SECTION, "2026-04"),
    ),
]

BUDGET_LINES = [line for _, _, lines in BUDGET_SECTIONS for line in lines]

# --------------------------------------------------------------------------------------
# Realisasi ledger (sheet 3)
# --------------------------------------------------------------------------------------


@dataclass(frozen=True)
class Txn:
    date: str
    kind: str  # Indonesian label written in the Jenis column
    event_type: str  # FinancialEventType the label maps to
    description: str
    category: str
    amount: int
    counterparty: str = ""
    due_date: str = ""
    obligation: str = ""
    volume: Decimal | None = None
    unit: str = ""
    unit_price: int | None = None


def sale(date: str, description: str, bottles: int) -> Txn:
    return Txn(
        date=date,
        kind="Uang Masuk",
        event_type="CASH_INCOME",
        description=description,
        category=REVENUE_SECTION,
        amount=bottles * OUTPUT_PRICE,
        volume=Decimal(bottles),
        unit=OUTPUT_UNIT,
        unit_price=OUTPUT_PRICE,
    )


def buy(date: str, description: str, category: str, amount: int,
        volume: Decimal | None = None, unit: str = "", unit_price: int | None = None) -> Txn:
    return Txn(date, "Uang Keluar", "CASH_EXPENSE", description, category, amount,
               volume=volume, unit=unit, unit_price=unit_price)


TRANSACTIONS: list[Txn] = [
    # ---- Februari 2026: modal masuk, belanja alat, produksi perdana -------------------
    Txn("2026-02-01", "Modal Masuk", "OWNER_CAPITAL", "Setoran modal awal pemilik", "Modal Sendiri", 15_000_000),
    Txn("2026-02-02", "Pinjaman Masuk", "LOAN_RECEIVED", "Pencairan KUR modal kerja", "Pinjaman Usaha", 20_000_000),
    Txn("2026-02-03", "Beli Alat Tunai", "ASSET_PURCHASE_CASH", "Mesin giling cabai", ASSET_SECTION, 4_500_000,
        volume=Decimal(1), unit="unit", unit_price=4_500_000),
    Txn("2026-02-03", "Beli Alat Tunai", "ASSET_PURCHASE_CASH", "Kompor industri dua tungku", ASSET_SECTION, 3_500_000,
        volume=Decimal(2), unit="unit", unit_price=1_750_000),
    Txn("2026-02-04", "Beli Alat Tunai", "ASSET_PURCHASE_CASH", "Mesin sealer botol", ASSET_SECTION, 2_300_000,
        volume=Decimal(1), unit="unit", unit_price=2_300_000),
    Txn("2026-02-04", "Beli Alat Tunai", "ASSET_PURCHASE_CASH", "Wajan dan panci stainless", ASSET_SECTION, 1_200_000,
        volume=Decimal(1), unit="set", unit_price=1_200_000),
    Txn("2026-02-05", "Beli Alat Tunai", "ASSET_PURCHASE_CASH", "Etalase dan rak simpan", ASSET_SECTION, 1_500_000,
        volume=Decimal(1), unit="unit", unit_price=1_500_000),
    buy("2026-02-05", "Sewa dapur produksi Februari", "Sewa Tempat", 2_500_000, Decimal(1), "bulan", 2_500_000),
    buy("2026-02-05", "Perizinan PIRT dan sertifikasi halal", "Perizinan dan Administrasi", 1_750_000,
        Decimal(1), "paket", 1_750_000),
    buy("2026-02-06", "Belanja cabai rawit merah", "Bahan Baku Makanan", 18_000_000, Decimal(400), "kg", 45_000),
    buy("2026-02-06", "Belanja bawang merah dan bawang putih", "Bahan Baku Makanan", 3_000_000,
        Decimal(100), "kg", 30_000),
    buy("2026-02-07", "Minyak goreng curah", "Bahan Baku Makanan", 3_600_000, Decimal(200), "liter", 18_000),
    Txn("2026-02-07", "Belanja Tempo", "CREDIT_PURCHASE_EXPENSE", "Gula, garam, dan penyedap (tempo 30 hari)",
        "Bahan Minuman dan Bumbu", 2_200_000, counterparty="Toko Sembako Jaya",
        due_date="2026-03-08", obligation="UT-01", volume=Decimal(1), unit="paket", unit_price=2_200_000),
    buy("2026-02-08", "Botol kaca dan tutup", "Kemasan dan Label", 5_000_000, Decimal(2_000), "pcs", 2_500),
    buy("2026-02-08", "Label dan segel", "Kemasan dan Label", 1_000_000, Decimal(2_000), "pcs", 500),
    sale("2026-02-15", "Penjualan kasir 01-15 Februari", 520),
    buy("2026-02-20", "Gaji dua karyawan produksi Februari", "Gaji Koki dan Pegawai", 4_000_000,
        Decimal(2), "orang-bulan", 2_000_000),
    buy("2026-02-25", "Gas, listrik, dan air Februari", "Gas, Listrik dan Air", 1_150_000),
    buy("2026-02-26", "Konten promosi dan iklan media sosial", "Pemasaran dan Promosi", 800_000),
    sale("2026-02-28", "Penjualan kasir 16-28 Februari", 640),
    Txn("2026-02-28", "Penjualan Tempo", "CREDIT_SALE", "Penjualan grosir Toko Berkah (tempo 30 hari)",
        REVENUE_SECTION, 250 * OUTPUT_PRICE, counterparty="Toko Berkah Sentosa",
        due_date="2026-03-30", obligation="PT-01", volume=Decimal(250), unit=OUTPUT_UNIT, unit_price=OUTPUT_PRICE),

    # ---- Maret 2026: produksi penuh, pelunasan utang, penagihan piutang ---------------
    buy("2026-03-05", "Sewa dapur produksi Maret", "Sewa Tempat", 2_500_000, Decimal(1), "bulan", 2_500_000),
    Txn("2026-03-08", "Bayar Utang", "PAYABLE_SETTLEMENT", "Pelunasan utang Toko Sembako Jaya",
        "Bahan Minuman dan Bumbu", 2_200_000, counterparty="Toko Sembako Jaya", obligation="UT-01"),
    buy("2026-03-10", "Belanja cabai rawit merah", "Bahan Baku Makanan", 18_900_000, Decimal(420), "kg", 45_000),
    buy("2026-03-10", "Bawang, gula, dan bumbu dapur", "Bahan Minuman dan Bumbu", 4_200_000),
    buy("2026-03-11", "Minyak goreng curah", "Bahan Baku Makanan", 3_780_000, Decimal(210), "liter", 18_000),
    Txn("2026-03-12", "Belanja Tempo", "CREDIT_PURCHASE_EXPENSE", "Botol kaca dan tutup (tempo 30 hari)",
        "Kemasan dan Label", 5_500_000, counterparty="CV Kemasan Mitra",
        due_date="2026-04-11", obligation="UT-02", volume=Decimal(2_200), unit="pcs", unit_price=2_500),
    buy("2026-03-12", "Label dan segel", "Kemasan dan Label", 1_100_000, Decimal(2_200), "pcs", 500),
    sale("2026-03-15", "Penjualan kasir 01-15 Maret", 760),
    buy("2026-03-20", "Gaji dua karyawan produksi Maret", "Gaji Koki dan Pegawai", 4_000_000,
        Decimal(2), "orang-bulan", 2_000_000),
    buy("2026-03-25", "Gas, listrik, dan air Maret", "Gas, Listrik dan Air", 1_250_000),
    buy("2026-03-26", "Ongkos kirim pesanan luar kota", "Transport dan Pengiriman", 950_000),
    buy("2026-03-26", "Iklan media sosial dan endorse", "Pemasaran dan Promosi", 1_200_000),
    Txn("2026-03-28", "Bayar Bunga", "LOAN_INTEREST_PAID", "Bunga KUR bulan Maret", "Biaya Bunga Pinjaman", 200_000),
    Txn("2026-03-28", "Bayar Pokok Pinjaman", "LOAN_PRINCIPAL_REPAID", "Angsuran pokok KUR Maret",
        "Pinjaman Usaha", 1_000_000),
    Txn("2026-03-30", "Terima Piutang", "RECEIVABLE_COLLECTION", "Pelunasan piutang Toko Berkah Sentosa",
        REVENUE_SECTION, 7_000_000, counterparty="Toko Berkah Sentosa", obligation="PT-01"),
    sale("2026-03-31", "Penjualan kasir 16-31 Maret", 880),
    Txn("2026-03-31", "Penjualan Tempo", "CREDIT_SALE", "Penjualan grosir Warung Bu Ani (tempo 30 hari)",
        REVENUE_SECTION, 300 * OUTPUT_PRICE, counterparty="Warung Bu Ani",
        due_date="2026-04-30", obligation="PT-02", volume=Decimal(300), unit=OUTPUT_UNIT, unit_price=OUTPUT_PRICE),

    # ---- April 2026: puncak penjualan, penambahan aset, prive ------------------------
    buy("2026-04-02", "Sewa dapur produksi April", "Sewa Tempat", 2_500_000, Decimal(1), "bulan", 2_500_000),
    buy("2026-04-05", "Belanja cabai rawit merah", "Bahan Baku Makanan", 20_250_000, Decimal(450), "kg", 45_000),
    buy("2026-04-05", "Bawang, gula, dan bumbu dapur", "Bahan Minuman dan Bumbu", 4_500_000),
    buy("2026-04-06", "Minyak goreng curah", "Bahan Baku Makanan", 4_050_000, Decimal(225), "liter", 18_000),
    buy("2026-04-08", "Label dan segel", "Kemasan dan Label", 1_200_000, Decimal(2_400), "pcs", 500),
    Txn("2026-04-11", "Bayar Utang", "PAYABLE_SETTLEMENT", "Pelunasan utang CV Kemasan Mitra",
        "Kemasan dan Label", 5_500_000, counterparty="CV Kemasan Mitra", obligation="UT-02"),
    sale("2026-04-15", "Penjualan kasir 01-15 April", 960),
    Txn("2026-04-18", "Beli Alat Tunai", "ASSET_PURCHASE_CASH", "Chiller penyimpanan bahan", ASSET_SECTION, 3_200_000,
        volume=Decimal(1), unit="unit", unit_price=3_200_000),
    buy("2026-04-20", "Gaji dua karyawan produksi April", "Gaji Koki dan Pegawai", 4_400_000,
        Decimal(2), "orang-bulan", 2_200_000),
    buy("2026-04-22", "Botol kaca dan tutup", "Kemasan dan Label", 6_000_000, Decimal(2_400), "pcs", 2_500),
    buy("2026-04-25", "Gas, listrik, dan air April", "Gas, Listrik dan Air", 1_320_000),
    buy("2026-04-26", "Ongkos kirim pesanan luar kota", "Transport dan Pengiriman", 1_100_000),
    buy("2026-04-26", "Iklan media sosial dan bazar UMKM", "Pemasaran dan Promosi", 1_500_000),
    Txn("2026-04-28", "Bayar Bunga", "LOAN_INTEREST_PAID", "Bunga KUR bulan April", "Biaya Bunga Pinjaman", 190_000),
    Txn("2026-04-28", "Bayar Pokok Pinjaman", "LOAN_PRINCIPAL_REPAID", "Angsuran pokok KUR April",
        "Pinjaman Usaha", 1_000_000),
    Txn("2026-04-29", "Modal Keluar", "OWNER_DRAWING", "Prive pemilik untuk kebutuhan pribadi",
        "Modal Sendiri", 2_000_000),
    Txn("2026-04-30", "Terima Piutang", "RECEIVABLE_COLLECTION", "Penerimaan sebagian Warung Bu Ani",
        REVENUE_SECTION, 5_000_000, counterparty="Warung Bu Ani", obligation="PT-02"),
    sale("2026-04-30", "Penjualan kasir 16-30 April", 1_020),
]

MONTHS = ["2026-02", "2026-03", "2026-04"]
MONTH_LABELS = {"2026-02": "Februari 2026", "2026-03": "Maret 2026", "2026-04": "April 2026"}

# Cost groups used by the profit and loss sheet. "HPP" here is the accounting cost of
# goods sold; Mankeu's own "HPP (Biaya per botol)" is total cost per unit, a different
# and broader figure. Both appear on sheet 5, each under its own name.
COGS_CATEGORIES = ["Bahan Baku Makanan", "Bahan Minuman dan Bumbu", "Kemasan dan Label", "Gaji Koki dan Pegawai"]
OPEX_CATEGORIES = [
    "Sewa Tempat",
    "Gas, Listrik dan Air",
    "Perizinan dan Administrasi",
    "Pemasaran dan Promosi",
    "Transport dan Pengiriman",
]
INTEREST_CATEGORY = "Biaya Bunga Pinjaman"


# --------------------------------------------------------------------------------------
# Derived figures
# --------------------------------------------------------------------------------------


@dataclass
class Realization:
    revenue: int
    operating_expense: int  # includes interest, exactly as ReportCalculator does
    expense_excluding_interest: int
    interest: int
    profit: int
    operating_cash: int
    investing_cash: int
    financing_cash: int
    cash_balance: int
    receivable_outstanding: int
    payable_outstanding: int
    quantity: int
    monthly: list[dict] = field(default_factory=list)


def active(kinds: set[str]) -> list[Txn]:
    return [t for t in TRANSACTIONS if t.event_type in kinds]


def total(kinds: set[str]) -> int:
    return sum(t.amount for t in active(kinds))


def category_total(category: str, kinds: set[str] = EXPENSE_TYPES) -> int:
    return sum(t.amount for t in TRANSACTIONS if t.category == category and t.event_type in kinds)


def compute_realization() -> Realization:
    revenue = total(REVENUE_TYPES)
    expense_excluding_interest = total(EXPENSE_TYPES)
    interest = total({"LOAN_INTEREST_PAID"})
    operating_expense = expense_excluding_interest + interest
    operating_cash = total(OPERATING_IN_TYPES) - total(OPERATING_OUT_TYPES)
    investing_cash = -total({"ASSET_PURCHASE_CASH"})
    financing_cash = total(FINANCING_PLUS) - total(FINANCING_MINUS)
    cash_balance = total(CASH_IN_TYPES) - total(CASH_OUT_TYPES)

    receivable = sum(t.amount for t in TRANSACTIONS if t.event_type == "CREDIT_SALE")
    collected = sum(t.amount for t in TRANSACTIONS if t.event_type == "RECEIVABLE_COLLECTION")
    payable = sum(t.amount for t in TRANSACTIONS if t.event_type == "CREDIT_PURCHASE_EXPENSE")
    settled = sum(t.amount for t in TRANSACTIONS if t.event_type == "PAYABLE_SETTLEMENT")

    quantity = sum(int(t.volume) for t in TRANSACTIONS if t.event_type in REVENUE_TYPES and t.volume is not None)

    monthly = []
    cumulative = OPENING_CASH
    for month in MONTHS:
        rows = [t for t in TRANSACTIONS if t.date.startswith(month)]

        def flow(rows_: list[Txn]) -> tuple[int, int, int]:
            op = sum(t.amount for t in rows_ if t.event_type in OPERATING_IN_TYPES) - sum(
                t.amount for t in rows_ if t.event_type in OPERATING_OUT_TYPES
            )
            inv = -sum(t.amount for t in rows_ if t.event_type == "ASSET_PURCHASE_CASH")
            fin = sum(t.amount for t in rows_ if t.event_type in FINANCING_PLUS) - sum(
                t.amount for t in rows_ if t.event_type in FINANCING_MINUS
            )
            return op, inv, fin

        op, inv, fin = flow(rows)
        net = op + inv + fin
        cumulative += net
        monthly.append(
            {
                "month": month,
                "label": MONTH_LABELS[month],
                "operating": op,
                "investing": inv,
                "financing": fin,
                "net": net,
                "cumulative": cumulative,
            }
        )

    return Realization(
        revenue=revenue,
        operating_expense=operating_expense,
        expense_excluding_interest=expense_excluding_interest,
        interest=interest,
        profit=revenue - operating_expense,
        operating_cash=operating_cash,
        investing_cash=investing_cash,
        financing_cash=financing_cash,
        cash_balance=cash_balance,
        receivable_outstanding=receivable - collected,
        payable_outstanding=payable - settled,
        quantity=quantity,
        monthly=monthly,
    )


def business_analysis(
    *,
    operating_expense: int,
    operating_cash: int,
    investing_cash: int,
    opening_cash: int,
    owner_capital: int,
    loan: int,
    interest_bps: int,
    other_cost: int,
    quantity: int,
    price: int,
    deepest_deficit: int,
) -> dict:
    """Mirror of BusinessAnalysisCalculator.calculate()."""
    interest = money_div(loan * interest_bps, 10_000)
    cash_before_financing = opening_cash + operating_cash + investing_cash
    financing_gap = max(max(-cash_before_financing, 0), deepest_deficit)
    ending_cash = cash_before_financing + owner_capital + loan - interest - other_cost
    total_cost = operating_expense + interest + other_cost
    revenue = quantity * price
    unit_cost = money_div(total_cost, quantity)
    bep_quantity = ratio(total_cost, price, 3)
    bcr = ratio(revenue, total_cost, 3) if total_cost > 0 else None
    gross_profit = revenue - operating_expense
    net_profit = revenue - total_cost
    return {
        "estimatedInterestRupiah": interest,
        "cashBeforeAssumedFinancingRupiah": cash_before_financing,
        "financingGapRupiah": financing_gap,
        "endingCashAfterAssumptionsRupiah": ending_cash,
        "estimatedRevenueRupiah": revenue,
        "totalCostRupiah": total_cost,
        "costPerOutputRupiah": unit_cost,
        "breakEvenQuantity": str(bep_quantity),
        "breakEvenRevenueRupiah": total_cost,
        "benefitCostRatio": str(bcr),
        "grossProfitRupiah": gross_profit,
        "netProfitRupiah": net_profit,
        "grossProfitMarginPercent": str(percent(gross_profit, revenue)),
        "netProfitMarginPercent": str(percent(net_profit, revenue)),
        "feasible": bcr >= Decimal(1),
    }


def compute_plan() -> dict:
    def lines_of(*sections: str, exclude: bool = False):
        for _, section, lines in BUDGET_SECTIONS:
            hit = section in sections
            if hit != exclude:
                yield from lines

    expense = sum(line.total for line in lines_of(ASSET_SECTION, REVENUE_SECTION, exclude=True))
    asset = sum(line.total for line in lines_of(ASSET_SECTION))
    revenue = sum(line.total for line in lines_of(REVENUE_SECTION))

    # The plan's own monthly cash schedule, mirroring BudgetPlanCalculator.schedule: revenue lines
    # are operating inflows, expense lines operating outflows, asset lines investing outflows, and
    # the deepest point of the cumulative column is the working capital the owner has to raise.
    monthly_plan = []
    cumulative = OPENING_CASH
    for month in PLAN_MONTHS:
        rows = [line for _, _, lines in BUDGET_SECTIONS for line in lines if line.month == month]
        revenue_month = sum(r.total for r in rows if r.section == REVENUE_SECTION)
        expense_month = sum(r.total for r in rows if r.section not in (ASSET_SECTION, REVENUE_SECTION))
        investing = -sum(r.total for r in rows if r.section == ASSET_SECTION)
        operating = revenue_month - expense_month
        net = operating + investing
        cumulative += net
        monthly_plan.append(
            {
                "month": month,
                "label": MONTH_LABELS[month],
                "operating": operating,
                "investing": investing,
                "financing": 0,
                "net": net,
                "cumulative": cumulative,
            }
        )

    return {
        "expense": expense,
        "asset": asset,
        "revenue": revenue,
        "costTotal": expense + asset,
        "monthly": monthly_plan,
    }


# --------------------------------------------------------------------------------------
# Minimal OOXML writer (formula + cached value capable)
# --------------------------------------------------------------------------------------

XML_HEADER = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'

STYLE_DEFAULT = 0
STYLE_HEADER = 1
STYLE_MONEY = 2
STYLE_TITLE = 3
STYLE_SECTION = 4
STYLE_MONEY_BOLD = 5
STYLE_NUMBER3 = 6


def escape(value: str) -> str:
    return (
        value.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace('"', "&quot;")
        .replace("'", "&apos;")
    )


def column_name(one_based: int) -> str:
    name = ""
    value = one_based
    while value > 0:
        value -= 1
        name = chr(ord("A") + value % 26) + name
        value //= 26
    return name


@dataclass
class Cell:
    value: object = None
    style: int = STYLE_DEFAULT
    formula: str | None = None


def C(value=None, style: int = STYLE_DEFAULT, formula: str | None = None) -> Cell:
    return Cell(value, style, formula)


def money(value: int, bold: bool = False) -> Cell:
    return Cell(value, STYLE_MONEY_BOLD if bold else STYLE_MONEY)


def fmoney(value: int, formula: str, bold: bool = False) -> Cell:
    return Cell(value, STYLE_MONEY_BOLD if bold else STYLE_MONEY, formula)


@dataclass
class Sheet:
    name: str
    rows: list[list[Cell]]
    widths: list[int]


def render_cell(reference: str, cell: Cell) -> str:
    style = f' s="{cell.style}"' if cell.style else ""
    if cell.formula is not None:
        return f'<c r="{reference}"{style}><f>{escape(cell.formula)}</f><v>{cell.value}</v></c>'
    value = cell.value
    if value is None or value == "":
        return f'<c r="{reference}"{style}/>'
    if isinstance(value, bool):
        return f'<c r="{reference}"{style} t="b"><v>{1 if value else 0}</v></c>'
    if isinstance(value, (int, float, Decimal)):
        return f'<c r="{reference}"{style} t="n"><v>{value}</v></c>'
    return (
        f'<c r="{reference}"{style} t="inlineStr"><is>'
        f'<t xml:space="preserve">{escape(str(value))}</t></is></c>'
    )


def render_sheet(sheet: Sheet) -> str:
    body = []
    for row_index, row in enumerate(sheet.rows, start=1):
        cells = "".join(
            render_cell(f"{column_name(col_index)}{row_index}", cell)
            for col_index, cell in enumerate(row, start=1)
            if not (cell.value in (None, "") and cell.formula is None and cell.style == STYLE_DEFAULT)
        )
        body.append(f'<row r="{row_index}">{cells}</row>')
    cols = "".join(
        f'<col min="{index}" max="{index}" width="{width}" customWidth="1"/>'
        for index, width in enumerate(sheet.widths, start=1)
    )
    max_col = column_name(max(len(sheet.widths), 1))
    return (
        XML_HEADER
        + '<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">'
        + f'<dimension ref="A1:{max_col}{max(len(sheet.rows), 1)}"/>'
        + '<sheetViews><sheetView workbookViewId="0"/></sheetViews>'
        + f"<cols>{cols}</cols>"
        + f'<sheetData>{"".join(body)}</sheetData>'
        + "</worksheet>"
    )


STYLES_XML = XML_HEADER + """<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
<numFmts count="2"><numFmt numFmtId="164" formatCode="&quot;Rp&quot;\\ #,##0"/><numFmt numFmtId="165" formatCode="#,##0.000"/></numFmts>
<fonts count="4">
<font><sz val="11"/><name val="Calibri"/></font>
<font><b/><color rgb="FFFFFFFF"/><sz val="11"/><name val="Calibri"/></font>
<font><b/><sz val="14"/><color rgb="FF0061A4"/><name val="Calibri"/></font>
<font><b/><sz val="11"/><name val="Calibri"/></font>
</fonts>
<fills count="3">
<fill><patternFill patternType="none"/></fill>
<fill><patternFill patternType="gray125"/></fill>
<fill><patternFill patternType="solid"><fgColor rgb="FF0061A4"/><bgColor indexed="64"/></patternFill></fill>
</fills>
<borders count="1"><border/></borders>
<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
<cellXfs count="7">
<xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
<xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1"/>
<xf numFmtId="164" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>
<xf numFmtId="0" fontId="2" fillId="0" borderId="0" xfId="0" applyFont="1"/>
<xf numFmtId="0" fontId="3" fillId="0" borderId="0" xfId="0" applyFont="1"/>
<xf numFmtId="164" fontId="3" fillId="0" borderId="0" xfId="0" applyNumberFormat="1" applyFont="1"/>
<xf numFmtId="165" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>
</cellXfs>
<cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>
</styleSheet>"""


def write_workbook(path: Path, sheets: list[Sheet]) -> None:
    count = len(sheets)
    content_types = (
        XML_HEADER
        + '<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">'
        + '<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>'
        + '<Default Extension="xml" ContentType="application/xml"/>'
        + '<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>'
        + '<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>'
        + "".join(
            f'<Override PartName="/xl/worksheets/sheet{i}.xml" '
            f'ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>'
            for i in range(1, count + 1)
        )
        + "</Types>"
    )
    root_rels = (
        XML_HEADER
        + '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">'
        + '<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>'
        + "</Relationships>"
    )
    workbook_xml = (
        XML_HEADER
        + '<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" '
        'xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">'
        + "<bookViews><workbookView/></bookViews><sheets>"
        + "".join(
            f'<sheet name="{escape(s.name)}" sheetId="{i}" r:id="rId{i}"/>'
            for i, s in enumerate(sheets, start=1)
        )
        + "</sheets><calcPr fullCalcOnLoad=\"1\"/></workbook>"
    )
    workbook_rels = (
        XML_HEADER
        + '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">'
        + "".join(
            f'<Relationship Id="rId{i}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" '
            f'Target="worksheets/sheet{i}.xml"/>'
            for i in range(1, count + 1)
        )
        + f'<Relationship Id="rId{count + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>'
        + "</Relationships>"
    )
    # A fixed timestamp on every entry, so regenerating the workbook from unchanged inputs produces
    # a byte-identical file. Without it `zipfile` stamps "now" and the artefact's checksum changes
    # on every run, which makes it useless as committed evidence.
    stamp = (2026, 2, 1, 0, 0, 0)

    def put(zf: zipfile.ZipFile, name: str, contents: str) -> None:
        info = zipfile.ZipInfo(name, date_time=stamp)
        info.compress_type = zipfile.ZIP_DEFLATED
        info.external_attr = 0o600 << 16
        zf.writestr(info, contents)

    with zipfile.ZipFile(path, "w", zipfile.ZIP_DEFLATED) as zf:
        put(zf, "[Content_Types].xml", content_types)
        put(zf, "_rels/.rels", root_rels)
        put(zf, "xl/workbook.xml", workbook_xml)
        put(zf, "xl/_rels/workbook.xml.rels", workbook_rels)
        put(zf, "xl/styles.xml", STYLES_XML)
        for index, sheet in enumerate(sheets, start=1):
            put(zf, f"xl/worksheets/sheet{index}.xml", render_sheet(sheet))


# --------------------------------------------------------------------------------------
# Sheet builders
# --------------------------------------------------------------------------------------

SHEET_PARAMS = "Petunjuk dan Parameter"
SHEET_RAB = "RAB (Proyeksi)"
SHEET_LEDGER = "Transaksi Harian (Realisasi)"
SHEET_PL = "Laporan Laba Rugi"
SHEET_CASH = "Arus Kas dan Analisis Kelayakan"

LEDGER_HEADERS = [
    "Tanggal",
    "Jenis",
    "Uraian",
    "Kategori",
    "Pihak",
    "Jatuh Tempo",
    "No. Kewajiban",
    "Volume",
    "Satuan",
    "Harga Satuan (Rp)",
    "Nominal (Rp)",
    "Pengeluaran (Rp)",
    "Pemasukan (Rp)",
    "Saldo Kas (Rp)",
]
LEDGER_FIRST_DATA_ROW = 2
LEDGER_LAST_DATA_ROW = LEDGER_FIRST_DATA_ROW + len(TRANSACTIONS) - 1
LEDGER_KATEGORI = f"'{SHEET_LEDGER}'!$D${LEDGER_FIRST_DATA_ROW}:$D${LEDGER_LAST_DATA_ROW}"
LEDGER_JENIS = f"'{SHEET_LEDGER}'!$B${LEDGER_FIRST_DATA_ROW}:$B${LEDGER_LAST_DATA_ROW}"
LEDGER_TANGGAL = f"'{SHEET_LEDGER}'!$A${LEDGER_FIRST_DATA_ROW}:$A${LEDGER_LAST_DATA_ROW}"
LEDGER_NOMINAL = f"'{SHEET_LEDGER}'!$K${LEDGER_FIRST_DATA_ROW}:$K${LEDGER_LAST_DATA_ROW}"

# Jenis labels grouped the way the report sheets need them.
SALE_KINDS = ["Uang Masuk", "Penjualan Tempo"]
EXPENSE_KINDS = ["Uang Keluar", "Belanja Tempo"]


def sumifs_category(category: str, kinds: list[str]) -> str:
    parts = [
        f'SUMIFS({LEDGER_NOMINAL},{LEDGER_KATEGORI},"{category}",{LEDGER_JENIS},"{kind}")'
        for kind in kinds
    ]
    return "+".join(parts)


def sumifs_kind(kind: str) -> str:
    return f'SUMIFS({LEDGER_NOMINAL},{LEDGER_JENIS},"{kind}")'


def sumifs_kind_month(kind: str, month: str) -> str:
    return (
        f'SUMIFS({LEDGER_NOMINAL},{LEDGER_JENIS},"{kind}",'
        f'{LEDGER_TANGGAL},">={month}-01",{LEDGER_TANGGAL},"<={month}-31")'
    )


def build_params_sheet(plan: dict, actual: Realization) -> Sheet:
    rows: list[list[Cell]] = [
        [C("Simulasi Kelayakan Usaha UMKM - Berkas Verifikasi Mankeu", STYLE_TITLE)],
        [C("Periode simulasi: Februari sampai April 2026")],
        [],
        [C("Identitas Usaha", STYLE_SECTION)],
        [C("Nama usaha"), C(BUSINESS_NAME)],
        [C("Nama proyek atau siklus"), C(PROJECT_NAME)],
        [C("Sektor usaha"), C(SECTOR)],
        [],
        [C("Parameter", STYLE_SECTION), C("Nilai", STYLE_SECTION), C("Satuan", STYLE_SECTION), C("Keterangan", STYLE_SECTION)],
        [C("Kapasitas produksi rencana"), C(PLAN_OUTPUT_QUANTITY), C(OUTPUT_UNIT),
         C("Target keluaran satu siklus produksi tiga bulan")],
        [C("Harga jual satuan"), money(OUTPUT_PRICE), C("Rp per " + OUTPUT_UNIT),
         C("Harga seragam untuk penjualan tunai maupun tempo pada simulasi ini")],
        [C("Modal awal sendiri"), money(OWNER_CAPITAL_PLAN), C("Rp"),
         C("Setoran pemilik pada awal siklus")],
        [C("Rencana pinjaman modal kerja"), money(LOAN_PLAN), C("Rp"),
         C("Pencairan KUR modal kerja")],
        [C("Suku bunga pinjaman"), C(INTEREST_BPS), C("bps"),
         C("1.200 bps sama dengan 12,00 persen untuk satu siklus")],
        [C("Biaya pembiayaan lainnya"), money(OTHER_FINANCING_COST), C("Rp"),
         C("Biaya administrasi dan provisi pinjaman")],
        [C("Saldo kas awal"), money(OPENING_CASH), C("Rp"),
         C("Usaha dimulai tanpa saldo kas sebelumnya")],
        [],
        [C("Cara memakai berkas ini", STYLE_SECTION)],
        [C("1."), C("Buat usaha baru di Mankeu dengan sektor Kuliner / F&B, lalu buat satu proyek atau siklus.")],
        [C("2."), C("Buka Berkas dan Data, pilih Ambil dari Excel, lalu pilih berkas ini.")],
        [C("3."), C("Lembar RAB (Proyeksi) masuk ke buku Proyeksi; lembar Transaksi Harian (Realisasi) masuk ke buku Realisasi.")],
        [C("4."), C("Isi Asumsi pada buku Proyeksi dengan nilai parameter di atas; isi Asumsi buku Realisasi hanya dengan volume dan harga jual.")],
        [C("5."), C("Bandingkan angka aplikasi dengan lembar Laporan Laba Rugi dan Arus Kas dan Analisis Kelayakan.")],
        [],
        [C("Catatan pembacaan", STYLE_SECTION)],
        [C("a."), C("Kolom Nominal (Rp) selalu berisi nilai transaksi. Kolom Pengeluaran dan Pemasukan hanya diisi untuk transaksi tunai.")],
        [C("b."), C("Baris Penjualan Tempo dan Belanja Tempo tidak menggerakkan kas, sehingga kolom Saldo Kas tidak berubah pada baris tersebut.")],
        [C("c."), C("Seluruh nilai uang adalah rupiah bulat. Tidak ada pecahan sen di berkas ini maupun di dalam aplikasi.")],
        [],
        [C("Ringkasan hasil yang harus muncul di aplikasi", STYLE_SECTION)],
        [C("Total RAB biaya (Proyeksi)"), money(plan["costTotal"])],
        [C("Target omzet (Proyeksi)"), money(plan["revenue"])],
        [C("Omzet penjualan (Realisasi)"), money(actual.revenue)],
        [C("Laba bersih (Realisasi)"), money(actual.profit)],
        [C("Kas akhir siklus (Realisasi)"), money(actual.cash_balance)],
    ]
    return Sheet(SHEET_PARAMS, rows, [34, 20, 16, 78])


def build_rab_sheet() -> Sheet:
    headers = ["No", "Uraian", "Volume", "Satuan", "Harga Satuan (Rp)", "Bulan Rencana", "Total Anggaran (Rp)"]
    rows: list[list[Cell]] = [[C(h, STYLE_HEADER) for h in headers]]
    number = 0
    for title, _section, lines in BUDGET_SECTIONS:
        rows.append([C(""), C(title, STYLE_SECTION)])
        for line in lines:
            number += 1
            row_index = len(rows) + 1
            rows.append(
                [
                    # Text, not a number: an integer in column A is what
                    # FinanceWorkbookImporter.findDeterministicEnrichments uses to spot an RAB
                    # line it may need to recover from the report sheets. Nothing in this
                    # workbook needs recovering — every plan line is a plan line and every fact
                    # is stated once — so the marker is deliberately withheld and the import
                    # stays free of enrichment guesses.
                    C(str(number)),
                    C(line.name),
                    C(line.volume),
                    C(line.unit),
                    money(line.unit_price),
                    C(line.month),
                    fmoney(line.total, f"C{row_index}*E{row_index}"),
                ]
            )
    plan = compute_plan()
    rows.append([])
    rows.append([C(""), C("Total anggaran biaya", STYLE_SECTION), C(""), C(""), C(""), C(""),
                 money(plan["costTotal"], bold=True)])
    rows.append([C(""), C("Total target omzet", STYLE_SECTION), C(""), C(""), C(""), C(""),
                 money(plan["revenue"], bold=True)])
    return Sheet(SHEET_RAB, rows, [6, 42, 12, 14, 20, 16, 22])


def build_ledger_sheet() -> Sheet:
    rows: list[list[Cell]] = [[C(h, STYLE_HEADER) for h in LEDGER_HEADERS]]
    balance = OPENING_CASH
    for index, txn in enumerate(TRANSACTIONS):
        row_index = LEDGER_FIRST_DATA_ROW + index
        cash_in = txn.amount if txn.event_type in CASH_IN_TYPES else None
        cash_out = txn.amount if txn.event_type in CASH_OUT_TYPES else None
        balance += (cash_in or 0) - (cash_out or 0)
        previous = f"N{row_index - 1}" if index > 0 else str(OPENING_CASH)
        rows.append(
            [
                C(txn.date),
                C(txn.kind),
                C(txn.description),
                C(txn.category),
                C(txn.counterparty),
                C(txn.due_date),
                C(txn.obligation),
                C(txn.volume if txn.volume is not None else ""),
                C(txn.unit),
                money(txn.unit_price) if txn.unit_price else C(""),
                money(txn.amount),
                money(cash_out) if cash_out else C(""),
                money(cash_in) if cash_in else C(""),
                fmoney(balance, f"{previous}+M{row_index}-L{row_index}"),
            ]
        )
    return Sheet(SHEET_LEDGER, rows, [12, 22, 44, 30, 22, 14, 14, 10, 14, 18, 18, 18, 18, 18])


def build_profit_loss_sheet(actual: Realization) -> Sheet:
    rows: list[list[Cell]] = [
        [C("Laporan Laba Rugi - Realisasi Februari sampai April 2026", STYLE_TITLE)],
        [C("Setiap angka pada lembar ini adalah rumus SUMIFS atas lembar Transaksi Harian (Realisasi).")],
        [],
        [C("Uraian", STYLE_HEADER), C("Jumlah (Rp)", STYLE_HEADER), C("Catatan", STYLE_HEADER)],
    ]

    cash_sales = sum(t.amount for t in TRANSACTIONS if t.event_type == "CASH_INCOME")
    credit_sales = sum(t.amount for t in TRANSACTIONS if t.event_type == "CREDIT_SALE")

    rows.append([C("Penjualan tunai"), fmoney(cash_sales, sumifs_kind("Uang Masuk")),
                 C("Jenis Uang Masuk pada buku transaksi")])
    rows.append([C("Penjualan tempo (piutang)"), fmoney(credit_sales, sumifs_kind("Penjualan Tempo")),
                 C("Diakui sebagai omzet saat penyerahan barang, bukan saat uang diterima")])
    revenue_row = len(rows) + 1
    rows.append([C("Omzet penjualan", STYLE_SECTION),
                 fmoney(actual.revenue, f"B{revenue_row - 2}+B{revenue_row - 1}", bold=True), C("")])
    rows.append([])

    rows.append([C("Beban pokok penjualan (HPP akuntansi)", STYLE_SECTION)])
    cogs_rows: list[int] = []
    for category in COGS_CATEGORIES:
        value = category_total(category)
        cogs_rows.append(len(rows) + 1)
        rows.append([C(category), fmoney(value, sumifs_category(category, EXPENSE_KINDS)), C("")])
    cogs_total = sum(category_total(c) for c in COGS_CATEGORIES)
    cogs_total_row = len(rows) + 1
    rows.append([C("Total beban pokok penjualan", STYLE_SECTION),
                 fmoney(cogs_total, "+".join(f"B{r}" for r in cogs_rows), bold=True), C("")])
    gross_row = len(rows) + 1
    rows.append([C("Laba kotor", STYLE_SECTION),
                 fmoney(actual.revenue - cogs_total, f"B{revenue_row}-B{cogs_total_row}", bold=True),
                 C("Omzet penjualan dikurangi beban pokok penjualan")])
    rows.append([])

    rows.append([C("Beban operasional", STYLE_SECTION)])
    opex_rows: list[int] = []
    for category in OPEX_CATEGORIES:
        value = category_total(category)
        opex_rows.append(len(rows) + 1)
        rows.append([C(category), fmoney(value, sumifs_category(category, EXPENSE_KINDS)), C("")])
    opex_total = sum(category_total(c) for c in OPEX_CATEGORIES)
    opex_total_row = len(rows) + 1
    rows.append([C("Total beban operasional", STYLE_SECTION),
                 fmoney(opex_total, "+".join(f"B{r}" for r in opex_rows), bold=True), C("")])
    operating_profit_row = len(rows) + 1
    rows.append([C("Laba usaha", STYLE_SECTION),
                 fmoney(actual.revenue - cogs_total - opex_total, f"B{gross_row}-B{opex_total_row}", bold=True),
                 C("Laba sebelum beban bunga")])
    rows.append([])

    interest_row = len(rows) + 1
    rows.append([C("Beban bunga pinjaman"), fmoney(actual.interest, sumifs_kind("Bayar Bunga")),
                 C("Bunga adalah beban; angsuran pokok bukan beban")])
    net_row = len(rows) + 1
    rows.append([C("Laba bersih", STYLE_SECTION),
                 fmoney(actual.profit, f"B{operating_profit_row}-B{interest_row}", bold=True),
                 C("Angka ini harus sama dengan Laba Bersih pada Laporan Mankeu")])
    rows.append([])

    rows.append([C("Pemeriksaan silang dengan model Mankeu", STYLE_SECTION)])
    rows.append([C("Total beban operasional versi Mankeu"),
                 fmoney(actual.operating_expense, f"B{cogs_total_row}+B{opex_total_row}+B{interest_row}"),
                 C("Mankeu menggabungkan HPP, beban operasional, dan bunga menjadi satu Beban Operasional")])
    rows.append([C("Laba bersih versi Mankeu"),
                 fmoney(actual.profit, f"B{revenue_row}-(B{cogs_total_row}+B{opex_total_row}+B{interest_row})"),
                 C("Sama dengan Laba bersih di atas")])
    rows.append([C("Margin laba bersih (persen)"),
                 Cell(str(percent(actual.profit, actual.revenue)), STYLE_NUMBER3,
                      f"ROUND(B{net_row}/B{revenue_row}*100,2)"),
                 C("Dibulatkan dua angka di belakang koma, sama dengan aplikasi")])
    return Sheet(SHEET_PL, rows, [42, 22, 76])


def build_cash_sheet(plan: dict, actual: Realization, plan_analysis: dict, actual_analysis: dict) -> Sheet:
    rows: list[list[Cell]] = [
        [C("Arus Kas dan Analisis Kelayakan Usaha", STYLE_TITLE)],
        [C("Bagian A dan B memakai buku Realisasi. Bagian C memakai buku Proyeksi (RAB dan parameter).")],
        [],
        [C("A. Arus kas bulanan (Realisasi)", STYLE_SECTION)],
        [C("Bulan", STYLE_HEADER), C("Arus Kas Operasi (Rp)", STYLE_HEADER),
         C("Arus Kas Investasi (Rp)", STYLE_HEADER), C("Arus Kas Pendanaan (Rp)", STYLE_HEADER),
         C("Arus Kas Bersih (Rp)", STYLE_HEADER), C("Kas Kumulatif (Rp)", STYLE_HEADER)],
    ]

    def month_operating_formula(month: str) -> str:
        plus = "+".join(sumifs_kind_month(k, month) for k in ["Uang Masuk", "Terima Piutang"])
        minus = "+".join(sumifs_kind_month(k, month) for k in ["Uang Keluar", "Bayar Utang", "Bayar Bunga"])
        return f"{plus}-({minus})"

    def month_investing_formula(month: str) -> str:
        return f'-({sumifs_kind_month("Beli Alat Tunai", month)})'

    def month_financing_formula(month: str) -> str:
        plus = "+".join(sumifs_kind_month(k, month) for k in ["Modal Masuk", "Pinjaman Masuk"])
        minus = "+".join(sumifs_kind_month(k, month) for k in ["Modal Keluar", "Bayar Pokok Pinjaman"])
        return f"{plus}-({minus})"

    first_month_row = len(rows) + 1
    for index, entry in enumerate(actual.monthly):
        row_index = len(rows) + 1
        previous = f"F{row_index - 1}" if index > 0 else str(OPENING_CASH)
        rows.append(
            [
                C(entry["label"]),
                fmoney(entry["operating"], month_operating_formula(entry["month"])),
                fmoney(entry["investing"], month_investing_formula(entry["month"])),
                fmoney(entry["financing"], month_financing_formula(entry["month"])),
                fmoney(entry["net"], f"B{row_index}+C{row_index}+D{row_index}"),
                fmoney(entry["cumulative"], f"{previous}+E{row_index}"),
            ]
        )
    last_month_row = len(rows)
    total_row = len(rows) + 1
    rows.append(
        [
            C("Total siklus", STYLE_SECTION),
            fmoney(actual.operating_cash, f"SUM(B{first_month_row}:B{last_month_row})", bold=True),
            fmoney(actual.investing_cash, f"SUM(C{first_month_row}:C{last_month_row})", bold=True),
            fmoney(actual.financing_cash, f"SUM(D{first_month_row}:D{last_month_row})", bold=True),
            fmoney(actual.cash_balance, f"SUM(E{first_month_row}:E{last_month_row})", bold=True),
            fmoney(actual.cash_balance, f"F{last_month_row}", bold=True),
        ]
    )
    rows.append([])

    rows.append([C("B. Kebutuhan modal kerja (Realisasi)", STYLE_SECTION)])
    deficit = min([0] + [entry["cumulative"] for entry in actual.monthly])
    deepest = -deficit if deficit < 0 else 0
    before_row = len(rows) + 1
    rows.append([C("Kas sebelum pendanaan (operasi ditambah investasi)"),
                 fmoney(actual.operating_cash + actual.investing_cash, f"B{total_row}+C{total_row}"),
                 C("Saldo kas awal ditambah arus kas operasi dan investasi")])
    deficit_row = len(rows) + 1
    rows.append([C("Defisit kumulatif terdalam"), money(deepest),
                 C("Nilai negatif terdalam pada kolom Kas Kumulatif; nol bila tidak pernah negatif")])
    rows.append([C("Kebutuhan modal kerja (financing gap)", STYLE_SECTION),
                 fmoney(actual_analysis["financingGapRupiah"],
                        f"MAX(-B{before_row},B{deficit_row},0)", bold=True),
                 C("Sama dengan Kebutuhan Modal Kerja pada Laporan Mankeu")])
    rows.append([C("Piutang belum tertagih"), money(actual.receivable_outstanding),
                 C("Sisa tagihan Warung Bu Ani")])
    rows.append([C("Utang belum dibayar"), money(actual.payable_outstanding),
                 C("Seluruh utang dagang sudah dilunasi")])
    rows.append([])

    rows.append([C("C. Analisis kelayakan - buku Proyeksi", STYLE_SECTION)])
    rows.append([C("Komponen", STYLE_HEADER), C("Nilai", STYLE_HEADER), C("Cara hitung", STYLE_HEADER)])
    plan_expense_row = len(rows) + 1
    rows.append([C("Total anggaran biaya operasional"), money(plan["expense"]),
                 C("Seluruh baris RAB selain peralatan dan target omzet")])
    plan_interest_row = len(rows) + 1
    rows.append([C("Beban bunga pinjaman"), money(plan_analysis["estimatedInterestRupiah"]),
                 C("Pinjaman dikali suku bunga dibagi 10.000 basis poin")])
    plan_other_row = len(rows) + 1
    rows.append([C("Biaya pembiayaan lainnya"), money(OTHER_FINANCING_COST), C("Parameter")])
    plan_cost_row = len(rows) + 1
    rows.append([C("Total biaya produksi", STYLE_SECTION),
                 fmoney(plan_analysis["totalCostRupiah"],
                        f"B{plan_expense_row}+B{plan_interest_row}+B{plan_other_row}", bold=True),
                 C("Dasar HPP, BEP, dan BCR di aplikasi")])
    plan_revenue_row = len(rows) + 1
    rows.append([C("Estimasi omzet"), money(plan_analysis["estimatedRevenueRupiah"]),
                 C(f"{PLAN_OUTPUT_QUANTITY} {OUTPUT_UNIT} dikali harga jual satuan")])
    rows.append([C("HPP (biaya total per botol)"),
                 fmoney(plan_analysis["costPerOutputRupiah"],
                        f"ROUND(B{plan_cost_row}/{PLAN_OUTPUT_QUANTITY},0)"),
                 C("Istilah HPP di aplikasi berarti biaya total per satuan, bukan hanya beban pokok penjualan")])
    rows.append([C("BEP produksi (titik impas)"),
                 Cell(plan_analysis["breakEvenQuantity"], STYLE_NUMBER3,
                      f"ROUND(B{plan_cost_row}/{OUTPUT_PRICE},3)"),
                 C(f"Total biaya dibagi harga jual, dalam {OUTPUT_UNIT}")])
    rows.append([C("BEP penjualan (omzet impas)"),
                 fmoney(plan_analysis["breakEvenRevenueRupiah"], f"B{plan_cost_row}"),
                 C("Sama dengan total biaya produksi")])
    plan_net_row = len(rows) + 1
    rows.append([C("Laba bersih rencana"),
                 fmoney(plan_analysis["netProfitRupiah"], f"B{plan_revenue_row}-B{plan_cost_row}"),
                 C("")])
    rows.append([C("Margin laba bersih (persen)"),
                 Cell(plan_analysis["netProfitMarginPercent"], STYLE_NUMBER3,
                      f"ROUND(B{plan_net_row}/B{plan_revenue_row}*100,2)"),
                 C("")])
    rows.append([C("B/C Ratio", STYLE_SECTION),
                 Cell(plan_analysis["benefitCostRatio"], STYLE_NUMBER3,
                      f"ROUND(B{plan_revenue_row}/B{plan_cost_row},3)"),
                 C("Layak bila lebih besar atau sama dengan 1,000")])
    rows.append([C("Status kelayakan", STYLE_SECTION),
                 C("LAYAK" if plan_analysis["feasible"] else "TIDAK LAYAK", STYLE_SECTION),
                 C("")])
    rows.append([])
    rows.append([C("C2. Jadwal kas rencana dan kebutuhan modal kerja", STYLE_SECTION)])
    rows.append([C("Bulan", STYLE_HEADER), C("Arus Kas Operasi (Rp)", STYLE_HEADER),
                 C("Arus Kas Investasi (Rp)", STYLE_HEADER), C("Arus Kas Bersih (Rp)", STYLE_HEADER),
                 C("Kas Kumulatif (Rp)", STYLE_HEADER)])
    plan_first_row = len(rows) + 1
    for index, entry in enumerate(plan["monthly"]):
        row_index = len(rows) + 1
        previous = f"E{row_index - 1}" if index > 0 else str(OPENING_CASH)
        rows.append(
            [
                C(entry["label"]),
                money(entry["operating"]),
                money(entry["investing"]),
                fmoney(entry["net"], f"B{row_index}+C{row_index}"),
                fmoney(entry["cumulative"], f"{previous}+D{row_index}"),
            ]
        )
    plan_last_row = len(rows)
    rows.append([C("Titik terendah kas kumulatif", STYLE_SECTION),
                 fmoney(min(entry["cumulative"] for entry in plan["monthly"]),
                        f"MIN(E{plan_first_row}:E{plan_last_row})", bold=True),
                 C(""), C(""),
                 C("Bulan pertama menanggung seluruh peralatan dan stok sebelum penjualan naik")])
    rows.append([C("Kebutuhan modal kerja rencana", STYLE_SECTION),
                 fmoney(plan_analysis["financingGapRupiah"],
                        f"MAX(-MIN(E{plan_first_row}:E{plan_last_row}),0)", bold=True),
                 C(""), C(""),
                 C("Ditutup oleh modal sendiri dan pinjaman pada parameter")])
    rows.append([])

    rows.append([C("D. Analisis kelayakan - buku Realisasi", STYLE_SECTION)])
    rows.append([C("Komponen", STYLE_HEADER), C("Nilai", STYLE_HEADER), C("Cara hitung", STYLE_HEADER)])
    actual_cost_row = len(rows) + 1
    rows.append([C("Total biaya produksi"), money(actual_analysis["totalCostRupiah"]),
                 C("Beban operasional Mankeu, sudah termasuk bunga")])
    actual_revenue_row = len(rows) + 1
    rows.append([C("Omzet penjualan"), money(actual_analysis["estimatedRevenueRupiah"]),
                 C(f"{actual.quantity} {OUTPUT_UNIT} terjual dikali harga jual satuan")])
    rows.append([C("HPP (biaya total per botol)"),
                 fmoney(actual_analysis["costPerOutputRupiah"],
                        f"ROUND(B{actual_cost_row}/{actual.quantity},0)"), C("")])
    rows.append([C("BEP produksi (titik impas)"),
                 Cell(actual_analysis["breakEvenQuantity"], STYLE_NUMBER3,
                      f"ROUND(B{actual_cost_row}/{OUTPUT_PRICE},3)"), C("")])
    rows.append([C("BEP penjualan (omzet impas)"), money(actual_analysis["breakEvenRevenueRupiah"]), C("")])
    actual_net_row = len(rows) + 1
    rows.append([C("Laba bersih"),
                 fmoney(actual_analysis["netProfitRupiah"], f"B{actual_revenue_row}-B{actual_cost_row}"), C("")])
    rows.append([C("Margin laba bersih (persen)"),
                 Cell(actual_analysis["netProfitMarginPercent"], STYLE_NUMBER3,
                      f"ROUND(B{actual_net_row}/B{actual_revenue_row}*100,2)"), C("")])
    rows.append([C("B/C Ratio", STYLE_SECTION),
                 Cell(actual_analysis["benefitCostRatio"], STYLE_NUMBER3,
                      f"ROUND(B{actual_revenue_row}/B{actual_cost_row},3)"), C("")])
    rows.append([C("Status kelayakan", STYLE_SECTION),
                 C("LAYAK" if actual_analysis["feasible"] else "TIDAK LAYAK", STYLE_SECTION), C("")])
    rows.append([])

    rows.append([C("E. Realisasi terhadap rencana", STYLE_SECTION)])
    rows.append([C("Komponen", STYLE_HEADER), C("Rencana (Rp)", STYLE_HEADER),
                 C("Realisasi (Rp)", STYLE_HEADER), C("Selisih (Rp)", STYLE_HEADER)])
    for label, planned, realized in [
        ("Omzet penjualan", plan_analysis["estimatedRevenueRupiah"], actual.revenue),
        ("Total biaya produksi", plan_analysis["totalCostRupiah"], actual_analysis["totalCostRupiah"]),
        ("Laba bersih", plan_analysis["netProfitRupiah"], actual_analysis["netProfitRupiah"]),
        ("Belanja peralatan", plan["asset"], -actual.investing_cash),
    ]:
        row_index = len(rows) + 1
        rows.append([C(label), money(planned), money(realized),
                     fmoney(realized - planned, f"C{row_index}-B{row_index}")])

    return Sheet(SHEET_CASH, rows, [42, 24, 24, 24, 24, 24])


# --------------------------------------------------------------------------------------
# Self-checks. These are the runnable guard on the arithmetic above; the workbook is only
# written when all of them hold.
# --------------------------------------------------------------------------------------


def self_check(plan: dict, actual: Realization, plan_analysis: dict, actual_analysis: dict) -> None:
    assert all(t.amount > 0 for t in TRANSACTIONS), "Setiap transaksi harus bernilai positif."
    assert all(isinstance(t.amount, int) for t in TRANSACTIONS), "Uang harus bilangan bulat rupiah."
    assert all(line.total == int(line.total) for line in BUDGET_LINES), "Total RAB harus bulat."

    for txn in TRANSACTIONS:
        if txn.volume is not None and txn.unit_price is not None:
            expected = int((txn.volume * Decimal(txn.unit_price)).quantize(Decimal("1"), ROUND_HALF_UP))
            assert expected == txn.amount, f"Volume kali harga satuan tidak cocok pada {txn.description}"

    # Cash balance must equal the last cumulative figure and the running column's last row.
    assert actual.cash_balance == actual.monthly[-1]["cumulative"], "Saldo kas tidak sama dengan kas kumulatif."
    assert actual.operating_cash + actual.investing_cash + actual.financing_cash == actual.cash_balance

    # Every settlement must point at a credit row that exists and must not overpay it.
    opened = {t.obligation: t.amount for t in TRANSACTIONS if t.event_type in NON_CASH_TYPES}
    paid: dict[str, int] = {}
    for txn in TRANSACTIONS:
        if txn.event_type in {"RECEIVABLE_COLLECTION", "PAYABLE_SETTLEMENT"}:
            assert txn.obligation in opened, f"Pelunasan {txn.obligation} tanpa transaksi tempo sumber."
            paid[txn.obligation] = paid.get(txn.obligation, 0) + txn.amount
            assert paid[txn.obligation] <= opened[txn.obligation], f"Pelunasan {txn.obligation} melebihi nilai asal."
        if txn.event_type in NON_CASH_TYPES:
            assert txn.counterparty, f"Transaksi tempo {txn.description} wajib punya nama pihak."

    # No RAB line may be recoverable as a deterministic enrichment.
    # FinanceWorkbookImporter.findDeterministicEnrichments needs three agreeing sources — the
    # RAB row itself plus a matching amount on *both* the profit-and-loss sheet and the cash-flow
    # sheet — before it invents a transaction. Every fact in this workbook is stated once, in the
    # ledger, so any such coincidence would be a false recovery and a double count.
    profit_loss_values = {category_total(c) for c in COGS_CATEGORIES + OPEX_CATEGORIES}
    cash_flow_values = (
        {entry["cumulative"] for entry in actual.monthly}
        | {entry["net"] for entry in actual.monthly}
        | {entry["operating"] for entry in actual.monthly}
        | {entry["investing"] for entry in actual.monthly}
        | {entry["financing"] for entry in actual.monthly}
    )
    for line in BUDGET_LINES:
        assert not (line.total in profit_loss_values and line.total in cash_flow_values), (
            f"Nilai RAB {line.name} tampil di lembar laba rugi dan arus kas sekaligus; "
            "impor bisa salah memulihkannya sebagai transaksi baru."
        )

    # Profit-and-loss composition must add up to Mankeu's single Beban Operasional figure.
    cogs = sum(category_total(c) for c in COGS_CATEGORIES)
    opex = sum(category_total(c) for c in OPEX_CATEGORIES)
    assert cogs + opex + actual.interest == actual.operating_expense, "Rincian beban tidak menutup total beban."
    assert actual.revenue - actual.operating_expense == actual.profit

    # Plan totals.
    assert plan["expense"] + plan["asset"] == plan["costTotal"]
    assert sum(e["operating"] for e in plan["monthly"]) == plan["revenue"] - plan["expense"], (
        "Jadwal kas rencana tidak menutup total pendapatan dikurangi biaya."
    )
    assert sum(e["investing"] for e in plan["monthly"]) == -plan["asset"]
    assert plan["monthly"][-1]["cumulative"] == plan["revenue"] - plan["expense"] - plan["asset"]
    assert plan_analysis["financingGapRupiah"] > 0, (
        "Jadwal kas rencana harus menunjukkan kebutuhan modal kerja; RAB tanpa bulan tidak bisa."
    )
    assert plan_analysis["totalCostRupiah"] == plan["expense"] + plan_analysis["estimatedInterestRupiah"] + OTHER_FINANCING_COST
    assert plan_analysis["feasible"] is True, "Studi kasus rencana harus berstatus layak."
    assert actual_analysis["feasible"] is True, "Studi kasus realisasi harus berstatus layak."
    assert actual_analysis["estimatedRevenueRupiah"] == actual.revenue, (
        "Volume terjual dikali harga jual harus sama dengan omzet, agar BCR realisasi bermakna."
    )
    assert actual_analysis["financingGapRupiah"] > 0, "Studi kasus harus menunjukkan kebutuhan modal kerja."


# --------------------------------------------------------------------------------------


def main() -> None:
    plan = compute_plan()
    actual = compute_realization()

    plan_trough = min([0] + [entry["cumulative"] for entry in plan["monthly"]])
    plan_analysis = business_analysis(
        operating_expense=plan["expense"],
        operating_cash=plan["revenue"] - plan["expense"],
        investing_cash=-plan["asset"],
        opening_cash=OPENING_CASH,
        owner_capital=OWNER_CAPITAL_PLAN,
        loan=LOAN_PLAN,
        interest_bps=INTEREST_BPS,
        other_cost=OTHER_FINANCING_COST,
        quantity=PLAN_OUTPUT_QUANTITY,
        price=OUTPUT_PRICE,
        deepest_deficit=-plan_trough if plan_trough < 0 else 0,
    )
    deficit = min([0] + [entry["cumulative"] for entry in actual.monthly])
    actual_analysis = business_analysis(
        operating_expense=actual.operating_expense,
        operating_cash=actual.operating_cash,
        investing_cash=actual.investing_cash,
        opening_cash=OPENING_CASH,
        owner_capital=0,
        loan=0,
        interest_bps=0,
        other_cost=0,
        quantity=actual.quantity,
        price=OUTPUT_PRICE,
        deepest_deficit=-deficit if deficit < 0 else 0,
    )

    self_check(plan, actual, plan_analysis, actual_analysis)

    sheets = [
        build_params_sheet(plan, actual),
        build_rab_sheet(),
        build_ledger_sheet(),
        build_profit_loss_sheet(actual),
        build_cash_sheet(plan, actual, plan_analysis, actual_analysis),
    ]
    write_workbook(XLSX_PATH, sheets)

    expected = {
        "sourceName": XLSX_PATH.name,
        "business": BUSINESS_NAME,
        "project": PROJECT_NAME,
        "sector": SECTOR,
        "recognizedSheets": [SHEET_RAB, SHEET_LEDGER, SHEET_PL, SHEET_CASH],
        "transactionCount": len(TRANSACTIONS),
        "budgetLineCount": len(BUDGET_LINES),
        "outputUnit": OUTPUT_UNIT,
        "outputPriceRupiah": OUTPUT_PRICE,
        "plan": {
            "budgetExpenseRupiah": plan["expense"],
            "budgetAssetRupiah": plan["asset"],
            "budgetRevenueRupiah": plan["revenue"],
            "budgetCostTotalRupiah": plan["costTotal"],
            "monthlyCashFlow": plan["monthly"],
            "assumptions": {
                "openingCashRupiah": OPENING_CASH,
                "ownerCapitalRupiah": OWNER_CAPITAL_PLAN,
                "loanAmountRupiah": LOAN_PLAN,
                "interestRateBasisPoints": INTEREST_BPS,
                "otherFinancingCostRupiah": OTHER_FINANCING_COST,
                "outputQuantityText": str(PLAN_OUTPUT_QUANTITY),
                "outputUnit": OUTPUT_UNIT,
                "outputPriceRupiah": OUTPUT_PRICE,
            },
            "analysis": plan_analysis,
        },
        "realization": {
            "operatingIncomeRupiah": actual.revenue,
            "operatingExpenseRupiah": actual.operating_expense,
            "profitRupiah": actual.profit,
            "operatingCashFlowRupiah": actual.operating_cash,
            "investingCashFlowRupiah": actual.investing_cash,
            "financingCashFlowRupiah": actual.financing_cash,
            "cashBalanceRupiah": actual.cash_balance,
            "receivableOutstandingRupiah": actual.receivable_outstanding,
            "payableOutstandingRupiah": actual.payable_outstanding,
            "soldQuantity": actual.quantity,
            "interestRupiah": actual.interest,
            "cogsRupiah": sum(category_total(c) for c in COGS_CATEGORIES),
            "opexRupiah": sum(category_total(c) for c in OPEX_CATEGORIES),
            "monthlyCashFlow": actual.monthly,
            "assumptions": {
                "openingCashRupiah": OPENING_CASH,
                "ownerCapitalRupiah": 0,
                "loanAmountRupiah": 0,
                "interestRateBasisPoints": 0,
                "otherFinancingCostRupiah": 0,
                "outputQuantityText": str(actual.quantity),
                "outputUnit": OUTPUT_UNIT,
                "outputPriceRupiah": OUTPUT_PRICE,
            },
            "analysis": actual_analysis,
        },
    }
    EXPECTED_PATH.parent.mkdir(parents=True, exist_ok=True)
    EXPECTED_PATH.write_text(json.dumps(expected, indent=2, ensure_ascii=False), encoding="utf-8")

    print(f"Ditulis: {XLSX_PATH}")
    print(f"Ditulis: {EXPECTED_PATH}")
    print()
    print(f"  Transaksi realisasi          : {len(TRANSACTIONS)} baris")
    print(f"  Baris RAB                    : {len(BUDGET_LINES)} baris")
    print(f"  Omzet realisasi              : Rp{actual.revenue:,}".replace(",", "."))
    print(f"  Beban operasional realisasi  : Rp{actual.operating_expense:,}".replace(",", "."))
    print(f"  Laba bersih realisasi        : Rp{actual.profit:,}".replace(",", "."))
    print(f"  Saldo kas akhir              : Rp{actual.cash_balance:,}".replace(",", "."))
    print(f"  Kebutuhan modal kerja        : Rp{actual_analysis['financingGapRupiah']:,}".replace(",", "."))
    print(f"  B/C Ratio proyeksi           : {plan_analysis['benefitCostRatio']}")
    print(f"  B/C Ratio realisasi          : {actual_analysis['benefitCostRatio']}")


if __name__ == "__main__":
    main()

