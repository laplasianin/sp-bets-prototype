# Web UI + Railway Deployment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a React/Vite frontend dashboard and deploy the full stack to Railway using EmbeddedKafka so no external Kafka service is needed.

**Architecture:** Two Railway services — `sporty-home-api` (Spring Boot + EmbeddedKafka, built via Dockerfile) and `sporty-home-ui` (React/Vite static site in `frontend/`). Frontend polls `GET /api/bets` every 2s and triggers settlement via `POST /api/event-outcomes`. CORS allows Railway origins via `allowedOriginPatterns`.

**Tech Stack:** Java 21 · Spring Boot 3.3 · EmbeddedKafkaBroker (spring-kafka-test in impl scope) · React 18 · Vite 6 · TypeScript · Tailwind CSS 3 · Railway

---

## File Map

**Created (backend):**
- `src/main/java/com/example/betting/api/BetController.java` — GET /api/bets, GET /api/bets/{id}
- `src/main/java/com/example/betting/config/CorsConfig.java` — CORS via allowedOriginPatterns
- `src/main/java/com/example/betting/config/EmbeddedKafkaInitializer.java` — starts EmbeddedKafkaBroker before Spring context beans
- `src/main/resources/application-railway.yml` — railway profile: disable H2 console, cors allowed-origins=*
- `Dockerfile` — multi-stage: JDK build → JRE runtime, SPRING_PROFILES_ACTIVE=railway
- `.dockerignore`

**Modified (backend):**
- `src/main/java/com/example/betting/BettingApplication.java` — add EmbeddedKafkaInitializer
- `src/main/java/com/example/betting/api/EventOutcomeController.java` — remove GET /api/bets endpoints + BetRepository/BetMapper deps
- `src/test/java/com/example/betting/controller/EventOutcomeControllerTest.java` — remove BetRepository/BetMapper mocks
- `build.gradle.kts` — move spring-kafka-test from testImplementation to implementation (demo-only)

**Created (frontend):**
- `frontend/package.json`
- `frontend/tsconfig.json`
- `frontend/vite.config.ts`
- `frontend/tailwind.config.js`
- `frontend/postcss.config.js`
- `frontend/index.html`
- `frontend/.env.example`
- `frontend/src/main.tsx`
- `frontend/src/index.css`
- `frontend/src/types.ts`
- `frontend/src/api.ts`
- `frontend/src/App.tsx`
- `frontend/src/components/AnnounceWinnerForm.tsx`
- `frontend/src/components/BetsTable.tsx`
- `frontend/src/components/PipelineStatus.tsx`

---

## Task 1: Extract BetController

Move GET /api/bets and GET /api/bets/{id} out of EventOutcomeController. After this task EventOutcomeController only handles POST /api/event-outcomes.

**Files:**
- Create: `src/main/java/com/example/betting/api/BetController.java`
- Modify: `src/main/java/com/example/betting/api/EventOutcomeController.java`
- Modify: `src/test/java/com/example/betting/controller/EventOutcomeControllerTest.java`

- [ ] **Step 1: Create BetController.java**

```java
package com.example.betting.api;

import com.example.betting.api.dto.BetResponse;
import com.example.betting.mapper.BetMapper;
import com.example.betting.persistence.BetRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class BetController {

    private final BetRepository betRepository;
    private final BetMapper betMapper;

    public BetController(BetRepository betRepository, BetMapper betMapper) {
        this.betRepository = betRepository;
        this.betMapper = betMapper;
    }

    @GetMapping("/bets")
    public List<BetResponse> getAllBets() {
        return betMapper.toResponseList(betRepository.findAll());
    }

    @GetMapping("/bets/{id}")
    public ResponseEntity<BetResponse> getBet(@PathVariable Long id) {
        return betRepository.findById(id)
                .map(betMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
```

- [ ] **Step 2: Trim EventOutcomeController.java to POST only**

Replace the entire file content:

```java
package com.example.betting.api;

import com.example.betting.api.dto.EventOutcomeAcceptedResponse;
import com.example.betting.api.dto.EventOutcomeRequest;
import com.example.betting.mapper.EventOutcomeMapper;
import com.example.betting.messaging.kafka.EventOutcomeProducer;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Instant;

@RestController
@RequestMapping("/api")
public class EventOutcomeController {

    private final EventOutcomeProducer producer;
    private final EventOutcomeMapper outcomeMapper;

    public EventOutcomeController(EventOutcomeProducer producer, EventOutcomeMapper outcomeMapper) {
        this.producer = producer;
        this.outcomeMapper = outcomeMapper;
    }

    @PostMapping("/event-outcomes")
    public ResponseEntity<EventOutcomeAcceptedResponse> publishOutcome(@Valid @RequestBody EventOutcomeRequest request) {
        producer.publish(outcomeMapper.toDomain(request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{eventId}")
                .buildAndExpand(request.eventId())
                .toUri();
        return ResponseEntity.accepted()
                .location(location)
                .body(new EventOutcomeAcceptedResponse(request.eventId(), Instant.now()));
    }
}
```

- [ ] **Step 3: Clean up EventOutcomeControllerTest.java — remove BetRepository and BetMapper mocks**

Replace the entire file:

```java
package com.example.betting.controller;

import com.example.betting.api.EventOutcomeController;
import com.example.betting.mapper.EventOutcomeMapper;
import com.example.betting.messaging.kafka.EventOutcomeProducer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventOutcomeController.class)
class EventOutcomeControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    EventOutcomeProducer producer;

    @MockBean
    EventOutcomeMapper outcomeMapper;

    @Test
    void publishOutcome_returns202() throws Exception {
        doNothing().when(producer).publish(any());
        when(outcomeMapper.toDomain(any())).thenReturn(
                new com.example.betting.domain.EventOutcome("evt-1", "Real vs Barca", "team-real"));

        String body = """
                {
                  "eventId": "evt-1",
                  "eventName": "Real vs Barca",
                  "winnerId": "team-real"
                }
                """;

        mockMvc.perform(post("/api/event-outcomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.eventId").value("evt-1"))
                .andExpect(jsonPath("$.acceptedAt").exists())
                .andExpect(header().exists("Location"));
    }

    @Test
    void publishOutcome_withMissingField_returns400() throws Exception {
        String body = """
                {
                  "eventName": "Real vs Barca"
                }
                """;
        mockMvc.perform(post("/api/event-outcomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
```

- [ ] **Step 4: Run tests**

```bash
./gradlew test
```

Expected: all tests PASS (no compilation errors, no test failures).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/betting/api/BetController.java \
        src/main/java/com/example/betting/api/EventOutcomeController.java \
        src/test/java/com/example/betting/controller/EventOutcomeControllerTest.java
git commit -m "refactor: extract BetController from EventOutcomeController"
```

---

## Task 2: CORS Config

Allow the frontend origin (Railway wildcard + localhost dev) on all `/api/**` endpoints.

**Files:**
- Create: `src/main/java/com/example/betting/config/CorsConfig.java`

- [ ] **Step 1: Create CorsConfig.java**

```java
package com.example.betting.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins:http://localhost:5173}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(allowedOrigins)
                .allowedMethods("GET", "POST")
                .allowedHeaders("Content-Type");
    }
}
```

- [ ] **Step 2: Run tests**

```bash
./gradlew test
```

Expected: all tests PASS.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/example/betting/config/CorsConfig.java
git commit -m "feat: add CORS config for frontend origins"
```

---

## Task 3: Railway Spring Profile + EmbeddedKafka

EmbeddedKafkaBroker must start before Spring auto-configures Kafka connections. Use `ApplicationContextInitializer` which runs before any beans are created.

**Files:**
- Modify: `build.gradle.kts`
- Create: `src/main/java/com/example/betting/config/EmbeddedKafkaInitializer.java`
- Modify: `src/main/java/com/example/betting/BettingApplication.java`
- Create: `src/main/resources/application-railway.yml`

- [ ] **Step 1: Move spring-kafka-test to implementation scope in build.gradle.kts**

Find the dependencies block and change the spring-kafka-test line:

```kotlin
// BEFORE:
testImplementation("org.springframework.kafka:spring-kafka-test")

// AFTER:
implementation("org.springframework.kafka:spring-kafka-test")
```

Keep it as `testImplementation` in the test block if it was already there — check. The full dependencies block should look like:

```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-jooq")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.springframework.kafka:spring-kafka-test")   // EmbeddedKafka for railway profile
    runtimeOnly("com.h2database:h2")

    implementation("org.mapstruct:mapstruct:$mapstructVersion")
    annotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")

    jooqGenerator("com.h2database:h2")
    jooqGenerator("org.jooq:jooq-meta-extensions:3.19.14")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.awaitility:awaitility")
}
```

- [ ] **Step 2: Create EmbeddedKafkaInitializer.java**

```java
package com.example.betting.config;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.kafka.test.EmbeddedKafkaBroker;

import java.util.Map;

public class EmbeddedKafkaInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static EmbeddedKafkaBroker broker;

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        if (!applicationContext.getEnvironment().matchesProfiles("railway")) {
            return;
        }
        broker = new EmbeddedKafkaBroker(1, true, 1, "event-outcomes", "event-outcomes.DLT")
                .kafkaPorts(9092);
        try {
            broker.afterPropertiesSet();
        } catch (Exception e) {
            throw new RuntimeException("Failed to start embedded Kafka broker", e);
        }
        applicationContext.getEnvironment().getPropertySources()
                .addFirst(new MapPropertySource("embedded-kafka", Map.of(
                        "spring.kafka.bootstrap-servers", "localhost:9092"
                )));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (broker != null) broker.destroy();
        }));
    }
}
```

- [ ] **Step 3: Register the initializer in BettingApplication.java**

```java
package com.example.betting;

import com.example.betting.config.EmbeddedKafkaInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BettingApplication {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(BettingApplication.class);
        app.addInitializers(new EmbeddedKafkaInitializer());
        app.run(args);
    }
}
```

- [ ] **Step 4: Create application-railway.yml**

```yaml
spring:
  h2:
    console:
      enabled: false

app:
  cors:
    allowed-origins: "*"
```

- [ ] **Step 5: Run tests to make sure nothing broke**

```bash
./gradlew test
```

Expected: all tests PASS. The initializer returns early for non-railway profiles, so tests are unaffected.

- [ ] **Step 6: Smoke-test the railway profile locally**

```bash
./gradlew bootRun --args='--spring.profiles.active=railway'
```

Expected: app starts without "Connection refused" Kafka errors. You should see log lines like `[EmbeddedKafka]` during startup. Try `curl http://localhost:8080/api/bets` — should return 5 bets.

Stop with Ctrl+C.

- [ ] **Step 7: Commit**

```bash
git add build.gradle.kts \
        src/main/java/com/example/betting/config/EmbeddedKafkaInitializer.java \
        src/main/java/com/example/betting/BettingApplication.java \
        src/main/resources/application-railway.yml
git commit -m "feat: add railway profile with EmbeddedKafka"
```

---

## Task 4: Dockerfile

Multi-stage build: JDK stage builds the jar, JRE stage runs it with `railway` profile active.

**Files:**
- Create: `Dockerfile`
- Create: `.dockerignore`

- [ ] **Step 1: Create Dockerfile in repo root**

```dockerfile
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Dspring.profiles.active=railway", "-jar", "app.jar"]
```

- [ ] **Step 2: Create .dockerignore**

```
.gradle/
build/
frontend/
docs/
.git/
.gitignore
.idea/
*.md
```

- [ ] **Step 3: Build Docker image locally to verify**

```bash
docker build -t sporty-home-api .
```

Expected: image builds successfully, `Successfully tagged sporty-home-api:latest`.

- [ ] **Step 4: Run container and smoke test**

```bash
docker run -p 8080:8080 sporty-home-api
```

In another terminal:
```bash
curl http://localhost:8080/api/bets
```

Expected: JSON array of 5 bets with status PENDING.

Stop container with Ctrl+C.

- [ ] **Step 5: Commit**

```bash
git add Dockerfile .dockerignore
git commit -m "feat: add multi-stage Dockerfile for Railway"
```

---

## Task 5: Frontend Scaffold

Initialize the `frontend/` directory with Vite + React + TypeScript + Tailwind CSS.

**Files:** All files under `frontend/`

- [ ] **Step 1: Create frontend directory and package.json**

Create `frontend/package.json`:

```json
{
  "name": "sporty-home-ui",
  "private": true,
  "version": "0.0.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "tsc && vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "react": "^18.3.1",
    "react-dom": "^18.3.1"
  },
  "devDependencies": {
    "@types/react": "^18.3.1",
    "@types/react-dom": "^18.3.1",
    "@vitejs/plugin-react": "^4.3.4",
    "autoprefixer": "^10.4.20",
    "postcss": "^8.4.47",
    "tailwindcss": "^3.4.14",
    "typescript": "^5.6.3",
    "vite": "^6.0.3"
  }
}
```

- [ ] **Step 2: Create frontend/tsconfig.json**

```json
{
  "compilerOptions": {
    "target": "ES2020",
    "useDefineForClassFields": true,
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "module": "ESNext",
    "skipLibCheck": true,
    "moduleResolution": "bundler",
    "allowImportingTsExtensions": true,
    "isolatedModules": true,
    "moduleDetection": "force",
    "noEmit": true,
    "jsx": "react-jsx",
    "strict": true
  },
  "include": ["src"]
}
```

- [ ] **Step 3: Create frontend/vite.config.ts**

```typescript
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
```

The proxy routes `/api` calls to the backend in local dev. On Railway, `VITE_API_URL` is used instead.

- [ ] **Step 4: Create Tailwind config files**

Create `frontend/tailwind.config.js`:

```js
/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {},
  },
  plugins: [],
};
```

Create `frontend/postcss.config.js`:

```js
export default {
  plugins: {
    tailwindcss: {},
    autoprefixer: {},
  },
};
```

- [ ] **Step 5: Create frontend/index.html**

```html
<!doctype html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Sporty Home</title>
  </head>
  <body>
    <div id="root"></div>
    <script type="module" src="/src/main.tsx"></script>
  </body>
</html>
```

- [ ] **Step 6: Create frontend/.env.example**

```
# Set to backend Railway URL for production builds
# Leave empty to use Vite proxy (localhost:8080) in development
VITE_API_URL=
```

- [ ] **Step 7: Install dependencies**

```bash
cd frontend && npm install
```

Expected: `node_modules/` created, no errors.

- [ ] **Step 8: Commit**

```bash
git add frontend/
git commit -m "feat: scaffold frontend with Vite + React + Tailwind"
```

---

## Task 6: Types and API Layer

Define TypeScript types matching `BetResponse.java` and implement fetch helpers.

**Files:**
- Create: `frontend/src/types.ts`
- Create: `frontend/src/api.ts`

- [ ] **Step 1: Create frontend/src/types.ts**

```typescript
export type BetStatus = 'PENDING' | 'WON' | 'LOST';

export interface Bet {
  id: number;
  userId: string;
  eventId: string;
  marketId: string;
  winnerId: string | null;
  amount: number;
  status: BetStatus;
  payout: number | null;
  settledAt: string | null;
  createdAt: string;
}
```

- [ ] **Step 2: Create frontend/src/api.ts**

```typescript
import type { Bet } from './types';

const BASE = import.meta.env.VITE_API_URL ?? '';

export async function fetchBets(): Promise<Bet[]> {
  const res = await fetch(`${BASE}/api/bets`);
  if (!res.ok) throw new Error(`GET /api/bets failed: ${res.status}`);
  return res.json() as Promise<Bet[]>;
}

export async function announceWinner(
  eventId: string,
  eventName: string,
  winnerId: string,
): Promise<void> {
  const res = await fetch(`${BASE}/api/event-outcomes`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ eventId, eventName, winnerId }),
  });
  if (!res.ok) throw new Error(`POST /api/event-outcomes failed: ${res.status}`);
}
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/types.ts frontend/src/api.ts
git commit -m "feat: add TypeScript types and API layer"
```

---

## Task 7: BetsTable Component

Polls `GET /api/bets` every 2s. Stops polling when all bets are settled (no PENDING rows).

**Files:**
- Create: `frontend/src/components/BetsTable.tsx`

- [ ] **Step 1: Create frontend/src/components/BetsTable.tsx**

```tsx
import { useEffect, useRef, useState } from 'react';
import { fetchBets } from '../api';
import type { Bet } from '../types';

const STATUS_CLASSES: Record<string, string> = {
  PENDING: 'bg-gray-100 text-gray-600',
  WON:     'bg-green-100 text-green-700',
  LOST:    'bg-red-100 text-red-700',
};

interface Props {
  pollTrigger: number;
}

export function BetsTable({ pollTrigger }: Props) {
  const [bets, setBets] = useState<Bet[]>([]);
  const [error, setError] = useState<string | null>(null);
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null);

  function stopPolling() {
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
      intervalRef.current = null;
    }
  }

  async function load() {
    try {
      const data = await fetchBets();
      setBets(data);
      if (data.length > 0 && data.every(b => b.status !== 'PENDING')) {
        stopPolling();
      }
    } catch {
      setError('Failed to load bets');
      stopPolling();
    }
  }

  useEffect(() => {
    load();
    intervalRef.current = setInterval(load, 2000);
    return stopPolling;
  }, [pollTrigger]);

  if (error) return <p className="text-red-600 text-sm">{error}</p>;

  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm border-collapse">
        <thead>
          <tr className="bg-gray-50 text-left">
            {['ID', 'User', 'Event', 'Picked', 'Amount', 'Status', 'Payout'].map(h => (
              <th key={h} className="px-4 py-2 border-b font-semibold text-gray-600">{h}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {bets.map(bet => (
            <tr key={bet.id} className="border-b hover:bg-gray-50 transition-colors">
              <td className="px-4 py-2 text-gray-500">{bet.id}</td>
              <td className="px-4 py-2">{bet.userId}</td>
              <td className="px-4 py-2">{bet.eventId}</td>
              <td className="px-4 py-2">{bet.marketId}</td>
              <td className="px-4 py-2">${Number(bet.amount).toFixed(2)}</td>
              <td className="px-4 py-2">
                <span className={`px-2 py-0.5 rounded text-xs font-semibold ${STATUS_CLASSES[bet.status]}`}>
                  {bet.status}
                </span>
              </td>
              <td className="px-4 py-2">
                {bet.payout != null ? `$${Number(bet.payout).toFixed(2)}` : '—'}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/BetsTable.tsx
git commit -m "feat: add BetsTable with auto-polling"
```

---

## Task 8: AnnounceWinnerForm Component

Dropdown selects event and winner; GO button calls `POST /api/event-outcomes`.

**Files:**
- Create: `frontend/src/components/AnnounceWinnerForm.tsx`

- [ ] **Step 1: Create frontend/src/components/AnnounceWinnerForm.tsx**

```tsx
import { useState } from 'react';
import { announceWinner } from '../api';

const EVENTS = [
  { id: 'evt-1', name: 'Real Madrid vs Barcelona' },
  { id: 'evt-2', name: 'Arsenal vs Chelsea' },
];

const WINNERS: Record<string, { id: string; label: string }[]> = {
  'evt-1': [
    { id: 'team-real',  label: 'Real Madrid' },
    { id: 'team-barca', label: 'Barcelona' },
  ],
  'evt-2': [
    { id: 'team-arsenal', label: 'Arsenal' },
    { id: 'team-chelsea', label: 'Chelsea' },
  ],
};

interface Props {
  onAnnounced: () => void;
}

export function AnnounceWinnerForm({ onAnnounced }: Props) {
  const [eventId, setEventId]   = useState('evt-1');
  const [winnerId, setWinnerId] = useState('team-real');
  const [loading, setLoading]   = useState(false);
  const [error, setError]       = useState<string | null>(null);

  const event   = EVENTS.find(e => e.id === eventId)!;
  const winners = WINNERS[eventId];

  function handleEventChange(newEventId: string) {
    setEventId(newEventId);
    setWinnerId(WINNERS[newEventId][0].id);
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      await announceWinner(eventId, event.name, winnerId);
      onAnnounced();
    } catch {
      setError('Failed to announce winner. Is the backend running?');
    } finally {
      setLoading(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-wrap items-end gap-4">
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">Event</label>
        <select
          value={eventId}
          onChange={e => handleEventChange(e.target.value)}
          className="border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          {EVENTS.map(ev => (
            <option key={ev.id} value={ev.id}>{ev.name}</option>
          ))}
        </select>
      </div>
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">Winner</label>
        <select
          value={winnerId}
          onChange={e => setWinnerId(e.target.value)}
          className="border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          {winners.map(w => (
            <option key={w.id} value={w.id}>{w.label}</option>
          ))}
        </select>
      </div>
      <button
        type="submit"
        disabled={loading}
        className="bg-blue-600 text-white px-6 py-2 rounded font-semibold text-sm hover:bg-blue-700 disabled:opacity-50 transition-colors"
      >
        {loading ? 'Sending…' : 'GO'}
      </button>
      {error && <span className="text-red-600 text-sm">{error}</span>}
    </form>
  );
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/AnnounceWinnerForm.tsx
git commit -m "feat: add AnnounceWinnerForm component"
```

---

## Task 9: PipelineStatus Component

Animates through API → Kafka → Settlement → DB stages after GO is pressed.

**Files:**
- Create: `frontend/src/components/PipelineStatus.tsx`

- [ ] **Step 1: Create frontend/src/components/PipelineStatus.tsx**

```tsx
import { useEffect, useState } from 'react';

const STAGES = ['API', 'Kafka', 'Settlement', 'DB'];
const STAGE_DELAY_MS = 500;

interface Props {
  active: boolean;
}

export function PipelineStatus({ active }: Props) {
  const [step, setStep] = useState(-1);

  useEffect(() => {
    if (!active) {
      setStep(-1);
      return;
    }
    const timers = STAGES.map((_, i) =>
      setTimeout(() => setStep(i), i * STAGE_DELAY_MS),
    );
    const done = setTimeout(() => setStep(STAGES.length), STAGES.length * STAGE_DELAY_MS);
    return () => {
      timers.forEach(clearTimeout);
      clearTimeout(done);
    };
  }, [active]);

  return (
    <div className="flex items-center gap-2">
      {STAGES.map((stage, i) => (
        <div key={stage} className="flex items-center gap-2">
          <span
            className={`px-3 py-1 rounded-full text-sm font-medium transition-colors duration-300 ${
              step === i
                ? 'bg-blue-600 text-white'
                : step > i
                ? 'bg-green-500 text-white'
                : 'bg-gray-200 text-gray-500'
            }`}
          >
            {stage}
          </span>
          {i < STAGES.length - 1 && (
            <span className={`font-bold transition-colors ${step > i ? 'text-green-500' : 'text-gray-300'}`}>
              →
            </span>
          )}
        </div>
      ))}
    </div>
  );
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/PipelineStatus.tsx
git commit -m "feat: add PipelineStatus component"
```

---

## Task 10: Wire Up App.tsx, main.tsx, index.css

Assemble the final page.

**Files:**
- Create: `frontend/src/main.tsx`
- Create: `frontend/src/index.css`
- Create: `frontend/src/App.tsx`

- [ ] **Step 1: Create frontend/src/index.css**

```css
@tailwind base;
@tailwind components;
@tailwind utilities;
```

- [ ] **Step 2: Create frontend/src/main.tsx**

```tsx
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import './index.css';
import App from './App';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
```

- [ ] **Step 3: Create frontend/src/App.tsx**

```tsx
import { useState } from 'react';
import { AnnounceWinnerForm } from './components/AnnounceWinnerForm';
import { BetsTable } from './components/BetsTable';
import { PipelineStatus } from './components/PipelineStatus';

export default function App() {
  const [pipelineActive, setPipelineActive] = useState(false);
  const [pollTrigger, setPollTrigger] = useState(0);

  function handleAnnounced() {
    setPipelineActive(true);
    setPollTrigger(t => t + 1);
    setTimeout(() => setPipelineActive(false), STAGES_COUNT * 500 + 200);
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white border-b px-8 py-4 shadow-sm">
        <h1 className="text-xl font-bold text-gray-900">Sporty Home — Bet Settlement Demo</h1>
        <p className="text-sm text-gray-500 mt-0.5">Spring Boot · Kafka · jOOQ · RocketMQ</p>
      </header>
      <main className="max-w-5xl mx-auto px-8 py-8 space-y-6">
        <section className="bg-white rounded-lg border p-6 space-y-4">
          <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wide">Announce Winner</h2>
          <AnnounceWinnerForm onAnnounced={handleAnnounced} />
        </section>
        <section className="bg-white rounded-lg border p-6 space-y-4">
          <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wide">Pipeline Status</h2>
          <PipelineStatus active={pipelineActive} />
        </section>
        <section className="bg-white rounded-lg border p-6 space-y-4">
          <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wide">Bets</h2>
          <BetsTable pollTrigger={pollTrigger} />
        </section>
      </main>
    </div>
  );
}

const STAGES_COUNT = 4;
```

- [ ] **Step 4: Run frontend dev server**

```bash
cd frontend && npm run dev
```

Expected: Vite starts on `http://localhost:5173`. Open the URL in a browser. You should see the dashboard with the header, form, pipeline bar, and an empty bets table (backend not running yet — that's ok, table shows empty or error).

- [ ] **Step 5: Commit**

```bash
git add frontend/src/main.tsx frontend/src/index.css frontend/src/App.tsx
git commit -m "feat: assemble App with all components wired up"
```

---

## Task 11: Full Local Smoke Test

Run backend and frontend together and verify the end-to-end flow.

- [ ] **Step 1: Start backend with railway profile (EmbeddedKafka)**

```bash
./gradlew bootRun --args='--spring.profiles.active=railway'
```

Wait for: `Started BettingApplication` log line.

- [ ] **Step 2: Start frontend dev server (separate terminal)**

```bash
cd frontend && npm run dev
```

- [ ] **Step 3: Open http://localhost:5173 and test the flow**

1. Verify bets table shows 5 rows, all PENDING (grey)
2. Select event `Real Madrid vs Barcelona`, winner `Real Madrid`, click GO
3. Verify pipeline bar animates: API → Kafka → Settlement → DB
4. Within 2-3 seconds, verify:
   - Bet 1 (user-1, team-real, 100.00) → **WON**, payout 200.00
   - Bet 2 (user-2, team-barca, 200.00) → **LOST**, payout 0.00
   - Bet 3 (user-3, team-real, 50.00) → **WON**, payout 100.00
5. Bets 4 and 5 remain PENDING (they are for evt-2)
6. Select `Arsenal vs Chelsea`, winner `Arsenal`, click GO
7. Verify bets 4 and 5 settle correctly

- [ ] **Step 4: Run full test suite one final time**

```bash
./gradlew test
```

Expected: all tests PASS.

- [ ] **Step 5: Commit if any fixes were made**

```bash
git add -p
git commit -m "fix: <describe what was fixed>"
```

---

## Task 12: Deploy to Railway

- [ ] **Step 1: Create Railway project**

Go to [railway.app](https://railway.app), create a new project called `sporty-home`.

- [ ] **Step 2: Deploy backend service**

In the Railway project, click "Add Service" → "GitHub Repo" → select this repo.
Railway will detect the `Dockerfile` and use it automatically.

Set service name: `sporty-home-api`.

No extra env vars needed — the Dockerfile sets `SPRING_PROFILES_ACTIVE=railway`.

Generate a Railway domain for this service: Settings → Networking → Generate Domain.
Note the URL, e.g. `https://sporty-home-api-production.up.railway.app`.

- [ ] **Step 3: Deploy frontend service**

In the same Railway project, click "Add Service" → "GitHub Repo" → same repo.

Configure the service:
- Root directory: `frontend`
- Build command: `npm run build`
- Output directory: `dist`

Set service name: `sporty-home-ui`.

Add env var:
```
VITE_API_URL=https://sporty-home-api-production.up.railway.app
```
(Use the actual URL from Step 2.)

Trigger a redeploy after adding the env var so Vite picks it up at build time.

Generate a Railway domain for this service. Open it — dashboard should load.

- [ ] **Step 4: Smoke test the Railway deployment**

Open `https://sporty-home-ui-production.up.railway.app`.
Repeat the same flow from Task 11 Step 3. Verify settlement works end-to-end on Railway.
