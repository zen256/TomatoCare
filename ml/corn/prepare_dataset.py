import argparse, hashlib, json, random, shutil, sys
from collections import defaultdict
from pathlib import Path

import numpy as np
from PIL import Image

EXT = {".jpg", ".jpeg", ".png", ".bmp", ".webp"}


def md5(p: Path) -> str:
    h = hashlib.md5()
    with open(p, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def dhash(p: Path, size=8) -> int:
    img = Image.open(p).convert("L").resize((size + 1, size), Image.LANCZOS)
    a = np.asarray(img, dtype=np.int16)
    bits = (a[:, 1:] > a[:, :-1]).flatten()
    return int("".join("1" if b else "0" for b in bits), 2)


def hamming(a: int, b: int) -> int:
    return bin(a ^ b).count("1")


class DSU:
    def __init__(self, n): self.p = list(range(n))
    def find(self, x):
        while self.p[x] != x:
            self.p[x] = self.p[self.p[x]]; x = self.p[x]
        return x
    def union(self, a, b): self.p[self.find(a)] = self.find(b)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--src", nargs="+", required=True, help="папки-источники (внутри - папки классов)")
    ap.add_argument("--map", nargs="*", default=[], help='переименование классов: "старое=новое"')
    ap.add_argument("--out", default="dataset")
    ap.add_argument("--near", type=int, default=2, help="порог dHash для почти-дубликатов (0 = только точные)")
    ap.add_argument("--split", nargs=3, type=float, default=[0.70, 0.15, 0.15])
    ap.add_argument("--seed", type=int, default=42)
    ap.add_argument("--min-side", type=int, default=64, help="отбросить слишком мелкие/битые картинки")
    a = ap.parse_args()

    rename = dict(m.split("=", 1) for m in a.map)
    out = Path(a.out)
    if out.exists() and any(out.iterdir()):
        sys.exit(f"{out} уже существует и не пуст - удалите или укажите другой --out")

    items = []  # (path, class, source)
    bad = 0
    for src in map(Path, a.src):
        for cdir in sorted(d for d in src.iterdir() if d.is_dir()):
            cls = rename.get(cdir.name, cdir.name)
            for p in cdir.rglob("*"):
                if p.suffix.lower() in EXT:
                    try:
                        with Image.open(p) as im:
                            im.verify()
                        with Image.open(p) as im:
                            if min(im.size) < a.min_side:
                                bad += 1; continue
                    except Exception:
                        bad += 1; continue
                    items.append((p, cls, src.name))
    print(f"Найдено файлов: {len(items)}, битых/мелких пропущено: {bad}")
    if not items:
        sys.exit("Нет изображений")

    # --- дубликаты ---
    n = len(items)
    dsu = DSU(n)
    by_md5 = defaultdict(list)
    for i, (p, _, _) in enumerate(items):
        by_md5[md5(p)].append(i)
    for idxs in by_md5.values():
        for j in idxs[1:]:
            dsu.union(idxs[0], j)
    n_exact = sum(len(v) - 1 for v in by_md5.values())

    hashes = np.array([dhash(p) for p, _, _ in items], dtype=np.uint64)
    if a.near >= 0:
        order = np.argsort(hashes)
        # точное совпадение dHash
        for k in range(1, n):
            if hashes[order[k]] == hashes[order[k - 1]]:
                dsu.union(int(order[k]), int(order[k - 1]))
        if a.near > 0:
            # попарно с векторизацией: n^2/2 операций, для ~20k изображений - секунды
            for i in range(n - 1):
                x = hashes[i] ^ hashes[i + 1:]
                pc = np.zeros(len(x), dtype=np.uint8)
                for sh in range(0, 64, 8):
                    pc += np.array([bin(v).count("1") for v in range(256)], dtype=np.uint8)[((x >> np.uint64(sh)) & np.uint64(255)).astype(np.int64)]
                for j in np.nonzero(pc <= a.near)[0]:
                    dsu.union(i, i + 1 + int(j))

    groups = defaultdict(list)
    for i in range(n):
        groups[dsu.find(i)].append(i)

    keep, dropped_dup, dropped_conflict = [], 0, 0
    conflicts = []
    for g in groups.values():
        classes = {items[i][1] for i in g}
        if len(classes) > 1:
            dropped_conflict += len(g)
            conflicts.append([str(items[i][0]) for i in g])
            continue
        g.sort(key=lambda i: -Path(items[i][0]).stat().st_size)  # оставляем самый большой файл
        keep.append(g[0])
        dropped_dup += len(g) - 1
    print(f"Удалено дубликатов: {dropped_dup} (из них точных по MD5: {n_exact}); "
          f"удалено из-за конфликта меток: {dropped_conflict}; осталось: {len(keep)}")

    # --- разбиение ---
    rng = random.Random(a.seed)
    per_class = defaultdict(list)
    for i in keep:
        per_class[items[i][1]].append(i)
    stats = {"splits": {}, "sources": defaultdict(lambda: defaultdict(int))}
    tr, va, te = a.split
    for cls, idxs in sorted(per_class.items()):
        rng.shuffle(idxs)
        n_tr, n_va = int(len(idxs) * tr), int(len(idxs) * va)
        parts = {"train": idxs[:n_tr], "valid": idxs[n_tr:n_tr + n_va], "test": idxs[n_tr + n_va:]}
        for sp, ids in parts.items():
            d = out / sp / cls
            d.mkdir(parents=True, exist_ok=True)
            for i in ids:
                p, _, srcname = items[i]
                shutil.copy2(p, d / f"{srcname}_{p.name}")
                stats["sources"][srcname][cls] += 1
            stats["splits"].setdefault(sp, {})[cls] = len(ids)
    stats["sources"] = {k: dict(v) for k, v in stats["sources"].items()}
    stats.update(found=len(items), broken=bad, duplicates_removed=dropped_dup,
                 exact_md5_duplicates=n_exact, label_conflicts_removed=dropped_conflict,
                 kept=len(keep), near_threshold=a.near, seed=a.seed, label_conflict_groups=conflicts[:20])
    (out / "dataset_stats.json").write_text(json.dumps(stats, ensure_ascii=False, indent=2), encoding="utf-8")
    for sp in ("train", "valid", "test"):
        print(sp, sum(stats["splits"][sp].values()), stats["splits"][sp])


if __name__ == "__main__":
    main()
