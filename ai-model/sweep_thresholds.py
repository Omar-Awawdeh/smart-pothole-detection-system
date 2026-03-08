import argparse
import json
from dataclasses import dataclass
from pathlib import Path

import cv2
import yaml
from ultralytics import YOLO


IMAGE_EXTENSIONS = {".jpg", ".jpeg", ".png", ".bmp", ".webp"}


@dataclass
class Box:
    x1: float
    y1: float
    x2: float
    y2: float
    score: float = 1.0


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Sweep confidence and NMS thresholds to find a better recall/precision trade-off."
    )
    parser.add_argument("--model", required=True, help="Path to YOLO weights, e.g. best.pt")
    parser.add_argument("--data", default="data.yaml", help="Path to YOLO data.yaml")
    parser.add_argument("--split", default="test", choices=["train", "val", "test"], help="Dataset split")
    parser.add_argument("--imgsz", type=int, default=640, help="Inference image size")
    parser.add_argument("--min-conf", type=float, default=0.20, help="Lowest confidence threshold")
    parser.add_argument("--max-conf", type=float, default=0.60, help="Highest confidence threshold")
    parser.add_argument("--conf-step", type=float, default=0.05, help="Confidence step")
    parser.add_argument("--min-iou", type=float, default=0.45, help="Lowest NMS IoU threshold")
    parser.add_argument("--max-iou", type=float, default=0.75, help="Highest NMS IoU threshold")
    parser.add_argument("--iou-step", type=float, default=0.05, help="NMS IoU step")
    parser.add_argument("--match-iou", type=float, default=0.50, help="IoU used to match predictions to labels")
    parser.add_argument("--device", default=None, help="Device override, e.g. 0 or cpu")
    parser.add_argument("--limit", type=int, default=None, help="Optional max number of images to evaluate")
    parser.add_argument("--output", default="threshold_sweep_results.json", help="Output JSON path")
    return parser.parse_args()


def load_data_paths(data_path: Path, split: str) -> tuple[Path, list[Path]]:
    with data_path.open("r", encoding="utf-8") as handle:
        config = yaml.safe_load(handle)

    root = Path(config["path"])
    if not root.is_absolute():
        root = (data_path.parent / root).resolve()

    split_key = "val" if split == "val" else split
    image_dir = (root / config[split_key]).resolve()
    image_paths = sorted(
        path for path in image_dir.iterdir() if path.suffix.lower() in IMAGE_EXTENSIONS
    )
    return root, image_paths


def label_path_for(image_path: Path) -> Path:
    return image_path.parent.parent / "labels" / f"{image_path.stem}.txt"


def read_image_shape(image_path: Path) -> tuple[int, int]:
    image = cv2.imread(str(image_path))
    if image is None:
        raise RuntimeError(f"Failed to load image: {image_path}")
    height, width = image.shape[:2]
    return width, height


def load_ground_truth(image_path: Path) -> list[Box]:
    label_path = label_path_for(image_path)
    if not label_path.exists():
        return []

    width, height = read_image_shape(image_path)
    boxes = []
    with label_path.open("r", encoding="utf-8") as handle:
        for raw_line in handle:
            parts = raw_line.strip().split()
            if len(parts) != 5:
                continue
            _, x_center, y_center, box_width, box_height = map(float, parts)
            half_width = box_width * width / 2.0
            half_height = box_height * height / 2.0
            x_center_px = x_center * width
            y_center_px = y_center * height
            boxes.append(
                Box(
                    x1=x_center_px - half_width,
                    y1=y_center_px - half_height,
                    x2=x_center_px + half_width,
                    y2=y_center_px + half_height,
                )
            )
    return boxes


def compute_iou(a: Box, b: Box) -> float:
    x1 = max(a.x1, b.x1)
    y1 = max(a.y1, b.y1)
    x2 = min(a.x2, b.x2)
    y2 = min(a.y2, b.y2)

    inter_w = max(0.0, x2 - x1)
    inter_h = max(0.0, y2 - y1)
    inter_area = inter_w * inter_h

    area_a = max(0.0, a.x2 - a.x1) * max(0.0, a.y2 - a.y1)
    area_b = max(0.0, b.x2 - b.x1) * max(0.0, b.y2 - b.y1)
    union = area_a + area_b - inter_area
    if union <= 0.0:
        return 0.0
    return inter_area / union


def evaluate_predictions(
    predictions: list[Box], ground_truth: list[Box], match_iou: float
) -> tuple[int, int, int]:
    matched_gt = set()
    true_positives = 0
    false_positives = 0

    sorted_predictions = sorted(predictions, key=lambda box: box.score, reverse=True)

    for prediction in sorted_predictions:
        best_iou = 0.0
        best_index = None
        for gt_index, gt_box in enumerate(ground_truth):
            if gt_index in matched_gt:
                continue
            iou = compute_iou(prediction, gt_box)
            if iou > best_iou:
                best_iou = iou
                best_index = gt_index

        if best_index is not None and best_iou >= match_iou:
            matched_gt.add(best_index)
            true_positives += 1
        else:
            false_positives += 1

    false_negatives = len(ground_truth) - len(matched_gt)
    return true_positives, false_positives, false_negatives


def frange(start: float, stop: float, step: float) -> list[float]:
    values = []
    current = start
    while current <= stop + 1e-9:
        values.append(round(current, 4))
        current += step
    return values


def main() -> None:
    args = parse_args()

    model_path = Path(args.model).resolve()
    data_path = Path(args.data)
    if not data_path.is_absolute():
        data_path = (Path(__file__).resolve().parent / data_path).resolve()

    _, image_paths = load_data_paths(data_path, args.split)
    if args.limit is not None:
        image_paths = image_paths[: args.limit]

    if not image_paths:
        raise RuntimeError("No images found for the requested split")

    labels_by_image = {image_path: load_ground_truth(image_path) for image_path in image_paths}
    model = YOLO(str(model_path))

    all_results = []
    best_result = None

    conf_values = frange(args.min_conf, args.max_conf, args.conf_step)
    iou_values = frange(args.min_iou, args.max_iou, args.iou_step)

    for conf in conf_values:
        for iou in iou_values:
            tp = 0
            fp = 0
            fn = 0

            predictions = model.predict(
                source=[str(path) for path in image_paths],
                conf=conf,
                iou=iou,
                imgsz=args.imgsz,
                verbose=False,
                device=args.device,
                stream=False,
            )

            for image_path, result in zip(image_paths, predictions):
                predicted_boxes = []
                if result.boxes is not None:
                    xyxy = result.boxes.xyxy.cpu().tolist()
                    scores = result.boxes.conf.cpu().tolist()
                    predicted_boxes = [
                        Box(x1=box[0], y1=box[1], x2=box[2], y2=box[3], score=score)
                        for box, score in zip(xyxy, scores)
                    ]

                image_tp, image_fp, image_fn = evaluate_predictions(
                    predicted_boxes,
                    labels_by_image[image_path],
                    match_iou=args.match_iou,
                )
                tp += image_tp
                fp += image_fp
                fn += image_fn

            precision = tp / (tp + fp) if (tp + fp) else 0.0
            recall = tp / (tp + fn) if (tp + fn) else 0.0
            f1 = (2.0 * precision * recall / (precision + recall)) if (precision + recall) else 0.0

            record = {
                "conf": conf,
                "nms_iou": iou,
                "precision": precision,
                "recall": recall,
                "f1": f1,
                "tp": tp,
                "fp": fp,
                "fn": fn,
                "images": len(image_paths),
            }
            all_results.append(record)

            if best_result is None or record["f1"] > best_result["f1"]:
                best_result = record

            print(
                f"conf={conf:.2f} iou={iou:.2f} "
                f"precision={precision:.4f} recall={recall:.4f} f1={f1:.4f}"
            )

    output_path = Path(args.output)
    if not output_path.is_absolute():
        output_path = (Path.cwd() / output_path).resolve()

    with output_path.open("w", encoding="utf-8") as handle:
        json.dump({"best": best_result, "results": all_results}, handle, indent=2)

    print("Best setting:")
    print(json.dumps(best_result, indent=2))
    print(f"Saved sweep results to {output_path}")


if __name__ == "__main__":
    main()
