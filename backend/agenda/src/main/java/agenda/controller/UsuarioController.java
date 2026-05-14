package agenda.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import agenda.dto.CrearUsuarioRequest;
import agenda.dto.UsuarioResponseDTO;
import agenda.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controlador REST para gestionar usuarios.
 * 
 * Delegación al servicio de usuarios:
 * - El controlador solo orquesta la petición HTTP
 * - La lógica de negocio vive en UsuarioService
 */
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    /**
     * Crea un nuevo usuario.
     * 
     * @param request Datos del usuario a crear
     * @return 201 Created con el usuario creado
     */
    @PostMapping
    public ResponseEntity<UsuarioResponseDTO> crearUsuario(@Valid @RequestBody CrearUsuarioRequest request) {
        UsuarioResponseDTO creado = usuarioService.crearUsuario(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    /**
     * Obtiene todos los usuarios.
     * 
     * @return 200 OK con lista de usuarios
     */
    @GetMapping
    public ResponseEntity<List<UsuarioResponseDTO>> listarUsuarios() {
        return ResponseEntity.ok(usuarioService.obtenerTodos());
    }

    /**
     * Obtiene un usuario por id.
     * 
     * @param id Identificador del usuario
     * @return 200 OK con el usuario
     */
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.obtenerPorId(id));
    }

    /**
     * Actualiza un usuario existente.
     * 
     * @param id Identificador del usuario
     * @param request Nuevos datos del usuario
     * @return 200 OK con el usuario actualizado
     */
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> actualizarUsuario(
            @PathVariable Long id,
            @Valid @RequestBody agenda.dto.ActualizarUsuarioRequest request) {
        return ResponseEntity.ok(usuarioService.actualizarUsuario(id, request));
    }

    /**
     * Elimina un usuario por id.
     * 
     * @param id Identificador del usuario
     * @return 204 No Content si se elimina correctamente
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarUsuario(@PathVariable Long id) {
        usuarioService.eliminarUsuario(id);
        return ResponseEntity.noContent().build();
    }
}