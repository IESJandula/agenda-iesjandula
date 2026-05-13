# 🏗️ Manejo Profesional de Errores en APIs REST - Spring Boot 3

## 📋 Resumen

Implementación completa de manejo de errores JSON profesionales en Spring Boot 3, siguiendo estándares REST y mejores prácticas enterprise.

## 🎯 Estructura de Respuesta de Error

### **Formato JSON Profesional**

```json
{
  "timestamp": "2025-05-12T14:30:45.123",
  "status": 400,
  "error": "Validation Error",
  "message": "Los datos proporcionados contienen errores de validación",
  "path": "/api/eventos",
  "fields": {
    "titulo": "El título es obligatorio",
    "fechaInicio": "La fecha no puede estar en el pasado"
  }
}
```

### **Campos de la Respuesta**

| Campo | Tipo | Descripción | Obligatorio |
|-------|------|-------------|-------------|
| `timestamp` | String (ISO-8601) | Momento exacto del error | ✅ |
| `status` | Integer | Código HTTP | ✅ |
| `error` | String | Tipo de error | ✅ |
| `message` | String | Mensaje descriptivo | ❌ |
| `path` | String | Ruta del endpoint | ❌ |
| `fields` | Map<String, String> | Errores por campo (solo validaciones) | ❌ |

---

## 📁 Archivos Implementados

### **1. DTO de Error - `ErrorResponseDTO.java`**

```java
package com.example.agenda.exception.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponseDTO {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private Map<String, String> fields;
}
```

**Características:**
- ✅ `@JsonInclude(JsonInclude.Include.NON_NULL)` - No incluye campos null
- ✅ `@Builder` - Construcción fluida
- ✅ Compatible con Jackson (Spring Boot 3)

---

### **2. Manejador Global - `GlobalExceptionHandlerRest.java`**

```java
package com.example.agenda.exception;

import com.example.agenda.exception.dto.ErrorResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationException(
            MethodArgumentNotValidException ex,
            WebRequest request) {
        
        log.warn("Error de validación detectado en: {}", getPath(request));
        
        Map<String, String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                    fieldError -> fieldError.getField(),
                    fieldError -> fieldError.getDefaultMessage(),
                    (existing, replacement) -> existing,
                    LinkedHashMap::new
                ));
        
        ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Error")
                .message("Los datos proporcionados contienen errores de validación")
                .path(getPath(request))
                .fields(fieldErrors)
                .build();
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    // ... otros manejadores
}
```

**Excepciones Manejadas:**
- ✅ `MethodArgumentNotValidException` - Errores de validación (@Valid)
- ✅ `Exception` - Errores genéricos (500)
- ✅ `IllegalArgumentException` - Argumentos inválidos (400)

---

### **3. Controlador de Ejemplo - `EventoControllerExample.java`**

```java
@RestController
@RequestMapping("/api/eventos")
@RequiredArgsConstructor
public class EventoController {

    private final EventoService eventoService;

    @PostMapping
    public ResponseEntity<Evento> crearEvento(@Valid @RequestBody CrearEventoRequest request) {
        Evento evento = eventoService.crearEvento(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(evento);
    }
}
```

**Punto clave:** Solo necesitas `@Valid` en el parámetro. El resto lo maneja automáticamente el `@ControllerAdvice`.

---

## 🧪 Ejemplos de Uso

### **Test 1: Validación Exitosa**

```bash
curl -X POST http://localhost:8080/api/eventos \
  -H "Content-Type: application/json" \
  -d '{
    "titulo": "Reunión de Equipo",
    "fechaInicio": "2025-06-15T10:00:00",
    "tipoId": 1,
    "creadorId": 1
  }'
```

**Respuesta: HTTP 201 Created** ✅

```json
{
  "id": 1,
  "titulo": "Reunión de Equipo",
  "fechaInicio": "2025-06-15T10:00:00"
}
```

---

### **Test 2: Errores de Validación**

```bash
curl -X POST http://localhost:8080/api/eventos \
  -H "Content-Type: application/json" \
  -d '{
    "titulo": "",
    "fechaInicio": "2020-01-01T10:00:00"
  }'
```

**Respuesta: HTTP 400 Bad Request** ❌

```json
{
  "timestamp": "2025-05-12T14:35:22.456",
  "status": 400,
  "error": "Validation Error",
  "message": "Los datos proporcionados contienen errores de validación",
  "path": "/api/eventos",
  "fields": {
    "titulo": "El título es obligatorio",
    "fechaInicio": "La fecha no puede estar en el pasado",
    "tipoId": "El tipo de evento es obligatorio",
    "creadorId": "El creador es obligatorio"
  }
}
```

---

### **Test 3: Error Genérico**

```bash
curl -X GET http://localhost:8080/api/eventos/999999
```

**Respuesta: HTTP 500 Internal Server Error** (ejemplo)

```json
{
  "timestamp": "2025-05-12T14:40:15.789",
  "status": 500,
  "error": "Internal Server Error",
  "message": "Ha ocurrido un error interno en el servidor",
  "path": "/api/eventos/999999"
}
```

---

## 🚀 Cómo Implementar

### **Paso 1: Agregar Dependencias**

En `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```

### **Paso 2: Crear DTO de Error**

Copia `ErrorResponseDTO.java` en tu paquete `exception.dto`.

### **Paso 3: Crear Manejador Global**

Copia `GlobalExceptionHandlerRest.java` en tu paquete `exception`.

### **Paso 4: Usar en Controladores**

```java
@PostMapping
public ResponseEntity<?> crear(@Valid @RequestBody MiDTO request) {
    // Tu lógica aquí
    return ResponseEntity.created(uri).body(entidad);
}
```

### **Paso 5: Configurar DTOs con Validaciones**

```java
public class CrearEventoRequest {
    
    @NotBlank(message = "El título es obligatorio")
    @Size(max = 100, message = "El título no puede exceder 100 caracteres")
    private String titulo;
    
    @NotNull(message = "La fecha de inicio es obligatoria")
    @FutureOrPresent(message = "La fecha no puede estar en el pasado")
    private LocalDateTime fechaInicio;
    
    // ... otros campos
}
```

---

## ✨ Características Enterprise

### **✅ Automático**
- No necesitas código adicional en controladores
- Se activa automáticamente con `@Valid`

### **✅ Consistente**
- Formato JSON idéntico para todos los errores
- Mensajes en español

### **✅ Profesional**
- Timestamp preciso
- Código HTTP correcto
- Errores por campo detallados

### **✅ Logging**
- Registra automáticamente errores en logs
- Facilita debugging en producción

### **✅ Compatible**
- Spring Boot 3 nativo
- Jakarta Validation (JSR-303/380)
- Jackson para JSON

---

## 🔧 Personalización

### **Agregar Más Manejadores**

```java
@ExceptionHandler(ResourceNotFoundException.class)
public ResponseEntity<ErrorResponseDTO> handleNotFound(ResourceNotFoundException ex, WebRequest request) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponseDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(404)
                .error("Not Found")
                .message(ex.getMessage())
                .path(getPath(request))
                .build());
}
```

### **Mensajes en Diferentes Idiomas**

Usa `MessageSource` de Spring:

```java
@Autowired
private MessageSource messageSource;

// En el manejador:
String message = messageSource.getMessage("error.validation.title", null, LocaleContextHolder.getLocale());
```

### **Campos Adicionales**

Agrega campos al `ErrorResponseDTO`:

```java
private String requestId;  // Para tracing
private String userId;     // Para auditoría
private List<String> details; // Lista de errores
```

---

## 📚 Referencias

- [Spring Boot Validation](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.validation)
- [Jakarta Validation](https://jakarta.ee/specifications/bean-validation/3.0/)
- [REST API Error Handling Best Practices](https://tools.ietf.org/html/rfc7807)

---

## 🎯 Checklist de Implementación

- [x] DTO de error con estructura profesional
- [x] Manejador global @ControllerAdvice
- [x] Manejo de MethodArgumentNotValidException
- [x] Manejo de excepciones genéricas
- [x] Logging automático
- [x] Compatible con Spring Boot 3
- [x] Ejemplos de uso y testing
- [x] Documentación completa

**¡Tu API REST ahora devuelve errores JSON profesionales!** 🚀