# Frontend

React single-page app built with Vite. It is designed to deploy independently from the Java backend.

## Requirements

Node.js 22.12 or newer and npm.

## Run

```bash
npm install
npm run dev
```

Copy `.env.example` to `.env.local` for local development, then set `VITE_API_BASE_URL` to the backend API base URL. Vite exposes `VITE_` variables to browser code, so never put secrets in them. The default local URL is `http://localhost:8080/api/v1`.

## Build

```bash
npm run build
```

The production output is written to `dist/` and can be deployed as a static site.

## Finance dashboard

After signing in or registering, the dashboard loads the signed-in user's transaction list and all-time income, expense, and balance totals. Search by category or note, filter by income/expense or date range, move through transaction pages, and export the matching results as CSV. Previously used categories appear as suggestions in transaction and budget forms; new category text is still accepted. Monthly insights compare income and expenses across six months and break the selected month's expenses down by category. Use the transaction form to add income or expenses, and the Edit/Delete actions on each row to maintain the list. Monthly budgets let you set a category limit and track spending. Savings goals let you set a target, record the amount saved, and follow progress. Amounts are shown in Bangladeshi taka (BDT).

The access token stays in React memory and is sent as a bearer token for API requests. It is cleared when you sign out or reload the page, so sign in again after a reload. The dashboard signs out automatically if the API reports that the token has expired.
