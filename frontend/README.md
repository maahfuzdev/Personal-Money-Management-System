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
