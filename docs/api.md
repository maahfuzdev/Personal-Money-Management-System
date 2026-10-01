# API contract

## Base path

All application endpoints will use `/api/v1`. The browser client receives the API base URL from `VITE_API_BASE_URL`; the backend port is configurable with `PORT`. The local default is `http://localhost:8080/api/v1`.

## Response rules

- Successful reads and writes return JSON unless the HTTP method/status has no response body.
- Invalid input returns `400` with field-level validation details.
- Missing resources return `404`.
- Unexpected errors return a generic `500` response without stack traces or database details.
- Monetary values use decimal JSON numbers backed by Java `BigDecimal`.
- Dates use ISO-8601 (`YYYY-MM-DD`).

## Planned resources

| Resource | Purpose | Status |
| --- | --- | --- |
| `/auth` | Register and sign in | Implemented |
| `/transactions` | Create and browse income and expenses | Planned |
| `/budgets` | Set and review spending budgets | Planned |
| `/goals` | Track savings goals | Planned |
| `/dashboard` | Return summary totals and trends | Planned |

The contract will be updated with concrete request and response examples as each feature is implemented.

## Authentication

### Register

`POST /api/v1/auth/register`

```json
{
  "name": "Amina Rahman",
  "email": "amina@example.com",
  "password": "a-long-unique-passphrase"
}
```

Returns `201 Created` with the created user's public profile and a 15-minute bearer access token. Passwords must contain 12–128 characters. An existing email returns `409 Conflict`.

### Sign in

`POST /api/v1/auth/login` accepts `email` and `password`. Invalid credentials return the same generic `401 Unauthorized` message, whether the email or password was incorrect.

The React authentication screen calls these endpoints directly. It keeps the access token in memory for the current page session and sends it only in the `Authorization` header.

### Current account

`GET /api/v1/auth/me` requires `Authorization: Bearer <accessToken>` and returns the public profile for that token's subject.

Validation errors use this shape:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Please check the submitted fields.",
  "fieldErrors": {
    "email": "Enter a valid email address"
  }
}
```
