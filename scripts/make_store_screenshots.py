"""Play Store phone screenshots from the user-guide captures (docs/panduan/gambar), so the listing
always shows the app as it is. Run after scripts/capture_module_screenshots.ps1:

    py -I scripts/make_store_screenshots.py

Play accepts a long side of at most twice the short side; the captures are 9:20, so each is cut to
1:2 from the top (header and main content) and saved as PNG in docs/assets/store/.
"""
import os
import sys

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SOURCE = os.path.join(ROOT, "docs", "panduan", "gambar")
TARGET = os.path.join(ROOT, "docs", "assets", "store")
SHOTS = [
    ("22_beranda_realisasi.jpg", "screenshot-01-beranda.png"),
    ("14_rencana_kelayakan.jpg", "screenshot-02-rencana-kelayakan.png"),
    ("20_daftar_catatan.jpg", "screenshot-03-catatan-ubah-hapus.png"),
    ("30_laporan_kelayakan.jpg", "screenshot-04-laporan-bep.png"),
    ("26_stok_daftar.jpg", "screenshot-05-stok.png"),
    ("33_pratinjau_impor.jpg", "screenshot-06-impor-excel.png"),
    ("43_sinkronisasi.jpg", "screenshot-07-sinkronisasi.png"),
    ("37_pusat_bantuan.jpg", "screenshot-08-pusat-bantuan.png"),
]

os.makedirs(TARGET, exist_ok=True)
for name in os.listdir(TARGET):
    if name.startswith("screenshot-") and name.endswith(".png"):
        os.remove(os.path.join(TARGET, name))
for source, target in SHOTS:
    image = Image.open(os.path.join(SOURCE, source)).convert("RGB")
    width, height = image.size
    cropped = image.crop((0, 0, width, min(height, width * 2)))
    assert max(cropped.size) <= 2 * min(cropped.size), (source, cropped.size)
    cropped.save(os.path.join(TARGET, target), optimize=True)
    print(f"{target:42} {cropped.size[0]}x{cropped.size[1]}")
sys.exit(0)
