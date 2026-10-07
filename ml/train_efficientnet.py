from pathlib import Path
import json
import copy

import torch
import torch.nn as nn
from torch.utils.data import DataLoader
from torchvision import datasets, transforms, models
from sklearn.metrics import classification_report, confusion_matrix
import matplotlib.pyplot as plt



# Настройки
DATASET_DIR = Path(".")

TRAIN_DIR = DATASET_DIR / "dataset/train"
VALID_DIR = DATASET_DIR / "dataset/valid"
TEST_DIR = DATASET_DIR / "dataset/test"

IMAGE_SIZE = 224
BATCH_SIZE = 64
EPOCHS = 10
LEARNING_RATE = 0.0001
WEIGHT_DECAY = 0.0001

NUM_WORKERS = 4

MODEL_PATH = "efficientnet_b0_best.pth"
CLASSES_PATH = "classes.json"

DEVICE = torch.device("cuda" if torch.cuda.is_available() else "cpu")


# Основная функция
def main():
    # Преобразования
    train_transform = transforms.Compose([
        transforms.Resize((IMAGE_SIZE, IMAGE_SIZE)),

        transforms.RandomHorizontalFlip(p=0.5),
        transforms.RandomRotation(15),
        transforms.ColorJitter(
            brightness=0.2,
            contrast=0.2,
            saturation=0.2
        ),

        transforms.ToTensor(),

        transforms.Normalize(
            mean=[0.485, 0.456, 0.406],
            std=[0.229, 0.224, 0.225]
        )
    ])

    valid_test_transform = transforms.Compose([
        transforms.Resize((IMAGE_SIZE, IMAGE_SIZE)),

        transforms.ToTensor(),

        transforms.Normalize(
            mean=[0.485, 0.456, 0.406],
            std=[0.229, 0.224, 0.225]
        )
    ])

    # Dataset
    train_dataset = datasets.ImageFolder(
        TRAIN_DIR,
        transform=train_transform
    )

    valid_dataset = datasets.ImageFolder(
        VALID_DIR,
        transform=valid_test_transform
    )

    test_dataset = datasets.ImageFolder(
        TEST_DIR,
        transform=valid_test_transform
    )


    classes = train_dataset.classes
    num_classes = len(classes)

    print("Классы:")
    for i, class_name in enumerate(classes):
        print(f"{i}: {class_name}")

    print()
    print(f"Количество классов: {num_classes}")
    print(f"Train: {len(train_dataset)}")
    print(f"Valid: {len(valid_dataset)}")
    print(f"Test:  {len(test_dataset)}")
    print(f"Device: {DEVICE}")
    print(f"Workers: {NUM_WORKERS}")
    print()


    if valid_dataset.classes != classes:
        raise RuntimeError("Классы train и valid отличаются")

    if test_dataset.classes != classes:
        raise RuntimeError("Классы train и test отличаются")

    # DataLoader
    train_loader = DataLoader(
        train_dataset,
        batch_size=BATCH_SIZE,
        shuffle=True,
        num_workers=NUM_WORKERS,
        pin_memory=True
    )

    valid_loader = DataLoader(
        valid_dataset,
        batch_size=BATCH_SIZE,
        shuffle=False,
        num_workers=NUM_WORKERS,
        pin_memory=True
    )

    test_loader = DataLoader(
        test_dataset,
        batch_size=BATCH_SIZE,
        shuffle=False,
        num_workers=NUM_WORKERS,
        pin_memory=True
    )


    # Модель
    print("Загрузка EfficientNet-B0...")

    weights = models.EfficientNet_B0_Weights.DEFAULT

    model = models.efficientnet_b0(
        weights=weights
    )

    model.classifier[1] = nn.Linear(
        model.classifier[1].in_features,
        num_classes
    )

    model = model.to(DEVICE)

    print()


    # Loss / Optimizer
    criterion = nn.CrossEntropyLoss()

    optimizer = torch.optim.AdamW(
        model.parameters(),
        lr=LEARNING_RATE,
        weight_decay=WEIGHT_DECAY
    )

    # Функция обучения одной эпохи
    def train_one_epoch(model, loader, criterion, optimizer):

        model.train()

        running_loss = 0.0
        correct = 0
        total = 0

        for images, labels in loader:

            images = images.to(
                DEVICE,
                non_blocking=True
            )

            labels = labels.to(
                DEVICE,
                non_blocking=True
            )

            optimizer.zero_grad()

            outputs = model(images)

            loss = criterion(
                outputs,
                labels
            )

            loss.backward()

            optimizer.step()

            running_loss += (
                loss.item() * images.size(0)
            )

            predictions = outputs.argmax(dim=1)

            correct += (
                predictions == labels
            ).sum().item()

            total += labels.size(0)

        loss = running_loss / total
        accuracy = correct / total

        return loss, accuracy

    # Валидация
    def evaluate(model, loader, criterion):

        model.eval()

        running_loss = 0.0
        correct = 0
        total = 0

        with torch.no_grad():

            for images, labels in loader:

                images = images.to(
                    DEVICE,
                    non_blocking=True
                )

                labels = labels.to(
                    DEVICE,
                    non_blocking=True
                )

                outputs = model(images)

                loss = criterion(
                    outputs,
                    labels
                )

                running_loss += (
                    loss.item() * images.size(0)
                )

                predictions = outputs.argmax(dim=1)

                correct += (
                    predictions == labels
                ).sum().item()

                total += labels.size(0)

        loss = running_loss / total
        accuracy = correct / total

        return loss, accuracy


    # Обучение
    history = {
        "train_loss": [],
        "train_accuracy": [],
        "valid_loss": [],
        "valid_accuracy": []
    }

    best_valid_accuracy = 0.0
    best_model_state = None

    print("Начало обучения")
    print("=" * 70)

    for epoch in range(EPOCHS):

        train_loss, train_accuracy = train_one_epoch(
            model,
            train_loader,
            criterion,
            optimizer
        )

        valid_loss, valid_accuracy = evaluate(
            model,
            valid_loader,
            criterion
        )

        history["train_loss"].append(train_loss)
        history["train_accuracy"].append(train_accuracy)
        history["valid_loss"].append(valid_loss)
        history["valid_accuracy"].append(valid_accuracy)

        print(
            f"Epoch {epoch + 1:2d}/{EPOCHS} | "
            f"Train Loss: {train_loss:.4f} | "
            f"Train Acc: {train_accuracy:.4f} | "
            f"Valid Loss: {valid_loss:.4f} | "
            f"Valid Acc: {valid_accuracy:.4f}"
        )

        if valid_accuracy > best_valid_accuracy:

            best_valid_accuracy = valid_accuracy

            best_model_state = copy.deepcopy(
                model.state_dict()
            )

            torch.save(
                {
                    "model_state_dict": best_model_state,
                    "classes": classes,
                    "image_size": IMAGE_SIZE
                },
                MODEL_PATH
            )

            print(
                f"  -> сохранена лучшая модель "
                f"(valid acc = {valid_accuracy:.4f})"
            )

    print("=" * 70)

    print()
    print(
        f"Лучшая Valid Accuracy: "
        f"{best_valid_accuracy:.4f}"
    )


    # Лучшая модель
    model.load_state_dict(
        best_model_state
    )

    # Test
    test_loss, test_accuracy = evaluate(
        model,
        test_loader,
        criterion
    )

    print()
    print("=" * 70)
    print("TEST")
    print("=" * 70)

    print(f"Test Loss: {test_loss:.4f}")
    print(f"Test Accuracy: {test_accuracy:.4f}")


    # Classification report
    model.eval()

    all_predictions = []
    all_labels = []

    with torch.no_grad():

        for images, labels in test_loader:

            images = images.to(DEVICE)

            outputs = model(images)

            predictions = outputs.argmax(dim=1)

            all_predictions.extend(
                predictions.cpu().numpy()
            )

            all_labels.extend(
                labels.numpy()
            )


    print()
    print("=" * 70)
    print("CLASSIFICATION REPORT")
    print("=" * 70)

    print(
        classification_report(
            all_labels,
            all_predictions,
            target_names=classes,
            digits=4
        )
    )


    # Confusion matrix
    cm = confusion_matrix(
        all_labels,
        all_predictions
    )

    print()
    print("Confusion Matrix:")
    print(cm)

    # Сохраняем классы
    with open(
        CLASSES_PATH,
        "w",
        encoding="utf-8"
    ) as f:

        json.dump(
            classes,
            f,
            ensure_ascii=False,
            indent=4
        )

    # График Loss
    epochs_range = range(
        1,
        EPOCHS + 1
    )

    plt.figure(figsize=(8, 5))

    plt.plot(
        epochs_range,
        history["train_loss"],
        label="Train Loss"
    )

    plt.plot(
        epochs_range,
        history["valid_loss"],
        label="Valid Loss"
    )

    plt.xlabel("Epoch")
    plt.ylabel("Loss")
    plt.title("EfficientNet-B0 Loss")

    plt.legend()
    plt.grid()

    plt.savefig(
        "efficientnet_b0_loss.png",
        dpi=150,
        bbox_inches="tight"
    )

    plt.close()


    # График Accuracy

    plt.figure(figsize=(8, 5))

    plt.plot(
        epochs_range,
        history["train_accuracy"],
        label="Train Accuracy"
    )

    plt.plot(
        epochs_range,
        history["valid_accuracy"],
        label="Valid Accuracy"
    )

    plt.xlabel("Epoch")
    plt.ylabel("Accuracy")
    plt.title("EfficientNet-B0 Accuracy")

    plt.legend()
    plt.grid()

    plt.savefig(
        "efficientnet_b0_accuracy.png",
        dpi=150,
        bbox_inches="tight"
    )

    plt.close()


    print()
    print("Сохранено:")
    print(f"  {MODEL_PATH}")
    print(f"  {CLASSES_PATH}")
    print("  efficientnet_b0_loss.png")
    print("  efficientnet_b0_accuracy.png")


if __name__ == "__main__":
    main()
