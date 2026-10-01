# Backend

Java 17+ and Spring Boot REST API. The backend owns validation, business rules, authentication, and persistence. It returns JSON and does not render web pages.

## Configuration

Set `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` in the process environment. Local database setup is in [Development setup](../docs/getting-started.md).

## Run

From this directory, run `mvn spring-boot:run` after the backend application and database configuration are in place.

## API contract

Endpoints are versioned under `/api/v1`. DTOs define the public request and response shapes; persistence entities are not returned directly.
