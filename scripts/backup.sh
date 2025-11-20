#!/bin/sh
# PostgreSQL Backup Script with GPG Encryption
# Usage: Run as cron job in Docker container

set -e

# Configuration from environment variables
POSTGRES_HOST="${POSTGRES_HOST:-postgres}"
POSTGRES_PORT="${POSTGRES_PORT:-5432}"
POSTGRES_DB="${POSTGRES_DB:-stockstatus}"
POSTGRES_USER="${POSTGRES_USER:-stockstatus}"
BACKUP_DIR="${BACKUP_DIR:-/backups}"
BACKUP_RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-14}"
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/backup_${POSTGRES_DB}_${DATE}.sql"
BACKUP_FILE_GZ="${BACKUP_FILE}.gz"

echo "[$(date)] Starting backup of database: ${POSTGRES_DB}"

# Create backup directory if it doesn't exist
mkdir -p "${BACKUP_DIR}"

# Perform database dump
echo "[$(date)] Creating database dump..."
PGPASSWORD="${POSTGRES_PASSWORD}" pg_dump \
    -h "${POSTGRES_HOST}" \
    -p "${POSTGRES_PORT}" \
    -U "${POSTGRES_USER}" \
    -d "${POSTGRES_DB}" \
    -F p \
    --no-owner \
    --no-acl \
    -f "${BACKUP_FILE}"

if [ $? -ne 0 ]; then
    echo "[$(date)] ERROR: Database dump failed"
    exit 1
fi

# Compress backup
echo "[$(date)] Compressing backup..."
gzip "${BACKUP_FILE}"

if [ $? -ne 0 ]; then
    echo "[$(date)] ERROR: Compression failed"
    exit 1
fi

# Optional: Encrypt with GPG (requires GPG key to be configured)
# Uncomment the following lines if you have GPG configured
# if [ -n "${BACKUP_ENCRYPTION_KEY}" ]; then
#     echo "[$(date)] Encrypting backup..."
#     gpg --encrypt --recipient "${BACKUP_ENCRYPTION_KEY}" "${BACKUP_FILE_GZ}"
#     rm "${BACKUP_FILE_GZ}"
#     BACKUP_FILE_GZ="${BACKUP_FILE_GZ}.gpg"
# fi

# Get backup file size
BACKUP_SIZE=$(du -h "${BACKUP_FILE_GZ}" | cut -f1)
echo "[$(date)] Backup created: ${BACKUP_FILE_GZ} (${BACKUP_SIZE})"

# Clean up old backups
echo "[$(date)] Cleaning up backups older than ${BACKUP_RETENTION_DAYS} days..."
find "${BACKUP_DIR}" -name "backup_${POSTGRES_DB}_*.sql.gz*" -type f -mtime +${BACKUP_RETENTION_DAYS} -delete

# Count remaining backups
BACKUP_COUNT=$(find "${BACKUP_DIR}" -name "backup_${POSTGRES_DB}_*.sql.gz*" -type f | wc -l)
echo "[$(date)] Backup completed successfully. Total backups: ${BACKUP_COUNT}"

# Optional: Send notification (implement as needed)
# curl -X POST "${NOTIFICATION_WEBHOOK}" -d "Backup completed: ${BACKUP_FILE_GZ}"

exit 0
