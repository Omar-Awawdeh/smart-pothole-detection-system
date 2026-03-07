#!/bin/bash
# Production Deployment Script for Pothole Detection System
# Run this on your VPS after initial setup

set -e  # Exit on any error

echo "🚀 Starting production deployment..."

# Check if .env file exists
if [ ! -f .env ]; then
    echo "❌ .env file not found! Please create one from .env.example"
    exit 1
fi

# Stop existing containers
echo "🛑 Stopping existing containers..."
docker-compose -f docker-compose.prod.yml down || true

# Pull latest code
echo "📥 Pulling latest code..."
git pull origin main

# Build and start containers
echo "🏗️ Building and starting containers..."
docker-compose -f docker-compose.prod.yml up -d --build

# Wait for containers to be healthy
echo "⏳ Waiting for services to be healthy..."
sleep 10

# Check if services are running
echo "✅ Checking service health..."
if curl -s http://localhost:3000/health | grep -q "Healthy"; then
    echo "✅ Backend is healthy"
else
    echo "❌ Backend health check failed"
    exit 1
fi

if curl -s http://localhost:4321 > /dev/null; then
    echo "✅ Dashboard is accessible"
else
    echo "❌ Dashboard health check failed"
    exit 1
fi

echo "🎉 Deployment complete!"
echo ""
echo "🔍 Check your services:"
echo "  Backend: https://api.potholesystem.tech"
echo "  Dashboard: https://potholesystem.tech"
echo ""
echo "📋 Useful commands:"
echo "  View logs: docker-compose -f docker-compose.prod.yml logs -f"
echo "  Restart: docker-compose -f docker-compose.prod.yml restart"
echo "  Stop: docker-compose -f docker-compose.prod.yml down"
