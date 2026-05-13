# 🧪 Pruebas y Ejemplos de Uso

## 🎯 Test Suite - GlobalExceptionHandler

### **Test 1: Validación Fallida**

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
    "numAsistentes": -1,
    "estado": null
  }' | jq .
```

**Respuesta esperada: HTTP 400 Bad Request**

```json
{
  "timestamp": "2025-05-12T10:30:45.123456",
  "status": 400,
  "error": "Validación fallida",
  "message": "Los datos enviados no cumplen con las validaciones requeridas",
  "path": "/api/eventos",
  "requestId": "a1b2c3d4-e5f6-47g8-h9i0-j1k2l3m4n5o6",
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
  "details": "org.springframework.web.bind.MethodArgumentNotValidException: Validation failed for argument..."
}
```

---

### **Test 2: Recurso No Encontrado**

```bash
curl -X GET http://localhost:8080/api/eventos/999 | jq .
```

**Respuesta esperada: HTTP 404 Not Found**

```json
{
  "timestamp": "2025-05-12T10:32:10.654321",
  "status": 404,
  "error": "Recurso no encontrado",
  "message": "Evento no encontrado con id: '999'",
  "path": "/api/eventos/999",
  "requestId": "b2c3d4e5-f6g7-48h9-i0j1-k2l3m4n5o6p7",
  "details": "Recurso: Evento, Campo: id, Valor: 999"
}
```

---

### **Test 3: Recurso Duplicado**

```bash
# Crear primer usuario
curl -X POST http://localhost:8080/api/usuarios \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Juan Pérez",
    "email": "juan@example.com"
  }'

# Intentar crear usuario con mismo email
curl -X POST http://localhost:8080/api/usuarios \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Otro Usuario",
    "email": "juan@example.com"
  }' | jq .
```

**Respuesta esperada: HTTP 409 Conflict**

```json
{
  "timestamp": "2025-05-12T10:33:45.789012",
  "status": 409,
  "error": "Recurso duplicado",
  "message": "Usuario con email 'juan@example.com' ya existe",
  "path": "/api/usuarios",
  "requestId": "c3d4e5f6-g7h8-49i0-j1k2-l3m4n5o6p7q8",
  "details": "Recurso: Usuario ya existe con email: juan@example.com"
}
```

---

### **Test 4: Error de Negocio**

```bash
# Crear evento futuro
curl -X POST http://localhost:8080/api/eventos \
  -H "Content-Type: application/json" \
  -d '{
    "tipoId": 1,
    "creadorId": 5,
    "titulo": "Evento Pasado",
    "fechaInicio": "2024-06-01T10:00:00",
    "lugar": "Sala A",
    "gruposAfectados": "2ºDAW",
    "numAsistentes": 20,
    "estado": "PENDIENTE"
  }' | jq .
```

**Respuesta esperada: HTTP 400 Bad Request**

```json
{
  "timestamp": "2025-05-12T10:34:20.456789",
  "status": 400,
  "error": "Error de negocio",
  "message": "No se pueden eliminar eventos pasados",
  "path": "/api/eventos",
  "requestId": "d4e5f6g7-h8i9-50j1-k2l3-m4n5o6p7q8r9",
  "details": "Código de error: EVENT_PAST_DELETE"
}
```

---

### **Test 5: Error Inesperado (500)**

```bash
# Este simularía una conexión a BD fallida, etc.
# Se captura automáticamente en Exception.class
```

**Respuesta esperada: HTTP 500 Internal Server Error**

```json
{
  "timestamp": "2025-05-12T10:35:00.123456",
  "status": 500,
  "error": "Error interno del servidor",
  "message": "Ha ocurrido un error inesperado. Por favor, contacte al administrador.",
  "path": "/api/eventos",
  "requestId": "e5f6g7h8-i9j0-51k1-l2m3-n4o5p6q7r8s9",
  "details": null
}
```

---

## 💡 Casos de Uso por Tipo de Excepción

### **Validación - MethodArgumentNotValidException**

```java
// Automáticamente lanzada por Spring cuando @Valid falla
// No necesita código adicional
```

**Cuándo se activa:**
- Campo @NotBlank vacío
- Campo @Size excede límite
- Campo @FutureOrPresent en pasado
- Campo @Positive no es positivo

---

### **Recurso No Encontrado - ResourceNotFoundException**

**Uso en Controlador:**
```java
@GetMapping("/{id}")
public ResponseEntity<EventoResponseDTO> obtenerPorId(@PathVariable Long id) {
    return eventoService.obtenerPorId(id)
        .map(ResponseEntity::ok)
        .orElseThrow(() -> new ResourceNotFoundException("Evento", "id", id));
}
```

**Uso en Servicio:**
```java
public EventoResponseDTO actualizarEvento(Long id, CrearEventoRequest request) {
    Evento evento = eventoRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Evento", "id", id));
    
    // Actualizar...
    return convertirAResponse(eventoRepository.save(evento));
}
```

---

### **Recurso Duplicado - DuplicateResourceException**

**Uso en Servicio:**
```java
public Usuario crearUsuario(Usuario usuario) {
    boolean exists = usuarioRepository.findByEmail(usuario.getEmail())
        .isPresent();
    
    if (exists) {
        throw new DuplicateResourceException("Usuario", "email", usuario.getEmail());
    }
    
    return usuarioRepository.save(usuario);
}
```

---

### **Error de Negocio - BusinessException**

**Uso en Servicio:**
```java
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
```

---

### **Argumento Inválido - IllegalArgumentException**

```java
public void procesarDatos(String dato) {
    if (dato == null || dato.trim().isEmpty()) {
        throw new IllegalArgumentException("El dato no puede estar vacío");
    }
    
    // Procesar...
}
```

---

### **Error Genérico - Exception**

Se captura automáticamente cualquier excepción no controlada.

---

## 📊 Tabla de Excepciones y HTTP Status

| Excepción | Handler | HTTP Status | Caso de Uso |
|-----------|---------|-------------|----------|
| `MethodArgumentNotValidException` | handleValidationException | **400** | Validación @Valid falla |
| `ResourceNotFoundException` | handleResourceNotFoundException | **404** | Recurso no existe |
| `DuplicateResourceException` | handleDuplicateResourceException | **409** | Recurso duplicado |
| `BusinessException` | handleBusinessException | **400** | Lógica de negocio fallida |
| `IllegalArgumentException` | handleIllegalArgumentException | **400** | Argumento inválido |
| `Exception` | handleGenericException | **500** | Error inesperado |

---

## 🔍 Debugging con RequestId

El `requestId` único en cada respuesta permite trackear errores:

**Log de error:**
```
2025-05-12 10:30:45.123 WARN  - Error de validación - RequestId: a1b2c3d4-e5f6-47g8-h9i0-j1k2l3m4n5o6
```

**Respuesta JSON:**
```json
{
  "requestId": "a1b2c3d4-e5f6-47g8-h9i0-j1k2l3m4n5o6"
}
```

Usar este ID en logs distribuidos para correlacionar solicitudes.

---

## 🌍 Diferencia Desarrollo vs Producción

### **Desarrollo (app.environment=development)**

```json
{
  "error": "Validación fallida",
  "details": "org.springframework.web.bind.MethodArgumentNotValidException: ...",
  "fieldErrors": { ... }
}
```

### **Producción (app.environment=production)**

```json
{
  "error": "Validación fallida",
  "details": null,
  "fieldErrors": { ... }
}
```

**Ventaja:** No expone internals de Spring en producción.

---

## 🚀 Integración Rápida

### **Paso 1: Copiar archivos**

```
exception/
├── GlobalExceptionHandler.java
├── ResourceNotFoundException.java
├── DuplicateResourceException.java
├── BusinessException.java
└── dto/
    └── ErrorResponse.java
```

### **Paso 2: Configurar application.properties**

```properties
app.environment=production
logging.level.agenda.exception=WARN
```

### **Paso 3: Usar en servicios**

```java
throw new ResourceNotFoundException("Evento", "id", id);
throw new DuplicateResourceException("Usuario", "email", email);
throw new BusinessException("CODE", "Mensaje error");
```

### **Paso 4: Listo**

GlobalExceptionHandler captura automáticamente todas las excepciones.

---

## ✨ Características Enterprise Implementadas

✅ **Captura centralizada** de excepciones  
✅ **DTO estándar** (ErrorResponse) para respuestas  
✅ **Excepciones personalizadas** para cada caso  
✅ **Logging detallado** con @Slf4j  
✅ **ID de solicitud único** (UUID)  
✅ **Detalles ocultos en producción** (seguridad)  
✅ **Códigos HTTP correctos** (400, 404, 409, 500)  
✅ **Timestamps exactos** para auditoría  
✅ **Rutas del endpoint** en respuestas  
✅ **Errores por campo** en validaciones  

---

## 📚 Documentación Completa

- [MANEJO_ERRORES_ENTERPRISE.md](MANEJO_ERRORES_ENTERPRISE.md) - Guía completa
- [MANEJO_ERRORES_CODIGO.md](MANEJO_ERRORES_CODIGO.md) - Código ready-to-copy
- [VALIDACIONES.md](VALIDACIONES.md) - Validaciones de DTOs
- [VALIDACIONES_CONTROLADORES.md](VALIDACIONES_CONTROLADORES.md) - @Valid en controladores

---

**Ready to deploy!** 🚀
