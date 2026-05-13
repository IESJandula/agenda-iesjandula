# 🔐 Activación de Validaciones en Controladores REST - Spring Boot 3

## 📋 Resumen Ejecutivo

Las validaciones se activan automáticamente en los controladores REST usando `@Valid` con `@RequestBody`. Spring Boot intercepta automáticamente los errores de validación y el `GlobalExceptionHandler` devuelve respuestas HTTP consistentes en formato JSON.

## 🕒 Formato de fechas en JSON

Para `POST /api/eventos` y `PUT /api/eventos/{id}`, las fechas deben enviarse en formato ISO-8601 con segundos:

```json
"fechaInicio": "2025-06-20T15:00:00",
"fechaFin": "2025-06-20T16:30:00"
```

No uses variantes sin segundos, con espacios o con formatos locales; el backend valida ese contrato de forma estricta.

---

## 🔄 Flujo Completo de Validación

```
┌─────────────────┐
│   Cliente HTTP  │
│  POST /api/..   │
│  {datos JSON}   │
└────────┬────────┘
         │
         ▼
┌─────────────────────────────┐
│  Spring DispatcherServlet   │
│  Recibe petición HTTP       │
└────────┬────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│  Controller - @Valid @RequestBody   │
│  Activa validaciones automáticas    │
└────────┬────────────────────────────┘
         │
         ├─→ ✅ Validación exitosa → Continúa en el método
         │
         └─→ ❌ Error de validación
                      ↓
              ┌───────────────────────────┐
              │ MethodArgumentNotValidEx. │
              └───────────┬───────────────┘
                          ↓
              ┌───────────────────────────────┐
              │ GlobalExceptionHandler        │
              │ (ControllerAdvice)            │
              │ - Captura la excepción        │
              │ - Extrae errores por campo    │
              │ - Genera respuesta JSON       │
              └───────────┬───────────────────┘
                          ↓
              ┌───────────────────────────────┐
              │ HTTP 400 Bad Request          │
              │ {                             │
              │   "status": 400,              │
              │   "fieldErrors": {...},       │
              │   "errorCount": 3             │
              │ }                             │
              └───────────────────────────────┘
```

---

## 💻 Código: Activar @Valid en Controladores

### **EventoController - POST (Crear)**

```java
@RestController
@RequestMapping("/api/eventos")
@RequiredArgsConstructor
public class EventoController {
    
    private final EventoService eventoService;

    /**
     * @Valid: Activa validaciones automáticas en CrearEventoRequest
     */
    @PostMapping
    public ResponseEntity<EventoResponseDTO> crearEvento(
        @Valid @RequestBody CrearEventoRequest request) {
        
        Evento creado = eventoService.crearEvento(request);
        EventoResponseDTO response = eventoService.convertirAResponse(creado);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
```

### **EventoController - PUT (Actualizar)**

```java
    /**
     * @Valid también funciona en actualizaciones
     */
    @PutMapping("/{id}")
    public ResponseEntity<EventoResponseDTO> actualizarEvento(
        @PathVariable Long id,
        @Valid @RequestBody CrearEventoRequest request) {
        
        EventoResponseDTO actualizado = eventoService.actualizarEvento(id, request);
        return ResponseEntity.ok(actualizado);
    }
```

### **UsuarioController - POST**

```java
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {
    
    private final UsuarioRepository usuarioRepository;

    /**
     * @Valid activa validaciones en el modelo Usuario
     */
    @PostMapping
    public ResponseEntity<Usuario> crearUsuario(
        @Valid @RequestBody Usuario usuario) {
        
        Usuario creado = usuarioRepository.save(usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }
}
```

---

## 🎯 Qué Hace @Valid

### **Anotación: `@Valid`**

```java
@Valid @RequestBody CrearEventoRequest request
```

**Propósito:**
- Instruye a Spring para validar el objeto deserializado
- Ejecuta todas las anotaciones de validación en el DTO (@NotBlank, @Size, etc.)
- Si hay errores, lanza `MethodArgumentNotValidException`

**¿Por qué se usa?**
- Validación automática sin código adicional
- Falla rápido antes de llegar a la lógica de negocio
- Devuelve errores consistentes

**Ubicación obligatoria:**
```java
@PostMapping
public void crear(
    @Valid                 // ← AQUÍ
    @RequestBody           // ← AQUÍ (después de @Valid)
    CrearEventoRequest request) {
    ...
}
```

---

## 🛡️ GlobalExceptionHandler - Captura de Errores

### **Cómo Funciona**

```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, Object>> handleValidationException(
        MethodArgumentNotValidException ex) {
        
        // 1. Crear respuesta con metadatos
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Validación fallida");
        
        // 2. Extraer errores por campo
        Map<String, String> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                error -> error.getField(),
                error -> error.getDefaultMessage()
            ));
        
        response.put("fieldErrors", errors);
        
        // 3. Devolver respuesta 400
        return ResponseEntity.badRequest().body(response);
    }
}
```

**`@ControllerAdvice`:**
- Clase anotada que maneja excepciones globalmente
- Se aplica a todos los controladores de la aplicación
- Intercepta excepciones antes de devolverlas al cliente

**`@ExceptionHandler`:**
- Especifica qué excepción maneja este método
- En este caso: `MethodArgumentNotValidException`
- Se ejecuta automáticamente cuando Spring lanza esta excepción

---

## 📊 Respuestas HTTP Esperadas

### **✅ Creación Exitosa: 201 Created**

> Formato obligatorio para fechas: `yyyy-MM-dd'T'HH:mm:ss`

```bash
curl -X POST http://localhost:8080/api/eventos \
  -H "Content-Type: application/json" \
  -d '{
    "tipoId": 1,
    "creadorId": 5,
    "titulo": "Reunión Importante",
    "descripcion": "Discusión de resultados",
    "fechaInicio": "2025-06-15T14:00:00",
    "lugar": "Sala A",
    "gruposAfectados": "2ºDAW",
    "numAsistentes": 25,
    "estado": "PENDIENTE"
  }'
```

**Respuesta:**
```json
{
  "id": 1,
  "tipoId": 1,
  "tipoNombre": "Reunión",
  "titulo": "Reunión Importante",
  "descripcion": "Discusión de resultados",
  "fechaInicio": "2025-06-15T14:00:00",
  "lugar": "Sala A",
  "gruposAfectados": "2ºDAW",
  "creadorId": 5,
  "creadorNombre": "Usuario Admin"
}
```

HTTP Status: **201 Created** ✅

---

### **❌ Validación Fallida: 400 Bad Request**

```bash
curl -X POST http://localhost:8080/api/eventos \
  -H "Content-Type: application/json" \
  -d '{
    "tipoId": null,
    "creadorId": 5,
    "titulo": "",
    "fechaInicio": "2024-01-01T10:00:00",
    "lugar": "   ",
    "gruposAfectados": null,
    "numAsistentes": 0,
    "estado": null
  }'
```

**Respuesta:**
```json
{
  "timestamp": "2025-05-12T10:30:45.123",
  "status": 400,
  "error": "Validación fallida",
  "message": "Los datos enviados no cumplen con las validaciones requeridas",
  "fieldErrors": {
    "tipoId": "El tipo de evento es requerido",
    "titulo": "El título es obligatorio y no puede estar vacío",
    "fechaInicio": "La fecha de inicio no puede estar en el pasado",
    "lugar": "El lugar del evento es requerido",
    "gruposAfectados": "Los grupos afectados son requeridos",
    "numAsistentes": "El número de asistentes debe ser mayor que 0",
    "estado": "El estado del evento es requerido"
  },
  "errorCount": 7
}
```

HTTP Status: **400 Bad Request** ❌

---

## 🔍 Orden Correcto de Anotaciones

### ✅ **CORRECTO - @Valid primero**

```java
@PostMapping
public ResponseEntity<EventoResponseDTO> crearEvento(
    @Valid                          // 1. Validar primero
    @RequestBody                    // 2. Deserializar después
    CrearEventoRequest request) {
    ...
}
```

### ❌ **INCORRECTO - @RequestBody primero**

```java
@PostMapping
public ResponseEntity<EventoResponseDTO> crearEvento(
    @RequestBody                    // ❌ Orden incorrecto
    @Valid
    CrearEventoRequest request) {
    ...
}
```

---

## 📝 Ejemplo Completo - Request & Response

### **1. Cliente envía petición POST**

```bash
POST /api/eventos HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{
  "tipoId": 1,
  "creadorId": 5,
  "titulo": "Junta Directiva",
  "descripcion": "Reunión trimestral de la junta",
  "fechaInicio": "2025-06-20T15:00:00",
  "fechaFin": "2025-06-20T16:30:00",
  "lugar": "Salón Principal",
  "gruposAfectados": "Directivos, Coordinadores",
  "enlaceDocumento": "https://docs.example.com/junta-q2",
  "numAsistentes": 12,
  "estado": "CONFIRMADO"
}
```

### **2. Spring deserializa JSON → CrearEventoRequest**

```java
CrearEventoRequest {
  tipoId: 1,
  creadorId: 5,
  titulo: "Junta Directiva",
  ...
}
```

### **3. Spring ejecuta @Valid**

```
✅ @NotNull tipoId = 1 ✓
✅ @NotNull creadorId = 5 ✓
✅ @NotBlank titulo = "Junta Directiva" ✓
✅ @Size(max=100) titulo.length = 15 ✓
✅ @FutureOrPresent fechaInicio = 2025-06-20 ✓
✅ @NotBlank lugar = "Salón Principal" ✓
✅ @NotBlank gruposAfectados = "..." ✓
✅ @Positive numAsistentes = 12 ✓
✅ @NotNull estado = CONFIRMADO ✓
```

### **4. Validaciones pasan → Se ejecuta método**

```java
public ResponseEntity<EventoResponseDTO> crearEvento(
    CrearEventoRequest request) {
    
    // Los datos están garantizados como válidos aquí
    Evento creado = eventoService.crearEvento(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(...);
}
```

### **5. Respuesta exitosa 201**

```json
HTTP/1.1 201 Created
Content-Type: application/json

{
  "id": 123,
  "tipoId": 1,
  "tipoNombre": "Reunión Administrativa",
  "titulo": "Junta Directiva",
  "descripcion": "Reunión trimestral de la junta",
  "fechaInicio": "2025-06-20T15:00:00",
  "lugar": "Salón Principal",
  "gruposAfectados": "Directivos, Coordinadores",
  "estado": "CONFIRMADO",
  "creadorId": 5,
  "creadorNombre": "Admin"
}
```

---

## 🎓 Buenas Prácticas

### ✅ **HECHO CORRECTAMENTE**

```java
@PostMapping
public ResponseEntity<EventoResponseDTO> crearEvento(
    @Valid @RequestBody CrearEventoRequest request) {
    
    // Los datos están garantizados como válidos
    Evento creado = eventoService.crearEvento(request);
    EventoResponseDTO response = eventoService.convertirAResponse(creado);
    
    // Usar Status HTTP adecuado
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
}
```

**Ventajas:**
- ✅ Validaciones automáticas
- ✅ Falla rápido antes de lógica de negocio
- ✅ Mensajes de error consistentes
- ✅ HTTP 201 en éxito, 400 en error
- ✅ Sin código adicional de validación

### ❌ **EVITAR**

```java
@PostMapping
public ResponseEntity<EventoResponseDTO> crearEvento(
    @RequestBody CrearEventoRequest request) {  // Sin @Valid
    
    // No hay validaciones automáticas
    // Código vulnerable a datos inválidos
    if (request.getTitulo() == null || request.getTitulo().isEmpty()) {
        // Validación manual (repetitiva)
        ...
    }
    
    Evento creado = eventoService.crearEvento(request);
    return ResponseEntity.ok(...);
}
```

**Problemas:**
- ❌ Validaciones manuales (código duplicado)
- ❌ Más código, menos mantenible
- ❌ Posible inconsistencia en mensajes de error
- ❌ No sigue estándares Spring Boot

---

## 🧪 Prueba de Validación (curl)

### **Test 1: Petición válida**

```bash
curl -X POST http://localhost:8080/api/eventos \
  -H "Content-Type: application/json" \
  -d '{
    "tipoId": 1,
    "creadorId": 5,
    "titulo": "Evento de prueba",
    "fechaInicio": "2025-12-31T18:00:00",
    "lugar": "Auditorio",
    "gruposAfectados": "Todos",
    "numAsistentes": 50,
    "estado": "PENDIENTE"
  }' | json_pp
```

**Esperado:** `201 Created` ✅

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
  }' | json_pp
```

**Esperado:** `400 Bad Request` con error "El título es obligatorio..." ❌

### **Test 3: Fecha en pasado**

```bash
curl -X POST http://localhost:8080/api/eventos \
  -H "Content-Type: application/json" \
  -d '{
    "tipoId": 1,
    "creadorId": 5,
    "titulo": "Evento",
    "fechaInicio": "2024-01-01T10:00:00",
    "lugar": "Auditorio",
    "gruposAfectados": "Todos",
    "numAsistentes": 50,
    "estado": "PENDIENTE"
  }' | json_pp
```

**Esperado:** `400 Bad Request` con error "La fecha de inicio no puede estar en el pasado" ❌

---

## 📚 Referencias

- [Spring Boot Validation Official Guide](https://docs.spring.io/spring-framework/reference/core/validation/annotations.html)
- [Jakarta Bean Validation](https://jakarta.ee/specifications/bean-validation/)
- [ControllerAdvice - Spring Documentation](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/advice.html)
- [Exception Handling in Spring REST](https://spring.io/blog/2013/11/01/exception-handling-in-spring-mvc)

---

## ✨ Resumen

| Concepto | Propósito | Ubicación |
|----------|-----------|-----------|
| `@Valid` | Activa validaciones automáticas | Antes de `@RequestBody` |
| `@RequestBody` | Deserializa JSON a objeto | En parámetro del método |
| `MethodArgumentNotValidException` | Excepción cuando validación falla | Lanzada por Spring automáticamente |
| `GlobalExceptionHandler` | Captura excepciones globalmente | Controlador con `@ControllerAdvice` |
| `@ExceptionHandler` | Método que maneja una excepción | En GlobalExceptionHandler |
| `@ResponseStatus` | Especifica código HTTP de respuesta | En método de excepción |

**Tu aplicación ahora tiene validaciones profesionales activadas en todos los endpoints REST** 🚀
