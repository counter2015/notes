#!/usr/bin/env bash
set -euo pipefail

DB_HOST="127.0.0.1"
DB_PORT="5432"
ADMIN_USER="postgres"
APP_DB="notes"
APP_USER="notes"
APP_PASSWORD="notes"

usage() {
  cat <<'EOF'
Usage:
  ./scripts/init-postgres.sh [options]

Options:
  --db-host <host>         PostgreSQL host (default: 127.0.0.1)
  --db-port <port>         PostgreSQL port (default: 5432)
  --admin-user <user>      Admin user for bootstrap (default: postgres)
  --app-db <name>          Application database name (default: notes)
  --app-user <user>        Application user name (default: notes)
  --app-password <pwd>     Application user password (default: notes)
  -h, --help               Show this help
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --db-host)
      DB_HOST="$2"
      shift 2
      ;;
    --db-port)
      DB_PORT="$2"
      shift 2
      ;;
    --admin-user)
      ADMIN_USER="$2"
      shift 2
      ;;
    --app-db)
      APP_DB="$2"
      shift 2
      ;;
    --app-user)
      APP_USER="$2"
      shift 2
      ;;
    --app-password)
      APP_PASSWORD="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage
      exit 1
      ;;
  esac
done

if ! command -v psql >/dev/null 2>&1; then
  echo "psql not found. Please install PostgreSQL client tools first." >&2
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SQL_FILE="$SCRIPT_DIR/init-postgres.sql"

echo "Initializing PostgreSQL (idempotent) ..."
echo "  host=$DB_HOST port=$DB_PORT admin=$ADMIN_USER app_db=$APP_DB app_user=$APP_USER"

psql \
  -v ON_ERROR_STOP=1 \
  -h "$DB_HOST" \
  -p "$DB_PORT" \
  -U "$ADMIN_USER" \
  -d postgres \
  -v "app_user=$APP_USER" \
  -v "app_password=$APP_PASSWORD" \
  -v "app_db=$APP_DB" \
  -f "$SQL_FILE"

echo "Done."
