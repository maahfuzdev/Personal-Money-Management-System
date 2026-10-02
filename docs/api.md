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
| `/transactions` | Manage income and expenses | Implemented |
| `/budgets` | Set and review monthly spending budgets | Implemented |
| `/goals` | Track savings goals | Planned |
| `/dashboard` | Return summary totals and trends | Planned |

## Transactions

All transaction endpoints require `Authorization: Bearer <accessToken>`. Each request is scoped to the signed-in account; clients do not send a user ID. A transaction belonging to another account is reported as not found.

### List transactions

`GET /api/v1/transactions` returns the current user's transactions, newest transaction date first.

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
