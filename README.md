# invoicing-service

Issues invoice documents in response to PaymentSucceeded events from Payments.

Owned by **Billing Platform**. Java 21. Registered as **Invoicing**.

## Endpoints

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/events/payment-succeeded` | Ingest a `PaymentSucceeded` event |
| `GET` | `/documents/:documentId` | Read an issued invoice document |
| `GET` | `/documents` | List issued documents |
| `GET` | `/health` | Liveness |

Consumes the **`PaymentSucceeded`** event published asynchronously by **Payments**.
When Payments stops publishing, this service stays healthy and simply issues nothing.

```bash
mvn -q -DskipTests package
java -jar target/invoicing-service-1.12.0.jar
```
