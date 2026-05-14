# (c) ggelado. Co-Authored by OpenAI ChatGPT Free. 14/05/2026 
# Conversation ID 6a05e141-1ac0-8328-8a32-21abf1412fca

from ultralytics import YOLO
from pathlib import Path
import cv2
import os

MODEL_PATH = "best.pt"
TEST_DIR = "testDir"
OUTPUT_DIR = "output"

VALID_EXTENSIONS = (".jpg", ".jpeg", ".png", ".bmp", ".webp")

print("[INFO] Cargando modelo...")
model = YOLO(MODEL_PATH)

print(f"[INFO] Tipo de modelo: {model.task}")
print("[INFO] Modelo cargado correctamente\n")

os.makedirs(OUTPUT_DIR, exist_ok=True)

test_path = Path(TEST_DIR)

images = [
    f for f in test_path.iterdir()
    if f.suffix.lower() in VALID_EXTENSIONS
]

for img_path in images:

    print("=" * 60)
    print(f"[IMAGE] {img_path.name}")

    image = cv2.imread(str(img_path))

    results = model(str(img_path))

    result = results[0]

    if result.probs is not None:

        top1_index = result.probs.top1
        top1_conf = result.probs.top1conf.item()

        class_name = result.names[top1_index]

        print(f"[RESULT] {class_name}")
        print(f"[CONF]   {top1_conf:.4f}")

        # Texto sobre imagen
        label = f"{class_name} {top1_conf:.2f}"

        color = (0, 0, 255)

        cv2.putText(
            image,
            label,
            (20, 40),
            cv2.FONT_HERSHEY_SIMPLEX,
            1,
            color,
            2
        )

    output_path = os.path.join(
        OUTPUT_DIR,
        f"classified_{img_path.name}"
    )

    cv2.imwrite(output_path, image)

    cv2.imshow("Classification", image)

    cv2.waitKey(0)

cv2.destroyAllWindows()

print("\n[INFO] Test finalizado")