package agenda.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import agenda.model.Usuario;
import agenda.repository.UsuarioRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controlador REST para gestionar usuarios.
 * 
 * Validaciones activas:
 * - @Valid activa automáticamente todas las validaciones del modelo Usuario
 * - Los errores de validación se capturan en GlobalExceptionHandler
 * - Respuestas HTTP: 201 Created (éxito), 400 Bad Request (validación fallida)
 */
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;

    /**
     * Crea un nuevo usuario con validaciones automáticas.
     * 
     * @Valid: Activa la validación de Jakarta Validation en el modelo Usuario
     * 
     * @param usuario Modelo Usuario (validado automáticamente)
     * @return 201 Created con el usuario creado en caso de éxito
     *         400 Bad Request si falla la validación
     */
    @PostMapping
    public ResponseEntity<Usuario> crearUsuario(@Valid @RequestBody Usuario usuario) {
        Usuario creado = usuarioRepository.save(usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    /**
     * Obtiene todos los usuarios.
     * 
     * @return 200 OK con lista de usuarios
     */
    @GetMapping
    public ResponseEntity<List<Usuario>> listarUsuarios() {
        return ResponseEntity.ok(usuarioRepository.findAll());
    }

    /**
     * Busca un usuario por correo electrónico.
     * 
     * @param email Correo del usuario
     * @return 200 OK si existe, 404 Not Found si no existe
     */
    @GetMapping("/email/{email}")
    public ResponseEntity<Usuario> buscarPorEmail(@PathVariable String email) {
        return usuarioRepository.findByEmail(email)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}