# LawraBot - Lista de Tareas

> Tareas pendientes y en progreso del proyecto LawraBot. Actualizado: 2 de Abril de 2026

***

## 🚀 Prioridad Alta

### Domain Model (Java)

- [ ] **Expediente.java** - Entidad principal con Lombok
  - [x] Refactorizar con anotaciones Lombok
  - [x] Agregar comentarios explicativos en español
  - [x] Agregar campos adicionales según PRD (juzgado, fecha presentación, etc.)
- [ ] **Spouse.java** - Entidad de cónyuge
  - [x] Crear clase con Lombok
  - [x] Campos: nombre, apellido, DNI, domicilio, ocupación, email
- [ ] **Matrimonio / Detalles** - Integrado en Expediente
- [ ] **Child.java** - Entidad de hijos/as
  - [x] Crear clase con Lombok
  - [x] Campos: nombre, fechaNacimiento, DNI (opcional)
- [ ] **RegulatoryAgreement.java** - Entidad de acuerdos
  - [x] Crear clase con Lombok
  - [x] Campos: tipoAcuerdo, descripcion, monto (si aplica)
- [ ] **SocioEconomicProfile.java** - Perfil para BLSG
- [ ] **Value Objects**
  - [x] PhoneNumberVO - Validación de números argentinos
  - [x] DNIVO - Validación de DNI argentino
  - [x] AddressVO / FullNameVO / CuilVO

### MCP Tools (Java)

- [ ] **ExpedienteTools \[LISTO]**
  - [x] `start_divorce_process` - Iniciar trámite
  - [x] `get_dossier_stage` - Consultar etapa actual
  - [x] `validate_agreement_legality` - Validación legal (Sanity Check)
- [ ] **Data Collection Tools \[LISTO]**
  - [x] `submit_marriage_details` - Registro de matrimonio
  - [x] `submit_children_info` - Registro de hijos
  - [x] `submit_socioeconomic_info` - Info para BLSG
  - [x] `draft_regulatory_agreement` - Borrador de acuerdo
- [ ] **BLSG Integration \[LISTO]**
  - [x] `consultar_blsg` - Consultar Beneficio de Litigar Sin Gastos
  - [x] Implementar Playwright para scraping de blsg.pjm.gob.ar (Federated Auth)
  - [x] Modelar respuesta de BLSG (Aprobado/Rechazado/Inconcluso)
  - [x] Descarga automática de constancia PDF
- [ ] **Document Generation**
  - [ ] `generar_convenio` - Generar Word .docx (divorcio mutuo acuerdo)
  - [ ] `generar_demanda` - Generar Word .docx (divorcio unilateral)
  - [ ] Integrar Apache POI o Docx4j
- [ ] **RAG Tools**
  - [ ] `consultar_normativa` - Buscar en documentación legal con embeddings
  - [ ] Configurar PGVector para almacenamiento de vectores
  - [ ] Cargar documentación legal de Mendoza

### Agent Frontend (TypeScript)

- [ ] **Specialization: divorce-assistant**
  - [ ] Crear spec en `agent/specs/divorce-assistant/`
  - [ ] `agent.config.json5` - Configuración
  - [ ] `prompts/system.md` - Prompt del sistema
  - [ ] Definir flujo de conversación (bienvenida → BLSG → recolección datos)
- [ ] **Channel Adapters**
  - [ ] WhatsApp (Baileys) - Refinar manejo de mensajes
  - [ ] Telegram (grammY) - Implementar adapter
- [ ] **Session Management**
  - [ ] Persistencia JSONL para historial
  - [ ] Recuperación de sesiones por número de teléfono

***

## 📋 Prioridad Media

### Testing

- [ ] **Unit Tests (Java)**
  - [ ] ExpedienteTest - State machine transitions
  - [ ] DNIVOTest - Validación de DNI
  - [ ] PhoneNumberVOTest - Validación de teléfono
- [ ] **Unit Tests (TypeScript)**
  - [ ] Session persistence tests
  - [ ] Tool registry tests
  - [ ] LLM provider tests (mock)
- [ ] **Integration Tests**
  - [ ] MCP Server + PostgreSQL
  - [ ] Agent + MCP Server
  - [ ] BLSG scraping (con mocks)

### Infrastructure

- [ ] **Docker**
  - [x] PostgreSQL 17 + PGVector
  - [x] Redis
  - [x] Adminer (UI DB)
  - [ ] MCP Server containerizado
  - [ ] Agent containerizado
- [ ] **CI/CD**
  - [ ] GitHub Actions para Java (Maven)
  - [ ] GitHub Actions para Node.js (npm)
  - [ ] Tests automáticos en PR

### Documentación

- [x] **README.md** - Instrucciones de setup
- [x] **API Documentation** - MCP Tools (OpenAPI/Swagger)
- [x] **User Guide** - Cómo usar el bot (para usuarios finales)
- [x] **ADRs** - Mantener registro de decisiones arquitectónicas
  - [x] Configurar adr-log para actualización automática de index.md
  - [x] Crear ADR-0003: Arquitectura reactiva (WebFlux/ASYNC) para estabilidad MCP
  - [ ] Crear ADR-0004: Base de datos (PostgreSQL + PGVector)
  - [ ] Crear ADR-0005: Framework de testing (JUnit 5 + Vitest)
  - [ ] Crear ADR-0006: Estrategia de RAG para normativa legal

***

***

## 🏛️ Fase 2: MCI & Operations Center (EN PROGRESO)

### Master Client Index (Java)

- [x] **Citizen.java** - Entidad de identidad única (DNI/CUIL)
- [x] **CaseParticipant.java** - Mapeo de ciudadanos a expedientes (Roles)
- [x] **MciTools.java** - Herramientas MCP para búsqueda y gestión de ciudadanos
- [x] **MciRestController.java** - API REST para búsqueda de ciudadanos y feedback

### Learning Loop (Java/Agent)

- [x] **CorrectionFeedback.java** - Entidad para registrar correcciones del operador
- [ ] **Extraction Prompt** - Inyectar Few-Shot examples desde el repositorio de feedback

### Operations Center (Next.js)

- [x] **Design Archetype** - Implementar Ethereal Glass / Double-Bezel
- [x] **Divorce Workspace** - Layout de 3 paneles (Casos, Chat, Expediente)
- [x] **Correction Modal** - Interfaz para registrar feedback del Learning Loop
- [ ] **Divorce API Integration** - Cargar expedientes reales desde el backend
- [x] **MCI Explorer** - Interfaz de búsqueda de ciudadanos

***

## 🔮 Prioridad Baja / Futuro

- [x] **BLSG Scraping** - Implementado exitosamente con Playwright y autenticación federada.
- [ ] **Baileys** - WhatsApp puede requerir re-autenticación periódica
- [ ] **Memory** - Sesiones largas pueden consumir mucho contexto LLM

***

## ✅ Completado Recientemente

- [x] Estabilizar null-safety en Expediente y Mappers (Fase 3a)
- [x] Implementar servidor MCP con transporte SSE en Java (Fase 3b)
- [x] Exponer 8 UseCases legales como herramientas MCP (Fase 3c)
- [x] Validar conectividad E2E con cliente de prueba Node (Fase 3d)
- [x] Corregir tipos de datos y corrupción en controlador MCP (Fase 3e)
- [x] Refactorizar a WebFlux/ASYNC para eliminar timeouts de 60s (Fase 4 - Estabilización de Conectividad)

***

## 📝 Notas

### Próximos Pasos Inmediatos

1. Crear entidades restantes del domain model (Conyuge, Matrimonio, Hijo, Acuerdo)
2. Implementar Value Objects (DNIVO, PhoneNumberVO)
3. Crear primer MCP Tool: `crear_expediente`
4. Configurar especialización del agente

### Bloqueantes

- Ninguno actualmente

### Recursos Externos Necesarios

- Acceso/credenciales para BLSG PJM (pendiente confirmación)
- SMTP institucional (actualmente usando Gmail)

***

## 📊 Estadísticas

- **Entidades Java creadas**: 6/6 (Completas)
- **MCP Tools implementados**: 12/12 (Base legal y BLSG listos)
- **Specs del agente**: 1/1 (divorce-assistant funcional)
- **Tests escritos**: 5/ (Mappers + Mocks)

***

Última actualización por: Claude - Fecha: 2026-03-27
