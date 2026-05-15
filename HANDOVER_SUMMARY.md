# LawraBot: Final Session Reference & Handover

This document summarizes the current state of the LawraBot repository as of April 6, 2026, after a comprehensive stabilization and optimization session.

## 1. Backend: Core Improvements (Spring Boot)
- **Null Safety**: All major "Potential null pointer access" warnings in `DivorceRestController` and `SpouseMapper` were resolved using safe access patterns and local variable capture.
- **Socio-Economic Profile**: 
    - Added `occupation` (String) field to `SocioEconomicProfileJpaEntity`.
    - Updated `SocioEconomicProfile` domain model and `SocioEconomicProfileDTO`.
    - Integrated `occupation` in `SubmitSocioEconomicEvaluationService` and documentation drafting logic.
- **Document Generation**: Context expanded for `DraftingRestController`. Any future `.docx` template can now use `${peticionante_ocupacion}`, `${peticionante_ingresos}`, and `${peticionante_vivienda}`.

## 2. Frontend: Dashboard Optimization (Next.js)
- **Modular Tabs**: The dashboard now uses an `activeTab` pattern (profile, evidence, agreement, process, history) for consistent navigation.
- **Type Safety**: Replaced multiple `any` types with specialized interfaces (`Expediente`, `StatusEnum`, etc.).
- **Performance**: Migrated all instances of `<img>` to Next.js `<Image />` components using the `unoptimized` flag (for localhost/external blob storage compatibility).
- **Cleanup**: Removed unused states like `openCorrection` and `correctionField`.

## 3. Infrastructure: Docker & Connectivity
- **PostgreSQL**: Running via Docker on **port 5433**. Mapping is strictly `0.0.0.0:5433 -> 5432/tcp`.
- **Redis**: Running via Docker on **port 6379**.
- **Agent Connectivity**: The WhatsApp node agent (`divorce-assistant` spec) was updated to use `127.0.0.1:8081` instead of `localhost` to avoid IPv6 resolution issues (`ECONNREFUSED ::1`).

## 4. Key Configuration Files
- **Backend Properties**: [application.properties](file:///b:/CODE/PROYECTOS/lawrabot/divorce_mcp_server/src/main/resources/application.properties) (contains DB secrets and MCP paths).
- **Docker Compose**: [docker-compose.yml](file:///b:/CODE/PROYECTOS/lawrabot/docker-compose.yml) (Postgres, Redis, Adminer, RedisInsight).
- **Agent Config**: [agent.config.json5](file:///b:/CODE/PROYECTOS/lawrabot/agent/specs/divorce-assistant/agent.config.json5) (MCP SSE endpoint pointer).

---

> [!IMPORTANT]
> To start the system in a clean state:
> 1. Run `docker-compose down; docker-compose up -d` in the root.
> 2. Run `./mvnw.cmd spring-boot:run` in `divorce_mcp_server`.
> 3. Run `npm run dev` in `admin_dashboard`.
