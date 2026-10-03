# Deployment

## Docker Compose deployment

The repository includes a container build for the Java API and React static site, plus a MySQL Compose service for local and small self-hosted installations.

1. Install Docker Desktop or Docker Engine with the Compose plugin.
2. Copy `.env.example` to `.env` and replace the example database passwords.
3. Generate a signing key and put the printed value in `.env`:

   ```powershell
   $bytes = New-Object byte[] 32
   $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
   $rng.GetBytes($bytes)
   [Convert]::ToBase64String($bytes)
   $rng.Dispose()
   ```

   `JWT_SECRET` must decode to at least 32 random bytes. Use a different key for every environment. Do not commit `.env`.
4. For local Compose, keep `APP_CORS_ALLOWED_ORIGIN=http://localhost:5173` and `VITE_API_BASE_URL=http://localhost:8081/api/v1`.
5. From the repository root, run `docker compose up --build -d`.
6. Check `docker compose ps` and `docker compose logs backend`. The API health endpoint is `http://localhost:8081/actuator/health`; the app is `http://localhost:5173`.
7. Stop the services with `docker compose down`. The named `mysql_data` volume keeps data. To delete the database permanently, explicitly run `docker compose down -v`.

MySQL is not published on a host port. Flyway applies schema migrations when the API starts. Keep backups of the database volume or, for a production service, use a managed MySQL provider with automated backups and TLS.

## Production environment

For a public deployment, use a managed MySQL database and deploy the backend and frontend as separate services. Build the backend with `backend/Dockerfile`. Build the frontend with `frontend/Dockerfile` and set `VITE_API_BASE_URL` to the public API base URL **at build time**. Configure these backend variables in the hosting service:

For this Railway deployment, set the frontend build variable `VITE_API_BASE_URL` to `https://personalmoneymanagementjava-production-fb33.up.railway.app/api/v1`. Set the backend variable `APP_CORS_ALLOWED_ORIGIN` to the frontend's exact public origin, `https://personal-money-management-system-production-0260.up.railway.app` (no trailing slash or path). Rebuild/redeploy both services after changing these values.

| Variable | Requirement |
| --- | --- |
| `DB_URL` | Managed MySQL JDBC URL, including provider-required TLS options |
| `DB_USERNAME` / `DB_PASSWORD` | Dedicated least-privilege application database account |
| `JWT_SECRET` | Unique Base64-encoded key with at least 32 random bytes |
| `JWT_ISSUER` | Stable issuer name shared by this API instance |
| `APP_CORS_ALLOWED_ORIGIN` | Exact HTTPS origin of the frontend, without a path |
| `PORT` | Supplied by the platform, or use `8081` |
| `JWT_ACCESS_TOKEN_MINUTES` | Short-lived access-token lifetime; default `15` |
| `JWT_REFRESH_TOKEN_DAYS` | Refresh-session lifetime; default `30` |

Terminate HTTPS at the hosting platform or a reverse proxy. Do not expose MySQL publicly. Use a secret manager for credentials, restrict API origins, configure database backups and restore checks, and monitor `/actuator/health`. Apply edge rate limits to registration, login, refresh, and any future password-recovery endpoints. Never reuse the sample/local secrets in a public deployment.

The refresh endpoint rotates opaque refresh tokens. The React client keeps the latest pair in memory, refreshes after an access-token 401, and retries the original API call once. The server stores only a SHA-256 hash, revokes a token after use, and revokes that session family if a rotated token is replayed. Logout revokes the refresh-token family. Already issued access tokens remain valid until their short expiry (15 minutes by default). Reloading the browser clears the in-memory session; users sign in again.

## API reference

The checked-in OpenAPI 3.1 contract is [`openapi.yaml`](openapi.yaml). It can be opened in Swagger Editor or imported into Postman. The API does not expose an unauthenticated interactive documentation UI.
