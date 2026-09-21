#!/usr/bin/env bash
# One-time cutover: copy data from the shared remote Binar DB into the new local Postgres
# container defined in docker-compose.yml. Run this ONCE, by hand, on the GCP VM, in
# ~/sakuku (same folder as docker-compose.yml + .env). NOT wired into CI on purpose - a DB
# restore is destructive to whatever's in the target and should never run on every push.
#
# Order matters:
#   1. Bring up ONLY the empty "db" container
#   2. Dump the remote DB, restore into it
#   3. Sanity-check row counts
#   4. Only THEN start/restart "backend" pointed at the local db
set -euo pipefail

cd "$(dirname "$0")/.."   # repo root (where docker-compose.yml + .env live)

if [ ! -f .env ]; then
  echo "No .env found here - copy your existing one into this folder first." >&2
  exit 1
fi
set -a; source .env; set +a

REMOTE_HOST="129.226.195.9"
REMOTE_DB="binar_finance"
DUMP_FILE="binar_finance_$(date +%Y%m%d_%H%M%S).sql"

echo "== 1. Starting empty local db container =="
docker compose up -d db
until docker exec sakuku-db pg_isready -U "$DB_USERNAME" -d "$REMOTE_DB" >/dev/null 2>&1; do
  echo "  waiting for local db to accept connections..."
  sleep 2
done

echo "== 2. Dumping remote DB ($REMOTE_HOST) to $DUMP_FILE =="
docker run --rm -e PGPASSWORD="$DB_PASSWORD" postgres:16-alpine \
  pg_dump -h "$REMOTE_HOST" -p 5432 -U "$DB_USERNAME" -d "$REMOTE_DB" > "$DUMP_FILE"
echo "  dump size: $(du -h "$DUMP_FILE" | cut -f1)"

echo "== 3. Restoring into local db container =="
docker exec -i sakuku-db psql -U "$DB_USERNAME" -d "$REMOTE_DB" < "$DUMP_FILE"

echo "== 4. Sanity check - row counts (adjust schema name if not 'vili') =="
docker exec sakuku-db psql -U "$DB_USERNAME" -d "$REMOTE_DB" -c "
  SELECT 'tbl_customer' AS tbl, count(*) FROM vili.tbl_customer
  UNION ALL SELECT 'tbl_user', count(*) FROM vili.tbl_user
  UNION ALL SELECT 'tbl_pengajuan', count(*) FROM vili.tbl_pengajuan
  UNION ALL SELECT 'tbl_plafond', count(*) FROM vili.tbl_plafond;
"

echo ""
echo "== Compare the counts above against the remote DB before continuing. =="
echo "If they match, bring up the backend against the local db:"
echo "    docker compose up -d --no-deps --force-recreate backend"
echo ""
echo "Dump file kept at: $DUMP_FILE (delete it once you've confirmed the migration worked -"
echo "it may contain real customer data, don't leave it lying around longer than needed)."
