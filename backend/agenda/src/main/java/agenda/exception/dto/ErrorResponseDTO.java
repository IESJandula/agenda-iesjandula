package agenda.exception.dto;

import java.time.LocalDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para respuestas de error estandarizado en APIs REST.
 *
 * Estructura profesional que incluye:
 * - timestamp: Momento exacto del error
 * - status: Código HTTP
 * - error: Tipo de error
 * - fields: Mapa de errores por campo (para validaciones)
 * - message: Mensaje general (opcional)
 * - path: Ruta del endpoint (opcional)
 *
 * Compatible con Spring Boot 3 y Jackson.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponseDTO {

    /**
     * Timestamp del error en formato ISO-8601
     */
    private LocalDateTime timestamp;

    /**
     * Código de estado HTTP
     */
    private int status;

    /**
     * Tipo de error (ej: "Validation Error", "Not Found")
     */
    private String error;

    /**
     * Mensaje descriptivo del error
     */
    private String message;

    /**
     * Ruta del endpoint que generó el error
     */
    private String path;

    /**
     * Mapa de errores específicos por campo
     * Ejemplo: {"titulo": "El título es obligatorio"}
     */
    private Map<String, String> fields;
}