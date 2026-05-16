from __future__ import annotations

import argparse
import json
import shutil
import tempfile
import urllib.request
from pathlib import Path
from urllib.parse import urlparse

from ultralytics import YOLO


BASE_DIR = Path(__file__).resolve().parent
MODELS_DIR = BASE_DIR / "models"

NEGATIVE_KEYWORDS = {"non", "no_", "not", "safe"}

POSITIVE_MAP = {
    "gore.pt": {"blood"},
    "nazi.pt": {"nazi-symbol"},
    "violence.pt": {"violence"},
    "fight.pt": {"violence", "fight"},
    "arms.pt": {"gun"},
    "arms2.pt": {
        "ak47",
        "m4a1-s",
        "m4a1",
        "galil",
        "famas",
        "tec-9",
        "five-seven",
        "glock-18",
        "usp-s",
        "eagle",
        "berettas",
        "p2000",
        "mac10",
        "mp5",
        "mp9",
        "p90",
        "p250",
        "ssg08",
        "awp",
    },
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Run the vision safety models on one image")
    source = parser.add_mutually_exclusive_group(required=True)
    source.add_argument("--image", help="Local image path")
    source.add_argument("--url", help="Image URL")
    return parser.parse_args()


def download_url(url: str) -> Path:
    suffix = Path(urlparse(url).path).suffix or ".jpg"
    temp_file = tempfile.NamedTemporaryFile(delete=False, suffix=suffix)
    temp_file.close()

    with urllib.request.urlopen(url) as response, open(temp_file.name, "wb") as output:
        shutil.copyfileobj(response, output)

    return Path(temp_file.name)


def load_models() -> dict[str, YOLO]:
    model_paths = sorted(path for path in MODELS_DIR.iterdir() if path.suffix.lower() == ".pt")
    if not model_paths:
        raise SystemExit(f"No se encontraron modelos .pt en {MODELS_DIR}")

    models: dict[str, YOLO] = {}
    for model_path in model_paths:
        models[model_path.name] = YOLO(str(model_path))

    return models


def analyze_image(image_path: Path, models: dict[str, YOLO]) -> dict:
    detected_labels: set[str] = set()
    triggers: list[str] = []
    positives_report: list[str] = []

    for model_name, model in models.items():
        results = model(str(image_path))
        result = results[0]

        if hasattr(result, "probs") and result.probs is not None:
            top1 = result.probs.top1
            confidence = float(result.probs.top1conf.item())
            class_name = result.names[top1].lower().strip()

            if any(keyword in class_name for keyword in NEGATIVE_KEYWORDS):
                continue

            if class_name in POSITIVE_MAP.get(model_name, set()):
                detected_labels.add(class_name)
                triggers.append(f"{model_name}:{class_name}")
                positives_report.append(f"{class_name} ({confidence:.2f})")

        elif hasattr(result, "boxes") and result.boxes is not None:
            for box in result.boxes:
                class_id = int(box.cls[0])
                confidence = float(box.conf[0])
                class_name = result.names[class_id].lower().strip()

                if any(keyword in class_name for keyword in NEGATIVE_KEYWORDS):
                    continue

                if class_name in POSITIVE_MAP.get(model_name, set()):
                    detected_labels.add(class_name)
                    triggers.append(f"{model_name}:{class_name}")
                    positives_report.append(f"{class_name} ({confidence:.2f})")

    is_safe = len(detected_labels) == 0

    return {
        "safe": is_safe,
        "detections": sorted(detected_labels),
        "positives": positives_report,
        "triggers": triggers,
        "image": str(image_path),
    }


def main() -> None:
    args = parse_args()
    models = load_models()

    temp_path: Path | None = None
    try:
        if args.url:
            temp_path = download_url(args.url)
            image_path = temp_path
            source = args.url
        else:
            image_path = Path(args.image).expanduser().resolve()
            source = str(image_path)

        if not image_path.exists():
            raise SystemExit(f"No se pudo encontrar la imagen: {source}")

        result = analyze_image(image_path, models)
        result["source"] = source

        print(json.dumps(result, ensure_ascii=False))
    finally:
        if temp_path is not None:
            temp_path.unlink(missing_ok=True)


if __name__ == "__main__":
    main()