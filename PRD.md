# LawraBot - Product Requirements Document (PRD)

## Objetivo
Automatizar la recolección de datos y documentos para divorcios en la Defensa Pública de Mendoza, reduciendo el tiempo de atención presencial y asegurando expedientes completos y validados.

## Usuarios
1. **Ciudadano**: Interactúa vía WhatsApp para cargar datos y documentos.
2. **Operador Humano**: Valida la información en el Admin Dashboard y genera la demanda.

## Requerimientos Clave (MVP+)
- [x] **Recolección Proactiva**: El bot guía al usuario para subir DNI y actas sin intervención humana.
- [x] **Validación de Identidad**: Interfaz para comparar DNI vs datos declarados.
- [x] **Validación de Matrimonio**: Control técnico de actas (antigüedad, datos registrales).
- [ ] **Evaluación BLSG**: Integración del resultado de scraping con la revisión manual de ingresos.
- [ ] **Generación Documental**: Creación automática de PDF/Docx basada en datos consolidados.

---

# LawraBot - TODO List

## 🟢 Prioridad Alta (Operaciones)
- [x] Implementar reset de scroll en el Dashboard.
- [x] Habilitar carga de datos registrales en pestaña Matrimonio.
- [x] Implementar selector de Acta Oficial.
- [x] Refactorizar VOs para estabilidad de compilación (CuilVO).
- [ ] **Siguiente**: Refactorizar Pestaña "Evaluación BLSG" para permitir la revisión de ingresos proactivos.

## 🟡 Prioridad Media (Funcionalidad)
- [ ] Implementar visor de "Convenio Regulador" basado en los acuerdos del bot.
- [ ] Generar vista previa de la demanda (Markdown o PDF).

## ⚪ Backlog
- [ ] Integración de firma digital.
- [ ] Canal Telegram.
