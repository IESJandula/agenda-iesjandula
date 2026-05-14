package agenda.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CrearTipoEventoRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 3, max = 80, message = "El nombre debe tener entre 3 y 80 caracteres")
    private String nombre;

    @NotBlank(message = "El color es obligatorio")
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "El color debe tener formato hexadecimal (#RRGGBB)")
    private String color;

    private String icono;

    @NotNull(message = "La prioridad es obligatoria")
    @Min(value = 0, message = "La prioridad debe ser un valor positivo o cero")
    private Integer prioridad;

    @NotNull(message = "El estado activo es obligatorio")
    private Boolean activo;
}