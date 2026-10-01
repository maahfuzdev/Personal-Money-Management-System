# API contract

## Base path

All application endpoints will use `/api/v1`. The browser client receives the API origin from `VITE_API_BASE_URL`; the backend port is configurable with `PORT`.

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
| `/auth` | Register and sign in | Planned |
| `/transactions` | Create and browse income and expenses | Planned |
| `/budgets` | Set and review spending budgets | Planned |
| `/goals` | Track savings goals | Planned |
| `/dashboard` | Return summary totals and trends | Planned |

The contract will be updated with concrete request and response examples as each feature is implemented.
