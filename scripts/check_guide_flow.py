"""Every in-text reference must point back (to a section already read). Navigation sections are exempt."""
import os, re, sys
p = sys.argv[1]
s = open(p, encoding="utf-8").read()
def key(h):
    m = re.match(r"(\d+)(?:\.(\d+))?", h)
    if m: return (int(m.group(1)), int(m.group(2) or 0))
    m = re.match(r"Lampiran ([AB])", h)
    return (100 + ord(m.group(1)), 0) if m else None
def slug(h): return re.sub(r"[^\w\- ]", "", h.strip().lower()).replace(" ", "-")
anchors = {}
for h in re.findall(r"^#{2,3} (.+)$", s, flags=re.M):
    anchors[slug(h)] = key(h)
problems, cur = [], None
for line in s.split("\n"):
    m = re.match(r"^#{2,3} (.+)$", line)
    if m:
        k = key(m.group(1))
        if k: cur = k
        elif m.group(1) in ("Cara Memakai Panduan Ini", "Daftar Isi"): cur = None
        continue
    if cur is None: continue
    for a in re.findall(r"\]\(#([^)]+)\)", line):
        t = anchors.get(a)
        if t is None: problems.append(f"broken #{a}")
        elif t > cur: problems.append(f"{cur} -> #{a} (forward)")
    for n in re.findall(r"bagian (\d+(?:\.\d+)?)", line):
        a, b = (n.split(".") + ["0"])[:2]
        if (int(a), int(b)) > cur: problems.append(f"{cur} -> 'bagian {n}' (forward)")
    for l in re.findall(r"Lampiran [AB]", line):
        problems.append(f"{cur} -> '{l}' (mentions an appendix in the text)")
imgs = set(re.findall(r"\]\((gambar/[^)]+)\)", s)); files = {"gambar/" + f for f in os.listdir(os.path.join(os.path.dirname(p), "gambar"))}
problems += [f"missing image {x}" for x in imgs - files] + [f"unused image {x}" for x in files - imgs]
print("\n".join(problems) or "OK: no forward references, all images and anchors valid")
