# LawraBot - Lista de Tareas

> Tareas pendientes y en progreso del proyecto LawraBot.
> Actualizado: 27 de Marzo de 2026

---

## 🚀 Prioridad Alta

### Domain Model (Java)
- [x] **Expediente.java** - Entidad principal con Lombok
  - [x] Refactorizar con anotaciones Lombok
  - [x] Agregar comentarios explicativos en español
  - [ ] Agregar campos adicionales según PRD (juzgado, fecha presentación, etc.)

- [ ] **Conyuge.java** - Entidad de cónyuge
  - [ ] Crear clase con Lombok
  - [ ] Campos: nombre, apellido, DNI, domicilio, ocupación, email
  - [ ] Relación con Expediente

- [ ] **Matrimonio.java** - Entidad de matrimonio
  - [ ] Crear clase con Lombok
  - [ ] Campos: fechaMatrimonio, lugarMatrimonio, regimenMatrimonial
  - [ ] Relación con Expediente

- [ ] **Hijo.java** - Entidad de hijos/as
  - [ ] Crear clase con Lombok
  - [ ] Campos: nombre, fechaNacimiento, DNI (opcional)
  - [ ] Relación con Expediente

- [ ] **Acuerdo.java** - Entidad de acuerdos
  [ ] Crear clase con Lombok
  - [ ] Campos: tipoAcuerdo, descripcion, monto (si aplica)
  - [ ] Relación con Expediente

- [ ] **Value Objects**
  - [ ] PhoneNumberVO - Validación de números argentinos
  - [ ] DNIVO - Validación de DNI argentino
  - [ ] MoneyVO - Para montos de dinero

### MCP Tools (Java)
- [ ] **ExpedienteTools**
  - [ ] `crear_expediente` - Crear nuevo expediente post-BLSG
  - [ ] `obtener_estado_caso` - Consultar estado actual
  - [ ] `obtener_datos_faltantes` - Validar datos incompletos

- [ ] **Data Collection Tools**
  - [ ] `registrar_datos_conyuge` - Guardar datos de cónyuges
  - [ ] `registrar_datos_matrimonio` - Guardar datos del matrimonio
  - [ ] `registrar_datos_hijos` - Guardar datos de hijos/as
  - [ ] `registrar_acuerdos` - Guardar acuerdos entre partes

- [ ] **BLSG Integration**
  - [ ] `consultar_blsg` - Consultar Beneficio de Litigar Sin Gastos
  - [ ] Implementar Playwright para scraping de blsg.pjm.gob.ar
  - [ ] Modelar respuesta de BLSG (activo/inactivo/no encontrado)

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

---

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
- [ ] **ADRs** - Mantener registro de decisiones arquitectónicas
  - [x] Configurar adr-log para actualización automática de index.md
  - [ ] Crear ADR-0003: Base de datos (PostgreSQL + PGVector)
  - [ ] Crear ADR-0004: Framework de testing (JUnit 5 + Vitest)
  - [ ] Crear ADR-0005: Estrategia de RAG para normativa legal

---

## 🔮 Prioridad Baja / Futuro

### Features Adicionales
- [ ] **Observaciones** - `registrar_observacion`, `obtener_observaciones_pendientes`
- [ ] **PDF Processing** - `procesar_documento_pdf` (OCR para documentos subidos)
- [ ] **Notificaciones** - Recordatorios de vencimientos
- [ ] **Dashboard** - Panel web para administradores
- [ ] **Analytics** - Métricas de uso del bot

### Mejoras Técnicas
- [ ] **Rate Limiting** - Protección contra abuso
- [ ] **i18n** - Soporte multiidioma (español formal vs informal)
- [ ] **A/B Testing** - Probar diferentes flujos de conversación
- [ ] **Caching** - Cache de respuestas MCP

---

## 🐛 Issues Conocidos / Bugs

- [ ] **BLSG Scraping** - Necesita confirmar que el sitio permite scraping
- [ ] **Baileys** - WhatsApp puede requerir re-autenticación periódica
- [ ] **Memory** - Sesiones largas pueden consumir mucho contexto LLM

---

## ✅ Completado Recientemente

- [x] Configurar Lombok en pom.xml
- [x] Crear estructura base del proyecto
- [x] Configurar Docker Compose (PostgreSQL, Redis)
- [x] Crear Expediente.java con Lombok
- [x] Documentar anotaciones Lombok en español
- [x] Configurar sistema de ADRs con adr-log
- [x] Crear ADR-0001: Uso de Lombok
- [x] Crear ADR-0002: Uso de MCP

---

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

---

## 📊 Estadísticas

- **Entidades Java creadas**: 1/6
- **MCP Tools implementados**: 0/12
- **Specs del agente**: 0/1
- **Tests escritos**: 0/

---

*Última actualización por: Claude*
*Fecha: 2026-03-27*
