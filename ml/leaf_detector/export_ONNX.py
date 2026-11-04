
from ultralytics import YOLO
from pathlib import Path


def main():
    WEIGHTS = Path(
        "runs/obb/runs/obb/yolo26s_100epochs/weights/best.pt"
    )

    if not WEIGHTS.exists():
        raise FileNotFoundError(f"Не найдены веса: {WEIGHTS}")

    model = YOLO(str(WEIGHTS))

    print("=" * 60)
    print("YOLO26s-OBB ONNX EXPORT")
    print("=" * 60)

    output = model.export(
        format="onnx",
        imgsz=640,
        batch=1,
        dynamic=False,
        simplify=True,
        opset=17,
        device="cpu"
    )

    print("\n" + "=" * 60)
    print("EXPORT FINISHED")
    print("=" * 60)
    print(f"ONNX model: {output}")
    print(f"File exists: {Path(output).exists()}")

    if Path(output).exists():
        print(f"Size: {Path(output).stat().st_size / 1024**2:.2f} MB")


if __name__ == "__main__":
    main()
