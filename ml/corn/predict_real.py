import argparse, json
from pathlib import Path
import numpy as np, onnxruntime as ort
from PIL import Image
from torchvision import transforms

MEAN, STD = [0.485, 0.456, 0.406], [0.229, 0.224, 0.225]
ap = argparse.ArgumentParser()
ap.add_argument("--dir", default="real_photos"); ap.add_argument("--onnx", default="efficientnet_b0.onnx")
ap.add_argument("--classes", default="classes.json"); ap.add_argument("--size", type=int, default=224)
a = ap.parse_args()
classes = json.loads(Path(a.classes).read_text(encoding="utf-8"))
tf = transforms.Compose([transforms.Resize((a.size, a.size)), transforms.ToTensor(), transforms.Normalize(MEAN, STD)])
sess = ort.InferenceSession(a.onnx, providers=["CPUExecutionProvider"])
rows, ok, lab = [], 0, 0
for p in sorted(x for x in Path(a.dir).rglob("*") if x.suffix.lower() in {".jpg", ".jpeg", ".png", ".webp"}):
    try:
        img = Image.open(p).convert("RGB")
    except Exception as e:
        print(f"ПРОПУЩЕН {p} - не удалось открыть как изображение (возможно, другой формат под именем .jpg, например AVIF/WEBP): {e}")
        continue
    lg = sess.run(None, {"input": tf(img).unsqueeze(0).numpy()})[0][0]
    pr = np.exp(lg - lg.max()); pr /= pr.sum(); top = pr.argsort()[::-1][:3]
    true = p.parent.name if p.parent.name in classes else ""
    mark = ""
    if true:
        lab += 1; hit = classes[top[0]] == true; ok += hit; mark = "✅" if hit else "❌"
    s = ", ".join(f"{classes[i]} {pr[i]:.2f}" for i in top)
    print(f"{p.name}: {s} {('[истина: ' + true + '] ' + mark) if true else ''}")
    rows.append(f"| {p.name} | {true or '—'} | {classes[top[0]]} | {pr[top[0]]:.2f} | {mark} |")
if lab: print(f"\nТочность на реальных фото с метками: {ok}/{lab} = {ok/lab:.2%}")
from collections import Counter
tot, good = Counter(), Counter()
for r in rows:
    c = r.split("|")[2].strip(); m = r.split("|")[5].strip()
    if c != "—":
        tot[c] += 1; good[c] += (m == "✅")
for c in tot: print(f"  {c}: {good[c]}/{tot[c]}")
Path("real_photos_results.md").write_text("| Файл | Истина | Предсказание | Уверенность | |\n|---|---|---|---|---|\n" + "\n".join(rows) + "\n", encoding="utf-8")
