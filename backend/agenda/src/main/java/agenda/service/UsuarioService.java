package agenda.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import agenda.dto.CrearUsuarioRequest;
import agenda.dto.UsuarioResponseDTO;
import agenda.exception.DuplicateResourceException;
import agenda.exception.ResourceNotFoundException;
import agenda.model.Usuario;
import agenda.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UsuarioResponseDTO crearUsuario(CrearUsuarioRequest request) {
        validarEmailDuplicado(request.getEmail(), null);

        Usuario usuario = Usuario.builder()
                .email(request.getEmail())
                .nombre(request.getNombre())
            .password(passwordEncoder.encode(request.getPassword()))
                .rol(request.getRol())
                .build();

        Usuario guardado = usuarioRepository.save(usuario);
        return convertirAResponse(guardado);
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> obtenerTodos() {
        return usuarioRepository.findAll()
                .stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UsuarioResponseDTO obtenerPorId(Long id) {
        Usuario usuario = obtenerEntidadPorId(id);
        return convertirAResponse(usuario);
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> buscarEntidadPorEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }

    @Transactional
    public UsuarioResponseDTO actualizarUsuario(Long id, agenda.dto.ActualizarUsuarioRequest request) {
        Usuario usuario = obtenerEntidadPorId(id);

        validarEmailDuplicado(request.getEmail(), id);

        usuario.setEmail(request.getEmail());
        usuario.setNombre(request.getNombre());
        // Password is not updated here; password change should use a dedicated endpoint
        usuario.setRol(request.getRol());

        Usuario actualizado = usuarioRepository.save(usuario);
        return convertirAResponse(actualizado);
    }

    @Transactional
    public void eliminarUsuario(Long id) {
        Usuario usuario = obtenerEntidadPorId(id);
        usuarioRepository.delete(usuario);
    }

    @Transactional(readOnly = true)
    public UsuarioResponseDTO convertirAResponse(Usuario usuario) {
        if (usuario == null) {
            return null;
        }

        UsuarioResponseDTO response = new UsuarioResponseDTO();
        response.setId(usuario.getId());
        response.setEmail(usuario.getEmail());
        response.setNombre(usuario.getNombre());
        response.setRol(usuario.getRol());
        response.setFechaAlta(usuario.getFechaAlta());
        return response;
    }

    private Usuario obtenerEntidadPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", id));
    }

    private void validarEmailDuplicado(String email, Long usuarioIdActual) {
        usuarioRepository.findByEmail(email)
                .filter(usuario -> usuarioIdActual == null || !usuario.getId().equals(usuarioIdActual))
                .ifPresent(usuario -> {
                    throw new DuplicateResourceException("Usuario", "email", usuario.getEmail());
                });
    }
}