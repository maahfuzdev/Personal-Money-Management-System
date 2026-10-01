# Frontend

React single-page app built with Vite. It is designed to deploy independently from the Java backend.

## Requirements

Node.js 22.12 or newer and npm.

## Run

```bash
npm install
npm run dev
```

Set `VITE_API_BASE_URL` to the backend origin for the target environment. Vite exposes `VITE_` variables to browser code, so never put secrets in them.

## Build

```bash
npm run build
```

The production output is written to `dist/` and can be deployed as a static site.
