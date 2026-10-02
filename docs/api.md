# API contract

## Base path

Application endpoints use `/api/v1`. The browser client receives the API base URL from `VITE_API_BASE_URL`; the backend port is configurable with `PORT`. The local default is `http://localhost:8081/api/v1`. The complete machine-readable contract is [OpenAPI 3.1](openapi.yaml).

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
| `/transactions` | Manage income and expenses | Implemented |
| `/budgets` | Set and review monthly spending budgets | Implemented |
| `/goals` | Track savings goals | Implemented |
| `/dashboard` | Return monthly totals and spending trends | Implemented |

## Transactions

All transaction endpoints require `Authorization: Bearer <accessToken>`. Each request is scoped to the signed-in account; clients do not send a user ID. A transaction belonging to another account is reported as not found.

### List transactions

`GET /api/v1/transactions?page=0&size=10&type=EXPENSE&search=food&startDate=2026-10-01&endDate=2026-10-31` returns one page of the current user's transactions, newest transaction date first. All query parameters are optional: `page` defaults to `0`, `size` defaults to `10` and is limited to `50`, `type` may be `INCOME` or `EXPENSE`, `search` matches category or note (up to 100 characters), and `startDate`/`endDate` filter inclusive ISO dates. Start date must not be after end date. Results always remain scoped to the signed-in account.

`GET /api/v1/transactions/export.csv` accepts the same search, type, and date filters and downloads all matching rows as UTF-8 CSV (up to 10,000 rows per export). It includes a spreadsheet-compatible BOM; user-entered text is escaped and guarded against spreadsheet formula execution.

If more than 10,000 rows match, the export endpoint returns `413 Content Too Large`; narrow the date range or other filters and retry.

`GET /api/v1/transactions/categories?type=EXPENSE` returns distinct category suggestions from the signed-in user's existing transactions. The optional type filter accepts `INCOME` or `EXPENSE`.

```json
{
  "items": [],
  "page": 0,
  "size": 10,
  "totalItems": 0,
  "totalPages": 0
}
```

### Create a transaction

`POST /api/v1/transactions` returns `201 Created`.

```json
{
  "type": "EXPENSE",
  "amount": 42.50,
  "category": "Food",
  "note": "Lunch",
  "transactionDate": "2026-10-02"
}
```

`type` must be `INCOME` or `EXPENSE`; amount must be positive and have at most two decimal places. Category is required (up to 60 characters), note is optional (up to 500 characters), and date uses `YYYY-MM-DD`.

### Update or delete

- `PUT /api/v1/transactions/{id}` accepts the same JSON as create and returns the updated transaction.
- `DELETE /api/v1/transactions/{id}` returns `204 No Content`.
- Missing or non-owned IDs return `404 Not Found`.

### Summary

`GET /api/v1/transactions/summary` returns all-time totals for the signed-in account:

```json
{
  "totalIncome": 2500.00,
  "totalExpense": 42.50,
  "balance": 2457.50
}
```

## Monthly budgets

All budget endpoints require a bearer access token and are scoped to the signed-in account. A category may have one budget per month; duplicate category/month pairs return `409 Conflict`. Spending totals include only `EXPENSE` transactions in that category and month (case-insensitive category match).

- `GET /api/v1/budgets?month=2026-10` lists budgets for a month. The `month` query is optional and defaults to the current month.
- `POST /api/v1/budgets` creates a budget with `category`, positive `monthlyLimit`, and `month` (`YYYY-MM`).
- `PUT /api/v1/budgets/{id}` replaces those budget fields.
- `DELETE /api/v1/budgets/{id}` returns `204 No Content`.

List and write responses include the calculated `spent` and `remaining` values for each category. Missing or non-owned IDs return `404 Not Found`.

```json
{
  "category": "Food",
  "monthlyLimit": 12000.00,
  "month": "2026-10"
}
```

## Savings goals

All goal endpoints require a bearer access token and only return goals belonging to the signed-in account.

- `GET /api/v1/goals` lists the account's goals.
- `POST /api/v1/goals` creates a goal.
- `PUT /api/v1/goals/{id}` updates a goal.
- `DELETE /api/v1/goals/{id}` removes a goal and returns `204 No Content`.

Goal requests contain a `name`, positive `targetAmount`, non-negative `currentAmount`, optional `targetDate` (`YYYY-MM-DD`), and optional `note`. Current savings cannot exceed the target. Responses also include `remainingAmount`, `completionPercent` (capped at 100), and `completed`.

```json
{
  "name": "Emergency fund",
  "targetAmount": 100000.00,
  "currentAmount": 25000.00,
  "targetDate": "2027-06-30",
  "note": "Build a six-month cushion"
}
```

## Dashboard analytics

`GET /api/v1/dashboard/analytics?month=2026-10` requires a bearer token. The `month` is optional and defaults to the current month. The response gives the selected month's income, expenses, and net balance; a six-month income/expense trend ending on the selected month; and that month's expense totals grouped by category. The endpoint only summarizes the signed-in account's data.

```json
{
  "selectedMonth": "2026-10",
  "monthIncome": 2500.00,
  "monthExpense": 420.00,
  "monthBalance": 2080.00,
  "monthlyTrend": [
    { "month": "2026-05", "income": 0.00, "expense": 0.00 },
    { "month": "2026-06", "income": 0.00, "expense": 0.00 },
    { "month": "2026-07", "income": 0.00, "expense": 0.00 },
    { "month": "2026-08", "income": 0.00, "expense": 0.00 },
    { "month": "2026-09", "income": 0.00, "expense": 0.00 },
    { "month": "2026-10", "income": 2500.00, "expense": 420.00 }
  ],
  "expenseByCategory": [
    { "category": "food", "amount": 420.00, "sharePercent": 100.0 }
  ]
}
```

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

Returns `201 Created` with the created user's public profile, a 15-minute bearer access token, and a 30-day opaque refresh token. Passwords must contain 12–128 characters. An existing email returns `409 Conflict`.

### Sign in

`POST /api/v1/auth/login` accepts `email` and `password`, then issues both tokens. Invalid credentials return the same generic `401 Unauthorized` message, whether the email or password was incorrect.

The client must keep tokens out of URLs. The React app keeps them in memory, sends the access token in the `Authorization` header, and automatically refreshes after a 401. The refresh token is opaque, random, and single-use. The API stores only its SHA-256 hash. `POST /api/v1/auth/refresh` rotates the supplied refresh token and returns a new pair; replaying a consumed token revokes its session family. `POST /api/v1/auth/logout` revokes the family and returns `204 No Content`. Unknown refresh tokens are accepted by logout to keep it idempotent. Already issued access tokens remain valid until expiry.

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
