package agenda.dto;

import java.time.LocalDateTime;

import agenda.enums.EstadoEvento;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CrearEventoRequest {

    @NotNull(message = "tipoId es requerido")
    private Long tipoId;

    @NotNull(message = "creadorId es requerido")
    private Long creadorId;

    @NotBlank(message = "titulo es requerido y no puede estar vacío")
    private String titulo;

    private String descripcion;

    @NotNull(message = "fechaInicio es requerida")
    private LocalDateTime fechaInicio;

    private LocalDateTime fechaFin;

    private String lugar;

    private String gruposAfectados;

    private String enlaceDocumento;

    @Min(value = 0, message = "numAsistentes no puede ser negativo")
    private Integer numAsistentes;

    private EstadoEvento estado;
}