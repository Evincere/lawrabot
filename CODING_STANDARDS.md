# Coding Standards — LawraBot

> **Estado:** Activo
> **Versión:** 1.0
> **Aplicación:** Todo código Java (MCP Server) y TypeScript (Agent)
> **Idioma Técnico:** Inglés
> **Idioma Usuario:** Español (solo en mensajes, prompts, UI)

---

## 1. Principios Generales

1. **Código en inglés**: Nombres de clases, métodos, variables, paquetes → inglés
2. **Mensajes al usuario en español**: WhatsApp, emails, prompts del sistema → español
3. **Comentarios en español**: Las explicaciones, documentación Javadoc y comentarios internos → español.
4. **Claridad sobre brevedad**: Nombres descriptivos, no abreviaturas crípticas
5. **Consistencia vertical**: Un concepto = una palabra en toda la codebase
6. **Arquitectura Hexagonal**: Separar dominio de infraestructura

---

## 2. Estructura de Paquetes (Hexagonal)

```
com.lawrabot.divorce_mcp_server/
├── domain/                    # Core puro - NO dependencias de frameworks
│   ├── model/                 # Entities
│   ├── valueobject/           # Value Objects (VO suffix)
│   ├── service/               # Domain Services
│   ├── repository/            # Repository interfaces (Port)
│   ├── exception/             # Domain exceptions
│   └── enums/                 # Enums (Enum suffix)
│
├── application/               # Casos de uso
│   ├── port/
│   │   ├── in/                # Driven Ports (entrada)
│   │   └── out/               # Driving Ports (salida)
│   ├── service/               # Application Services
│   └── dto/                   # Data Transfer Objects
│
├── infrastructure/            # Adaptadores tecnológicos
│   ├── persistence/           # JPA, PostgreSQL
│   ├── integration/           # BLSG, Email, LLM
│   ├── web/                   # Controllers REST, MCP Tools
│   └── config/                # Spring Beans
```

---

## 3. Convenciones de Nomenclatura

### 3.1 General

| Elemento | Convención | Ejemplo |
|----------|------------|---------|
| **Packages** | lowercase, sin guiones | `com.lawrabot.divorce_mcp_server.domain.model` |
| **Classes** | PascalCase, sustantivos | `Expediente`, `Conyuge`, `BLSGConsultationService` |
| **Interfaces** | PascalCase, prefijo `I` | `IExpedienteRepository`, `IBLSGPort` |
| **Implementations** | PascalCase, sufijo `Impl` | `ExpedienteRepositoryImpl`, `BLSGPlaywrightImpl` |
| **Enums** | PascalCase, sufijo `Enum`, valores UPPER_SNAKE | `ExpedienteStatusEnum.BLSG_PRECONSULTA` |
| **Value Objects** | PascalCase, sufijo `VO` | `DNIVO`, `AddressVO`, `CuilVO` |
| **Methods** | camelCase, verbos infinitivo | `crearExpediente()`, `consultarBLSG()` |
| **Variables** | camelCase, descriptivas | `idExpediente`, `fechaMatrimonio` |
| **Constants** | UPPER_SNAKE_CASE | `MAX_PDF_SIZE_MB`, `BLSG_TIMEOUT_MS` |
| **Tests** | Clase + `Test` suffix | `ExpedienteTest`, `BLSGServiceTest` |

### 3.2 Booleanos

Usar prefijos que revelen intención:

| Prefijo | Ejemplo |
|---------|---------|
| `is` / `es` | `isValid()`, `isComplete()` |
| `has` / `tiene` | `hasChildren()`, `hasAttachments()` |
| `can` / `puede` | `canAccess()`, `canProceed()` |
| `should` / `debe` | `shouldValidate()`, `shouldNotify()` |

### 3.3 Sufijos por Capa

| Tipo | Sufijo | Ejemplo |
|------|--------|---------|
| Repository (interface) | `I` + nombre | `IExpedienteRepository` |
| Repository (impl) | `Impl` | `ExpedienteRepositoryImpl` |
| Service (domain) | `DomainService` | `ValidationDomainService` |
| Service (application) | `Service` | `CreateExpedienteService` |
| Adapter (infrastructure) | `Adapter` | `BLSGPlaywrightAdapter` |
| Client (infrastructure) | `Client` | `EmailSMTPClient` |
| DTO Request | `Request` | `CreateExpedienteRequest` |
| DTO Response | `Response` | `ExpedienteResponse` |
| Exception | `Exception` | `BLSGConsultationException` |
| Entity (JPA) | `Entity` | `ExpedienteEntity` |
| Value Object | `VO` | `DNIVO` |
| Enum | `Enum` | `ExpedienteStatusEnum` |

---

## 4. Entidades vs Entidades JPA

**Separación obligatoria**: El dominio no conoce JPA.

```java
// domain/model/Expediente.java - Pura, sin anotaciones
public class Expediente {
    private final UUID id;
    private final String phoneNumber;
    private ExpedienteStatusEnum status;

    public static Expediente createNew(String phone) {
        return new Expediente(UUID.randomUUID(), phone, ExpedienteStatusEnum.BLSG_PRECONSULTA);
    }
}

// infrastructure/persistence/entity/ExpedienteEntity.java - JPA
@Entity
@Table(name = "expedientes")
public class ExpedienteEntity {
    @Id
    private UUID id;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    private ExpedienteStatusEnum status;
}
```

---

## 5. Convenciones de Base de Datos

| Elemento | Convención | Ejemplo |
|----------|------------|---------|
| **Tables** | snake_case, plural | `expedientes`, `spouses`, `blsg_constancies` |
| **Columns** | snake_case | `created_at`, `divorce_type`, `phone_number` |
| **Primary Key** | `id` | `id` |
| **Foreign Key** | `table_id` | `expediente_id`, `spouse_id` |
| **Indexes** | `idx_table_column` | `idx_expedientes_phone` |
| **Enums DB** | VARCHAR (no números) | Guardar `"BLSG_PRECONSULTA"` no `0` |

---

## 6. Convenciones MCP Tools

| Elemento | Convención | Ejemplo |
|----------|------------|---------|
| **Tool name** | camelCase, acción + objeto | `crearExpediente`, `consultarBLSG` |
| **Parameters** | camelCase, inglés | `dni`, `fullName`, `birthDate` |
| **Response** | PascalCase, objeto + `Response` | `ExpedienteCreadoResponse` |

---

## 7. Reglas de Oro

1. **Un concepto = una palabra**: Si usas `crear` en dominio, usa `crear` en API, no `save` ni `insert`
2. **Nombres revelan intención**: `consultarBLSG()` no `procesar()`
3. **Sin abreviaturas crípticas**: `documentNumber` no `numDoc`, `expediente` no `exp`
4. **Consistencia de idioma**: Código = inglés, usuario = español
5. **Test descriptivos**: `shouldCreateExpedienteWithValidPhone()` no `test1()`

---

## 8. Ejemplo Completo

### Dominio (Core)

```java
// domain/model/Expediente.java
package com.lawrabot.divorce_mcp_server.domain.model;

import com.lawrabot.divorce_mcp_server.domain.enums.ExpedienteStatusEnum;
import com.lawrabot.divorce_mcp_server.domain.valueobject.DNIVO;

public class Expediente {
    private final UUID id;
    private final String phoneNumber;
    private ExpedienteStatusEnum status;
    private DNIVO petitioningSpouseDni;

    private Expediente(UUID id, String phoneNumber, ExpedienteStatusEnum status) {
        this.id = id;
        this.phoneNumber = phoneNumber;
        this.status = status;
    }

    public static Expediente createNew(String phoneNumber) {
        validatePhone(phoneNumber);
        return new Expediente(UUID.randomUUID(), phoneNumber, ExpedienteStatusEnum.BLSG_PRECONSULTA);
    }

    public boolean canProceedWithDataCollection() {
        return this.status == ExpedienteStatusEnum.BLSG_PRECONSULTA;
    }

    public void transitionToInProgress() {
        if (!canProceedWithDataCollection()) {
            throw new IllegalStateException("Cannot transition to IN_PROGRESS from " + this.status);
        }
        this.status = ExpedienteStatusEnum.IN_PROGRESS;
    }
}

// domain/enums/ExpedienteStatusEnum.java
package com.lawrabot.divorce_mcp_server.domain.enums;

public enum ExpedienteStatusEnum {
    BLSG_PRECONSULTA,
    BLSG_RECHAZADO,
    IN_PROGRESS,
    DATA_COMPLETE,
    DOCUMENTS_GENERATED,
    UNDER_REVIEW,
    OBSERVATIONS_PENDING,
    SUBMITTED,
    IN_PROCEDURE,
    FINALIZED
}

// domain/valueobject/DNIVO.java
package com.lawrabot.divorce_mcp_server.domain.valueobject;

public final class DNIVO {
    private final String value;

    private DNIVO(String value) {
        this.value = value;
    }

    public static DNIVO of(String value) {
        if (value == null || !value.matches("^\\d{7,8}$")) {
            throw new IllegalArgumentException("Invalid DNI format");
        }
        return new DNIVO(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DNIVO dnivo = (DNIVO) o;
        return value.equals(dnivo.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}

// domain/repository/IExpedienteRepository.java (Port)
package com.lawrabot.divorce_mcp_server.domain.repository;

import com.lawrabot.divorce_mcp_server.domain.model.Expediente;
import java.util.Optional;
import java.util.UUID;

public interface IExpedienteRepository {
    Expediente save(Expediente expediente);
    Optional<Expediente> findById(UUID id);
    Optional<Expediente> findByPhoneNumber(String phoneNumber);
    boolean existsByPhoneNumber(String phoneNumber);
}
```

### Infraestructura (Adapter)

```java
// infrastructure/persistence/ExpedienteRepositoryImpl.java
package com.lawrabot.divorce_mcp_server.infrastructure.persistence;

import com.lawrabot.divorce_mcp_server.domain.model.Expediente;
import com.lawrabot.divorce_mcp_server.domain.repository.IExpedienteRepository;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.entity.ExpedienteEntity;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.jpa.IExpedienteJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class ExpedienteRepositoryImpl implements IExpedienteRepository {

    private final IExpedienteJpaRepository jpaRepository;
    private final ExpedienteMapper mapper;

    public ExpedienteRepositoryImpl(IExpedienteJpaRepository jpaRepository, ExpedienteMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Expediente save(Expediente expediente) {
        ExpedienteEntity entity = mapper.toEntity(expediente);
        ExpedienteEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Expediente> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Expediente> findByPhoneNumber(String phoneNumber) {
        return jpaRepository.findByPhoneNumber(phoneNumber).map(mapper::toDomain);
    }

    @Override
    public boolean existsByPhoneNumber(String phoneNumber) {
        return jpaRepository.existsByPhoneNumber(phoneNumber);
    }
}

// infrastructure/persistence/entity/ExpedienteEntity.java
package com.lawrabot.divorce_mcp_server.infrastructure.persistence.entity;

import com.lawrabot.divorce_mcp_server.domain.enums.ExpedienteStatusEnum;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "expedientes")
public class ExpedienteEntity {

    @Id
    private UUID id;

    @Column(name = "phone_number", nullable = false, unique = true)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExpedienteStatusEnum status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Getters, setters, etc.
}
```

---

## 9. Referencia Rápida

### Cheat Sheet

```java
// ❌ Mal
public class expediente {}
public class ExpedienteRepo {} // sin I en interface
public class ExpedienteAdapter {} // adapter sin ser impl de interfaz
public void procesar() {} // no revela intención
String numDoc; // abreviatura
int status; // enum como número

// ✅ Bien
public class Expediente {}
public interface IExpedienteRepository {}
public class ExpedienteRepositoryImpl implements IExpedienteRepository {}
public void consultarBLSG() {} // revela intención
String documentNumber; // descriptivo
ExpedienteStatusEnum status; // enum con tipo
```

---

## 10. Architecture Decision Records (ADRs)

Las decisiones arquitectónicas importantes deben documentarse como ADRs.

### Ubicación
```
adr/
├── index.md              # Índice auto-generado
├── 0001-decision-uno.md
├── 0002-decision-dos.md
└── ...
```

### Formato de Nombre
```
NNNN-titulo-corto-en-espanol.md
```

Ejemplo: `0001-uso-de-lombok-en-entidades.md`

### Template ADR

```markdown
# N. Título de la Decisión

**Fecha**: YYYY-MM-DD

**Estado**: Propuesto | Aceptado | Deprecado | Reemplazado por [ADR-XXXX](adr-xxxx.md)

## Contexto

¿Qué problema estamos resolviendo? ¿Quas restricciones existen?

## Decisión

¿Qué decidimos hacer? Debe ser explícito.

## Consecuencias

### Positivas
- Beneficio 1
- Beneficio 2

### Negativas
- Costo 1
- Costo 2

## Alternativas Consideradas

1. **Alternativa A**: Por qué se descartó
2. **Alternativa B**: Por qué se descartó

## Referencias

- Links relevantes
- Documentación
```

### Actualización del Índice

Para regenerar el `index.md` automáticamente:

```bash
# Instalar adr-log (solo la primera vez)
npm install -g adr-log

# Actualizar índice (ejecutar desde directorio adr/)
cd adr
adr-log -d .
```

El comando escanea todos los archivos `NNNN-*.md` y reconstruye el índice.

### Cuándo Crear un ADR

Crear ADR cuando:
- Se elige una tecnología/framework
- Se define un patrón arquitectónico
- Se hace un trade-off significativo
- Se cambia una decisión previa

No es necesario ADR para:
- Cambios menores de implementación
- Decisiones triviales
- Configuraciones de herramientas

---

> **Nota:** Estas convenciones son obligatorias. Cualquier PR que no las siga será rechazado en code review.

> **Próxima actualización:** Añadir convenciones para TypeScript (Agent frontend).
