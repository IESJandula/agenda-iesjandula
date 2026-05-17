package agenda.dto.importacion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportarFestivoErrorDTO {

    private Integer linea;
    private String mensaje;
}