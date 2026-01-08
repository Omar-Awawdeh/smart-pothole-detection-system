# Project Timeline - 4 Week Implementation Schedule

This document provides a detailed day-by-day task breakdown for the Smart Pothole Detection System implementation. The schedule is designed for 2 team members working 2-3 hours per day, 7 days a week.

---

## Timeline Overview

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                            4-WEEK PROJECT TIMELINE                                   │
├─────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                      │
│  WEEK 1: Foundation                                                                  │
│  ├── Omar: AI Model Training (Dataset + YOLOv8n + TFLite export)                   │
│  └── Hamza: Backend Core (Server setup + Database + Auth + Basic CRUD)             │
│                                                                                      │
│  WEEK 2: Core Features                                                               │
│  ├── Omar: Android App Core (Camera + GPS + Detection integration)                 │
│  └── Hamza: Dashboard MVP (Login + Pothole list + Map view)                        │
│                                                                                      │
│  WEEK 3: Integration                                                                 │
│  ├── Omar: App ↔ API Integration (Upload queue + Sync + UI polish)                 │
│  └── Hamza: Advanced Features (S3 images + Statistics + Vehicle management)        │
│                                                                                      │
│  WEEK 4: Polish & Deploy                                                             │
│  ├── Omar: Testing + Optimization + Bug fixes                                       │
│  └── Hamza: Deployment + Production setup + Final testing                          │
│                                                                                      │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

### Team Assignments

| Team Member | Primary Responsibilities | Skills Required |
|-------------|-------------------------|-----------------|
| **Omar** | AI Model + Android App | Python, Kotlin, TensorFlow, Android SDK |
| **Hamza** | Backend API + Dashboard + DevOps | TypeScript, Node.js, React, PostgreSQL, Docker |

### Daily Time Budget

- **Per person**: 2-3 hours/day
- **Weekly per person**: 14-21 hours
- **Total project hours**: 112-168 hours combined
- **Buffer**: Built into Week 4 for unexpected issues

---

## Week 1: Foundation

### Week 1 Goals

```
WEEK 1 SUCCESS CRITERIA

Omar (AI + Android Setup):
├── ✓ YOLOv8n model trained on pothole dataset
├── ✓ Model achieves >75% mAP@50 on validation set
├── ✓ TFLite model exported and tested
├── ✓ Android project scaffolded with dependencies
└── ✓ LiteRT integration verified with sample inference

Hamza (Backend + Database):
├── ✓ Hetzner VPS provisioned and secured
├── ✓ PostgreSQL + PostGIS running in Docker
├── ✓ Database schema created with migrations
├── ✓ Fastify server with auth endpoints working
└── ✓ Basic pothole CRUD endpoints implemented
```

---

### Day 1 (Week 1)

#### Omar - AI Model Setup

```
TIME: 2-3 hours
FOCUS: Dataset preparation

TASKS:
├── 1. Set up Google Colab environment (30 min)
│   ├── Create new notebook: "pothole_yolov8_training.ipynb"
│   ├── Mount Google Drive for persistent storage
│   ├── Verify GPU is available (Runtime → Change runtime type → GPU)
│   └── Install ultralytics package
│
├── 2. Download pothole datasets (1 hour)
│   ├── Roboflow: Search "pothole detection" datasets
│   │   ├── Download in YOLOv8 format
│   │   └── Aim for 2000+ images minimum
│   ├── Alternative sources:
│   │   ├── Kaggle pothole datasets
│   │   └── GitHub pothole-detection repos
│   └── Upload to Google Drive
│
├── 3. Explore and validate dataset (30 min)
│   ├── Check image quality and variety
│   ├── Verify annotation format (YOLO format: class x y w h)
│   ├── Count images per class
│   └── Identify any data quality issues
│
└── 4. Prepare dataset structure (30 min)
    ├── Organize into train/val/test splits (70/20/10)
    ├── Create data.yaml configuration file
    └── Verify paths are correct

DELIVERABLE:
└── Dataset ready for training in Google Drive
    ├── /pothole-dataset/train/images/
    ├── /pothole-dataset/train/labels/
    ├── /pothole-dataset/val/images/
    ├── /pothole-dataset/val/labels/
    └── /pothole-dataset/data.yaml
```

#### Hamza - Server Setup

```
TIME: 2-3 hours
FOCUS: VPS provisioning and security

TASKS:
├── 1. Create Hetzner Cloud account (15 min)
│   ├── Sign up at hetzner.com
│   ├── Add payment method
│   └── Create new project: "pothole-detection"
│
├── 2. Generate SSH key (10 min)
│   ├── Run: ssh-keygen -t ed25519 -C "pothole-project"
│   ├── Save to ~/.ssh/pothole_hetzner
│   └── Copy public key for Hetzner
│
├── 3. Provision VPS (20 min)
│   ├── Location: Falkenstein (Germany)
│   ├── Image: Ubuntu 22.04
│   ├── Type: CX31 (4 vCPU, 8GB RAM)
│   ├── Add SSH key
│   ├── Create firewall (ports 22, 80, 443)
│   └── Note the IP address
│
├── 4. Initial server configuration (1 hour)
│   ├── SSH into server
│   ├── Run system updates: apt update && apt upgrade -y
│   ├── Install essential packages
│   ├── Create 'deploy' user with sudo
│   ├── Configure SSH (disable root login, password auth)
│   ├── Setup UFW firewall
│   └── Configure fail2ban
│
└── 5. Install Docker (30 min)
    ├── Add Docker repository
    ├── Install Docker Engine and Compose plugin
    ├── Add deploy user to docker group
    └── Verify with: docker run hello-world

DELIVERABLE:
└── Secured Ubuntu server with Docker ready
    ├── SSH access via key only
    ├── Firewall configured
    └── Docker installed and working
```

---

### Day 2 (Week 1)

#### Omar - Model Training

```
TIME: 2-3 hours
FOCUS: Train YOLOv8n model

TASKS:
├── 1. Configure training parameters (30 min)
│   ├── Create training config in notebook
│   ├── Set hyperparameters:
│   │   ├── epochs: 100
│   │   ├── batch: 16 (or 8 if memory issues)
│   │   ├── imgsz: 640
│   │   ├── model: yolov8n.pt (nano - smallest)
│   │   └── patience: 20 (early stopping)
│   └── Configure data augmentation
│
├── 2. Start training run (15 min to start, runs in background)
│   ├── Initialize YOLO model
│   ├── Start training: model.train(data='data.yaml', ...)
│   ├── Training will take 2-4 hours on Colab GPU
│   └── Can monitor in background or check later
│
├── 3. While training: Research TFLite export (1 hour)
│   ├── Read Ultralytics TFLite export documentation
│   ├── Understand quantization options:
│   │   ├── FP16 (good balance)
│   │   ├── INT8 (smallest, needs calibration data)
│   │   └── FP32 (largest, most accurate)
│   ├── Research LiteRT/TFLite Android integration
│   └── Find example inference code
│
└── 4. While training: Review Android LiteRT docs (1 hour)
    ├── Read TensorFlow Lite Android guide
    ├── Understand input/output tensor formats
    ├── Research NNAPI delegate for NPU acceleration
    └── Note required dependencies for build.gradle

DELIVERABLE:
└── Training in progress (check results on Day 3)
    └── Understanding of export and mobile integration
```

#### Hamza - Database Setup

```
TIME: 2-3 hours
FOCUS: PostgreSQL + PostGIS + Schema

TASKS:
├── 1. Create Docker Compose for database (30 min)
│   ├── Create project directory: /home/deploy/pothole
│   ├── Create docker-compose.yml with postgres service
│   ├── Use postgis/postgis:16-3.4 image
│   ├── Configure environment variables
│   └── Set up volume for data persistence
│
├── 2. Start database and verify (15 min)
│   ├── Run: docker compose up -d
│   ├── Check logs: docker compose logs postgres
│   ├── Connect: docker compose exec postgres psql -U pothole_user
│   └── Verify PostGIS: SELECT PostGIS_Version();
│
├── 3. Initialize backend project (45 min)
│   ├── Create backend directory
│   ├── Run: npm init -y
│   ├── Install dependencies:
│   │   ├── fastify, @fastify/cors, @fastify/multipart
│   │   ├── drizzle-orm, postgres (driver)
│   │   ├── typescript, tsx, @types/node
│   │   ├── zod (validation)
│   │   └── bcrypt, jsonwebtoken, @types/bcrypt, @types/jsonwebtoken
│   ├── Configure tsconfig.json
│   └── Set up project structure (src/routes, src/services, etc.)
│
├── 4. Create database schema with Drizzle (45 min)
│   ├── Define users table
│   ├── Define vehicles table
│   ├── Define potholes table with PostGIS geometry
│   ├── Create drizzle.config.ts
│   └── Generate initial migration
│
└── 5. Run migrations and verify (15 min)
    ├── Run: npm run db:migrate
    ├── Connect to database and verify tables
    └── Check PostGIS spatial columns

DELIVERABLE:
└── Database running with schema
    ├── PostgreSQL + PostGIS in Docker
    ├── users, vehicles, potholes tables created
    └── Backend project initialized
```

---

### Day 3 (Week 1)

#### Omar - Model Validation & Export

```
TIME: 2-3 hours
FOCUS: Validate training results and export to TFLite

TASKS:
├── 1. Check training results (30 min)
│   ├── Review training logs and metrics
│   ├── Check mAP@50 score (target: >75%)
│   ├── Review confusion matrix
│   ├── Check for overfitting (compare train vs val loss)
│   └── If poor results: adjust and retrain
│
├── 2. Validate model on test set (30 min)
│   ├── Run validation: model.val(data='data.yaml')
│   ├── Review per-class metrics
│   ├── Test on sample images visually
│   └── Save validation results
│
├── 3. Export to TFLite (45 min)
│   ├── Export with FP16 quantization:
│   │   model.export(format='tflite', half=True)
│   ├── Note output file size (target: <15MB)
│   ├── Export with INT8 if needed for smaller size
│   └── Download .tflite file to local machine
│
├── 4. Test TFLite model (45 min)
│   ├── Install TFLite interpreter locally
│   ├── Write simple Python test script
│   ├── Load model and run inference on test image
│   ├── Verify output format and values
│   └── Measure inference time
│
└── 5. Document model specifications (15 min)
    ├── Input size: 640x640x3
    ├── Input format: float32 normalized [0,1] or uint8 [0,255]
    ├── Output format: [1, num_detections, 6] (x,y,w,h,conf,class)
    └── Note any preprocessing requirements

DELIVERABLE:
└── Validated TFLite model ready for Android
    ├── yolov8n_pothole.tflite file
    ├── Validation metrics documented
    └── Input/output specifications noted
```

#### Hamza - Authentication System

```
TIME: 2-3 hours
FOCUS: Implement JWT authentication

TASKS:
├── 1. Create auth service (1 hour)
│   ├── Implement password hashing with bcrypt
│   ├── Implement JWT token generation
│   ├── Implement JWT token verification
│   ├── Create user registration logic
│   └── Create user login logic
│
├── 2. Create auth routes (45 min)
│   ├── POST /api/auth/register
│   │   ├── Validate input (email, password, name)
│   │   ├── Check if user exists
│   │   ├── Hash password
│   │   └── Create user and return token
│   │
│   ├── POST /api/auth/login
│   │   ├── Validate credentials
│   │   ├── Verify password
│   │   └── Return JWT token
│   │
│   └── GET /api/auth/me
│       ├── Require authentication
│       └── Return current user info
│
├── 3. Create auth middleware/plugin (30 min)
│   ├── Create Fastify plugin for JWT verification
│   ├── Extract token from Authorization header
│   ├── Verify and decode token
│   └── Attach user to request object
│
├── 4. Test authentication (30 min)
│   ├── Test registration with curl/Postman
│   ├── Test login and get token
│   ├── Test protected route with token
│   └── Test invalid/expired token handling
│
└── 5. Create .env file (15 min)
    ├── DATABASE_URL
    ├── JWT_SECRET (generate secure random)
    └── PORT

DELIVERABLE:
└── Working authentication system
    ├── User registration and login
    ├── JWT token generation and verification
    └── Protected route middleware
```

---

### Day 4 (Week 1)

#### Omar - Android Project Setup

```
TIME: 2-3 hours
FOCUS: Create Android project with dependencies

TASKS:
├── 1. Create new Android project (30 min)
│   ├── Open Android Studio
│   ├── New Project → Empty Compose Activity
│   ├── Name: PotholeDetector
│   ├── Package: com.pothole.detector
│   ├── Minimum SDK: API 28 (Android 9.0)
│   └── Build configuration: Kotlin DSL
│
├── 2. Configure build.gradle.kts (45 min)
│   ├── Add dependencies:
│   │   ├── Jetpack Compose (BOM)
│   │   ├── CameraX (camera-core, camera-camera2, camera-view)
│   │   ├── LiteRT / TensorFlow Lite
│   │   ├── Google Play Services Location
│   │   ├── Ktor Client (Android + CIO)
│   │   ├── Room Database
│   │   ├── Hilt (dependency injection)
│   │   ├── WorkManager
│   │   └── Kotlin Coroutines
│   │
│   ├── Configure compose options
│   ├── Enable viewBinding if needed
│   └── Sync project
│
├── 3. Set up project structure (30 min)
│   ├── Create package structure:
│   │   ├── ui/ (screens, components)
│   │   ├── detection/ (AI inference)
│   │   ├── camera/ (CameraX)
│   │   ├── location/ (GPS)
│   │   ├── network/ (API client)
│   │   ├── data/ (repository, Room)
│   │   ├── worker/ (background upload)
│   │   └── di/ (Hilt modules)
│   │
│   └── Create placeholder files for each module
│
├── 4. Configure AndroidManifest.xml (15 min)
│   ├── Add permissions:
│   │   ├── CAMERA
│   │   ├── ACCESS_FINE_LOCATION
│   │   ├── ACCESS_COARSE_LOCATION
│   │   ├── INTERNET
│   │   └── ACCESS_NETWORK_STATE
│   │
│   └── Configure camera and location features
│
└── 5. Verify project builds (30 min)
    ├── Clean and rebuild project
    ├── Fix any dependency conflicts
    ├── Run on emulator to verify setup
    └── Check for any deprecated APIs

DELIVERABLE:
└── Android project scaffolded
    ├── All dependencies configured
    ├── Project structure created
    └── Builds successfully
```

#### Hamza - Pothole CRUD Endpoints

```
TIME: 2-3 hours
FOCUS: Implement pothole management endpoints

TASKS:
├── 1. Create pothole service (1 hour)
│   ├── Implement createPothole function
│   │   ├── Insert pothole with PostGIS point
│   │   ├── Link to vehicle
│   │   └── Set initial status as 'unverified'
│   │
│   ├── Implement getPotholes function
│   │   ├── Support pagination (limit, offset)
│   │   ├── Support filtering by status
│   │   └── Return with vehicle info
│   │
│   ├── Implement getPotholeById function
│   │
│   └── Implement updatePotholeStatus function
│       └── Allow: verified, repaired, false_positive
│
├── 2. Create pothole routes (45 min)
│   ├── POST /api/potholes
│   │   ├── Require authentication
│   │   ├── Validate: latitude, longitude, confidence, vehicleId
│   │   └── Return created pothole
│   │
│   ├── GET /api/potholes
│   │   ├── Require authentication
│   │   ├── Support query params: status, limit, offset
│   │   └── Return paginated list
│   │
│   ├── GET /api/potholes/:id
│   │   └── Return single pothole with details
│   │
│   └── PATCH /api/potholes/:id/status
│       ├── Require admin role
│       └── Update status
│
├── 3. Implement PostGIS spatial helpers (30 min)
│   ├── Create function to insert point geometry
│   ├── Create function to query within radius
│   └── Test spatial queries work correctly
│
├── 4. Test endpoints (30 min)
│   ├── Create test pothole via API
│   ├── List potholes with pagination
│   ├── Get single pothole
│   ├── Update status
│   └── Verify data in database
│
└── 5. Add request validation with Zod (15 min)
    ├── Create schemas for each endpoint
    ├── Add validation to routes
    └── Return proper error messages

DELIVERABLE:
└── Working pothole CRUD API
    ├── Create, Read, Update endpoints
    ├── Pagination and filtering
    └── PostGIS spatial data storage
```

---

### Day 5 (Week 1)

#### Omar - LiteRT Integration

```
TIME: 2-3 hours
FOCUS: Integrate TFLite model into Android app

TASKS:
├── 1. Add model file to project (15 min)
│   ├── Create assets/models/ directory
│   ├── Copy yolov8n_pothole.tflite to assets
│   ├── Configure build.gradle to not compress tflite files
│   └── Verify file is included in APK
│
├── 2. Create PotholeDetector class (1.5 hours)
│   ├── Create class to handle model loading
│   ├── Load model from assets using Interpreter
│   ├── Configure NNAPI delegate for hardware acceleration
│   ├── Implement image preprocessing:
│   │   ├── Resize to 640x640
│   │   ├── Convert to RGB if needed
│   │   ├── Normalize pixel values
│   │   └── Convert to ByteBuffer
│   │
│   ├── Implement inference method
│   ├── Implement output parsing:
│   │   ├── Extract bounding boxes
│   │   ├── Extract confidence scores
│   │   └── Filter by confidence threshold
│   │
│   └── Implement Non-Maximum Suppression (NMS)
│
├── 3. Create DetectionResult data class (15 min)
│   ├── boundingBox: RectF
│   ├── confidence: Float
│   ├── classId: Int (0 = pothole)
│   └── timestamp: Long
│
├── 4. Test detection with sample image (45 min)
│   ├── Load sample pothole image from resources
│   ├── Run detection
│   ├── Log results
│   ├── Verify bounding box coordinates
│   └── Measure inference time
│
└── 5. Optimize for performance (15 min)
    ├── Try different delegate options (GPU, NNAPI)
    ├── Measure inference time with each
    └── Choose best option for target devices

DELIVERABLE:
└── Working AI inference in Android
    ├── Model loads successfully
    ├── Inference returns detections
    └── Inference time <150ms
```

#### Hamza - Vehicle & User Endpoints

```
TIME: 2-3 hours
FOCUS: Complete remaining API endpoints

TASKS:
├── 1. Create vehicle service (45 min)
│   ├── Implement createVehicle
│   │   ├── Generate unique serial number
│   │   └── Link to user
│   │
│   ├── Implement getVehicles (for user)
│   ├── Implement getVehicleById
│   ├── Implement updateVehicle
│   └── Implement updateLastActive (for heartbeat)
│
├── 2. Create vehicle routes (30 min)
│   ├── POST /api/vehicles
│   ├── GET /api/vehicles
│   ├── GET /api/vehicles/:id
│   ├── PATCH /api/vehicles/:id
│   └── POST /api/vehicles/:id/heartbeat
│
├── 3. Create user management endpoints (30 min)
│   ├── GET /api/users (admin only)
│   ├── GET /api/users/:id
│   ├── PATCH /api/users/:id
│   └── Implement role-based access control
│
├── 4. Create statistics endpoint (30 min)
│   ├── GET /api/stats
│   │   ├── Total potholes count
│   │   ├── Potholes by status
│   │   ├── Potholes this week/month
│   │   ├── Active vehicles count
│   │   └── Recent activity
│   │
│   └── Implement efficient aggregation queries
│
├── 5. Add health check endpoint (15 min)
│   ├── GET /health
│   │   ├── Check database connection
│   │   └── Return status and uptime
│   │
│   └── No authentication required
│
└── 6. Test all endpoints (30 min)
    ├── Create complete API test flow
    ├── Document any issues
    └── Verify error handling

DELIVERABLE:
└── Complete backend API
    ├── All CRUD operations
    ├── Statistics endpoint
    └── Health check
```

---

### Day 6 (Week 1)

#### Omar - CameraX Integration

```
TIME: 2-3 hours
FOCUS: Implement camera preview with CameraX

TASKS:
├── 1. Create CameraManager class (1 hour)
│   ├── Initialize CameraX with lifecycle
│   ├── Configure Preview use case
│   ├── Configure ImageAnalysis use case
│   │   ├── Set target resolution (640x480 or 1280x720)
│   │   ├── Set backpressure strategy (keep only latest)
│   │   └── Set output image format (YUV or RGBA)
│   │
│   ├── Bind use cases to lifecycle
│   └── Handle camera permissions
│
├── 2. Create camera preview composable (45 min)
│   ├── Create AndroidView for PreviewView
│   ├── Handle surface provider
│   ├── Implement camera permission request
│   └── Show permission denied state
│
├── 3. Implement frame analysis (45 min)
│   ├── Create ImageAnalysis.Analyzer implementation
│   ├── Convert ImageProxy to Bitmap
│   ├── Handle image rotation
│   ├── Call PotholeDetector with frame
│   └── Process results
│
├── 4. Connect camera to detector (30 min)
│   ├── In analyzer, pass frames to detector
│   ├── Implement frame skipping (process every 2nd-3rd frame)
│   ├── Handle threading (detection on background thread)
│   └── Emit results to UI
│
└── 5. Test camera preview (15 min)
    ├── Run app on physical device
    ├── Verify preview displays
    ├── Verify frames are being analyzed
    └── Check for crashes or memory issues

DELIVERABLE:
└── Working camera with live analysis
    ├── Camera preview displays
    ├── Frames passed to detector
    └── Results logged
```

#### Hamza - Deduplication Service

```
TIME: 2-3 hours
FOCUS: Implement server-side deduplication with PostGIS

TASKS:
├── 1. Create deduplication service (1.5 hours)
│   ├── Implement findNearbyPotholes function
│   │   ├── Use ST_DWithin for radius query
│   │   ├── Search within 15 meters
│   │   └── Return existing pothole if found
│   │
│   ├── Implement checkDuplicate function
│   │   ├── Query for potholes within radius
│   │   ├── If found: increment confirmation_count
│   │   ├── If not found: return null (not duplicate)
│   │   └── Return duplicate info
│   │
│   └── Write PostGIS spatial query:
│       SELECT * FROM potholes
│       WHERE ST_DWithin(
│           location::geography,
│           ST_MakePoint(lng, lat)::geography,
│           15  -- meters
│       )
│       AND status != 'false_positive'
│       ORDER BY ST_Distance(location, ST_MakePoint(lng, lat))
│       LIMIT 1
│
├── 2. Integrate into pothole creation (30 min)
│   ├── Modify POST /api/potholes endpoint
│   ├── Check for duplicates before inserting
│   ├── If duplicate:
│   │   ├── Update existing pothole
│   │   └── Return { isDuplicate: true, existingId }
│   │
│   └── If not duplicate:
│       └── Create new pothole
│
├── 3. Add confirmation tracking (30 min)
│   ├── Add confirmation_count column to potholes
│   ├── Add last_confirmed_at column
│   ├── Increment on duplicate detection
│   └── Update migration
│
├── 4. Test deduplication (30 min)
│   ├── Create pothole at location A
│   ├── Create pothole 10m away → should dedupe
│   ├── Create pothole 20m away → should be new
│   ├── Verify confirmation count increases
│   └── Test edge cases
│
└── 5. Add spatial index (15 min)
    ├── Create GiST index on location column
    ├── Run ANALYZE on potholes table
    └── Verify query uses index (EXPLAIN)

DELIVERABLE:
└── Working spatial deduplication
    ├── 15m radius duplicate detection
    ├── Confirmation count tracking
    └── Efficient spatial queries
```

---

### Day 7 (Week 1)

#### Omar - Location Service

```
TIME: 2-3 hours
FOCUS: Implement GPS tracking with Fused Location Provider

TASKS:
├── 1. Create LocationService class (1 hour)
│   ├── Initialize FusedLocationProviderClient
│   ├── Configure LocationRequest:
│   │   ├── Priority: HIGH_ACCURACY
│   │   ├── Interval: 5 seconds
│   │   └── Fastest interval: 2 seconds
│   │
│   ├── Implement location permission handling
│   ├── Create Flow to emit location updates
│   ├── Handle location settings (prompt user to enable GPS)
│   └── Implement start/stop location updates
│
├── 2. Create location data classes (15 min)
│   ├── PotholeLocation data class
│   │   ├── latitude: Double
│   │   ├── longitude: Double
│   │   ├── accuracy: Float
│   │   └── timestamp: Long
│   │
│   └── Handle null/invalid locations
│
├── 3. Integrate with detection flow (45 min)
│   ├── When detection occurs:
│   │   ├── Get current location
│   │   ├── Combine with detection result
│   │   └── Create PotholeReport
│   │
│   ├── Handle case when location unavailable
│   └── Add location accuracy threshold (ignore if >50m accuracy)
│
├── 4. Implement local deduplication (45 min)
│   ├── Keep in-memory list of recent detections
│   ├── Check if new detection is within 10m of recent
│   ├── Use Haversine formula for distance calculation
│   ├── Time window: 60 seconds
│   └── Clear old entries periodically
│
└── 5. Test location service (15 min)
    ├── Run app and verify location updates
    ├── Test with GPS enabled/disabled
    ├── Verify accuracy values
    └── Test local deduplication

DELIVERABLE:
└── Working GPS integration
    ├── Location updates flowing
    ├── Combined with detections
    └── Local deduplication working
```

#### Hamza - AWS S3 Integration

```
TIME: 2-3 hours
FOCUS: Set up S3 bucket and integrate image uploads

TASKS:
├── 1. Create AWS S3 bucket (30 min)
│   ├── Login to AWS Console
│   ├── Create bucket: pothole-detection-images-{suffix}
│   ├── Region: eu-central-1 (Frankfurt)
│   ├── Block all public access
│   ├── Create IAM user with limited S3 permissions
│   └── Generate access key and secret
│
├── 2. Configure S3 in backend (30 min)
│   ├── Install @aws-sdk/client-s3
│   ├── Create S3 client configuration
│   ├── Add AWS credentials to .env
│   └── Create upload service
│
├── 3. Implement image upload service (1 hour)
│   ├── Create uploadImage function
│   │   ├── Generate unique filename: {vehicleId}/{date}/{uuid}.jpg
│   │   ├── Upload to S3 with PutObjectCommand
│   │   └── Return S3 URL/key
│   │
│   ├── Create getSignedUrl function
│   │   ├── Generate presigned URL for viewing
│   │   └── Set expiration (1 hour)
│   │
│   └── Handle upload errors
│
├── 4. Integrate with pothole creation (30 min)
│   ├── Modify POST /api/potholes to accept image
│   ├── Use @fastify/multipart for file upload
│   ├── Upload image to S3
│   ├── Store S3 key in database
│   └── Return image URL in response
│
├── 5. Test image upload (30 min)
│   ├── Upload test image via API
│   ├── Verify file in S3 bucket
│   ├── Test presigned URL generation
│   └── Verify image displays correctly
│
└── 6. Configure CORS on S3 bucket (15 min)
    ├── Add CORS configuration for dashboard domain
    └── Test image loading from browser

DELIVERABLE:
└── Working S3 image storage
    ├── Images upload to S3
    ├── URLs stored in database
    └── Presigned URLs for viewing
```

---

### Week 1 Checkpoint

```
END OF WEEK 1 REVIEW

Omar - What Should Be Complete:
├── ✅ YOLOv8n model trained (>75% mAP)
├── ✅ TFLite model exported and validated
├── ✅ Android project with all dependencies
├── ✅ LiteRT inference working
├── ✅ CameraX preview with frame analysis
├── ✅ GPS location service
└── ✅ Local deduplication

Hamza - What Should Be Complete:
├── ✅ Hetzner VPS secured and Docker installed
├── ✅ PostgreSQL + PostGIS running
├── ✅ Database schema with migrations
├── ✅ JWT authentication working
├── ✅ Pothole CRUD endpoints
├── ✅ Vehicle and user endpoints
├── ✅ Server-side deduplication
└── ✅ S3 image upload

WEEK 1 SYNC MEETING (30 min):
├── Review completed work
├── Demo backend API to Omar
├── Demo Android detection to Hamza
├── Identify any blockers
├── Discuss API contract for integration
└── Plan Week 2 priorities
```

---

## Week 2: Core Features

### Week 2 Goals

```
WEEK 2 SUCCESS CRITERIA

Omar (Android App):
├── ✓ Complete detection screen with live preview
├── ✓ Detection overlay showing bounding boxes
├── ✓ Background detection service (can run while screen off)
├── ✓ Detection history screen
├── ✓ Settings screen with API configuration
└── ✓ Offline storage with Room database

Hamza (Dashboard):
├── ✓ Dashboard project scaffolded with Astro + React
├── ✓ Login page working with backend
├── ✓ Dashboard home with statistics cards
├── ✓ Pothole list view with table
├── ✓ Interactive map showing pothole locations
└── ✓ Pothole detail page with image
```

---

### Day 8 (Week 2)

#### Omar - Detection Screen UI

```
TIME: 2-3 hours
FOCUS: Build detection screen with Jetpack Compose

TASKS:
├── 1. Create DetectionScreen composable (1 hour)
│   ├── Camera preview taking full screen
│   ├── Detection overlay layer
│   │   ├── Draw bounding boxes on detections
│   │   ├── Show confidence percentage
│   │   └── Animate detection highlights
│   │
│   ├── Status bar overlay
│   │   ├── GPS status indicator
│   │   ├── Detection count
│   │   └── Recording/paused state
│   │
│   └── Control buttons
│       ├── Start/Stop detection toggle
│       ├── Settings button
│       └── History button
│
├── 2. Create DetectionViewModel (45 min)
│   ├── State: isDetecting, detections, location, stats
│   ├── Collect location updates
│   ├── Handle detection results
│   ├── Manage detection on/off state
│   └── Track session statistics
│
├── 3. Implement bounding box overlay (45 min)
│   ├── Create Canvas composable for drawing
│   ├── Convert detection coordinates to screen coordinates
│   ├── Draw rectangles with labels
│   ├── Handle different screen sizes/orientations
│   └── Add animation for new detections
│
├── 4. Create detection status indicators (15 min)
│   ├── GPS accuracy indicator (green/yellow/red)
│   ├── Detection active indicator
│   └── Pending upload count badge
│
└── 5. Test detection UI (15 min)
    ├── Run on physical device
    ├── Verify overlay aligns with camera
    ├── Test orientation changes
    └── Verify performance is smooth

DELIVERABLE:
└── Detection screen with live overlay
    ├── Camera preview
    ├── Bounding box visualization
    └── Status indicators
```

#### Hamza - Dashboard Project Setup

```
TIME: 2-3 hours
FOCUS: Initialize Astro + React dashboard project

TASKS:
├── 1. Create Astro project (30 min)
│   ├── Run: npm create astro@latest dashboard
│   ├── Choose: Empty template
│   ├── Install React integration: npx astro add react
│   ├── Install Tailwind: npx astro add tailwind
│   └── Verify project runs: npm run dev
│
├── 2. Configure project structure (30 min)
│   ├── Create directory structure:
│   │   ├── src/pages/
│   │   ├── src/components/react/
│   │   ├── src/components/astro/
│   │   ├── src/layouts/
│   │   ├── src/lib/
│   │   └── src/styles/
│   │
│   └── Configure astro.config.mjs for SSR
│
├── 3. Install additional dependencies (15 min)
│   ├── leaflet + react-leaflet (maps)
│   ├── @tanstack/react-table (data tables)
│   ├── chart.js + react-chartjs-2 (charts)
│   ├── lucide-react (icons)
│   └── swr (data fetching)
│
├── 4. Create base layout (45 min)
│   ├── Create Layout.astro with HTML structure
│   ├── Create DashboardLayout.astro
│   │   ├── Sidebar navigation
│   │   ├── Header with user info
│   │   └── Main content area
│   │
│   └── Style with Tailwind
│
├── 5. Create API client (30 min)
│   ├── Create src/lib/api.ts
│   ├── Configure base URL from env
│   ├── Add auth token handling
│   ├── Create typed fetch wrapper
│   └── Handle errors consistently
│
└── 6. Test setup (15 min)
    ├── Verify page renders
    ├── Verify Tailwind styles work
    ├── Verify React islands hydrate
    └── Test API client connection

DELIVERABLE:
└── Dashboard project initialized
    ├── Astro + React + Tailwind
    ├── Base layout with sidebar
    └── API client configured
```

---

### Day 9 (Week 2)

#### Omar - Room Database Integration

```
TIME: 2-3 hours
FOCUS: Implement offline storage for pending uploads

TASKS:
├── 1. Create Room database setup (45 min)
│   ├── Create AppDatabase class
│   ├── Define PendingUpload entity
│   │   ├── id: Long (auto-generate)
│   │   ├── imagePath: String (local file path)
│   │   ├── latitude: Double
│   │   ├── longitude: Double
│   │   ├── confidence: Float
│   │   ├── detectedAt: Long (timestamp)
│   │   ├── uploadStatus: String (pending/uploading/failed)
│   │   └── retryCount: Int
│   │
│   ├── Create PendingUploadDao with queries
│   │   ├── insert()
│   │   ├── getAll()
│   │   ├── getPending()
│   │   ├── updateStatus()
│   │   └── delete()
│   │
│   └── Configure database version and migrations
│
├── 2. Create local image storage (30 min)
│   ├── Create ImageStorage helper class
│   ├── Save detection image to internal storage
│   ├── Generate unique filename
│   ├── Compress image (JPEG, 80% quality)
│   └── Return file path
│
├── 3. Integrate with detection flow (45 min)
│   ├── When detection occurs:
│   │   ├── Crop detection region from frame
│   │   ├── Save image locally
│   │   ├── Create PendingUpload record
│   │   └── Trigger upload worker
│   │
│   └── Handle storage errors gracefully
│
├── 4. Create repository layer (30 min)
│   ├── Create PotholeRepository class
│   ├── Implement savePendingDetection()
│   ├── Implement getPendingUploads()
│   ├── Abstract database access
│   └── Inject via Hilt
│
└── 5. Test offline storage (15 min)
    ├── Trigger detection without network
    ├── Verify record saved to Room
    ├── Verify image saved locally
    └── Check file sizes are reasonable

DELIVERABLE:
└── Offline storage working
    ├── Detections saved locally
    ├── Images stored on device
    └── Ready for background upload
```

#### Hamza - Login Page

```
TIME: 2-3 hours
FOCUS: Implement login page with authentication

TASKS:
├── 1. Create login page (1 hour)
│   ├── Create src/pages/login.astro
│   ├── Create LoginForm React component
│   │   ├── Email input field
│   │   ├── Password input field
│   │   ├── Remember me checkbox
│   │   ├── Submit button
│   │   └── Error message display
│   │
│   ├── Style with Tailwind (clean, professional)
│   └── Add form validation
│
├── 2. Implement authentication logic (45 min)
│   ├── Create src/lib/auth.ts
│   ├── Implement login function
│   │   ├── Call POST /api/auth/login
│   │   ├── Store token in localStorage
│   │   └── Handle errors
│   │
│   ├── Implement logout function
│   ├── Implement isAuthenticated check
│   └── Implement getToken function
│
├── 3. Create auth context/state (30 min)
│   ├── Create AuthContext for React
│   ├── Store user info and token
│   ├── Provide login/logout functions
│   └── Persist across page navigation
│
├── 4. Add route protection (30 min)
│   ├── Create middleware for protected pages
│   ├── Redirect to login if not authenticated
│   ├── Redirect to dashboard after login
│   └── Handle expired tokens
│
└── 5. Test login flow (15 min)
    ├── Test successful login
    ├── Test invalid credentials
    ├── Test redirect after login
    ├── Test logout
    └── Test protected route redirect

DELIVERABLE:
└── Working login system
    ├── Login form with validation
    ├── Token stored in browser
    └── Protected routes redirect
```

---

### Day 10 (Week 2)

#### Omar - WorkManager Upload

```
TIME: 2-3 hours
FOCUS: Implement background upload with WorkManager

TASKS:
├── 1. Create UploadWorker class (1.5 hours)
│   ├── Extend CoroutineWorker
│   ├── In doWork():
│   │   ├── Get pending uploads from Room
│   │   ├── For each pending:
│   │   │   ├── Read image file
│   │   │   ├── Make API request
│   │   │   ├── Handle response
│   │   │   ├── Update status in Room
│   │   │   └── Delete local file on success
│   │   │
│   │   └── Return Result.success/retry/failure
│   │
│   ├── Handle network errors with retry
│   ├── Implement exponential backoff
│   └── Respect retry limits
│
├── 2. Configure WorkManager (30 min)
│   ├── Create WorkManager configuration
│   ├── Define constraints:
│   │   ├── RequiredNetworkType.CONNECTED
│   │   └── Optional: RequiresBatteryNotLow
│   │
│   ├── Configure retry policy
│   └── Initialize in Application class
│
├── 3. Create upload trigger mechanism (30 min)
│   ├── Enqueue upload work when detection saved
│   ├── Use unique work (replace if exists)
│   ├── Handle immediate vs deferred upload
│   └── Observe work status in UI
│
├── 4. Create API client (30 min)
│   ├── Create ApiService with Ktor
│   ├── Configure base URL
│   ├── Implement uploadPothole endpoint
│   │   ├── Multipart form data
│   │   ├── Include image file
│   │   └── Include metadata (lat, lng, confidence)
│   │
│   └── Handle authentication token
│
└── 5. Test upload flow (15 min)
    ├── Save detection while offline
    ├── Enable network
    ├── Verify upload completes
    ├── Verify local data cleaned up
    └── Test retry on failure

DELIVERABLE:
└── Background upload working
    ├── Uploads when network available
    ├── Retries on failure
    └── Cleans up after success
```

#### Hamza - Dashboard Home Page

```
TIME: 2-3 hours
FOCUS: Create dashboard home with statistics

TASKS:
├── 1. Create dashboard home page (30 min)
│   ├── Create src/pages/index.astro
│   ├── Use DashboardLayout
│   ├── Add page title and header
│   └── Create grid layout for widgets
│
├── 2. Create StatsCards React component (1 hour)
│   ├── Fetch stats from GET /api/stats
│   ├── Create stat card component:
│   │   ├── Icon
│   │   ├── Value (large number)
│   │   ├── Label
│   │   └── Change indicator (optional)
│   │
│   ├── Display cards:
│   │   ├── Total Potholes
│   │   ├── Unverified (pending review)
│   │   ├── Verified
│   │   ├── Repaired
│   │   └── Active Vehicles
│   │
│   └── Add loading skeleton
│
├── 3. Create RecentActivity component (45 min)
│   ├── Fetch recent potholes
│   ├── Display list with:
│   │   ├── Location (reverse geocode or coords)
│   │   ├── Time (relative: "2 hours ago")
│   │   ├── Status badge
│   │   └── Vehicle name
│   │
│   └── Link to pothole detail
│
├── 4. Create mini map widget (30 min)
│   ├── Use Leaflet with React
│   ├── Show recent pothole markers
│   ├── Center on last detection
│   └── Link to full map view
│
└── 5. Test dashboard (15 min)
    ├── Verify stats load correctly
    ├── Verify recent activity shows
    ├── Verify map displays
    └── Test responsive layout

DELIVERABLE:
└── Dashboard home page
    ├── Statistics cards
    ├── Recent activity list
    └── Mini map preview
```

---

### Day 11 (Week 2)

#### Omar - History Screen

```
TIME: 2-3 hours
FOCUS: Create detection history screen

TASKS:
├── 1. Create HistoryScreen composable (1 hour)
│   ├── List of past detections
│   ├── Each item shows:
│   │   ├── Thumbnail image
│   │   ├── Date/time
│   │   ├── Location (coords or address)
│   │   ├── Confidence score
│   │   └── Upload status (pending/uploaded/failed)
│   │
│   ├── Sort by date (newest first)
│   ├── Add pull-to-refresh
│   └── Handle empty state
│
├── 2. Create HistoryViewModel (30 min)
│   ├── Load from Room database
│   ├── Combine with upload status
│   ├── Support refresh
│   └── Implement delete functionality
│
├── 3. Create history item composable (30 min)
│   ├── Row layout with image and details
│   ├── Status indicator chip
│   ├── Tap to view details
│   └── Swipe to delete (optional)
│
├── 4. Implement image loading (30 min)
│   ├── Load thumbnail from local storage
│   ├── Use Coil for image loading
│   ├── Handle missing images gracefully
│   └── Show placeholder for uploaded items
│
└── 5. Add navigation (30 min)
    ├── Add History button to DetectionScreen
    ├── Implement navigation between screens
    ├── Handle back navigation
    └── Test navigation flow

DELIVERABLE:
└── History screen complete
    ├── Shows all detections
    ├── Upload status visible
    └── Navigation working
```

#### Hamza - Pothole List Page

```
TIME: 2-3 hours
FOCUS: Create pothole list view with data table

TASKS:
├── 1. Create pothole list page (30 min)
│   ├── Create src/pages/potholes/index.astro
│   ├── Use DashboardLayout
│   ├── Add page header with title
│   └── Add filter controls section
│
├── 2. Create PotholeTable React component (1.5 hours)
│   ├── Use TanStack Table
│   ├── Define columns:
│   │   ├── ID
│   │   ├── Location (lat, lng)
│   │   ├── Confidence
│   │   ├── Status (with badge)
│   │   ├── Vehicle
│   │   ├── Detected Date
│   │   └── Actions (view, update status)
│   │
│   ├── Implement features:
│   │   ├── Sorting (click column header)
│   │   ├── Pagination
│   │   ├── Status filter dropdown
│   │   └── Search (optional)
│   │
│   └── Style with Tailwind
│
├── 3. Create StatusBadge component (15 min)
│   ├── Color-coded by status:
│   │   ├── unverified: yellow
│   │   ├── verified: blue
│   │   ├── repaired: green
│   │   └── false_positive: red
│   │
│   └── Consistent styling
│
├── 4. Implement data fetching (30 min)
│   ├── Use SWR for data fetching
│   ├── Implement pagination (server-side)
│   ├── Handle loading state
│   └── Handle error state
│
└── 5. Test table functionality (15 min)
    ├── Verify data loads
    ├── Test sorting
    ├── Test pagination
    └── Test filtering

DELIVERABLE:
└── Pothole list page
    ├── Data table with sorting
    ├── Pagination
    └── Status filtering
```

---

### Day 12 (Week 2)

#### Omar - Settings Screen

```
TIME: 2-3 hours
FOCUS: Create settings screen with configuration options

TASKS:
├── 1. Create SettingsScreen composable (1 hour)
│   ├── Section: Server Configuration
│   │   ├── API URL input
│   │   ├── Connection status indicator
│   │   └── Test connection button
│   │
│   ├── Section: Detection Settings
│   │   ├── Confidence threshold slider
│   │   ├── Auto-start detection toggle
│   │   └── Sound/vibration on detection toggle
│   │
│   ├── Section: Account (if logged in)
│   │   ├── Vehicle info display
│   │   ├── Upload stats
│   │   └── Logout button
│   │
│   └── Section: About
│       ├── App version
│       └── Clear local data button
│
├── 2. Create SettingsViewModel (30 min)
│   ├── Load settings from DataStore
│   ├── Save settings on change
│   ├── Implement connection test
│   └── Implement logout
│
├── 3. Implement DataStore for preferences (30 min)
│   ├── Create PreferencesDataStore
│   ├── Define preference keys
│   ├── Implement read/write functions
│   └── Provide via Hilt
│
├── 4. Implement API connection test (30 min)
│   ├── Call /health endpoint
│   ├── Show success/failure
│   ├── Display latency
│   └── Handle timeout
│
└── 5. Add login functionality (30 min)
    ├── Create simple login dialog
    ├── Store JWT token securely
    ├── Link vehicle to app
    └── Display login status in settings

DELIVERABLE:
└── Settings screen complete
    ├── Configuration options
    ├── Account management
    └── Connection testing
```

#### Hamza - Interactive Map Page

```
TIME: 2-3 hours
FOCUS: Create full-page map with pothole markers

TASKS:
├── 1. Create map page (30 min)
│   ├── Create src/pages/map.astro
│   ├── Full-height map layout
│   ├── Add floating controls panel
│   └── Configure for client-side only (no SSR)
│
├── 2. Create PotholeMap React component (1.5 hours)
│   ├── Initialize Leaflet map
│   │   ├── Center on default location (Jordan)
│   │   ├── Add OpenStreetMap tiles
│   │   └── Configure zoom levels
│   │
│   ├── Load pothole data
│   ├── Create markers:
│   │   ├── Custom marker icon by status
│   │   ├── Cluster markers when zoomed out
│   │   └── Popup with pothole info
│   │
│   ├── Implement popup:
│   │   ├── Small image thumbnail
│   │   ├── Confidence
│   │   ├── Status
│   │   ├── Date
│   │   └── "View Details" link
│   │
│   └── Handle marker click
│
├── 3. Add map controls (30 min)
│   ├── Status filter checkboxes
│   ├── Date range filter (optional)
│   ├── Locate me button
│   └── Refresh data button
│
├── 4. Implement marker clustering (15 min)
│   ├── Install Leaflet.markercluster
│   ├── Configure cluster options
│   └── Style cluster icons
│
└── 5. Test map functionality (15 min)
    ├── Verify markers display
    ├── Test popup interaction
    ├── Test filtering
    └── Test on mobile viewport

DELIVERABLE:
└── Interactive map page
    ├── All potholes on map
    ├── Clickable markers with popups
    └── Filtering by status
```

---

### Day 13 (Week 2)

#### Omar - Detection Service

```
TIME: 2-3 hours
FOCUS: Create foreground service for continuous detection

TASKS:
├── 1. Create DetectionService (1.5 hours)
│   ├── Extend Service class
│   ├── Run as foreground service with notification
│   ├── Initialize camera without preview
│   ├── Run detection loop
│   └── Handle service lifecycle
│
├── 2. Create service notification (30 min)
│   ├── Create notification channel
│   ├── Build persistent notification:
│   │   ├── Title: "Pothole Detection Active"
│   │   ├── Content: "X potholes detected this session"
│   │   ├── Stop action button
│   │   └── Open app action
│   │
│   └── Update notification on detection
│
├── 3. Implement service controls (30 min)
│   ├── Start service from DetectionScreen
│   ├── Stop service from notification or app
│   ├── Handle app in background
│   └── Bind service to activity for status updates
│
├── 4. Handle battery and resource management (15 min)
│   ├── Reduce detection frequency when battery low
│   ├── Respect battery optimization settings
│   └── Clean up resources properly
│
└── 5. Test service behavior (15 min)
    ├── Start detection, minimize app
    ├── Verify detection continues
    ├── Verify notification updates
    └── Test stop functionality

DELIVERABLE:
└── Background detection service
    ├── Runs with notification
    ├── Continues when app minimized
    └── Proper resource cleanup
```

#### Hamza - Pothole Detail Page

```
TIME: 2-3 hours
FOCUS: Create pothole detail view with status management

TASKS:
├── 1. Create pothole detail page (45 min)
│   ├── Create src/pages/potholes/[id].astro
│   ├── Fetch pothole data in getStaticPaths (or server)
│   ├── Layout with image and info sections
│   └── Back navigation
│
├── 2. Create detail view content (1 hour)
│   ├── Large image display
│   │   ├── Load from S3 (signed URL)
│   │   ├── Lightbox on click
│   │   └── Handle missing image
│   │
│   ├── Information panel:
│   │   ├── Status with badge
│   │   ├── Confidence score
│   │   ├── GPS coordinates
│   │   ├── Detected date/time
│   │   ├── Vehicle name
│   │   └── Confirmation count
│   │
│   └── Mini map showing location
│
├── 3. Create status update form (45 min)
│   ├── Status dropdown select
│   ├── Notes textarea (optional)
│   ├── Save button
│   ├── Call PATCH /api/potholes/:id/status
│   └── Show success/error feedback
│
├── 4. Add action buttons (15 min)
│   ├── Open in Google Maps
│   ├── Copy coordinates
│   ├── Delete (admin only)
│   └── Confirmation modal for delete
│
└── 5. Test detail page (15 min)
    ├── Navigate from list
    ├── Verify image loads
    ├── Test status update
    └── Test navigation buttons

DELIVERABLE:
└── Pothole detail page
    ├── Full pothole information
    ├── Image display
    └── Status management
```

---

### Day 14 (Week 2)

#### Omar - UI Polish & Navigation

```
TIME: 2-3 hours
FOCUS: Polish UI and implement complete navigation

TASKS:
├── 1. Create main navigation structure (45 min)
│   ├── Bottom navigation bar:
│   │   ├── Detection (camera icon)
│   │   ├── History (list icon)
│   │   └── Settings (gear icon)
│   │
│   ├── Implement Navigation Compose
│   └── Handle navigation state
│
├── 2. Polish Detection Screen (30 min)
│   ├── Improve bounding box visualization
│   ├── Add detection animation/feedback
│   ├── Improve status indicators
│   └── Test various lighting conditions
│
├── 3. Polish History Screen (30 min)
│   ├── Add empty state illustration
│   ├── Improve list item design
│   ├── Add delete confirmation
│   └── Improve upload status display
│
├── 4. Polish Settings Screen (30 min)
│   ├── Improve section headers
│   ├── Add icons to settings items
│   ├── Improve input validation
│   └── Add save confirmation
│
├── 5. Create app theme (30 min)
│   ├── Define color scheme
│   ├── Configure Material 3 theme
│   ├── Support light/dark mode (optional)
│   └── Apply consistently across screens
│
└── 6. Test complete app flow (15 min)
    ├── Fresh install experience
    ├── Permission flow
    ├── Navigation between screens
    └── Various device sizes

DELIVERABLE:
└── Polished app UI
    ├── Complete navigation
    ├── Consistent theming
    └── Smooth user experience
```

#### Hamza - Dashboard Polish & Vehicle Management

```
TIME: 2-3 hours
FOCUS: Polish dashboard and add vehicle management

TASKS:
├── 1. Create vehicle management page (1 hour)
│   ├── Create src/pages/vehicles/index.astro
│   ├── Vehicle list table:
│   │   ├── Name
│   │   ├── Serial number
│   │   ├── Status (active/inactive)
│   │   ├── Last active time
│   │   ├── Detection count
│   │   └── Actions
│   │
│   ├── Add new vehicle form
│   └── Edit vehicle modal
│
├── 2. Polish sidebar navigation (30 min)
│   ├── Highlight active page
│   ├── Add icons to menu items
│   ├── Collapsible on mobile
│   └── User info at bottom
│
├── 3. Polish header (30 min)
│   ├── Show current page title
│   ├── User dropdown menu:
│   │   ├── Profile (optional)
│   │   ├── Settings
│   │   └── Logout
│   │
│   └── Notification bell (optional)
│
├── 4. Add loading states (30 min)
│   ├── Skeleton loaders for cards
│   ├── Table loading state
│   ├── Map loading state
│   └── Consistent across all components
│
├── 5. Add error handling (15 min)
│   ├── Error boundary component
│   ├── API error display
│   └── Retry functionality
│
└── 6. Test responsive design (15 min)
    ├── Desktop layout
    ├── Tablet layout
    ├── Mobile layout
    └── Fix any issues

DELIVERABLE:
└── Polished dashboard
    ├── Vehicle management
    ├── Responsive design
    └── Loading/error states
```

---

### Week 2 Checkpoint

```
END OF WEEK 2 REVIEW

Omar - What Should Be Complete:
├── ✅ Detection screen with live overlay
├── ✅ Room database for offline storage
├── ✅ WorkManager background upload
├── ✅ History screen showing detections
├── ✅ Settings screen with configuration
├── ✅ Foreground detection service
└── ✅ Polished navigation and UI

Hamza - What Should Be Complete:
├── ✅ Dashboard project with layout
├── ✅ Login page and authentication
├── ✅ Dashboard home with statistics
├── ✅ Pothole list with data table
├── ✅ Interactive map with markers
├── ✅ Pothole detail page
└── ✅ Vehicle management

WEEK 2 SYNC MEETING (30 min):
├── Demo complete Android app
├── Demo complete dashboard
├── Test API integration points
├── Identify integration issues
├── Plan Week 3 integration work
└── Discuss deployment timeline
```

---

## Week 3: Integration

### Week 3 Goals

```
WEEK 3 SUCCESS CRITERIA

Omar (Integration & Testing):
├── ✓ Mobile app fully connected to backend API
├── ✓ Image upload to S3 working
├── ✓ Deduplication working (local + server)
├── ✓ Error handling and retry logic robust
├── ✓ Performance optimization complete
└── ✓ Testing on multiple devices

Hamza (Advanced Features & Pre-Deploy):
├── ✓ Dashboard charts and analytics
├── ✓ Export functionality (CSV)
├── ✓ User management (if needed)
├── ✓ API rate limiting and security
├── ✓ Docker images built and tested
└── ✓ Staging deployment to Hetzner
```

---

### Day 15 (Week 3)

#### Omar - API Integration

```
TIME: 2-3 hours
FOCUS: Connect app to backend API

TASKS:
├── 1. Configure API client for production (45 min)
│   ├── Update base URL configuration
│   ├── Implement proper auth token handling
│   ├── Add request/response logging
│   └── Configure timeouts
│
├── 2. Implement vehicle registration (45 min)
│   ├── On first app launch:
│   │   ├── Generate device identifier
│   │   ├── Register with backend
│   │   └── Store vehicle ID and token
│   │
│   └── Handle registration errors
│
├── 3. Update upload worker for real API (45 min)
│   ├── Use correct endpoint format
│   ├── Include auth token
│   ├── Send image as multipart
│   ├── Handle server responses
│   └── Handle deduplication response
│
├── 4. Test end-to-end upload (30 min)
│   ├── Detect pothole
│   ├── Verify saved locally
│   ├── Verify uploaded to server
│   ├── Verify appears in dashboard
│   └── Verify image in S3
│
└── 5. Fix any integration issues (15 min)
    ├── Debug API errors
    ├── Fix data format mismatches
    └── Update as needed

DELIVERABLE:
└── App connected to backend
    ├── Registration working
    ├── Upload working
    └── Data visible in dashboard
```

#### Hamza - Analytics & Charts

```
TIME: 2-3 hours
FOCUS: Add analytics charts to dashboard

TASKS:
├── 1. Create analytics API endpoints (45 min)
│   ├── GET /api/analytics/timeline
│   │   └── Potholes by day/week/month
│   │
│   ├── GET /api/analytics/by-status
│   │   └── Count by status
│   │
│   ├── GET /api/analytics/by-vehicle
│   │   └── Detections per vehicle
│   │
│   └── Implement efficient queries
│
├── 2. Create AnalyticsChart components (1 hour)
│   ├── Line chart: Detections over time
│   │   ├── Use Chart.js
│   │   ├── Selectable time range
│   │   └── Responsive sizing
│   │
│   ├── Pie chart: Status distribution
│   │   ├── Color by status
│   │   └── Show percentages
│   │
│   └── Bar chart: Top vehicles
│
├── 3. Add charts to dashboard home (30 min)
│   ├── Rearrange layout for charts
│   ├── Add chart section
│   └── Handle loading states
│
├── 4. Create dedicated analytics page (30 min)
│   ├── Create src/pages/analytics.astro
│   ├── More detailed charts
│   ├── Date range selector
│   └── Export data option
│
└── 5. Test analytics (15 min)
    ├── Verify data accuracy
    ├── Test with various data
    └── Test responsive behavior

DELIVERABLE:
└── Dashboard analytics
    ├── Timeline chart
    ├── Status distribution
    └── Vehicle statistics
```

---

### Day 16 (Week 3)

#### Omar - Error Handling & Retry

```
TIME: 2-3 hours
FOCUS: Robust error handling and retry logic

TASKS:
├── 1. Improve upload retry logic (1 hour)
│   ├── Implement exponential backoff:
│   │   ├── 1st retry: 30 seconds
│   │   ├── 2nd retry: 2 minutes
│   │   ├── 3rd retry: 10 minutes
│   │   ├── 4th retry: 1 hour
│   │   └── Max 5 retries
│   │
│   ├── Store retry count in Room
│   ├── Show retry status in UI
│   └── Allow manual retry from history
│
├── 2. Handle specific error types (45 min)
│   ├── Network unavailable → queue for later
│   ├── Server error (5xx) → retry later
│   ├── Client error (4xx) → mark as failed
│   ├── Timeout → retry with longer timeout
│   └── Auth error (401) → re-authenticate
│
├── 3. Add user feedback for errors (30 min)
│   ├── Toast messages for upload status
│   ├── Error details in history item
│   ├── Snackbar for recoverable errors
│   └── Dialog for critical errors
│
├── 4. Implement connectivity monitoring (30 min)
│   ├── Monitor network state
│   ├── Show offline indicator in UI
│   ├── Queue operations when offline
│   └── Resume when online
│
└── 5. Test error scenarios (15 min)
    ├── Simulate network errors
    ├── Test retry behavior
    ├── Test offline→online transition
    └── Test auth token expiry

DELIVERABLE:
└── Robust error handling
    ├── Automatic retry with backoff
    ├── User feedback
    └── Offline support
```

#### Hamza - Export & Rate Limiting

```
TIME: 2-3 hours
FOCUS: Add export functionality and API security

TASKS:
├── 1. Implement CSV export (1 hour)
│   ├── Create export endpoint: GET /api/potholes/export
│   ├── Generate CSV with columns:
│   │   ├── ID, Latitude, Longitude
│   │   ├── Confidence, Status
│   │   ├── Vehicle, Date
│   │   └── Image URL
│   │
│   ├── Support date range filter
│   ├── Stream large datasets
│   └── Set proper content headers
│
├── 2. Add export button to dashboard (30 min)
│   ├── Button on pothole list page
│   ├── Date range selector modal
│   ├── Download progress indicator
│   └── Handle large exports
│
├── 3. Implement API rate limiting (45 min)
│   ├── Install @fastify/rate-limit
│   ├── Configure limits:
│   │   ├── General API: 100 requests/minute
│   │   ├── Auth: 5 requests/minute
│   │   └── Upload: 30 requests/minute
│   │
│   ├── Return proper 429 status
│   └── Add rate limit headers
│
├── 4. Add API security headers (15 min)
│   ├── Helmet middleware
│   ├── CORS configuration
│   └── Content-Security-Policy
│
└── 5. Test security features (30 min)
    ├── Test rate limiting triggers
    ├── Test CORS from dashboard
    ├── Verify headers in responses
    └── Test export with large data

DELIVERABLE:
└── Export and security
    ├── CSV export working
    ├── Rate limiting active
    └── Security headers set
```

---

### Day 17 (Week 3)

#### Omar - Performance Optimization

```
TIME: 2-3 hours
FOCUS: Optimize app performance

TASKS:
├── 1. Optimize inference performance (1 hour)
│   ├── Profile current inference time
│   ├── Try different delegates:
│   │   ├── NNAPI (default)
│   │   ├── GPU delegate
│   │   └── Compare performance
│   │
│   ├── Optimize frame skipping:
│   │   ├── Adaptive based on device capability
│   │   └── Skip more when battery low
│   │
│   └── Reduce memory allocations
│
├── 2. Optimize camera pipeline (45 min)
│   ├── Reduce preview resolution if not needed
│   ├── Optimize image conversion
│   ├── Reuse buffers
│   └── Profile memory usage
│
├── 3. Optimize battery usage (30 min)
│   ├── Measure current battery drain
│   ├── Reduce GPS update frequency when stationary
│   ├── Reduce detection frequency when battery low
│   └── Release camera when app backgrounded (if service not active)
│
├── 4. Optimize storage (30 min)
│   ├── Compress images efficiently
│   ├── Clean up old uploaded records
│   ├── Limit local storage size
│   └── Implement cache cleanup
│
└── 5. Profile and measure (15 min)
    ├── Use Android Profiler
    ├── Measure CPU usage
    ├── Measure memory usage
    ├── Measure battery impact
    └── Document results

DELIVERABLE:
└── Optimized performance
    ├── <100ms inference time
    ├── Reasonable battery usage
    └── Efficient storage
```

#### Hamza - Docker Build & Test

```
TIME: 2-3 hours
FOCUS: Build and test Docker images locally

TASKS:
├── 1. Finalize Dockerfiles (30 min)
│   ├── Review backend Dockerfile
│   ├── Review dashboard Dockerfile
│   ├── Ensure multi-stage builds working
│   └── Minimize image sizes
│
├── 2. Test docker-compose locally (45 min)
│   ├── Run: docker compose up --build
│   ├── Verify all services start
│   ├── Check logs for errors
│   ├── Test inter-service communication
│   └── Test database connectivity
│
├── 3. Test production compose file (45 min)
│   ├── Run with production config
│   ├── Verify environment variables
│   ├── Test health checks
│   └── Test restart behavior
│
├── 4. Push images to registry (30 min)
│   ├── Create Docker Hub account (or use GitHub Container Registry)
│   ├── Tag images appropriately
│   ├── Push images
│   └── Document image names
│
└── 5. Document deployment process (30 min)
    ├── Write deployment commands
    ├── Document environment variables
    ├── Create deployment checklist
    └── Note any gotchas

DELIVERABLE:
└── Docker images ready
    ├── Images tested locally
    ├── Images pushed to registry
    └── Deployment documented
```

---

### Day 18 (Week 3)

#### Omar - Multi-Device Testing

```
TIME: 2-3 hours
FOCUS: Test on multiple Android devices

TASKS:
├── 1. Test on different Android versions (1 hour)
│   ├── Android 9 (API 28) - minimum
│   ├── Android 11 (API 30)
│   ├── Android 13 (API 33)
│   └── Android 14 (API 34) - if available
│   │
│   ├── Check for:
│   │   ├── Camera permission flow
│   │   ├── Location permission flow
│   │   ├── Background service behavior
│   │   └── UI rendering
│   │
│   └── Fix any version-specific issues
│
├── 2. Test on different screen sizes (45 min)
│   ├── Small phone (~5")
│   ├── Large phone (~6.5")
│   ├── Tablet (if available)
│   └── Verify UI adapts correctly
│
├── 3. Test in various conditions (45 min)
│   ├── Good lighting
│   ├── Low lighting
│   ├── Moving vehicle (simulate)
│   ├── No network
│   └── Poor GPS signal
│
├── 4. Fix discovered issues (30 min)
│   ├── Document issues found
│   ├── Prioritize fixes
│   └── Implement critical fixes
│
└── 5. Create testing report (15 min)
    ├── Devices tested
    ├── Issues found
    ├── Known limitations
    └── Recommendations

DELIVERABLE:
└── Multi-device validation
    ├── Tested on 3+ devices
    ├── Version compatibility confirmed
    └── Issues documented
```

#### Hamza - Staging Deployment

```
TIME: 2-3 hours
FOCUS: Deploy to Hetzner staging environment

TASKS:
├── 1. Prepare server (30 min)
│   ├── SSH into Hetzner server
│   ├── Verify Docker is installed
│   ├── Create project directory
│   └── Clone repository (or copy files)
│
├── 2. Configure environment (30 min)
│   ├── Create .env file with production values
│   ├── Generate secure JWT secret
│   ├── Configure database password
│   └── Add AWS credentials
│
├── 3. Deploy application (45 min)
│   ├── Run docker compose pull (if using registry)
│   ├── Or run docker compose build
│   ├── Run docker compose up -d
│   ├── Run database migrations
│   └── Verify all containers running
│
├── 4. Configure Nginx (30 min)
│   ├── Create Nginx site configs
│   ├── Enable sites
│   ├── Test config: nginx -t
│   └── Reload Nginx
│
├── 5. Test staging deployment (30 min)
│   ├── Test API endpoints via IP
│   ├── Test dashboard access
│   ├── Verify database operations
│   └── Test image upload to S3
│
└── 6. Document staging URL (15 min)
    ├── Note IP address
    ├── Document test credentials
    └── Share with Omar for testing

DELIVERABLE:
└── Staging environment running
    ├── API accessible
    ├── Dashboard accessible
    └── Database populated
```

---

### Day 19 (Week 3)

#### Omar - Integration Testing

```
TIME: 2-3 hours
FOCUS: Test complete flow against staging server

TASKS:
├── 1. Configure app for staging (15 min)
│   ├── Update API URL to staging server
│   ├── Build debug APK
│   └── Install on test device
│
├── 2. Test complete detection flow (1 hour)
│   ├── Launch app
│   ├── Grant permissions
│   ├── Start detection
│   ├── Detect test pothole (use image on screen)
│   ├── Verify local save
│   ├── Verify upload completes
│   └── Check dashboard shows new pothole
│
├── 3. Test deduplication (45 min)
│   ├── Detect same location twice quickly
│   ├── Verify local dedup works
│   ├── Detect same location after 60+ seconds
│   ├── Verify server dedup works
│   └── Check confirmation count increases
│
├── 4. Test offline behavior (30 min)
│   ├── Enable airplane mode
│   ├── Detect potholes
│   ├── Verify saved locally
│   ├── Disable airplane mode
│   ├── Verify uploads complete
│   └── Check all appear in dashboard
│
├── 5. Test error recovery (30 min)
│   ├── Kill server mid-upload
│   ├── Verify retry occurs
│   ├── Simulate auth token expiry
│   └── Verify recovery works
│
└── 6. Document test results (15 min)
    ├── Test cases passed
    ├── Issues found
    └── Ready for production?

DELIVERABLE:
└── Integration verified
    ├── End-to-end flow working
    ├── Deduplication working
    └── Offline/retry working
```

#### Hamza - SSL & Domain Setup

```
TIME: 2-3 hours
FOCUS: Configure domain and SSL certificates

TASKS:
├── 1. Configure Cloudflare DNS (30 min)
│   ├── Add A record for api subdomain
│   ├── Add A record for dashboard subdomain
│   ├── Set proxy to DNS only (gray cloud)
│   └── Verify DNS propagation
│
├── 2. Obtain SSL certificates (45 min)
│   ├── Install certbot if not installed
│   ├── Run certbot for API domain
│   ├── Run certbot for dashboard domain
│   ├── Verify certificates issued
│   └── Test auto-renewal
│
├── 3. Update Nginx for SSL (45 min)
│   ├── Update server blocks with SSL config
│   ├── Add HTTP → HTTPS redirect
│   ├── Enable SSL certificates
│   ├── Configure SSL protocols and ciphers
│   └── Reload Nginx
│
├── 4. Enable Cloudflare proxy (15 min)
│   ├── Change to orange cloud (proxied)
│   ├── Set SSL mode to Full (strict)
│   └── Enable Always HTTPS
│
├── 5. Test HTTPS access (30 min)
│   ├── Test https://api.domain.com/health
│   ├── Test https://dashboard.domain.com
│   ├── Verify certificate is valid
│   ├── Test from mobile app
│   └── Verify no mixed content
│
└── 6. Update mobile app URL (15 min)
    ├── Update API URL to production domain
    ├── Build new APK
    └── Test connection

DELIVERABLE:
└── Production domain ready
    ├── SSL certificates active
    ├── HTTPS enforced
    └── Cloudflare protecting
```

---

### Day 20 (Week 3)

#### Omar - Final Bug Fixes

```
TIME: 2-3 hours
FOCUS: Fix remaining bugs and edge cases

TASKS:
├── 1. Review and fix open issues (2 hours)
│   ├── List all known bugs
│   ├── Prioritize by severity
│   ├── Fix critical bugs first
│   ├── Test each fix
│   └── Document any unfixed issues
│
├── 2. Edge case handling (30 min)
│   ├── App killed during detection
│   ├── Permission revoked while running
│   ├── Storage full
│   └── GPS disabled mid-session
│
├── 3. UI edge cases (15 min)
│   ├── Empty states
│   ├── Loading states
│   ├── Error states
│   └── Very long text handling
│
└── 4. Final APK preparation (15 min)
    ├── Update version number
    ├── Build release APK
    ├── Test release build
    └── Prepare for distribution

DELIVERABLE:
└── Release-ready app
    ├── Known bugs fixed
    ├── Edge cases handled
    └── Release APK built
```

#### Hamza - Security & Final Backend Fixes

```
TIME: 2-3 hours
FOCUS: Security hardening and final fixes

TASKS:
├── 1. Security audit (1 hour)
│   ├── Review authentication logic
│   ├── Check for SQL injection (Drizzle should prevent)
│   ├── Verify file upload validation
│   ├── Check authorization on all routes
│   └── Review error messages (don't leak info)
│
├── 2. Input validation review (30 min)
│   ├── All endpoints have schema validation
│   ├── Coordinate ranges validated
│   ├── File size limits enforced
│   └── String lengths limited
│
├── 3. Logging setup (30 min)
│   ├── Configure production logging
│   ├── Log requests (without sensitive data)
│   ├── Log errors with stack traces
│   └── Set up log rotation
│
├── 4. Performance review (30 min)
│   ├── Check database query efficiency
│   ├── Verify indexes are used
│   ├── Check for N+1 queries
│   └── Load test basic endpoints
│
└── 5. Final deployment update (30 min)
    ├── Deploy latest code to production
    ├── Run any new migrations
    ├── Verify all features work
    └── Monitor logs for errors

DELIVERABLE:
└── Production-ready backend
    ├── Security reviewed
    ├── Logging configured
    └── Performance verified
```

---

### Day 21 (Week 3)

#### Omar & Hamza - Integration Day

```
TIME: 2-3 hours each
FOCUS: Final integration testing together

TASKS (Joint):
├── 1. End-to-end testing session (1.5 hours)
│   ├── Fresh app install
│   ├── Registration flow
│   ├── Detection and upload
│   ├── Verify in dashboard
│   ├── Status update in dashboard
│   ├── Verify reflected (if app shows status)
│   └── Test multiple scenarios
│
├── 2. Load testing (30 min)
│   ├── Simulate multiple detections rapidly
│   ├── Upload many images
│   ├── Check server handles load
│   └── Monitor resource usage
│
├── 3. Documentation (30 min each)
│   ├── Omar: Document Android app setup/usage
│   ├── Hamza: Document API endpoints (brief)
│   ├── Both: Screenshot key features
│   └── Create demo video (optional)
│
└── 4. Week 3 review (30 min)
    ├── What's working well
    ├── What needs more work
    ├── Plan Week 4 priorities
    └── Decide on MVP scope

DELIVERABLE:
└── Integrated system verified
    ├── All components working together
    ├── Documentation started
    └── Week 4 plan ready
```

---

### Week 3 Checkpoint

```
END OF WEEK 3 REVIEW

Omar - What Should Be Complete:
├── ✅ App connected to production API
├── ✅ Image upload to S3 working
├── ✅ Deduplication verified
├── ✅ Error handling robust
├── ✅ Multi-device testing done
├── ✅ Performance optimized
└── ✅ Release APK ready

Hamza - What Should Be Complete:
├── ✅ Analytics dashboard with charts
├── ✅ CSV export functionality
├── ✅ Rate limiting active
├── ✅ Docker images tested
├── ✅ Production deployment running
├── ✅ SSL certificates active
└── ✅ Security reviewed

System Status:
├── ✅ App → API → Database flow working
├── ✅ Images stored in S3
├── ✅ Dashboard displays all data
├── ✅ HTTPS everywhere
└── ✅ Ready for final polish
```

---

## Week 4: Polish & Deploy

### Week 4 Goals

```
WEEK 4 SUCCESS CRITERIA

Omar (Final Polish):
├── ✓ All critical bugs fixed
├── ✓ UI fully polished
├── ✓ Demo video/screenshots ready
├── ✓ App documentation complete
├── ✓ APK ready for submission/demo
└── ✓ Presentation preparation support

Hamza (Production & Demo):
├── ✓ Production system stable
├── ✓ Monitoring in place
├── ✓ Backup system working
├── ✓ Dashboard fully polished
├── ✓ Demo data populated
└── ✓ System documentation complete
```

---

### Days 22-28 (Week 4) - Overview

```
WEEK 4 DAILY BREAKDOWN

Day 22 (Both): Bug Bash
├── Fix all remaining bugs
├── Polish UI details
└── Test edge cases

Day 23 (Both): Documentation
├── Omar: Complete app documentation
├── Hamza: Complete system documentation
└── Create setup guides

Day 24 (Both): Demo Preparation
├── Create demo data/scenarios
├── Practice demo flow
└── Create backup plans

Day 25 (Both): Buffer / Final Fixes
├── Address any remaining issues
├── Final testing
└── Prepare presentation materials

Day 26 (Both): Presentation Prep
├── Create slides (if needed)
├── Prepare talking points
└── Practice presentation

Day 27 (Both): Final Review
├── Complete system walkthrough
├── Verify everything works
└── Create handoff documentation

Day 28 (Both): Delivery Day
├── Final deployment check
├── Submit deliverables
└── Celebrate! 🎉
```

---

### Day 22 (Week 4) - Bug Bash

#### Omar - App Bug Fixes

```
TIME: 2-3 hours
FOCUS: Fix remaining app bugs

TASKS:
├── Review all known issues
├── Fix UI glitches
├── Fix crash scenarios
├── Test fixes
└── Build updated APK
```

#### Hamza - Dashboard Bug Fixes

```
TIME: 2-3 hours
FOCUS: Fix remaining dashboard/backend bugs

TASKS:
├── Review all known issues
├── Fix API edge cases
├── Fix dashboard UI issues
├── Deploy fixes
└── Verify in production
```

---

### Day 23 (Week 4) - Documentation

#### Omar - App Documentation

```
TIME: 2-3 hours
FOCUS: Document Android app

TASKS:
├── Installation guide
├── User manual with screenshots
├── Developer setup guide
├── Known limitations
└── Troubleshooting guide
```

#### Hamza - System Documentation

```
TIME: 2-3 hours
FOCUS: Document backend and deployment

TASKS:
├── API documentation (endpoints list)
├── Database schema documentation
├── Deployment guide
├── Maintenance procedures
└── Monitoring guide
```

---

### Day 24 (Week 4) - Demo Preparation

#### Both - Demo Setup

```
TIME: 2-3 hours each
FOCUS: Prepare for demonstration

TASKS:
├── Create demo data (test potholes with images)
├── Prepare demo devices
├── Create demo script/flow
├── Test demo scenarios
├── Prepare backup plans (offline demo, screenshots)
└── Practice explaining features
```

---

### Day 25 (Week 4) - Buffer Day

```
TIME: 2-3 hours each
FOCUS: Handle any remaining issues

TASKS:
├── Fix any critical bugs found during demo prep
├── Polish any rough edges
├── Final testing
├── Update documentation if needed
└── Prepare presentation materials
```

---

### Day 26 (Week 4) - Presentation Prep

```
TIME: 2-3 hours each
FOCUS: Prepare for project presentation

TASKS:
├── Create presentation slides (if required)
├── Prepare talking points for each team member
├── Create architecture diagrams
├── Prepare answers for likely questions
└── Practice presentation timing
```

---

### Day 27 (Week 4) - Final Review

```
TIME: 2-3 hours each
FOCUS: Final system verification

TASKS:
├── Complete walkthrough of all features
├── Verify production system is stable
├── Check all documentation is complete
├── Verify demo data is ready
├── Create final backup
└── Prepare deliverable package
```

---

### Day 28 (Week 4) - Delivery

```
TIME: As needed
FOCUS: Project delivery

TASKS:
├── Final deployment verification
├── Submit all deliverables:
│   ├── Source code (GitHub repo)
│   ├── APK file
│   ├── Documentation
│   ├── Live demo URLs
│   └── Presentation materials
│
├── Demo presentation (if scheduled)
└── Project complete! 🎉
```

---

## Risk Buffer & Contingency

### If Behind Schedule

```
CATCH-UP STRATEGIES

If 1-2 days behind:
├── Reduce scope of polish tasks
├── Simplify UI where possible
├── Skip optional features
└── Work extra hours on critical days

If 3-5 days behind:
├── Cut non-essential features:
│   ├── Charts/analytics → basic stats only
│   ├── Export functionality
│   ├── Background service → foreground only
│   └── Polish animations
│
├── Focus on core flow:
│   ├── Detection works
│   ├── Upload works
│   ├── Dashboard shows data
│   └── Basic navigation
│
└── Discuss with supervisor about reduced scope

Critical path (cannot cut):
├── AI model trained and working
├── Mobile app detects and uploads
├── Backend stores and serves data
├── Dashboard shows potholes on map
└── System deployed and accessible
```

### Feature Priority Matrix

```
PRIORITY LEVELS

P0 - Must Have (Core MVP):
├── YOLOv8n model detecting potholes
├── Android camera with live detection
├── GPS location capture
├── Upload to backend
├── PostgreSQL storage
├── Dashboard pothole list
├── Map visualization
└── Basic authentication

P1 - Should Have:
├── Background detection service
├── Offline storage and sync
├── Server-side deduplication
├── S3 image storage
├── Status management
└── Vehicle management

P2 - Nice to Have:
├── Analytics charts
├── CSV export
├── Detection history in app
├── Marker clustering
├── Push notifications
└── Dark mode

P3 - Future Enhancement:
├── Route tracking
├── Severity classification
├── Admin user management
├── API rate limiting
└── Advanced analytics
```

---

## Quick Reference

### Daily Standup Questions

```
Each day, ask yourself:
1. What did I complete yesterday?
2. What will I work on today?
3. Are there any blockers?
4. Do I need help from my teammate?
```

### Communication Schedule

```
Recommended sync points:
├── Daily: Quick message about progress (5 min)
├── Mid-week: 30 min video call to discuss issues
├── End of week: 1 hour review and planning
└── As needed: Immediate message for blockers
```

### Essential Commands

```bash
# Omar - Android
./gradlew assembleDebug          # Build debug APK
./gradlew assembleRelease        # Build release APK
adb install app-debug.apk        # Install on device
adb logcat | grep Pothole        # View app logs

# Hamza - Backend
npm run dev                      # Start development server
npm run build                    # Build for production
npm run db:migrate               # Run migrations
docker compose up -d             # Start all services
docker compose logs -f           # View logs

# Both - Git
git pull origin main             # Get latest code
git push origin feature/xxx      # Push changes
git checkout -b feature/xxx      # Create feature branch
```

---

## Success Metrics Reminder

```
At project end, verify:

Technical:
├── [ ] Model mAP >75%
├── [ ] Inference <150ms
├── [ ] App doesn't crash
├── [ ] API responds <500ms
├── [ ] Dashboard loads <3s
└── [ ] All data persists correctly

Functional:
├── [ ] Can detect potholes
├── [ ] Location is accurate
├── [ ] Images upload successfully
├── [ ] Dashboard shows all potholes
├── [ ] Map displays correctly
└── [ ] Status updates work

Quality:
├── [ ] Code is readable
├── [ ] Documentation exists
├── [ ] No critical bugs
├── [ ] UI is usable
└── [ ] System is deployed
```

---

Good luck with your graduation project! Follow this timeline, communicate regularly, and you'll have a working system in 4 weeks. 🚀
