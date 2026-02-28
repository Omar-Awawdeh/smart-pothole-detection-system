# Production Deployment Documentation

**Project**: Smart Pothole Detection System  
**Deployment URL**: https://potholesystem.tech  
**API Endpoint**: https://api.potholesystem.tech  
**Status**: ✅ Production Ready  

---

## Executive Summary

This document describes the production deployment architecture, configuration, and operational details for the Smart Pothole Detection System. The system has been successfully deployed to a DigitalOcean VPS with full HTTPS support, automated SSL certificate management, and cloud-based image storage.

---

## Deployment Architecture

### System Overview

The production deployment consists of a multi-tier architecture deployed on a single VPS:

```
Internet Users
       │
       ▼
┌─────────────────────────────────────┐
│  DNS: potholesystem.tech           │
│  DNS: api.potholesystem.tech       │
└──────────┬──────────────────────────┘
           │
           ▼
┌─────────────────────────────────────┐
│  NGINX Reverse Proxy (80/443)      │
│  - SSL Termination (Let's Encrypt) │
│  - Load Balancing                  │
│  - Static File Serving             │
└──────┬──────────────────────┬──────┘
       │                      │
       ▼                      ▼
┌─────────────┐        ┌─────────────┐
│  Backend    │        │  Dashboard  │
│  API        │        │  (React)    │
│  :3000      │        │  :4321      │
└──────┬──────┘        └─────────────┘
       │
       ▼
┌─────────────┐
│ PostgreSQL  │
│ + PostGIS   │
│ :5432       │
└─────────────┘
       │
       ▼
┌─────────────────────────────────────┐
│  Cloudflare R2 (Image Storage)     │
│  - Free Tier (10GB)                 │
│  - S3-Compatible API               │
└─────────────────────────────────────┘
```

### Component Details

#### 1. VPS Infrastructure
- **Provider**: DigitalOcean
- **Plan**: $12/month (2GB RAM, 2 vCPU, 50GB SSD)
- **OS**: Ubuntu 22.04 LTS
- **Location**: Frankfurt, Germany
- **Domain**: potholesystem.tech (via tech.com GitHub Student Pack)

#### 2. Application Stack

| Component | Technology | Port | Purpose |
|-----------|-----------|------|---------|
| **Backend API** | .NET 8 | 3000 | REST API, JWT Auth, Image Processing |
| **Dashboard** | React + Vite | 4321 | Admin panel, analytics, maps |
| **Database** | PostgreSQL 16 + PostGIS | 5432 | Data persistence, spatial queries |
| **Proxy** | NGINX | 80/443 | SSL termination, routing |
| **Storage** | Cloudflare R2 | - | Image storage (S3-compatible) |

#### 3. Docker Configuration

All services run in Docker containers orchestrated by Docker Compose:

```yaml
# Production Configuration (docker-compose.prod.yml)

Services:
  - postgres: PostgreSQL with PostGIS extension
  - backend: .NET 8 API (exposed on port 3000)
  - dashboard: React/Vite frontend (exposed on port 4321)

Networking:
  - Internal Docker network for service communication
  - Ports mapped to host for NGINX proxy

Volumes:
  - pgdata_prod: Persistent database storage
```

---

## Deployment Process

### Phase 1: VPS Provisioning

1. **Droplet Creation** (DigitalOcean)
   - Ubuntu 22.04 LTS image
   - 2GB RAM / 2 vCPU / 50GB SSD
   - SSH key authentication
   - Firewall configured (UFW)

2. **System Setup**
   ```bash
   apt update && apt upgrade -y
   apt install -y docker.io docker-compose-plugin nginx certbot
   ufw allow 'Nginx Full'
   ufw allow ssh
   ufw enable
   ```

### Phase 2: Domain Configuration

1. **Domain Registration** (tech.com via GitHub Student Pack)
   - Domain: potholesystem.tech
   - DNS A Records:
     - `@` → VPS IP
     - `api` → VPS IP

2. **DNS Propagation** (5-30 minutes)
   - Verified with `nslookup potholesystem.tech`

### Phase 3: Application Deployment

1. **Repository Setup**
   ```bash
   git clone https://github.com/Omar-Awawdeh/smart-pothole-detection-system.git /opt/pothole
   cd /opt/pothole
   ```

2. **Environment Configuration** (.env)
   ```bash
   # Generated secure secrets
   DB_PASSWORD=<generated_random_password>
   JWT_SECRET=<generated_32_char_secret>
   JWT_REFRESH_SECRET=<generated_32_char_secret>
   
   # Domain configuration
   FRONTEND_ORIGIN=https://potholesystem.tech
   Storage__PublicBaseUrl=https://api.potholesystem.tech
   
   # S3/R2 Configuration
   S3__AccessKeyId=<cloudflare_r2_key>
   S3__SecretAccessKey=<cloudflare_r2_secret>
   S3__BucketName=smartpothole-images
   S3__Region=auto
   ```

3. **Docker Deployment**
   ```bash
   docker-compose -f docker-compose.prod.yml up -d --build
   ```

### Phase 4: Reverse Proxy & SSL

1. **NGINX Configuration**
   - Backend proxy: api.potholesystem.tech → localhost:3000
   - Dashboard proxy: potholesystem.tech → localhost:4321
   - Client max body size: 50MB (for image uploads)
   - Proxy timeouts: 300s

2. **SSL Certificates** (Let's Encrypt)
   ```bash
   certbot --nginx -d api.potholesystem.tech -d potholesystem.tech
   ```
   - Auto-renewal configured via cron

### Phase 5: Cloud Storage Setup

1. **Cloudflare R2 Bucket**
   - Bucket name: smartpothole-images
   - Region: Europe
   - Access: S3-compatible API

2. **Backend Integration**
   - Configured via environment variables
   - Automatic fallback to local storage if not configured

---

## Security Measures

### 1. Authentication & Authorization
- **JWT Tokens**: Short-lived access tokens (15 min), refresh tokens (7 days)
- **Password Hashing**: BCrypt with salt
- **CORS**: Restricted to production domain

### 2. Network Security
- **Firewall**: UFW with minimal open ports (22, 80, 443)
- **Docker Network**: Internal networking between containers
- **No Direct DB Access**: PostgreSQL only accessible within Docker network

### 3. Data Protection
- **SSL/TLS**: All traffic encrypted via HTTPS
- **Secrets Management**: Environment variables, never committed to git
- **Image Storage**: Cloud-based with access control

### 4. Secrets (Redacted in Documentation)
- Database password: `[REDACTED]`
- JWT secrets: `[REDACTED]`
- S3 credentials: `[REDACTED]`

---

## Operational Details

### Health Monitoring

**Backend Health Check:**
```bash
Endpoint: GET https://api.potholesystem.tech/health
Response: "Healthy"
Status: ✅ Operational
```

**Dashboard Access:**
```bash
URL: https://potholesystem.tech
Status: ✅ Accessible
Note: Vite allowedHosts configured for production domain
```

### Performance Specifications

| Metric | Value |
|--------|-------|
| **VPS Uptime** | 99.9% (DigitalOcean SLA) |
| **SSL Certificate** | Let's Encrypt (90-day auto-renewal) |
| **Database Storage** | 50GB SSD |
| **Image Storage** | Cloudflare R2 (10GB free tier) |
| **Monthly Transfer** | 1TB (DigitalOcean limit) |

### Resource Usage

Current production usage:
- **CPU**: < 10% average
- **RAM**: ~800MB / 2GB
- **Disk**: ~15GB / 50GB
- **Network**: Minimal (graduation project demo)

---

## Deployment Files

All deployment configurations are version-controlled:

| File | Purpose | Location |
|------|---------|----------|
| `docker-compose.prod.yml` | Production Docker stack | Root directory |
| `deploy/nginx.conf` | NGINX reverse proxy | `deploy/` directory |
| `deploy/deploy.sh` | One-command deployment script | `deploy/` directory |
| `deploy/README.md` | Deployment guide | `deploy/` directory |
| `.env.example` | Environment template | Root directory |
| `.gitignore` | Excludes secrets | Root directory |

---

## Troubleshooting Guide

### Common Issues & Solutions

#### 1. 502 Bad Gateway
**Cause**: Backend container not accessible
**Solution**:
```bash
docker-compose -f docker-compose.prod.yml ps
docker-compose -f docker-compose.prod.yml logs backend
# Verify port mapping: 3000:8080
```

#### 2. Database Connection Failed
**Cause**: Password mismatch or PostgreSQL not ready
**Solution**:
```bash
# Restart with fresh volume if needed
docker-compose -f docker-compose.prod.yml down -v
docker-compose -f docker-compose.prod.yml up -d
```

#### 3. SSL Certificate Expired
**Cause**: Auto-renewal failed
**Solution**:
```bash
certbot renew --force-renewal
systemctl reload nginx
```

#### 4. Image Upload Fails
**Cause**: S3 credentials not configured or CORS issue
**Solution**: Verify `.env` has correct S3__AccessKeyId and S3__SecretAccessKey

---

## Maintenance Procedures

### Regular Tasks

**Weekly:**
- Check disk usage: `df -h`
- Review logs: `docker-compose -f docker-compose.prod.yml logs --tail=100`
- Verify SSL certificate expiry: `certbot certificates`

**Monthly:**
- Update system packages: `apt update && apt upgrade`
- Review and rotate secrets if necessary
- Database backup: `docker exec pothole_postgres_1 pg_dump -U postgres pothole_detection > backup_$(date +%Y%m%d).sql`

### Backup Strategy

1. **Database**: Automated weekly dumps
2. **Images**: Stored in Cloudflare R2 (redundant)
3. **Code**: Git version control
4. **Configuration**: Version-controlled deployment files

---

## Cost Analysis

### Monthly Operating Costs

| Service | Provider | Cost |
|---------|----------|------|
| **VPS** | DigitalOcean | $12.00 |
| **Domain** | tech.com (GitHub Pack) | $0.00 (free via student pack) |
| **DNS** | tech.com | $0.00 (included) |
| **SSL** | Let's Encrypt | $0.00 |
| **Image Storage** | Cloudflare R2 | ~$0.10 (10GB free tier) |
| **Total** | | **~$12.10/month** |

### Credits Used
- **DigitalOcean**: $200 credit (60 days) - covers 16 months at current usage
- **GitHub Student Pack**: Free domain for 1 year

---

## Conclusion

The Smart Pothole Detection System has been successfully deployed to production with:
- ✅ Full HTTPS support
- ✅ Automated SSL certificate management
- ✅ Docker containerization
- ✅ Cloud-based image storage
- ✅ Domain configuration
- ✅ Security best practices

The deployment is stable, cost-effective, and ready for demonstration and evaluation.

---

**Document Version**: 1.0  
**Last Updated**: February 2026  
**Deployed By**: Omar Awawdeh  
**Status**: Production Active
