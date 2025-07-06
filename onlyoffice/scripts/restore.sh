#!/bin/bash
# Restore script

if [ -z "$1" ]; then
    echo "Usage: $0 <backup_directory>"
    exit 1
fi

BACKUP_DIR=$1

if [ ! -d "$BACKUP_DIR" ]; then
    echo "Backup directory $BACKUP_DIR does not exist"
    exit 1
fi

echo "Restoring from $BACKUP_DIR..."

# Restore PostgreSQL
if [ -f "$BACKUP_DIR/postgres_backup.sql" ]; then
    echo "Restoring PostgreSQL..."
    docker-compose exec -T postgres psql -U postgres < $BACKUP_DIR/postgres_backup.sql
fi

# Restore uploads
if [ -f "$BACKUP_DIR/uploads_backup.tar.gz" ]; then
    echo "Restoring uploads..."
    cat $BACKUP_DIR/uploads_backup.tar.gz | docker-compose exec -T eform-backend tar -xzf - -C /
fi

# Restore OnlyOffice data
if [ -f "$BACKUP_DIR/onlyoffice_backup.tar.gz" ]; then
    echo "Restoring OnlyOffice data..."
    cat $BACKUP_DIR/onlyoffice_backup.tar.gz | docker-compose exec -T onlyoffice-documentserver tar -xzf - -C /
fi

echo "Restore completed"