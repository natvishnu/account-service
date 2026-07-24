# account-service

Internal API for the [Event Ledger](https://github.com/natvishnu/event-ledger) system. It maintains
account balances and transaction history. It is called only by the
[`event-gateway`](https://github.com/natvishnu/event-gateway) and is never exposed to external
clients.

> For the full system overview (architecture, cross-service behaviour, how to run both services
> together with Docker Compose), see the **[event-ledger](https://github.com/natvishnu/event-ledger)**
> orchestration repo.

## Responsibilities

- Apply transactions to accounts, creating an account on first use.
- Maintain the running balance: `balance = Σ CREDIT − Σ DEBIT`.
- Be **idempotent** on `eventId`, so a re-applied transaction never double-counts.
- Serve balance and account-detail queries.

Applying is order-independent: the balance is a commutative sum of signed amounts, so events that
arrive out of chronological order still produce the correct balance.

## Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/accounts/{accountId}/transactions` | Apply a transaction (idempotent on `eventId`) |
| `GET`  | `/accounts/{accountId}/balance` | Current balance |
| `GET`  | `/accounts/{accountId}` | Balance + recent transactions and counts |
| `GET`  | `/health` | Service status + database connectivity |
| `GET`  | `/actuator/health`, `/actuator/prometheus` | Actuator diagnostics/metrics |

**`POST /accounts/{accountId}/transactions` body**

```json
{ "eventId": "evt-1", "type": "CREDIT", "amount": 100.00, "eventTimestamp": "2026-07-13T10:00:00Z" }
```

**Response**

```json
{ "accountId": "acc-1", "eventId": "evt-1", "balance": 100.00, "duplicate": false }
```

`duplicate: true` means the `eventId` was already applied; the balance is returned unchanged.
Validation failures return `400`; an unknown account on a query returns `404`.

## Tech stack

Java 21 · Spring Boot 3.4 · Spring Web / Data JPA / Validation / Actuator · H2 (in-memory) ·
Micrometer Tracing (OpenTelemetry) · structured JSON logging.

## Configuration

| Setting | Property | Default |
|---|---|---|
| HTTP port | `server.port` | `8081` |

The database is in-memory H2, recreated on startup; the H2 console is available at `/h2-console`.

## Build & run

**From source (JDK 21 + Maven 3.9+):**

```bash
mvn spring-boot:run          # runs on :8081
mvn test                     # run the test suite
```

**As a Docker image:**

```bash
docker build -t ghcr.io/natvishnu/account-service:latest .
docker run -p 8081:8081 ghcr.io/natvishnu/account-service:latest
docker push ghcr.io/natvishnu/account-service:latest   # after `docker login ghcr.io`
```

## Tests

`mvn test` covers balance computation, idempotency (no double-apply), out-of-order tolerance, and
validation (zero/negative amount, unknown type → `400`; unknown account → `404`).
