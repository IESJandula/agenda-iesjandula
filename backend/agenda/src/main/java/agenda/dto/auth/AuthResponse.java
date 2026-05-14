package agenda.dto.auth;

import agenda.enums.Rol;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String token;

    private String tipoToken; // ej. "Bearer"

    private String email;

    private String nombre;

    private Rol rol;
}
