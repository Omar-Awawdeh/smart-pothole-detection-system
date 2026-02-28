# Deployment Setup and Structure

This is the single deployment document for the project discussion.

## Overview

The system is deployed on one Linux VPS using Docker Compose and NGINX.

- `https://potholesystem.tech` serves the dashboard.
- `https://www.potholesystem.tech` redirects to `https://potholesystem.tech`.
- `https://api.potholesystem.tech` serves the backend API.
- `https://images.potholesystem.tech` serves uploaded images from Cloudflare R2.
- PostgreSQL + PostGIS runs in Docker for persistent data.
- Image files are stored in S3-compatible object storage (Cloudflare R2 recommended).

## Architecture

```
Internet
   |
   v
NGINX (Host: 80/443, TLS via Let's Encrypt)
   |
   +--> potholesystem.tech
   |      - /        -> dashboard container
   |      - /api/*   -> backend container
   |      - /hubs/*  -> backend container (SignalR)
   +--> www.potholesystem.tech
   |      - 301 redirect -> potholesystem.tech
   |
   +--> api.potholesystem.tech
          - all paths -> backend container

Docker Compose Stack
   - dashboard (React build served by nginx container)
   - backend (.NET 8 API)
   - postgres (PostgreSQL 16 + PostGIS 3.4)

External Service
   - Cloudflare R2 bucket exposed at images.potholesystem.tech
```

## Runtime Components

### Host Layer

- Ubuntu VPS
- Docker Engine + Docker Compose
- NGINX reverse proxy
- Certbot (Let's Encrypt)

### Container Layer

- `postgres`: `postgis/postgis:16-3.4`
- `backend`: ASP.NET Core 8 (`3000:8080`)
- `dashboard`: static React build served via NGINX (`4321:80`)

### Persistent Volumes

- `pgdata_prod`: PostgreSQL data
- `backend-uploads`: backend local upload fallback

## Configuration Structure

### Main Files

- `docker-compose.prod.yml`: production service wiring
- `deploy/nginx.conf`: host reverse-proxy rules
- `deploy/deploy.sh`: deployment automation script
- `.github/workflows/deploy.yml`: automatic server deployment on push to `main`
- `.env`: production runtime secrets and environment values (server only)
- `.env.example`: template for required variables

### Required Environment Values

- `DB_PASSWORD`
- `JWT_SECRET`
- `JWT_REFRESH_SECRET`
- `FRONTEND_ORIGIN=https://potholesystem.tech`
- `Storage__PublicBaseUrl=https://api.potholesystem.tech`

### Optional Object Storage Values

- `S3__AccessKeyId`
- `S3__SecretAccessKey`
- `S3__BucketName=potholes`
- `S3__Region`
- `S3__Endpoint=https://<ACCOUNT_ID>.r2.cloudflarestorage.com`
- `S3__PublicBaseUrl=https://images.potholesystem.tech`
- `S3__ForcePathStyle`

## DNS Records

Current DNS records used by production:

- `A @` -> server public IP
- `A api` -> server public IP
- `A www` -> server public IP
- `CNAME images` -> Cloudflare R2 custom-domain target

## CI/CD (GitHub Actions)

Deploys are automated from GitHub Actions using SSH to the VPS.

- Trigger: push to `main`
- Workflow file: `.github/workflows/deploy.yml`
- Action steps:
  1. SSH to server
  2. Pull latest code in `/opt/pothole`
  3. Rebuild and restart backend/dashboard containers
  4. Reload NGINX config

Required repository secrets:

- `SERVER_HOST`
- `SERVER_USER`
- `SERVER_SSH_KEY`

Server user requirements:

- Can run Docker commands on `/opt/pothole`
- Can update NGINX config and reload NGINX (directly or via passwordless `sudo`)

## Deployment Flow (High-Level)

1. Provision VPS and install Docker, NGINX, and Certbot.
2. Point DNS records (`@` and `api`) to VPS IP.
3. Apply NGINX config and issue SSL certificates.
4. Configure `.env` with secrets and domain values.
5. Start stack with `docker-compose.prod.yml`.
6. Verify dashboard and API health.
7. Confirm GitHub Actions deploy workflow is green after push.

## Validation Checklist

- `https://api.potholesystem.tech/health` returns healthy.
- `https://potholesystem.tech` loads the dashboard.
- `https://www.potholesystem.tech` redirects to root domain.
- Dashboard authentication works.
- API requests from dashboard succeed.
- Pothole records are written to PostgreSQL.
- Uploaded images are served from `https://images.potholesystem.tech/...`.

## Notes

- Production dashboard runs as static files, not Vite dev server.
- Keep all real secrets in server `.env` only.
- If S3 values are omitted, backend stores images locally in fallback volume.
