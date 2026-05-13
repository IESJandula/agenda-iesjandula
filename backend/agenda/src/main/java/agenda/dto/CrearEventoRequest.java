package agenda.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import agenda.enums.EstadoEvento;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CrearEventoRequest {

    @NotNull(message = "El tipo de evento es obligatorio")
    private Long tipoId;

    @NotNull(message = "El creador del evento es obligatorio")
    private Long creadorId;

    @NotBlank(message = "El título es obligatorio")
    @Size(max = 100, message = "El título no puede superar los 100 caracteres")
    private String titulo;

    @Size(max = 500, message = "La descripción no puede superar los 500 caracteres")
    private String descripcion;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @FutureOrPresent(message = "La fecha de inicio no puede estar en el pasado")
    private LocalDateTime fechaInicio;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaFin;

    @NotBlank(message = "El lugar es obligatorio")
    private String lugar;

    @NotBlank(message = "Los grupos afectados son obligatorios")
    private String gruposAfectados;

    private String enlaceDocumento;

    @Positive(message = "El número de asistentes debe ser positivo")
    private Integer numAsistentes;

    @NotNull(message = "El estado del evento es obligatorio")
    private EstadoEvento estado;
}