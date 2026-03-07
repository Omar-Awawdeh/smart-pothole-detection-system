# Smart Pothole Detection System - AI-Focused Report

This document accompanies the generated PDF report and highlights the AI-integrated implementation state of the project.

## Included Artifacts

- Source report (HTML): `Report_updated_source.html`
- Generated report (root PDF): `Report.pdf`
- Generated report (docs PDF): `docs/Report_AI_Focused.pdf`

## AI Scope Covered

- Dataset preparation and split from public pothole datasets
- YOLOv8n training workflow and export to TensorFlow Lite float16
- Android on-device inference integration (`PotholeDetector.kt`, `ProcessFrameUseCase.kt`)
- Quantitative evaluation and deployment-oriented interpretation
- AI-specific risks, ethics, and future model-improvement work

## Evidence Anchors

- Training metrics and analysis: `ai-model/trained_model/TRAINING_METRICS.md`
- Epoch metrics CSV: `ai-model/trained_model/results.csv`
- Dataset/training summary: `ai-model/TRAINING_SUMMARY.md`
- Training workspace docs: `ai-model/README.md`
- Android inference implementation:
  - `android/app/src/main/java/com/pothole/detection/detection/PotholeDetector.kt`
  - `android/app/src/main/java/com/pothole/detection/domain/usecase/ProcessFrameUseCase.kt`

## Final Report Metric Set (from `results.csv`, epoch 100)

- mAP@50: 80.52%
- mAP@50-95: 50.29%
- Precision: 81.16%
- Recall: 71.77%

## Notes

- The report explicitly distinguishes measured model metrics from estimated deployment expectations.
- Core detection is on-device; backend connectivity is used for report upload/operations workflows.
