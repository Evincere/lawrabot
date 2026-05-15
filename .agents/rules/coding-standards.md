---
trigger: always_on
glob: "**/*.java"
description: "Estándares de codificación para evitar advertencias de Linters."
---

# Reglas de Codificación y Linting

1. **Prevención de Advertencias de Punteros Nulos (Null Pointer Access) en Getters:**
   Cuando necesites validar y acceder a una propiedad de un objeto o anidada en Java usando un getter (por ejemplo, `obj.getProperty()`), **nunca debes llamar al getter más de una vez en la misma estructura de control**. 
   Dado que el IDE y los linters estáticos no pueden garantizar la inmutabilidad de los métodos en tiempo de evaluación, llamar repetidas veces al mismo getter luego de un chequeo `!= null` desencadenará advertencias ("Potential null pointer access"). 
   
   **Solución aplicable en cualquier situación:** Debes siempre extraer y encadenar el valor del getter en una variable temporal local, y luego realizar la verificación sobre esta.

   *Correcto* (Caché en variable temporal):
   ```java
   var property = obj.getProperty();
   if (property != null) {
       return property.getValue();
   }
   ```
   
   *Incorrecto* (Evaluación múltiple y propensa a error):
   ```java
   if (obj.getProperty() != null) {
       return obj.getProperty().getValue(); // Dispara Warning en el IDE
   }
   ```

2. **Manejo Centralizado de Errores REST (GlobalExceptionHandler):**
   El proyecto implementa un `@RestControllerAdvice` global ubicado en:
   `infrastructure/rest/GlobalExceptionHandler.java`
   
   Los controladores REST **NUNCA** deben capturar excepciones internamente para construir respuestas de error. Las excepciones deben propagarse naturalmente al `GlobalExceptionHandler`, que las transforma en respuestas `ApiErrorResponse` estandarizadas.

   **Reglas obligatorias:**
   - **PROHIBIDO** usar `try-catch` dentro de métodos de controlador para convertir excepciones en `ResponseEntity`.
   - **PROHIBIDO** usar `ResponseEntity<?>` como tipo de retorno. Los controladores deben declarar el tipo fuerte de su respuesta (ej: `ResponseEntity<DivorceResponseDTO>`).
   - **PROHIBIDO** crear `@ExceptionHandler` locales en un controlador individual. Toda excepción nueva debe registrarse en el `GlobalExceptionHandler`.
   - Las excepciones de validación de Value Objects (`IllegalArgumentException` de `DNIVO`, `CuilVO`, `PhoneNumberVO`, etc.) se capturan automáticamente y devuelven `400 Bad Request`.
   - Las excepciones no previstas se capturan como `500 Internal Server Error` sin exponer detalles internos al cliente.

   *Correcto* (Controlador limpio, tipado fuerte):
   ```java
   @PutMapping("/cases/{id}")
   public ResponseEntity<DivorceResponseDTO> updateCase(@PathVariable UUID id, @RequestBody UpdateRequest req) {
       // Las excepciones de validación se propagan al GlobalExceptionHandler
       return repository.findById(id).map(exp -> {
           exp.update(mapToSpouse(req.getData())); // Si CuilVO lanza, el handler global captura
           repository.save(exp);
           return ResponseEntity.ok(mapToDTO(exp));
       }).orElse(ResponseEntity.notFound().build());
   }
   ```
   
   *Incorrecto* (Anti-patrón: try-catch interno con tipos débiles):
   ```java
   @PutMapping("/cases/{id}")
   public ResponseEntity<?> updateCase(@PathVariable UUID id, @RequestBody UpdateRequest req) {
       try {
           // ...
           return ResponseEntity.ok(mapToDTO(exp));
       } catch (IllegalArgumentException e) {
           return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); // ANTI-PATRÓN
       }
   }
   ```

   **Formato de respuesta de error (`ApiErrorResponse`):**
   ```json
   {
     "status": 400,
     "error": "Datos Inválidos",
     "message": "Formato de CUIL/CUIT inválido: 27299332567",
     "path": "/api/divorce/cases/7000c064-...",
     "timestamp": "2026-04-14T22:00:00"
   }
   ```
