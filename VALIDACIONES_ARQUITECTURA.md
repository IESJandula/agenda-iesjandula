# 🔗 Integración Completa: Validaciones en Spring Boot

## 📦 Arquitectura de Validación

```
┌─────────────────────────────────────────────────────────────────┐
│                     CLIENT (Frontend/Postman)                   │
└─────────────────────┬───────────────────────────────────────────┘
                      │
                      │ HTTP Request
                      │ POST /api/eventos
                      │ Content-Type: application/json
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│              Spring Boot DispatcherServlet                       │
│              • Recibe petición HTTP                              │
│              • Enruta a controlador correcto                     │
└─────────────────────┬───────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│          Jackson HttpMessageConverter                            │
│          • Deserializa JSON a objeto Java                        │
│          • Crea instancia de CrearEventoRequest                  │
└─────────────────────┬───────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│          Jakarta Validation (LocalValidatorFactoryBean)         │
│          • @Valid activa validaciones automáticas               │
│          • Ejecuta todas las anotaciones de validación           │
│            - @NotNull, @NotBlank, @Size                         │
│            - @FutureOrPresent, @Positive, etc.                  │
└────────────────┬────────────────────────┬──────────────────────┘
                 │                        │
         ✅ Validación OK         ❌ Validación Fallida
                 │                        │
                 ▼                        ▼
        ┌─────────────────┐      ┌──────────────────────────┐
        │  Continúa en    │      │ MethodArgumentNotValid   │
        │  el método      │      │ Exception                │
        │  del            │      │ • Errores por campo      │
        │  controlador    │      │ • Mensajes de error      │
        │                 │      └────────┬─────────────────┘
        │ @PostMapping    │              │
        │ public          │              ▼
        │ ResponseEntity  │      ┌──────────────────────────┐
        │ crearEvento()   │      │ GlobalExceptionHandler   │
        │ {               │      │ (@ControllerAdvice)      │
        │   ...lógica...  │      │ • Intercepta excepción   │
        │ }               │      │ • Extrae errores         │
        │                 │      │ • Genera JSON de respuesta
        └────────┬────────┘      └────────┬─────────────────┘
                 │                        │
                 │                        │
                 ▼                        ▼
        ┌──────────────────┐     ┌─────────────────────────┐
        │ EventoService    │     │ HTTP Response           │
        │ .crearEvento()   │     │ Status: 400             │
        │                  │     │ Body: JSON con errores  │
        └────────┬─────────┘     └─────────────────────────┘
                 │                        │
                 ▼                        │
        ┌──────────────────┐             │
        │ Persistencia en  │             │
        │ Base de Datos    │             │
        └────────┬─────────┘             │
                 │                        │
                 ▼                        ▼
        ┌──────────────────┐     ┌─────────────────────────┐
        │ HTTP Response    │     │ Client recibe error     │
        │ Status: 201      │     │ {                       │
        │ Body: JSON Event │     │   "fieldErrors": {...}  │
        │ + headers        │     │ }                       │
        └──────────────────┘     └─────────────────────────┘
```

---

## 🎯 Componentes de la Validación

### **1. DTO con Validaciones (@NotBlank, @Size, etc.)**

Archivo: [src/main/java/agenda/dto/CrearEventoRequest.java](src/main/java/agenda/dto/CrearEventoRequest.java)

```java
@Data
public class CrearEventoRequest {
    
    @NotNull(message = "El tipo de evento es requerido")
    private Long tipoId;
    
    @NotBlank(message = "El título es obligatorio y no puede estar vacío")
    @Size(max = 100, message = "El título no puede superar 100 caracteres")
    private String titulo;
    
    @NotNull(message = "La fecha de inicio es requerida")
    @FutureOrPresent(message = "La fecha de inicio no puede estar en el pasado")
    private LocalDateTime fechaInicio;
    
    @Positive(message = "El número de asistentes debe ser mayor que 0")
    private Integer numAsistentes;
}
```

**Propósito:** Especificar las reglas de validación en la fuente de datos

---

### **2. Controlador con @Valid**

Archivo: [src/main/java/agenda/controller/EventoController.java](src/main/java/agenda/controller/EventoController.java)

```java
@PostMapping
public ResponseEntity<EventoResponseDTO> crearEvento(
    @Valid                          // ← ACTIVA VALIDACIONES
    @RequestBody CrearEventoRequest request) {
    
    // En este punto, request está garantizado como válido
    Evento creado = eventoService.crearEvento(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(
        eventoService.convertirAResponse(creado)
    );
}
```

**Propósito:** Activar automáticamente las validaciones del DTO

---

### **3. GlobalExceptionHandler - Captura errores**

Archivo: [src/main/java/agenda/exception/GlobalExceptionHandler.java](src/main/java/agenda/exception/GlobalExceptionHandler.java)

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, Object>> handleValidationException(
        MethodArgumentNotValidException ex) {
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", 400);
        response.put("error", "Validación fallida");
        
        Map<String, String> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                error -> error.getField(),
                error -> error.getDefaultMessage()
            ));
        
        response.put("fieldErrors", errors);
        return ResponseEntity.badRequest().body(response);
    }
}
```

**Propósito:** Interceptar excepciones y devolver respuestas HTTP consistentes

---

## 🔄 Ciclo de Vida Completo

### **Escenario: Crear evento con datos válidos**

```
1. Cliente envía:
   POST /api/eventos
   {
     "tipoId": 1,
     "creadorId": 5,
     "titulo": "Reunión",
     "fechaInicio": "2025-12-25T10:00:00",
     "lugar": "Sala A",
     "gruposAfectados": "2ºDAW",
     "numAsistentes": 20,
     "estado": "PENDIENTE"
   }

2. Spring deserializa JSON → CrearEventoRequest object

3. @Valid ejecuta validaciones:
   ✅ tipoId: 1 != null
   ✅ creadorId: 5 != null
   ✅ titulo: "Reunión" no está vacío
   ✅ titulo: "Reunión".length() <= 100
   ✅ fechaInicio: 2025-12-25 > hoy
   ✅ lugar: "Sala A" no está vacío
   ✅ gruposAfectados: "2ºDAW" no está vacío
   ✅ numAsistentes: 20 > 0
   ✅ estado: PENDIENTE != null

4. Validación exitosa → Continúa en método

5. EventoService.crearEvento(request)
   - Valida lógica de negocio
   - Persiste en BD
   - Devuelve Evento guardado

6. Controller convierte a EventoResponseDTO

7. Spring devuelve:
   HTTP 201 Created
   {
     "id": 123,
     "tipoId": 1,
     "titulo": "Reunión",
     ...
   }
```

---

### **Escenario: Crear evento con datos inválidos**

```
1. Cliente envía:
   POST /api/eventos
   {
     "tipoId": null,              ❌ Nulo
     "creadorId": 5,
     "titulo": "",                ❌ Vacío
     "fechaInicio": "2024-01-01T10:00:00",  ❌ Pasado
     "lugar": "   ",              ❌ Solo espacios
     "gruposAfectados": null,     ❌ Nulo
     "numAsistentes": 0,          ❌ No positivo
     "estado": null               ❌ Nulo
   }

2. Spring deserializa JSON → CrearEventoRequest object

3. @Valid ejecuta validaciones:
   ❌ tipoId: null → @NotNull falla
   ❌ titulo: "" → @NotBlank falla
   ❌ fechaInicio: 2024-01-01 < hoy → @FutureOrPresent falla
   ❌ lugar: "   " → @NotBlank falla
   ❌ gruposAfectados: null → @NotBlank falla
   ❌ numAsistentes: 0 → @Positive falla
   ❌ estado: null → @NotNull falla

4. Validación fallida → Spring lanza MethodArgumentNotValidException

5. GlobalExceptionHandler captura la excepción

6. Extrae todos los errores:
   {
     "tipoId": "El tipo de evento es requerido",
     "titulo": "El título es obligatorio y no puede estar vacío",
     "fechaInicio": "La fecha de inicio no puede estar en el pasado",
     "lugar": "El lugar del evento es requerido",
     "gruposAfectados": "Los grupos afectados son requeridos",
     "numAsistentes": "El número de asistentes debe ser mayor que 0",
     "estado": "El estado del evento es requerido"
   }

7. Spring devuelve:
   HTTP 400 Bad Request
   {
     "timestamp": "2025-05-12T10:35:22.456",
     "status": 400,
     "error": "Validación fallida",
     "fieldErrors": { ... },
     "errorCount": 7
   }

8. Método del controlador NUNCA se ejecuta
   → Evita lógica de negocio innecesaria
   → Ahorra recursos de BD
```

---

## 📊 Tabla de Validaciones

| Validación | Campo | Uso en DTO | Uso en Controller | Resultado |
|-----------|-------|-----------|-----------------|-----------|
| `@NotNull` | tipoId | ✅ Sí | Implícito con @Valid | 400 si null |
| `@NotBlank` | titulo | ✅ Sí | Implícito con @Valid | 400 si vacío |
| `@Size(100)` | titulo | ✅ Sí | Implícito con @Valid | 400 si > 100 |
| `@FutureOrPresent` | fechaInicio | ✅ Sí | Implícito con @Valid | 400 si pasado |
| `@Positive` | numAsistentes | ✅ Sí | Implícito con @Valid | 400 si ≤ 0 |

---

## ⚙️ Configuración Necesaria

### **1. Dependencia Maven (pom.xml)**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

✅ **Ya está incluida** en tu proyecto

### **2. Anotación en Controlador**

```java
@PostMapping
public ResponseEntity<...> create(
    @Valid @RequestBody DTO request) {  // ← Obligatorio
    ...
}
```

✅ **Ya está implementada** en EventoController y UsuarioController

### **3. GlobalExceptionHandler**

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ...
}
```

✅ **Ya está implementado** y mejorado

---

## 🧪 Pruebas Prácticas

### **Test 1: Validación exitosa**

```bash
curl -X POST http://localhost:8080/api/eventos \
  -H "Content-Type: application/json" \
  -d '{
    "tipoId": 1,
    "creadorId": 5,
    "titulo": "Evento válido",
    "fechaInicio": "2025-12-31T18:00:00",
    "lugar": "Auditorio",
    "gruposAfectados": "Todos",
    "numAsistentes": 50,
    "estado": "PENDIENTE"
  }'
```

**Esperado:**
```json
HTTP/1.1 201 Created
{
  "id": 1,
  "titulo": "Evento válido",
  ...
}
```

### **Test 2: Título vacío**

```bash
curl -X POST http://localhost:8080/api/eventos \
  -H "Content-Type: application/json" \
  -d '{
    "tipoId": 1,
    "creadorId": 5,
    "titulo": "",
    "fechaInicio": "2025-12-31T18:00:00",
    "lugar": "Auditorio",
    "gruposAfectados": "Todos",
    "numAsistentes": 50,
    "estado": "PENDIENTE"
  }'
```

**Esperado:**
```json
HTTP/1.1 400 Bad Request
{
  "status": 400,
  "error": "Validación fallida",
  "fieldErrors": {
    "titulo": "El título es obligatorio y no puede estar vacío"
  }
}
```

### **Test 3: Multiple errores**

```bash
curl -X POST http://localhost:8080/api/eventos \
  -H "Content-Type: application/json" \
  -d '{
    "tipoId": null,
    "creadorId": 5,
    "titulo": "",
    "fechaInicio": "2024-01-01T10:00:00",
    "lugar": null,
    "gruposAfectados": null,
    "numAsistentes": -5,
    "estado": null
  }'
```

**Esperado:**
```json
HTTP/1.1 400 Bad Request
{
  "status": 400,
  "error": "Validación fallida",
  "errorCount": 7,
  "fieldErrors": {
    "tipoId": "El tipo de evento es requerido",
    "titulo": "El título es obligatorio y no puede estar vacío",
    "fechaInicio": "La fecha de inicio no puede estar en el pasado",
    "lugar": "El lugar del evento es requerido",
    "gruposAfectados": "Los grupos afectados son requeridos",
    "numAsistentes": "El número de asistentes debe ser mayor que 0",
    "estado": "El estado del evento es requerido"
  }
}
```

---

## 🎓 Resumen de Decisiones de Arquitectura

| Decisión | Razón |
|----------|-------|
| **Jakarta Validation** | Standard Java (JSR-303/380), integrado en Spring Boot 3 |
| **@Valid en controlador** | Validación automática, fail-fast antes de lógica de negocio |
| **GlobalExceptionHandler** | Manejo centralizado, respuestas consistentes |
| **Mensajes en español** | Claridad para usuarios finales |
| **Validaciones en DTO** | Separación de responsabilidades, reutilización |
| **@ResponseStatus(400)** | Código HTTP estándar para errores de validación |
| **Timestamps en respuesta** | Trazabilidad y debugging |

---

## ✅ Checklist de Implementación

- [x] Dependencia `spring-boot-starter-validation` en pom.xml
- [x] DTOs con anotaciones de validación (@Valid, @NotBlank, @Size, etc.)
- [x] @Valid en métodos POST de controladores
- [x] @Valid en métodos PUT de controladores
- [x] GlobalExceptionHandler configurado
- [x] @ExceptionHandler para MethodArgumentNotValidException
- [x] Mensajes personalizados en español
- [x] HTTP 400 en errores de validación
- [x] HTTP 201 en creación exitosa
- [x] Documentación completa

**Tu aplicación está completamente configurada con validaciones profesionales** 🚀

---

## 📚 Archivos de Referencia

1. [VALIDACIONES.md](VALIDACIONES.md) - Explicación de cada validación
2. [VALIDACIONES_CONTROLADORES.md](VALIDACIONES_CONTROLADORES.md) - Integración en controladores
3. [src/main/java/agenda/dto/CrearEventoRequest.java](src/main/java/agenda/dto/CrearEventoRequest.java) - DTO validado
4. [src/main/java/agenda/controller/EventoController.java](src/main/java/agenda/controller/EventoController.java) - Controlador con @Valid
5. [src/main/java/agenda/exception/GlobalExceptionHandler.java](src/main/java/agenda/exception/GlobalExceptionHandler.java) - Manejo de excepciones
