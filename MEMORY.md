# LawraBot - Memoria del Proyecto

> Archivo de referencia rápida con información clave sobre el proyecto.
> Actualizado: 27 de Marzo de 2026

---

## 📋 Resumen del Proyecto

**LawraBot** es un asistente legal automatizado para trámites de divorcio (mutuo acuerdo/unilateral) en Argentina, provincia de Mendoza.

- **Cliente**: Ministerio Público de la Defensa (Mendoza)
- **Canales**: WhatsApp (principal), Telegram
- **Arquitectura**: Dual-service con comunicación MCP

---

## 🏗️ Arquitectura

```
┌─────────────────┐      HTTP/SSE      ┌──────────────────────┐
│  Agent Frontend │  ←────────────→   │  MCP Server (Legal)  │
│   (Node.js/TS)  │      MCP Protocol │   (Java/Spring Boot) │
│   TemplateClaw  │                   │    Port: 8081        │
└────────┬────────┘                   └──────────────────────┘
         │
    ┌────┴────┐
    │ WhatsApp │  ← Baileys library
    │ Telegram │  ← grammY library
    └─────────┘
```

### Stack Tecnológico

| Componente | Tecnología | Puerto |
|------------|------------|--------|
| Agent Frontend | Node.js 22+, TypeScript (ESM) | Variable |
| MCP Server | Java 21, Spring Boot 3.5.12 | 8081 |
| Base de Datos | PostgreSQL 17 + PGVector | 5432 |
| Cache/Sesiones | Redis | 6379 |
| LLM | Ollama Cloud (gpt-oss:120b) | - |

---

## 📁 Estructura de Directorios

```
lawrabot/
├── agent/                          # Frontend del Agente
│   ├── src/
│   │   ├── channels/              # WhatsApp/Telegram adapters
│   │   ├── config/                # Configuración Zod
│   │   ├── gateway/               # HTTP server
│   │   ├── hooks/                 # Sistema de hooks
│   │   ├── llm/                   # Proveedor Ollama
│   │   ├── sessions/              # Persistencia JSONL
│   │   ├── specialization/        # Carga dinámica de specs
│   │   ├── tools/                 # Registro de herramientas
│   │   └── utils/                 # Logger (pino)
│   ├── specs/                     # Especializaciones
│   └── tests/                     # Vitest
│
├── divorce_mcp_server/            # Backend Legal MCP
│   └── src/main/java/
│       └── com.lawrabot.divorce_mcp_server/
│           ├── domain/            # Entidades de dominio
│           │   ├── enums/
│           │   ├── model/
│           │   ├── valueobject/
│           │   └── events/
│           ├── tools/             # @McpTool anotados
│           ├── service/           # Lógica de negocio
│           ├── repository/        # Spring Data JPA
│           └── integration/       # BLSG, Email
│
├── adr/                           # Architecture Decision Records
│   ├── index.md                   # Índice de ADRs (auto-generado)
│   ├── 0001-uso-de-lombok.md      # ADR: Uso de Lombok
│   └── 0002-uso-de-mcp.md         # ADR: Protocolo MCP
│
├── docker-compose.yml             # PostgreSQL, Redis, Adminer
├── mcp_config.json                # Configuración MCP
├── PRD.md                         # Product Requirements
├── TechSpecs.md                   # Especificaciones Técnicas
├── CLAUDE.md                      # Guía para Claude Code
├── MEMORY.md                      # Memoria del proyecto
└── TODO.md                        # Lista de tareas
```

---

## 🔧 Convenciones de Código

### TypeScript (Agente)
- **Módulos**: ESM only, extensiones `.js` en imports
- **Nombres**: kebab-case directorios, camelCase archivos
- **Clases**: PascalCase, funciones: camelCase
- **Logging**: `createLogger("templateclaw")` - NUNCA console.log

### Java (MCP Server)
- **Paquetes**: `com.lawrabot.divorce_mcp_server`
- **Entidades**: Clases de dominio con Lombok
- **Anotaciones**: `@McpTool` para herramientas MCP

### Comentarios
- **Idioma**: Español (obligatorio en todo el código)
- **Javadoc**: Explicar qué hace, no cómo lo hace
- **Lombok**: Documentar cada anotación con su propósito

---

## 🔌 Integraciones Externas

| Servicio | Descripción | Estado |
|----------|-------------|--------|
| **Ollama Cloud** | LLM provider | Configurado |
| **BLSG PJM** | Beneficio de Litigar Sin Gastos | Pendiente implementación |
| **Gmail SMTP** | Envío de emails | Configurado (migrar a institucional) |
| **PostgreSQL** | Base de datos + embeddings | Docker Compose |

---

## 🔄 Estados del Expediente

```
BLSG_PRECONSULTA → IN_DATA_COLLECTION_PROGRESS → DATA_COMPLETE
        ↓                    ↓
   BLSG_RECHAZADO      (más estados por definir)
```

---

## 📝 Decisiones Técnicas Clave

Las decisiones arquitectónicas importantes están documentadas en el registro ADR:

1. **ADR-0001: Lombok** - Reducir código repetitivo en entidades Java
2. **ADR-0002: MCP** - Protocolo HTTP/SSE para comunicación agente-servidor
3. **PGVector**: Para búsqueda semántica en documentación legal
4. **Baileys**: Librería WhatsApp sin API oficial (limitaciones conocidas)

Ver todos los ADRs en: `adr/index.md`

---

## 🐛 Notas Importantes

- **Seguridad**: PII (DNI, nombres) encriptada en reposo
- **Logs**: Nunca loguear DNI o nombres de menores en texto plano
- **BLSG**: Requiere confirmación de credenciales del Poder Judicial de Mendoza

---

## 🔗 Referencias Rápidas

- **PRD**: `PRD.md` - Requerimientos del producto
- **Tech Specs**: `TechSpecs.md` - Especificaciones técnicas detalladas
- **CLAUDE.md**: Guía de desarrollo para Claude Code
- **CODING_STANDARDS.md**: Estándares de código del proyecto
- **ADRs**: `adr/index.md` - Registro de Decisiones de Arquitectura

---

## ✅ Checklist de Desarrollo

- [ ] Verificar `MEMORY.md` actualizado
- [ ] Verificar `TODO.md` con tareas pendientes
- [ ] Tests pasando (`npm run test`, `./mvnw test`)
- [ ] Comentarios en español
- [ ] Lombok aplicado en nuevas entidades Java
