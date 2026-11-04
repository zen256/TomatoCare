from pathlib import Path

import torch
from ultralytics import YOLO


def main():
    MODEL = "yolo26s-obb.pt"
    DATA_YAML = "data.yaml"

    PROJECT = "runs/obb"
    NAME = "yolo26s_100epochs"

    EPOCHS = 100
    PATIENCE = 20

    print("=" * 65)
    print("YOLO26s-OBB TRAINING")
    print("=" * 65)

    print(f"PyTorch:       {torch.__version__}")
    print(f"CUDA:          {torch.cuda.is_available()}")

    if torch.cuda.is_available():
        print(f"GPU:           {torch.cuda.get_device_name(0)}")
        print(
            f"VRAM:          "
            f"{torch.cuda.get_device_properties(0).total_memory / 1024**3:.2f} GB"
        )
    else:
        raise RuntimeError("CUDA недоступна!")

    print(f"Model:         {MODEL}")
    print(f"Dataset:       {DATA_YAML}")
    print(f"Epochs:        {EPOCHS}")
    print(f"Early stopping: patience={PATIENCE}")

    print("=" * 65)

    model = YOLO(MODEL)

    # Обучение с проверкой на validation.
    train_results = model.train(
        data=DATA_YAML,

        epochs=EPOCHS,
        imgsz=640,
        batch=16,
        device=0,
        workers=6,

        project=PROJECT,
        name=NAME,
        exist_ok=True,

        patience=PATIENCE,

        optimizer="auto",
        seed=42,
        deterministic=True,

        amp=True,
        cache=False,

        save=True,
        save_period=10,

        plots=True,
        val=True,

        verbose=True
    )

    save_dir = Path(train_results.save_dir)
    best_weights = save_dir / "weights" / "best.pt"
    last_weights = save_dir / "weights" / "last.pt"

    print("\n" + "=" * 65)
    print("TRAINING FINISHED")
    print("=" * 65)

    print(f"Results:       {save_dir}")
    print(f"Best weights:  {best_weights}")
    print(f"Last weights:  {last_weights}")

    if not best_weights.exists():
        raise FileNotFoundError(f"Не найдены лучшие веса: {best_weights}")

    # Загружаем лучшую модель.
    best_model = YOLO(str(best_weights))

    # Финальная проверка на TEST.
    # Test не использовался при обучении и выборе лучшей эпохи.
    print("\n" + "=" * 65)
    print("FINAL TEST EVALUATION")
    print("=" * 65)

    metrics = best_model.val(
        data=DATA_YAML,
        split="test",

        imgsz=640,
        batch=16,
        device=0,
        workers=6,

        plots=True,
        save_json=True,

        project=PROJECT,
        name=f"{NAME}_test",
        exist_ok=True,

        verbose=True
    )

    # Сохраняем численные метрики в отдельный файл.
    test_dir = Path(metrics.save_dir)
    metrics_file = test_dir / "test_metrics.txt"

    with open(metrics_file, "w") as file:
        file.write("YOLO26s-OBB TEST RESULTS\n")
        file.write("=" * 50 + "\n\n")

        for metric_name, value in metrics.results_dict.items():
            file.write(f"{metric_name}: {value}\n")

    print("\n" + "=" * 65)
    print("TEST METRICS")
    print("=" * 65)

    for metric_name, value in metrics.results_dict.items():
        print(f"{metric_name:<30} {value:.4f}")

    print("\n" + "=" * 65)
    print("ALL RESULTS SAVED")
    print("=" * 65)

    print(f"Training results: {save_dir}")
    print(f"Test results:     {test_dir}")
    print(f"Test metrics:     {metrics_file}")


if __name__ == "__main__":
    main()
