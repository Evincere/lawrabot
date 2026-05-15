---
name: start-session
description: Inicia toda la infraestructura de desarrollo de LawraBot en el orden correcto (Docker, MCP Server, Agent)
disable-model-invocation: false
---

# Start Session - Inicio de Desarrollo LawraBot

Este skill inicia todos los servicios necesarios para desarrollar LawraBot en el orden correcto.

## Secuencia de Inicio

1. **Infraestructura Docker** (PostgreSQL, Redis, Adminer, RedisInsight)
   ```bash
   docker compose up -d
   ```

2. **Esperar** 5 segundos para que PostgreSQL esté listo

3. **MCP Server Java** (puerto 8081)
   ```bash
   cd divorce_mcp_server && ./mvnw spring-boot:run
   ```

4. **Agent Node.js** (con spec divorce-assistant)
   ```bash
   cd agent && npm run dev -- --spec ./specs/divorce-assistant
   ```

## Verificación de Salud

- PostgreSQL: localhost:5433
- Redis: localhost:6379
- MCP Server: localhost:8081/actuator/health
- Agent: Depende del canal configurado (WhatsApp/Telegram)

## Notas

- El MCP server requiere Java 21 y Maven
- El Agent requiere Node.js 22+
- Asegúrate de tener las variables de entorno configuradas en `.env`
