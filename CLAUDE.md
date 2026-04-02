# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**LawraBot** is a legal assistant for divorce procedures (mutuo acuerdo/unilateral) in Argentina (Mendoza province), operating via WhatsApp for the Ministerio Público de la Defensa. The system uses a dual-service architecture communicating via Model Context Protocol (MCP).

**Architecture:**
- **Agent Frontend** (`agent/`): Node.js/TypeScript agent using TemplateClaw framework; handles WhatsApp (Baileys) and Telegram (grammY) channels
- **Backend Legal MCP Server** (`divorce_mcp_server/`): Java/Spring Boot + Spring AI; provides MCP tools for legal document generation, BLSG integration, and RAG
- **Infrastructure**: PostgreSQL 17 + PGVector (RAG embeddings), Redis (sessions), Docker Compose

**External Integrations:**
- **Ollama Cloud** (`https://ollama.com`): LLM provider via OpenAI-compatible API
- **BLSG PJM** (`blsg.pjm.gob.ar`): Browser automation (Playwright) for Beneficio de Litigar Sin Gastos queries
- **Gmail SMTP**: Email confirmations (migrating to institutional SMTP in future)

## Common Commands

### Infrastructure (Docker)
```bash
# Start PostgreSQL, Redis, Adminer, RedisInsight
docker compose up -d

# View logs
docker compose logs -f db
docker compose logs -f redis
```

### Agent Frontend (Node.js)
Working directory: `agent/`

```bash
# Development
npm run dev -- --spec ./specs/<specialization-name>

# Build for production
npm run build        # Outputs to ./dist using tsdown (ESM)
npm run start          # Run built output

# Testing
npm run test           # Run all Vitest suites
npm run test -- <pattern>      # Run tests matching pattern
npm run test -- src/utils/logger.test.ts  # Run single test file
npx vitest run -t "test name"   # Run test by name

# Type checking and linting
npm run check          # TypeScript --noEmit (strict mode)
npm run lint           # oxlint on src/

# Validate a specialization without starting
npx tsx src/index.ts validate --spec ./specs/<name>
```

**Runtime Requirements:** Node.js 22+, ESM only

### MCP Server (Java/Spring Boot)
Working directory: `divorce_mcp_server/`

```bash
# Development
./mvnw spring-boot:run        # Starts on port 8081

# Testing
./mvnw test
./mvnw test -Dtest=ClassName#methodName

# Build
./mvnw clean package          # Creates JAR in target/

# Windows (cmd)
mvnw.cmd spring-boot:run
```

**Runtime Requirements:** Java 21 (OpenJDK), Maven (wrapper included)

## Architecture Details

### Agent Frontend Structure (`agent/src/`)

```
src/
├── channels/           # Messaging adapters (WhatsApp via Baileys, Telegram via grammY)
├── config/             # Zod-validated configuration loading
├── gateway/            # HTTP server and message routing
├── hooks/              # Lifecycle hook system (priority-based)
├── llm/                # Ollama LLM provider with tool-calling
├── sessions/           # Per-conversation history with JSONL persistence
├── specialization/     # Dynamic spec loading (prompts + tools)
├── tools/              # Tool registry and built-in tools
└── utils/              # Logger (pino), env helpers
```

**Specialization Structure:**
```
specs/my-spec/
├── agent.config.json5    # Zod-validated config (name, channels, LLM)
├── prompts/
│   └── system.md         # System prompt / personality
└── tools/
    └── my-tool.ts        # AgentTool exports
```

**Key Conventions:**
- ESM only with `.js` extensions in imports: `import { foo } from "./utils.js"`
- File naming: kebab-case directories, camelCase files
- Classes: PascalCase, Functions: camelCase, Constants: camelCase (objects), UPPER_CASE (primitives)
- Logging: Use `createLogger("templateclaw")` — never `console.log`

### MCP Server Structure (`divorce_mcp_server/src/main/java/`)

```
com.lawrabot.divorce_mcp_server/
├── DivorceMcpServerApplication.java    # Entry point
├── infrastructure/mcp/      # Implementación del servidor MCP (Controller, Config, DTOs)
├── domain/                  # Entidades de dominio (Expediente, Spouse, Child, etc.)
├── application/port/        # Puertos de entrada y salida (Use Cases)
├── repository/             # Spring Data JPA repositories
├── service/                # Business logic (Implementación de Use Cases)
├── integration/            # HTTP clients (Email)
└── config/                 # Bean configuration
```

**Planned MCP Tools:**
- **Expediente [IMPLEMENTADO]**: `start_divorce_process`, `get_dossier_stage`, `validate_agreement_legality`
- **Data Collection [IMPLEMENTADO]**: `submit_marriage_details`, `submit_children_info`, `submit_socioeconomic_info`, `draft_regulatory_agreement`
- **BLSG Integration [PARCIAL]**: `process_scraping_result` (scraping real pendiente via Playwright)
- **PDF Processing**: `procesar_documento_pdf` (pendiente)
- **Document Generation**: `generar_convenio`, `generar_demanda` (Apache POI/Docx4j pendiente)
- **RAG**: `consultar_normativa` (PGVector configurado, búsqueda por implementar)
- **Observations**: `registrar_observacion`, `obtener_observaciones_pendientes` (pendiente)

**Database Entities:**
- `Expediente` (case file with states: en_progreso → datos_completos → blsg_consultado → documentos_generados → en_revision → presentado → en_tramite)
- `Conyuge`, `Matrimonio`, `Hijo`, `Acuerdo`
- `ConstanciaBLSG`, `DocumentoDigital`, `DocumentoGenerado`, `Observacion`

### Communication Protocol (MCP)

- **Transport**: HTTP/SSE (Synchronous)
- **MCP Server Port**: 8081
- **Configuration**: `mcp_config.json` at project root

## Development Environment

### Prerequisites
- Java 21 (OpenJDK)
- Node.js 22+ LTS
- Docker Desktop
- Git

### Startup Sequence
1. Infrastructure: `docker compose up -d` (PostgreSQL on 5432, Redis on 6379, Adminer on 8082, RedisInsight on 8001)
2. MCP Server: `cd divorce_mcp_server && ./mvnw spring-boot:run` (port 8081)
3. Agent: `cd agent && npm run dev -- --spec ./specs/<name>`

### Database Access
- **Adminer**: http://localhost:8082 (PostgreSQL, user: postgres, password: postgres_password)
- **RedisInsight**: http://localhost:8001

### Environment Variables (`.env` at project root)
```bash
OLLAMA_CLOUD_API_KEY=<token>    # For Ollama Cloud LLM access
DB_PASSWORD=postgres_password   # PostgreSQL password
SPRING_PROFILES_ACTIVE=dev    # Spring profile
POSTGRES_DB=lawrabot_db
POSTGRES_USER=postgres
```

## Key Technical Decisions

- **TypeScript**: ES2023 target, NodeNext module resolution, strict mode enabled
- **Java**: Spring Boot 3.5.12, Spring AI 1.1.3, synchronous MCP server
- **Database**: PostgreSQL 17 with PGVector extension for legal document embeddings
- **LLM**: Ollama Cloud via OpenAI-compatible adapter (model: gpt-oss:120b)
- **BLSG**: Browser automation required (Playwright for Java) — no public API available
- **Document Generation**: Apache POI/Docx4j TBD for Word document generation
- **Email**: Gmail SMTP with App Password (migrating to institutional SMTP)
- **MCP Config**: Located at project root (`mcp_config.json`)

## Security Notes

- Secrets stored in `.env` only — never commit to Git
- PII encrypted at rest; logs must not contain plain DNI/names of minors
- MCP server listens on localhost only in MVP (not exposed to internet)
- BLSG access requires confirmation of credentials/permissions from Poder Judicial de Mendoza
