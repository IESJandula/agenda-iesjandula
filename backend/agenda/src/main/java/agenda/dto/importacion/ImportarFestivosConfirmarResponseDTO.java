package agenda.dto.importacion;

import java.util.List;

import agenda.dto.EventoResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportarFestivosConfirmarResponseDTO {

    private List<EventoResponseDTO> eventos;
    private int importados;
    private int omitidosDuplicados;
    private int omitidosPorDuplicado;
    private List<ImportarFestivoErrorDTO> errores;
}