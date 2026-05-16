from __future__ import annotations

import argparse
import json
import shutil
import sys
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
    parser.add_argument("--url", required=True, help="Image URL (http:// or https://)")
    return parser.parse_args()


def download_url(url: str) -> Path:
    parsed = urlparse(url)
    suffix = Path(parsed.path).suffix or ".jpg"
    if parsed.scheme not in ("http", "https"):
        raise ValueError("URL must start with http:// or https://")

    temp_file = tempfile.NamedTemporaryFile(delete=False, suffix=suffix)
    temp_file.close()

    with urllib.request.urlopen(url, timeout=15) as response, open(temp_file.name, "wb") as output:
        shutil.copyfileobj(response, output)

    return Path(temp_file.name)


def load_models():
    model_paths = sorted(path for path in MODELS_DIR.iterdir() if path.suffix.lower() == ".pt")
    if not model_paths:
        raise SystemExit(f"No se encontraron modelos .pt en {MODELS_DIR}")

    models = {}
    for model_path in model_paths:
        models[model_path.name] = YOLO(str(model_path))

    return models


def analyze_image(image_path: Path, models: dict) -> dict:
    detected_labels = set()
    triggers = []
    positives_report = []

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
        "triggers": triggers
    }


def main():
    args = parse_args()
    temp_path = None
    try:
        temp_path = download_url(args.url)
        image_path = temp_path
        source = args.url

        models = load_models()

        if not image_path.exists():
            raise RuntimeError(f"No se pudo encontrar la imagen: {source}")

        result = analyze_image(image_path, models)
        result["source"] = source
        print(json.dumps(result, ensure_ascii=False))

    except Exception as e:
        print(json.dumps({"error": str(e), "source": getattr(args, "url", None)}), flush=True)
        sys.exit(1)

    finally:
        if temp_path is not None:
            try:
                temp_path.unlink(missing_ok=True)
            except Exception:
                pass


if __name__ == "__main__":
    main()
