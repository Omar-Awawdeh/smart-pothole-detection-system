# Android Mobile App Implementation

**Owner**: Omar  
**Duration**: Weeks 2-3 (Days 8-21)  
**Prerequisites**: Trained TFLite model from [AI Model Training](./01-ai-model.md) (already integrated)

## Implementation Status (Feb 16, 2026)

The Android app implementation is complete in this repository.

Key code entry points:
- App entry + navigation: `android/app/src/main/java/com/pothole/detection/MainActivity.kt`, `android/app/src/main/java/com/pothole/detection/ui/AppNavigation.kt`
- Detection (TFLite + NMS): `android/app/src/main/java/com/pothole/detection/detection/PotholeDetector.kt`, `android/app/src/main/java/com/pothole/detection/detection/NmsProcessor.kt`
- Camera preview + frame feed: `android/app/src/main/java/com/pothole/detection/ui/detection/components/CameraPreview.kt`
- Overlay UI: `android/app/src/main/java/com/pothole/detection/ui/detection/components/DetectionOverlay.kt`
- Location: `android/app/src/main/java/com/pothole/detection/location/LocationProvider.kt`
- Pending uploads (Room) + background worker: `android/app/src/main/java/com/pothole/detection/data/local/AppDatabase.kt`, `android/app/src/main/java/com/pothole/detection/worker/UploadWorker.kt`
- Model asset: `android/app/src/main/assets/best_float16.tflite`

Build/run:
```bash
cd android
./gradlew :app:assembleDebug
```

---

## Objective

Build a native Android application using Kotlin and Jetpack Compose that:
1. Captures real-time camera feed from vehicle-mounted phone
2. Runs YOLOv8n inference on-device using LiteRT (TFLite)
3. Records GPS coordinates when potholes are detected
4. Implements local deduplication to prevent spam
5. Uploads detection data to backend API
6. Handles offline scenarios with background upload queue

---

## App Architecture

### Pattern: MVVM + Clean Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         PRESENTATION LAYER                           │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │                    Jetpack Compose UI                          │  │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐│  │
│  │  │ Detection   │  │  Settings   │  │  History/Stats Screen   ││  │
│  │  │   Screen    │  │   Screen    │  │                         ││  │
│  │  └──────┬──────┘  └─────────────┘  └─────────────────────────┘│  │
│  └─────────┼─────────────────────────────────────────────────────┘  │
│            │ State + Events                                          │
│  ┌─────────▼─────────────────────────────────────────────────────┐  │
│  │                      ViewModels                                │  │
│  │  ┌─────────────────┐  ┌─────────────────────────────────────┐ │  │
│  │  │ Detection       │  │ Settings/History ViewModels         │ │  │
│  │  │ ViewModel       │  │                                     │ │  │
│  │  └────────┬────────┘  └─────────────────────────────────────┘ │  │
│  └───────────┼───────────────────────────────────────────────────┘  │
└──────────────┼──────────────────────────────────────────────────────┘
               │ Use Cases
┌──────────────▼──────────────────────────────────────────────────────┐
│                          DOMAIN LAYER                                │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │                       Use Cases                                 │ │
│  │  ┌────────────────┐  ┌──────────────┐  ┌────────────────────┐  │ │
│  │  │ StartDetection │  │UploadPothole │  │ CheckDeduplication │  │ │
│  │  │ UseCase        │  │UseCase       │  │ UseCase            │  │ │
│  │  └────────────────┘  └──────────────┘  └────────────────────┘  │ │
│  │  ┌────────────────┐  ┌──────────────┐                          │ │
│  │  │ GetLocation    │  │ SyncPending  │                          │ │
│  │  │ UseCase        │  │ UseCase      │                          │ │
│  │  └────────────────┘  └──────────────┘                          │ │
│  └────────────────────────────────────────────────────────────────┘ │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │                    Domain Models                                │ │
│  │  Detection, PotholeReport, Location, DetectionSettings          │ │
│  └────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
               │ Repository Interfaces
┌──────────────▼──────────────────────────────────────────────────────┐
│                           DATA LAYER                                 │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │                     Repositories                                │ │
│  │  ┌────────────────┐  ┌──────────────┐  ┌────────────────────┐  │ │
│  │  │ PotholeRepo    │  │ LocationRepo │  │ SettingsRepo       │  │ │
│  │  └───────┬────────┘  └──────┬───────┘  └────────────────────┘  │ │
│  └──────────┼──────────────────┼──────────────────────────────────┘ │
│             │                  │                                     │
│  ┌──────────▼──────────────────▼──────────────────────────────────┐ │
│  │                      Data Sources                               │ │
│  │  ┌────────────┐  ┌────────────┐  ┌────────────┐  ┌───────────┐ │ │
│  │  │  CameraX   │  │  LiteRT    │  │    GPS     │  │   Room    │ │ │
│  │  │  Source    │  │  Source    │  │   Source   │  │   Source  │ │ │
│  │  └────────────┘  └────────────┘  └────────────┘  └───────────┘ │ │
│  │  ┌────────────────────────────────────────────────────────────┐│ │
│  │  │                  Remote API (Ktor)                          ││ │
│  │  └────────────────────────────────────────────────────────────┘│ │
│  └────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Module Breakdown

### 1. Detection Module

**Purpose**: Run YOLOv8n inference on camera frames

**Components**:
```
detection/
├── PotholeDetector          # Main inference orchestrator
├── TFLiteModelLoader        # Load and configure TFLite interpreter
├── ImagePreprocessor        # Bitmap → Tensor conversion
├── OutputParser             # Parse YOLO output format
├── NmsProcessor             # Non-maximum suppression
└── DetectionResult          # Data class for results
```

**Key Responsibilities**:
- Load TFLite model with appropriate delegate (NNAPI → GPU → CPU fallback)
- Preprocess camera frames (resize to 640x640, normalize to 0-1)
- Run inference asynchronously (off main thread)
- Post-process outputs (apply NMS, filter by confidence)
- Report inference time for performance monitoring

**Inference Pipeline Pseudocode**:

```
FUNCTION detect(bitmap: Bitmap) -> List<Detection>:
    
    # 1. Preprocess
    resized = RESIZE(bitmap, 640, 640)
    tensor = BITMAP_TO_FLOAT_ARRAY(resized)  # Normalize to 0-1
    inputBuffer = FLOAT_ARRAY_TO_BYTEBUFFER(tensor)
    
    # 2. Run inference
    interpreter.RUN(inputBuffer, outputBuffer)
    
    # 3. Parse output
    rawDetections = PARSE_YOLO_OUTPUT(outputBuffer)
    # Output shape: [1, 5, 8400]
    # Format: [x_center, y_center, width, height, confidence]
    
    # 4. Filter by confidence
    filtered = FILTER(rawDetections, confidence > THRESHOLD)
    
    # 5. Apply NMS
    finalDetections = NON_MAX_SUPPRESSION(filtered, iouThreshold=0.5)
    
    # 6. Scale back to original image size
    scaled = SCALE_TO_ORIGINAL(finalDetections, bitmap.width, bitmap.height)
    
    RETURN scaled
```

**Delegate Selection Strategy**:

```
FUNCTION selectBestDelegate() -> Delegate:
    
    # Priority: NNAPI (NPU) > GPU > CPU
    
    IF androidVersion >= 9 (API 28):
        TRY:
            delegate = NnApiDelegate()
            IF delegate.WORKS():
                RETURN delegate  # Uses NPU if available
        CATCH:
            PASS
    
    TRY:
        delegate = GpuDelegate()
        IF delegate.WORKS():
            RETURN delegate
    CATCH:
        PASS
    
    # Fallback to CPU with multiple threads
    RETURN CpuDelegate(numThreads=4)
```

### 2. Camera Module

**Purpose**: Manage CameraX lifecycle and frame capture

**Components**:
```
camera/
├── CameraManager            # CameraX setup and lifecycle
├── FrameAnalyzer            # ImageAnalysis.Analyzer implementation
└── CameraState              # Camera status and errors
```

**Key Responsibilities**:
- Initialize CameraX with back camera
- Set up Preview use case (for UI display)
- Set up ImageAnalysis use case (for AI processing)
- Handle camera permissions
- Manage camera lifecycle with Compose

**Frame Analysis Strategy**:

```
FUNCTION setupImageAnalysis():
    
    imageAnalysis = ImageAnalysis.Builder()
        .setTargetResolution(Size(640, 480))  # Lower res for analysis
        .setBackpressureStrategy(STRATEGY_KEEP_ONLY_LATEST)  # Drop frames if behind
        .setOutputImageFormat(RGBA_8888)
        .build()
    
    frameCounter = 0
    
    imageAnalysis.setAnalyzer(executor) { imageProxy ->
        
        frameCounter++
        
        # Process every Nth frame for performance
        IF frameCounter % FRAME_SKIP_RATE == 0:
            bitmap = imageProxy.toBitmap()
            onFrameReady(bitmap)
        
        imageProxy.close()  # Always close!
    }
```

**Frame Skip Strategy**:

```
# Adaptive frame skipping based on device performance

IF inferenceTime < 50ms:
    FRAME_SKIP_RATE = 2   # Process every 2nd frame (15 FPS)
ELSE IF inferenceTime < 100ms:
    FRAME_SKIP_RATE = 3   # Process every 3rd frame (10 FPS)
ELSE:
    FRAME_SKIP_RATE = 4   # Process every 4th frame (7.5 FPS)
```

### 3. Location Module

**Purpose**: Track GPS coordinates with high accuracy

**Components**:
```
location/
├── LocationService          # FusedLocationProvider wrapper
├── LocationState            # Current location + accuracy
└── LocationPermissionHelper # Handle runtime permissions
```

**Key Responsibilities**:
- Request location permissions
- Get high-accuracy GPS updates
- Provide current location on demand
- Handle location unavailable scenarios

**Location Configuration**:

```
locationRequest = LocationRequest.Builder(
    Priority.PRIORITY_HIGH_ACCURACY,
    intervalMs = 1000           # Update every 1 second
)
.setMinUpdateDistanceMeters(5f)  # Minimum 5m between updates
.setGranularity(GRANULARITY_FINE)
.setWaitForAccurateLocation(true)
.build()
```

### 4. Deduplication Module

**Purpose**: Prevent reporting same pothole multiple times

**Components**:
```
deduplication/
├── LocalDeduplicator        # In-memory recent detections
└── SpatialUtils             # Distance calculations
```

**Local Deduplication Logic**:

```
CLASS LocalDeduplicator:
    
    recentDetections: List<DetectionRecord> = []
    DEDUP_RADIUS_METERS = 10.0
    DEDUP_TIME_WINDOW_MS = 60_000  # 1 minute
    MAX_RECORDS = 100
    
    FUNCTION shouldReport(latitude: Double, longitude: Double) -> Boolean:
        
        currentTime = System.currentTimeMillis()
        
        # Clean old records
        recentDetections = recentDetections.FILTER { record ->
            currentTime - record.timestamp < DEDUP_TIME_WINDOW_MS
        }
        
        # Check for nearby recent detections
        FOR record IN recentDetections:
            distance = haversineDistance(
                latitude, longitude,
                record.latitude, record.longitude
            )
            IF distance < DEDUP_RADIUS_METERS:
                RETURN false  # Duplicate, don't report
        
        # Not a duplicate - add to recent and report
        recentDetections.ADD(DetectionRecord(latitude, longitude, currentTime))
        
        # Trim to max size
        IF recentDetections.SIZE > MAX_RECORDS:
            recentDetections.REMOVE_FIRST()
        
        RETURN true  # Report this detection
    
    FUNCTION haversineDistance(lat1, lon1, lat2, lon2) -> Double:
        # Standard Haversine formula for distance between two GPS points
        R = 6371000  # Earth radius in meters
        dLat = toRadians(lat2 - lat1)
        dLon = toRadians(lon2 - lon1)
        a = sin(dLat/2)^2 + cos(toRadians(lat1)) * cos(toRadians(lat2)) * sin(dLon/2)^2
        c = 2 * atan2(sqrt(a), sqrt(1-a))
        RETURN R * c
```

### 5. Network Module

**Purpose**: Communicate with backend API

**Components**:
```
network/
├── ApiService               # Ktor HTTP client
├── AuthManager              # JWT token storage and refresh
├── models/                  # Request/Response DTOs
│   ├── PotholeRequest
│   ├── PotholeResponse
│   └── AuthModels
└── NetworkState             # Online/offline status
```

**API Client Setup**:

```
FUNCTION createApiClient() -> HttpClient:
    
    RETURN HttpClient(Android) {
        
        # JSON serialization
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        
        # Timeout configuration
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 10_000
        }
        
        # Auth header
        install(Auth) {
            bearer {
                loadTokens {
                    BearerTokens(accessToken, refreshToken)
                }
            }
        }
        
        # Logging (debug only)
        install(Logging) {
            level = LogLevel.HEADERS
        }
    }
```

**Upload Function**:

```
FUNCTION uploadPothole(
    imageBytes: ByteArray,
    latitude: Double,
    longitude: Double,
    confidence: Float,
    vehicleId: String
) -> Result<PotholeResponse>:
    
    TRY:
        response = httpClient.submitFormWithBinaryData(
            url = "$BASE_URL/api/potholes",
            formData = formData {
                append("image", imageBytes, Headers {
                    contentType = "image/jpeg"
                    contentDisposition = "filename=pothole_${uuid}.jpg"
                })
                append("latitude", latitude)
                append("longitude", longitude)
                append("confidence", confidence)
                append("vehicleId", vehicleId)
                append("timestamp", System.currentTimeMillis())
            }
        )
        
        IF response.status.isSuccess():
            RETURN Result.success(response.body())
        ELSE:
            RETURN Result.failure(ApiException(response.status))
    
    CATCH e: Exception:
        RETURN Result.failure(e)
```

### 6. Background Upload Module

**Purpose**: Reliable upload queue for offline scenarios

**Components**:
```
worker/
├── UploadWorker             # WorkManager worker
├── UploadQueue              # Room-based queue
└── PendingUpload            # Entity for pending uploads
```

**WorkManager Configuration**:

```
FUNCTION scheduleUpload(pendingUpload: PendingUpload):
    
    # Save to local database first
    database.pendingUploads.INSERT(pendingUpload)
    
    # Create work request
    uploadWork = OneTimeWorkRequestBuilder<UploadWorker>()
        .setInputData(workDataOf("uploadId" to pendingUpload.id))
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        )
        .setBackoffCriteria(
            BackoffPolicy.EXPONENTIAL,
            initialDelayMs = 30_000,
            timeUnit = TimeUnit.MILLISECONDS
        )
        .build()
    
    # Enqueue with unique ID to prevent duplicates
    WorkManager.getInstance()
        .enqueueUniqueWork(
            uniqueWorkName = "upload_${pendingUpload.id}",
            existingWorkPolicy = ExistingWorkPolicy.KEEP,
            request = uploadWork
        )
```

**Upload Worker Logic**:

```
CLASS UploadWorker : CoroutineWorker:
    
    FUNCTION doWork() -> Result:
        
        uploadId = inputData.getString("uploadId")
        pendingUpload = database.pendingUploads.GET(uploadId)
        
        IF pendingUpload == null:
            RETURN Result.failure()  # Already processed
        
        # Load image from local storage
        imageBytes = loadImage(pendingUpload.localImagePath)
        
        # Attempt upload
        result = apiService.uploadPothole(
            imageBytes = imageBytes,
            latitude = pendingUpload.latitude,
            longitude = pendingUpload.longitude,
            confidence = pendingUpload.confidence,
            vehicleId = pendingUpload.vehicleId
        )
        
        IF result.isSuccess():
            # Clean up local data
            database.pendingUploads.DELETE(uploadId)
            deleteLocalImage(pendingUpload.localImagePath)
            RETURN Result.success()
        
        ELSE IF result.isRetryable():
            # Network error - retry later
            RETURN Result.retry()
        
        ELSE:
            # Permanent failure (e.g., 400 Bad Request)
            pendingUpload.failureCount++
            IF pendingUpload.failureCount >= MAX_RETRIES:
                database.pendingUploads.DELETE(uploadId)
            ELSE:
                database.pendingUploads.UPDATE(pendingUpload)
            RETURN Result.failure()
```

---

## UI Screens (Jetpack Compose)

### Screen Architecture

```
ui/
├── detection/
│   ├── DetectionScreen.kt       # Main detection UI
│   ├── DetectionViewModel.kt    # State management
│   └── components/
│       ├── CameraPreview.kt     # Camera composable
│       ├── DetectionOverlay.kt  # Bounding box overlay
│       └── StatsCard.kt         # Detection statistics
│
├── settings/
│   ├── SettingsScreen.kt
│   └── SettingsViewModel.kt
│
├── history/
│   ├── HistoryScreen.kt
│   └── HistoryViewModel.kt
│
└── components/                   # Shared components
    ├── PermissionHandler.kt
    └── LoadingIndicator.kt
```

### Detection Screen State

```
DATA CLASS DetectionUiState(
    isDetecting: Boolean = false,
    currentLocation: Location? = null,
    recentDetections: List<Detection> = emptyList(),
    detectionsToday: Int = 0,
    pendingUploads: Int = 0,
    inferenceTimeMs: Long = 0,
    errorMessage: String? = null,
    cameraPermissionGranted: Boolean = false,
    locationPermissionGranted: Boolean = false
)

SEALED CLASS DetectionEvent:
    StartDetection
    StopDetection
    PotholeDetected(detection: Detection, location: Location)
    UploadCompleted(id: String)
    UploadFailed(id: String, error: String)
    PermissionResult(permission: String, granted: Boolean)
```

### Detection Screen Layout

```
┌─────────────────────────────────────────┐
│              Status Bar                  │
├─────────────────────────────────────────┤
│                                         │
│                                         │
│           Camera Preview                │
│       (with detection overlay)          │
│                                         │
│    ┌───────────────────────────────┐   │
│    │  [Bounding Box]               │   │
│    │       Pothole                 │   │
│    │       85%                     │   │
│    └───────────────────────────────┘   │
│                                         │
│                                         │
├─────────────────────────────────────────┤
│  ┌─────────────────────────────────┐   │
│  │  Stats Card                      │   │
│  │  📍 32.5521, 35.8461            │   │
│  │  🕳️ Detected today: 12          │   │
│  │  ⏱️ Inference: 65ms             │   │
│  │  📤 Pending: 2                   │   │
│  └─────────────────────────────────┘   │
│                                         │
│    ┌─────────────────────────────┐     │
│    │    START DETECTION          │     │
│    │    (or STOP if active)      │     │
│    └─────────────────────────────┘     │
│                                         │
└─────────────────────────────────────────┘
```

### Detection Screen Composable Structure

```
@Composable
FUNCTION DetectionScreen(viewModel: DetectionViewModel):
    
    state = viewModel.uiState.collectAsState()
    
    # Permission handling
    RequestPermissions(
        permissions = [CAMERA, ACCESS_FINE_LOCATION],
        onResult = { viewModel.onPermissionResult(it) }
    )
    
    IF NOT state.cameraPermissionGranted:
        PermissionDeniedContent()
        RETURN
    
    Box(modifier = Modifier.fillMaxSize()):
        
        # Camera preview (full screen background)
        CameraPreview(
            onFrameAnalyzed = { bitmap ->
                IF state.isDetecting:
                    viewModel.processFrame(bitmap)
            }
        )
        
        # Detection overlay (bounding boxes)
        DetectionOverlay(
            detections = state.recentDetections,
            modifier = Modifier.fillMaxSize()
        )
        
        # Bottom controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ):
            
            # Stats card
            StatsCard(
                location = state.currentLocation,
                detectionsToday = state.detectionsToday,
                inferenceTime = state.inferenceTimeMs,
                pendingUploads = state.pendingUploads
            )
            
            Spacer(height = 16.dp)
            
            # Start/Stop button
            DetectionButton(
                isDetecting = state.isDetecting,
                onClick = {
                    IF state.isDetecting:
                        viewModel.stopDetection()
                    ELSE:
                        viewModel.startDetection()
                }
            )
        
        # Error snackbar
        IF state.errorMessage != null:
            Snackbar(message = state.errorMessage)
```

---

## Data Flow: Complete Detection Cycle

```
┌────────────────────────────────────────────────────────────────────────┐
│                        DETECTION CYCLE                                  │
│                                                                         │
│  User taps "START DETECTION"                                           │
│           │                                                             │
│           ▼                                                             │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │ DetectionViewModel.startDetection()                              │   │
│  │ - Sets isDetecting = true                                        │   │
│  │ - Starts location updates                                        │   │
│  │ - Enables frame analysis                                         │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│           │                                                             │
│           ▼                                                             │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │ CameraX captures frame (30 FPS)                                  │   │
│  │ FrameAnalyzer.analyze(imageProxy)                                │   │
│  │ - Check frame skip (process every 2nd frame)                     │   │
│  │ - Convert to Bitmap                                              │   │
│  │ - Call viewModel.processFrame(bitmap)                            │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│           │                                                             │
│           ▼                                                             │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │ PotholeDetector.detect(bitmap)                                   │   │
│  │ - Preprocess: resize 640x640, normalize 0-1                      │   │
│  │ - Run LiteRT inference (~50-100ms)                               │   │
│  │ - Parse output tensor [1, 5, 8400]                               │   │
│  │ - Filter confidence > 0.5                                        │   │
│  │ - Apply NMS (IoU threshold 0.5)                                  │   │
│  │ - Return List<Detection>                                         │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│           │                                                             │
│           ▼                                                             │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │ IF detections.isNotEmpty():                                      │   │
│  │                                                                   │   │
│  │   # Get current location                                         │   │
│  │   location = locationService.getCurrentLocation()                │   │
│  │                                                                   │   │
│  │   FOR detection IN detections:                                   │   │
│  │                                                                   │   │
│  │     # Check local deduplication                                  │   │
│  │     IF deduplicator.shouldReport(location.lat, location.lng):    │   │
│  │                                                                   │   │
│  │       # Crop detection region from frame                         │   │
│  │       croppedImage = cropDetection(bitmap, detection.boundingBox)│   │
│  │                                                                   │   │
│  │       # Save to local storage                                    │   │
│  │       localPath = saveImage(croppedImage)                        │   │
│  │                                                                   │   │
│  │       # Create pending upload                                    │   │
│  │       pendingUpload = PendingUpload(                             │   │
│  │         localImagePath = localPath,                              │   │
│  │         latitude = location.lat,                                 │   │
│  │         longitude = location.lng,                                │   │
│  │         confidence = detection.confidence,                       │   │
│  │         vehicleId = settings.vehicleId,                          │   │
│  │         timestamp = System.currentTimeMillis()                   │   │
│  │       )                                                          │   │
│  │                                                                   │   │
│  │       # Queue for upload                                         │   │
│  │       uploadQueue.enqueue(pendingUpload)                         │   │
│  │                                                                   │   │
│  │       # Update UI                                                │   │
│  │       updateDetectionCount()                                     │   │
│  │                                                                   │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│           │                                                             │
│           ▼                                                             │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │ WorkManager (background, when network available)                 │   │
│  │ - Load pending upload from Room database                         │   │
│  │ - Read image from local storage                                  │   │
│  │ - POST to /api/potholes                                          │   │
│  │ - On success: delete local data                                  │   │
│  │ - On failure: retry with exponential backoff                     │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
└────────────────────────────────────────────────────────────────────────┘
```

---

## Dependencies

### build.gradle.kts (app)

```
Key dependencies to include:

# Jetpack Compose
- androidx.compose.ui:ui
- androidx.compose.material3:material3
- androidx.activity:activity-compose
- androidx.lifecycle:lifecycle-viewmodel-compose
- androidx.navigation:navigation-compose

# CameraX
- androidx.camera:camera-core
- androidx.camera:camera-camera2
- androidx.camera:camera-lifecycle
- androidx.camera:camera-view

# LiteRT (TensorFlow Lite)
- com.google.ai.edge.litert:litert
- com.google.ai.edge.litert:litert-gpu
- com.google.ai.edge.litert:litert-support

# Location
- com.google.android.gms:play-services-location

# Networking
- io.ktor:ktor-client-android
- io.ktor:ktor-client-content-negotiation
- io.ktor:ktor-serialization-kotlinx-json

# Background processing
- androidx.work:work-runtime-ktx

# Local storage
- androidx.room:room-runtime
- androidx.room:room-ktx

# Dependency Injection
- com.google.dagger:hilt-android
- androidx.hilt:hilt-navigation-compose

# Coroutines
- org.jetbrains.kotlinx:kotlinx-coroutines-android
```

### Permissions (AndroidManifest.xml)

```xml
Required permissions:
- CAMERA (runtime permission)
- ACCESS_FINE_LOCATION (runtime permission)
- ACCESS_COARSE_LOCATION (runtime permission)
- INTERNET
- FOREGROUND_SERVICE
- FOREGROUND_SERVICE_LOCATION
```

---

## Configuration Constants

```
OBJECT AppConfig:
    
    # API
    API_BASE_URL = "https://api.yoursite.com"
    API_TIMEOUT_MS = 30_000
    
    # Detection
    MODEL_FILENAME = "yolov8n_pothole_float16.tflite"
    INPUT_SIZE = 640
    CONFIDENCE_THRESHOLD = 0.5
    NMS_IOU_THRESHOLD = 0.5
    FRAME_SKIP_RATE = 2  # Process every Nth frame
    
    # Deduplication
    DEDUP_RADIUS_METERS = 10.0
    DEDUP_TIME_WINDOW_MS = 60_000
    MAX_DEDUP_RECORDS = 100
    
    # Upload
    MAX_UPLOAD_RETRIES = 5
    UPLOAD_BACKOFF_INITIAL_MS = 30_000
    IMAGE_QUALITY = 85  # JPEG compression quality
    
    # Location
    LOCATION_UPDATE_INTERVAL_MS = 1_000
    LOCATION_MIN_DISTANCE_METERS = 5.0
```

---

## Testing Strategy

### Unit Tests

```
Tests to implement:

# Detection Module
- PotholeDetectorTest
  - testPreprocessing_correctOutputShape
  - testOutputParsing_validDetections
  - testNms_removesOverlappingBoxes
  - testConfidenceFiltering_belowThreshold

# Deduplication Module
- LocalDeduplicatorTest
  - testShouldReport_firstDetection_returnsTrue
  - testShouldReport_nearbyRecent_returnsFalse
  - testShouldReport_farAway_returnsTrue
  - testShouldReport_oldRecord_returnsTrue
  - testHaversineDistance_knownValues

# Network Module
- ApiServiceTest
  - testUploadPothole_success
  - testUploadPothole_networkError_retries
  - testUploadPothole_authError_refreshesToken
```

### Integration Tests

```
# Camera + Detection
- testCameraToDetection_endToEnd
- testFrameSkipping_correctRate

# Upload Queue
- testOfflineUpload_queuesCorrectly
- testOnlineSync_uploadsAll
```

### Manual Testing Checklist

```
□ Camera permission flow works
□ Location permission flow works
□ Camera preview displays correctly
□ Detection overlay shows bounding boxes
□ GPS coordinates update in real-time
□ Detection count increments
□ Offline mode queues uploads
□ Background upload works when online
□ App doesn't crash on rotation
□ App handles permission denial gracefully
□ Memory usage stays reasonable (<200MB)
□ Battery drain is acceptable
```

---

## Performance Targets

| Metric | Minimum | Target | How to Measure |
|--------|---------|--------|----------------|
| Inference time | <150ms | <100ms | Profiler / logs |
| Frame rate (detection ON) | 10 FPS | 15 FPS | FPS counter |
| Memory usage | <300MB | <200MB | Android Profiler |
| App startup | <3s | <2s | Cold start timing |
| Battery drain | <15%/hour | <10%/hour | Battery stats |

### Performance Optimization Tips

```
1. Use NNAPI/GPU delegate for inference
2. Process every 2nd or 3rd frame
3. Use STRATEGY_KEEP_ONLY_LATEST for ImageAnalysis
4. Resize images on background thread
5. Use ByteBuffer.allocateDirect() for tensors
6. Avoid creating objects in hot paths
7. Use Bitmap.Config.ARGB_8888 for camera frames
8. Release resources properly (interpreter, delegates)
```

---

## Error Handling

```
ERROR_HANDLING_STRATEGY:

# Camera errors
- CameraAccessException → Show "Camera unavailable" message
- CameraPermissionDenied → Show permission rationale dialog

# Location errors
- LocationPermissionDenied → App can work but won't upload
- LocationUnavailable → Use last known location or skip

# Detection errors
- ModelLoadFailure → Fatal error, show message and exit
- InferenceException → Log and skip frame, continue

# Network errors
- NoNetworkConnection → Queue locally, sync later
- ServerError (5xx) → Retry with backoff
- ClientError (4xx) → Log and discard (except 401)
- AuthError (401) → Refresh token or re-login

# Storage errors
- DiskFull → Show warning, stop saving new detections
- FileNotFound → Skip upload, log error
```

---

## Timeline

| Day | Tasks | Deliverables |
|-----|-------|--------------|
| Day 8 | Project setup, dependencies, architecture scaffold | Empty app with navigation |
| Day 9 | Camera module implementation | Camera preview working |
| Day 10 | LiteRT integration, model loading | Model loads successfully |
| Day 11 | Detection pipeline, preprocessing | Inference runs on frames |
| Day 12 | Output parsing, NMS, overlay UI | Bounding boxes displayed |
| Day 13 | Location service, GPS integration | Location shown in UI |
| Day 14 | Deduplication logic | Local dedup working |
| Day 15 | Room database setup, pending uploads | Local queue working |
| Day 16 | API client (Ktor), upload function | Upload works (online) |
| Day 17 | WorkManager background upload | Offline queue works |
| Day 18 | Settings screen, configuration | User can adjust settings |
| Day 19 | Testing, bug fixes | Core flow stable |
| Day 20 | Performance optimization | Targets met |
| Day 21 | Polish, final testing | App ready for demo |

**Total: ~14 days, 2-3 hours/day = 28-42 hours**

---

## Deliverables

```
At the end of Week 3, Omar should have:

□ Working Android app (APK)
  └── Detection screen with camera preview
  └── Real-time pothole detection with bounding boxes
  └── GPS location tracking
  └── Local deduplication
  └── Offline upload queue
  └── Settings screen

□ Source code
  └── Clean architecture
  └── Documented code
  └── Unit tests for core logic

□ Documentation
  └── Build instructions
  └── Configuration options
  └── Known issues/limitations

□ Test results
  └── Performance benchmarks
  └── Detection accuracy on test images
```

---

## Next Steps

After Android app is complete:

1. **Integration testing** with backend API (coordinate with Hamza)
2. **Field testing** on actual roads
3. **Performance tuning** based on real-world results
4. **UI polish** based on feedback

Continue to [Backend API Design](./03-backend-api.md)
