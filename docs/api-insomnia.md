# Pothole Detection API

---

## 1. Login

```
POST {{ base_url }}/api/auth/login
```

**Headers:**

| Header       | Value            |
| ------------ | ---------------- |
| Content-Type | application/json |

**Body (JSON):**

```json
{
  "email": "user@example.com",
  "password": "your_password"
}
```

**Expected Response (200 OK):**

```json
{
  "user": {
    "id": "uuid-string",
    "email": "user@example.com",
    "name": "Omar Awawdeh",
    "role": "user"
  },
  "tokens": {
    "access_token": "eyJhbGciOiJIUzI1NiIs...",
    "refresh_token": "eyJhbGciOiJIUzI1NiIs..."
  }
}
```

---

## 2. Upload Pothole Report

```
POST {{ base_url }}/api/potholes
```

**Headers:**

| Header        | Value                     |
| ------------- | ------------------------- |
| Authorization | Bearer {{ access_token }} |

**Body (Multipart Form):**

In Insomnia, set the body type to **Multipart Form** and add these fields:

| Field Name | Type | Value / Description                     |
| ---------- | ---- | --------------------------------------- |
| image      | File | Select a `.jpg` file from your computer |
| latitude   | Text | `31.9539`                               |
| longitude  | Text | `35.9106`                               |
| confidence | Text | `0.87`                                  |
| vehicleId  | Text | `vehicle-001`                           |
| timestamp  | Text | `1708456800000`                         |

**Expected Response (200 OK / 201 Created):**

```json
{
  "id": "uuid-string",
  "isDuplicate": false,
  "existingId": null,
  "confirmationCount": null,
  "latitude": 31.9539,
  "longitude": 35.9106,
  "confidence": 0.87,
  "image_url": "https://your-storage.com/uploads/pothole_1708456800000.jpg",
  "status": "reported",
  "severity": null,
  "detected_at": "2025-02-20T18:00:00Z"
}
```

**Duplicate Detection Response (200 OK):**

When the backend finds an existing pothole within proximity of the reported coordinates:

```json
{
  "id": "new-report-uuid",
  "isDuplicate": true,
  "existingId": "original-pothole-uuid",
  "confirmationCount": 3,
  "latitude": 31.954,
  "longitude": 35.9107,
  "confidence": 0.92,
  "image_url": "https://your-storage.com/uploads/pothole_1708456800000.jpg",
  "status": "confirmed",
  "severity": "high",
  "detected_at": "2025-02-20T18:05:00Z"
}
```

**Error Responses:**

| Status | Meaning                                  |
| ------ | ---------------------------------------- |
| 400    | Missing required fields                  |
| 401    | Missing or invalid Bearer token          |
| 403    | Token valid but user lacks permission    |
| 404    | Endpoint not found                       |
| 422    | Validation error (bad coordinates, etc.) |
| 500    | Server error (app will retry these)      |

---

## Field Reference

### Login Request

| Field    | Type   | Required | Notes              |
| -------- | ------ | -------- | ------------------ |
| email    | string | yes      | Valid email format |
| password | string | yes      | User password      |

### Upload Request (Multipart)

| Field      | Type   | Required | Notes                                  |
| ---------- | ------ | -------- | -------------------------------------- |
| image      | file   | yes      | JPEG image, content-type `image/jpeg`  |
| latitude   | string | yes      | Decimal degrees (e.g. `"31.9539"`)     |
| longitude  | string | yes      | Decimal degrees (e.g. `"35.9106"`)     |
| confidence | string | yes      | Float 0.0-1.0, AI detection confidence |
| vehicleId  | string | yes      | Identifier for the reporting vehicle   |
| timestamp  | string | yes      | Unix epoch in milliseconds             |

### Upload Response

| Field             | Type    | Nullable | Notes                                         |
| ----------------- | ------- | -------- | --------------------------------------------- |
| id                | string  | no       | Unique ID for this report                     |
| isDuplicate       | boolean | no       | `true` if near an existing report             |
| existingId        | string  | yes      | ID of the original pothole (if duplicate)     |
| confirmationCount | int     | yes      | How many times this pothole has been reported |
| latitude          | double  | yes      | Stored latitude                               |
| longitude         | double  | yes      | Stored longitude                              |
| confidence        | double  | yes      | Stored confidence value                       |
| image_url         | string  | yes      | Public URL of the stored image                |
| status            | string  | yes      | `reported`, `confirmed`, `repaired`, etc.     |
| severity          | string  | yes      | `low`, `medium`, `high`, etc.                 |
| detected_at       | string  | yes      | ISO 8601 datetime                             |

### Login Response

| Field                | Type   | Notes                        |
| -------------------- | ------ | ---------------------------- |
| user.id              | string | User UUID                    |
| user.email           | string | User email                   |
| user.name            | string | Display name                 |
| user.role            | string | `user`, `admin`, etc.        |
| tokens.access_token  | string | JWT for Bearer auth          |
| tokens.refresh_token | string | JWT for obtaining new tokens |

---

## Retry Behavior (How the Android App Handles Errors)

The app uses WorkManager with these rules. Your backend should return appropriate status codes:

| Backend Returns | App Does                                            |
| --------------- | --------------------------------------------------- |
| 2xx             | Success - removes from local queue                  |
| 400             | Permanent failure - no retry                        |
| 401             | Permanent failure - no retry                        |
| 403             | Permanent failure - no retry                        |
| 404             | Permanent failure - no retry                        |
| 422             | Permanent failure - no retry                        |
| 5xx             | Retries with exponential backoff (30s base, max 5x) |
| Network error   | Retries with exponential backoff (30s base, max 5x) |

---

## Backend Implementation Notes

1. **JSON keys use snake_case** for `access_token`, `refresh_token`, `image_url`, `detected_at`. The Android app maps them via `@SerialName`.

2. **Token refresh endpoint** - The app stores `refresh_token` but doesn't call a refresh endpoint yet. Plan for `POST /api/auth/refresh` accepting `{ "refresh_token": "..." }` and returning new `AuthTokens`.

3. **Geospatial deduplication** - Use PostGIS `ST_DWithin` to check if a new report is within ~20-50 meters of an existing one. If so, set `isDuplicate: true` and increment `confirmationCount`.

4. **Image storage** - Store uploaded images and return a publicly accessible `image_url` in the response.

5. **Request timeout** - The app has a 30s request timeout and 10s connect timeout. Keep endpoint response times well under that.
