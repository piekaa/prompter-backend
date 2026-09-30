# Project Overview

## What it is

**Prompter Backend** — a fresh Spring Boot backend with the full dev setup ready: hot-reload workflow, Docker stack, Spring Security form-login backoffice, static HTML/CSS/JS frontend with a shared theme + UI kit, and a TestContainers-based test suite.

No domain logic yet — this is the shell to build the prompter domain on.

## Tech Stack

| Layer | Choice |
|-------|--------|
| Backend | Spring Boot 3.2, Java 17+, Maven |
| DB | PostgreSQL (Docker), PostgreSQL via TestContainers (tests) |
| Frontend | Static HTML/CSS/JS (tokens → components CSS, `window.Aff` core utils) |
| Auth | Spring Security form-login (backoffice only) |
| Infra | Docker Compose (app + postgres) |

## What's set up

- `SecurityConfig` — form-login at `/login.html`, `/backoffice/**` + non-public APIs require auth, `/api/public/**` convention for open endpoints
- `DataInitializer` — seeds `admin` / `admin` on startup
- `WebConfig` — `currentUser` request attribute for authenticated requests
- `users` table (Flyway `V1__init.sql`) + `User` entity + `UserDetailsServiceImpl`
- Static shell: `index.html` (public), `login.html`, `backoffice.html` (auth-gated)
- Theme: `css/tokens.css` (design tokens) + `css/components.css` (UI kit) + `js/core.js` (utils: fetchJSON, toast, confirm, modals, form errors)
- `IntegrationTestBase` — TestContainers PostgreSQL + Flyway for all integration tests

## Conventions

- No templating engine. All pages served from `src/main/resources/static/`; controllers `forward:` to the HTML file.
- Schema managed by Flyway only (`ddl-auto: none`). New tables → new migration file.
- Public API endpoints go under `/api/public/**` (open by security config).
- Auth-gated pages: map in `PageController` → forward to static HTML under a path matched by security rules.

## Running

### Local Dev (fast iteration — use during development)

```bash
docker compose up db -d          # start postgres only
./scripts/dev-start.sh           # start app with dev profile + hot reload
# or: mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

**Hot reload:** Java changes → auto-restart (~1-2s). CSS/JS/HTML → visible on browser refresh.

### Production Deploy (use when ready to ship)

```bash
docker compose up --build -d     # full stack, localhost:8080
docker compose down              # stop everything
```

### Cleanup

```bash
./scripts/dev-stop.sh            # stop postgres, remove all data
```

Admin login: `admin` / `admin` (set by `DataInitializer`).

## File Layout

```
prompter-backend/
├── CLAUDE.md              ← agent guidelines
├── PROJECT.md             ← you are here
├── pom.xml
├── docker-compose.yml / docker-compose.prod.yml / Dockerfile
├── scripts/               ← dev-start.sh, dev-stop.sh, browser-explore.py
└── src/
    ├── main/java/com/prompter/
    │   ├── PrompterApp.java
    │   ├── config/        ← SecurityConfig, WebConfig, DataInitializer
    │   ├── controller/    ← PageController
    │   ├── service/       ← UserDetailsServiceImpl
    │   ├── repository/    ← UserRepository
    │   └── model/         ← User
    ├── main/resources/
    │   ├── application.yml / application-dev.yml / application-docker.yml
    │   ├── db/migration/  ← V1__init.sql
    │   └── static/        ← index.html, login.html, backoffice.html, css/, js/
    └── test/
        ├── java/com/prompter/   ← IntegrationTestBase, controller/
        └── resources/           ← application-test.yml
```
