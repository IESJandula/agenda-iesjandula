package agenda.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import agenda.enums.Rol;
import lombok.Data;

@Data
public class UsuarioResponseDTO {

    private Long id;
    private String email;
    private String nombre;
    private Rol rol;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaAlta;
}