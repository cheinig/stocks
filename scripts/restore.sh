#!/bin/sh
# PostgreSQL Restore Script
# Usage: ./restore.sh <backup_file.sql.gz>

set -e

if [ -z "$1" ]; then
    echo "Usage: $0 <backup_file.sql.gz>"
    echo "Example: $0 /backups/backup_stockstatus_20250120_020000.sql.gz"
    exit 1
fi

BACKUP_FILE="$1"
POSTGRES_HOST="${POSTGRES_HOST:-postgres}"
POSTGRES_PORT="${POSTGRES_PORT:-5432}"
POSTGRES_DB="${POSTGRES_DB:-stockstatus}"
POSTGRES_USER="${POSTGRES_USER:-stockstatus}"

if [ ! -f "${BACKUP_FILE}" ]; then
    echo "ERROR: Backup file not found: ${BACKUP_FILE}"
    exit 1
fi

echo "[$(date)] Starting restore from: ${BACKUP_FILE}"

# Decrypt if encrypted (GPG)
# if [[ "${BACKUP_FILE}" == *.gpg ]]; then
#     echo "[$(date)] Decrypting backup..."
#     gpg --decrypt "${BACKUP_FILE}" > "${BACKUP_FILE%.gpg}"
#     BACKUP_FILE="${BACKUP_FILE%.gpg}"
# fi

# Decompress if compressed
if [[ "${BACKUP_FILE}" == *.gz ]]; then
    echo "[$(date)] Decompressing backup..."
    gunzip -c "${BACKUP_FILE}" > /tmp/restore_temp.sql
    RESTORE_FILE="/tmp/restore_temp.sql"
else
    RESTORE_FILE="${BACKUP_FILE}"
fi

# Warning
echo "WARNING: This will drop and recreate the database: ${POSTGRES_DB}"
echo "Press CTRL+C within 10 seconds to cancel..."
sleep 10

# Drop and recreate database
echo "[$(date)] Dropping existing database..."
PGPASSWORD="${POSTGRES_PASSWORD}" psql \
    -h "${POSTGRES_HOST}" \
    -p "${POSTGRES_PORT}" \
    -U "${POSTGRES_USER}" \
    -d postgres \
    -c "DROP DATABASE IF EXISTS ${POSTGRES_DB};"

echo "[$(date)] Creating new database..."
PGPASSWORD="${POSTGRES_PASSWORD}" psql \
    -h "${POSTGRES_HOST}" \
    -p "${POSTGRES_PORT}" \
    -U "${POSTGRES_USER}" \
    -d postgres \
    -c "CREATE DATABASE ${POSTGRES_DB};"

# Restore database
echo "[$(date)] Restoring database..."
PGPASSWORD="${POSTGRES_PASSWORD}" psql \
    -h "${POSTGRES_HOST}" \
    -p "${POSTGRES_PORT}" \
    -U "${POSTGRES_USER}" \
    -d "${POSTGRES_DB}" \
    -f "${RESTORE_FILE}"

if [ $? -ne 0 ]; then
    echo "[$(date)] ERROR: Restore failed"
    exit 1
fi

# Clean up temp file
if [ "${RESTORE_FILE}" = "/tmp/restore_temp.sql" ]; then
    rm -f /tmp/restore_temp.sql
fi

echo "[$(date)] Restore completed successfully"
exit 0
