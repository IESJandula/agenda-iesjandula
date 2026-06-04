package agenda.init;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import agenda.enums.Rol;
import agenda.model.Usuario;
import agenda.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UsuarioInitializer implements CommandLineRunner {

    private static final String ADMIN_EMAIL = "admin@iesjandula.es";
    private static final String ADMIN_NOMBRE = "Administrador";
    private static final String ADMIN_PASSWORD = "Admin1234!";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        boolean existeAlgunAdmin = usuarioRepository.findAll().stream()
                .anyMatch(usuario -> Rol.ADMIN.equals(usuario.getRol()));

        if (existeAlgunAdmin) {
            return;
        }

        Usuario admin = Usuario.builder()
                .email(ADMIN_EMAIL)
                .nombre(ADMIN_NOMBRE)
                .password(passwordEncoder.encode(ADMIN_PASSWORD))
                .rol(Rol.ADMIN)
                .build();

        usuarioRepository.save(admin);
    }
}