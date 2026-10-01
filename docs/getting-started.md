# Development setup

## Requirements

- Java 17 or newer
- Maven 3.6.3 or newer
- Node.js 22.12 or newer and npm
- MySQL 8 or a compatible MySQL server

## Local database

Create an empty database named `personal_money_management`, then configure the backend environment variables:

```text
DB_URL=jdbc:mysql://localhost:3306/personal_money_management
DB_USERNAME=your_mysql_user
DB_PASSWORD=your_mysql_password
```

Set those values in your IDE run configuration or process environment before starting the API. Never place real credentials in a committed file. The backend and frontend READMEs contain their individual run commands.

## Local URLs

- React development server: `http://localhost:5173`
- Java API: `http://localhost:8080`

The browser app's API URL is supplied through `VITE_API_BASE_URL`. Local defaults and production settings will be documented alongside the API setup.

## Learning in increments

Each feature increment should explain the files it adds, the role of each layer, and how to run the feature. The architecture and API documents should be updated as contracts change.
