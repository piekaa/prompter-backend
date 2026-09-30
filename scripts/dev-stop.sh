#!/usr/bin/env bash
# Stop local dev, clean database
set -euo pipefail
cd "$(dirname "$0")/.."

echo "Stopping PostgreSQL and removing data..."
docker compose down -v
echo "Done. Run scripts/dev-start.sh to start fresh."
