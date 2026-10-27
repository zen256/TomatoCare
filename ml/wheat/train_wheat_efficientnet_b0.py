from pathlib import Path
import json
import urllib.request
import urllib.error
import copy
import time

import torch
import torch.nn as nn
from torch.utils.data import DataLoader
from torchvision import datasets, transforms, models
from sklearn.metrics import (
    accuracy_score,
    classification_report,
    confusion_matrix,
    ConfusionMatrixDisplay,
)
import matplotlib.pyplot as plt
from PIL import Image

# Настройки
# Ожидаемая структура:
# dataset/
#   train/
#     class_1/
#     class_2/
#     ...
#   valid/
#     class_1/
#     class_2/
#     ...
#   test/
#     class_1/
#     class_2/
#     ...

DATASET_DIR = Path("dataset")
TRAIN_DIR = DATASET_DIR / "train"
VALID_DIR = DATASET_DIR / "valid"
TEST_DIR = DATASET_DIR / "test"

IMAGE_SIZE = 224
BATCH_SIZE = 32
EPOCHS = 15
LEARNING_RATE = 0.0001
WEIGHT_DECAY = 0.0001
NUM_WORKERS = 0  # 0 обычно надёжнее на Windows и в некоторых средах

MODEL_PATH = "efficientnet_b0_best.pth"
CLASSES_PATH = "classes.json"
LOSS_PLOT_PATH = "efficientnet_b0_loss.png"
ACCURACY_PLOT_PATH = "efficientnet_b0_accuracy.png"
CM_PLOT_PATH = "efficientnet_b0_confusion_matrix.png"
INTERNET_DIR = Path("internet_images")

DEVICE = torch.device("cuda" if torch.cuda.is_available() else "cpu")

def check_folders():
    """Проверяет наличие папок и классов."""
    for folder in (TRAIN_DIR, VALID_DIR, TEST_DIR):
        if not folder.exists():
            raise FileNotFoundError(
                f"Не найдена папка: {folder.resolve()}\n"
                "Проверьте структуру dataset/train, dataset/valid и dataset/test."
            )


def make_transforms():
    # EfficientNet-B0 предобучена на ImageNet. Используем нормализацию ImageNet.
    # Все изображения подаются в сеть в размере 224x224.
    train_transform = transforms.Compose([
        transforms.Resize((IMAGE_SIZE, IMAGE_SIZE)),
        transforms.RandomHorizontalFlip(p=0.5),
        transforms.RandomRotation(10),
        transforms.ColorJitter(brightness=0.15, contrast=0.15, saturation=0.10),
        transforms.ToTensor(),
        transforms.Normalize(
            mean=[0.485, 0.456, 0.406],
            std=[0.229, 0.224, 0.225],
        ),
    ])

    valid_test_transform = transforms.Compose([
        transforms.Resize((IMAGE_SIZE, IMAGE_SIZE)),
        transforms.ToTensor(),
        transforms.Normalize(
            mean=[0.485, 0.456, 0.406],
            std=[0.229, 0.224, 0.225],
        ),
    ])
    return train_transform, valid_test_transform


def train_one_epoch(model, loader, criterion, optimizer):
    model.train()
    running_loss = 0.0
    correct = 0
    total = 0

    for images, labels in loader:
        images = images.to(DEVICE)
        labels = labels.to(DEVICE)

        optimizer.zero_grad()
        outputs = model(images)
        loss = criterion(outputs, labels)
        loss.backward()
        optimizer.step()

        running_loss += loss.item() * images.size(0)
        correct += (outputs.argmax(dim=1) == labels).sum().item()
        total += labels.size(0)

    return running_loss / total, correct / total


def evaluate(model, loader, criterion):
    model.eval()
    running_loss = 0.0
    correct = 0
    total = 0

    with torch.no_grad():
        for images, labels in loader:
            images = images.to(DEVICE)
            labels = labels.to(DEVICE)

            outputs = model(images)
            loss = criterion(outputs, labels)

            running_loss += loss.item() * images.size(0)
            correct += (outputs.argmax(dim=1) == labels).sum().item()
            total += labels.size(0)

    return running_loss / total, correct / total


def get_predictions(model, loader):
    model.eval()
    all_predictions = []
    all_labels = []

    with torch.no_grad():
        for images, labels in loader:
            outputs = model(images.to(DEVICE))
            predictions = outputs.argmax(dim=1)
            all_predictions.extend(predictions.cpu().tolist())
            all_labels.extend(labels.tolist())

    return all_labels, all_predictions


def save_training_plots(history):
    epochs_range = range(1, len(history["train_loss"]) + 1)

    plt.figure(figsize=(8, 5))
    plt.plot(epochs_range, history["train_loss"], label="Train Loss")
    plt.plot(epochs_range, history["valid_loss"], label="Validation Loss")
    plt.xlabel("Эпоха")
    plt.ylabel("Loss")
    plt.title("EfficientNet-B0: функция потерь")
    plt.legend()
    plt.grid(True)
    plt.tight_layout()
    plt.savefig(LOSS_PLOT_PATH, dpi=150)
    plt.close()

    plt.figure(figsize=(8, 5))
    plt.plot(epochs_range, history["train_accuracy"], label="Train Accuracy")
    plt.plot(epochs_range, history["valid_accuracy"], label="Validation Accuracy")
    plt.xlabel("Эпоха")
    plt.ylabel("Accuracy")
    plt.title("EfficientNet-B0: точность")
    plt.legend()
    plt.grid(True)
    plt.tight_layout()
    plt.savefig(ACCURACY_PLOT_PATH, dpi=150)
    plt.close()


def main():
    check_folders()
    train_transform, valid_test_transform = make_transforms()

    train_dataset = datasets.ImageFolder(TRAIN_DIR, transform=train_transform)
    valid_dataset = datasets.ImageFolder(VALID_DIR, transform=valid_test_transform)
    test_dataset = datasets.ImageFolder(TEST_DIR, transform=valid_test_transform)

    classes = train_dataset.classes
    num_classes = len(classes)

    if num_classes != 4:
        raise ValueError(
            f"Ожидалось 4 класса, но в train найдено {num_classes}: {classes}"
        )
    if valid_dataset.classes != classes or test_dataset.classes != classes:
        raise RuntimeError(
            "Наборы train, valid и test должны содержать одинаковые папки классов."
        )
    if len(train_dataset) == 0 or len(valid_dataset) == 0 or len(test_dataset) == 0:
        raise RuntimeError("Одна из выборок пуста.")

    print("Классы:")
    for index, class_name in enumerate(classes):
        print(f"  {index}: {class_name}")
    print(f"\nTrain: {len(train_dataset)}")
    print(f"Valid: {len(valid_dataset)}")
    print(f"Test:  {len(test_dataset)}")
    print(f"Размер входа: {IMAGE_SIZE}x{IMAGE_SIZE}")
    print(f"Устройство: {DEVICE}\n")

    train_loader = DataLoader(
        train_dataset, batch_size=BATCH_SIZE, shuffle=True,
        num_workers=NUM_WORKERS, pin_memory=(DEVICE.type == "cuda")
    )
    valid_loader = DataLoader(
        valid_dataset, batch_size=BATCH_SIZE, shuffle=False,
        num_workers=NUM_WORKERS, pin_memory=(DEVICE.type == "cuda")
    )
    test_loader = DataLoader(
        test_dataset, batch_size=BATCH_SIZE, shuffle=False,
        num_workers=NUM_WORKERS, pin_memory=(DEVICE.type == "cuda")
    )

    print("Загрузка предобученной EfficientNet-B0...")
    weights = models.EfficientNet_B0_Weights.DEFAULT
    model = models.efficientnet_b0(weights=weights)

    # Меняем последний слой на 4 выходных нейронов (по числу классов).
    model.classifier[1] = nn.Linear(
        model.classifier[1].in_features,
        num_classes
    )
    model = model.to(DEVICE)

    criterion = nn.CrossEntropyLoss()
    optimizer = torch.optim.AdamW(
        model.parameters(),
        lr=LEARNING_RATE,
        weight_decay=WEIGHT_DECAY
    )

    history = {
        "train_loss": [],
        "train_accuracy": [],
        "valid_loss": [],
        "valid_accuracy": [],
    }

    best_valid_loss = float("inf")

    print("\nНачало обучения")

    for epoch in range(EPOCHS):
        train_loss, train_accuracy = train_one_epoch(
            model, train_loader, criterion, optimizer
        )
        valid_loss, valid_accuracy = evaluate(
            model, valid_loader, criterion
        )

        history["train_loss"].append(train_loss)
        history["train_accuracy"].append(train_accuracy)
        history["valid_loss"].append(valid_loss)
        history["valid_accuracy"].append(valid_accuracy)

        print(
            f"Эпоха {epoch + 1:02d}/{EPOCHS} | "
            f"Train Loss: {train_loss:.4f} | "
            f"Train Acc: {train_accuracy:.4f} | "
            f"Val Loss: {valid_loss:.4f} | "
            f"Val Acc: {valid_accuracy:.4f}"
        )

        # Лучшая модель выбирается ИМЕННО по минимальному val_loss.
        if valid_loss < best_valid_loss:
            best_valid_loss = valid_loss
            torch.save(
                {
                    "model_state_dict": model.state_dict(),
                    "classes": classes,
                    "image_size": IMAGE_SIZE,
                    "best_valid_loss": best_valid_loss,
                },
                MODEL_PATH
            )
            print(f"  -> сохранена лучшая модель (val_loss = {valid_loss:.4f})")

    print(f"Минимальный val_loss: {best_valid_loss:.4f}")

    save_training_plots(history)

    # Загружаем с диска именно сохранённую лучшую модель.
    checkpoint = torch.load(MODEL_PATH, map_location=DEVICE)
    model.load_state_dict(checkpoint["model_state_dict"])
    classes = checkpoint["classes"]

    # Оценка на тестовой выборке выполняется после выбора лучшей модели.
    test_loss, test_accuracy = evaluate(model, test_loader, criterion)
    y_true, y_pred = get_predictions(model, test_loader)

    print("РЕЗУЛЬТАТЫ НА ТЕСТОВОЙ ВЫБОРКЕ")
    print(f"Test Loss: {test_loss:.4f}")
    print(f"Accuracy:  {accuracy_score(y_true, y_pred):.4f}")
    print("\nPrecision, Recall и F1-score по классам:")
    print(classification_report(
        y_true,
        y_pred,
        labels=list(range(num_classes)),
        target_names=classes,
        digits=4,
        zero_division=0
    ))

    cm = confusion_matrix(y_true, y_pred, labels=list(range(num_classes)))
    print("Confusion Matrix:")
    print(cm)

    display = ConfusionMatrixDisplay(confusion_matrix=cm, display_labels=classes)
    fig, ax = plt.subplots(figsize=(9, 7))
    display.plot(ax=ax, cmap="Blues", xticks_rotation=45, values_format="d")
    ax.set_title("EfficientNet-B0: матрица ошибок на тестовой выборке")
    fig.tight_layout()
    fig.savefig(CM_PLOT_PATH, dpi=150, bbox_inches="tight")
    plt.close(fig)

    with open(CLASSES_PATH, "w", encoding="utf-8") as file:
        json.dump(classes, file, ensure_ascii=False, indent=4)

    print("\nСохранённые файлы:")
    print(f"  Лучшая модель: {MODEL_PATH}")
    print(f"  Названия классов: {CLASSES_PATH}")
    print(f"  График Loss: {LOSS_PLOT_PATH}")
    print(f"  График Accuracy: {ACCURACY_PLOT_PATH}")
    print(f"  Матрица ошибок: {CM_PLOT_PATH}")
    print(f"  Фотографии из интернета и предсказания: {INTERNET_DIR.resolve()}")


if __name__ == "__main__":
    main()
