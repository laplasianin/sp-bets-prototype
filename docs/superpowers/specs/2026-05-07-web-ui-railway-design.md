# Web UI + Railway Deployment — Design Spec
Date: 2026-05-07

## Goal

Add a visual React frontend and deploy the full stack to Railway for technical interview demos. Show the entire bet settlement lifecycle (Kafka → Settlement → DB) without curl.

## Architecture

```
Railway Project: sporty-home
├── sporty-home-api   (Spring Boot, Gradle)
│   ├── Profile: railway → EmbeddedKafka inside JVM
│   ├── H2 in-memory (unchanged)
│   ├── CORS: sporty-home-ui.up.railway.app + localhost:5173
│   └── PORT: 8080
└── sporty-home-ui   (React/Vite, static site)
    ├── VITE_API_URL → sporty-home-api Railway URL
    └── Build output: dist/
```

Both services live in one Railway project, one GitHub repo. Frontend in `frontend/` subfolder.

## Repo Structure

```
sporty-home/
├── src/                          # Spring Boot (unchanged business logic)
├── frontend/                     # New
│   ├── src/
│   │   ├── App.tsx
│   │   ├── components/
│   │   │   ├── AnnounceWinnerForm.tsx
│   │   │   ├── PipelineStatus.tsx
│   │   │   └── BetsTable.tsx
│   │   └── main.tsx
│   ├── package.json
│   └── vite.config.ts
├── Dockerfile                    # New
├── build.gradle.kts              # Unchanged
└── docker-compose.yml            # Unchanged
```

## Frontend

**Stack:** React + Vite + Tailwind CSS. Single page, no router.

**Components:**

- `AnnounceWinnerForm` — select eventId (evt-1 | evt-2), free-text winnerId input, GO button → `POST /api/event-outcomes`
- `PipelineStatus` — visual bar: `API → Kafka → Settlement → DB`, animates for ~2s after GO is pressed
- `BetsTable` — polls `GET /api/bets` every 2s while any bet is PENDING, stops when all settled

**Bet row states:**
- `PENDING` — grey
- `WON` — green, shows payout
- `LOST` — red, payout 0.00

**Columns:** ID | User | Event | Picked | Amount | Status | Payout

## Backend Changes

Minimal. Business logic untouched.

### 1. `application-railway.yml`
New Spring profile for Railway. Enables EmbeddedKafka on localhost:9092 so no external Kafka service is needed.

### 2. `CorsConfig.java`
New `@Configuration` bean. Allows `sporty-home-ui.up.railway.app` and `localhost:5173`.

### 3. `BetController.java`
Extract `GET /api/bets` and `GET /api/bets/{id}` from `EventOutcomeController` into a dedicated `BetController`. Cleaner separation — event outcomes and bet reads are different concerns.

### 4. `Dockerfile`
```dockerfile
FROM eclipse-temurin:21-jre
COPY build/libs/betting-*.jar app.jar
ENTRYPOINT ["java", "-Dspring.profiles.active=railway", "-jar", "app.jar"]
```

## Data Flow

```
User: select evt-1, winner team-real → click GO
  → POST /api/event-outcomes { eventId, eventName, winnerId }
  → 202 Accepted
  → EmbeddedKafka topic: event-outcomes
  → EventOutcomeConsumer → SettlementOrchestrator
  → H2: bets updated (WON/LOST + payout)

Frontend: polling GET /api/bets every 2s
  → table rows flip from PENDING to WON/LOST
  → polling stops when no PENDING rows remain
```

## Railway Deployment

**sporty-home-api:**
- Build command: `./gradlew bootJar`
- Start: via Dockerfile (`SPRING_PROFILES_ACTIVE=railway`)
- Env vars: `PORT=8080`

**sporty-home-ui:**
- Root directory: `frontend/`
- Build command: `npm run build`
- Output directory: `dist/`
- Env vars: `VITE_API_URL=https://sporty-home-api.up.railway.app`

## What Is NOT Changed

- Core business logic (SettlementOrchestrator, BetSettlementHandler, MockRocketMQ)
- H2 schema and seed data
- Kafka consumer/producer logic
- Existing tests
- docker-compose.yml (still works for local dev)
