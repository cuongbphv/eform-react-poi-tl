#!/bin/bash
# Backup script for databases and files

BACKUP_DIR="backups/$(date +%Y%m%d_%H%M%S)"
mkdir -p $BACKUP_DIR

echo "Creating backup at $BACKUP_DIR..."

# Backup PostgreSQL
echo "Backing up PostgreSQL..."
docker-compose exec -T postgres pg_dumpall -U postgres > $BACKUP_DIR/postgres_backup.sql

# Backup uploads
echo "Backing up uploads..."
docker-compose exec -T eform-backend tar -czf - /app/uploads > $BACKUP_DIR/uploads_backup.tar.gz

# Backup OnlyOffice data
echo "Backing up OnlyOffice data..."
docker-compose exec -T onlyoffice-documentserver tar -czf - /var/www/onlyoffice/Data > $BACKUP_DIR/onlyoffice_backup.tar.gz

echo "Backup completed at $BACKUP_DIR"