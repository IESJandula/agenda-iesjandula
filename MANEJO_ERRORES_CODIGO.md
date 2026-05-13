# 💾 Código Completo Ready-to-Copy

## 🔗 Todos los Archivos Creados

---

## **1. ErrorResponse.java** (DTO)

```java
package agenda.exception.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO para respuestas de error estandarizado.
 * 
 * Estructura profesional que incluye:
 * - Timestamp: Cuándo ocurrió el error
 * - Status: Código HTTP
 * - Error: Tipo de error
 * - Message: Descripción general
 * - Path: Ruta del endpoint afectado
 * - Details: Detalles específicos (opcional)
 * - FieldErrors: Errores por campo en validaciones (opcional)
 * 
 * @JsonInclude(JsonInclude.Include.NON_NULL): No incluye campos null en JSON
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    
    /**
     * Timestamp exacto cuando ocurrió el error
     */
    private LocalDateTime timestamp;
    
    /**
     * Código HTTP de la respuesta (400, 404, 500, etc.)
     */
    private Integer status;
    
    /**
     * Tipo de error (ej: "Validación fallida", "Recurso no encontrado")
     */
    private String error;
    
    /**
     * Mensaje descriptivo del error
     */
    private String message;
    
    /**
     * Ruta del endpoint que causó el error
     */
    private String path;
    
    /**
     * Información adicional del error (stack trace en desarrollo, etc.)
     * Optional - No se incluye en producción
     */
    private String details;
    
    /**
     * Mapa de errores de validación por campo
     * Ej: { "titulo": "Campo obligatorio", "fecha": "Fecha inválida" }
     */
    private Map<String, String> fieldErrors;
    
    /**
     * Contador de errores de validación
     */
    private Integer errorCount;
    
    /**
     * ID único de solicitud para logging y debugging
     */
    private String requestId;
    
    /**
     * Lista de errores adicionales
     */
    private List<String> errors;
}
```

---

## **2. ResourceNotFoundException.java**

```java
package agenda.exception;

/**
 * Excepción personalizada para recursos no encontrados.
 * Se lanza cuando se solicita un recurso que no existe en BD.
 * 
 * HTTP Status: 404 Not Found
 */
public class ResourceNotFoundException extends RuntimeException {
    
    private String resourceName;
    private String fieldName;
    private Object fieldValue;
    
    /**
     * Constructor simple con mensaje
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
    
    /**
     * Constructor completo para logging detallado
     * 
     * @param resourceName Nombre del recurso (ej: "Evento")
     * @param fieldName Campo por el que se buscaba (ej: "id")
     * @param fieldValue Valor del campo (ej: 123)
     */
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s no encontrado con %s: '%s'", 
            resourceName, fieldName, fieldValue));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }
    
    public String getResourceName() {
        return resourceName;
    }
    
    public String getFieldName() {
        return fieldName;
    }
    
    public Object getFieldValue() {
        return fieldValue;
    }
}
```

---

## **3. DuplicateResourceException.java**

```java
package agenda.exception;

/**
 * Excepción personalizada para recursos duplicados.
 * Se lanza cuando se intenta crear un recurso que ya existe.
 * 
 * HTTP Status: 409 Conflict
 */
public class DuplicateResourceException extends RuntimeException {
    
    private String resourceName;
    private String fieldName;
    private Object fieldValue;
    
    /**
     * Constructor simple
     */
    public DuplicateResourceException(String message) {
        super(message);
    }
    
    /**
     * Constructor completo
     * 
     * @param resourceName Nombre del recurso (ej: "Usuario")
     * @param fieldName Campo que tiene el duplicado (ej: "email")
     * @param fieldValue Valor del campo (ej: "usuario@example.com")
     */
    public DuplicateResourceException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s con %s '%s' ya existe", 
            resourceName, fieldName, fieldValue));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }
    
    public String getResourceName() {
        return resourceName;
    }
    
    public String getFieldName() {
        return fieldName;
    }
    
    public Object getFieldValue() {
        return fieldValue;
    }
}
```

---

## **4. BusinessException.java**

```java
package agenda.exception;

/**
 * Excepción personalizada para errores de negocio.
 * Se lanza cuando se viola una regla de negocio.
 * 
 * HTTP Status: 400 Bad Request o 409 Conflict
 */
public class BusinessException extends RuntimeException {
    
    private String errorCode;
    
    /**
     * Constructor simple
     */
    public BusinessException(String message) {
        super(message);
    }
    
    /**
     * Constructor con código de error
     * 
     * @param errorCode Código único del error para cliente
     * @param message Mensaje descriptivo
     */
    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
```

---

## **5. GlobalExceptionHandler.java** (COMPLETO)

```java
package agenda.exception;

import agenda.exception.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Manejador global centralizado de excepciones.
 * 
 * Propósito:
 * - Capturar automáticamente todas las excepciones no manejadas
 * - Devolver respuestas JSON consistentes y profesionales
 * - Proporcionar información detallada para debugging
 * - Implementar logging centralizado
 * - Cumplir con estándares REST y buenas prácticas enterprise
 * 
 * Excepciones manejadas:
 * 1. MethodArgumentNotValidException - Errores de validación @Valid
 * 2. ResourceNotFoundException - Recurso no encontrado (404)
 * 3. DuplicateResourceException - Recurso duplicado (409)
 * 4. BusinessException - Errores de lógica de negocio (400/409)
 * 5. IllegalArgumentException - Argumentos inválidos (400)
 * 6. Exception - Excepciones genéricas no esperadas (500)
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @Value("${app.environment:development}")
    private String environment;

    /**
     * Maneja excepciones de validación de Jakarta Validation (@Valid).
     * HTTP Status: 400 Bad Request
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            WebRequest request) {
        
        log.warn("Error de validación en endpoint: {}", getPath(request));
        
        Map<String, String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                    error -> error.getField(),
                    error -> error.getDefaultMessage(),
                    (existing, replacement) -> existing + "; " + replacement,
                    LinkedHashMap::new
                ));
        
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validación fallida")
                .message("Los datos enviados no cumplen con las validaciones requeridas")
                .path(getPath(request))
                .requestId(generateRequestId())
                .fieldErrors(fieldErrors)
                .errorCount(fieldErrors.size())
                .details(isProduction() ? null : ex.getMessage())
                .build();
        
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Maneja excepciones de recurso no encontrado.
     * HTTP Status: 404 Not Found
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex,
            WebRequest request) {
        
        log.warn("Recurso no encontrado: {} - {}", 
            ex.getResourceName(), ex.getFieldValue());
        
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Recurso no encontrado")
                .message(ex.getMessage())
                .path(getPath(request))
                .requestId(generateRequestId())
                .details(isProduction() ? null : String.format(
                    "Recurso: %s, Campo: %s, Valor: %s",
                    ex.getResourceName(), ex.getFieldName(), ex.getFieldValue()
                ))
                .build();
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Maneja excepciones de recurso duplicado.
     * HTTP Status: 409 Conflict
     */
    @ExceptionHandler(DuplicateResourceException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<ErrorResponse> handleDuplicateResourceException(
            DuplicateResourceException ex,
            WebRequest request) {
        
        log.warn("Recurso duplicado detectado: {} - {} = {}", 
            ex.getResourceName(), ex.getFieldName(), ex.getFieldValue());
        
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("Recurso duplicado")
                .message(ex.getMessage())
                .path(getPath(request))
                .requestId(generateRequestId())
                .details(isProduction() ? null : String.format(
                    "Recurso: %s ya existe con %s: %s",
                    ex.getResourceName(), ex.getFieldName(), ex.getFieldValue()
                ))
                .build();
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * Maneja excepciones de lógica de negocio.
     * HTTP Status: 400 Bad Request
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex,
            WebRequest request) {
        
        log.warn("Error de negocio: {} - Código: {}", 
            ex.getMessage(), ex.getErrorCode());
        
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Error de negocio")
                .message(ex.getMessage())
                .path(getPath(request))
                .requestId(generateRequestId())
                .details(isProduction() ? null : "Código de error: " + ex.getErrorCode())
                .build();
        
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Maneja IllegalArgumentException.
     * HTTP Status: 400 Bad Request
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex,
            WebRequest request) {
        
        log.warn("Argumento inválido: {}", ex.getMessage());
        
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Argumento inválido")
                .message(ex.getMessage())
                .path(getPath(request))
                .requestId(generateRequestId())
                .details(isProduction() ? null : ex.getMessage())
                .build();
        
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Maneja todas las excepciones no capturadas.
     * HTTP Status: 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            WebRequest request) {
        
        log.error("Error inesperado en endpoint: {}", getPath(request), ex);
        
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Error interno del servidor")
                .message("Ha ocurrido un error inesperado. Por favor, contacte al administrador.")
                .path(getPath(request))
                .requestId(generateRequestId())
                .details(isProduction() ? null : ex.toString())
                .build();
        
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

    /**
     * Extrae la ruta del endpoint desde la solicitud
     */
    private String getPath(WebRequest request) {
        String description = request.getDescription(false);
        return description.startsWith("uri=") ? description.substring(4) : description;
    }

    /**
     * Genera un ID único para la solicitud (para logging y debugging)
     */
    private String generateRequestId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Verifica si estamos en ambiente de producción
     */
    private boolean isProduction() {
        return "production".equalsIgnoreCase(environment);
    }
}
```

---

## 🔧 Uso en Servicios

### **EventoService.java**

```java
@Service
@RequiredArgsConstructor
public class EventoService {
    
    private final EventoRepository eventoRepository;
    private final TipoEventoRepository tipoEventoRepository;
    
    /**
     * Crear evento con validaciones de negocio
     */
    public Evento crearEvento(CrearEventoRequest request) {
        // Validar lógica de negocio
        if (request.getFechaInicio().isBefore(LocalDateTime.now())) {
            throw new BusinessException("FECHA_INVALIDA", 
                "La fecha no puede ser en el pasado");
        }
        
        // Verificar que tipo existe
        TipoEvento tipo = tipoEventoRepository.findById(request.getTipoId())
            .orElseThrow(() -> new ResourceNotFoundException("TipoEvento", "id", request.getTipoId()));
        
        Evento evento = new Evento();
        evento.setTipoEvento(tipo);
        evento.setTitulo(request.getTitulo());
        evento.setDescripcion(request.getDescripcion());
        evento.setFechaInicio(request.getFechaInicio());
        evento.setFechaFin(request.getFechaFin());
        evento.setLugar(request.getLugar());
        evento.setGruposAfectados(request.getGruposAfectados());
        evento.setNumAsistentes(request.getNumAsistentes());
        evento.setEstado(request.getEstado());
        
        return eventoRepository.save(evento);
    }
    
    /**
     * Obtener evento por ID
     */
    public Optional<EventoResponseDTO> obtenerPorId(Long id) {
        return eventoRepository.findById(id)
            .map(this::convertirAResponse);
    }
    
    /**
     * Actualizar evento
     */
    public EventoResponseDTO actualizarEvento(Long id, CrearEventoRequest request) {
        Evento evento = eventoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Evento", "id", id));
        
        // Validar lógica de negocio
        if (evento.getFechaInicio().isBefore(LocalDateTime.now())) {
            throw new BusinessException("EVENT_PAST_UPDATE", 
                "No se pueden actualizar eventos pasados");
        }
        
        evento.setTitulo(request.getTitulo());
        evento.setDescripcion(request.getDescripcion());
        evento.setFechaInicio(request.getFechaInicio());
        evento.setFechaFin(request.getFechaFin());
        evento.setLugar(request.getLugar());
        evento.setGruposAfectados(request.getGruposAfectados());
        evento.setNumAsistentes(request.getNumAsistentes());
        evento.setEstado(request.getEstado());
        
        return convertirAResponse(eventoRepository.save(evento));
    }
    
    /**
     * Eliminar evento
     */
    public boolean eliminarEvento(Long id) {
        Evento evento = eventoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Evento", "id", id));
        
        // Validar lógica de negocio
        if (evento.getFechaInicio().isBefore(LocalDateTime.now())) {
            throw new BusinessException("EVENT_PAST_DELETE", 
                "No se pueden eliminar eventos pasados");
        }
        
        eventoRepository.deleteById(id);
        return true;
    }
    
    /**
     * Convertir a response DTO
     */
    public EventoResponseDTO convertirAResponse(Evento evento) {
        return EventoResponseDTO.builder()
                .id(evento.getId())
                .tipoId(evento.getTipoEvento().getId())
                .tipoNombre(evento.getTipoEvento().getNombre())
                .tipoColor(evento.getTipoEvento().getColor())
                .titulo(evento.getTitulo())
                .descripcion(evento.getDescripcion())
                .fechaInicio(evento.getFechaInicio())
                .fechaFin(evento.getFechaFin())
                .lugar(evento.getLugar())
                .gruposAfectados(evento.getGruposAfectados())
                .estado(evento.getEstado())
                .creadorId(evento.getCreadorId())
                .build();
    }
}
```

---

## 🌍 application.properties

```properties
# Configuración del ambiente para GlobalExceptionHandler
app.environment=development  # O 'production'

# Logging
logging.level.root=INFO
logging.level.agenda=DEBUG
logging.level.agenda.exception=WARN
```

---

## ✨ Resumen de Uso

```bash
# Copiar todos los archivos listados arriba a:
# src/main/java/agenda/exception/
# src/main/java/agenda/exception/dto/
```

**Tu aplicación tiene manejo de errores enterprise completo** 🚀
