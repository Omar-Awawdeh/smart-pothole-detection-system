# AI Model Training Plan

**Owner**: Omar  
**Duration**: Week 1 (Days 1-7)  
**Output**: `yolov8n_pothole_float16.tflite` (~6MB)

---

## Objective

Train a YOLOv8n object detection model to identify potholes in road images, then export to TensorFlow Lite format optimized for on-device mobile inference with NPU/GPU acceleration.

---

## Model Selection Analysis

### Comparison of YOLO Variants

| Model | Size | mAP@50 (est.) | Mobile FPS | TFLite Support | Status |
|-------|------|---------------|------------|----------------|--------|
| **YOLOv8n** | 6.3 MB | ~82% | 25-30 | Excellent | **SELECTED** |
| YOLOv8s | 22 MB | ~85% | 15-20 | Good | Backup option |
| YOLO11n | 5.4 MB | ~83% | 25-30 | Good | Alternative |
| YOLOv10n | 5.8 MB | ~81% | 20-25 | NMS issues | Not recommended |
| YOLOv5n | 3.9 MB | ~78% | 30+ | Excellent | If size critical |

### Why YOLOv8n?

1. **Mature ecosystem**: Best documented export pipeline to TFLite
2. **Balanced performance**: Good accuracy without sacrificing speed
3. **Proven mobile deployment**: Many production examples
4. **Active community**: Easy to find solutions to problems
5. **Size constraint**: 6MB fits comfortably in mobile app

### Fallback Strategy

If YOLOv8n accuracy is below 75% mAP@50:
1. First try: Add more training data (especially local Jordan roads)
2. Second try: Switch to YOLOv8s (larger but more accurate)
3. Last resort: Use YOLOv5n (faster, simpler, but less accurate)

---

## Dataset Strategy

### Phase 1: Gather Existing Datasets

| Source | Images | Format | Quality | Download |
|--------|--------|--------|---------|----------|
| Roboflow Pothole | 665 | YOLO | High | [Link](https://public.roboflow.com/object-detection/pothole) |
| Kaggle Pothole Detection | 1,500+ | YOLO | Good | [Link](https://www.kaggle.com/datasets/anggadwisunarto/potholes-detection-yolov8) |
| Road Damage Dataset (RDD) | 2,000+ | VOC | Mixed | [GitHub](https://github.com/sekilab/RoadDamageDetector) |
| Pothole Detection Dataset | 800+ | YOLO | Good | [Kaggle](https://www.kaggle.com/datasets/atulyakumar98/pothole-detection-dataset) |

**Note**: RDD dataset contains multiple classes (cracks, potholes, etc.) - filter for potholes only.

### Phase 2: Collect Local Data (SKIPPED)

**Note**: This phase has been skipped as per the updated plan. We will rely entirely on existing public datasets (Roboflow, Kaggle, RDD) to train the model. The importance of local data is noted for future improvements, but for the initial prototype, we will proceed with the gathered datasets.

### Phase 3: Data Labeling (SKIPPED)

**Note**: Since we are using pre-labeled public datasets, manual labeling is not required for this phase. Any unlabeled data found in the public datasets (like RDD raw images) will be filtered out or auto-labeled if necessary, but the primary strategy is to use the existing YOLO-format labels.

### Phase 4: Dataset Preparation

**Final Dataset Structure**:

```
datasets/
└── pothole_combined/
    ├── train/
    │   ├── images/
    │   │   ├── img_00001.jpg
    │   │   ├── img_00002.jpg
    │   │   └── ... (~2,800 images)
    │   └── labels/
    │       ├── img_00001.txt
    │       ├── img_00002.txt
    │       └── ...
    ├── valid/
    │   ├── images/     (~500 images)
    │   └── labels/
    ├── test/
    │   ├── images/     (~200 images)
    │   └── labels/
    └── data.yaml
```

**YOLO Label Format** (each .txt file):

```
# Format: class_id x_center y_center width height
# All values normalized to 0-1

# Example: img_00001.txt
0 0.456 0.623 0.120 0.089
0 0.234 0.445 0.095 0.072
```

**data.yaml Configuration**:

```yaml
# Dataset configuration for YOLOv8 training

path: /content/datasets/pothole_combined  # Root path (Colab)
train: train/images
val: valid/images
test: test/images

# Class configuration
nc: 1  # Number of classes
names: ['pothole']  # Class names

# Optional: Download script
# download: https://your-dataset-url.zip
```

### Dataset Split Strategy

```
Total target: ~3,500 images

Split ratio:
├── Train: 80% (~2,800 images)
├── Valid: 15% (~525 images)  
└── Test:  5%  (~175 images)

Important: 
- Keep images from same location/video in same split
- Don't let similar images appear in both train and valid
- Test set should be completely unseen conditions if possible
```

---

## Training Process

### Environment: Google Colab

**Why Colab**:
- Free GPU access (T4 16GB)
- No local setup required
- Easy to share and reproduce
- Google Drive integration for saving models

**Colab Setup Checklist**:

```
□ Runtime → Change runtime type → GPU (T4)
□ Mount Google Drive for persistent storage
□ Verify GPU: !nvidia-smi
□ Install ultralytics: !pip install ultralytics
□ Check CUDA: import torch; print(torch.cuda.is_available())
```

### Training Pipeline Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                     COLAB TRAINING PIPELINE                      │
│                                                                  │
│  Step 1: Setup Environment                                       │
│  ├── Install dependencies (ultralytics, etc.)                   │
│  ├── Mount Google Drive                                          │
│  └── Verify GPU availability                                     │
│                                                                  │
│  Step 2: Prepare Dataset                                         │
│  ├── Download/upload dataset to Colab                           │
│  ├── Verify directory structure                                  │
│  ├── Check data.yaml paths                                       │
│  └── Preview sample images with labels                          │
│                                                                  │
│  Step 3: Configure Training                                      │
│  ├── Load pretrained YOLOv8n weights                            │
│  ├── Set hyperparameters                                         │
│  └── Configure augmentation                                      │
│                                                                  │
│  Step 4: Train Model                                             │
│  ├── Start training (100 epochs)                                │
│  ├── Monitor loss curves                                         │
│  ├── Early stopping if no improvement                           │
│  └── Save best weights to Drive                                  │
│                                                                  │
│  Step 5: Evaluate Model                                          │
│  ├── Run validation metrics                                      │
│  ├── Check mAP@50 > 80%                                         │
│  ├── Review confusion matrix                                     │
│  └── Test on sample images                                       │
│                                                                  │
│  Step 6: Export to TFLite                                        │
│  ├── Export with float16 quantization                           │
│  ├── Verify output shape                                         │
│  ├── Test inference in Python                                    │
│  └── Save to Google Drive                                        │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Training Hyperparameters

**Recommended Configuration**:

```yaml
# Model
model: yolov8n.pt           # Start from COCO pretrained weights
task: detect                 # Object detection task

# Training
epochs: 100                  # Training iterations
batch: 16                    # Batch size (reduce to 8 if OOM)
imgsz: 640                   # Input image size
patience: 20                 # Early stopping patience
save: true                   # Save checkpoints
project: pothole_training    # Output directory name
name: yolov8n_run1          # Run name

# Optimizer
optimizer: AdamW             # Optimizer (default is good)
lr0: 0.01                    # Initial learning rate
lrf: 0.01                    # Final learning rate factor
momentum: 0.937              # SGD momentum / Adam beta1
weight_decay: 0.0005         # L2 regularization

# Data Augmentation
augment: true
hsv_h: 0.015                 # Hue augmentation range
hsv_s: 0.7                   # Saturation augmentation
hsv_v: 0.4                   # Value/brightness augmentation
degrees: 10.0                # Rotation range (±degrees)
translate: 0.1               # Translation range (fraction)
scale: 0.5                   # Scale range
shear: 0.0                   # Shear range
perspective: 0.0             # Perspective distortion
flipud: 0.0                  # Vertical flip (disabled - potholes are on ground)
fliplr: 0.5                  # Horizontal flip probability
mosaic: 1.0                  # Mosaic augmentation probability
mixup: 0.1                   # Mixup augmentation probability
copy_paste: 0.0              # Copy-paste augmentation
```

**Adjustments for Limited Resources**:

```yaml
# If running out of GPU memory:
batch: 8                     # Reduce batch size
imgsz: 416                   # Reduce image size

# If training is too slow:
epochs: 50                   # Reduce epochs
workers: 2                   # Reduce data loader workers

# If underfitting (low accuracy):
epochs: 150                  # More training time
batch: 32                    # Larger batch if memory allows
```

### Training Pseudocode

```
TRAINING_PIPELINE:

    # Step 1: Environment
    INSTALL ultralytics
    MOUNT google_drive
    VERIFY gpu_available

    # Step 2: Dataset
    UPLOAD dataset TO /content/datasets/
    VERIFY data.yaml paths are correct
    PREVIEW random samples with labels

    # Step 3: Initialize
    model = LOAD_MODEL("yolov8n.pt")  # Pretrained on COCO

    # Step 4: Train
    results = model.TRAIN(
        data = "data.yaml",
        epochs = 100,
        imgsz = 640,
        batch = 16,
        patience = 20,
        device = 0,  # GPU
        project = "/content/drive/MyDrive/pothole_model",
        name = "yolov8n_pothole"
    )

    # Step 5: Evaluate
    metrics = model.VAL()
    
    IF metrics.map50 < 0.75:
        PRINT "Warning: Low accuracy, consider more data or larger model"
    ELSE:
        PRINT "Success: mAP@50 = {metrics.map50}"

    # Step 6: Export
    model.EXPORT(
        format = "tflite",
        imgsz = 640,
        half = True  # float16
    )

    # Step 7: Save
    COPY best_float16.tflite TO google_drive
```

### Monitoring Training

**Key Metrics to Watch**:

```
During training, monitor these in the output:

Loss values (should decrease):
├── box_loss: Bounding box regression loss
├── cls_loss: Classification loss (less important for single class)
└── dfl_loss: Distribution focal loss

Validation metrics (should increase):
├── precision: True positives / (True positives + False positives)
├── recall: True positives / (True positives + False negatives)
├── mAP50: Mean Average Precision at IoU 0.5
└── mAP50-95: Mean AP across IoU 0.5-0.95
```

**Healthy Training Signs**:

```
✓ Loss decreasing steadily over epochs
✓ Validation mAP increasing
✓ No sudden spikes in loss
✓ Train and val metrics similar (no overfitting)
```

**Warning Signs**:

```
✗ Val loss increasing while train loss decreasing → Overfitting
✗ Both losses stuck at high value → Underfitting or bad data
✗ NaN values → Learning rate too high
✗ Very slow improvement → Learning rate too low
```

---

## Model Export

### Export Options Comparison

| Format | File Size | Inference Speed | Accuracy Loss | Recommended For |
|--------|-----------|-----------------|---------------|-----------------|
| float32 | ~12 MB | Baseline | None | Testing/debugging |
| **float16** | ~6 MB | 1.5-2x faster | <0.1% | **Production** |
| int8 | ~3 MB | 2-3x faster | 1-2% | Low-end devices |

**Decision**: Use **float16** as primary export format.

### Export Process

```
EXPORT_PIPELINE:

    # Load trained model
    model = LOAD("runs/detect/train/weights/best.pt")

    # Export to TFLite with float16 quantization
    model.EXPORT(
        format = "tflite",
        imgsz = 640,
        half = True,        # float16 quantization
        int8 = False,       # Not int8
        dynamic = False,    # Fixed input size
        simplify = True     # ONNX simplification
    )

    # Output file: best_float16.tflite
    
    # Optional: Also export int8 version for older phones
    model.EXPORT(
        format = "tflite",
        imgsz = 640,
        int8 = True,
        data = "data.yaml"  # Required for int8 calibration
    )
```

### Verify Exported Model

```
VERIFICATION_STEPS:

    # 1. Check file size
    file_size = GET_SIZE("best_float16.tflite")
    ASSERT file_size < 10_MB, "Model too large"
    PRINT "File size: {file_size} MB"  # Should be ~6MB

    # 2. Load model and check shape
    interpreter = TFLiteInterpreter("best_float16.tflite")
    
    input_details = interpreter.GET_INPUT_DETAILS()
    PRINT "Input shape: {input_details[0]['shape']}"   # [1, 640, 640, 3]
    PRINT "Input dtype: {input_details[0]['dtype']}"   # float32
    
    output_details = interpreter.GET_OUTPUT_DETAILS()
    PRINT "Output shape: {output_details[0]['shape']}" # [1, 5, 8400]
    
    # 3. Run test inference
    test_image = LOAD_IMAGE("test_pothole.jpg")
    test_image = RESIZE(test_image, 640, 640)
    test_image = NORMALIZE(test_image, 0, 1)  # Scale to 0-1
    test_image = EXPAND_DIMS(test_image, axis=0)  # Add batch dimension
    
    interpreter.SET_INPUT(test_image)
    interpreter.INVOKE()
    output = interpreter.GET_OUTPUT()
    
    PRINT "Inference successful"
    PRINT "Output shape: {output.shape}"  # (1, 5, 8400)
    
    # 4. Parse output to verify detections
    detections = PARSE_YOLO_OUTPUT(output, confidence_threshold=0.5)
    PRINT "Found {len(detections)} potential potholes"
```

### Output Tensor Format

```
YOLOv8n TFLite Output Specification:

Shape: [1, 5, 8400]

Dimensions:
├── 1: Batch size (always 1 for inference)
├── 5: Detection attributes
│   ├── [0]: x_center (normalized 0-1)
│   ├── [1]: y_center (normalized 0-1)
│   ├── [2]: width (normalized 0-1)
│   ├── [3]: height (normalized 0-1)
│   └── [4]: confidence (0-1)
└── 8400: Number of predictions
    └── Based on 640x640 input with 3 detection scales

For single-class detection (pothole only):
- No class probabilities needed
- Index [4] is directly the pothole confidence

For multi-class, shape would be [1, 4+num_classes, 8400]
- Additional rows for each class probability
```

---

## Validation Checklist

Before proceeding to Android development, verify:

| Checkpoint | Minimum | Target | Status |
|------------|---------|--------|--------|
| Dataset size | 2,500 images | 3,500+ images | □ |
| Local Jordan data | 0 images | 0 images | Skipped |
| mAP@50 | 75% | >80% | □ |
| mAP@50-95 | 40% | >50% | □ |
| Precision | 70% | >80% | □ |
| Recall | 70% | >80% | □ |
| TFLite file size | <10 MB | ~6 MB | □ |
| TFLite inference test | Passes | Passes | □ |
| False positive rate | <30% | <20% | □ |

---

## Troubleshooting Guide

### Common Issues and Solutions

**1. CUDA Out of Memory**

```
Symptom: "CUDA out of memory" error during training

Solutions (try in order):
1. Reduce batch size: batch=8 or batch=4
2. Reduce image size: imgsz=416
3. Close other Colab tabs
4. Restart runtime and try again
5. Use Colab Pro for more GPU memory
```

**2. Low Accuracy (mAP < 70%)**

```
Symptom: Model trains but mAP@50 is below 70%

Solutions:
1. Check label quality - review random samples
2. Add more training data, especially local data
3. Increase training epochs to 150-200
4. Try YOLOv8s instead of YOLOv8n
5. Increase augmentation diversity
6. Check class balance in dataset
```

**3. Overfitting**

```
Symptom: Train loss decreases but val loss increases

Solutions:
1. Add more training data
2. Increase augmentation (mosaic, mixup)
3. Add dropout or weight decay
4. Reduce model complexity (stick with YOLOv8n)
5. Use early stopping (patience=15)
```

**4. TFLite Export Fails**

```
Symptom: Error during model.export(format='tflite')

Solutions:
1. Update ultralytics: pip install ultralytics --upgrade
2. Install tensorflow: pip install tensorflow
3. Try export to ONNX first, then convert separately
4. Check for unsupported operations in model
5. Use opset=12 for ONNX intermediate
```

**5. Slow Training**

```
Symptom: Training takes >4 hours for 100 epochs

Solutions:
1. Verify GPU is being used: nvidia-smi
2. Reduce image size: imgsz=416
3. Reduce dataset size for initial experiments
4. Use Colab Pro for faster GPU (V100/A100)
5. Reduce number of workers if I/O bound
```

---

## Deliverables

At the end of Week 1, Omar should have:

```
□ Combined dataset
  └── Location: Google Drive/pothole_dataset/
  └── Contents: train/, valid/, test/, data.yaml
  └── Size: 3,000+ images

□ Training artifacts
  └── Location: Google Drive/pothole_model/
  └── Contents:
      ├── best.pt (PyTorch weights)
      ├── last.pt (final checkpoint)
      ├── results.csv (training metrics)
      ├── confusion_matrix.png
      └── results.png (training curves)

□ Exported TFLite model
  └── File: yolov8n_pothole_float16.tflite
  └── Size: ~6 MB
  └── Location: Google Drive/pothole_model/export/

□ Documentation
  └── Training configuration used
  └── Final metrics (mAP, precision, recall)
  └── Known limitations or failure cases

□ Handoff for Android
  └── TFLite file ready to copy to android/app/src/main/assets/models/
  └── Input specification: [1, 640, 640, 3] float32, normalized 0-1
  └── Output specification: [1, 5, 8400] float32
  └── Recommended confidence threshold: 0.5
```

---

## Timeline

| Day | Tasks | Hours | Deliverable |
|-----|-------|-------|-------------|
| Day 1 | Download and merge existing datasets (Roboflow, Kaggle, RDD) | 3-4h | Combined dataset ready |
| Day 2 | Pre-processing (verify labels, standardize format) | 2-3h | Cleaned dataset |
| Day 3 | Set up Colab, configure training, start run | 2-3h | Training started |
| Day 4 | Monitor training, evaluate results | 2-3h | Training complete |
| Day 5 | Export to TFLite, verify, document | 2-3h | TFLite model ready |

**Total: ~12-15 hours over 5 days**

---

## Next Steps

After completing AI model training:

1. **Copy model file** to Android project:
   ```
   android/app/src/main/assets/models/yolov8n_pothole_float16.tflite
   ```

2. **Share model specs** with Android development:
   - Input: `[1, 640, 640, 3]` float32, values normalized 0-1
   - Output: `[1, 5, 8400]` float32
   - Confidence threshold: 0.5 (adjustable)
   - NMS IoU threshold: 0.5

3. **Provide test images** with known potholes for Android testing

4. Continue to [Android App Implementation](./02-android-app.md)
