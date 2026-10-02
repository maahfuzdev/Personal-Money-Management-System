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

The React app keeps account access and the signed-in dashboard in feature folders, with API clients and styling separate. The dashboard supports responsive transaction list, filters, entry, editing, deletion, all-time totals, monthly category budgets, and savings goals with progress. Forms provide labels, browser/server validation, loading states, and clear errors.

## Data and configuration

- MySQL is the persistent database.
- Connection details come from environment variables.
- Production schema changes will use versioned migrations instead of Hibernate schema updates.
- Frontend and backend URLs are configuration, not source-code constants.
- Passwords and tokens must never be committed.

## Authentication

The API hashes account passwords with Spring Security's PBKDF2 encoder and issues 15-minute HMAC-signed bearer access tokens. The signing key is supplied as `JWT_SECRET` and must contain at least 32 random bytes after Base64 decoding. The React app keeps access tokens in memory and sends them in the `Authorization` header. Refresh-token support is a separate follow-up before production use.

## API conventions

- JSON REST endpoints use `/api/v1/...`.
- Request validation happens at the API boundary.
- Errors use a consistent JSON shape and appropriate HTTP status codes.
- Dates and money use explicit formats and decimal types.

## Deployment

Build and deploy `frontend/` as a static web app and `backend/` as a Java service. Set the frontend API base URL and backend allowed-origin/database environment variables in their respective hosting services. Deployment steps will be documented when the app's runtime configuration is in place.
