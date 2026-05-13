package agenda.exception.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
