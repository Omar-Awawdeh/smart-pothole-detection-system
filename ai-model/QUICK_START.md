# Quick Start Guide - Train Your Pothole Detection Model

## TL;DR

You have 2,642 labeled pothole images ready to train. Use Google Colab (free GPU) to train in ~2-3 hours.

---

## Fast Track: Google Colab Training (5 steps)

### 1. Open Google Drive
Go to [drive.google.com](https://drive.google.com)

### 2. Upload Files
Upload these 2 files from `ai-model/` folder:
- ✅ `colab_training_notebook.ipynb`
- ✅ `datasets.zip` (759MB)

### 3. Open Notebook
- Right-click `colab_training_notebook.ipynb`
- Select **"Open with" → "Google Colaboratory"**
- If you don't see it, click "Connect more apps" and search for "Colab"

### 4. Enable GPU
- Click **Runtime → Change runtime type**
- Set **Hardware accelerator** to **GPU**
- Click **Save**

### 5. Run Training
- Click **Runtime → Run all** (or press Ctrl+F9)
- When prompted, authorize Google Drive access
- Go grab coffee ☕ - training takes 2-3 hours
- Model will auto-save to Google Drive when done

### 6. Download Result
- Open Google Drive
- Navigate to `pothole_model/` folder
- Download the `.tflite` file (should be ~6MB)
- Copy it to: `android/app/src/main/assets/models/`

---

## What You'll Get

After training completes:

```
✓ best_float16.tflite     (~6MB)  ← The Android model
✓ results.csv                     ← Training metrics
✓ results.png                     ← Training curves  
✓ confusion_matrix.png            ← Accuracy visualization
✓ best.pt                         ← PyTorch weights (backup)
```

---

## Expected Results

Your model should achieve:
- **mAP@50**: >75% (ideally >80%)
- **Precision**: >70%
- **Recall**: >70%
- **File size**: ~6MB

If metrics are lower, the notebook provides suggestions for improvement.

---

## Need Help?

📖 **Detailed instructions**: Read `COLAB_INSTRUCTIONS.md`

🔍 **Dataset info**: Run `python dataset_stats.py`

🖼️ **Preview images**: Run `python visualize_samples.py`

📊 **Training summary**: Read `TRAINING_SUMMARY.md`

---

## Alternative: Local Training

If you have a CUDA GPU locally:

```bash
pip install -r requirements.txt
python train.py          # 2-3 hours with GPU
python export.py         # Creates .tflite file
```

---

## Next Steps

1. ✅ Train model (you are here)
2. ⏳ Download `.tflite` file
3. ⏳ Move to Android project
4. ⏳ Build Android app (see `docs/02-android-app.md`)

---

**Ready? Upload to Colab and start training!** 🚀
