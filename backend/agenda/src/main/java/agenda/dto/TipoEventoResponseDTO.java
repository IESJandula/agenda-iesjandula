package agenda.dto;

import lombok.Data;

@Data
public class TipoEventoResponseDTO {

    private Long id;
    private String nombre;
    private String color;
    private String icono;
    private int prioridad;
    private boolean activo;
    private boolean protegido;
}