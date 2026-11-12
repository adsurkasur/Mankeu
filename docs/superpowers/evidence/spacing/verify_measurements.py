"""Verify recorded spacing evidence. Reads XML/PNG only; does not touch device data."""

from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parent
SCALE = 420 / 160


def nodes(name):
    return list(ET.parse(ROOT / f"{name}.xml").getroot().iter("node"))


def bounds(node):
    return tuple(map(int, node.attrib["bounds"].replace("][", ",").strip("[]").split(",")))


def text_bounds(name, text):
    return bounds(next(n for n in nodes(name) if n.get("text") == text))


def last_control_bottom(name):
    scroll = next(n for n in nodes(name) if n.get("class") == "android.widget.ScrollView")
    return max(bounds(n)[3] for n in scroll)


for xml in ROOT.glob("*.xml"):
    assert xml.with_suffix(".png").is_file(), f"Missing screenshot: {xml.stem}"
    assert xml.with_suffix(".png").read_bytes()[:8] == b"\x89PNG\r\n\x1a\n"

expected = {
    "money-no-keyboard": (1043, 2126),
    "money-keyboard": (1043, 1285),
    "product-empty": (1596, 2126),
    "sale-no-keyboard": (1131, 2126),
    "buy-no-keyboard": (1380, 2126),
}
for name, (bottom, reason_top) in expected.items():
    reason_text = {
        "product-empty": "Isi namanya dulu.",
        "sale-no-keyboard": "Tambahkan barang yang dijual dulu.",
    }.get(name, "Isi jumlah uang lebih dari Rp0 dulu.")
    reason = next(n for n in nodes(name) if n.get("text") == reason_text)
    actual = last_control_bottom(name), bounds(reason)[1]
    assert actual == (bottom, reason_top), (name, actual)
    print(f"{name}: {reason_top-bottom}px = {(reason_top-bottom)/SCALE:.1f}dp")

product_label_bottom = text_bounds("product-empty", "Resep 1 batch")[3]
scroll = next(n for n in nodes("product-empty") if n.get("class") == "android.widget.ScrollView")
button = next(n for n in scroll if any(c.get("text") == "+ Biaya lain" for c in n.iter()))
assert bounds(button)[1] - product_label_bottom == 65
assert text_bounds("money-no-keyboard", "Simpan")[1] - text_bounds("money-keyboard", "Simpan")[1] == 841
assert text_bounds("product-line-sheet", "Biaya lain")[1] - text_bounds("product-line-sheet-keyboard", "Biaya lain")[1] == 841
print("PASS: screenshot pairs, form gaps, recipe gap, and 841px IME movement")
