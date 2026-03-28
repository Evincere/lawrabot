# 1. Uso de Lombok en Entidades de Dominio

**Fecha**: 2026-03-27

**Estado**: Aceptado

## Contexto

El proyecto LawraBot utiliza Java con Spring Boot para el MCP Server. Las entidades de dominio requieren:
- Getters y setters para todos los campos
- Métodos `equals()` y `hashCode()` basados en el ID
- Método `toString()` para logging
- Constructor con múltiples parámetros

Esto resulta en código repetitivo y verboso ("boilerplate"), dificultando la lectura y mantenimiento.

## Decisión

Se decide utilizar **Project Lombok** para generar automáticamente código repetitivo mediante anotaciones.

## Anotaciones Utilizadas

| Anotación | Propósito |
|-----------|-----------|
| `@Slf4j` | Genera logger estático `log` |
| `@Getter` | Genera getters públicos para todos los campos |
| `@Setter(AccessLevel.PRIVATE)` | Genera setters privados |
| `@EqualsAndHashCode(of = "id")` | Genera equals/hashCode basado solo en ID |
| `@ToString` | Genera toString con todos los campos |

## Consecuencias

### Positivas
- Reducción de ~50-70% de líneas de código en entidades
- Código más legible, enfocado en lógica de negocio
- Mantenimiento simplificado (cambios en campos automáticos)
- Consistencia garantizada en equals/hashCode/toString

### Negativas
- Requiere instalación del plugin de Lombok en IDE
- Curva de aprendizaje para desarrolladores nuevos
- Dependencia adicional en el proyecto
- Dificultad para debuggear (código generado no visible directamente)

## Alternativas Consideradas

1. **Records de Java 16+**: No permiten campos mutables, incompatible con JPA/Hibernate
2. **Generación manual**: Demasiado verboso y propenso a errores
3. **IDE auto-generate**: Código estático que requiere regeneración manual

## Referencias

- [Lombok Documentation](https://projectlombok.org/features/)
- [Coding Standards](../CODING_STANDARDS.md)
