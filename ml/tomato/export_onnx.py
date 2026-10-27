import json

import torch
import torch.nn as nn
from torchvision import models


# Настройки
MODEL_PATH = "efficientnet_b0_best.pth"
CLASSES_PATH = "classes.json"
ONNX_PATH = "efficientnet_b0.onnx"

IMAGE_SIZE = 224

DEVICE = torch.device(
    "cuda" if torch.cuda.is_available() else "cpu"
)


# Загрузка классов
with open(
    CLASSES_PATH,
    "r",
    encoding="utf-8"
) as f:

    classes = json.load(f)

num_classes = len(classes)

print("Классы:")

for i, class_name in enumerate(classes):
    print(f"{i}: {class_name}")

print()
print(f"Количество классов: {num_classes}")
print(f"Device: {DEVICE}")


# Создание модели
print()
print("Создание EfficientNet-B0...")

model = models.efficientnet_b0(
    weights=None
)

model.classifier[1] = nn.Linear(
    model.classifier[1].in_features,
    num_classes
)


# Загрузка обученных весов
print("Загрузка весов...")

checkpoint = torch.load(
    MODEL_PATH,
    map_location=DEVICE,
    weights_only=True
)

model.load_state_dict(
    checkpoint["model_state_dict"]
)

model = model.to(DEVICE)

model.eval()


# Подготовка входного тензора
dummy_input = torch.randn(
    1,
    3,
    IMAGE_SIZE,
    IMAGE_SIZE,
    device=DEVICE
)


# Экспорт в ONNX
print()
print("Экспорт модели в ONNX...")

torch.onnx.export(
    model,
    dummy_input,
    ONNX_PATH,
    input_names=["input"],
    output_names=["output"],
    dynamic_axes={
        "input": {
            0: "batch_size"
        },
        "output": {
            0: "batch_size"
        }
    },
    opset_version=18,
    external_data=False
)


# Результат
print()
print("=" * 60)
print("Экспорт завершён")
print("=" * 60)

print(f"ONNX модель: {ONNX_PATH}")
print(f"Размер входа: {IMAGE_SIZE}x{IMAGE_SIZE}")
print(f"Количество классов: {num_classes}")
