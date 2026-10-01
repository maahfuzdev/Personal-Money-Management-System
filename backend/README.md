# Backend

Java 17+ and Spring Boot REST API. The backend owns validation, business rules, authentication, and persistence. It returns JSON and does not render web pages.

## Configuration

Set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET` in the process environment. `JWT_SECRET` must be Base64 text representing at least 32 random bytes. `APP_CORS_ALLOWED_ORIGIN` can override the local React origin when the UI is deployed. Local setup is in [Development setup](../docs/getting-started.md).

## Run on Windows

From this directory, run `.\mvnw.cmd spring-boot:run` after setting the database environment variables. The Maven Wrapper downloads and uses the project Maven version, so a global Maven install is not required.

On macOS or Linux, run `./mvnw spring-boot:run`.

## API contract

Endpoints are versioned under `/api/v1`. DTOs define the public request and response shapes; persistence entities are not returned directly.

## Authentication endpoints

- `POST /api/v1/auth/register` creates an account and returns a short-lived bearer token.
- `POST /api/v1/auth/login` checks credentials and returns a bearer token.
- `GET /api/v1/auth/me` returns the signed-in account; send `Authorization: Bearer <token>`.

Passwords are stored as PBKDF2 hashes. Access tokens expire after 15 minutes. The browser app should keep the token in memory and send it in the `Authorization` header; do not put it in a URL or commit it.

On startup, Flyway applies versioned scripts from `src/main/resources/db/migration`. The first script creates the `app_users` table; Hibernate validates the resulting schema and does not modify it.
