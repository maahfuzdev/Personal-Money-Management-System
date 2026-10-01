# Backend

Java 17+ and Spring Boot REST API. The backend owns validation, business rules, authentication, and persistence. It returns JSON and does not render web pages.

## Configuration

Set `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` in the process environment. Local database setup is in [Development setup](../docs/getting-started.md).

## Run on Windows

From this directory, run `.\mvnw.cmd spring-boot:run` after setting the database environment variables. The Maven Wrapper downloads and uses the project Maven version, so a global Maven install is not required.

On macOS or Linux, run `./mvnw spring-boot:run`.

## API contract

Endpoints are versioned under `/api/v1`. DTOs define the public request and response shapes; persistence entities are not returned directly.
