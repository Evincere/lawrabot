# LawraBot - Memoria del Proyecto

> Archivo de referencia rápida con información clave sobre el proyecto.
> Actualizado: 7 de Mayo de 2026

---

## 📋 Resumen del Proyecto

**LawraBot** es un asistente legal automatizado para trámites de divorcio (mutuo acuerdo/unilateral) en Argentina, provincia de Mendoza. El sistema guía al ciudadano proactivamente para recolectar toda la evidencia digital necesaria antes de la intervención del operador humano.

- **Cliente**: Ministerio Público de la Defensa (Mendoza)
- **Canales**: WhatsApp (principal), Telegram
- **Arquitectura**: Dual-service con comunicación MCP + Admin Dashboard
- **Estrategia**: Recolección Proactiva de Evidencia (DNI, Actas, Ingresos)

---

## 🏗️ Arquitectura

```
┌─────────────────┐      HTTP/SSE      ┌──────────────────────┐
│  Agent Frontend │  ←────────────→   │  MCP Server (Legal)  │
│   (Node.js/TS)  │      MCP Protocol │   (Java/Spring Boot) │
│   TemplateClaw  │                   │    Port: 8081        │
└────────┬────────┘                   └──────────┬───────────┘
         │                                       │
    ┌────┴────┐                         ┌────────┴───────────┐
    │ WhatsApp │  ← Baileys library      │  Admin Dashboard   │
    │ Telegram │  ← grammY library       │   (Next.js/TS)     │
    └─────────┘                         │    Port: 3000      │
                                        └────────────────────┘
```

### Stack Tecnológico

| Componente | Tecnología | Puerto |
|------------|------------|--------|
| Agent Frontend | Node.js 22+, TypeScript (ESM) | Variable |
| MCP Server | Java 21, Spring Boot 3.5.12 | 8081 |
| Admin Dashboard | Next.js 16 (App Router), Tailwind v4 | 3000 |
| Base de Datos | PostgreSQL 17 + PGVector | 5432 |
| Cache/Sesiones | Redis | 6379 |
| LLM | Ollama Cloud (gpt-oss:120b) | - |

---

## 📁 Estructura de Directorios

```
lawrabot/
├── agent/                          # Frontend del Agente (WhatsApp)
├── admin_dashboard/                # Centro de Operaciones (Next.js)
│   ├── app/                        # App Router
│   ├── components/                 # Componentes UI Premium
│   └── lib/                        # Hooks y tipos compartidos
├── divorce_mcp_server/            # Backend Legal MCP (Java)
│   └── src/main/java/
│       └── com.lawrabot.divorce_mcp_server/
│           ├── domain/            # Modelos de Dominio (Expediente, Spouse, Child, etc.)
│           ├── infrastructure/    # Adaptadores (MCP, REST, JPA)
│           └── application/       # Puertos e interfaces
├── adr/                           # Architecture Decision Records
├── docker-compose.yml             # Infraestructura (Postgres, Redis)
├── PRD.md                         # Product Requirements
├── TechSpecs.md                   # Especificaciones Técnicas
├── CLAUDE.md                      # Guía para Claude Code
└── MEMORY.md                      # Memoria del proyecto (este archivo)
```

---

## 🔧 Convenciones de Código

### TypeScript (Agente & Dashboard)
- **Módulos**: ESM only, extensiones `.js` en imports (Agent).
- **Estilos**: Tailwind CSS v4 con variables CSS nativas.
- **Iconos**: Phosphor Icons (weight="duotone").

### Java (MCP Server)
- **Paquetes**: `com.lawrabot.divorce_mcp_server`
- **Lombok**: Se usa en `Expediente` y entidades JPA. 
- **Value Objects**: **REGLA CRÍTICA**: Si un VO (como `CuilVO`) presenta problemas de compilación con Lombok, se debe refactorizar a clase `final` con campos `private final` y getter explícito `getValue()`.

---

## 🔄 Estados y Flujos

### Estados del Expediente
`BLSG_PRECONSULTA` → `IN_DATA_COLLECTION_PROGRESS` → `DATA_COMPLETE` → `READY_FOR_PORTAL`

### Flujo de Evidencia Proactiva
1. **Fase 2**: Solicitud de DNI (Frente/Dorso).
2. **Fase 3**: Comprobantes de ingresos (Bono o Negativa de ANSES).
3. **Fase 4**: Acta de Matrimonio y Nacimiento de hijos.
4. **Validación**: El Operador Humano valida cada documento en el Dashboard.

---

## 💍 Gestión de Matrimonio (Mejoras Mayo 2026)
- **Vínculo Oficial**: `marriageCertificateId` vincula el expediente con el archivo de evidencia validado.
- **Validación Técnica**: Alerta visual si el acta tiene más de 6 meses (`marriageCertificateIssuanceDate`).
- **Datos Registrales**: El operador carga Tomo, Folio, Acta Nº y Oficina para la demanda.

---

## 📝 Decisiones Técnicas Clave
1. **ADR-0001: Lombok** - Reducir boilerplate (pero con fallback a manual en VOs críticos).
2. **ADR-0004: Recolección Proactiva** - El bot solicita archivos sin esperar observaciones.
3. **ADR-0005: Admin Dashboard** - Centro de operaciones para validación humana y HITL.

---

## 📂 Fresh Start (Limpieza Total)
Usa el script `.\cleanup_session.ps1 -All` para:
1. Truncar DB (lawrabot_db).
2. Eliminar archivos `.jsonl` de sesiones.
3. Limpiar memoria del LLM (archivos `.json` en `.data/`).
