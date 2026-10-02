# Architecture

## System shape

This is a monorepo with two separately deployable applications:

```text
Browser
  └── React single-page app (frontend)
        └── HTTPS JSON requests
              └── Spring Boot REST API (backend)
                    └── MySQL database
```

The frontend and backend can use separate hosting providers and therefore have separate public URLs. `VITE_API_BASE_URL` configures the API URL used by the browser app. The backend will allow requests only from configured frontend origins.

## Backend boundaries

Backend code will be grouped by feature. Each feature keeps its HTTP controller, request/response DTOs, business service, and persistence repository close together. Entities stay internal to persistence; API responses use DTOs. This prevents database details from becoming part of the public API.

## Frontend boundaries

The React app keeps account access and the signed-in finance workspace in feature folders, with API clients and styling separate. The workspace has distinct Overview, Transactions, Budgets, Savings goals, and Reports routes. Desktop uses a persistent sidebar, while mobile uses bottom navigation; browser back and forward are supported. The Transactions page supports search, date/type filters, server-side pagination, CSV export, category suggestions, and entry, editing, and deletion. Overview shows all-time totals and recent activity. Reports shows monthly cash-flow and category charts. Budgets tracks monthly category limits, and Savings goals tracks targets and progress. Forms provide labels, browser/server validation, loading states, and clear errors.

## Data and configuration

- MySQL is the persistent database.
- Connection details come from environment variables.
- Production schema changes will use versioned migrations instead of Hibernate schema updates.
- Frontend and backend URLs are configuration, not source-code constants.
- Passwords and tokens must never be committed.

## Authentication

The API hashes account passwords with Spring Security's PBKDF2 encoder and issues short-lived HMAC-signed bearer access tokens. Opaque refresh tokens are single-use, stored as SHA-256 hashes, rotated on refresh, and revoked by session family on logout or replay. The signing key is supplied as `JWT_SECRET` and must contain at least 32 random bytes after Base64 decoding. The browser client keeps both tokens in memory, sends access tokens in the `Authorization` header, and rotates an access token after a 401. Reloading ends the in-memory session.

## API conventions

- JSON REST endpoints use `/api/v1/...`.
- Request validation happens at the API boundary.
- Errors use a consistent JSON shape and appropriate HTTP status codes.
- Dates and money use explicit formats and decimal types.

## Deployment

Build and deploy `frontend/` as a static web app and `backend/` as a Java service, or use the included Docker Compose setup for a self-hosted install. Set the frontend API base URL and backend allowed-origin/database/JWT environment variables in their respective hosting services. See [Deployment](deployment.md) and the [OpenAPI contract](openapi.yaml).
