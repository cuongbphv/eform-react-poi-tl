# Docker Compose với OnlyOffice - Complete Setup Guide

## **Quick Start:**

### 1. **Create .env file:**
```bash
# Copy template và customize
cp .env.example .env
nano .env  # Update JWT secret và passwords
```

### 2. **Start all services:**
```bash
# Build và start tất cả
docker-compose up -d --build

# Hoặc sử dụng script
chmod +x scripts/setup.sh
./scripts/setup.sh
```

### 3. **Verify services:**
```bash
# Check containers
docker-compose ps

# Check logs
docker-compose logs -f onlyoffice-documentserver
docker-compose logs -f eform-backend
```

## **Services Overview:**

| Service | Port | Description |
|---------|------|-------------|
| **onlyoffice-documentserver** | 8081 | OnlyOffice Document Server |
| **eform-backend** | 8080 | Spring Boot API |
| **eform-frontend** | 3000 | React Application |
| **postgres** | 5432 | PostgreSQL Database |
| **redis** | 6379 | Cache & Sessions |
| **nginx** | 8081/8443 | Reverse Proxy (Optional) |

## **Management Commands:**

### **Basic Operations:**
```bash
# Start all services
docker-compose up -d

# Stop all services
docker-compose down

# Restart specific service
docker-compose restart eform-backend

# View logs
docker-compose logs -f eform-backend
docker-compose logs -f onlyoffice-documentserver

# Execute commands in container
docker-compose exec eform-backend bash
docker-compose exec postgres psql -U postgres
```

### **Development Commands:**
```bash
# Rebuild specific service
docker-compose build eform-backend
docker-compose up -d eform-backend

# Scale service (if needed)
docker-compose up -d --scale eform-backend=2

# Pull latest images
docker-compose pull
```

## **Environment Variables:**

### **Critical Variables (Must Update):**
- `ONLYOFFICE_JWT_SECRET` - JWT key cho OnlyOffice security
- `POSTGRES_EFORM_PASSWORD` - Main database password
- `POSTGRES_ONLYOFFICE_PASSWORD` - OnlyOffice database password

### **Optional Variables:**
- `BACKEND_PORT` - Backend port (default: 8080)
- `FRONTEND_PORT` - Frontend port (default: 3000)
- `SPRING_PROFILE` - Spring environment (development/production)

## **Health Checks:**

```bash
# OnlyOffice health
curl http://localhost:8081/healthcheck
# Should return: {"status": "true"}

# Backend health
curl http://localhost:8080/actuator/health

# Frontend access
curl http://localhost:3000

# Database connection
docker-compose exec postgres psql -U postgres -c "SELECT version();"
```

## **Monitoring:**

### **Container Stats:**
```bash
# Resource usage
docker stats

# Specific container logs
docker-compose logs --tail=100 eform-backend

# Follow logs in real-time
docker-compose logs -f --tail=10 onlyoffice-documentserver
```

### **Database Monitoring:**
```bash
# Connect to PostgreSQL
docker-compose exec postgres psql -U postgres -d eform_db

# List databases
docker-compose exec postgres psql -U postgres -c "\l"

# List tables
docker-compose exec postgres psql -U postgres -d eform_db -c "\dt"
```

## **Backup & Restore:**

### **Automated Backup:**
```bash
# Run backup script
./scripts/backup.sh

# Manual database backup
docker-compose exec postgres pg_dump -U postgres eform_db > backup.sql
```

### **Restore:**
```bash
# Restore from backup
./scripts/restore.sh backups/20241207_143000

# Manual restore
docker-compose exec -T postgres psql -U postgres eform_db < backup.sql
```

## **Success Verification:**

After setup, verify:

1. **OnlyOffice**: `http://localhost:8081/healthcheck` returns `{"status": "true"}`
2. **Backend**: `http://localhost:8080/api/v1/onlyoffice/test` returns success
3. **Frontend**: `http://localhost:3000` loads E-Form interface
4. **Integration**: Upload template → Click "Edit với OnlyOffice" → Editor opens
