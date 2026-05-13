package agenda.exception;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import agenda.exception.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @Value("${app.environment:development}")
    private String environment;

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
                        LinkedHashMap::new));

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

        @ExceptionHandler(HttpMessageNotReadableException.class)
        @ResponseStatus(HttpStatus.BAD_REQUEST)
        public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
                        HttpMessageNotReadableException ex,
                        WebRequest request) {

                log.warn("JSON mal formado o no legible en endpoint: {}", getPath(request));

                ErrorResponse response = ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.BAD_REQUEST.value())
                                .error("JSON inválido")
                                .message("El cuerpo de la petición no tiene un formato JSON válido")
                                .path(getPath(request))
                                .requestId(generateRequestId())
                                .details(isProduction() ? null : ex.getMostSpecificCause().getMessage())
                                .build();

                return ResponseEntity.badRequest().body(response);
        }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex,
            WebRequest request) {

        log.warn("Recurso no encontrado: {} - {}", ex.getResourceName(), ex.getFieldValue());

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Recurso no encontrado")
                .message(ex.getMessage())
                .path(getPath(request))
                .requestId(generateRequestId())
                .details(isProduction() ? null : String.format(
                        "Recurso: %s, Campo: %s, Valor: %s",
                        ex.getResourceName(), ex.getFieldName(), ex.getFieldValue()))
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

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
                        ex.getResourceName(), ex.getFieldName(), ex.getFieldValue()))
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

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

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private String getPath(WebRequest request) {
        String description = request.getDescription(false);
        return description.startsWith("uri=") ? description.substring(4) : description;
    }

    private String generateRequestId() {
        return UUID.randomUUID().toString();
    }

    private boolean isProduction() {
        return "production".equalsIgnoreCase(environment);
    }
}