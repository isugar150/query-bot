# Repository Guidelines

## Project Structure & Modules
- `backend/`: Spring Boot API (auth, DB metadata, chat, query execution). Config in `src/main/resources/application.yaml`; code under `src/main/java`. Tests in `backend/src/test/java`.
- `frontend/`: React 19 + Chakra UI + Zustand. Entry in `src/main.tsx`, pages in `src/pages`, shared UI in `src/components`, API clients in `src/api`, state in `src/store`, utilities in `src/utils`.
- `docker-compose.yml`, `Dockerfile`, `start.sh`, `nginx.conf`: containerized full stack served on port 5213. `.env.example` lists environment variables.

## Build, Test, Run
- Backend: `cd backend && ./gradlew test` (unit tests), `./gradlew bootRun` (run API on port 8080; override with `--args='--server.port=8090'`).
- Frontend: `cd frontend && npm install` (once), `npm run dev` (Vite dev server), `npm run build` (type-check + production build).
- Docker: `docker compose up --build` (build and run backend+frontend+nginx). Configure via `.env`.

## Coding Style & Naming
- Backend: Java 17, Spring Boot. Prefer constructor injection, package `com.namejm.query_bot`. Use Lombok-free POJOs; keep DTOs as records. Follow existing casing (`camelCase` fields, `PascalCase` classes).
- Frontend: TypeScript, React function components. Use Chakra UI components; avoid `any` and prefer typed DTOs from `src/types`. Keep state in hooks/Zustand slices; API calls in `src/api/*`. Styling via Chakra props; minimal custom CSS.
- Formatting: Default project formatters (Gradle/Prettier not enforced here). Use 2 spaces in JSX/TS, Java default indentation.

## Testing Guidelines
- Backend tests: JUnit via `./gradlew test`. Add new tests under `backend/src/test/java` mirroring package structure; name methods `should...`.
- Frontend: No test harness configured; ensure `npm run build` passes. If adding tests, use vitest/react-testing-library and document commands.
- Validate critical flows (init/login/chat/DB metadata) with local runs or curl examples.

## Git Commit & PR Expectations
- Commits: Use clear, imperative messages (e.g., “Add DB refresh endpoint”, “Fix chat session selection”). Group related changes; avoid committing secrets.
- PRs: Include summary, testing steps (`gradlew test`, `npm run build`, manual checks), screenshots/GIFs for UI changes, and reference related issues. Note any env vars or migration steps (`APP_DATA_DIR`, `OPENAI_API_KEY`, `JWT_SECRET`).

## Security & Config Notes
- Secrets: set `JWT_SECRET`, `OPENAI_API_KEY`, and `APP_DATA_DIR` via env/.env; never commit real keys. Default dev secrets are placeholders only.
- Data: SQLite lives at `APP_DATA_DIR/querybot.db` (default `./data` or `/app/data` in Docker). Schema refresh via `/api/db/refresh/{id}`.
