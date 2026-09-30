#!/usr/bin/env bash
# Deploy prompter-backend to production — build, scp image, restart compose stack
set -euo pipefail

HOST="${1:-root@piekoszek.pl}"
APP_DIR="/opt/prompter-app"
IMAGE_NAME="prompter-backend"
IMAGE_TAG="latest"
HOST_PORT=8081   # host port on server — nginx proxies prompter.piekoszek.pl → 127.0.0.1:8081
BUILD_CONTEXT="$(dirname "$0")/.."

TEMP_TAR=$(mktemp /tmp/prompter-backend-docker-XXXXX.tar)
trap 'rm -f "$TEMP_TAR"' EXIT

echo "=== Building Docker image ==="
docker build --tag "${IMAGE_NAME}:${IMAGE_TAG}" "${BUILD_CONTEXT}"

echo ""
echo "=== Saving image to tarball ==="
docker save -o "$TEMP_TAR" "${IMAGE_NAME}:${IMAGE_TAG}"

echo ""
echo "=== Sending image to server ($HOST) ==="
ssh "$HOST" "mkdir -p $APP_DIR"
scp "$TEMP_TAR" "${HOST}:${APP_DIR}/image.tar"

echo ""
echo "=== Loading image on server ==="
ssh "$HOST" "docker load -i ${APP_DIR}/image.tar && rm -f ${APP_DIR}/image.tar"

echo ""
echo "=== Updating compose file ==="
scp "${BUILD_CONTEXT}/docker-compose.prod.yml" "${HOST}:${APP_DIR}/docker-compose.prod.yml"

echo ""
echo "=== Restarting stack ==="
ssh "$HOST" "cd $APP_DIR && docker compose -f docker-compose.prod.yml down"
ssh "$HOST" "cd $APP_DIR && docker compose -f docker-compose.prod.yml up -d"

echo ""
echo "=== Waiting for health check ==="
for i in $(seq 1 60); do
  HEALTH=$(ssh "$HOST" "curl -sf http://localhost:${HOST_PORT}/actuator/health" 2>/dev/null | grep -o '"status":"[^"]*"' || true)
  if echo "$HEALTH" | grep -q '"status":"UP"'; then
    echo "App is UP!"
    break
  fi
  echo "  waiting... ($i)"
  sleep 2
done

echo ""
echo "=== Done ==="
