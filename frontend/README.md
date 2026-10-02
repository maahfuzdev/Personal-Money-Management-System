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

## Finance workspace

After signing in or registering, the responsive workspace is split into five navigable pages: Overview, Transactions, Budgets, Savings goals, and Reports. Desktop uses a persistent sidebar; mobile uses a fixed bottom navigation bar. Browser back/forward navigation is supported.

Overview shows all-time income, expense, and balance totals plus recent transactions, with a quick action to add a transaction. Transactions supports search by category or note, income/expense and date-range filters, date-range validation, a one-click clear action with an active-filter count, server-side pagination, CSV export, and create/edit/delete actions. Deletion uses a keyboard-accessible confirmation dialog. Successful transaction, budget, and savings goal changes show a dismissible status message that clears automatically. On narrower screens, the transaction form appears before the list for quicker entry. Previously used categories appear as suggestions in transaction and budget forms; new category text is accepted. Budgets lets you switch months to review existing category limits and spending, or create a monthly limit. Savings goals tracks target amounts, saved amounts, dates, and progress. Reports compares six months of cash flow and shows the selected month's spending by category. Amounts are shown in Bangladeshi taka (BDT).

The access token stays in React memory and is sent as a bearer token for API requests. It is cleared when you sign out or reload the page, so sign in again after a reload. The dashboard signs out automatically if the API reports that the token has expired.
