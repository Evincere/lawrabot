# 3. Arquitectura MCP Reactiva (WebFlux + ASYNC) para Estabilización de SSE

**Fecha**: 2026-03-29

**Estado**: Aceptado

## Contexto

Durante las pruebas de estrés con el cliente de WhatsApp, se identificaron **timeouts de 60 segundos** en las herramientas de guardado de datos (`submit_marriage_details`, `submit_children_info`). 

El diagnóstico técnico reveló que la arquitectura original basada en `Spring WebMVC` (Tomcat) y el modo `SYNC` del servidor MCP causaban un bloqueo del hilo de ejecución. El stream SSE (Server-Sent Events) mantenía ocupado el pool de hilos, impidiendo que las peticiones POST de ejecución de herramientas se procesaran de forma concurrente para la misma sesión.

## Decisión

Se decide migrar el **Backend Legal (Java)** de un stack bloqueante a un **stack reactivo no bloqueante** para garantizar la estabilidad del protocolo MCP sobre SSE.

### Cambios Arquitectónicos
1. **Framework de Red**: Migración de `spring-boot-starter-web` (Tomcat) a `spring-boot-starter-webflux` (Netty).
2. **Servidor MCP**: Actualización a `spring-ai-starter-mcp-server-webflux`.
3. **Modo de Procesamiento**: Cambio de `spring.ai.mcp.server.type=SYNC` a **`ASYNC`**.

## Consecuencias

### Positivas
- **Eliminación de Timeouts**: Netty permite manejar miles de conexiones SSE concurrentes sin bloquear hilos del sistema.
- **Baja Latencia**: El modo `ASYNC` de Spring AI permite que el servidor responda inmediatamente al agente mientras la lógica de persistencia se ejecuta.
- **Preparación para RAG**: La arquitectura reactiva es ideal para integrar procesos de búsqueda vectorial (PGVector) que pueden ser intensivos en E/S.

### Negativas
- **Curva de Aprendizaje**: El paradigma reactivo (Project Reactor / Mono / Flux) es más complejo de depurar.
- **Incompatibilidad**: Algunas librerías tradicionales de Java (JDBC estándar) no son reactivas, lo que requiere cuidado con los hilos de bloqueo de base de datos (se usa el pool de Hikari optimizado para contrarrestar esto).

## Referencias

- [Spring WebFlux Documentation](https://docs.spring.io/spring-framework/reference/web/webflux.html)
- [Spring AI MCP - WebFlux Support](https://docs.spring.io/spring-ai/reference/api/mcp.html#_webflux_server)
- [Issue Log #042 - 60s SSE Timeout Diagnostics]
