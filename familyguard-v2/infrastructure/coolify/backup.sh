#!/bin/bash
# FamilyGuard 2.0 - Automated PostgreSQL Backup Script
# Place in crontab: 0 3 * * * /path/to/backup.sh

set -e

BACKUP_DIR="/var/backups/familyguard_v2"
DATE=$(date +"%Y%m%d_%H%M%S")
FILENAME="${BACKUP_DIR}/fg2_backup_${DATE}.sql.gz"

mkdir -p "${BACKUP_DIR}"

echo "[$(date)] Iniciando copia de seguridad de PostgreSQL FamilyGuard 2.0..."

docker exec fg2_postgres pg_dump -U fg_admin -d familyguard_prod | gzip > "${FILENAME}"

echo "[$(date)] Copia de seguridad guardada con éxito en: ${FILENAME}"

# Delete backups older than 14 days
find "${BACKUP_DIR}" -type f -name "fg2_backup_*.sql.gz" -mtime +14 -exec rm {} \;

echo "[$(date)] Limpieza de respaldos antiguos completada."
