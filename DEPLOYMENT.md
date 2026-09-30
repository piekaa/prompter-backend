
# Deployment — Production at piekoszek.pl

## Prerequisites (server side) — all verified 2026-09-30

- [x] SSH key for `root@piekoszek.pl` (145.239.82.11) configured on this machine
- [x] Docker 24.0.3 + compose v2.19.1 installed
- [x] Nginx installed, main sites working
- [x] Certbot + `python3-certbot-nginx` installed, renewals dir active
- [x] DNS: `prompter.piekoszek.pl` → 145.239.82.11
- [x] Nginx server block for `prompter.piekoszek.pl` (HTTP :80 → proxy 127.0.0.1:8081 + ACME challenge location)
- [ ] **SSL certificate for `prompter.piekoszek.pl` — one-time `certbot --nginx` (see below)**

## Port Layout (important)

| Host port | What |
|-----------|------|
| 8080 | **Already taken** — main `piekoszek.pl` app (affiliate stack) |
| **8081** | prompter-backend (this project) — nginx proxies here |

`docker-compose.prod.yml` maps `8081:8080` (host 8081 → container 8080). Do not change to 8080.

## Quick Start

```bash
# One-time: get SSL cert for the new subdomain (nginx block already in place)
ssh root@piekoszek.pl "certbot --nginx -d prompter.piekoszek.pl"

# Deploy every time you push changes:
./scripts/deploy.sh root@piekoszek.pl          # Linux / macOS / Git Bash
```

**Windows (PowerShell)** — same logic, no bash dependency:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/deploy.ps1
```

## Deployment Script (`scripts/deploy.sh`)

Each run automates:
1. **Build** — `docker build` locally with existing Dockerfile
2. **Save + Transfer** — `docker save` → `scp` to server
3. **Load** — `docker load` on server, removes temp tarball
4. **Update compose file** — syncs `docker-compose.prod.yml` to server
5. **Restart** — `docker compose down && up -d`
6. **Verify** — polls `http://localhost:8081/actuator/health` until UP

## Server Layout

```
/etc/nginx/nginx.conf                     ← prompter.piekoszek.pl block (HTTP now, certbot adds :443)
/etc/letsencrypt/live/prompter.piekoszek.pl/  ← certbot certs (created on first certbot run)
/var/www/certbot                          ← ACME challenge webroot (already in nginx block)
/var/lib/docker/volumes/prompter-app_pgdata ← PostgreSQL data (compose project name = dir name)
/opt/prompter-app/
└── docker-compose.prod.yml               ← managed by deploy.sh
```

**Data isolation:** compose project name derives from the directory (`/opt/prompter-app` → volume `prompter-app_pgdata`), so it never collides with the affiliate stack's `pgdata` volume. First deploy = fresh DB; Flyway migrations build the schema.

## Architecture

```
                  ┌──────────────┐
  HTTPS:443 ──►  │   Nginx      │  prompter.piekoszek.pl (shared with other sites)
                  │   + Certbot  │  ← SSL termination, reverse proxy
                  └──────┬───────┘
                         │ http://127.0.0.1:8081
                  ┌──────▼───────┐
                  │  App (Docker)│  Spring Boot :8080 in container
                  │              │
                  │   DB (Docker)│  PostgreSQL 16 (volume prompter-app_pgdata)
                  └──────────────┘
```

## One-Time SSL Setup

The nginx block (already on the server) is intentionally **HTTP-only** — certbot's nginx plugin adds the `:443` listener and cert paths itself. Do not add SSL lines by hand:

```bash
# 1. Ensure DNS is live (verified):
dig +short prompter.piekoszek.pl    # → 145.239.82.11

# 2. Get cert — certbot rewrites the block, adds # managed by Certbot lines:
ssh root@piekoszek.pl "certbot --nginx -d prompter.piekoszek.pl"

# 3. Reload + verify
ssh root@piekoszek.pl "nginx -t && nginx -s reload"
curl -sI https://prompter.piekoszek.pl
```

Renewal: joins the standard certbot renewal cycle automatically (system timer/cron already active on this box — existing certs renew fine).

## Nginx Block (as deployed 2026-09-30)

```nginx
server {
    listen 80;
    server_name prompter.piekoszek.pl;

    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }

    location / {
        proxy_pass         http://127.0.0.1:8081;
        proxy_set_header   Host $host;
        proxy_set_header   X-Real-IP $remote_addr;
        proxy_set_header   X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header   X-Forwarded-Proto $scheme;
    }
}
```

## Manual Troubleshooting

### Check app logs

```bash
ssh root@piekoszek.pl "docker compose -f /opt/prompter-app/docker-compose.prod.yml logs -f app"
```

### Restart manually on server

```bash
ssh root@piekoszek.pl "cd /opt/prompter-app && docker compose -f docker-compose.prod.yml restart"
```

### Check SSL expiry

```bash
ssh root@piekoszek.pl "openssl x509 -enddate -noout -in /etc/letsencrypt/live/prompter.piekoszek.pl/fullchain.pem"
```

### Force cert renewal test

```bash
ssh root@piekoszek.pl "certbot renew --dry-run"
```

### Verify nginx → app path without touching DNS

```bash
ssh root@piekoszek.pl "curl -s http://localhost:8081/actuator/health"
```

## Current State — LIVE (2026-09-30)

- [x] nginx block deployed + reloaded
- [x] `docker-compose.prod.yml` fixed to 8081
- [x] `scripts/deploy.sh` (Linux) + `scripts/deploy.ps1` (Windows) created
- [x] first deploy run — stack in `/opt/prompter-app`, db healthy, app UP
- [x] `certbot --nginx -d prompter.piekoszek.pl` — cert valid until 2026-12-29, auto-renew scheduled
- [x] `https://prompter.piekoszek.pl` → 200, `/actuator/health` = UP
- [x] pre-deploy backup of affiliate postgres: server `/opt/db-backups/affiliate-pg-dump.sql` + local `C:\Users\Piotr\backups\piekoszek-2026-09-30\`
