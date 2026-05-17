package agenda.dto.importacion;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class ImportarFestivosConfirmarRequestDTO {

    @NotEmpty(message = "Debes enviar al menos un festivo para importar")
    @Valid
    private List<ImportarFestivoDTO> eventos;
}