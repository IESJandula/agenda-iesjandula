package agenda.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import agenda.dto.CrearUsuarioRequest;
import agenda.dto.UsuarioResponseDTO;
import agenda.enums.Rol;
import agenda.exception.DuplicateResourceException;
import agenda.exception.ResourceNotFoundException;
import agenda.model.Usuario;
import agenda.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    void crearUsuario_debeGuardarYDevolverDto() {
        CrearUsuarioRequest request = crearRequest();
        Usuario guardado = Usuario.builder()
                .id(1L)
                .email(request.getEmail())
                .nombre(request.getNombre())
            .password("encoded-password")
                .rol(request.getRol())
                .fechaAlta(LocalDateTime.of(2026, 5, 14, 10, 30))
                .build();

        when(usuarioRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded-password");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(guardado);

        UsuarioResponseDTO response = usuarioService.crearUsuario(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("alumno@iesjandula.es");
        assertThat(response.getNombre()).isEqualTo("Alumno Demo");
        assertThat(response.getRol()).isEqualTo(Rol.PROFESORADO);
        assertThat(response.getFechaAlta()).isEqualTo(LocalDateTime.of(2026, 5, 14, 10, 30));
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    void crearUsuario_siEmailExiste_lanzaDuplicateResourceException() {
        CrearUsuarioRequest request = crearRequest();
        when(usuarioRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.of(Usuario.builder().id(1L).email(request.getEmail()).build()));

        assertThatThrownBy(() -> usuarioService.crearUsuario(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Usuario")
                .hasMessageContaining("email");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void obtenerTodos_debeDevolverListaDeDtos() {
        Usuario usuario = Usuario.builder()
                .id(1L)
                .email("alumno@iesjandula.es")
                .nombre("Alumno Demo")
            .password("encoded-password")
                .rol(Rol.PROFESORADO)
                .fechaAlta(LocalDateTime.of(2026, 5, 14, 10, 30))
                .build();

        when(usuarioRepository.findAll()).thenReturn(List.of(usuario));

        List<UsuarioResponseDTO> response = usuarioService.obtenerTodos();

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getEmail()).isEqualTo("alumno@iesjandula.es");
    }

    @Test
    void obtenerPorId_siNoExiste_lanzaResourceNotFoundException() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.obtenerPorId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Usuario")
                .hasMessageContaining("99");
    }

    @Test
    void actualizarUsuario_debeModificarYDevolverDto() {
        agenda.dto.ActualizarUsuarioRequest request = new agenda.dto.ActualizarUsuarioRequest();
        request.setEmail("alumno@iesjandula.es");
        request.setNombre("Alumno Demo");
        request.setRol(Rol.PROFESORADO);
        Usuario existente = Usuario.builder()
                .id(1L)
                .email("viejo@iesjandula.es")
                .nombre("Antiguo")
            .password("encoded-old-password")
                .rol(Rol.ADMIN)
                .fechaAlta(LocalDateTime.of(2026, 5, 14, 10, 30))
                .build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(usuarioRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UsuarioResponseDTO response = usuarioService.actualizarUsuario(1L, request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("alumno@iesjandula.es");
        assertThat(response.getNombre()).isEqualTo("Alumno Demo");
        assertThat(response.getRol()).isEqualTo(Rol.PROFESORADO);
    }

    @Test
    void eliminarUsuario_debeBorrarEntidadExistente() {
        Usuario existente = Usuario.builder()
                .id(1L)
                .email("alumno@iesjandula.es")
            .password("encoded-password")
                .build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));

        usuarioService.eliminarUsuario(1L);

        verify(usuarioRepository).delete(existente);
    }

    private CrearUsuarioRequest crearRequest() {
        CrearUsuarioRequest request = new CrearUsuarioRequest();
        request.setEmail("alumno@iesjandula.es");
        request.setNombre("Alumno Demo");
        request.setPassword("Password123");
        request.setRol(Rol.PROFESORADO);
        return request;
    }
}