# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository layout

| Directory | Purpose |
|---|---|
| `timeless-api/` | Java 21 / Quarkus 3 backend + Angular 19 frontend (served via Quinoa) |
| `whatsapp/` | Node.js service that bridges WhatsApp messages to the API via SQS |
| `docker/` | Local-dev docker-compose (LocalStack SQS/S3, PostgreSQL, Ollama) |
| `infrastructure/` | Terraform / AWS provisioning |
| `site/` | Landing page |

## Commands

### timeless-api (Java/Quarkus)

```bash
# Dev mode (hot reload) — requires OPENAI_API_KEY in application.properties
cd timeless-api && ./mvnw quarkus:dev

# Dev mode using local Ollama instead of OpenAI
cd timeless-api && ./mvnw quarkus:dev -Pollama -Dquarkus.profile=local

# Clean, format, and run all tests (required before submitting a PR)
cd timeless-api && ./mvnw clean package

# Run a single test class
cd timeless-api && ./mvnw test -Dtest=SignUpResourceTest

# Run a single test method
cd timeless-api && ./mvnw test -Dtest=SignUpResourceTest#should_returnCreated_when_validSignUpRequestIsProvided
```

### Frontend (Angular — inside Quinoa)

```bash
cd timeless-api/src/main/webui
npm run prettier:write   # format check + apply
```

### whatsapp service

```bash
cd whatsapp
npm install
npm run start:local      # loads .env.local
npm run prettier:write   # format check + apply
```

### Local infrastructure

```bash
cd docker && docker-compose up -d   # starts LocalStack, PostgreSQL, Ollama
```

## CI pipeline

CI runs on every push/PR to `main` when `timeless-api/`, `whatsapp/`, or `infrastructure/` change. Key differences from local dev:

- **Java build uses check mode, not auto-format**: `mvn formatter:validate impsort:check package -Pollama` — this means CI will **fail** if formatting is wrong rather than fixing it. Run `./mvnw clean package` locally to auto-format before pushing.
- **CI uses the `ollama` Maven profile** (`-Pollama`), so tests don't require an OpenAI API key.
- **Prettier runs in check mode**: `npm run prettier:check` for both frontend and whatsapp. Run `npm run prettier:write` locally to fix formatting.

## Architecture: message processing flow

The whatsapp service handles three input types with different processing paths:

**Text messages → async SQS path** (production path):
```
WhatsApp text → whatsapp/src/index.js → SQS (incoming FIFO queue)
                                              ↓ (polled every 5s)
                              timeless-api/SQS.java → LangChain4j AI → DB
                                              ↓
                              SQS (recognized/processed FIFO queue)
                                              ↓
                              whatsapp/src/index.js → reply to user
```

**Audio messages → sync path via Whisper + API**:
```
WhatsApp audio → whatsapp/src/index.js → OpenAI Whisper transcription
                                              ↓
                              POST /api/messages → MessageResource → LangChain4j AI → DB
```

**Image messages → sync path via API**:
```
WhatsApp image → whatsapp/src/index.js → POST /api/messages/image → MessageResource → LangChain4j AI → DB
```

`SQS.java` (`infra/queue/SQS.java`) is the scheduler that drives the async path. It dispatches to two operation types: `ADD_TRANSACTION` and `GET_BALANCE`.

## AI integration

`TextAiService` and `ImageAiService` are LangChain4j `@RegisterAiService` interfaces. The `TextAiService` prompt (in the annotation) defines the full classification logic — it returns a JSON with `operation` and `content`. `ImageAiService` uses the `gpt-4-turbo` model name for vision capabilities. The AI must return one of these operations:

- `ADD_TRANSACTION` — extracts amount, description, type (IN/OUT), category
- `GET_BALANCE` — uses `GetBalanceTool` (a LangChain4j `@Tool`) to query the DB and respond in Portuguese

Categories: `GOALS`, `COMFORT`, `FIXED_COSTS`, `PLEASURES`, `FINANCIAL_FREEDOM`, `KNOWLEDGE`

Note: `Record.Builder` enforces that IN transactions get category `NONE`; null category defaults to `GENERAL`.

## Backend package structure

All Java code lives under `dev.matheuscruz` with three layers:

- `domain/` — JPA entities (`User`, `Record`) and Panache repositories. `User` encrypts `email` and `phoneNumber` at rest via `SecretAttributeConverter` (AES).
- `infra/` — AI services (`ai/`), SQS queue processing (`queue/`), security adapters (`security/`), JPA converters (`persistence/`)
- `presentation/` — JAX-RS resources and request/response DTOs

## Configuration profiles

| Profile | Activated by | Key behavior |
|---|---|---|
| `dev` (default) | `quarkus:dev` | Uses OpenAI, requires `OPENAI_API_KEY` in `application.properties` |
| `local` | `-Dquarkus.profile=local` | Uses Ollama at `localhost:11434`, uses LocalStack for SQS |
| `test` | `@QuarkusTest` | Disables SQS devservices, disables OpenAI integration, disables scheduler |

Maven profiles `openai` (active by default) and `ollama` control which LangChain4j dependency is included. Use `-Pollama` to switch.

## Testing conventions

- Framework: `@QuarkusTest` (RestAssured + real DB, no mocks for persistence)
- Assertions: AssertJ only (`assertThat(...)`)
- Test naming: `should_expectedBehavior_when_stateUnderTest`
- Tests run in integration mode — a real PostgreSQL connection is expected (via Quarkus Dev Services or the test profile config)

## Code style

- Commits: [Conventional Commits](https://www.conventionalcommits.org) format, imperative mood, ≤50 chars on first line
- Java formatting enforced by `impsort` and `formatter` Maven plugins — `./mvnw clean package` applies them automatically
- JS/TS formatting enforced by Prettier — run `npm run prettier:write` before committing

## Key env vars

| Variable | Used by | Notes |
|---|---|---|
| `OPENAI_API_KEY` | timeless-api, whatsapp | Required for OpenAI mode |
| `ALLOWED_PHONE_NUMBERS` | whatsapp `.env.local` | Comma-separated list of allowed numbers |
| `SECURITY_KEY` | timeless-api | AES secret (base64); `%dev` defaults to `YS0xNi1ieXRlLXNlY3JldA==` |
| `JWT_PUBLIC_KEY` / `JWT_PRIVATE_KEY` | timeless-api | SmallRye JWT signing keys |
| `INCOMING_MESSAGE_FIFO_URL` / `RECOGNIZED_MESSAGE_FIFO_URL` | both services | SQS FIFO queue URLs |
