# invoicing-service

Issues invoice documents in response to PaymentSucceeded events from Payments.

Owned by **Billing Platform**. Registered in the Atlassian Service Registry as **Invoicing**.

## Endpoints

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/events/payment-succeeded` | Ingest a `PaymentSucceeded` event |
| `GET` | `/documents/:documentId` | Read an issued invoice document |
| `POST` | `/documents/:documentId/void` | Void a document after a reversal |
| `GET` | `/documents` | List issued documents, optionally by invoice |
| `GET` | `/health` | Liveness |
| `GET` | `/ready` | Readiness |
| `GET` | `/metrics` | Counts by status |

## Quick start

```bash
npm install
npm start
```

## Configuration

| Variable | Default | Purpose |
|---|---|---|
| `PORT` | `3000` | HTTP listen port |
| `SERVICE_NAME` | `invoicing-service` | Service name in logs and telemetry |
| `RELEASE` | `invoicing-<package version>` | Release identifier in logs and telemetry |
| `API_KEY` | unset | When set, requests must send `x-api-key` |
| `RATE_LIMIT_WINDOW_MS` | `60000` | Rate-limit window |
| `RATE_LIMIT_MAX` | `100` | Requests per window per client |

## Architecture

Routes parse and validate, controllers map to HTTP, the service layer holds the business
rules, and the repository keeps state in an in-memory `Map`. There is no external database.

## Dependencies

Consumes the **`PaymentSucceeded`** event published asynchronously by **Payments**.
