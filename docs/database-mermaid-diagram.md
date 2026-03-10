# Database Mermaid Diagram

This Mermaid ER diagram reflects the current backend models and relationships in the repository.

- Source models: `backend/src/PotholeDetection.Api/Models/User.cs`, `backend/src/PotholeDetection.Api/Models/Vehicle.cs`, `backend/src/PotholeDetection.Api/Models/Pothole.cs`
- Relationship note: `vehicles` is currently not linked to `users`; `potholes` keeps a required `vehicle_id` foreign key.

```mermaid
erDiagram
    %% Current implementation: vehicles is not linked to users.
    %% Enums used in code: status = unverified|verified|repaired|false_positive
    %% severity = low|medium|high

    USERS {
        uuid id PK
        string email UK
        string password_hash
        string name
        string role
        datetime created_at
        datetime updated_at
    }

    VEHICLES {
        uuid id PK
        string name
        string serial_number UK
        boolean is_active
        datetime last_active_at
        datetime created_at
    }

    POTHOLES {
        uuid id PK
        uuid vehicle_id FK
        double latitude
        double longitude
        geography location
        double confidence
        string image_url
        string status
        string severity
        int confirmation_count
        datetime detected_at
        datetime repaired_at
        datetime created_at
        datetime updated_at
    }

    VEHICLES ||--o{ POTHOLES : reports
```

## Notes

- `USERS` exists for authentication and authorization.
- `VEHICLES` stores reporting devices/units and has a unique `serial_number`.
- `POTHOLES.location` is a PostGIS `geography(Point, 4326)` column for spatial queries.
- Indexes are defined on `potholes.status`, `potholes.detected_at`, and `potholes.vehicle_id` in `backend/src/PotholeDetection.Api/Data/AppDbContext.cs`.
