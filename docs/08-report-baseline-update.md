# Report Baseline Update

This addendum records the new tuned YOLOv8n baseline that supersedes the earlier training result used in the written report draft.

## New Baseline

- **Checkpoint**: `ai-model/training_runs/yolov8n_recall_a/weights/best.pt`
- **Training setup**: YOLOv8n, 640 image size, 160 epochs, AdamW, cosine LR, reduced mosaic, no mixup
- **Best checkpoint metrics**:
  - mAP@50: **81.45%**
  - mAP@50-95: **52.13%**
  - Precision: **79.98%**
  - Recall: **75.90%**

## Threshold Sweep Recommendation

- **Recommended operating point**: confidence `0.30`, NMS IoU `0.45`
- **Operating-point metrics**:
  - Precision: **77.41%**
  - Recall: **77.02%**
  - F1: **77.38%**

## Difference From Previous Baseline

- Previous baseline recall: **72.04%**
- New tuned baseline recall: **75.90%**
- Gain in checkpoint recall: **+3.86 points**
- Precision dropped only modestly, which matches the project goal of improving recall without a major precision collapse.

## Suggested Report Wording

The final adopted detector is a recall-tuned YOLOv8n baseline trained on the merged pothole dataset. Compared with the earlier baseline, the tuned model improved recall from 72.04% to 75.90% while maintaining precision near 80%. A post-training threshold sweep identified confidence 0.30 and NMS IoU 0.45 as the best deployment trade-off, yielding 77.41% precision and 77.02% recall.
