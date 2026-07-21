#!/bin/bash
# Backup MariaDB -> ~/backups/ (giu toi da 5 ban)
set -e
DB_NAME="${1:-nro1}"
BACKUP_DIR="$HOME/backups"
TS=$(date +%Y%m%d_%H%M%S)
OUT="$BACKUP_DIR/${DB_NAME}_${TS}.sql.gz"
mkdir -p "$BACKUP_DIR"
echo "Backup $DB_NAME -> $OUT"
mysqldump -u root --single-transaction --quick "$DB_NAME" 2>/dev/null | gzip > "$OUT"
cp "$OUT" "$BACKUP_DIR/${DB_NAME}_latest.sql.gz"
echo "Done: $(ls -lh $OUT | awk '{print $5}')"
ls -t "$BACKUP_DIR/${DB_NAME}_"*.sql.gz 2>/dev/null | tail -n +6 | xargs rm -f 2>/dev/null || true
echo "Backups: $(ls $BACKUP_DIR/${DB_NAME}_*.sql.gz 2>/dev/null | wc -l) files"
