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
