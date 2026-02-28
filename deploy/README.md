# Production Deployment Guide

This guide explains how to deploy the Pothole Detection System to a production VPS.

## Prerequisites

- VPS with Ubuntu 22.04 (DigitalOcean, AWS, Hetzner, etc.)
- Domain name (e.g., potholesystem.tech)
- Docker and Docker Compose installed
- Nginx installed
- SSL certificates (Let's Encrypt)

## Quick Deployment

1. **Clone repository on VPS:**
   ```bash
   git clone https://github.com/YOUR_USERNAME/YOUR_REPO.git /opt/pothole
   cd /opt/pothole
   ```

2. **Create .env file:**
   ```bash
   cp .env.example .env
   # Edit .env and fill in all required values
   ```

3. **Generate secure secrets:**
   ```bash
   export JWT_SECRET=$(openssl rand -base64 32)
   export JWT_REFRESH_SECRET=$(openssl rand -base64 32)
   export DB_PASSWORD=$(openssl rand -base64 16)
   # Add these to .env file
   ```

4. **Run deployment:**
   ```bash
   ./deploy/deploy.sh
   ```

## Manual Steps

### 1. Install Dependencies

```bash
apt update && apt upgrade -y
apt install -y docker.io docker-compose-plugin nginx certbot python3-certbot-nginx ufw git
```

### 2. Configure Firewall

```bash
ufw default deny incoming
ufw default allow outgoing
ufw allow ssh
ufw allow 'Nginx Full'
ufw --force enable
```

### 3. Setup NGINX

```bash
# Copy config
cp deploy/nginx.conf /etc/nginx/sites-available/potholesystem
ln -sf /etc/nginx/sites-available/potholesystem /etc/nginx/sites-enabled/
rm -f /etc/nginx/sites-enabled/default
nginx -t
systemctl reload nginx
```

### 4. Get SSL Certificates

```bash
certbot --nginx -d api.potholesystem.tech -d potholesystem.tech
```

### 5. Configure Cloudflare R2 (Optional)

1. Create account at https://dash.cloudflare.com
2. Go to R2 → Create bucket
3. Generate API token with Object Read & Write permissions
4. Add credentials to .env file

## Environment Variables

Required in `.env`:

| Variable | Description | Example |
|----------|-------------|---------|
| `DB_PASSWORD` | PostgreSQL password | Generated random string |
| `JWT_SECRET` | JWT signing secret | Minimum 32 characters |
| `JWT_REFRESH_SECRET` | JWT refresh token secret | Minimum 32 characters |
| `FRONTEND_ORIGIN` | Dashboard domain | https://potholesystem.tech |
| `Storage__PublicBaseUrl` | API domain | https://api.potholesystem.tech |
| `S3__AccessKeyId` | Cloudflare R2 access key | Optional |
| `S3__SecretAccessKey` | Cloudflare R2 secret | Optional |
| `S3__BucketName` | Bucket name | smartpothole-images |
| `S3__Region` | Region | auto |

## Troubleshooting

### 502 Bad Gateway
- Check if containers are running: `docker-compose -f docker-compose.prod.yml ps`
- Check backend logs: `docker-compose -f docker-compose.prod.yml logs backend`
- Verify port mapping is correct (3000:8080)

### SSL Certificate Issues
- Test renewal: `certbot renew --dry-run`
- Check certificate: `certbot certificates`

### Database Connection Failed
- Verify DB_PASSWORD matches in .env
- Check PostgreSQL logs: `docker-compose -f docker-compose.prod.yml logs postgres`

## Useful Commands

```bash
# View logs
docker-compose -f docker-compose.prod.yml logs -f

# Restart specific service
docker-compose -f docker-compose.prod.yml restart backend

# Complete rebuild
docker-compose -f docker-compose.prod.yml down -v
docker-compose -f docker-compose.prod.yml up -d --build

# Database backup
docker exec pothole_postgres_1 pg_dump -U postgres pothole_detection > backup.sql
```

## Architecture

```
Internet
    │
    ▼
Cloudflare (DNS)
    │
    ▼
NGINX (SSL/Reverse Proxy)
    │
    ├── api.potholesystem.tech → Backend (Port 3000)
    └── potholesystem.tech → Dashboard (Port 4321)
    │
    ▼
Docker Compose
    ├── Backend (.NET 8)
    ├── Dashboard (React/Vite)
    └── PostgreSQL + PostGIS
    │
    ▼
Cloudflare R2 (Image Storage)
```

## Support

For issues or questions, refer to:
- Main README.md
- docs/05-deployment.md (detailed VPS setup guide)
- GitHub Issues
