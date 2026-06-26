# Sentra Gateway Console

Modern Next.js frontend for the Sentra Gateway repository.

## Run

```bash
npm install
npm run dev
```

The console starts on `http://localhost:3000` by default.

## Backend Targets

The Next.js server-side proxy defaults to:

```txt
GATEWAY_BASE_URL=http://localhost:8080
USER_SERVICE_BASE_URL=http://localhost:8081
ORDER_SERVICE_BASE_URL=http://localhost:8082
PAYMENT_SERVICE_BASE_URL=http://localhost:8083
NOTIFICATION_SERVICE_BASE_URL=http://localhost:8084
```

Override these values in `.env.local` when the services run elsewhere.

## Checks

```bash
npm run lint
npm run typecheck
npm run test
npm run build
```
