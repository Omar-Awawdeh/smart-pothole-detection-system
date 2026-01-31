# Google Colab Training Instructions

## Overview
This guide will help you train the YOLOv8n pothole detection model on Google Colab with free GPU access.

## Prerequisites
- Google account
- Google Drive with ~1GB free space

## Step-by-Step Instructions

### 1. Upload Dataset to Google Drive

**Option A: Direct Upload (Recommended)**
1. Open Google Drive in your browser
2. Create a new folder called `pothole_dataset`
3. Upload the entire `ai-model/datasets/pothole_combined/` folder structure
4. The structure should be:
   ```
   pothole_dataset/
   ├── train/
   │   ├── images/
   │   └── labels/
   ├── valid/
   │   ├── images/
   │   └── labels/
   └── test/
       ├── images/
       └── labels/
   ```

**Option B: Upload Zip File**
1. The dataset has been zipped for you: `ai-model/datasets.zip` (759MB)
2. Upload this zip to Google Drive
3. You'll extract it in Colab

### 2. Open the Notebook in Google Colab

1. Upload `colab_training_notebook.ipynb` to Google Drive
2. Right-click the file → Open with → Google Colaboratory
3. If you don't see "Google Colaboratory", click "Connect more apps" and search for it

### 3. Configure GPU Runtime

1. In Colab: **Runtime → Change runtime type**
2. Select **Hardware accelerator: GPU**
3. Choose **GPU type: T4** (free tier)
4. Click **Save**

### 4. Run the Notebook

1. **Run each cell sequentially** from top to bottom
2. When prompted to mount Google Drive:
   - Click the link
   - Sign in to your Google account
   - Copy the authorization code
   - Paste it back in Colab
3. When you reach the dataset upload step:
   - If using Option A: Uncomment the copy command and adjust the path
   - If using Option B: Upload the zip file and uncomment the unzip command

### 5. Monitor Training

- Training will take approximately **2-3 hours** on Colab T4 GPU
- Watch the loss curves to ensure they're decreasing
- Key metrics to monitor:
  - `box_loss` - should decrease steadily
  - `mAP@50` - should increase above 0.75 (75%)
  - `precision` and `recall` - should increase

### 6. After Training Completes

1. The notebook will automatically:
   - Evaluate the model
   - Export to TFLite (float16)
   - Save all files to your Google Drive
2. Download these files from `Google Drive/pothole_model/`:
   - `best_float16.tflite` (the main model file, ~6MB)
   - `results.csv` (training metrics)
   - `results.png` (training curves)
   - `confusion_matrix.png`

### 7. Verify Model Quality

Check the final metrics in the notebook output:

| Metric | Minimum | Target | Status |
|--------|---------|--------|--------|
| mAP@50 | 75% | >80% | Check output |
| mAP@50-95 | 40% | >50% | Check output |
| Precision | 70% | >80% | Check output |
| Recall | 70% | >80% | Check output |
| File size | <10 MB | ~6 MB | Should be ~6MB |

If any metric is below minimum:
- Try training for more epochs (increase from 100 to 150)
- Consider using YOLOv8s (larger, more accurate model)
- The model should still work, but may have more false positives/negatives

## Troubleshooting

### "CUDA Out of Memory"
- Reduce batch size: Change `batch=16` to `batch=8` or `batch=4`
- Reduce image size: Change `imgsz=640` to `imgsz=416`

### "Runtime Disconnected"
- Colab free tier has usage limits
- Training progress is saved to Google Drive
- You can resume from the last checkpoint

### "TFLite Export Failed"
- Install tensorflow: Add cell with `!pip install tensorflow`
- Try exporting to ONNX first, then convert to TFLite

### Slow Training
- Verify GPU is enabled: Run `!nvidia-smi` in a cell
- Free tier T4 should train 100 epochs in 2-3 hours
- If much slower, check if GPU is actually being used

## Expected Training Time

| Hardware | Expected Time (100 epochs) |
|----------|---------------------------|
| Colab T4 (free) | 2-3 hours |
| Colab V100 (Pro) | 1-1.5 hours |
| Colab A100 (Pro+) | 45-60 minutes |
| CPU only | 15-20 hours (not recommended) |

## Next Steps

Once you have the `.tflite` file:
1. Copy it to: `android/app/src/main/assets/models/yolov8n_pothole_float16.tflite`
2. Proceed to Android app development (docs/02-android-app.md)

## Support

If you encounter issues:
1. Check the troubleshooting section above
2. Review the Colab notebook output for error messages
3. Ensure all dependencies are installed correctly
4. Verify dataset was uploaded completely
