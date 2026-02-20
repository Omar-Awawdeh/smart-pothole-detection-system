# Current Implementation Notes (Feb 2026)

This file documents the current running implementation in this repository.
Some older docs in this folder are planning documents and may describe earlier design choices.

## Runtime Stack

- Backend: ASP.NET Core 8 Web API + Entity Framework Core + PostgreSQL/PostGIS
- Dashboard: React + Vite + Tailwind + React Leaflet
- Docker orchestration: `docker-compose.yml`

## Local Ports

Docker defaults (configurable in compose):

- Backend host port: `3000` (container `8080`)
- Dashboard host port: `4321` (container `5173`)
- Postgres host port: `5432`

Compose variables:

- `BACKEND_PORT` (default `3000`)
- `FRONTEND_PORT` (default `4321`)
- `DB_PORT` (default `5432`)

## Data Model Notes

- `vehicles` is no longer linked to `users` (no `user_id` column)
- `potholes` keeps `vehicle_id` relation
- Vehicle deletion is a hard delete in the service layer
- Vehicle list endpoint returns active vehicles

## Map and UI Notes

- Map view loads all pothole points (not only first page)
- Map auto-fits bounds to all loaded points
- Clicking a point focuses the map on that point
- Pothole images use non-cropping display (`object-contain`)
- Dashboard layout includes mobile overflow fixes for map pages

## Seed Test Accounts

- Admin: `admin@pothole.dev` / `admin123`
- Operator: `operator@pothole.dev` / `operator123`
- Viewer: `viewer@pothole.dev` / `viewer123`
