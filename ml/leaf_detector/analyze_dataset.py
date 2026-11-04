
from pathlib import Path
from collections import Counter


DATASET = Path("data")

SPLITS = {
    "train": Path("train.txt"),
    "val": Path("val.txt"),
    "test": Path("test.txt"),
}

CLASSES = {
    0: "wheat",
    1: "tomato",
    2: "corn",
}


def analyze_split(split_name, list_file):
    if not list_file.exists():
        raise FileNotFoundError(f"Не найден файл: {list_file}")

    labels_dir = DATASET / "labels" / split_name
    images_dir = DATASET / "images" / split_name

    counts = Counter()
    errors = []

    lines = list_file.read_text().splitlines()
    image_paths = set()

    for line_number, line in enumerate(lines, 1):
        line = line.strip()

        if not line:
            continue

        image_path = Path(line)

        if not image_path.is_absolute():
            image_path = Path.cwd() / image_path

        image_path = image_path.resolve()

        if image_path in image_paths:
            errors.append(f"Дубликат в {list_file}:{line_number}: {line}")
            continue

        image_paths.add(image_path)

        if image_path.parent != images_dir.resolve():
            errors.append(
                f"Неверный путь в {list_file}:{line_number}: {line}"
            )
            continue

        if not image_path.exists():
            errors.append(f"Изображение не найдено: {image_path}")
            continue

        label_path = labels_dir / f"{image_path.stem}.txt"

        if not label_path.exists():
            errors.append(f"Разметка не найдена: {label_path}")
            continue

        classes = set()

        try:
            for label_line in label_path.read_text().splitlines():
                parts = label_line.split()

                if len(parts) != 9:
                    raise ValueError(
                        f"Ожидалось 9 полей, получено {len(parts)}"
                    )

                class_id = int(parts[0])

                if class_id not in CLASSES:
                    raise ValueError(f"Неизвестный класс: {class_id}")

                classes.add(class_id)

        except ValueError as error:
            errors.append(f"{label_path}: {error}")
            continue

        if len(classes) != 1:
            errors.append(
                f"{label_path}: ожидался один класс, найдено {classes}"
            )
            continue

        counts[classes.pop()] += 1

    total = sum(counts.values())

    return {
        "counts": counts,
        "total": total,
        "errors": errors,
        "listed": len(image_paths),
    }


def print_results(results):
    print("\n" + "=" * 76)
    print("РАСПРЕДЕЛЕНИЕ КЛАССОВ ПО ВЫБОРКАМ")
    print("=" * 76)

    header = (
        f"{'Выборка':<12}"
        f"{'Wheat':>12}"
        f"{'Tomato':>12}"
        f"{'Corn':>12}"
        f"{'Всего':>12}"
    )

    print(header)
    print("-" * 76)

    total_counts = Counter()
    total_images = 0

    for split_name, result in results.items():
        counts = result["counts"]
        total = result["total"]

        total_counts.update(counts)
        total_images += total

        print(
            f"{split_name:<12}"
            f"{counts[0]:>12}"
            f"{counts[1]:>12}"
            f"{counts[2]:>12}"
            f"{total:>12}"
        )

    print("-" * 76)

    print(
        f"{'Всего':<12}"
        f"{total_counts[0]:>12}"
        f"{total_counts[1]:>12}"
        f"{total_counts[2]:>12}"
        f"{total_images:>12}"
    )

    print("\n" + "=" * 76)
    print("СООТНОШЕНИЕ ВЫБОРОК")
    print("=" * 76)

    for split_name, result in results.items():
        total = result["total"]
        percentage = total / total_images * 100 if total_images else 0

        print(
            f"{split_name:<12}"
            f"{total:>8} изображений"
            f"{percentage:>10.2f}%"
        )

    print("\n" + "=" * 76)
    print("РАСПРЕДЕЛЕНИЕ КЛАССОВ ВО ВСЕМ ДАТАСЕТЕ")
    print("=" * 76)

    for class_id, class_name in CLASSES.items():
        count = total_counts[class_id]
        percentage = count / total_images * 100 if total_images else 0

        print(
            f"{class_name:<12}"
            f"{count:>8} изображений"
            f"{percentage:>10.2f}%"
        )

    print("\n" + "=" * 76)
    print("ПРОВЕРКА")
    print("=" * 76)

    has_errors = False

    for split_name, result in results.items():
        print(
            f"{split_name}: "
            f"в списке {result['listed']}, "
            f"успешно проверено {result['total']}, "
            f"ошибок {len(result['errors'])}"
        )

        if result["errors"]:
            has_errors = True
            for error in result["errors"]:
                print(f"  ОШИБКА: {error}")

    if not has_errors:
        print("Ошибок не обнаружено.")


def main():
    results = {}

    for split_name, list_file in SPLITS.items():
        results[split_name] = analyze_split(split_name, list_file)

    print_results(results)


if __name__ == "__main__":
    main()
