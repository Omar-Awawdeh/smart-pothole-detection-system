# Backend API Implementation Plan

> Note: This file is an implementation plan. For currently deployed behavior and schema notes, see `docs/07-current-implementation.md`.

**Owner**: Hamza  
**Duration**: Weeks 1-3 (Days 1-21)  
**Stack**: Fastify + TypeScript + PostgreSQL/PostGIS + AWS S3

---

## Objective

Build a REST API server that:
1. Authenticates mobile app and dashboard users (JWT)
2. Receives pothole reports from mobile devices
3. Performs server-side deduplication using spatial queries
4. Stores images in AWS S3
5. Provides data to the web dashboard
6. Manages vehicles and users

---

## Architecture Overview

```
┌────────────────────────────────────────────────────────────────────────┐
│                         FASTIFY API SERVER                              │
│                                                                         │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                        Request Flow                               │  │
│  │                                                                   │  │
│  │  Request → CORS → Auth → Validation → Route Handler → Response   │  │
│  │                                                                   │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                                                                         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐   │
│  │   Plugins   │  │   Routes    │  │  Services   │  │     DB      │   │
│  │             │  │             │  │             │  │             │   │
│  │ - Auth      │  │ - /auth     │  │ - Auth      │  │ - Drizzle   │   │
│  │ - Database  │  │ - /potholes │  │ - Pothole   │  │ - PostGIS   │   │
│  │ - S3        │  │ - /vehicles │  │ - Vehicle   │  │ - Queries   │   │
│  │ - Multipart │  │ - /users    │  │ - Upload    │  │             │   │
│  │ - CORS      │  │ - /stats    │  │ - Dedup     │  │             │   │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘   │
│                                                                         │
└────────────────────────────────────────────────────────────────────────┘
                    │                               │
                    ▼                               ▼
         ┌──────────────────┐            ┌──────────────────┐
         │   PostgreSQL     │            │     AWS S3       │
         │   + PostGIS      │            │                  │
         │                  │            │  pothole-images/ │
         │  - users         │            │  └── {vehicle}/  │
         │  - potholes      │            │      └── {date}/ │
         │  - vehicles      │            │          └── .jpg│
         └──────────────────┘            └──────────────────┘
```

---

## Project Structure

```
backend/
├── src/
│   ├── index.ts                 # Entry point
│   ├── server.ts                # Fastify app setup
│   ├── config.ts                # Environment configuration
│   │
│   ├── routes/
│   │   ├── index.ts             # Route registration
│   │   ├── auth.routes.ts       # Authentication endpoints
│   │   ├── pothole.routes.ts    # Pothole CRUD
│   │   ├── vehicle.routes.ts    # Vehicle management
│   │   ├── user.routes.ts       # User management
│   │   └── stats.routes.ts      # Statistics/analytics
│   │
│   ├── services/
│   │   ├── auth.service.ts      # JWT, password hashing
│   │   ├── pothole.service.ts   # Pothole business logic
│   │   ├── dedup.service.ts     # Spatial deduplication
│   │   ├── vehicle.service.ts   # Vehicle operations
│   │   ├── user.service.ts      # User operations
│   │   └── upload.service.ts    # S3 upload handling
│   │
│   ├── plugins/
│   │   ├── auth.plugin.ts       # JWT verification decorator
│   │   ├── database.plugin.ts   # Drizzle connection
│   │   └── s3.plugin.ts         # S3 client setup
│   │
│   ├── db/
│   │   ├── index.ts             # Database client export
│   │   ├── schema.ts            # Drizzle schema definitions
│   │   └── migrations/          # SQL migrations
│   │       └── 0001_initial.sql
│   │
│   ├── utils/
│   │   ├── errors.ts            # Custom error classes
│   │   ├── spatial.ts           # PostGIS query helpers
│   │   └── validation.ts        # Zod schemas
│   │
│   └── types/
│       ├── fastify.d.ts         # Fastify type extensions
│       └── index.ts             # Shared types
│
├── drizzle.config.ts            # Drizzle ORM config
├── package.json
├── tsconfig.json
└── .env.example
```

---

## Database Schema

Based on the project report, implementing with PostGIS for spatial queries:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         DATABASE SCHEMA                                  │
│                                                                          │
│  ┌─────────────────────┐                                                │
│  │       users         │                                                │
│  ├─────────────────────┤                                                │
│  │ id          UUID PK │──────┐                                         │
│  │ email       VARCHAR │      │                                         │
│  │ password    VARCHAR │      │                                         │
│  │ name        VARCHAR │      │                                         │
│  │ role        ENUM    │      │  role: 'admin' | 'operator' | 'viewer' │
│  │ created_at  TIMESTAMP      │                                         │
│  │ updated_at  TIMESTAMP      │                                         │
│  └─────────────────────┘      │                                         │
│                               │                                         │
│  ┌─────────────────────┐      │                                         │
│  │      vehicles       │      │                                         │
│  ├─────────────────────┤      │                                         │
│  │ id          UUID PK │──────┼──────┐                                  │
│  │ user_id     UUID FK │◀─────┘      │                                  │
│  │ name        VARCHAR │             │                                  │
│  │ serial_num  VARCHAR │             │                                  │
│  │ is_active   BOOLEAN │             │                                  │
│  │ last_active TIMESTAMP             │                                  │
│  │ created_at  TIMESTAMP             │                                  │
│  └─────────────────────┘             │                                  │
│                                      │                                  │
│  ┌─────────────────────┐             │                                  │
│  │      potholes       │             │                                  │
│  ├─────────────────────┤             │                                  │
│  │ id          UUID PK │             │                                  │
│  │ vehicle_id  UUID FK │◀────────────┘                                  │
│  │ latitude    DECIMAL │                                                │
│  │ longitude   DECIMAL │                                                │
│  │ location    GEOGRAPHY(Point) │  ◀── PostGIS spatial column          │
│  │ confidence  DECIMAL │                                                │
│  │ image_url   VARCHAR │             ◀── S3 URL                        │
│  │ status      ENUM    │             status: see below                  │
│  │ severity    ENUM    │             severity: 'low'|'medium'|'high'   │
│  │ confirm_cnt INTEGER │             ◀── Times reported by diff vehicles│
│  │ detected_at TIMESTAMP             ◀── When first detected           │
│  │ repaired_at TIMESTAMP             ◀── When marked repaired          │
│  │ created_at  TIMESTAMP                                                │
│  │ updated_at  TIMESTAMP                                                │
│  └─────────────────────┘                                                │
│                                                                          │
│  Status ENUM: 'unverified' | 'verified' | 'repaired' | 'false_positive' │
│                                                                          │
│  Spatial Index: CREATE INDEX idx_potholes_location                      │
│                 ON potholes USING GIST(location);                       │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### Schema Definition (Drizzle ORM)

```
SCHEMA DEFINITION:

# Users table
users:
  id: uuid PRIMARY KEY DEFAULT gen_random_uuid()
  email: varchar(255) UNIQUE NOT NULL
  password_hash: varchar(255) NOT NULL
  name: varchar(100) NOT NULL
  role: user_role_enum DEFAULT 'viewer'
  created_at: timestamp DEFAULT now()
  updated_at: timestamp DEFAULT now()

# Vehicles table
vehicles:
  id: uuid PRIMARY KEY DEFAULT gen_random_uuid()
  user_id: uuid REFERENCES users(id)
  name: varchar(100) NOT NULL
  serial_number: varchar(50) UNIQUE NOT NULL
  is_active: boolean DEFAULT true
  last_active_at: timestamp
  created_at: timestamp DEFAULT now()

# Potholes table
potholes:
  id: uuid PRIMARY KEY DEFAULT gen_random_uuid()
  vehicle_id: uuid REFERENCES vehicles(id)
  latitude: decimal(10, 7) NOT NULL
  longitude: decimal(10, 7) NOT NULL
  location: geography(Point, 4326) NOT NULL  # PostGIS
  confidence: decimal(4, 3) NOT NULL
  image_url: varchar(500)
  status: pothole_status_enum DEFAULT 'unverified'
  severity: severity_enum DEFAULT 'medium'
  confirmation_count: integer DEFAULT 1
  detected_at: timestamp NOT NULL
  repaired_at: timestamp
  created_at: timestamp DEFAULT now()
  updated_at: timestamp DEFAULT now()

# Indexes
CREATE INDEX idx_potholes_location ON potholes USING GIST(location);
CREATE INDEX idx_potholes_status ON potholes(status);
CREATE INDEX idx_potholes_detected_at ON potholes(detected_at);
CREATE INDEX idx_vehicles_user_id ON vehicles(user_id);
```

---

## API Endpoints

### Authentication Routes (`/api/auth`)

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/auth/register` | Register new user (admin only) | Yes (admin) |
| POST | `/api/auth/login` | Login, returns JWT token | No |
| POST | `/api/auth/refresh` | Refresh access token | Yes |
| POST | `/api/auth/logout` | Invalidate refresh token | Yes |
| GET | `/api/auth/me` | Get current user info | Yes |

### Pothole Routes (`/api/potholes`)

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/potholes` | Create new pothole report (with image) | Yes |
| GET | `/api/potholes` | List potholes (paginated, filterable) | Yes |
| GET | `/api/potholes/:id` | Get single pothole details | Yes |
| PATCH | `/api/potholes/:id` | Update pothole (status, severity) | Yes (admin/operator) |
| DELETE | `/api/potholes/:id` | Delete pothole | Yes (admin) |
| GET | `/api/potholes/nearby` | Find potholes near a point | Yes |
| GET | `/api/potholes/export` | Export potholes as CSV/GeoJSON | Yes (admin) |

### Vehicle Routes (`/api/vehicles`)

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/vehicles` | Register new vehicle | Yes (admin) |
| GET | `/api/vehicles` | List all vehicles | Yes |
| GET | `/api/vehicles/:id` | Get vehicle details | Yes |
| PATCH | `/api/vehicles/:id` | Update vehicle | Yes (admin) |
| DELETE | `/api/vehicles/:id` | Delete vehicle | Yes (admin) |
| GET | `/api/vehicles/:id/potholes` | Get potholes reported by vehicle | Yes |

### User Routes (`/api/users`)

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/users` | List all users | Yes (admin) |
| GET | `/api/users/:id` | Get user details | Yes (admin) |
| PATCH | `/api/users/:id` | Update user | Yes (admin) |
| DELETE | `/api/users/:id` | Delete user | Yes (admin) |

### Statistics Routes (`/api/stats`)

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/stats/overview` | Dashboard overview stats | Yes |
| GET | `/api/stats/daily` | Daily detection counts | Yes |
| GET | `/api/stats/by-status` | Count by status | Yes |
| GET | `/api/stats/by-vehicle` | Count by vehicle | Yes |
| GET | `/api/stats/heatmap` | Aggregated location data | Yes |

---

## Core Service Logic

### Pothole Creation with Deduplication

```
FUNCTION createPothole(request):
    
    # 1. Validate input
    VALIDATE request.body AGAINST PotholeCreateSchema
    
    # 2. Check for duplicates using PostGIS
    existingPothole = QUERY:
        SELECT * FROM potholes
        WHERE ST_DWithin(
            location,
            ST_MakePoint(request.longitude, request.latitude)::geography,
            15  -- meters
        )
        AND status != 'repaired'
        AND status != 'false_positive'
        ORDER BY ST_Distance(location, ST_MakePoint(request.longitude, request.latitude)::geography)
        LIMIT 1
    
    # 3. If duplicate found, increment confirmation count
    IF existingPothole:
        UPDATE potholes
        SET confirmation_count = confirmation_count + 1,
            updated_at = now()
        WHERE id = existingPothole.id
        
        RETURN {
            isDuplicate: true,
            existingId: existingPothole.id,
            confirmationCount: existingPothole.confirmation_count + 1
        }
    
    # 4. Not a duplicate - upload image to S3
    imageKey = "{vehicleId}/{date}/{uuid}.jpg"
    imageUrl = UPLOAD_TO_S3(request.image, imageKey)
    
    # 5. Determine severity based on confidence
    severity = CALCULATE_SEVERITY(request.confidence)
    
    # 6. Insert new pothole
    newPothole = INSERT INTO potholes (
        vehicle_id,
        latitude,
        longitude,
        location,  -- ST_MakePoint(lng, lat)::geography
        confidence,
        image_url,
        status,
        severity,
        detected_at
    ) VALUES (
        request.vehicleId,
        request.latitude,
        request.longitude,
        ST_MakePoint(request.longitude, request.latitude)::geography,
        request.confidence,
        imageUrl,
        'unverified',
        severity,
        request.timestamp
    )
    RETURNING *
    
    # 7. Update vehicle last_active
    UPDATE vehicles
    SET last_active_at = now()
    WHERE id = request.vehicleId
    
    RETURN {
        isDuplicate: false,
        pothole: newPothole
    }


FUNCTION CALCULATE_SEVERITY(confidence):
    IF confidence >= 0.8:
        RETURN 'high'
    ELSE IF confidence >= 0.6:
        RETURN 'medium'
    ELSE:
        RETURN 'low'
```

### Listing Potholes with Filters

```
FUNCTION listPotholes(query):
    
    # Default pagination
    page = query.page OR 1
    limit = MIN(query.limit OR 20, 100)
    offset = (page - 1) * limit
    
    # Build dynamic query
    baseQuery = SELECT * FROM potholes WHERE 1=1
    countQuery = SELECT COUNT(*) FROM potholes WHERE 1=1
    
    # Apply filters
    IF query.status:
        baseQuery += AND status = query.status
        countQuery += AND status = query.status
    
    IF query.severity:
        baseQuery += AND severity = query.severity
        countQuery += AND severity = query.severity
    
    IF query.vehicleId:
        baseQuery += AND vehicle_id = query.vehicleId
        countQuery += AND vehicle_id = query.vehicleId
    
    IF query.startDate:
        baseQuery += AND detected_at >= query.startDate
        countQuery += AND detected_at >= query.startDate
    
    IF query.endDate:
        baseQuery += AND detected_at <= query.endDate
        countQuery += AND detected_at <= query.endDate
    
    IF query.bounds:  # Map viewport: {north, south, east, west}
        baseQuery += AND ST_Within(
            location::geometry,
            ST_MakeEnvelope(query.bounds.west, query.bounds.south, 
                           query.bounds.east, query.bounds.north, 4326)
        )
    
    # Sorting
    sortField = query.sortBy OR 'detected_at'
    sortOrder = query.sortOrder OR 'DESC'
    baseQuery += ORDER BY {sortField} {sortOrder}
    
    # Pagination
    baseQuery += LIMIT {limit} OFFSET {offset}
    
    # Execute queries
    potholes = EXECUTE(baseQuery)
    totalCount = EXECUTE(countQuery)
    
    RETURN {
        data: potholes,
        pagination: {
            page: page,
            limit: limit,
            total: totalCount,
            totalPages: CEIL(totalCount / limit)
        }
    }
```

### S3 Image Upload

```
FUNCTION uploadImage(imageBuffer, vehicleId):
    
    # Generate unique key
    date = FORMAT(now(), 'YYYY-MM-DD')
    filename = GENERATE_UUID() + '.jpg'
    key = "potholes/{vehicleId}/{date}/{filename}"
    
    # Upload to S3
    result = S3_CLIENT.putObject({
        Bucket: S3_BUCKET,
        Key: key,
        Body: imageBuffer,
        ContentType: 'image/jpeg',
        ACL: 'private'  # Use signed URLs for access
    })
    
    # Return the full URL or key
    RETURN "https://{S3_BUCKET}.s3.{REGION}.amazonaws.com/{key}"
    
    # Alternative: Return just the key and generate signed URLs on demand
    # RETURN key
```

### Authentication Flow

```
FUNCTION login(email, password):
    
    # Find user
    user = SELECT * FROM users WHERE email = email
    
    IF NOT user:
        THROW UnauthorizedError("Invalid credentials")
    
    # Verify password
    isValid = BCRYPT_COMPARE(password, user.password_hash)
    
    IF NOT isValid:
        THROW UnauthorizedError("Invalid credentials")
    
    # Generate tokens
    accessToken = JWT_SIGN(
        payload: { userId: user.id, role: user.role },
        secret: JWT_SECRET,
        expiresIn: '15m'
    )
    
    refreshToken = JWT_SIGN(
        payload: { userId: user.id, type: 'refresh' },
        secret: JWT_REFRESH_SECRET,
        expiresIn: '7d'
    )
    
    RETURN {
        accessToken,
        refreshToken,
        user: {
            id: user.id,
            email: user.email,
            name: user.name,
            role: user.role
        }
    }


FUNCTION verifyToken(token):
    
    TRY:
        decoded = JWT_VERIFY(token, JWT_SECRET)
        RETURN decoded
    CATCH TokenExpiredError:
        THROW UnauthorizedError("Token expired")
    CATCH:
        THROW UnauthorizedError("Invalid token")
```

---

## Validation Schemas (Zod)

```
VALIDATION SCHEMAS:

# Auth
LoginSchema:
    email: string().email()
    password: string().min(8)

RegisterSchema:
    email: string().email()
    password: string().min(8)
    name: string().min(2).max(100)
    role: enum(['admin', 'operator', 'viewer']).optional()

# Pothole
PotholeCreateSchema:
    image: binary()  # multipart file
    latitude: number().min(-90).max(90)
    longitude: number().min(-180).max(180)
    confidence: number().min(0).max(1)
    vehicleId: string().uuid()
    timestamp: number()  # Unix timestamp ms

PotholeUpdateSchema:
    status: enum(['unverified', 'verified', 'repaired', 'false_positive']).optional()
    severity: enum(['low', 'medium', 'high']).optional()

PotholeQuerySchema:
    page: number().int().positive().optional()
    limit: number().int().min(1).max(100).optional()
    status: enum([...]).optional()
    severity: enum([...]).optional()
    vehicleId: string().uuid().optional()
    startDate: string().datetime().optional()
    endDate: string().datetime().optional()
    sortBy: enum(['detected_at', 'confidence', 'status']).optional()
    sortOrder: enum(['ASC', 'DESC']).optional()

# Vehicle
VehicleCreateSchema:
    name: string().min(2).max(100)
    serialNumber: string().min(5).max(50)

VehicleUpdateSchema:
    name: string().min(2).max(100).optional()
    isActive: boolean().optional()
```

---

## Error Handling

```
ERROR_CLASSES:

class AppError extends Error:
    statusCode: number
    code: string
    
class ValidationError extends AppError:
    statusCode = 400
    code = 'VALIDATION_ERROR'

class UnauthorizedError extends AppError:
    statusCode = 401
    code = 'UNAUTHORIZED'

class ForbiddenError extends AppError:
    statusCode = 403
    code = 'FORBIDDEN'

class NotFoundError extends AppError:
    statusCode = 404
    code = 'NOT_FOUND'

class ConflictError extends AppError:
    statusCode = 409
    code = 'CONFLICT'

class InternalError extends AppError:
    statusCode = 500
    code = 'INTERNAL_ERROR'


ERROR_HANDLER (Fastify):

    IF error instanceof AppError:
        reply.status(error.statusCode).send({
            success: false,
            error: {
                code: error.code,
                message: error.message
            }
        })
    
    ELSE IF error instanceof ZodError:
        reply.status(400).send({
            success: false,
            error: {
                code: 'VALIDATION_ERROR',
                message: 'Invalid request data',
                details: error.errors
            }
        })
    
    ELSE:
        LOG.error(error)
        reply.status(500).send({
            success: false,
            error: {
                code: 'INTERNAL_ERROR',
                message: 'An unexpected error occurred'
            }
        })
```

---

## AWS S3 Setup Guide

### Step 1: Create AWS Account (if needed)

```
1. Go to aws.amazon.com
2. Create account (requires credit card)
3. Free tier includes 5GB S3 storage for 12 months
```

### Step 2: Create S3 Bucket

```
1. Go to S3 Console
2. Click "Create bucket"
3. Settings:
   - Bucket name: pothole-detection-images-{unique-suffix}
   - Region: eu-central-1 (Frankfurt) - close to Hetzner
   - Block all public access: YES (we'll use signed URLs)
   - Bucket versioning: Disabled
   - Encryption: SSE-S3 (default)
4. Create bucket
```

### Step 3: Create IAM User

```
1. Go to IAM Console
2. Users → Create user
3. User name: pothole-api-user
4. Attach policies directly:
   - Create custom policy:

{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "s3:PutObject",
                "s3:GetObject",
                "s3:DeleteObject"
            ],
            "Resource": "arn:aws:s3:::pothole-detection-images-*/*"
        },
        {
            "Effect": "Allow",
            "Action": [
                "s3:ListBucket"
            ],
            "Resource": "arn:aws:s3:::pothole-detection-images-*"
        }
    ]
}

5. Create user
6. Create access key (Application running outside AWS)
7. Save Access Key ID and Secret Access Key
```

### Step 4: Configure CORS (for direct browser uploads if needed)

```
CORS Configuration (S3 Bucket → Permissions → CORS):

[
    {
        "AllowedHeaders": ["*"],
        "AllowedMethods": ["GET", "PUT", "POST"],
        "AllowedOrigins": ["https://dashboard.yoursite.com"],
        "ExposeHeaders": ["ETag"]
    }
]
```

### Step 5: Lifecycle Rules (Optional, Cost Saving)

```
1. Bucket → Management → Lifecycle rules
2. Create rule:
   - Name: delete-old-images
   - Filter: Prefix = potholes/
   - Actions:
     - Expire current versions: 365 days
     - (Keeps images for 1 year, then auto-deletes)
```

---

## Environment Configuration

```
# .env.example

# Server
NODE_ENV=development
PORT=3000
HOST=0.0.0.0
LOG_LEVEL=info

# Database
DATABASE_URL=postgresql://pothole_user:password@localhost:5432/pothole_db

# JWT
JWT_SECRET=your-256-bit-secret-generate-with-openssl-rand-hex-32
JWT_REFRESH_SECRET=another-256-bit-secret
JWT_EXPIRES_IN=15m
JWT_REFRESH_EXPIRES_IN=7d

# AWS S3
AWS_ACCESS_KEY_ID=AKIA...
AWS_SECRET_ACCESS_KEY=your-secret-key
AWS_REGION=eu-central-1
AWS_S3_BUCKET=pothole-detection-images

# CORS
CORS_ORIGINS=http://localhost:4321,https://dashboard.yoursite.com

# Rate Limiting
RATE_LIMIT_MAX=100
RATE_LIMIT_WINDOW_MS=60000
```

---

## Testing Strategy

### Unit Tests

```
UNIT_TESTS:

# Services
- auth.service.test.ts
  - hashPassword_returnsHash
  - verifyPassword_correctPassword_returnsTrue
  - verifyPassword_wrongPassword_returnsFalse
  - generateTokens_returnsValidTokens

- dedup.service.test.ts
  - findNearbyPothole_exists_returnsId
  - findNearbyPothole_notExists_returnsNull
  - findNearbyPothole_repaired_ignores

- pothole.service.test.ts
  - createPothole_new_createsRecord
  - createPothole_duplicate_incrementsCount
  - calculateSeverity_highConfidence_returnsHigh
```

### Integration Tests

```
INTEGRATION_TESTS:

# API Endpoints
- POST /api/auth/login
  - validCredentials_returnsTokens
  - invalidCredentials_returns401
  
- POST /api/potholes
  - validRequest_createsPothole
  - duplicateLocation_incrementsCount
  - invalidImage_returns400
  - noAuth_returns401

- GET /api/potholes
  - returnsPagedResults
  - filterByStatus_works
  - filterByDateRange_works
```

### Test Database Setup

```
For testing, use a separate database:

DATABASE_URL_TEST=postgresql://pothole_user:password@localhost:5432/pothole_db_test

Before each test suite:
1. Run migrations
2. Seed test data

After each test:
1. Clean up created records

After all tests:
1. Drop test database (optional)
```

---

## Performance Considerations

### Database Indexes

```
CRITICAL_INDEXES:

# Spatial index for deduplication queries (most important)
CREATE INDEX idx_potholes_location ON potholes USING GIST(location);

# Status filter (common query)
CREATE INDEX idx_potholes_status ON potholes(status);

# Date range queries
CREATE INDEX idx_potholes_detected_at ON potholes(detected_at DESC);

# Vehicle lookup
CREATE INDEX idx_potholes_vehicle_id ON potholes(vehicle_id);

# Combined index for common dashboard query
CREATE INDEX idx_potholes_status_date ON potholes(status, detected_at DESC);
```

### Query Optimization

```
OPTIMIZATION_TIPS:

1. Deduplication query - ensure spatial index is used:
   EXPLAIN ANALYZE SELECT ... WHERE ST_DWithin(...)
   Should show "Index Scan using idx_potholes_location"

2. Pagination - use keyset pagination for large datasets:
   Instead of: OFFSET 10000 LIMIT 20
   Use: WHERE detected_at < last_seen_date ORDER BY detected_at DESC LIMIT 20

3. Dashboard aggregations - consider materialized views:
   CREATE MATERIALIZED VIEW daily_stats AS
   SELECT date_trunc('day', detected_at) as day, count(*), ...
   GROUP BY 1;
   
   Refresh periodically: REFRESH MATERIALIZED VIEW daily_stats;
```

### Connection Pooling

```
Use pg connection pool:

pool = new Pool({
    connectionString: DATABASE_URL,
    max: 20,              # Maximum connections
    idleTimeoutMillis: 30000,
    connectionTimeoutMillis: 2000
})
```

---

## Timeline

| Day | Tasks | Deliverables |
|-----|-------|--------------|
| Day 1 | Project setup, Fastify scaffold, TypeScript config | Empty server running |
| Day 2 | Database setup, Drizzle schema, migrations | DB connected |
| Day 3 | Auth plugin, JWT implementation | Login/register working |
| Day 4 | Pothole routes (CRUD basics) | Basic CRUD working |
| Day 5 | PostGIS integration, deduplication | Spatial queries working |
| Day 6 | S3 setup, image upload | Images uploading |
| Day 7 | Vehicle routes | Vehicle management done |
| Day 8-14 | (Continue with dashboard development) | |
| Day 15 | Stats routes, aggregations | Analytics endpoints |
| Day 16 | Testing, bug fixes | API stable |
| Day 17 | Documentation, cleanup | API ready |

**Total backend-only: ~7 days, then parallel with dashboard**

---

## Deliverables

```
At the end of Week 2, Hamza should have:

□ Working API server
  └── All endpoints functional
  └── JWT authentication
  └── PostGIS deduplication
  └── S3 image upload

□ Database
  └── Schema migrated
  └── Indexes created
  └── Test data seeded

□ Documentation
  └── API endpoint list
  └── Environment setup guide
  └── S3 configuration guide

□ Testing
  └── Core services tested
  └── API integration tests
```

---

## Next Steps

After backend API is ready:

1. **Coordinate with Omar** for mobile app integration testing
2. **Start dashboard development** (see [Web Dashboard Plan](./04-web-dashboard.md))
3. **Set up staging environment** on Hetzner (see [Deployment Guide](./05-deployment.md))

Continue to [Web Dashboard Plan](./04-web-dashboard.md)
