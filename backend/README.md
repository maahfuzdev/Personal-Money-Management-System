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

- `POST /api/v1/auth/register` creates an account and returns short-lived access and rotating refresh tokens.
- `POST /api/v1/auth/login` checks credentials and returns access and refresh tokens.
- `POST /api/v1/auth/refresh` accepts `{ "refreshToken": "..." }`, revokes that token, and returns a replacement pair.
- `POST /api/v1/auth/logout` accepts `{ "refreshToken": "..." }` and revokes its session family. It is idempotent.
- `GET /api/v1/auth/me` returns the signed-in account; send `Authorization: Bearer <token>`.

Passwords are stored as PBKDF2 hashes. Access tokens expire after 15 minutes by default. Refresh tokens are random opaque values; only their SHA-256 hashes are stored. Each refresh token is single-use. Replaying a consumed token revokes the related session family. Logout revokes the refresh family, while issued access tokens remain valid until expiry. Keep tokens out of URLs and source control.

On startup, Flyway applies versioned scripts from `src/main/resources/db/migration`. The first script creates the `app_users` table; Hibernate validates the resulting schema and does not modify it.

The complete endpoint contract is in [OpenAPI](../docs/openapi.yaml). Container and production environment setup is documented in [Deployment](../docs/deployment.md).
