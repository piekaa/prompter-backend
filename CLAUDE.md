# Prompter Backend — AI Agent Guidelines

## Project

- See [PROJECT.md](PROJECT.md) — overview, running instructions.

## Stack

- **Backend**: Spring Boot 3.x (Java 17+, Maven)
- **Frontend**: Plain HTML/CSS/JS (static resources served by Spring Boot)
- **Database**: PostgreSQL (via Docker Compose)
- **Containerization**: Docker + Docker Compose
- **Hot Reload**: Spring Boot DevTools (auto-restart Java, instant CSS/JS)

## Golden Rules

1. **After every task, the project must compile and run.** Never leave build broken.
2. **After every task, something must be viewable in browser.** Even if it's just a placeholder or debug page.
3. **For dev: use hot-reload workflow** (scripts/dev-start.sh). Never build/start the Docker stack on your own initiative — only when the user explicitly asks.
4. **Every code change must have matching tests.** Unit tests for business logic, integration tests for endpoints.
5. **Read existing code before writing.** Match style, naming, package structure, annotation patterns.

## Dev Workflow (Fast Iteration)

### Local Development with Hot Reload

**Normal dev day:**
1. Start DB in Docker: `docker compose up db -d`
2. Run app locally with dev profile + hot reload: `./scripts/dev-start.sh`
   - Java changes → auto-restart (~1-2s via DevTools)
   - CSS/JS/HTML changes → refresh browser, no restart needed (static files served directly from classpath)
3. Code freely — edits to `src/main/resources/static/*` are visible immediately on page refresh

### Docker (only when the user asks)

Do not build or start the full Docker stack unless explicitly asked (e.g. shipping, verifying production deploy, checking the final image):

```bash
docker compose up --build -d     # build + start full stack
docker compose down              # stop everything
```

### Dev Profile Details (`application-dev.yml`)

- Flyway migrations run normally (same as production)
- `ddl-auto` stays at default (`none`) — schema managed by Flyway
- `devtools.restart.enabled: true` — auto-restart on Java class changes
- DevTools only overrides what's needed for hot reload; everything else inherits from default profile

### Dev Scripts

```bash
./scripts/dev-start.sh           # start postgres + app with hot reload
./scripts/dev-stop.sh            # stop postgres, clean data
```

## Project Structure

```
prompter-backend/
├── CLAUDE.md              ← you are here
├── pom.xml                ← Maven (devtools included)
├── docker-compose.yml     ← starts app + postgres
├── .dockerignore
├── scripts/
│   ├── dev-start.sh       ← hot reload dev mode
│   ├── dev-stop.sh        ← cleanup
│   └── browser-explore.py ← visual page exploration (Playwright + Ollama vision)
└── src/main/java/com/prompter/
    ├── PrompterApp.java           ← @SpringBootApplication main class
    ├── config/                    ← SecurityConfig, WebConfig, DataInitializer
    ├── controller/                ← REST + page controllers
    ├── service/                   ← business logic
    ├── repository/                ← Spring Data JPA repos
    ├── model/                     ← JPA entities
    └── dto/                       ← request/response DTOs
└── src/main/resources/
    ├── application.yml            ← default profile
    ├── application-dev.yml        ← dev: hot reload only (inherits all other defaults)
    ├── application-docker.yml     ← docker: db host = "db" service name
    ├── db/migration/              ← Flyway migrations
    └── static/                    ← HTML/CSS/JS (served at /)
```

## Running the Project

### Local Dev (hot reload — use during development)

```bash
# 1. Start DB only
docker compose up db -d

# 2. Run app with dev profile + hot reload
./scripts/dev-start.sh       # one-liner: starts DB + runs mvn spring-boot:run
mvn spring-boot:run -Dspring-boot.run.profiles=dev   # alternative
```

### Docker (only when the user asks)

```bash
docker compose up --build -d     # build + start full stack
docker compose down              # stop everything
```

App serves at `http://localhost:8080`. Health check at `/actuator/health`.

## Build Process & Testing

```bash
mvn compile                    ← verify compiles (local Maven only)
mvn test                       ← run tests locally (local Maven only)
docker compose up --build -d   ← start full stack
```

**Before committing or saying "done": run `mvn compile` and `mvn test` locally. Must pass.**

## Frontend Rules

- Static files go in `src/main/resources/static/`
- Index at `static/index.html` — renders at `http://localhost:8080/`
- CSS in `static/css/` (load order: tokens.css → components.css → pages.css), JS in `static/js/` (core.js first)
- All API calls from frontend use relative paths (`/api/...`)
- No build step required for frontend. Plain files only.
- **During local dev (hot reload): changes visible immediately on browser refresh** — no app restart needed for static file changes

## Backend Rules

- Controllers return `String` (for page rendering) or JSON (REST endpoints)
- Entities have JPA annotations, Lombok where appropriate
- Repositories extend `JpaRepository<Entity, Long>`
- DTOs separate from entities — no entity leakage to API
- Validation on input: `@Valid` + `Validator` service
- Use Spring profiles for DB config (`application-docker.yml`)
- Security model: public pages + `/api/public/**` open; everything else (incl. `/backoffice/**`) requires form-login. New public API endpoints go under `/api/public/**` and must be added to `SecurityConfig`.

## Docker Rules

- PostgreSQL port: 5432 (mapped)
- App port: 8080 (mapped)
- DB name: `prompter`, user: `prompter`, password: `prompter`
- Use multi-stage Dockerfile: build with Maven, run with slim JRE image
- Health check in docker-compose for postgres dependency

## Testing Rules

- **Tests run via local Maven only** (`mvn test`). Docker image has no Maven — use `docker compose build app` for compilation only.
- Unit tests: test classes directly (`@SpringBootTest`, Mockito)
- Integration tests: test endpoints with `MockMvc` (not TestRestTemplate) — Spring Security form-login redirects unauthenticated requests to login page, breaking raw HTTP calls from TestRestTemplate in tests. Use `@AutoConfigureMockMvc` + `@WithMockUser`.
- Test DB via TestContainers PostgreSQL (profile `test`) with Flyway migrations from `src/main/resources/db/migration/`. Extend `IntegrationTestBase` for the container wiring.
- Every new feature gets at least one test covering happy path
- **During local dev: frontend changes visible without rebuild.**
- Only test changes in the browser when the user asks — then use Playwright MCP + screenshots with vision

## Browser Testing (only when the user asks)

- Use **Playwright MCP** (`browser_snapshot`, `browser_navigate`, `browser_click`, etc.) for DOM-based navigation
- Use **Playwright screenshot + your own vision capabilities (just open image file)** for pixel-level analysis
- Test public URLs at `http://localhost:8080/`
- Backoffice pages require login at `/login` (credentials: `admin`/`admin`)
- After opening a page, wait for 1 second before making a screenshot or validating any DOM elements or anything like it

## Commits

Use Conventional Commits format. Keep body only when "why" isn't obvious from the diff.
Do not commit screenshots taken during testing.

## Common Patterns to Follow

### New entity → repository → service → controller pipeline:
1. Create entity in `model/` with JPA annotations
2. Create Flyway migration in `db/migration/` (V{n}__name.sql)
3. Create repository in `repository/` extending JpaRepository
4. Create service in `service/` with business logic + tests
5. Create controller in `controller/` with endpoints
6. Update frontend if it affects user-visible output

### New static page:
1. Create HTML in `static/`
2. Optionally create controller returning the page name (if dynamic or auth-gated — forward pattern, see `PageController`)
3. Link from navigation or index
4. Verify renders at correct URL

## Browser Exploration

For visually exploring the app UI with qwen3.6 via Ollama, two tools are available:

**Quick start:**
```bash
# Headless (public pages):
python3 scripts/browser-explore.py "Describe the visual appearance — colors, layout, typography" --url http://localhost:8080/

# Headed (visible browser, requires display):
DISPLAY=:1 python3 scripts/browser-explore.py "Check if the layout looks good" --url http://localhost:8080/
```

please prefer Headed

**Known pitfalls:**
- **Blank screenshots**: Content renders via JS fetch after load. Always wait for DOM content (`browser_snapshot` or `wait_for_selector`) before capturing screenshot. Viewport-only screenshots miss below-fold content — use `fullPage: true`.
- **Local dev auth state preserved**: Session survives java hot-reload restarts (unlike docker rebuilds). No re-login needed after devtools auto-restart.
- **Screenshots show blank but DOM has content**: 90% viewport cutoff or stale auth. Cross-check with `browser_snapshot` (accessibility tree) — it shows real rendered content regardless of visual state.

### Option 1 — MCP browser tools (Claude Code native)

Playwright MCP server installed via `claude mcp add playwright npx @playwright/mcp@latest`.
Use directly in Claude Code sessions for reliable DOM-based navigation and interaction, example:

```
"Open http://localhost:8080 and take a screenshot"
"Navigate to localhost:8080, click login, fill the form"
"Extract all links from the current page"
```

MCP gives you `browser_snapshot` (accessibility tree), `browser_take_screenshot`, and DOM interaction. For pixel-level analysis of screenshots, use your own Vision. MCP snapshot gives structure, not rendered visuals.
