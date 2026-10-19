import argparse, json, random
from pathlib import Path

import numpy as np
import onnx
import onnxruntime as ort
import torch, torch.nn as nn
from PIL import Image
from torchvision import models, transforms

MEAN, STD = [0.485, 0.456, 0.406], [0.229, 0.224, 0.225]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--pth", default="efficientnet_b0_best.pth")
    ap.add_argument("--onnx", default="efficientnet_b0.onnx")
    ap.add_argument("--classes", default="classes.json")
    ap.add_argument("--data", default="dataset/test")
    ap.add_argument("--n", type=int, default=200, help="сколько тестовых изображений сравнить")
    a = ap.parse_args()

    ck = torch.load(a.pth, map_location="cpu")
    classes, S = ck["classes"], ck.get("image_size", 224)
    model = models.efficientnet_b0(weights=None)
    model.classifier[1] = nn.Linear(model.classifier[1].in_features, len(classes))
    model.load_state_dict(ck["model_state_dict"])
    model.eval()

    onnx_path = Path(a.onnx)
    for stale in (onnx_path, Path(str(onnx_path) + ".data")):
        stale.unlink(missing_ok=True)
    dummy = torch.randn(1, 3, S, S)
    torch.onnx.export(model, dummy, str(onnx_path), input_names=["input"], output_names=["output"],
                      opset_version=17, dynamo=False)
    # на случай, если экспортёр вынес веса наружу - вшиваем обратно
    m = onnx.load(str(onnx_path), load_external_data=True)
    onnx.save_model(m, str(onnx_path), save_as_external_data=False)
    Path(str(onnx_path) + ".data").unlink(missing_ok=True)
    assert not Path(str(onnx_path) + ".data").exists()
    onnx.checker.check_model(onnx.load(str(onnx_path)))
    Path(a.classes).write_text(json.dumps(classes, ensure_ascii=False, indent=4), encoding="utf-8")
    print(f"Сохранено: {onnx_path} ({onnx_path.stat().st_size/1e6:.1f} МБ), {a.classes} ({len(classes)} классов)")

    # --- сравнение ---
    tf = transforms.Compose([transforms.Resize((S, S)), transforms.ToTensor(), transforms.Normalize(MEAN, STD)])
    files = [p for p in Path(a.data).rglob("*") if p.suffix.lower() in {".jpg", ".jpeg", ".png", ".webp", ".bmp"}]
    random.seed(0); random.shuffle(files); files = files[:a.n]
    sess = ort.InferenceSession(str(onnx_path), providers=["CPUExecutionProvider"])
    same = 0; max_diff = 0.0; correct_t = correct_o = 0
    for p in files:
        x = tf(Image.open(p).convert("RGB")).unsqueeze(0)
        with torch.no_grad():
            lt = model(x).numpy()
        lo = sess.run(None, {"input": x.numpy()})[0]
        max_diff = max(max_diff, float(np.abs(lt - lo).max()))
        same += int(lt.argmax() == lo.argmax())
        true = p.parent.name
        correct_t += classes[int(lt.argmax())] == true; correct_o += classes[int(lo.argmax())] == true
    n = len(files)
    print(f"Сравнено {n} изображений: одинаковый класс у {same}/{n}; макс. |Δ логитов| = {max_diff:.2e}")
    print(f"Accuracy на этой выборке: PyTorch {correct_t/n:.4f}, ONNX {correct_o/n:.4f}")
    assert same == n, "ONNX и PyTorch расходятся!"


if __name__ == "__main__":
    main()
