package agenda.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import agenda.enums.EstadoEvento;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventoResponseDTO {

    private Long id;
    private Long tipoId;
    private String tipoNombre;
    private String tipoColor;
    private Integer tipoPrioridad;
    private String titulo;
    private String descripcion;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaInicio;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaFin;
    private String lugar;
    private String gruposAfectados;
    private EstadoEvento estado;
    private Long creadorId;
    private String creadorNombre;
    private String motivoRechazo;
    private List<ResponsableDTO> responsables;
}