# Antigravity Kit Architecture

> Comprehensive AI Agent Capability Expansion Toolkit

---

## 📋 Overview

Antigravity Kit is a modular system consisting of:

- **20 Specialist Agents** - Role-based AI personas
- **36 Skills** - Domain-specific knowledge modules
- **11 Workflows** - Slash command procedures

---

## 🏗️ Directory Structure

```plaintext
.agent/
├── ARCHITECTURE.md          # This file
├── agents/                  # 20 Specialist Agents
├── skills/                  # 36 Skills
├── workflows/               # 11 Slash Commands
├── rules/                   # Global Rules
└── scripts/                 # Master Validation Scripts
```

---

## 🤖 Agents (20)

Specialist AI personas for different domains.

| Agent                    | Focus                      | Skills Used                                              |
| ------------------------ | -------------------------- | -------------------------------------------------------- |
| `orchestrator`           | Multi-agent coordination   | parallel-agents, behavioral-modes                        |
| `project-planner`        | Discovery, task planning   | brainstorming, plan-writing, architecture                |
| `frontend-specialist`    | Web UI/UX                  | frontend-design, react-best-practices, tailwind-patterns |
| `backend-specialist`     | API, business logic        | api-patterns, nodejs-best-practices, database-design     |
| `database-architect`     | Schema, SQL                | database-design, prisma-expert                           |
| `mobile-developer`       | iOS, Android, RN           | mobile-design                                            |
| `game-developer`         | Game logic, mechanics      | game-development                                         |
| `devops-engineer`        | CI/CD, Docker              | deployment-procedures, docker-expert                     |
| `security-auditor`       | Security compliance        | vulnerability-scanner, red-team-tactics                  |
| `penetration-tester`     | Offensive security         | red-team-tactics                                         |
| `test-engineer`          | Testing strategies         | testing-patterns, tdd-workflow, webapp-testing           |
| `debugger`               | Root cause analysis        | systematic-debugging                                     |
| `performance-optimizer`  | Speed, Web Vitals          | performance-profiling                                    |
| `seo-specialist`         | Ranking, visibility        | seo-fundamentals, geo-fundamentals                       |
| `documentation-writer`   | Manuals, docs              | documentation-templates                                  |
| `product-manager`        | Requirements, user stories | plan-writing, brainstorming                              |
| `product-owner`          | Strategy, backlog, MVP     | plan-writing, brainstorming                              |
| `qa-automation-engineer` | E2E testing, CI pipelines  | webapp-testing, testing-patterns                         |
| `code-archaeologist`     | Legacy code, refactoring   | clean-code, code-review-checklist                        |
| `explorer-agent`         | Codebase analysis          | -                                                        |

---

## 🧩 Skills (36)

Modular knowledge domains that agents can load on-demand. based on task context.

### Frontend & UI

| Skill                   | Description                                                           |
| ----------------------- | --------------------------------------------------------------------- |
| `react-best-practices`  | React & Next.js performance optimization (Vercel - 57 rules)          |
| `web-design-guidelines` | Web UI audit - 100+ rules for accessibility, UX, performance (Vercel) |
| `tailwind-patterns`     | Tailwind CSS v4 utilities                                             |
| `frontend-design`       | UI/UX patterns, design systems                                        |
| `ui-ux-pro-max`         | 50 styles, 21 palettes, 50 fonts                                      |

### Backend & API

| Skill                   | Description                    |
| ----------------------- | ------------------------------ |
| `api-patterns`          | REST, GraphQL, tRPC            |
| `nestjs-expert`         | NestJS modules, DI, decorators |
| `nodejs-best-practices` | Node.js async, modules         |
| `python-patterns`       | Python standards, FastAPI      |

### Database

| Skill             | Description                 |
| ----------------- | --------------------------- |
| `database-design` | Schema design, optimization |
| `prisma-expert`   | Prisma ORM, migrations      |

### TypeScript/JavaScript

| Skill               | Description                         |
| ------------------- | ----------------------------------- |
| `typescript-expert` | Type-level programming, performance |

### Cloud & Infrastructure

| Skill                   | Description               |
| ----------------------- | ------------------------- |
| `docker-expert`         | Containerization, Compose |
| `deployment-procedures` | CI/CD, deploy workflows   |
| `server-management`     | Infrastructure management |

### Testing & Quality

| Skill                   | Description              |
| ----------------------- | ------------------------ |
| `testing-patterns`      | Jest, Vitest, strategies |
| `webapp-testing`        | E2E, Playwright          |
| `tdd-workflow`          | Test-driven development  |
| `code-review-checklist` | Code review standards    |
| `lint-and-validate`     | Linting, validation      |

### Security

| Skill                   | Description              |
| ----------------------- | ------------------------ |
| `vulnerability-scanner` | Security auditing, OWASP |
| `red-team-tactics`      | Offensive security       |

### Architecture & Planning

| Skill           | Description                |
| --------------- | -------------------------- |
| `app-builder`   | Full-stack app scaffolding |
| `architecture`  | System design patterns     |
| `plan-writing`  | Task planning, breakdown   |
| `brainstorming` | Socratic questioning       |

### Mobile

| Skill           | Description           |
| --------------- | --------------------- |
| `mobile-design` | Mobile UI/UX patterns |

### Game Development

| Skill              | Description           |
| ------------------ | --------------------- |
| `game-development` | Game logic, mechanics |

### SEO & Growth

| Skill              | Description                   |
| ------------------ | ----------------------------- |
| `seo-fundamentals` | SEO, E-E-A-T, Core Web Vitals |
| `geo-fundamentals` | GenAI optimization            |

### Shell/CLI

| Skill                | Description               |
| -------------------- | ------------------------- |
| `bash-linux`         | Linux commands, scripting |
| `powershell-windows` | Windows PowerShell        |

### Other

| Skill                     | Description               |
| ------------------------- | ------------------------- |
| `clean-code`              | Coding standards (Global) |
| `behavioral-modes`        | Agent personas            |
| `parallel-agents`         | Multi-agent patterns      |
| `mcp-builder`             | Model Context Protocol    |
| `documentation-templates` | Doc formats               |
| `i18n-localization`       | Internationalization      |
| `performance-profiling`   | Web Vitals, optimization  |
| `systematic-debugging`    | Troubleshooting           |

---

## 🔄 Workflows (11)

Slash command procedures. Invoke with `/command`.

| Command          | Description              |
| ---------------- | ------------------------ |
| `/brainstorm`    | Socratic discovery       |
| `/create`        | Create new features      |
| `/debug`         | Debug issues             |
| `/deploy`        | Deploy application       |
| `/enhance`       | Improve existing code    |
| `/orchestrate`   | Multi-agent coordination |
| `/plan`          | Task breakdown           |
| `/preview`       | Preview changes          |
| `/status`        | Check project status     |
| `/test`          | Run tests                |
| `/ui-ux-pro-max` | Design with 50 styles    |

---

## 🎯 Skill Loading Protocol

```plaintext
User Request → Skill Description Match → Load SKILL.md
                                            ↓
                                    Read references/
                                            ↓
                                    Read scripts/
```

### Skill Structure

```plaintext
skill-name/
├── SKILL.md           # (Required) Metadata & instructions
├── scripts/           # (Optional) Python/Bash scripts
├── references/        # (Optional) Templates, docs
└── assets/            # (Optional) Images, logos
```

### Enhanced Skills (with scripts/references)

| Skill               | Files | Coverage                            |
| ------------------- | ----- | ----------------------------------- |
| `ui-ux-pro-max`     | 27    | 50 styles, 21 palettes, 50 fonts    |
| `app-builder`       | 20    | Full-stack scaffolding              |

---

## � Scripts (2)

Master validation scripts that orchestrate skill-level scripts.

### Master Scripts

| Script          | Purpose                                 | When to Use              |
| --------------- | --------------------------------------- | ------------------------ |
| `checklist.py`  | Priority-based validation (Core checks) | Development, pre-commit  |
| `verify_all.py` | Comprehensive verification (All checks) | Pre-deployment, releases |

### Usage

```bash
# Quick validation during development
python .agent/scripts/checklist.py .

# Full verification before deployment
python .agent/scripts/verify_all.py . --url http://localhost:3000
```

### What They Check

**checklist.py** (Core checks):

- Security (vulnerabilities, secrets)
- Code Quality (lint, types)
- Schema Validation
- Test Suite
- UX Audit
- SEO Check

**verify_all.py** (Full suite):

- Everything in checklist.py PLUS:
- Lighthouse (Core Web Vitals)
- Playwright E2E
- Bundle Analysis
- Mobile Audit
- i18n Check

For details, see [scripts/README.md](scripts/README.md)

---

## 📊 Statistics

| Metric              | Value                         |
| ------------------- | ----------------------------- |
| **Total Agents**    | 20                            |
| **Total Skills**    | 36                            |
| **Total Workflows** | 11                            |
| **Total Scripts**   | 2 (master) + 18 (skill-level) |
| **Coverage**        | ~90% web/mobile development   |

---

## 🔗 Quick Reference

| Need     | Agent                 | Skills                                |
| -------- | --------------------- | ------------------------------------- |
| Web App  | `frontend-specialist` | react-best-practices, frontend-design |
| API      | `backend-specialist`  | api-patterns, nodejs-best-practices   |
| Mobile   | `mobile-developer`    | mobile-design                         |
| Database | `database-architect`  | database-design, prisma-expert        |
| Security | `security-auditor`    | vulnerability-scanner                 |
| Testing  | `test-engineer`       | testing-patterns, webapp-testing      |
| Debug    | `debugger`            | systematic-debugging                  |
| Plan     | `project-planner`     | brainstorming, plan-writing           |
The above content shows the entire, complete file contents of the requested file.

## 📋 Conversational Plugin/Skill Expansion – Q&A

### 1. Manejo de versiones del playbook durante un expediente abierto
- **Versión inmóvil por caso**: Al crear el expediente se registra la versión exacta del *playbook* (campo `playbook_version` en la tabla `cases`). Esa versión se utiliza durante toda la sesión, garantizando reproducibilidad.
- **Versionado semántico**: Cada especialidad (`specs/<spoke>-assistant`) lleva su propio `agent.config.json5` con campo `version`. Cuando se publica una nueva versión, se incrementa el número semántico (p.ej., `1.2.0 → 1.3.0`).
- **Migración controlada**: Si se necesita migrar un caso a una versión nueva, se crea una *migration entry* en la tabla `case_playbook_migrations` que indica la versión de origen, la nueva versión y la fecha de migración. El motor de orquestación verifica esta tabla antes de cargar el playbook.

### 2. Snapshot de la configuración para recuperación
- **Persistencia automática**: Al iniciar la conversación, el hub serializa la configuración completa del spoke activo (todo el `agent.config.json5` + `prompts/system.md`) y la guarda en la tabla `playbook_snapshots` (columna `snapshot_json`).
- **Rollback**: En caso de error crítico, el flujo puede revertir cargando el snapshot almacenado y actualizando `cases.playbook_version` al valor del snapshot. Esto garantiza que la lógica de negocio y los prompts vuelvan a un estado probado.

### 3. Almacenamiento y sincronización del registro de spokes activos
- **Fuente única**: El registro principal vive en la base de datos (`spoke_registry`), con columnas `name`, `active`, `version`, `availability_json` y `last_deployed_at`.
- **Manifest JSON**: En el repositorio se mantiene `spokes_manifest.json` (generado por CI) que lista todos los spokes presentes en `specs/` y sus metadatos. Este archivo es fuente de verdad para los despliegues.
- **Sincronización CI/CD**:
  1. **Escaneo**: Un script CI recorre `specs/` y genera `spokes_manifest.json`.
  2. **Comparación**: El script compara el manifest con la tabla `spoke_registry`. Si hay discrepancias (spoke nuevo, eliminado o versión modificada) falla la pipeline.
  3. **Migración automática**: En el paso de despliegue, otro script inserta/actualiza los registros en la base de datos, marcando `active=true` o `false` según la disponibilidad declarada.

### 4. Métricas para detección de fallos en un spoke
- **Excepciones capturadas**: Conteo de eventos `ApiErrorResponse` provenientes de `GlobalExceptionHandler` filtrado por `spoke_id`.
- **Latencia de respuesta**: Histograma de tiempo de procesamiento de cada mensaje (`request_timestamp` → `response_timestamp`). Umbral configurado a **500 ms** para respuestas normales; > 1 s genera alerta.
- **RAG retries**: Número de reintentos del motor RAG por consulta (`rag_retry_count`). Un valor mayor a **3** indica problemas de fuentes o conectividad.
- **Cola de tareas**: Métrica `spoke_task_queue_latency` que mide cuánto tiempo una tarea (p. ej., `submit_marriage.ts`) permanece en la cola antes de ejecutarse.
- **Tasa de fallback**: Porcentaje de interacciones que cayeron al *default router* porque el spoke no estaba disponible.

### 5. Observabilidad del stack
- **Estado actual**: No existe integración formal con Prometheus/Grafana ni Elastic en el proyecto.
- **Propuesta**:
  - **Instrumentación Node.js**: Añadir `prom-client` en el orquestador para exponer métricas `/metrics` (contadores y histogramas de los ítems anteriores).
  - **Instrumentation Java**: Utilizar Micrometer con exportador Prometheus en los servicios Spring Boot (RAG, base de datos, GlobalExceptionHandler).
  - **Dashboards**: Crear paneles Grafana que muestren métricas por spoke, versiones y alertas de umbrales.
  - **Alertas**: Configurar reglas de alerta en Alertmanager para notificar al canal de Slack/Telegram cuando alguna métrica supere los límites definidos.

> **Resultado**: Con este enfoque se garantiza trazabilidad total de la lógica conversacional, permite recuperaciones rápidas ante fallos críticos y brinda visibilidad operativa completa para los equipos de desarrollo y operaciones.
