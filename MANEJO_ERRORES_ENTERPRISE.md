# 🛡️ Manejo Global de Errores Enterprise - Spring Boot 3

## 📋 Resumen

He implementado un sistema profesional y enterprise de manejo de excepciones usando `@ControllerAdvice`. La solución incluye:

- ✅ **GlobalExceptionHandler** - Captura centralizada de excepciones
- ✅ **ErrorResponse DTO** - Estructura estándar de respuestas de error
- ✅ **Excepciones personalizadas** - Para casos de negocio específicos
- ✅ **Logging centralizado** - Trazabilidad completa
- ✅ **ID de solicitud único** - Para debugging distribuido
- ✅ **Respuestas consistentes en JSON** - Compatible REST

---

## 🎯 Arquitectura

```
┌────────────────────┐
│   Cliente HTTP     │
│  POST /api/eventos │
│  {datos JSON}      │
└────────┬───────────┘
         │
         ▼
┌────────────────────────────────┐
│   Spring DispatcherServlet     │
│   Procesa solicitud            │
└────────┬───────────────────────┘
         │
         ├─→ Validación @Valid
         ├─→ Mapeo de parámetros
         ├─→ Ejecución de lógica
         │
         ├─→ ✅ Éxito → ResponseEntity 200/201
         │
         └─→ ❌ Excepción
              ↓
    ┌─────────────────────────────────┐
    │  GlobalExceptionHandler         │
    │  (@ControllerAdvice)            │
    │                                 │
    │  Mapeo de excepciones:          │
    │  • MethodArgumentNotValidEx.    │
    │  • ResourceNotFoundException    │
    │  • DuplicateResourceException   │
    │  • BusinessException            │
    │  • IllegalArgumentException     │
    │  • Exception (genérica)         │
    └──────────┬──────────────────────┘
               │
               ▼
    ┌─────────────────────────────────┐
    │  ErrorResponse DTO              │
    │  • timestamp                    │
    │  • status (HTTP)                │
    │  • error (tipo)                 │
    │  • message (descripción)        │
    │  • path (endpoint)              │
    │  • requestId (UUID)             │
    │  • fieldErrors (validación)     │
    │  • details (dev only)           │
    └──────────┬──────────────────────┘
               │
               ▼
    ┌─────────────────────────────────┐
    │   HTTP Response 400/404/500     │
    │   Content-Type: application/json│
    │   Body: JSON ErrorResponse      │
    └─────────────────────────────────┘
               │
               ▼
    ┌─────────────────────────────────┐
    │   Cliente recibe error          │
    │   formateado profesionalmente   │
    └─────────────────────────────────┘
```

---

## 📁 Estructura de Archivos Creados

```
src/main/java/agenda/exception/
├── GlobalExceptionHandler.java      ✅ Manejador central (mejorado)
├── ResourceNotFoundException.java    ✅ Excepciones personalizadas
├── DuplicateResourceException.java  ✅ Excepciones personalizadas
├── BusinessException.java           ✅ Excepciones personalizadas
└── dto/
    └── ErrorResponse.java           ✅ DTO de respuesta de error
```

---

## 🔍 Componentes Principales

### **1. ErrorResponse DTO**

```java
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private LocalDateTime timestamp;      // Cuándo ocurrió
    private Integer status;               // Código HTTP
    private String error;                 // Tipo de error
    private String message;               // Descripción
    private String path;                  // Endpoint
    private String requestId;             // ID único (UUID)
    private Map<String, String> fieldErrors;  // Errores por campo
    private Integer errorCount;           // Conteo de errores
    private String details;               // Detalles (solo en dev)
}
```

**Ventajas:**
- `@JsonInclude(NON_NULL)` - No incluye campos null en JSON
- `@Builder` - Construcción fluida
- Estructura consistente
- Información completa para debugging

---

### **2. GlobalExceptionHandler**

Maneja 6 tipos de excepciones:

#### **A. Validación (@Valid)**
```java
@ExceptionHandler(MethodArgumentNotValidException.class)
@ResponseStatus(HttpStatus.BAD_REQUEST)
public ResponseEntity<ErrorResponse> handleValidationException(...)
```

HTTP: **400 Bad Request**

---

#### **B. Recurso No Encontrado**
```java
@ExceptionHandler(ResourceNotFoundException.class)
@ResponseStatus(HttpStatus.NOT_FOUND)
public ResponseEntity<ErrorResponse> handleResourceNotFoundException(...)
```

HTTP: **404 Not Found**

---

#### **C. Recurso Duplicado**
```java
@ExceptionHandler(DuplicateResourceException.class)
@ResponseStatus(HttpStatus.CONFLICT)
public ResponseEntity<ErrorResponse> handleDuplicateResourceException(...)
```

HTTP: **409 Conflict**

---

#### **D. Lógica de Negocio**
```java
@ExceptionHandler(BusinessException.class)
@ResponseStatus(HttpStatus.BAD_REQUEST)
public ResponseEntity<ErrorResponse> handleBusinessException(...)
```

HTTP: **400 Bad Request**

---

#### **E. Argumentos Inválidos**
```java
@ExceptionHandler(IllegalArgumentException.class)
@ResponseStatus(HttpStatus.BAD_REQUEST)
public ResponseEntity<ErrorResponse> handleIllegalArgumentException(...)
```

HTTP: **400 Bad Request**

---

#### **F. Excepciones Genéricas**
```java
@ExceptionHandler(Exception.class)
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public ResponseEntity<ErrorResponse> handleGenericException(...)
```

HTTP: **500 Internal Server Error**

---

## 💡 Excepciones Personalizadas

### **ResourceNotFoundException**

```java
// Constructor simple
throw new ResourceNotFoundException("Evento con ID 123 no encontrado");

// Constructor completo (mejor para logging)
throw new ResourceNotFoundException("Evento", "id", 123);
```

**Uso en controlador:**
```java
@GetMapping("/{id}")
public ResponseEntity<EventoResponseDTO> obtenerPorId(@PathVariable Long id) {
    return eventoService.obtenerPorId(id)
        .map(ResponseEntity::ok)
        .orElseThrow(() -> new ResourceNotFoundException("Evento", "id", id));
}
```

---

### **DuplicateResourceException**

```java
// Constructor simple
throw new DuplicateResourceException("Usuario con email ya existe");

// Constructor completo
throw new DuplicateResourceException("Usuario", "email", "usuario@example.com");
```

**Uso en servicio:**
```java
public Usuario crearUsuario(Usuario usuario) {
    boolean exists = usuarioRepository.findByEmail(usuario.getEmail()).isPresent();
    if (exists) {
        throw new DuplicateResourceException("Usuario", "email", usuario.getEmail());
    }
    return usuarioRepository.save(usuario);
}
```

---

### **BusinessException**

```java
// Constructor simple
throw new BusinessException("No se puede eliminar un evento pasado");

// Constructor con código de error
throw new BusinessException("EVENT_PAST_DELETE", "No se puede eliminar un evento pasado");
```

**Uso en servicio:**
```java
public void eliminarEvento(Long id) {
    Evento evento = eventoRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Evento", "id", id));
    
    if (evento.getFechaInicio().isBefore(LocalDateTime.now())) {
        throw new BusinessException("EVENT_PAST", 
            "No se pueden eliminar eventos pasados");
    }
    
    eventoRepository.deleteById(id);
}
```

---

## 📊 Ejemplos de Respuestas

### **✅ Éxito: 201 Created**

```json
POST /api/eventos
Content-Type: application/json

{
  "tipoId": 1,
  "creadorId": 5,
  "titulo": "Reunión Importante",
  "fecha Inicio": "2025-06-20T14:00:00",
  "lugar": "Sala A",
  "gruposAfectados": "2ºDAW",
  "numAsistentes": 25,
  "estado": "PENDIENTE"
}

Response: HTTP 201 Created
{
  "id": 1,
  "titulo": "Reunión Importante",
  "estado": "PENDIENTE"
  ...
}
```

---

### **❌ Error: 400 Validación**

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
    "numAsistentes": 0,
    "estado": null
  }'
```

**Response: HTTP 400 Bad Request**

```json
{
  "timestamp": "2025-05-12T10:35:22.456",
  "status": 400,
  "error": "Validación fallida",
  "message": "Los datos enviados no cumplen con las validaciones requeridas",
  "path": "/api/eventos",
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "fieldErrors": {
    "tipoId": "El tipo de evento es requerido",
    "titulo": "El título es obligatorio y no puede estar vacío",
    "fechaInicio": "La fecha de inicio no puede estar en el pasado",
    "lugar": "El lugar del evento es requerido",
    "gruposAfectados": "Los grupos afectados son requeridos",
    "numAsistentes": "El número de asistentes debe ser mayor que 0",
    "estado": "El estado del evento es requerido"
  },
  "errorCount": 7,
  "details": "org.springframework.web.bind.MethodArgumentNotValidException: ..."
}
```

---

### **❌ Error: 404 No Encontrado**

```bash
curl -X GET http://localhost:8080/api/eventos/999
```

**Response: HTTP 404 Not Found**

```json
{
  "timestamp": "2025-05-12T10:40:15.789",
  "status": 404,
  "error": "Recurso no encontrado",
  "message": "Evento no encontrado con id: '999'",
  "path": "/api/eventos/999",
  "requestId": "660e8400-e29b-41d4-a716-446655440001",
  "details": "Recurso: Evento, Campo: id, Valor: 999"
}
```

---

### **❌ Error: 409 Conflicto (Duplicado)**

```bash
curl -X POST http://localhost:8080/api/usuarios \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Juan",
    "email": "juan@example.com",  // Email ya existe
    ...
  }'
```

**Response: HTTP 409 Conflict**

```json
{
  "timestamp": "2025-05-12T10:42:30.123",
  "status": 409,
  "error": "Recurso duplicado",
  "message": "Usuario con email 'juan@example.com' ya existe",
  "path": "/api/usuarios",
  "requestId": "770e8400-e29b-41d4-a716-446655440002",
  "details": "Recurso: Usuario ya existe con email: juan@example.com"
}
```

---

### **❌ Error: 500 Interno**

```json
{
  "timestamp": "2025-05-12T10:45:00.999",
  "status": 500,
  "error": "Error interno del servidor",
  "message": "Ha ocurrido un error inesperado. Por favor, contacte al administrador.",
  "path": "/api/eventos",
  "requestId": "880e8400-e29b-41d4-a716-446655440003",
  "details": "java.sql.SQLException: Conexión a BD fallida"  // Solo en desarrollo
}
```

---

## 🎓 Características Enterprise

### **1. Logging Centralizado**

```java
log.warn("Error de validación en endpoint: {}", getPath(request));
log.warn("Recurso no encontrado: {} - {}", ex.getResourceName(), ex.getFieldValue());
log.error("Error inesperado en endpoint: {}", getPath(request), ex);
```

**Salida esperada en consola:**
```
WARN  - Error de validación en endpoint: /api/eventos
WARN  - Recurso no encontrado: Evento - 123
ERROR - Error inesperado en endpoint: /api/eventos - java.io.IOException: ...
```

---

### **2. ID de Solicitud Único (RequestId)**

```java
private String generateRequestId() {
    return UUID.randomUUID().toString();
}
```

**Ventaja:** Trackear una solicitud a través de logs distribuidos

```json
{
  "requestId": "a1b2c3d4-e5f6-47g8-h9i0-j1k2l3m4n5o6"
}
```

---

### **3. Detalles Solo en Desarrollo**

```java
.details(isProduction() ? null : ex.getMessage())
```

**application.properties:**
```properties
app.environment=development  # O 'production'
```

- **Desarrollo:** Incluye stack traces y detalles técnicos
- **Producción:** Mensaje genérico, sin exponer internals

---

### **4. LinkedHashMap para Orden de Errores**

```java
.collect(Collectors.toMap(
    error -> error.getField(),
    error -> error.getDefaultMessage(),
    (existing, replacement) -> existing + "; " + replacement,
    LinkedHashMap::new  // Mantiene orden de inserción
))
```

Los errores aparecen en el JSON en el mismo orden que se validaron.

---

## 🔧 Integración en Servicios

### **Ejemplo: EventoService**

```java
@Service
@RequiredArgsConstructor
public class EventoService {
    
    private final EventoRepository eventoRepository;
    
    public Evento crearEvento(CrearEventoRequest request) {
        // Validar lógica de negocio
        if (request.getFechaInicio().isBefore(LocalDateTime.now())) {
            throw new BusinessException("FECHA_INVALIDA", 
                "La fecha no puede ser en el pasado");
        }
        
        Evento evento = new Evento();
        // Mapear campos...
        return eventoRepository.save(evento);
    }
    
    public Optional<EventoResponseDTO> obtenerPorId(Long id) {
        return eventoRepository.findById(id)
            .map(this::convertirAResponse);
    }
    
    public EventoResponseDTO actualizarEvento(Long id, CrearEventoRequest request) {
        Evento evento = eventoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Evento", "id", id));
        
        // Actualizar campos...
        return convertirAResponse(eventoRepository.save(evento));
    }
    
    public boolean eliminarEvento(Long id) {
        Evento evento = eventoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Evento", "id", id));
        
        if (evento.getFechaInicio().isBefore(LocalDateTime.now())) {
            throw new BusinessException("EVENT_PAST_DELETE", 
                "No se pueden eliminar eventos pasados");
        }
        
        eventoRepository.deleteById(id);
        return true;
    }
}
```

---

## 📋 Checklist de Implementación

- [x] ErrorResponse DTO creado
- [x] ResourceNotFoundException implementada
- [x] DuplicateResourceException implementada
- [x] BusinessException implementada
- [x] GlobalExceptionHandler mejorado con 6 handlers
- [x] Logging centralizado con @Slf4j
- [x] ID de solicitud único (UUID)
- [x] Detalles solo en desarrollo
- [x] LinkedHashMap para mantener orden
- [x] Respuestas consistentes en JSON
- [x] Códigos HTTP correctos (400, 404, 409, 500)
- [x] Compatible Spring Boot 3
- [x] Compatible Jakarta Validation

---

## 🚀 Uso Rápido

### **1. En Controlador (Resource Not Found)**

```java
@GetMapping("/{id}")
public ResponseEntity<EventoResponseDTO> obtenerPorId(@PathVariable Long id) {
    return eventoService.obtenerPorId(id)
        .map(ResponseEntity::ok)
        .orElseThrow(() -> new ResourceNotFoundException("Evento", "id", id));
}
```

### **2. En Servicio (Duplicate)**

```java
if (usuarioRepository.findByEmail(email).isPresent()) {
    throw new DuplicateResourceException("Usuario", "email", email);
}
```

### **3. En Servicio (Business Logic)**

```java
if (evento.getFechaInicio().isBefore(LocalDateTime.now())) {
    throw new BusinessException("EVENT_PAST", "No se pueden eliminar eventos pasados");
}
```

---

## 📚 Documentación Relacionada

- [VALIDACIONES.md](VALIDACIONES.md) - Validaciones de DTOs
- [VALIDACIONES_CONTROLADORES.md](VALIDACIONES_CONTROLADORES.md) - @Valid en controladores
- [VALIDACIONES_ARQUITECTURA.md](VALIDACIONES_ARQUITECTURA.md) - Flujo completo

---

## ✨ Resumen

| Componente | Propósito | Archivo |
|-----------|----------|---------|
| ErrorResponse | DTO estándar de error | `exception/dto/ErrorResponse.java` |
| GlobalExceptionHandler | Captura central | `exception/GlobalExceptionHandler.java` |
| ResourceNotFoundException | Recurso no encontrado (404) | `exception/ResourceNotFoundException.java` |
| DuplicateResourceException | Recurso duplicado (409) | `exception/DuplicateResourceException.java` |
| BusinessException | Lógica de negocio (400) | `exception/BusinessException.java` |

**Tu aplicación tiene manejo de errores enterprise implementado** 🚀
