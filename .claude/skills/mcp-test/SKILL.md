---
name: mcp-test
description: Verifica la conectividad entre el Agent Node.js y el MCP Server Java, incluyendo health checks y validación de herramientas
disable-model-invocation: false
---

# MCP Test - Validación de Integración

Este skill verifica que la comunicación entre el Agent y el MCP Server funciona correctamente.

## Verificaciones

1. **Docker Services**
   - PostgreSQL está corriendo en puerto 5433
   - Redis está corriendo en puerto 6379

2. **MCP Server Java**
   - Responde en localhost:8081/actuator/health
   - Estado del servidor MCP

3. **Agent Node.js**
   - Puede conectarse vía SSE al MCP Server
   - Las herramientas están registradas correctamente

## Comandos de Diagnóstico

```bash
# Verificar contenedores Docker
docker compose ps

# Health check MCP Server
curl http://localhost:8081/actuator/health

# Ver logs MCP Server
docker compose logs -f db

# Test de compilación TypeScript (agent)
cd agent && npm run check

# Test de compilación Java (MCP Server)
cd divorce_mcp_server && ./mvnw test
```

## Solución de Problemas

- **MCP Server no responde**: Verifica que Java 21 esté instalado y el puerto 8081 esté libre
- **Agent no conecta**: Verifica `mcp_config.json` y que el MCP Server esté iniciado
- **Errores de BD**: Verifica que PostgreSQL esté corriendo en el puerto 5433 (no 5432)
