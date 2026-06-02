package agenda.dto.importacion;

import java.util.List;

import jakarta.validation.Valid;
import lombok.Data;

@Data
public class ImportarFestivosConfirmarRequestDTO {

    @Valid
    private List<ImportarFestivoDTO> eventos;
}