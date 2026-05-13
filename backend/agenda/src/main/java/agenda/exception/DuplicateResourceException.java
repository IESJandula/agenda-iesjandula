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
