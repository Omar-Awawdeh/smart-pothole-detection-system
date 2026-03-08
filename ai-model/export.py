import argparse
import shutil
from pathlib import Path

from ultralytics import YOLO


DEFAULT_WEIGHTS = "training_runs/yolov8n_recall_a/weights/best.pt"
DEFAULT_OUTPUT_DIR = "trained_model"
DEFAULT_OUTPUT_NAME = "best_float16.tflite"
DEFAULT_ANDROID_ASSET = "../android/app/src/main/assets/best_float16.tflite"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Export the tuned YOLOv8n checkpoint to float16 TFLite."
    )
    parser.add_argument("--weights", default=DEFAULT_WEIGHTS, help="Path to source .pt weights")
    parser.add_argument("--imgsz", type=int, default=640, help="Export image size")
    parser.add_argument("--output-dir", default=DEFAULT_OUTPUT_DIR, help="Directory for exported model")
    parser.add_argument("--output-name", default=DEFAULT_OUTPUT_NAME, help="Final TFLite filename")
    parser.add_argument(
        "--android-asset",
        default=DEFAULT_ANDROID_ASSET,
        help="Optional Android asset destination; use empty string to skip",
    )
    return parser.parse_args()


def resolve_path(raw_path: str, base_dir: Path) -> Path:
    path = Path(raw_path)
    if not path.is_absolute():
        path = (base_dir / path).resolve()
    return path


def main() -> None:
    args = parse_args()
    base_dir = Path(__file__).resolve().parent

    weights_path = resolve_path(args.weights, base_dir)
    output_dir = resolve_path(args.output_dir, base_dir)
    output_dir.mkdir(parents=True, exist_ok=True)
    output_path = output_dir / args.output_name

    if not weights_path.exists():
        raise FileNotFoundError(f"Weights file not found: {weights_path}")

    model = YOLO(str(weights_path))
    exported_path = Path(model.export(format="tflite", imgsz=args.imgsz, half=True, int8=False))
    if not exported_path.is_absolute():
        exported_path = (Path.cwd() / exported_path).resolve()

    if not exported_path.exists():
        raise FileNotFoundError(f"Exported TFLite file not found: {exported_path}")

    shutil.copy2(exported_path, output_path)
    print(f"Saved TFLite model to {output_path}")

    if args.android_asset:
        android_asset_path = resolve_path(args.android_asset, base_dir)
        android_asset_path.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(output_path, android_asset_path)
        print(f"Updated Android asset at {android_asset_path}")


if __name__ == "__main__":
    main()
