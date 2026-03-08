# Smart Pothole Detection System - Implementation Overview

> Note: This document includes original planning architecture. For the current running implementation details (stack, ports, schema updates), see `docs/07-current-implementation.md`.

## Project Summary

A smart pothole detection system that uses smartphone-based AI to detect road potholes in real-time, records their GPS locations, and reports them to a centralized dashboard for city maintenance teams.

**Timeline**: 4 weeks (28 days)  
**Team**: 2 developers, 2-3 hours/day each  
**Total effort**: ~120-170 hours combined

---

## System Architecture

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                              MOBILE APP (Android)                             │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌──────────────┐   │
│  │  CameraX    │───▶│  LiteRT     │───▶│   Local     │───▶│   Upload     │   │
│  │  Preview    │    │  YOLOv8n    │    │   Dedup     │    │   Queue      │   │
│  └─────────────┘    └─────────────┘    └─────────────┘    └──────┬───────┘   │
│                                                                   │           │
│  ┌─────────────┐                                                  │           │
│  │    GPS      │──────────────────────────────────────────────────┘           │
│  │  Service    │                                                              │
│  └─────────────┘                                                              │
└──────────────────────────────────────────────────────────────────┼────────────┘
                                                                   │
                                                          HTTPS + JWT
                                                                   │
┌──────────────────────────────────────────────────────────────────▼────────────┐
│                            HETZNER VPS (Ubuntu 22.04)                         │
│                                                                               │
│  ┌────────────────────────────────────────────────────────────────────────┐  │
│  │                         Fastify API Server                              │  │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐ │  │
│  │  │   Auth   │  │ Pothole  │  │ Vehicle  │  │  User    │  │  Stats   │ │  │
│  │  │  Routes  │  │  Routes  │  │  Routes  │  │  Routes  │  │  Routes  │ │  │
│  │  └──────────┘  └──────────┘  └──────────┘  └──────────┘  └──────────┘ │  │
│  └────────────────────────────────┬───────────────────────────────────────┘  │
│                                   │                                          │
│  ┌────────────────────────────────▼───────────────────────────────────────┐  │
│  │                    PostgreSQL 16 + PostGIS                              │  │
│  │  ┌─────────┐  ┌───────────┐  ┌──────────┐  ┌─────────────────────────┐ │  │
│  │  │  users  │  │  potholes │  │ vehicles │  │  spatial_ref_sys (GIS)  │ │  │
│  │  └─────────┘  └───────────┘  └──────────┘  └─────────────────────────┘ │  │
│  └────────────────────────────────────────────────────────────────────────┘  │
│                                                                               │
│  ┌────────────────────────────────────────────────────────────────────────┐  │
│  │                    Astro + React Dashboard                              │  │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐ │  │
│  │  │Dashboard │  │ Pothole  │  │  Map     │  │ Vehicle  │  │ Settings │ │  │
│  │  │  Home    │  │  List    │  │  View    │  │  Mgmt    │  │  Page    │ │  │
│  │  └──────────┘  └──────────┘  └──────────┘  └──────────┘  └──────────┘ │  │
│  └────────────────────────────────────────────────────────────────────────┘  │
│                                                                               │
│  ┌────────────────────────────────────────────────────────────────────────┐  │
│  │                         Nginx Reverse Proxy                             │  │
│  │              (SSL termination, static files, API routing)               │  │
│  └────────────────────────────────────────────────────────────────────────┘  │
└───────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        │ HTTPS
                                        ▼
┌───────────────────────────────────────────────────────────────────────────────┐
│                                  AWS S3                                        │
│                     pothole-images-bucket/{vehicle_id}/{date}/{uuid}.jpg      │
└───────────────────────────────────────────────────────────────────────────────┘
```

---

## Technology Stack

### Mobile Application (Omar)

| Component | Technology | Justification |
|-----------|------------|---------------|
| Language | Kotlin 1.9+ | Modern, concise, official Android language |
| UI Framework | Jetpack Compose | Declarative UI, modern Android standard |
| Camera | CameraX | Simplified camera API, lifecycle-aware |
| ML Runtime | LiteRT (TFLite) | Best NPU/GPU support via NNAPI |
| AI Model | YOLOv8n | Best accuracy/speed balance for mobile |
| Networking | Ktor Client | Kotlin-native, coroutine support |
| Background | WorkManager | Reliable background upload queue |
| Local DB | Room | Offline caching of pending uploads |
| Location | Fused Location Provider | High accuracy with battery optimization |
| DI | Hilt | Standard Android dependency injection |

### Backend Server (Hamza)

| Component | Technology | Justification |
|-----------|------------|---------------|
| Runtime | Node.js 20 LTS | Fast, widely supported |
| Framework | Fastify 4.x | 2x faster than Express, TypeScript support |
| Language | TypeScript 5.x | Type safety, better maintainability |
| Database | PostgreSQL 16 | Robust, supports PostGIS |
| Spatial | PostGIS 3.4 | Geospatial queries for deduplication |
| ORM | Drizzle ORM | Type-safe, lightweight, good PostgreSQL support |
| Auth | JWT + bcrypt | Stateless authentication |
| File Upload | @fastify/multipart | Handle image uploads |
| Validation | Zod | Runtime schema validation |
| S3 Client | @aws-sdk/client-s3 | Official AWS SDK v3 |

### Web Dashboard (Hamza)

| Component | Technology | Justification |
|-----------|------------|---------------|
| Framework | Astro 4.x | Fast static pages, partial hydration |
| Interactive | React 18 | Islands for maps, tables, forms |
| Styling | Tailwind CSS 3.x | Rapid UI development |
| Maps | Leaflet + OpenStreetMap | Free, no API key required |
| Tables | TanStack Table v8 | Sorting, filtering, pagination |
| Charts | Chart.js 4.x | Simple analytics visualizations |
| HTTP Client | fetch + SWR | Data fetching with caching |
| Icons | Lucide React | Consistent icon set |

### Infrastructure

| Component | Technology | Justification |
|-----------|------------|---------------|
| VPS | Hetzner CX31 (4 vCPU, 8GB RAM) | Cost-effective (~€8/month) |
| OS | Ubuntu 22.04 LTS | Stable, well-documented |
| Containerization | Docker + Compose | Consistent environments |
| Reverse Proxy | Nginx | SSL termination, static files |
| SSL | Let's Encrypt + Certbot | Free SSL certificates |
| File Storage | AWS S3 | Scalable, cheap ($0.023/GB) |
| DNS | Cloudflare (free) | DNS management, basic DDoS protection |

---

## Repository Structure

```
smart-pothole-detection/
│
├── android/                          # Mobile app (Omar)
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── java/com/pothole/detector/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── PotholeApp.kt           # Application class
│   │   │   │   ├── ui/                     # Jetpack Compose screens
│   │   │   │   │   ├── detection/          # Detection screen + ViewModel
│   │   │   │   │   ├── settings/           # Settings screen
│   │   │   │   │   ├── history/            # Detection history
│   │   │   │   │   └── components/         # Reusable composables
│   │   │   │   ├── detection/              # AI inference module
│   │   │   │   │   ├── PotholeDetector.kt
│   │   │   │   │   ├── DetectionResult.kt
│   │   │   │   │   └── NmsProcessor.kt
│   │   │   │   ├── camera/                 # CameraX integration
│   │   │   │   │   └── CameraManager.kt
│   │   │   │   ├── location/               # GPS service
│   │   │   │   │   └── LocationService.kt
│   │   │   │   ├── network/                # API client
│   │   │   │   │   ├── ApiService.kt
│   │   │   │   │   └── models/
│   │   │   │   ├── data/                   # Repository + Room DB
│   │   │   │   │   ├── PotholeRepository.kt
│   │   │   │   │   ├── local/
│   │   │   │   │   └── remote/
│   │   │   │   ├── worker/                 # Background upload
│   │   │   │   │   └── UploadWorker.kt
│   │   │   │   └── di/                     # Hilt modules
│   │   │   ├── assets/
│   │   │   │   └── models/
│   │   │   │       └── yolov8n_pothole.tflite
│   │   │   └── res/
│   │   └── build.gradle.kts
│   ├── build.gradle.kts
│   └── settings.gradle.kts
│
├── backend/                          # API server (Hamza)
│   ├── src/
│   │   ├── index.ts                  # Entry point
│   │   ├── server.ts                 # Fastify setup
│   │   ├── routes/
│   │   │   ├── auth.routes.ts
│   │   │   ├── pothole.routes.ts
│   │   │   ├── vehicle.routes.ts
│   │   │   └── user.routes.ts
│   │   ├── services/
│   │   │   ├── auth.service.ts
│   │   │   ├── pothole.service.ts
│   │   │   ├── deduplication.service.ts
│   │   │   └── upload.service.ts
│   │   ├── db/
│   │   │   ├── schema.ts             # Drizzle schema
│   │   │   ├── migrations/
│   │   │   └── index.ts
│   │   ├── plugins/
│   │   │   ├── auth.plugin.ts
│   │   │   ├── database.plugin.ts
│   │   │   └── s3.plugin.ts
│   │   ├── utils/
│   │   │   ├── spatial.ts            # PostGIS helpers
│   │   │   └── errors.ts
│   │   └── types/
│   ├── drizzle.config.ts
│   ├── package.json
│   └── tsconfig.json
│
├── dashboard/                        # Web dashboard (Hamza)
│   ├── src/
│   │   ├── pages/
│   │   │   ├── index.astro           # Dashboard home
│   │   │   ├── login.astro
│   │   │   ├── potholes/
│   │   │   │   ├── index.astro       # List view
│   │   │   │   └── [id].astro        # Detail view
│   │   │   ├── map.astro             # Full map view
│   │   │   ├── vehicles/
│   │   │   │   └── index.astro
│   │   │   └── settings.astro
│   │   ├── components/
│   │   │   ├── react/                # React islands
│   │   │   │   ├── PotholeMap.tsx
│   │   │   │   ├── PotholeTable.tsx
│   │   │   │   ├── StatsCards.tsx
│   │   │   │   └── StatusBadge.tsx
│   │   │   └── astro/                # Astro components
│   │   │       ├── Sidebar.astro
│   │   │       ├── Header.astro
│   │   │       └── Card.astro
│   │   ├── layouts/
│   │   │   ├── Layout.astro
│   │   │   └── DashboardLayout.astro
│   │   ├── lib/
│   │   │   ├── api.ts                # API client
│   │   │   └── auth.ts               # Auth helpers
│   │   └── styles/
│   │       └── global.css
│   ├── public/
│   ├── astro.config.mjs
│   ├── tailwind.config.js
│   └── package.json
│
├── ai-training/                      # Model training (Omar)
│   ├── notebooks/
│   │   └── train_pothole_yolov8.ipynb
│   ├── scripts/
│   │   ├── download_datasets.py
│   │   ├── prepare_dataset.py
│   │   └── export_tflite.py
│   ├── configs/
│   │   └── training_config.yaml
│   └── README.md
│
├── docker/                           # Docker configurations
│   ├── backend.Dockerfile
│   ├── dashboard.Dockerfile
│   └── nginx/
│       ├── nginx.conf
│       └── sites/
│
├── scripts/                          # Deployment scripts
│   ├── setup-server.sh
│   └── deploy.sh
│
├── docker-compose.yml                # Local development
├── docker-compose.prod.yml           # Production
├── .env.example
├── .gitignore
└── README.md
```

---

## Team Responsibilities

### Omar (Mobile + AI)

| Week | Primary Focus | Key Deliverables |
|------|---------------|------------------|
| Week 1 | AI Model Training | Trained YOLOv8n model, TFLite export |
| Week 2 | Android Core Features | Camera preview, GPS, AI inference on device |
| Week 3 | Integration & Upload | API integration, offline queue, deduplication |
| Week 4 | Testing & Polish | Bug fixes, performance optimization, UI polish |

**Key Skills Needed**:
- Python (for model training)
- Kotlin + Jetpack Compose
- TensorFlow Lite / LiteRT
- Android CameraX

### Hamza (Backend + Dashboard)

| Week | Primary Focus | Key Deliverables |
|------|---------------|------------------|
| Week 1 | Backend API Core | Database schema, auth, pothole CRUD endpoints |
| Week 2 | Dashboard MVP | Login, pothole list view, map integration |
| Week 3 | S3 + Advanced Features | Image upload, vehicle management, statistics |
| Week 4 | Deployment | Hetzner setup, Docker, SSL, production deploy |

**Key Skills Needed**:
- TypeScript + Node.js
- PostgreSQL + PostGIS
- Astro + React
- Docker + Linux administration

---

## Database Schema

Based on the project report, the core entities are:

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│     users       │     │    vehicles     │     │    potholes     │
├─────────────────┤     ├─────────────────┤     ├─────────────────┤
│ id (PK)         │     │ id (PK)         │     │ id (PK)         │
│ email           │────▶│ user_id (FK)    │     │ vehicle_id (FK) │◀────┐
│ password_hash   │     │ name            │     │ latitude        │     │
│ name            │     │ serial_number   │     │ longitude       │     │
│ role            │     │ is_active       │     │ location (GIS)  │     │
│ created_at      │     │ last_active     │     │ confidence      │     │
│ updated_at      │     │ created_at      │     │ image_url       │     │
└─────────────────┘     └────────┬────────┘     │ status          │     │
                                 │              │ severity        │     │
                                 │              │ detected_at     │     │
                                 └──────────────│ created_at      │     │
                                                │ updated_at      │     │
                                                └─────────────────┘     │
                                                         │              │
                                                         └──────────────┘
```

**Key Fields**:
- `potholes.location`: PostGIS GEOGRAPHY point for spatial queries
- `potholes.status`: ENUM ('unverified', 'verified', 'repaired', 'false_positive')
- `potholes.severity`: ENUM ('low', 'medium', 'high') - based on AI confidence
- `users.role`: ENUM ('admin', 'operator', 'viewer')

---

## Data Flow

### Detection to Upload Pipeline

```
┌────────────────────────────────────────────────────────────────────────────┐
│ MOBILE APP                                                                  │
│                                                                             │
│  1. Camera captures frame (30 FPS)                                         │
│           │                                                                 │
│           ▼                                                                 │
│  2. Skip frames for performance (process every 2nd-3rd frame)              │
│           │                                                                 │
│           ▼                                                                 │
│  3. Preprocess: Resize to 640x640, normalize pixels to 0-1                 │
│           │                                                                 │
│           ▼                                                                 │
│  4. LiteRT inference (~50-100ms on modern phones)                          │
│           │                                                                 │
│           ▼                                                                 │
│  5. Parse output: Extract bounding boxes with confidence > 0.5             │
│           │                                                                 │
│           ▼                                                                 │
│  6. Apply NMS (Non-Maximum Suppression) to remove duplicates               │
│           │                                                                 │
│           ▼                                                                 │
│  7. If detection found:                                                     │
│      a. Get current GPS coordinates                                        │
│      b. Check local deduplication (10m radius, last 60 seconds)           │
│      c. If NOT duplicate:                                                  │
│         - Crop detection region from frame                                 │
│         - Save to Room database (pending upload)                           │
│         - Enqueue upload via WorkManager                                   │
│           │                                                                 │
│           ▼                                                                 │
│  8. WorkManager triggers upload when network available                     │
│           │                                                                 │
└───────────┼────────────────────────────────────────────────────────────────┘
            │
            │  HTTPS POST /api/potholes
            │  Body: { image, latitude, longitude, confidence, vehicleId }
            ▼
┌───────────────────────────────────────────────────────────────────────────┐
│ BACKEND SERVER                                                             │
│                                                                            │
│  9. Validate JWT token                                                     │
│           │                                                                │
│           ▼                                                                │
│  10. Server-side deduplication:                                            │
│      - PostGIS query: Find potholes within 15m radius                     │
│      - If existing pothole found:                                          │
│        → Increment confirmation_count                                      │
│        → Return { isDuplicate: true, existingId }                         │
│           │                                                                │
│           ▼                                                                │
│  11. If new pothole:                                                       │
│      a. Upload image to S3                                                 │
│      b. Insert record into PostgreSQL                                      │
│      c. Return { success: true, potholeId }                               │
│           │                                                                │
└───────────┼───────────────────────────────────────────────────────────────┘
            │
            ▼
┌───────────────────────────────────────────────────────────────────────────┐
│ DASHBOARD                                                                  │
│                                                                            │
│  12. Real-time or polling update                                           │
│  13. New pothole appears on map                                            │
│  14. Admin can verify/reject/mark as repaired                             │
│                                                                            │
└───────────────────────────────────────────────────────────────────────────┘
```

---

## Key Design Decisions

### 1. Edge Computing (On-Device AI)

**Decision**: Run AI inference on the phone, not in the cloud.

**Why**:
- No constant video streaming (saves bandwidth: ~500MB/hour vs ~5MB/hour)
- Works offline (queues uploads for later)
- Lower latency (~100ms vs ~500ms+ for cloud)
- Better privacy: raw video never leaves device
- No cloud GPU costs

**Trade-off**: Limited to smaller models (YOLOv8n vs YOLOv8x)

### 2. Dual Deduplication Strategy

**Decision**: Deduplicate both on-device AND on server.

**Mobile deduplication (real-time)**:
- Prevents rapid-fire reports of same pothole while driving
- Simple in-memory check: "Was there a detection within 10m in last 60 seconds?"
- Fast, no network needed

**Server deduplication (persistent)**:
- Handles reports from multiple vehicles on different days
- PostGIS spatial query: `ST_DWithin(location, point, 15 meters)`
- Updates confirmation_count for existing potholes

### 3. YOLOv8n Over Newer Versions

**Decision**: Use YOLOv8n, not YOLOv10/v11/v12.

**Why**:
- Most mature TFLite export pipeline
- YOLOv10 removed NMS (causes issues on mobile)
- YOLOv11/v12 have limited mobile deployment examples
- 6MB model size fits mobile constraints
- Sufficient accuracy (80%+ mAP) for this use case

### 4. Astro + React Over Pure React/Next.js

**Decision**: Use Astro with React islands.

**Why**:
- Dashboard is mostly static (settings, vehicle list)
- Only map and data tables need interactivity
- Smaller JS bundle: ~50KB vs ~200KB+ for full React SPA
- Faster initial page load
- Simpler deployment (static files + API)

### 5. PostgreSQL + PostGIS Over MongoDB

**Decision**: Use relational database with spatial extension.

**Why**:
- PostGIS enables efficient spatial queries (critical for deduplication)
- Relational model fits our data (users → vehicles → potholes)
- Strong consistency for status updates
- Better for complex analytics queries
- Well-supported by Drizzle ORM

### 6. AWS S3 Over Local Storage

**Decision**: Store images in S3, not on VPS filesystem.

**Why**:
- Scales independently of compute
- Cheaper for storage ($0.023/GB vs VPS disk expansion)
- Built-in redundancy
- CDN-ready (CloudFront if needed later)
- Hetzner VPS disk is limited and expensive to expand

---

## Environment Variables

### Backend (.env)

```env
# Server
NODE_ENV=production
PORT=3000
HOST=0.0.0.0

# Database
DATABASE_URL=postgresql://pothole_user:password@localhost:5432/pothole_db

# JWT
JWT_SECRET=your-256-bit-secret-key-here
JWT_EXPIRES_IN=7d

# AWS S3
AWS_ACCESS_KEY_ID=AKIA...
AWS_SECRET_ACCESS_KEY=your-secret-key
AWS_REGION=eu-central-1
AWS_S3_BUCKET=pothole-detection-images

# CORS
CORS_ORIGIN=https://dashboard.yoursite.com
```

### Dashboard (.env)

```env
# API
PUBLIC_API_URL=https://api.yoursite.com

# Build
PUBLIC_SITE_URL=https://dashboard.yoursite.com
```

### Android (local.properties or BuildConfig)

```properties
# API Configuration
API_BASE_URL=https://api.yoursite.com
API_TIMEOUT_MS=30000

# Detection Settings
DETECTION_CONFIDENCE_THRESHOLD=0.30
DETECTION_NMS_THRESHOLD=0.45
DEDUP_RADIUS_METERS=10
DEDUP_TIME_WINDOW_MS=60000
```

---

## Success Metrics

| Metric | Minimum | Target | How to Measure |
|--------|---------|--------|----------------|
| Model mAP@50 | 75% | >80% | Ultralytics validation |
| Inference time | <150ms | <100ms | On-device profiling |
| App frame rate | 10 FPS | >15 FPS | With detection enabled |
| API response (P95) | <1s | <500ms | Server logs |
| Upload success | 90% | >98% | With retry queue |
| Dashboard load | <5s | <3s | Lighthouse |
| False positive rate | <30% | <20% | Manual review |

---

## Risk Mitigation

| Risk | Impact | Mitigation |
|------|--------|------------|
| Model accuracy too low | High | Collect more local training data, try YOLOv8s |
| TFLite export issues | Medium | Fall back to ONNX Runtime Mobile |
| Phone overheating | Medium | Reduce inference frequency, add cooldown |
| Network failures | Low | WorkManager handles retries automatically |
| S3 costs spike | Low | Set up billing alerts, use lifecycle policies |
| Deployment issues | Medium | Test locally with Docker first |

---

## Quick Links

- [AI Model Training Plan](./01-ai-model.md)
- [Android App Implementation](./02-android-app.md)
- [Backend API Design](./03-backend-api.md)
- [Web Dashboard Plan](./04-web-dashboard.md)
- [Deployment Guide](./05-deployment.md)
- [Timeline & Task Breakdown](./06-timeline.md)
- [Current Implementation Notes](./07-current-implementation.md)
