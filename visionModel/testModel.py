# (c) ggelado. Co-Authored by OpenAI ChatGPT Free. 14/05/2026
# Conversation ID 6a05e141-1ac0-8328-8a32-21abf1412fca

from ultralytics import YOLO
from pathlib import Path
import cv2
import os

# =========================
# CONFIG
# =========================

MODELS_DIR = "models"
TEST_DIR = "testDir"
OUTPUT_DIR = "output"

VALID_EXTENSIONS = (".jpg", ".jpeg", ".png", ".bmp", ".webp")

# =========================
# LOAD MODELS
# =========================

print("[INFO] Buscando modelos en carpeta models/...")

model_paths = [f for f in Path(MODELS_DIR).iterdir() if f.suffix.lower() == ".pt"]

if not model_paths:
    print("[ERROR] No se encontraron modelos en /models")
    exit()

models = {}

for m_path in model_paths:
    print(f"[INFO] Cargando modelo: {m_path.name}")
    models[m_path.name] = YOLO(str(m_path))

print(f"\n[INFO] Total modelos cargados: {len(models)}\n")

# =========================
# MOSTRAR CATEGORÍAS DE CADA MODELO
# =========================

print("\n==============================")
print(" MODELOS Y SUS CATEGORÍAS")
print("==============================\n")

for name, model in models.items():

    try:
        names = model.names  # dict id -> class name

        categories = [str(v) for v in names.values()]

        print(f"[MODEL] {name}")
        print(f"Classes ({len(categories)}):")
        for c in categories:
            print(" -", c)
        print()

    except Exception as e:
        print(f"[WARN] No se pudieron leer clases de {name}: {e}")

# =========================
# POSITIVE MAP
# =========================

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

# =========================
# NEGATIVE FILTER
# =========================

NEGATIVE_KEYWORDS = {"non", "no_", "not", "safe"}

# =========================
# OUTPUT SETUP
# =========================

os.makedirs(OUTPUT_DIR, exist_ok=True)

# =========================
# LOAD IMAGES
# =========================

test_path = Path(TEST_DIR)

images = [f for f in test_path.iterdir() if f.suffix.lower() in VALID_EXTENSIONS]

if not images:
    print("[INFO] No hay imágenes")
    exit()

# =========================
# INFERENCE
# =========================

for img_path in images:

    print("=" * 60)
    print(f"[IMAGE] {img_path.name}")

    image = cv2.imread(str(img_path))

    if image is None:
        print(f"[ERROR] No se pudo leer: {img_path}")
        continue

    detected_labels = set()
    triggers = []
    positives_report = []

    for model_name, model in models.items():

        results = model(str(img_path))
        result = results[0]

        # =========================
        # CLASSIFICATION
        # =========================

        if hasattr(result, "probs") and result.probs is not None:

            top1 = result.probs.top1
            conf = float(result.probs.top1conf.item())
            class_name = result.names[top1].lower().strip()

            if any(n in class_name for n in NEGATIVE_KEYWORDS):
                continue

            if class_name in POSITIVE_MAP.get(model_name, set()):
                detected_labels.add(class_name)
                triggers.append(f"{model_name}:{class_name}")
                positives_report.append(f"{class_name} ({conf:.2f})")

        # =========================
        # DETECTION
        # =========================

        elif hasattr(result, "boxes") and result.boxes is not None:

            for box in result.boxes:

                cls_id = int(box.cls[0])
                conf = float(box.conf[0])
                class_name = result.names[cls_id].lower().strip()

                if any(n in class_name for n in NEGATIVE_KEYWORDS):
                    continue

                if class_name in POSITIVE_MAP.get(model_name, set()):
                    detected_labels.add(class_name)
                    triggers.append(f"{model_name}:{class_name}")
                    positives_report.append(f"{class_name} ({conf:.2f})")

                # dibujo
                x1, y1, x2, y2 = map(int, box.xyxy[0])

                cv2.rectangle(image, (x1, y1), (x2, y2), (0, 0, 255), 2)

                cv2.putText(
                    image,
                    f"{class_name} {conf:.2f}",
                    (x1, y1 - 10),
                    cv2.FONT_HERSHEY_SIMPLEX,
                    0.6,
                    (0, 0, 255),
                    2,
                )

    # =========================
    # SAFE LOGIC
    # =========================

    is_safe = len(detected_labels) == 0

    if is_safe:
        detected_labels.add("safe")

    for label in detected_labels:

        folder = os.path.join(OUTPUT_DIR, label)
        os.makedirs(folder, exist_ok=True)

        out_path = os.path.join(folder, img_path.name)
        cv2.imwrite(out_path, image)

        print(f"[SAVED] {out_path}")

    print("\n[RESULT SUMMARY]")

    if is_safe:
        print("SAFE IMAGE")
    else:
        print("POSITIVE DETECTIONS:")
        for p in positives_report:
            print(" -", p)

        print(f"[TRIGGERS] {triggers}")

print("\n[INFO] Test finalizado")
