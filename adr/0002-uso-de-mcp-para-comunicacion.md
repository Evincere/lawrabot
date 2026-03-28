# 2. Uso de Model Context Protocol (MCP) para Comunicación Agente-Servidor

**Fecha**: 2026-03-24

**Estado**: Aceptado

## Contexto

LawraBot requiere comunicación entre:
- **Agent Frontend** (Node.js/TypeScript): Maneja WhatsApp/Telegram
- **Backend Legal** (Java/Spring Boot): Lógica de dominio, generación de documentos

Se necesita un protocolo estándar para:
- Exponer herramientas (tools) del backend al agente
- Manejar llamadas síncronas con parámetros tipados
- Permitir evolución independiente de ambos servicios

## Decisión

Se adopta **Model Context Protocol (MCP)** de Anthropic para la comunicación entre agente y servidor legal.

### Modo de Transporte
- **HTTP/SSE (Sincrónico)**: Para llamadas request/response inmediatas
- Puerto del MCP Server: **8081**

## Características del Protocolo

```
Agente (TypeScript)                    MCP Server (Java)
     │                                         │
     │ POST /mcp/v1/tools/call                 │
     │ { "tool": "crear_expediente",           │
     │   "params": {...} }                     │
     │────────────────────────────────────────→│
     │                                         │
     │         { "result": {...} }             │
     │←────────────────────────────────────────│
```

## Ventajas de MCP

1. **Estandarización**: Protocolo abierto, documentado
2. **Descubrimiento**: El agente puede listar herramientas disponibles
3. **Tipado**: Parámetros y respuestas con esquema definido
4. **Extensibilidad**: Nuevas herramientas sin cambios en protocolo

## Consecuencias

### Positivas
- Separación clara de responsabilidades
- Agente puede funcionar con diferentes backends MCP
- Testing independiente por módulos

### Negativas
- Latencia adicional por HTTP
- Acoplamiento temporal (servidor debe estar disponible)
- Complejidad de configuración inicial

## Implementación

- **Spring AI MCP**: `spring-ai-starter-mcp-server-webmvc`
- **Configuración**: `mcp_config.json` en raíz del proyecto
- **Anotación**: `@McpTool` para exponer métodos como herramientas

## Referencias

- [MCP Specification](https://modelcontextprotocol.io/)
- [Spring AI MCP](https://docs.spring.io/spring-ai/reference/api/mcp.html)
- [TechSpecs.md](../TechSpecs.md) - Sección "MCP Protocol"
