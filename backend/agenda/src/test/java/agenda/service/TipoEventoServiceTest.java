package agenda.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import agenda.dto.CrearTipoEventoRequest;
import agenda.dto.TipoEventoResponseDTO;
import agenda.exception.DuplicateResourceException;
import agenda.exception.ResourceNotFoundException;
import agenda.model.TipoEvento;
import agenda.repository.TipoEventoRepository;

@ExtendWith(MockitoExtension.class)
class TipoEventoServiceTest {

    @Mock
    private TipoEventoRepository tipoEventoRepository;

    @InjectMocks
    private TipoEventoService tipoEventoService;

    @Test
    void crearTipoEvento_debeGuardarYDevolverDto() {
        CrearTipoEventoRequest request = crearRequest();
        TipoEvento guardado = TipoEvento.builder()
                .id(1L)
                .nombre(request.getNombre())
                .color(request.getColor())
                .icono(request.getIcono())
                .prioridad(request.getPrioridad())
                .activo(request.getActivo())
                .protegido(false)
                .build();

        when(tipoEventoRepository.existsByNombreIgnoreCase(request.getNombre())).thenReturn(false);
        when(tipoEventoRepository.save(any(TipoEvento.class))).thenReturn(guardado);

        TipoEventoResponseDTO response = tipoEventoService.crearTipoEvento(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getNombre()).isEqualTo("Reunión General");
        assertThat(response.getColor()).isEqualTo("#1F8EF1");
        assertThat(response.getIcono()).isEqualTo("meeting_room");
        assertThat(response.getPrioridad()).isEqualTo(2);
        assertThat(response.isActivo()).isTrue();
        assertThat(response.isProtegido()).isFalse();
        verify(tipoEventoRepository).save(any(TipoEvento.class));
    }

    @Test
    void crearTipoEvento_siNombreExiste_lanzaDuplicateResourceException() {
        CrearTipoEventoRequest request = crearRequest();
        when(tipoEventoRepository.existsByNombreIgnoreCase(request.getNombre())).thenReturn(true);

        assertThatThrownBy(() -> tipoEventoService.crearTipoEvento(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("TipoEvento")
                .hasMessageContaining("nombre");

        verify(tipoEventoRepository, never()).save(any());
    }

    @Test
    void obtenerTodos_debeDevolverListaDeDtos() {
        TipoEvento tipoEvento = TipoEvento.builder()
                .id(1L)
                .nombre("Académico")
                .color("#1F8EF1")
                .icono("book")
                .prioridad(1)
                .activo(true)
            .protegido(true)
                .build();

        when(tipoEventoRepository.findAll()).thenReturn(List.of(tipoEvento));

        List<TipoEventoResponseDTO> response = tipoEventoService.obtenerTodos();

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getNombre()).isEqualTo("Académico");
        assertThat(response.get(0).getColor()).isEqualTo("#1F8EF1");
    }

    @Test
    void obtenerPorId_siNoExiste_lanzaResourceNotFoundException() {
        when(tipoEventoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tipoEventoService.obtenerPorId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("TipoEvento")
                .hasMessageContaining("99");
    }

    @Test
    void actualizarTipoEvento_debeModificarYDevolverDto() {
        CrearTipoEventoRequest request = crearRequest();
        TipoEvento existente = TipoEvento.builder()
                .id(1L)
                .nombre("Académico")
                .color("#000000")
                .icono("school")
                .prioridad(1)
                .activo(true)
            .protegido(false)
                .build();

        when(tipoEventoRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(tipoEventoRepository.existsByNombreIgnoreCase(request.getNombre())).thenReturn(false);
        when(tipoEventoRepository.save(any(TipoEvento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TipoEventoResponseDTO response = tipoEventoService.actualizarTipoEvento(1L, request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getNombre()).isEqualTo("Reunión General");
        assertThat(response.getColor()).isEqualTo("#1F8EF1");
        assertThat(response.getIcono()).isEqualTo("meeting_room");
        assertThat(response.getPrioridad()).isEqualTo(2);
        assertThat(response.isActivo()).isTrue();
    }

    @Test
    void eliminarTipoEvento_debeBorrarEntidadExistente() {
        TipoEvento existente = TipoEvento.builder()
                .id(1L)
                .nombre("Académico")
            .protegido(false)
                .build();

        when(tipoEventoRepository.findById(1L)).thenReturn(Optional.of(existente));

        tipoEventoService.eliminarTipoEvento(1L);

        verify(tipoEventoRepository).delete(existente);
    }

    private CrearTipoEventoRequest crearRequest() {
        CrearTipoEventoRequest request = new CrearTipoEventoRequest();
        request.setNombre("Reunión General");
        request.setColor("#1F8EF1");
        request.setIcono("meeting_room");
        request.setPrioridad(2);
        request.setActivo(true);
        return request;
    }
}