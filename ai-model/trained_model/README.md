# Trained Model Artifacts

This directory contains the legacy exported YOLOv8n TFLite artifact plus documentation for the current tuned baseline.

## Current Baseline

- **Current tuned weights**: `../training_runs/yolov8n_recall_a/weights/best.pt`
- **Best checkpoint validation metrics**: mAP@50 `81.45%`, mAP@50-95 `52.13%`, precision `79.98%`, recall `75.90%`
- **Recommended operating point**: confidence `0.30`, NMS IoU `0.45`

## Download Instructions

**Download these files from Google Drive** (`pothole_model/` folder):

### Required Files
1. ✅ `best_float16.tflite` (~6MB)
   - **This is the main model file for Android**
   - Float16 quantized TensorFlow Lite format
   - Copy to: `android/app/src/main/assets/best_float16.tflite`

### Documentation Files  
2. ✅ `results.csv`
   - Training metrics for each epoch
   - Use for analysis and reporting

3. ✅ `results.png`
   - Training curves visualization
   - Include in project report

4. ✅ `confusion_matrix.png`
   - Model evaluation visualization
   - Include in project report

### Backup Files (Optional)
5. ⭕ `best.pt`
   - PyTorch weights
   - Keep for future fine-tuning
   - Size: ~12MB

6. ⭕ `last.pt`
   - Final epoch checkpoint
   - Backup only

## File Organization

After downloading, your structure should be:

```
ai-model/trained_model/
├── best_float16.tflite      ← For Android deployment
├── best.pt                  ← For future retraining
├── results.csv              ← Training data
├── results.png              ← Training visualization
├── confusion_matrix.png     ← Evaluation visualization
├── TRAINING_METRICS.md      ← Metrics documentation
└── README.md                ← This file
```

## Model Specifications

- **Input**: 640×640×3 RGB image, float32, normalized [0-1]
- **Output**: [1, 5, 8400] tensor (class + bbox)
- **Legacy export accuracy reference**: superseded by the tuned baseline in `../training_runs/yolov8n_recall_a/`
- **Size**: ~6MB
- **Format**: TensorFlow Lite (float16)

## Quick Verification

After downloading `best_float16.tflite`:

```bash
# Check file size (should be ~6MB)
ls -lh best_float16.tflite

# Expected output: ~5-7 MB
```

## Next Steps

1. ✅ Download all files to this directory
2. ✅ Verify `best_float16.tflite` size (~6MB)
3. ➡️  Copy `.tflite` to Android project
4. ➡️  Start Android app development

## Support

See `TRAINING_METRICS.md` for detailed performance analysis and the tuned baseline summary.
