# Deployment Guide - Hetzner VPS Setup

This document covers the complete deployment process for the Smart Pothole Detection System on a Hetzner VPS, including server provisioning, Docker configuration, database setup, Nginx reverse proxy, SSL certificates, and domain configuration.

---

## Table of Contents

1. [Infrastructure Overview](#infrastructure-overview)
2. [Hetzner VPS Setup](#hetzner-vps-setup)
3. [Initial Server Configuration](#initial-server-configuration)
4. [Docker Installation](#docker-installation)
5. [PostgreSQL + PostGIS Setup](#postgresql--postgis-setup)
6. [AWS S3 Configuration](#aws-s3-configuration)
7. [Application Dockerfiles](#application-dockerfiles)
8. [Docker Compose Configuration](#docker-compose-configuration)
9. [Nginx Reverse Proxy](#nginx-reverse-proxy)
10. [SSL with Let's Encrypt](#ssl-with-lets-encrypt)
11. [Domain & DNS (Cloudflare)](#domain--dns-cloudflare)
12. [Deployment Scripts](#deployment-scripts)
13. [Monitoring & Maintenance](#monitoring--maintenance)
14. [Backup Strategy](#backup-strategy)
15. [Troubleshooting](#troubleshooting)

---

## Infrastructure Overview

### Architecture Diagram

```
                                    ┌─────────────────────────────────────┐
                                    │           CLOUDFLARE                │
                                    │  (DNS + Proxy + DDoS Protection)    │
                                    │                                     │
                                    │  pothole-api.yoursite.com    ──────┼───┐
                                    │  pothole-dash.yoursite.com   ──────┼───┤
                                    └─────────────────────────────────────┘   │
                                                                              │
                                                                              │ HTTPS (443)
                                                                              │
┌─────────────────────────────────────────────────────────────────────────────▼───────────┐
│                            HETZNER VPS (CX31 - €8.39/month)                             │
│                            Ubuntu 22.04 LTS | 4 vCPU | 8GB RAM                          │
│                                                                                          │
│  ┌────────────────────────────────────────────────────────────────────────────────────┐ │
│  │                              NGINX (Host - Port 80/443)                             │ │
│  │                                                                                     │ │
│  │   SSL Termination (Let's Encrypt)                                                  │ │
│  │   ├── api.yoursite.com      →  proxy_pass http://localhost:3000                   │ │
│  │   └── dashboard.yoursite.com →  proxy_pass http://localhost:4321                   │ │
│  │       (also serves static files from /var/www/dashboard)                           │ │
│  └────────────────────────────────────────────────────────────────────────────────────┘ │
│                                           │                                              │
│                         ┌─────────────────┴─────────────────┐                           │
│                         │                                   │                           │
│                         ▼                                   ▼                           │
│  ┌──────────────────────────────────────┐ ┌──────────────────────────────────────────┐ │
│  │     DOCKER: backend-api              │ │     DOCKER: dashboard                     │ │
│  │     Port: 3000                       │ │     Port: 4321 (or static files)          │ │
│  │                                      │ │                                           │ │
│  │  ┌────────────────────────────────┐  │ │  ┌─────────────────────────────────────┐  │ │
│  │  │     Fastify + TypeScript       │  │ │  │        Astro + React                │  │ │
│  │  │     Node.js 20 LTS             │  │ │  │     (Static Build + Islands)        │  │ │
│  │  └──────────────┬─────────────────┘  │ │  └─────────────────────────────────────┘  │ │
│  │                 │                    │ │                                           │ │
│  └─────────────────┼────────────────────┘ └───────────────────────────────────────────┘ │
│                    │                                                                     │
│                    │ localhost:5432                                                      │
│                    ▼                                                                     │
│  ┌──────────────────────────────────────────────────────────────────────────────────┐   │
│  │              DOCKER: postgres-db                                                  │   │
│  │              PostgreSQL 16 + PostGIS 3.4                                          │   │
│  │              Port: 5432                                                           │   │
│  │                                                                                   │   │
│  │   Volume: /var/lib/docker/volumes/pothole_postgres_data                          │   │
│  └──────────────────────────────────────────────────────────────────────────────────┘   │
│                                                                                          │
└──────────────────────────────────────────────────────────────────────────────────────────┘
                                             │
                                             │ HTTPS (AWS SDK)
                                             ▼
                         ┌──────────────────────────────────────┐
                         │             AWS S3                    │
                         │    Bucket: pothole-detection-images   │
                         │    Region: eu-central-1 (Frankfurt)   │
                         └──────────────────────────────────────┘
```

### Cost Breakdown

| Service | Specification | Monthly Cost |
|---------|--------------|--------------|
| Hetzner CX31 | 4 vCPU, 8GB RAM, 80GB SSD | €8.39 (~$9) |
| Hetzner CX31 IPv4 | Public IPv4 address | €0.50 (~$0.55) |
| Domain | .com or .net (yearly/12) | ~$1 |
| AWS S3 | ~10GB storage estimate | ~$0.25 |
| AWS S3 Data Transfer | ~5GB/month | ~$0.50 |
| Cloudflare | Free plan | $0 |
| Let's Encrypt | Free SSL | $0 |
| **Total** | | **~$11-12/month** |

### Why CX31 (Not CX21)?

- PostgreSQL + PostGIS benefits from extra RAM for spatial queries
- Running 3 Docker containers (API, Dashboard, PostgreSQL)
- Leaves headroom for Nginx, certbot, system processes
- €4/month extra is worth the stability

---

## Hetzner VPS Setup

### Step 1: Create Hetzner Account

```
PROCESS: Hetzner Account Setup
│
├── 1. Go to https://www.hetzner.com
├── 2. Click "Cloud" → "Get Started"
├── 3. Create account with email
├── 4. Verify email address
├── 5. Add payment method (credit card or PayPal)
└── 6. Accept terms of service
```

### Step 2: Create Cloud Server

```
PROCESS: Server Creation via Hetzner Cloud Console
│
├── 1. Login to https://console.hetzner.cloud
│
├── 2. Create New Project
│   ├── Name: "pothole-detection"
│   └── Click "Create"
│
├── 3. Add Server
│   ├── Location: Choose closest to Jordan
│   │   ├── Recommended: "Falkenstein" (Germany) - good latency to Middle East
│   │   └── Alternative: "Helsinki" if Falkenstein is congested
│   │
│   ├── Image: Ubuntu 22.04 LTS
│   │
│   ├── Type: CX31 (Shared vCPU)
│   │   ├── 4 vCPU
│   │   ├── 8 GB RAM
│   │   └── 80 GB SSD
│   │
│   ├── Networking:
│   │   ├── Public IPv4: ✓ Enable (required)
│   │   └── Public IPv6: ✓ Enable (free, good to have)
│   │
│   ├── SSH Key: (IMPORTANT - Add before creating server)
│   │   │
│   │   ├── Generate SSH key on your local machine:
│   │   │   └── RUN: ssh-keygen -t ed25519 -C "hamza@pothole-project"
│   │   │       ├── Save to: ~/.ssh/pothole_hetzner
│   │   │       └── Passphrase: (optional but recommended)
│   │   │
│   │   ├── Copy public key:
│   │   │   └── RUN: cat ~/.ssh/pothole_hetzner.pub
│   │   │
│   │   └── Paste into Hetzner "SSH Keys" section
│   │
│   ├── Volumes: None (using S3 for images)
│   │
│   ├── Firewalls: Create new firewall
│   │   ├── Name: "pothole-firewall"
│   │   └── Rules: (configure after creation)
│   │
│   ├── Backups: Disable (we'll use pg_dump for database)
│   │
│   ├── Name: "pothole-server"
│   │
│   └── Click "Create & Buy Now"
│
└── 4. Note the Public IP Address
    └── Example: 168.119.xxx.xxx
```

### Step 3: Configure Firewall

```
PROCESS: Hetzner Firewall Configuration
│
├── Go to: Project → Firewalls → pothole-firewall
│
├── Inbound Rules:
│   │
│   ├── Rule 1: SSH
│   │   ├── Protocol: TCP
│   │   ├── Port: 22
│   │   └── Source: Any (or your IP for extra security)
│   │
│   ├── Rule 2: HTTP
│   │   ├── Protocol: TCP
│   │   ├── Port: 80
│   │   └── Source: Any
│   │
│   ├── Rule 3: HTTPS
│   │   ├── Protocol: TCP
│   │   ├── Port: 443
│   │   └── Source: Any
│   │
│   └── Rule 4: ICMP (for ping diagnostics)
│       ├── Protocol: ICMP
│       └── Source: Any
│
├── Outbound Rules:
│   └── Allow all (default)
│
└── Apply to server: "pothole-server"
```

### Step 4: First SSH Connection

```
PSEUDOCODE: First Connection

# On your local machine
SSH_KEY_PATH = "~/.ssh/pothole_hetzner"
SERVER_IP = "168.119.xxx.xxx"  # Replace with your IP

# Set correct permissions on key
chmod 600 $SSH_KEY_PATH

# Connect to server
ssh -i $SSH_KEY_PATH root@$SERVER_IP

# Verify connection
IF connected successfully:
    PRINT "Server access confirmed"
    RUN: hostname
    RUN: uname -a
    RUN: df -h
ELSE:
    CHECK: Firewall rules
    CHECK: SSH key was added correctly
    CHECK: IP address is correct
```

---

## Initial Server Configuration

### Step 1: System Update

```
PSEUDOCODE: Update Ubuntu System

# Update package lists
apt update

# Upgrade all packages
apt upgrade -y

# Install essential tools
apt install -y \
    curl \
    wget \
    git \
    vim \
    htop \
    unzip \
    software-properties-common \
    apt-transport-https \
    ca-certificates \
    gnupg \
    lsb-release \
    ufw \
    fail2ban

# Set timezone (choose your timezone)
timedatectl set-timezone Asia/Amman

# Verify timezone
date
```

### Step 2: Create Non-Root User

```
PSEUDOCODE: Create Deploy User

USERNAME = "deploy"

# Create user with home directory
adduser $USERNAME
# Enter password when prompted (save this password!)

# Add to sudo group
usermod -aG sudo $USERNAME

# Copy SSH key from root to deploy user
mkdir -p /home/$USERNAME/.ssh
cp /root/.ssh/authorized_keys /home/$USERNAME/.ssh/
chown -R $USERNAME:$USERNAME /home/$USERNAME/.ssh
chmod 700 /home/$USERNAME/.ssh
chmod 600 /home/$USERNAME/.ssh/authorized_keys

# Test login in NEW terminal window (keep root session open!)
# On your local machine:
ssh -i ~/.ssh/pothole_hetzner deploy@168.119.xxx.xxx

# If successful, continue. If not, fix before proceeding.
```

### Step 3: Secure SSH

```
PSEUDOCODE: SSH Hardening

# Edit SSH config
vim /etc/ssh/sshd_config

# Find and change these lines:
# PermitRootLogin no          # Disable root login
# PasswordAuthentication no    # Disable password auth
# PubkeyAuthentication yes     # Enable key auth
# MaxAuthTries 3               # Limit login attempts
# ClientAliveInterval 300      # Timeout after 5 min inactive
# ClientAliveCountMax 2        # 2 keepalive checks

# Test config before restart
sshd -t

IF config valid:
    # Restart SSH service
    systemctl restart sshd
    
    # IMPORTANT: Test login in NEW terminal before closing current session!
    # ssh -i ~/.ssh/pothole_hetzner deploy@168.119.xxx.xxx
ELSE:
    FIX config errors before continuing
```

### Step 4: Configure UFW Firewall (Defense in Depth)

```
PSEUDOCODE: UFW Configuration

# Note: Hetzner firewall is primary, UFW is secondary protection

# Reset UFW to defaults
ufw --force reset

# Set default policies
ufw default deny incoming
ufw default allow outgoing

# Allow SSH (CRITICAL - do this first!)
ufw allow 22/tcp

# Allow HTTP and HTTPS
ufw allow 80/tcp
ufw allow 443/tcp

# Enable UFW
ufw enable

# Verify status
ufw status verbose

# Expected output:
# Status: active
# To                         Action      From
# --                         ------      ----
# 22/tcp                     ALLOW       Anywhere
# 80/tcp                     ALLOW       Anywhere
# 443/tcp                    ALLOW       Anywhere
```

### Step 5: Configure Fail2Ban

```
PSEUDOCODE: Fail2Ban Setup

# Create local config (don't edit main config)
cp /etc/fail2ban/jail.conf /etc/fail2ban/jail.local

# Edit local config
vim /etc/fail2ban/jail.local

# Enable SSH jail (find [sshd] section):
# [sshd]
# enabled = true
# port = 22
# filter = sshd
# logpath = /var/log/auth.log
# maxretry = 3
# bantime = 3600
# findtime = 600

# Restart fail2ban
systemctl restart fail2ban

# Enable on boot
systemctl enable fail2ban

# Check status
fail2ban-client status
fail2ban-client status sshd
```

### Step 6: Configure Swap (Optional but Recommended)

```
PSEUDOCODE: Add Swap Space

# 8GB RAM might be tight with PostgreSQL + Docker
# Add 4GB swap as safety net

# Check current swap
free -h

# Create swap file
fallocate -l 4G /swapfile

# Set permissions
chmod 600 /swapfile

# Make it swap
mkswap /swapfile

# Enable swap
swapon /swapfile

# Verify
free -h

# Make permanent (add to fstab)
echo '/swapfile none swap sw 0 0' >> /etc/fstab

# Optimize swappiness for server (lower = prefer RAM)
echo 'vm.swappiness=10' >> /etc/sysctl.conf
sysctl -p
```

---

## Docker Installation

### Step 1: Install Docker Engine

```
PSEUDOCODE: Docker Installation (Ubuntu 22.04)

# Remove old versions (if any)
apt-get remove docker docker-engine docker.io containerd runc

# Add Docker's official GPG key
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | \
    gpg --dearmor -o /etc/apt/keyrings/docker.gpg
chmod a+r /etc/apt/keyrings/docker.gpg

# Add Docker repository
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
  https://download.docker.com/linux/ubuntu \
  $(lsb_release -cs) stable" | \
  tee /etc/apt/sources.list.d/docker.list > /dev/null

# Update package index
apt-get update

# Install Docker Engine, CLI, and plugins
apt-get install -y \
    docker-ce \
    docker-ce-cli \
    containerd.io \
    docker-buildx-plugin \
    docker-compose-plugin

# Verify installation
docker --version
docker compose version

# Expected output:
# Docker version 24.x.x
# Docker Compose version v2.x.x
```

### Step 2: Configure Docker for Non-Root User

```
PSEUDOCODE: Docker User Configuration

# Add deploy user to docker group
usermod -aG docker deploy

# Apply group changes (or logout/login)
newgrp docker

# Test as deploy user
su - deploy
docker run hello-world

# If successful, output shows "Hello from Docker!"
```

### Step 3: Configure Docker Daemon

```
PSEUDOCODE: Docker Daemon Configuration

# Create daemon config
vim /etc/docker/daemon.json

# Add these settings:
{
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "3"
  },
  "storage-driver": "overlay2",
  "live-restore": true
}

# Restart Docker
systemctl restart docker

# Enable Docker to start on boot
systemctl enable docker

# Verify daemon is running
systemctl status docker
```

---

## PostgreSQL + PostGIS Setup

### Option A: PostgreSQL in Docker (Recommended)

```
PSEUDOCODE: PostgreSQL Docker Setup

# Create directory for Docker volumes
mkdir -p /home/deploy/pothole/data/postgres

# This will be handled by docker-compose.yml
# See "Docker Compose Configuration" section below

# The docker-compose.yml will include:
# - postgis/postgis:16-3.4 image
# - Volume mount for data persistence
# - Environment variables for credentials
# - Health checks
```

### Option B: PostgreSQL on Host (Alternative)

```
PSEUDOCODE: Native PostgreSQL Installation (if needed)

# Only use this if Docker PostgreSQL has issues

# Add PostgreSQL APT repository
sh -c 'echo "deb https://apt.postgresql.org/pub/repos/apt \
    $(lsb_release -cs)-pgdg main" > /etc/apt/sources.list.d/pgdg.list'

# Import repository key
wget --quiet -O - https://www.postgresql.org/media/keys/ACCC4CF8.asc | \
    apt-key add -

# Update and install
apt-get update
apt-get install -y postgresql-16 postgresql-16-postgis-3

# Start and enable
systemctl start postgresql
systemctl enable postgresql

# Create database and user
sudo -u postgres psql

# In PostgreSQL shell:
CREATE USER pothole_user WITH PASSWORD 'your_secure_password_here';
CREATE DATABASE pothole_db OWNER pothole_user;
\c pothole_db
CREATE EXTENSION IF NOT EXISTS postgis;
GRANT ALL ON SCHEMA public TO pothole_user;
\q

# Configure to listen on localhost only (for Docker network access)
vim /etc/postgresql/16/main/postgresql.conf
# listen_addresses = 'localhost'

# Configure authentication
vim /etc/postgresql/16/main/pg_hba.conf
# Add: local pothole_db pothole_user md5
#      host  pothole_db pothole_user 172.17.0.0/16 md5  # Docker network

systemctl restart postgresql
```

---

## AWS S3 Configuration

### Step 1: Create AWS Account & IAM User

```
PROCESS: AWS S3 Setup
│
├── 1. Create AWS Account (if not exists)
│   ├── Go to https://aws.amazon.com
│   ├── Click "Create an AWS Account"
│   ├── Follow signup process
│   └── Free tier includes 5GB S3 storage for 12 months
│
├── 2. Create S3 Bucket
│   ├── Go to S3 Console: https://s3.console.aws.amazon.com
│   ├── Click "Create bucket"
│   ├── Bucket name: "pothole-detection-images-{unique-suffix}"
│   │   └── Example: "pothole-detection-images-yarmouk-2025"
│   ├── Region: eu-central-1 (Frankfurt - close to Hetzner Germany)
│   ├── Object Ownership: ACLs disabled (recommended)
│   ├── Block Public Access: Keep ALL checked (private bucket)
│   ├── Versioning: Disabled (not needed)
│   ├── Encryption: SSE-S3 (default)
│   └── Click "Create bucket"
│
├── 3. Create IAM User for API Access
│   ├── Go to IAM Console: https://console.aws.amazon.com/iam
│   ├── Click "Users" → "Create user"
│   ├── User name: "pothole-api-s3-user"
│   ├── Select "Access key - Programmatic access"
│   ├── Click "Next: Permissions"
│   │
│   ├── Attach policies:
│   │   └── Create custom policy (see below)
│   │
│   ├── Click "Create user"
│   └── SAVE the Access Key ID and Secret Access Key!
│       └── You won't see the secret again!
│
└── 4. Create IAM Policy
    │
    └── Custom policy JSON:
```

### IAM Policy for S3 Access

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "PotholeImagesBucketAccess",
            "Effect": "Allow",
            "Action": [
                "s3:PutObject",
                "s3:GetObject",
                "s3:DeleteObject",
                "s3:ListBucket"
            ],
            "Resource": [
                "arn:aws:s3:::pothole-detection-images-yarmouk-2025",
                "arn:aws:s3:::pothole-detection-images-yarmouk-2025/*"
            ]
        }
    ]
}
```

### Step 2: Configure S3 Bucket CORS (For Dashboard Image Display)

```
PROCESS: S3 CORS Configuration
│
├── Go to S3 → Your Bucket → Permissions → CORS
│
└── Add this configuration:
```

```json
[
    {
        "AllowedHeaders": ["*"],
        "AllowedMethods": ["GET"],
        "AllowedOrigins": [
            "https://dashboard.yoursite.com",
            "http://localhost:4321"
        ],
        "ExposeHeaders": [],
        "MaxAgeSeconds": 3600
    }
]
```

### Step 3: S3 Bucket Lifecycle Policy (Cost Optimization)

```
PROCESS: S3 Lifecycle Rules
│
├── Go to S3 → Your Bucket → Management → Lifecycle rules
├── Create rule:
│   ├── Rule name: "delete-old-images"
│   ├── Apply to all objects
│   │
│   └── Lifecycle rule actions:
│       └── Expire current versions: 365 days
│           (Delete images older than 1 year)
│
└── This keeps storage costs minimal for a student project
```

---

## Application Dockerfiles

### Backend Dockerfile

```dockerfile
# File: docker/backend.Dockerfile
# Multi-stage build for smaller image

# ============================================
# Stage 1: Build
# ============================================
FROM node:20-alpine AS builder

WORKDIR /app

# Copy package files
COPY backend/package*.json ./

# Install ALL dependencies (including devDependencies for build)
RUN npm ci

# Copy source code
COPY backend/ .

# Build TypeScript
RUN npm run build

# ============================================
# Stage 2: Production
# ============================================
FROM node:20-alpine AS production

WORKDIR /app

# Create non-root user for security
RUN addgroup -g 1001 -S nodejs
RUN adduser -S fastify -u 1001

# Copy package files
COPY backend/package*.json ./

# Install only production dependencies
RUN npm ci --only=production && npm cache clean --force

# Copy built files from builder stage
COPY --from=builder /app/dist ./dist

# Change ownership to non-root user
RUN chown -R fastify:nodejs /app

# Switch to non-root user
USER fastify

# Expose port
EXPOSE 3000

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:3000/health || exit 1

# Start server
CMD ["node", "dist/index.js"]
```

### Dashboard Dockerfile

```dockerfile
# File: docker/dashboard.Dockerfile
# Static build - Astro generates static HTML

# ============================================
# Stage 1: Build
# ============================================
FROM node:20-alpine AS builder

WORKDIR /app

# Copy package files
COPY dashboard/package*.json ./

# Install dependencies
RUN npm ci

# Copy source code
COPY dashboard/ .

# Build static site
RUN npm run build

# ============================================
# Stage 2: Production (Nginx for static files)
# ============================================
FROM nginx:alpine AS production

# Copy built static files
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy custom nginx config
COPY docker/nginx/dashboard-nginx.conf /etc/nginx/conf.d/default.conf

# Expose port
EXPOSE 4321

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:4321 || exit 1

CMD ["nginx", "-g", "daemon off;"]
```

### Dashboard Nginx Config (Inside Container)

```nginx
# File: docker/nginx/dashboard-nginx.conf

server {
    listen 4321;
    server_name localhost;
    root /usr/share/nginx/html;
    index index.html;

    # Gzip compression
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml;

    # Cache static assets
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    # SPA routing - serve index.html for all routes
    location / {
        try_files $uri $uri/ /index.html;
    }

    # Security headers
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
}
```

---

## Docker Compose Configuration

### Development Configuration

```yaml
# File: docker-compose.yml (Development)

version: "3.8"

services:
  # ============================================
  # PostgreSQL + PostGIS Database
  # ============================================
  postgres:
    image: postgis/postgis:16-3.4
    container_name: pothole-db
    restart: unless-stopped
    environment:
      POSTGRES_USER: pothole_user
      POSTGRES_PASSWORD: ${DB_PASSWORD:-dev_password_change_me}
      POSTGRES_DB: pothole_db
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./backend/src/db/init.sql:/docker-entrypoint-initdb.d/init.sql:ro
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U pothole_user -d pothole_db"]
      interval: 10s
      timeout: 5s
      retries: 5

  # ============================================
  # Backend API (Development with hot reload)
  # ============================================
  backend:
    build:
      context: .
      dockerfile: docker/backend.Dockerfile
      target: builder  # Use builder stage for dev
    container_name: pothole-api
    restart: unless-stopped
    working_dir: /app
    command: npm run dev
    environment:
      NODE_ENV: development
      PORT: 3000
      DATABASE_URL: postgresql://pothole_user:${DB_PASSWORD:-dev_password_change_me}@postgres:5432/pothole_db
      JWT_SECRET: ${JWT_SECRET:-dev_jwt_secret_change_in_production}
      AWS_ACCESS_KEY_ID: ${AWS_ACCESS_KEY_ID}
      AWS_SECRET_ACCESS_KEY: ${AWS_SECRET_ACCESS_KEY}
      AWS_REGION: ${AWS_REGION:-eu-central-1}
      AWS_S3_BUCKET: ${AWS_S3_BUCKET}
    volumes:
      - ./backend:/app
      - /app/node_modules  # Don't override node_modules
    ports:
      - "3000:3000"
    depends_on:
      postgres:
        condition: service_healthy

  # ============================================
  # Dashboard (Development with hot reload)
  # ============================================
  dashboard:
    build:
      context: .
      dockerfile: docker/dashboard.Dockerfile
      target: builder
    container_name: pothole-dashboard
    restart: unless-stopped
    working_dir: /app
    command: npm run dev -- --host 0.0.0.0
    environment:
      PUBLIC_API_URL: http://localhost:3000
    volumes:
      - ./dashboard:/app
      - /app/node_modules
    ports:
      - "4321:4321"

volumes:
  postgres_data:
```

### Production Configuration

```yaml
# File: docker-compose.prod.yml (Production)

version: "3.8"

services:
  # ============================================
  # PostgreSQL + PostGIS Database
  # ============================================
  postgres:
    image: postgis/postgis:16-3.4
    container_name: pothole-db
    restart: always
    environment:
      POSTGRES_USER: pothole_user
      POSTGRES_PASSWORD: ${DB_PASSWORD}
      POSTGRES_DB: pothole_db
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - pothole-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U pothole_user -d pothole_db"]
      interval: 10s
      timeout: 5s
      retries: 5
    # No ports exposed - only internal access

  # ============================================
  # Backend API
  # ============================================
  backend:
    build:
      context: .
      dockerfile: docker/backend.Dockerfile
      target: production
    container_name: pothole-api
    restart: always
    environment:
      NODE_ENV: production
      PORT: 3000
      HOST: 0.0.0.0
      DATABASE_URL: postgresql://pothole_user:${DB_PASSWORD}@postgres:5432/pothole_db
      JWT_SECRET: ${JWT_SECRET}
      JWT_EXPIRES_IN: 7d
      AWS_ACCESS_KEY_ID: ${AWS_ACCESS_KEY_ID}
      AWS_SECRET_ACCESS_KEY: ${AWS_SECRET_ACCESS_KEY}
      AWS_REGION: ${AWS_REGION}
      AWS_S3_BUCKET: ${AWS_S3_BUCKET}
      CORS_ORIGIN: https://${DASHBOARD_DOMAIN}
    ports:
      - "127.0.0.1:3000:3000"  # Only localhost, Nginx will proxy
    networks:
      - pothole-network
    depends_on:
      postgres:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:3000/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  # ============================================
  # Dashboard (Static files served by Nginx on host)
  # ============================================
  dashboard:
    build:
      context: .
      dockerfile: docker/dashboard.Dockerfile
      target: production
    container_name: pothole-dashboard
    restart: always
    ports:
      - "127.0.0.1:4321:4321"  # Only localhost
    networks:
      - pothole-network
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:4321"]
      interval: 30s
      timeout: 10s
      retries: 3

networks:
  pothole-network:
    driver: bridge

volumes:
  postgres_data:
```

### Environment File Template

```bash
# File: .env.example (Copy to .env and fill in values)

# ============================================
# Database
# ============================================
DB_PASSWORD=your_very_secure_password_here_min_32_chars

# ============================================
# JWT Authentication
# ============================================
JWT_SECRET=your_256_bit_secret_key_use_openssl_rand_hex_32

# ============================================
# AWS S3
# ============================================
AWS_ACCESS_KEY_ID=AKIA...
AWS_SECRET_ACCESS_KEY=your_aws_secret_key
AWS_REGION=eu-central-1
AWS_S3_BUCKET=pothole-detection-images-yarmouk-2025

# ============================================
# Domains (Production)
# ============================================
API_DOMAIN=api.yoursite.com
DASHBOARD_DOMAIN=dashboard.yoursite.com
```

---

## Nginx Reverse Proxy

### Install Nginx on Host

```
PSEUDOCODE: Nginx Installation

# Install Nginx
apt install nginx -y

# Remove default site
rm /etc/nginx/sites-enabled/default

# Verify Nginx is running
systemctl status nginx

# Enable on boot
systemctl enable nginx
```

### Main Nginx Configuration

```nginx
# File: /etc/nginx/nginx.conf

user www-data;
worker_processes auto;
pid /run/nginx.pid;
error_log /var/log/nginx/error.log;

events {
    worker_connections 1024;
    multi_accept on;
}

http {
    # Basic Settings
    sendfile on;
    tcp_nopush on;
    tcp_nodelay on;
    keepalive_timeout 65;
    types_hash_max_size 2048;
    server_tokens off;  # Hide Nginx version

    # MIME Types
    include /etc/nginx/mime.types;
    default_type application/octet-stream;

    # Logging
    access_log /var/log/nginx/access.log;
    error_log /var/log/nginx/error.log;

    # Gzip Compression
    gzip on;
    gzip_vary on;
    gzip_proxied any;
    gzip_comp_level 6;
    gzip_types text/plain text/css text/xml application/json application/javascript 
               application/xml application/xml+rss text/javascript image/svg+xml;

    # Rate Limiting
    limit_req_zone $binary_remote_addr zone=api_limit:10m rate=10r/s;
    limit_req_zone $binary_remote_addr zone=login_limit:10m rate=5r/m;

    # File Upload Size (for pothole images)
    client_max_body_size 10M;

    # Include site configurations
    include /etc/nginx/sites-enabled/*;
}
```

### API Server Configuration

```nginx
# File: /etc/nginx/sites-available/api.yoursite.com

# Upstream for backend
upstream backend_api {
    server 127.0.0.1:3000;
    keepalive 32;
}

# Redirect HTTP to HTTPS
server {
    listen 80;
    listen [::]:80;
    server_name api.yoursite.com;

    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }

    location / {
        return 301 https://$host$request_uri;
    }
}

# HTTPS Server
server {
    listen 443 ssl http2;
    listen [::]:443 ssl http2;
    server_name api.yoursite.com;

    # SSL Certificates (Let's Encrypt)
    ssl_certificate /etc/letsencrypt/live/api.yoursite.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/api.yoursite.com/privkey.pem;

    # SSL Configuration
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384;
    ssl_prefer_server_ciphers off;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 1d;

    # Security Headers
    add_header X-Frame-Options "DENY" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;

    # Health check endpoint (no rate limiting)
    location /health {
        proxy_pass http://backend_api;
        proxy_http_version 1.1;
        proxy_set_header Connection "";
    }

    # Authentication endpoints (stricter rate limiting)
    location /api/auth/login {
        limit_req zone=login_limit burst=3 nodelay;
        limit_req_status 429;

        proxy_pass http://backend_api;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header Connection "";
    }

    # API endpoints
    location /api {
        limit_req zone=api_limit burst=20 nodelay;
        limit_req_status 429;

        proxy_pass http://backend_api;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header Connection "";
        
        # Timeouts
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # Block all other paths
    location / {
        return 404;
    }
}
```

### Dashboard Server Configuration

```nginx
# File: /etc/nginx/sites-available/dashboard.yoursite.com

# Upstream for dashboard
upstream dashboard_app {
    server 127.0.0.1:4321;
    keepalive 32;
}

# Redirect HTTP to HTTPS
server {
    listen 80;
    listen [::]:80;
    server_name dashboard.yoursite.com;

    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }

    location / {
        return 301 https://$host$request_uri;
    }
}

# HTTPS Server
server {
    listen 443 ssl http2;
    listen [::]:443 ssl http2;
    server_name dashboard.yoursite.com;

    # SSL Certificates
    ssl_certificate /etc/letsencrypt/live/dashboard.yoursite.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/dashboard.yoursite.com/privkey.pem;

    # SSL Configuration (same as API)
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384;
    ssl_prefer_server_ciphers off;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 1d;

    # Security Headers
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;

    # Static assets caching
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        proxy_pass http://dashboard_app;
        proxy_http_version 1.1;
        proxy_set_header Connection "";
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    # All other requests
    location / {
        proxy_pass http://dashboard_app;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header Connection "";
    }
}
```

### Enable Sites

```
PSEUDOCODE: Enable Nginx Sites

# Create symbolic links to enable sites
ln -s /etc/nginx/sites-available/api.yoursite.com /etc/nginx/sites-enabled/
ln -s /etc/nginx/sites-available/dashboard.yoursite.com /etc/nginx/sites-enabled/

# Test configuration
nginx -t

IF configuration valid:
    # Reload Nginx
    systemctl reload nginx
ELSE:
    FIX configuration errors
    RUN nginx -t again
```

---

## SSL with Let's Encrypt

### Step 1: Install Certbot

```
PSEUDOCODE: Certbot Installation

# Install Certbot with Nginx plugin
apt install certbot python3-certbot-nginx -y

# Verify installation
certbot --version
```

### Step 2: Create Certificates (Before DNS Setup)

```
PSEUDOCODE: Initial Certificate Setup

# IMPORTANT: DNS must be configured FIRST (see next section)
# Certbot needs to verify domain ownership

# Create webroot directory for ACME challenge
mkdir -p /var/www/certbot

# Get certificates for API domain
certbot certonly --webroot \
    -w /var/www/certbot \
    -d api.yoursite.com \
    --email your-email@example.com \
    --agree-tos \
    --non-interactive

# Get certificates for Dashboard domain
certbot certonly --webroot \
    -w /var/www/certbot \
    -d dashboard.yoursite.com \
    --email your-email@example.com \
    --agree-tos \
    --non-interactive

# Verify certificates
certbot certificates
```

### Step 3: Configure Auto-Renewal

```
PSEUDOCODE: Certificate Auto-Renewal

# Test renewal process
certbot renew --dry-run

# Certbot automatically adds a cron job, but verify:
systemctl list-timers | grep certbot

# The timer should run twice daily
# Certificates are renewed if expiring within 30 days

# Create post-renewal hook to reload Nginx
mkdir -p /etc/letsencrypt/renewal-hooks/post

# Create hook script
cat > /etc/letsencrypt/renewal-hooks/post/reload-nginx.sh << 'EOF'
#!/bin/bash
systemctl reload nginx
EOF

chmod +x /etc/letsencrypt/renewal-hooks/post/reload-nginx.sh
```

---

## Domain & DNS (Cloudflare)

### Step 1: Get a Domain

```
PROCESS: Domain Registration Options
│
├── Option A: Cheap domains (~$10/year)
│   ├── Namecheap.com
│   ├── Porkbun.com
│   └── Cloudflare Registrar (at-cost pricing)
│
├── Option B: Free subdomains (for testing)
│   ├── FreeDNS (freedns.afraid.org)
│   └── DuckDNS (duckdns.org)
│
└── Recommendation: Get a cheap .com or .net domain
    Example: pothole-detect.com, roadwatch.net
```

### Step 2: Setup Cloudflare

```
PROCESS: Cloudflare Setup
│
├── 1. Create Cloudflare Account
│   ├── Go to https://cloudflare.com
│   ├── Sign up with email
│   └── Verify email
│
├── 2. Add Your Domain
│   ├── Click "Add a Site"
│   ├── Enter your domain: yoursite.com
│   ├── Select FREE plan
│   └── Click "Continue"
│
├── 3. Update Nameservers
│   ├── Cloudflare will show you 2 nameservers:
│   │   ├── Example: bella.ns.cloudflare.com
│   │   └── Example: todd.ns.cloudflare.com
│   │
│   ├── Go to your domain registrar
│   ├── Find "Nameservers" or "DNS" settings
│   ├── Change nameservers to Cloudflare's
│   └── Wait 24-48 hours for propagation (usually faster)
│
└── 4. Cloudflare shows "Active" when complete
```

### Step 3: Configure DNS Records

```
PROCESS: DNS Records Configuration
│
├── Go to: Cloudflare Dashboard → Your Domain → DNS
│
├── Add A Record for API:
│   ├── Type: A
│   ├── Name: api
│   ├── IPv4 address: 168.119.xxx.xxx (your Hetzner IP)
│   ├── Proxy status: DNS only (gray cloud) - for SSL setup
│   │   └── Can enable proxy (orange cloud) after SSL works
│   └── TTL: Auto
│
├── Add A Record for Dashboard:
│   ├── Type: A
│   ├── Name: dashboard
│   ├── IPv4 address: 168.119.xxx.xxx (same IP)
│   ├── Proxy status: DNS only (gray cloud)
│   └── TTL: Auto
│
├── Add A Record for root (optional):
│   ├── Type: A
│   ├── Name: @ (represents root domain)
│   ├── IPv4 address: 168.119.xxx.xxx
│   ├── Proxy status: DNS only
│   └── TTL: Auto
│
└── Verify DNS propagation:
    └── RUN: dig api.yoursite.com
    └── RUN: dig dashboard.yoursite.com
```

### Step 4: Cloudflare SSL Settings

```
PROCESS: Cloudflare SSL Configuration
│
├── Go to: SSL/TLS → Overview
│   └── Set mode: "Full (strict)"
│       ├── Requires valid SSL cert on origin (Let's Encrypt)
│       └── End-to-end encryption
│
├── Go to: SSL/TLS → Edge Certificates
│   ├── Always Use HTTPS: ON
│   ├── Minimum TLS Version: 1.2
│   └── Automatic HTTPS Rewrites: ON
│
└── AFTER Let's Encrypt certs are working:
    └── Enable Proxy (orange cloud) for DDoS protection
```

---

## Deployment Scripts

### Initial Setup Script

```bash
#!/bin/bash
# File: scripts/setup-server.sh
# Run this on a fresh Ubuntu 22.04 server

set -e  # Exit on any error

echo "=========================================="
echo "Pothole Detection Server Setup Script"
echo "=========================================="

# ============================================
# 1. Update System
# ============================================
echo "[1/8] Updating system..."
apt update && apt upgrade -y

# ============================================
# 2. Install Dependencies
# ============================================
echo "[2/8] Installing dependencies..."
apt install -y \
    curl wget git vim htop unzip \
    software-properties-common \
    apt-transport-https ca-certificates gnupg \
    lsb-release ufw fail2ban nginx certbot \
    python3-certbot-nginx

# ============================================
# 3. Install Docker
# ============================================
echo "[3/8] Installing Docker..."
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | \
    gpg --dearmor -o /etc/apt/keyrings/docker.gpg

echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
    https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | \
    tee /etc/apt/sources.list.d/docker.list > /dev/null

apt update
apt install -y docker-ce docker-ce-cli containerd.io \
    docker-buildx-plugin docker-compose-plugin

systemctl enable docker

# ============================================
# 4. Create Deploy User
# ============================================
echo "[4/8] Creating deploy user..."
if ! id "deploy" &>/dev/null; then
    useradd -m -s /bin/bash deploy
    usermod -aG sudo,docker deploy
    mkdir -p /home/deploy/.ssh
    cp /root/.ssh/authorized_keys /home/deploy/.ssh/
    chown -R deploy:deploy /home/deploy/.ssh
    chmod 700 /home/deploy/.ssh
    chmod 600 /home/deploy/.ssh/authorized_keys
fi

# ============================================
# 5. Configure Firewall
# ============================================
echo "[5/8] Configuring firewall..."
ufw --force reset
ufw default deny incoming
ufw default allow outgoing
ufw allow 22/tcp
ufw allow 80/tcp
ufw allow 443/tcp
ufw --force enable

# ============================================
# 6. Configure Fail2Ban
# ============================================
echo "[6/8] Configuring Fail2Ban..."
cp /etc/fail2ban/jail.conf /etc/fail2ban/jail.local
systemctl enable fail2ban
systemctl restart fail2ban

# ============================================
# 7. Setup Swap
# ============================================
echo "[7/8] Setting up swap..."
if [ ! -f /swapfile ]; then
    fallocate -l 4G /swapfile
    chmod 600 /swapfile
    mkswap /swapfile
    swapon /swapfile
    echo '/swapfile none swap sw 0 0' >> /etc/fstab
    echo 'vm.swappiness=10' >> /etc/sysctl.conf
    sysctl -p
fi

# ============================================
# 8. Create Project Directory
# ============================================
echo "[8/8] Creating project directory..."
mkdir -p /home/deploy/pothole
chown deploy:deploy /home/deploy/pothole

echo "=========================================="
echo "Server setup complete!"
echo "=========================================="
echo ""
echo "Next steps:"
echo "1. Configure DNS records pointing to this server"
echo "2. Run deploy.sh to deploy the application"
echo "3. Setup SSL certificates with certbot"
```

### Deployment Script

```bash
#!/bin/bash
# File: scripts/deploy.sh
# Deploy or update the application

set -e

APP_DIR="/home/deploy/pothole"
REPO_URL="git@github.com:yourusername/smart-pothole-detection.git"

echo "=========================================="
echo "Deploying Pothole Detection System"
echo "=========================================="

# ============================================
# 1. Pull Latest Code
# ============================================
echo "[1/5] Pulling latest code..."
cd $APP_DIR

if [ -d ".git" ]; then
    git pull origin main
else
    git clone $REPO_URL .
fi

# ============================================
# 2. Check Environment File
# ============================================
echo "[2/5] Checking environment..."
if [ ! -f ".env" ]; then
    echo "ERROR: .env file not found!"
    echo "Copy .env.example to .env and configure it."
    exit 1
fi

# ============================================
# 3. Build Docker Images
# ============================================
echo "[3/5] Building Docker images..."
docker compose -f docker-compose.prod.yml build

# ============================================
# 4. Run Database Migrations
# ============================================
echo "[4/5] Running database migrations..."
docker compose -f docker-compose.prod.yml run --rm backend npm run db:migrate

# ============================================
# 5. Start/Restart Services
# ============================================
echo "[5/5] Starting services..."
docker compose -f docker-compose.prod.yml up -d

# Wait for services to be healthy
echo "Waiting for services to be ready..."
sleep 10

# ============================================
# Health Check
# ============================================
echo ""
echo "Checking service health..."

# Check backend
if curl -s http://localhost:3000/health | grep -q "ok"; then
    echo "✓ Backend API is healthy"
else
    echo "✗ Backend API health check failed"
fi

# Check dashboard
if curl -s http://localhost:4321 | grep -q "html"; then
    echo "✓ Dashboard is healthy"
else
    echo "✗ Dashboard health check failed"
fi

# Check database
if docker compose -f docker-compose.prod.yml exec -T postgres pg_isready -U pothole_user; then
    echo "✓ Database is healthy"
else
    echo "✗ Database health check failed"
fi

echo ""
echo "=========================================="
echo "Deployment complete!"
echo "=========================================="
docker compose -f docker-compose.prod.yml ps
```

### SSL Setup Script

```bash
#!/bin/bash
# File: scripts/setup-ssl.sh
# Setup Let's Encrypt SSL certificates

set -e

API_DOMAIN=${1:-"api.yoursite.com"}
DASHBOARD_DOMAIN=${2:-"dashboard.yoursite.com"}
EMAIL=${3:-"your-email@example.com"}

echo "=========================================="
echo "Setting up SSL Certificates"
echo "=========================================="
echo "API Domain: $API_DOMAIN"
echo "Dashboard Domain: $DASHBOARD_DOMAIN"
echo "Email: $EMAIL"
echo ""

# Create webroot directory
mkdir -p /var/www/certbot

# Get certificate for API
echo "[1/3] Getting certificate for API..."
certbot certonly --webroot \
    -w /var/www/certbot \
    -d $API_DOMAIN \
    --email $EMAIL \
    --agree-tos \
    --non-interactive

# Get certificate for Dashboard
echo "[2/3] Getting certificate for Dashboard..."
certbot certonly --webroot \
    -w /var/www/certbot \
    -d $DASHBOARD_DOMAIN \
    --email $EMAIL \
    --agree-tos \
    --non-interactive

# Reload Nginx
echo "[3/3] Reloading Nginx..."
nginx -t && systemctl reload nginx

echo ""
echo "=========================================="
echo "SSL setup complete!"
echo "=========================================="
certbot certificates
```

---

## Monitoring & Maintenance

### Log Viewing Commands

```
PSEUDOCODE: Useful Monitoring Commands

# View Docker container logs
docker compose -f docker-compose.prod.yml logs -f backend    # API logs
docker compose -f docker-compose.prod.yml logs -f postgres   # DB logs
docker compose -f docker-compose.prod.yml logs -f dashboard  # Dashboard logs

# View all logs together
docker compose -f docker-compose.prod.yml logs -f

# View Nginx logs
tail -f /var/log/nginx/access.log
tail -f /var/log/nginx/error.log

# Check system resources
htop                    # Interactive process viewer
df -h                   # Disk usage
free -h                 # Memory usage
docker stats            # Container resource usage

# Check service status
systemctl status nginx
systemctl status docker
docker compose -f docker-compose.prod.yml ps
```

### Health Check Endpoint

```
PSEUDOCODE: Health Check Implementation

# Backend should expose /health endpoint
# Returns 200 OK with JSON:
{
    "status": "ok",
    "timestamp": "2025-01-01T12:00:00Z",
    "database": "connected",
    "uptime": 12345
}

# Simple monitoring script
cat > /home/deploy/check-health.sh << 'EOF'
#!/bin/bash
API_HEALTH=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:3000/health)
DASH_HEALTH=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:4321)

if [ "$API_HEALTH" != "200" ] || [ "$DASH_HEALTH" != "200" ]; then
    echo "Health check failed: API=$API_HEALTH, Dashboard=$DASH_HEALTH"
    # Could send alert here (email, Telegram, etc.)
fi
EOF

chmod +x /home/deploy/check-health.sh

# Add to crontab to run every 5 minutes
crontab -e
# Add: */5 * * * * /home/deploy/check-health.sh >> /var/log/health-check.log 2>&1
```

### Docker Cleanup

```
PSEUDOCODE: Regular Maintenance

# Remove unused Docker resources (run monthly)
docker system prune -f             # Remove stopped containers, unused networks
docker image prune -f              # Remove dangling images
docker volume prune -f             # Remove unused volumes (CAREFUL!)

# Check Docker disk usage
docker system df

# Rotate Docker logs if they get too large
# (Already configured in daemon.json with max-size: 10m)
```

---

## Backup Strategy

### Database Backup Script

```bash
#!/bin/bash
# File: scripts/backup-db.sh
# Run daily via cron

set -e

BACKUP_DIR="/home/deploy/backups"
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="pothole_db_${DATE}.sql.gz"
RETENTION_DAYS=7

# Create backup directory
mkdir -p $BACKUP_DIR

# Dump database and compress
echo "Creating backup: $BACKUP_FILE"
docker compose -f /home/deploy/pothole/docker-compose.prod.yml \
    exec -T postgres pg_dump -U pothole_user pothole_db | \
    gzip > "${BACKUP_DIR}/${BACKUP_FILE}"

# Verify backup was created
if [ -f "${BACKUP_DIR}/${BACKUP_FILE}" ]; then
    SIZE=$(du -h "${BACKUP_DIR}/${BACKUP_FILE}" | cut -f1)
    echo "Backup created successfully: $SIZE"
else
    echo "ERROR: Backup failed!"
    exit 1
fi

# Delete old backups
echo "Cleaning up backups older than ${RETENTION_DAYS} days..."
find $BACKUP_DIR -name "pothole_db_*.sql.gz" -mtime +$RETENTION_DAYS -delete

# List current backups
echo "Current backups:"
ls -lah $BACKUP_DIR/pothole_db_*.sql.gz
```

### Backup to S3 (Optional)

```bash
#!/bin/bash
# File: scripts/backup-to-s3.sh
# Upload backups to S3 for offsite storage

set -e

BACKUP_DIR="/home/deploy/backups"
S3_BUCKET="s3://your-backup-bucket/pothole-db/"
DATE=$(date +%Y%m%d_%H%M%S)

# Find latest backup
LATEST_BACKUP=$(ls -t ${BACKUP_DIR}/pothole_db_*.sql.gz | head -1)

if [ -z "$LATEST_BACKUP" ]; then
    echo "No backup found to upload"
    exit 1
fi

# Upload to S3
echo "Uploading $LATEST_BACKUP to S3..."
aws s3 cp "$LATEST_BACKUP" "${S3_BUCKET}"

echo "Backup uploaded successfully"
```

### Database Restore Process

```
PSEUDOCODE: Database Restore

# List available backups
ls -la /home/deploy/backups/

# Choose backup to restore
BACKUP_FILE="/home/deploy/backups/pothole_db_20250101_120000.sql.gz"

# Stop backend (to prevent writes during restore)
docker compose -f docker-compose.prod.yml stop backend

# Restore database
gunzip -c $BACKUP_FILE | docker compose -f docker-compose.prod.yml \
    exec -T postgres psql -U pothole_user pothole_db

# Restart backend
docker compose -f docker-compose.prod.yml start backend

# Verify restore
docker compose -f docker-compose.prod.yml exec postgres \
    psql -U pothole_user -d pothole_db -c "SELECT COUNT(*) FROM potholes;"
```

### Cron Jobs Setup

```
PSEUDOCODE: Cron Configuration

# Edit crontab for deploy user
su - deploy
crontab -e

# Add these scheduled tasks:

# Daily database backup at 3 AM
0 3 * * * /home/deploy/pothole/scripts/backup-db.sh >> /var/log/pothole-backup.log 2>&1

# Health check every 5 minutes
*/5 * * * * /home/deploy/check-health.sh >> /var/log/health-check.log 2>&1

# Weekly Docker cleanup (Sunday 4 AM)
0 4 * * 0 docker system prune -f >> /var/log/docker-cleanup.log 2>&1

# Log rotation (built into Ubuntu via logrotate)
```

---

## Troubleshooting

### Common Issues and Solutions

```
TROUBLESHOOTING GUIDE

┌─────────────────────────────────────────────────────────────────────────────┐
│ Issue: Can't connect to server via SSH                                       │
├─────────────────────────────────────────────────────────────────────────────┤
│ Checks:                                                                      │
│ ├── Verify IP address is correct                                            │
│ ├── Check Hetzner firewall allows port 22                                   │
│ ├── Check UFW allows port 22: ufw status                                    │
│ ├── Verify SSH key permissions: chmod 600 ~/.ssh/pothole_hetzner            │
│ └── Try verbose: ssh -v -i ~/.ssh/pothole_hetzner user@ip                   │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│ Issue: Docker containers not starting                                        │
├─────────────────────────────────────────────────────────────────────────────┤
│ Checks:                                                                      │
│ ├── View logs: docker compose logs backend                                  │
│ ├── Check .env file exists and has all variables                            │
│ ├── Verify port isn't in use: ss -tlnp | grep 3000                          │
│ ├── Check disk space: df -h                                                 │
│ └── Restart Docker: systemctl restart docker                                │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│ Issue: Database connection refused                                           │
├─────────────────────────────────────────────────────────────────────────────┤
│ Checks:                                                                      │
│ ├── Is postgres container running? docker ps                                │
│ ├── Check postgres logs: docker compose logs postgres                       │
│ ├── Verify DATABASE_URL in .env                                             │
│ ├── Test connection: docker compose exec postgres psql -U pothole_user      │
│ └── Check if healthy: docker compose ps (should show "healthy")             │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│ Issue: SSL certificate errors / can't get certificates                       │
├─────────────────────────────────────────────────────────────────────────────┤
│ Checks:                                                                      │
│ ├── DNS is pointing to your server: dig api.yoursite.com                    │
│ ├── Port 80 is open and accessible                                          │
│ ├── Nginx is running: systemctl status nginx                                │
│ ├── Check Nginx config: nginx -t                                            │
│ ├── Cloudflare proxy is OFF (gray cloud) during cert setup                  │
│ └── View certbot logs: cat /var/log/letsencrypt/letsencrypt.log             │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│ Issue: 502 Bad Gateway errors                                                │
├─────────────────────────────────────────────────────────────────────────────┤
│ Checks:                                                                      │
│ ├── Is backend running? docker compose ps                                   │
│ ├── Backend logs: docker compose logs backend                               │
│ ├── Test backend directly: curl http://localhost:3000/health                │
│ ├── Check Nginx upstream config matches port                                │
│ └── Nginx error log: tail /var/log/nginx/error.log                          │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│ Issue: S3 upload failures                                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│ Checks:                                                                      │
│ ├── Verify AWS credentials in .env                                          │
│ ├── Check bucket name is correct                                            │
│ ├── Verify IAM policy allows s3:PutObject                                   │
│ ├── Check bucket region matches AWS_REGION                                  │
│ └── Test with AWS CLI: aws s3 ls s3://your-bucket/                          │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│ Issue: Server running slow / out of memory                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│ Checks:                                                                      │
│ ├── Check memory: free -h                                                   │
│ ├── Check container usage: docker stats                                     │
│ ├── Check disk: df -h                                                       │
│ ├── Check CPU: htop                                                         │
│ ├── Restart containers: docker compose restart                              │
│ └── Consider upgrading to CX41 if consistent issues                         │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Useful Debug Commands

```bash
# ============================================
# System Information
# ============================================
uname -a                        # Kernel info
lsb_release -a                  # Ubuntu version
uptime                          # Server uptime
df -h                           # Disk space
free -h                         # Memory
htop                            # Process monitor

# ============================================
# Docker Commands
# ============================================
docker ps                       # Running containers
docker ps -a                    # All containers
docker images                   # All images
docker compose ps               # Compose services status
docker compose logs -f          # Follow all logs
docker stats                    # Live resource usage
docker system df                # Docker disk usage

# ============================================
# Network Commands
# ============================================
ss -tlnp                        # Listening ports
curl -I http://localhost:3000   # Test API locally
dig api.yoursite.com            # DNS lookup
ping api.yoursite.com           # Test connectivity

# ============================================
# Nginx Commands
# ============================================
nginx -t                        # Test config
nginx -T                        # Show full config
systemctl status nginx          # Service status
tail -f /var/log/nginx/error.log # Error log

# ============================================
# Database Commands
# ============================================
docker compose exec postgres psql -U pothole_user -d pothole_db
# Then in psql:
\dt                             # List tables
\d potholes                     # Describe table
SELECT COUNT(*) FROM potholes;  # Count records
```

---

## Deployment Checklist

```
PRE-DEPLOYMENT CHECKLIST

[ ] Server Setup
    [ ] Hetzner account created
    [ ] VPS provisioned (CX31, Ubuntu 22.04)
    [ ] SSH key added
    [ ] Firewall configured (ports 22, 80, 443)
    [ ] Initial server setup script run

[ ] DNS & Domain
    [ ] Domain purchased
    [ ] Cloudflare account created
    [ ] Nameservers updated
    [ ] A records created for api and dashboard subdomains
    [ ] DNS propagation verified

[ ] Docker & Application
    [ ] Docker installed
    [ ] deploy user created and in docker group
    [ ] Repository cloned
    [ ] .env file configured with all secrets
    [ ] Docker images build successfully
    [ ] Containers start without errors

[ ] Database
    [ ] PostgreSQL container healthy
    [ ] PostGIS extension enabled
    [ ] Migrations run successfully
    [ ] Initial admin user created

[ ] SSL & Security
    [ ] Let's Encrypt certificates obtained
    [ ] HTTPS redirects working
    [ ] Cloudflare proxy enabled
    [ ] Fail2ban configured
    [ ] UFW firewall enabled

[ ] AWS S3
    [ ] S3 bucket created
    [ ] IAM user with limited permissions
    [ ] CORS configured
    [ ] Test upload/download working

[ ] Final Verification
    [ ] API health check passes
    [ ] Dashboard loads correctly
    [ ] Login functionality works
    [ ] Mobile app can connect to API
    [ ] Image upload to S3 works
    [ ] Cron jobs scheduled
    [ ] Backup script tested
```

---

## Quick Reference

### Essential Commands

```bash
# Start services
cd /home/deploy/pothole
docker compose -f docker-compose.prod.yml up -d

# Stop services
docker compose -f docker-compose.prod.yml down

# Restart services
docker compose -f docker-compose.prod.yml restart

# View logs
docker compose -f docker-compose.prod.yml logs -f

# Update and redeploy
./scripts/deploy.sh

# Backup database
./scripts/backup-db.sh

# Check status
docker compose -f docker-compose.prod.yml ps
```

### Important Paths

```
/home/deploy/pothole/           # Application root
/home/deploy/pothole/.env       # Environment variables
/home/deploy/backups/           # Database backups
/etc/nginx/sites-available/     # Nginx configs
/etc/letsencrypt/live/          # SSL certificates
/var/log/nginx/                 # Nginx logs
/var/log/letsencrypt/           # Certbot logs
```

---

## Next Steps

After deployment is complete:

1. **Test API endpoints** from mobile app
2. **Create initial admin user** via database or registration
3. **Monitor logs** for first few days
4. **Set up alerts** (optional: email, Telegram, Discord webhook)
5. **Document any custom configurations** for team reference

See [Timeline & Task Breakdown](./06-timeline.md) for the detailed implementation schedule.
