# Pothole Detection Mobile Application

**Graduation Project - Computer Engineering**  
**Institution**: [Your University Name]  
**Team Members**: Omar [+ Team Members]  
**Academic Year**: 2025-2026

---

## Project Overview

An end-to-end mobile application for real-time pothole detection using computer vision and deep learning. The system detects potholes through smartphone cameras, reports them to a central database, and provides analytics for road maintenance authorities.

### Key Features

- 🚗 **Real-time Detection**: On-device AI inference using TensorFlow Lite
- 📱 **Mobile-First**: Native Android application with modern UI
- 🗺️ **GPS Integration**: Automatic location tagging for detected potholes
- 📊 **Analytics Dashboard**: Web-based admin panel for road authorities
- 🔐 **User Management**: Authentication and report tracking
- ☁️ **Cloud Backend**: RESTful API with Spring Boot

---

## Technology Stack

### AI/ML
- **Model**: YOLOv8n (nano) - optimized for mobile
- **Framework**: Ultralytics YOLO
- **Deployment**: TensorFlow Lite (float16 quantization)
- **Training**: Google Colab (free T4 GPU)

### Mobile
- **Platform**: Android (API 24+)
- **Language**: Kotlin
- **ML Integration**: TensorFlow Lite Android
- **Camera**: CameraX API
- **UI**: Material Design 3

### Backend
- **Framework**: Spring Boot 3.x
- **Language**: Java/Kotlin
- **Database**: PostgreSQL + PostGIS
- **Authentication**: JWT tokens
- **API**: RESTful with JSON

### DevOps
- **Version Control**: Git
- **Training**: Google Colab
- **Deployment**: [TBD]

---

## Project Structure

```
graduation_project/
├── docs/                          # Project documentation
│   ├── 01-ai-model.md            # AI training plan
│   └── 02-android-app.md         # Android development plan
│
├── ai-model/                      # ✅ COMPLETE - Ready for training
│   ├── datasets/                  # 2,642 labeled images
│   ├── colab_training_notebook.ipynb  # Training pipeline
│   ├── QUICK_START.md             # Fast track guide
│   ├── COLAB_INSTRUCTIONS.md      # Detailed setup
│   └── [training scripts]
│
├── android/                       # 🔜 PENDING
│   └── [Android app source]
│
├── backend/                       # 🔜 PENDING
│   └── [Spring Boot API]
│
└── README.md                      # This file
```

---

## Current Status

### ✅ Phase 1: AI Model Training - READY
**Status**: All preparation complete

- ✅ Dataset collected and preprocessed (2,642 images, 9,077 potholes)
- ✅ Training environment configured (Google Colab notebook)
- ✅ Export pipeline ready (TFLite conversion)
- ✅ Comprehensive documentation written

**Next Action**: Upload to Google Colab and train (~2-3 hours)

### 🔜 Phase 2: Android App Development
**Status**: Pending model training completion

**Planned Features**:
- Camera capture with real-time inference
- Detection results overlay
- Report submission with GPS
- User authentication
- Report history

### 🔜 Phase 3: Backend API Development
**Status**: Not started

**Planned Components**:
- User management endpoints
- Report CRUD operations
- Geospatial queries
- Admin dashboard API
- File upload handling

### 🔜 Phase 4: Integration & Testing
**Status**: Not started

---

## Quick Start

### For AI Model Training

1. Navigate to the AI model directory:
   ```bash
   cd ai-model
   ```

2. Read the quick start guide:
   ```bash
   cat QUICK_START.md
   ```

3. Upload to Google Colab:
   - Upload `colab_training_notebook.ipynb`
   - Upload `datasets.zip` (759MB)
   - Enable GPU runtime
   - Run all cells

4. Download the trained `.tflite` model when complete

### For Android Development
*Coming soon - awaiting trained model*

### For Backend Development
*Coming soon - awaiting requirements finalization*

---

## Dataset Information

**Size**: 2,642 images  
**Annotations**: 9,077 potholes  
**Sources**: 
- Roboflow Pothole Dataset (665 images)
- Kaggle Potholes Detection YOLOv8 (1,977 images)

**Split**:
- Training: 2,129 images (80.6%)
- Validation: 382 images (14.5%)
- Testing: 131 images (5.0%)

**Format**: YOLO (normalized bounding boxes)

---

## Model Specifications

**Architecture**: YOLOv8n (nano variant)  
**Input Size**: 640×640×3  
**Output**: [1, 5, 8400] (class + bbox coordinates)  
**Target Accuracy**: mAP@50 >80%  
**File Size**: ~6MB (float16 TFLite)  
**Inference Speed**: 25-30 FPS on mobile devices  

---

## Development Timeline

| Phase | Duration | Status |
|-------|----------|--------|
| AI Model Training | Week 1 (5 days) | ✅ Ready |
| Android App Dev | Week 2-3 (10 days) | 🔜 Pending |
| Backend API Dev | Week 4 (5 days) | 🔜 Pending |
| Integration & Testing | Week 5 (5 days) | 🔜 Pending |

**Current**: End of Week 1 (Dataset Prep Complete)

---

## Documentation

- **AI Model**: See `ai-model/README.md` and `ai-model/QUICK_START.md`
- **Training Plan**: See `docs/01-ai-model.md`
- **Android Plan**: See `docs/02-android-app.md` (when available)
- **Project Status**: See `PROJECT_STATUS.md`

---

## System Requirements

### For Training
- Google account (for Colab)
- ~1GB Google Drive storage
- Web browser

### For Android Development
- Android Studio Arctic Fox or later
- Android SDK 24+ (Android 7.0)
- Physical device or emulator with camera

### For Backend Development
- Java 17+
- PostgreSQL 13+
- Maven 3.8+

---

## Performance Targets

### Model Performance
- **Accuracy**: mAP@50 >80%
- **Speed**: 25-30 FPS on mobile
- **Size**: <10MB model file

### Mobile Performance
- **Latency**: <100ms per frame
- **Memory**: <500MB RAM usage
- **Battery**: <5% per hour active use

### Backend Performance
- **API Latency**: <200ms per request
- **Throughput**: 100+ requests/second
- **Uptime**: >99.5%

---

## Contributing

This is an academic graduation project. For questions or collaboration:

**Project Owner**: Omar  
**Repository**: [GitHub URL if applicable]  
**Contact**: [Your email]

---

## License

Academic project - all rights reserved to the authors and institution.

---

## Acknowledgments

- **Datasets**: Roboflow Community, Kaggle Contributors
- **Framework**: Ultralytics (YOLOv8)
- **Platform**: Google Colab (free GPU access)
- **Advisors**: [Your project advisor(s)]

---

## Appendix

### Key Files to Upload for Training
1. `ai-model/colab_training_notebook.ipynb` (14KB)
2. `ai-model/datasets.zip` (759MB)

### Expected Deliverables
1. Trained TFLite model (~6MB)
2. Android APK
3. Backend deployment
4. Technical documentation
5. User manual
6. Project presentation

---

**Last Updated**: January 30, 2026  
**Version**: 0.1.0 (Pre-training)  
**Status**: Phase 1 Complete ✅
