# 📋 Validaciones Jakarta Validation - Arquitectura Profesional

## 🎯 Descripción General

Se han implementado validaciones profesionales usando **Jakarta Validation** (antes conocido como Bean Validation / JSR-303/380) en el DTO `CrearEventoRequest`. Estas validaciones se ejecutan automáticamente cuando Spring Boot recibe una petición HTTP.

---

## 📌 Validaciones Implementadas

### Formato de fecha aceptado
Las fechas del cuerpo de la petición deben enviarse en formato ISO-8601 con segundos:

```json
"fechaInicio": "2025-06-15T14:00:00",
"fechaFin": "2025-06-15T15:30:00"
```

Si se envía otro formato, Spring rechazará la petición antes de llegar a la validación de negocio.

### 1. **@NotNull** - Validación de No Nulo
```java
@NotNull(message = "El tipo de evento es requerido")
private Long tipoId;

@NotNull(message = "El creador del evento es requerido")
private Long creadorId;

@NotNull(message = "La fecha de inicio es requerida")
private LocalDateTime fechaInicio;

@NotNull(message = "El estado del evento es requerido")
private EstadoEvento estado;
```

**¿Qué hace?**
- Verifica que el campo no sea `null`
- Es la validación más básica pero crítica

**¿Cuándo usar?**
- En campos obligatorios de tipo objeto (Long, LocalDateTime, Enum, etc.)
- No valida campos vacíos o en blanco

**Ejemplo de rechazo:**
```json
{
  "titulo": "Reunión importante",
  "tipoId": null  // ❌ Rechazado
}
```

---

### 2. **@NotBlank** - Validación de No Vacío
```java
@NotBlank(message = "El título es obligatorio y no puede estar vacío")
private String titulo;

@NotBlank(message = "El lugar del evento es requerido")
private String lugar;

@NotBlank(message = "Los grupos afectados son requeridos")
private String gruposAfectados;
```

**¿Qué hace?**
- Verifica que el String no sea `null`
- Verifica que no sea una cadena vacía `""`
- Verifica que no sea solo espacios en blanco `"   "`

**¿Cuándo usar?**
- En campos de texto obligatorios
- Es más estricto que `@NotNull` para Strings

**Ejemplo de rechazo:**
```json
{
  "titulo": "",        // ❌ Rechazado
  "lugar": "   ",      // ❌ Rechazado
  "gruposAfectados": null  // ❌ Rechazado
}
```

---

### 3. **@Size** - Validación de Tamaño
```java
@Size(max = 100, message = "El título no puede superar 100 caracteres")
private String titulo;

@Size(max = 500, message = "La descripción no puede superar 500 caracteres")
private String descripcion;
```

**¿Qué hace?**
- Valida la longitud de una cadena o colección
- Se puede usar con `min` y `max`

**¿Cuándo usar?**
- Para limitar la longitud de texto
- Para proteger la base de datos
- Para mantener coherencia UX

**Ejemplo de rechazo:**
```json
{
  "titulo": "Este es un título extremadamente largo que supera los 100 caracteres límite establecido por el sistema",  // ❌ Rechazado
  "descripcion": "Lorem ipsum dolor sit amet consectetur adipiscing elit..." // Si > 500 caracteres ❌
}
```

---

### 4. **@FutureOrPresent** - Validación Temporal
```java
@FutureOrPresent(message = "La fecha de inicio no puede estar en el pasado")
private LocalDateTime fechaInicio;
```

**¿Qué hace?**
- Verifica que la fecha sea igual o posterior a la fecha actual
- Rechaza fechas en el pasado

**¿Cuándo usar?**
- En eventos que no pueden ser retroactivos
- En fechas de inicio de procesos futuros
- Para prevenir datos inconsistentes

**Ejemplo de rechazo:**
```json
{
  "fechaInicio": "2025-01-01T10:00:00"  // ❌ Si es anterior a hoy
}
```

**Alternativas:**
- `@Future`: Solo acepta fechas futuras (no presente)
- `@PastOrPresent`: Para fechas pasadas
- `@Past`: Solo fechas pasadas

---

### 5. **@Positive** - Validación de Número Positivo
```java
@Positive(message = "El número de asistentes debe ser mayor que 0")
private Integer numAsistentes;
```

**¿Qué hace?**
- Verifica que el número sea > 0 (no incluye 0)
- Solo acepta valores positivos

**¿Cuándo usar?**
- En cantidades que deben ser mayores que cero
- En precios, calificaciones, conteos

**Ejemplo de rechazo:**
```json
{
  "numAsistentes": 0      // ❌ Rechazado (debe ser > 0)
  "numAsistentes": -5     // ❌ Rechazado
}
```

**Alternativas:**
- `@Min(value = 0)`: Acepta >= 0 (incluye cero)
- `@PositiveOrZero`: Acepta >= 0
- `@NegativeOrZero`: Acepta <= 0

---

## 🔄 Cómo Funciona la Validación en Spring Boot

### 1. **Activación Automática en Controllers**

Añade `@Valid` o `@Validated` en tu controlador:

```java
@RestController
@RequestMapping("/api/eventos")
public class EventoController {
    
    @PostMapping
    public ResponseEntity<EventoResponseDTO> crearEvento(
        @Valid @RequestBody CrearEventoRequest request) {
        // Si las validaciones pasan, el código continúa
        // Si fallan, Spring lanza un MethodArgumentNotValidException
        return ResponseEntity.ok(eventoService.crearEvento(request));
    }
}
```

### 2. **Manejo de Errores de Validación**

El `GlobalExceptionHandler` debe capturar los errores:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
        MethodArgumentNotValidException ex) {
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            errors.put(error.getField(), error.getDefaultMessage())
        );
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("Validación fallida", errors));
    }
}
```

### 3. **Respuesta de Error (Ejemplo)**

Cuando hay errores de validación:

```json
{
  "status": 400,
  "mensaje": "Validación fallida",
  "errores": {
    "titulo": "El título es obligatorio y no puede estar vacío",
    "fechaInicio": "La fecha de inicio no puede estar en el pasado",
    "numAsistentes": "El número de asistentes debe ser mayor que 0"
  },
  "timestamp": "2025-05-12T10:30:00"
}
```

---

## ✅ Ejemplo de Petición Válida

> Nota: `fechaInicio` y `fechaFin` deben incluir segundos. El formato aceptado es `yyyy-MM-dd'T'HH:mm:ss`.

```json
{
  "tipoId": 1,
  "creadorId": 5,
  "titulo": "Reunión Trimestral",
  "descripcion": "Reunión para discutir los resultados del trimestre",
  "fechaInicio": "2025-06-15T14:00:00",
  "fechaFin": "2025-06-15T15:30:00",
  "lugar": "Sala de Conferencias A",
  "gruposAfectados": "2ºDAW, 2ºDAM",
  "enlaceDocumento": "https://docs.example.com/reunion-q2",
  "numAsistentes": 25,
  "estado": "PENDIENTE"
}
```

✅ **Respuesta exitosa: 201 Created**

---

## ✅ Ejemplo de Petición Inválida

```json
{
  "tipoId": null,  // ❌ No nulo
  "creadorId": 5,
  "titulo": "",    // ❌ No vacío
  "descripcion": "A".repeat(501),  // ❌ Max 500
  "fechaInicio": "2024-01-01T10:00:00",  // ❌ No en pasado
  "lugar": "   ",  // ❌ No en blanco
  "gruposAfectados": null,  // ❌ No nulo
  "numAsistentes": 0,  // ❌ Debe ser > 0
  "estado": null  // ❌ No nulo
}
```

❌ **Respuesta: 400 Bad Request**

---

## 🛠️ Resumen de Validaciones Profesionales

| Validación | Campo | Regla | Razón |
|-----------|-------|-------|-------|
| `@NotNull` | `tipoId` | No puede ser null | Referencia obligatoria |
| `@NotNull` | `creadorId` | No puede ser null | Auditoría y permisos |
| `@NotBlank` | `titulo` | Obligatorio + no vacío | Identificación del evento |
| `@Size(max=100)` | `titulo` | Máximo 100 caracteres | Límite de base de datos |
| `@Size(max=500)` | `descripcion` | Máximo 500 caracteres | Contenido detallado pero acotado |
| `@NotNull` | `fechaInicio` | No puede ser null | Dato crítico |
| `@FutureOrPresent` | `fechaInicio` | No en el pasado | Lógica de negocio |
| `@NotBlank` | `lugar` | Obligatorio + no vacío | Ubicación del evento |
| `@NotBlank` | `gruposAfectados` | Obligatorio + no vacío | Audiencia objetivo |
| `@Positive` | `numAsistentes` | Mayor que 0 | Cantidad significativa |
| `@NotNull` | `estado` | No puede ser null | Estado transaccional |

---

## 🎓 Buenas Prácticas Implementadas

✅ **1. Mensajes en español**: Todos los mensajes están en el idioma del negocio  
✅ **2. Especificidad**: Cada validación es específica y clara  
✅ **3. Documentación Javadoc**: Cada campo está comentado  
✅ **4. Combinación de validaciones**: Se usan múltiples anotaciones donde es necesario  
✅ **5. Uso de @FutureOrPresent**: Para validaciones temporales correctas  
✅ **6. Uso de @Positive**: Mejor que @Min(1) para semántica clara  
✅ **7. Campos opcionales**: Algunos campos pueden ser null si la lógica lo permite  

---

## 📚 Referencias

- [Jakarta Bean Validation Specification](https://jakarta.ee/specifications/bean-validation/)
- [Spring Boot Validation Guide](https://docs.spring.io/spring-boot/reference/features/developing-web-applications.html#web.servlet.support-matrix.validation)
- [Jakarta Validation Built-in Constraints](https://jakarta.ee/specifications/bean-validation/3.0/jakarta-bean-validation-spec-3.0.html#builtinconstraints)
