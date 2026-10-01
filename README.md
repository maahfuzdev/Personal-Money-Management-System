# Personal Money Management System

A responsive personal finance app for recording income and expenses, planning budgets, and understanding spending.

## Project layout

```text
backend/   Java 17 + Spring Boot REST API
frontend/  React + Vite web application
docs/      Architecture, setup, API, and deployment documentation
```

The frontend and backend live in this repository but build and deploy independently. The React app calls the Java API through an environment-configured API URL. MySQL stores application data. Credentials belong in environment variables and must not be committed.

## Documentation

- [Architecture](docs/architecture.md)
- [API contract](docs/api.md)
- [Development setup](docs/getting-started.md)
- [Backend setup](backend/README.md)
- [Frontend setup](frontend/README.md)

## Product progress

- [x] Account registration and sign in
- [ ] Income and expense tracking
- [ ] Dashboard summaries and transaction history
- [ ] Budgets, savings goals, and reports
- [x] Responsive account screens for phones, tablets, and desktop

Features will be delivered in small, documented increments. See the documentation for the current implementation status.
