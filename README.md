# Sporty Home — Kafka & RocketMQ Bet Settlement Service

A Spring Boot backend + React frontend that simulates sports betting event outcome handling and bet settlement via Kafka and a mock RocketMQ broker.

## Live Demo

**[https://dazzling-success-production-78c2.up.railway.app/](https://dazzling-success-production-78c2.up.railway.app/)**

Try the full pipeline in the browser:

1. **Pick an event and winner** in the "Announce Winner" form, click **GO**
2. **Watch the pipeline** animate: API → Kafka → Settlement → DB
3. **See bets update** in the table — WON bets get `payout = amount × 2`, LOST get `0.00`
4. Click **Reset Demo** to restore all 5 bets to `PENDING` and repeat

| Event | Bettors |
|-------|---------|
| Real Madrid vs Barcelona (evt-1) | user-1 → Real Madrid · user-2 → Barcelona · user-3 → Real Madrid |
| Arsenal vs Chelsea (evt-2) | user-4 → Arsenal · user-5 → Chelsea |

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

**Tech stack:** Java 21 · Spring Boot 3.3 · Gradle (Kotlin DSL) · jOOQ (DDLDatabase codegen) · Spring Kafka · H2 in-memory · MapStruct · React 18 · Vite · TypeScript · Tailwind CSS

**Railway deployment:** EmbeddedKafkaKraftBroker runs inside the JVM via `ApplicationContextInitializer` — no external Kafka service needed.

**RocketMQ note:** The `BetSettlementProducer` interface is production-ready for swap — add `spring-rocketmq-starter` + `@Profile("rocketmq")` implementation to go live.

## Running Locally

### Backend

Prerequisites: Java 21+, Docker (for external Kafka — or skip with `--no-kafka` profile)

```bash
# Start Kafka
docker-compose up -d

# Start backend
./gradlew bootRun
```

Backend starts on `http://localhost:8080`.

H2 Console: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:betsdb`, user: `sa`, password: empty)

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend starts on `http://localhost:5173`, proxies `/api` to `localhost:8080`.

## API

### Publish event outcome

```bash
curl -X POST http://localhost:8080/api/event-outcomes \
  -H "Content-Type: application/json" \
  -d '{"eventId":"evt-1","eventName":"Real Madrid vs Barcelona","winnerId":"team-real"}'
```

Response: `202 Accepted`

### Check bets

```bash
curl http://localhost:8080/api/bets
curl http://localhost:8080/api/bets/1
```

### Reset demo data

```bash
curl -X POST http://localhost:8080/api/reset
```

After settling `evt-1` with `winnerId=team-real`:
- Bet 1 (user-1, team-real) → **WON**, payout = 200.00
- Bet 2 (user-2, team-barca) → **LOST**, payout = 0.00
- Bet 3 (user-3, team-real) → **WON**, payout = 100.00

## Tests

```bash
./gradlew test
```

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

## What is NOT implemented

- Real RocketMQ cluster (interface is ready for swap via `@Profile("rocketmq")`)
- Authentication / authorization
- Outbox pattern (transactional Kafka publish)
- Metrics / tracing (logs only)
- Multi-instance coordination
