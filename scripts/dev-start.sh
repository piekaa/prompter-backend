#!/usr/bin/env bash
# Start local dev: postgres in docker, java app from console with hot reload
set -euo pipefail
cd "$(dirname "$0")/.."

echo "=== Starting PostgreSQL ==="
docker compose up db -d 2>/dev/null || true

echo ""
echo "=== Waiting for database ==="
for i in $(seq 1 30); do
  if docker exec prompter-backend-db-1 pg_isready -U prompter >/dev/null 2>&1; then
    echo "DB ready!"
    break
  fi
  echo "  waiting... ($i)"
  sleep 1
done

echo ""
echo "=== Starting Spring Boot with dev profile (hot reload enabled, Flyway + docker DB) ==="
echo "Java changes: auto-restart (~1s)"
echo "CSS/JS/HTML: refresh browser — no restart needed"
echo ""
mvn spring-boot:run -Dspring-boot.run.profiles=dev
