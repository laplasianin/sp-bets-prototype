# Sporty Home — Kafka & RocketMQ Bet Settlement Service

A Spring Boot backend that simulates sports betting event outcome handling and bet settlement via Kafka and a mock RocketMQ broker.

## Architecture

```
POST /api/event-outcomes  (202 Accepted)
        │
        ▼
Kafka topic: event-outcomes ──── (3 retries + exp backoff) ──► event-outcomes.DLT
        │
        ▼
EventOutcomeConsumer
 → SettlementOrchestrator
     • loads PENDING bets for the event from H2 (via jOOQ)
     • computes WON / LOST + payout (amount × 2 on win)
     • sends BetSettlement to MockRocketMqBroker (async queue)
                │
                ▼
        MockRocketMqBroker  (in-process, non-blocking)
         • worker thread dispatches to @RocketMqMockListener methods
         • 3 retries with exponential backoff (500ms × 2^n)
         • DLQ on exhaustion
                │
                ▼
        BetSettlementHandler  (@RocketMqMockListener)
         • idempotent UPDATE: only settles PENDING bets
         • persists status + payout + settled_at via jOOQ
```

**Tech stack:** Java 21 · Spring Boot 3.3 · Gradle (Kotlin DSL) · jOOQ (DDLDatabase codegen) · Spring Kafka · H2 in-memory · MapStruct

**RocketMQ note:** The `BetSettlementProducer` interface is production-ready for swap — add `spring-rocketmq-starter` + `@Profile("rocketmq")` implementation to go live.

## Prerequisites

- Docker (for Kafka)
- Java 21+

## Running

### 1. Start Kafka

```bash
docker-compose up -d
```

### 2. Start the application

```bash
./gradlew bootRun
```

The app starts on `http://localhost:8080`.

H2 Console available at `http://localhost:8080/h2-console`  
(JDBC URL: `jdbc:h2:mem:betsdb`, user: `sa`, password: empty)

### 3. Seed data

Five sample bets are pre-loaded:

| Bet ID | User   | Event  | They picked  | Amount |
|--------|--------|--------|--------------|--------|
| 1      | user-1 | evt-1  | team-real    | 100.00 |
| 2      | user-2 | evt-1  | team-barca   | 200.00 |
| 3      | user-3 | evt-1  | team-real    | 50.00  |
| 4      | user-4 | evt-2  | team-arsenal | 150.00 |
| 5      | user-5 | evt-2  | team-chelsea | 75.00  |

## API

### Publish event outcome

```bash
curl -X POST http://localhost:8080/api/event-outcomes \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "evt-1",
    "eventName": "Real Madrid vs Barcelona",
    "winnerId": "team-real"
  }'
```

Response: `202 Accepted`
```json
{
  "eventId": "evt-1",
  "acceptedAt": "2026-05-07T10:00:00Z"
}
```

### Check bet settlement results

```bash
# All bets
curl http://localhost:8080/api/bets

# Single bet
curl http://localhost:8080/api/bets/1
```

After publishing `evt-1` with `winnerId=team-real`:
- Bet 1 → **WON**, payout = 200.00
- Bet 2 → **LOST**, payout = 0.00
- Bet 3 → **WON**, payout = 100.00

## Running Tests

```bash
./gradlew test
```

Tests use `EmbeddedKafka` — no Docker needed for tests.

### Test coverage

| Test | What it covers |
|------|----------------|
| `EventOutcomeControllerTest` | 202 response, Location header, validation (400) |
| `SettlementOrchestratorTest` | WON/LOST logic, payout calculation, no bets case |
| `MockRocketMqBrokerTest` | Async dispatch to `@RocketMqMockListener` |
| `EndToEndTest` | Full flow: POST → Kafka → DB settled (Awaitility) |

## Payout Rules

| Outcome | Payout |
|---------|--------|
| WON | `bet_amount × 2` |
| LOST | `0.00` |

Odds are not part of the spec; fixed 2× multiplier is used.

## What is NOT implemented

- Real RocketMQ cluster (interface is ready for swap via `@Profile("rocketmq")`)
- Authentication / authorization
- Outbox pattern (transactional Kafka publish)
- Metrics / tracing (logs only)
- Multi-instance coordination
