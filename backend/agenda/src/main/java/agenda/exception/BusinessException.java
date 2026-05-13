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
