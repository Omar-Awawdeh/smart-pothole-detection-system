# AI Model Training Workspace

This directory contains scripts and configuration to execute the AI Model Training Plan (Week 1).

## Dataset Status ✓

**Dataset Successfully Prepared!**
- Total images: **2,642**
- Total potholes: **9,077**
- Average potholes per image: **3.44**

**Split:**
- Train: 2,129 images (80.6%)
- Valid: 382 images (14.5%)
- Test: 131 images (5.0%)

**Sources:**
- Roboflow Pothole Dataset
- Kaggle Pothole Detection Datasets (2 sources)

## Training Options

### Option 1: Google Colab (Recommended)

**Why Colab:**
- Free GPU access (T4, 16GB)
- No local setup required
- Faster training (2-3 hours vs 15-20 hours on CPU)

**Steps:**
1. Read `COLAB_INSTRUCTIONS.md` for detailed guide
2. Upload `colab_training_notebook.ipynb` to Google Drive
3. Open with Google Colaboratory
4. Follow the notebook cells sequentially

The notebook includes:
- Environment setup
- Dataset mounting
- Training with progress monitoring
- Model evaluation
- TFLite export
- Automatic saving to Google Drive

### Option 2: Local Training

**Prerequisites:**
- Python 3.8+
- CUDA-compatible GPU (recommended)
- 8GB+ RAM

**Setup:**
```bash
pip install -r requirements.txt
```

**Train:**
```bash
python train.py
```

**Export to TFLite:**
```bash
python export.py
```

## Utility Scripts

### View Dataset Statistics
```bash
python dataset_stats.py
```

### Visualize Samples (requires matplotlib, cv2)
```bash
python visualize_samples.py
```

### Re-ingest Data (if needed)
```bash
python ingest_data.py
```

## Key Files

- `colab_training_notebook.ipynb` - Complete Google Colab training pipeline
- `COLAB_INSTRUCTIONS.md` - Detailed Colab setup guide
- `train.py` - Local training script
- `export.py` - Model export to TFLite
- `data.yaml` - Dataset configuration
- `ingest_data.py` - Dataset ingestion from zip files
- `dataset_stats.py` - Analyze dataset statistics
- `visualize_samples.py` - Preview labeled images
- `datasets.zip` - Compressed dataset (759MB) for Colab upload

## Expected Output

After training completes, you should have:
- `best.pt` - PyTorch model weights
- `best_float16.tflite` - TFLite model (~6MB) **← This goes to Android**
- `results.csv` - Training metrics
- `results.png` - Training curves
- `confusion_matrix.png` - Model evaluation

## Target Metrics

| Metric | Minimum | Target |
|--------|---------|--------|
| mAP@50 | 75% | >80% |
| mAP@50-95 | 40% | >50% |
| Precision | 70% | >80% |
| Recall | 70% | >80% |
| File size | <10 MB | ~6 MB |

## Next Steps

1. Train the model using Google Colab (recommended)
2. Download the `.tflite` file from Google Drive
3. Copy to: `../android/app/src/main/assets/models/`
4. Proceed to Android app development (see `../docs/02-android-app.md`)
