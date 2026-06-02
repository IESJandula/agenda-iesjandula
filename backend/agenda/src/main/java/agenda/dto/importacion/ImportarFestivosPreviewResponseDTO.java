package agenda.dto.importacion;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportarFestivosPreviewResponseDTO {

    private List<ImportarFestivoDTO> eventos;
    private List<ImportarFestivoErrorDTO> errores;
    private List<ImportarFestivoErrorDTO> advertencias;
    private int validos;
    private int duplicados;
    private int totalErrores;
}