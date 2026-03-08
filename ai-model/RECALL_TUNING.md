# Recall Tuning Guide

This project already has a solid YOLOv8n baseline, but recall is still the weakest metric.

Current tuned baseline from `training_runs/yolov8n_recall_a/results.csv` and the threshold sweep:
- Best checkpoint precision: 79.98%
- Best checkpoint recall: 75.90%
- Best checkpoint mAP@50: 81.45%
- Recommended operating point: confidence 0.30, NMS IoU 0.45
- Operating-point precision / recall: 77.41% / 77.02%

Since `YOLOv8s` is too heavy for your constraint, the best path is to tune `YOLOv8n` for better recall with a controlled precision drop.

## Recommended Strategy

1. Improve the training recipe before changing the architecture.
2. Sweep confidence and NMS thresholds on the test set.
3. Retrain with recall-oriented defaults.
4. Add hard examples and fix bad labels.

## What Usually Helps Recall Most

- Add more hard data:
  - small potholes
  - distant potholes
  - low light and shadows
  - motion blur
  - wet roads and glare
  - visually confusing non-pothole road damage
- Clean labels carefully:
  - missing boxes directly reduce recall
  - boxes that are too tight or too loose make matching harder
- Train longer and stop based on metrics, not only the last epoch.
- Reduce augmentation settings that may distort small potholes too much.

## Included Scripts

### 1) Train a recall-tuned YOLOv8n

```bash
python train.py --cos-lr
```

Default changes compared with the original notebook:
- epochs: 160
- patience: 40
- lr0: 0.005
- mosaic: 0.5 instead of 1.0
- mixup: 0.0 instead of 0.1
- close_mosaic: 10

These defaults aim to keep generalization while making training less aggressive on small objects.
### 2) Sweep thresholds on the held-out set

```bash
python sweep_thresholds.py --model training_runs/yolov8n_recall_tuned/weights/best.pt
```

This reports precision, recall, and F1 across multiple confidence and NMS thresholds.

## Practical Experiment Order

### Experiment A: Better checkpoint and threshold selection

Run:

```bash
python train.py --name yolov8n_recall_a --cos-lr
python sweep_thresholds.py --model training_runs/yolov8n_recall_a/weights/best.pt
```

Goal:
- Get a better operating point without changing model size.

### Experiment B: Slightly larger training resolution

Run:

```bash
python train.py --name yolov8n_recall_b --imgsz 704 --batch 12 --cos-lr
```

Goal:
- Improve detection of smaller potholes.

If this is too slow or unstable, stay on `640`.

### Experiment C: More conservative augmentation

Run:

```bash
python train.py --name yolov8n_recall_c --mosaic 0.3 --mixup 0.0 --degrees 5 --translate 0.06 --scale 0.3 --cos-lr
```

Goal:
- Reduce unrealistic synthetic samples that can hurt localization.

## Label and Data Review Checklist

Look at false negatives first. If many misses share the same pattern, fix that pattern in the data.

Focus on:
- tiny potholes
- partial potholes near image edges
- multiple potholes close together
- dark scenes
- potholes with weak texture contrast

## Recommended Success Criteria

Given your constraint, a good next target is:
- Recall: 76-80%
- Precision: stay at or above 75%

That would be a meaningful recall gain without a major precision collapse.
