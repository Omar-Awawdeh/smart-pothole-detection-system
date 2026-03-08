import argparse
from pathlib import Path

from ultralytics import YOLO


DEFAULT_PROJECT = "training_runs"
DEFAULT_RUN_NAME = "yolov8n_recall_tuned"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Train YOLOv8n with defaults biased slightly toward better recall."
    )
    parser.add_argument("--data", default="data.yaml", help="Path to YOLO data.yaml")
    parser.add_argument("--weights", default="yolov8n.pt", help="Starting weights")
    parser.add_argument("--epochs", type=int, default=160, help="Training epochs")
    parser.add_argument("--imgsz", type=int, default=640, help="Training image size")
    parser.add_argument("--batch", type=int, default=16, help="Batch size")
    parser.add_argument("--patience", type=int, default=40, help="Early stopping patience")
    parser.add_argument("--lr0", type=float, default=0.005, help="Initial learning rate")
    parser.add_argument("--weight-decay", type=float, default=0.0005, help="Weight decay")
    parser.add_argument("--optimizer", default="AdamW", help="Optimizer name")
    parser.add_argument("--device", default=None, help="Device override, e.g. 0 or cpu")
    parser.add_argument("--project", default=DEFAULT_PROJECT, help="Output project directory")
    parser.add_argument("--name", default=DEFAULT_RUN_NAME, help="Run name")
    parser.add_argument("--workers", type=int, default=8, help="Dataloader workers")
    parser.add_argument("--seed", type=int, default=42, help="Training seed")
    parser.add_argument("--close-mosaic", type=int, default=10, help="Disable mosaic in final epochs")
    parser.add_argument("--mosaic", type=float, default=0.5, help="Mosaic probability")
    parser.add_argument("--mixup", type=float, default=0.0, help="Mixup probability")
    parser.add_argument("--copy-paste", type=float, default=0.0, help="Copy-paste probability")
    parser.add_argument("--degrees", type=float, default=7.5, help="Rotation augmentation")
    parser.add_argument("--translate", type=float, default=0.08, help="Translation augmentation")
    parser.add_argument("--scale", type=float, default=0.4, help="Scale augmentation")
    parser.add_argument("--fliplr", type=float, default=0.5, help="Horizontal flip probability")
    parser.add_argument("--hsv-h", type=float, default=0.015, help="HSV hue augmentation")
    parser.add_argument("--hsv-s", type=float, default=0.6, help="HSV saturation augmentation")
    parser.add_argument("--hsv-v", type=float, default=0.4, help="HSV value augmentation")
    parser.add_argument("--cos-lr", action="store_true", help="Use cosine LR schedule")
    parser.add_argument("--cache", default=False, action="store_true", help="Cache images in RAM")
    parser.add_argument("--resume", action="store_true", help="Resume the latest checkpoint")
    return parser.parse_args()


def main() -> None:
    args = parse_args()

    data_path = Path(args.data)
    if not data_path.is_absolute():
        data_path = Path(__file__).resolve().parent / data_path

    model = YOLO(args.weights)
    results = model.train(
        data=str(data_path),
        epochs=args.epochs,
        imgsz=args.imgsz,
        batch=args.batch,
        patience=args.patience,
        optimizer=args.optimizer,
        lr0=args.lr0,
        weight_decay=args.weight_decay,
        project=args.project,
        name=args.name,
        workers=args.workers,
        seed=args.seed,
        close_mosaic=args.close_mosaic,
        mosaic=args.mosaic,
        mixup=args.mixup,
        copy_paste=args.copy_paste,
        degrees=args.degrees,
        translate=args.translate,
        scale=args.scale,
        fliplr=args.fliplr,
        hsv_h=args.hsv_h,
        hsv_s=args.hsv_s,
        hsv_v=args.hsv_v,
        cos_lr=args.cos_lr,
        cache=args.cache,
        resume=args.resume,
        device=args.device,
        pretrained=True,
        plots=True,
        val=True,
        save=True,
    )

    metrics = model.val(data=str(data_path), split="test")

    print("Training complete")
    print(f"Run directory: {results.save_dir}")
    print("Test metrics:")
    print(f"  Precision: {metrics.box.mp:.4f}")
    print(f"  Recall:    {metrics.box.mr:.4f}")
    print(f"  mAP@50:    {metrics.box.map50:.4f}")
    print(f"  mAP@50-95: {metrics.box.map:.4f}")


if __name__ == "__main__":
    main()
