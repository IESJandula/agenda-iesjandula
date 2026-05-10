package agenda.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import agenda.dto.CrearEventoRequest;
import agenda.dto.EventoResponseDTO;
import agenda.enums.EstadoEvento;
import agenda.model.Evento;
import agenda.model.TipoEvento;
import agenda.model.Usuario;
import agenda.repository.EventoRepository;
import agenda.repository.TipoEventoRepository;
import agenda.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventoService {

    private final EventoRepository eventoRepository;
    private final TipoEventoRepository tipoEventoRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public Evento crearEvento(CrearEventoRequest request) {
        Evento evento = Evento.builder()
            .tipo(buscarTipoPorId(request.getTipoId()))
            .creador(buscarUsuarioPorId(request.getCreadorId()))
                .titulo(request.getTitulo())
                .descripcion(request.getDescripcion())
                .fechaInicio(request.getFechaInicio())
                .fechaFin(request.getFechaFin())
                .lugar(request.getLugar())
                .gruposAfectados(request.getGruposAfectados())
                .enlaceDocumento(request.getEnlaceDocumento())
                .numAsistentes(request.getNumAsistentes())
                .estado(request.getEstado())
                .build();

        return eventoRepository.save(evento);
    }

    @Transactional
    public EventoResponseDTO actualizarEvento(Long id, CrearEventoRequest request) {
        Evento evento = eventoRepository.findById(id)
                .orElse(null);

        if (evento == null) {
            return null;
        }

        evento.setTipo(buscarTipoPorId(request.getTipoId()));
        evento.setCreador(buscarUsuarioPorId(request.getCreadorId()));
        evento.setTitulo(request.getTitulo());
        evento.setDescripcion(request.getDescripcion());
        evento.setFechaInicio(request.getFechaInicio());
        evento.setFechaFin(request.getFechaFin());
        evento.setLugar(request.getLugar());
        evento.setGruposAfectados(request.getGruposAfectados());
        evento.setEnlaceDocumento(request.getEnlaceDocumento());
        evento.setNumAsistentes(request.getNumAsistentes());
        evento.setEstado(request.getEstado());

        Evento actualizado = eventoRepository.save(evento);
        return convertirAResponse(actualizado);
    }

    @Transactional
    public boolean eliminarEvento(Long id) {
        if (!eventoRepository.existsById(id)) {
            return false;
        }

        eventoRepository.deleteById(id);
        return true;
    }

    @Transactional(readOnly = true)
    public List<EventoResponseDTO> obtenerTodos() {
        return convertirListaAResponse(eventoRepository.findAll());
    }

    @Transactional(readOnly = true)
    public List<EventoResponseDTO> filtrarPorEstado(EstadoEvento estado) {
        return convertirListaAResponse(eventoRepository.findByEstado(estado));
    }

    @Transactional(readOnly = true)
    public List<EventoResponseDTO> filtrarEventos(
            EstadoEvento estado,
            Long tipoId,
            Long creadorId,
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta) {
        return eventoRepository
            .filtrarEventos(
                        estado,
                        tipoId,
                        creadorId,
                        fechaDesde,
                        fechaHasta)
                .stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EventoResponseDTO> filtrarPorEstadoTipoCreadorYRangoFechas(
            EstadoEvento estado,
            Long tipoId,
            Long creadorId,
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta) {
        return filtrarEventos(estado, tipoId, creadorId, fechaDesde, fechaHasta);
    }

    @Transactional(readOnly = true)
    public Optional<EventoResponseDTO> obtenerPorId(Long id) {
        return eventoRepository.findById(id)
                .map(this::convertirAResponse);
    }

    @Transactional(readOnly = true)
    public List<EventoResponseDTO> convertirListaAResponse(List<Evento> eventos) {
        return eventos.stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EventoResponseDTO convertirAResponse(Evento evento) {
        if (evento == null) {
            return null;
        }

        TipoEvento tipo = evento.getTipo();
        Usuario creador = evento.getCreador();

        return EventoResponseDTO.builder()
                .id(evento.getId())
                .tipoId(tipo != null ? tipo.getId() : null)
                .tipoNombre(tipo != null ? tipo.getNombre() : null)
                .tipoColor(tipo != null ? tipo.getColor() : null)
                .titulo(evento.getTitulo())
                .descripcion(evento.getDescripcion())
                .fechaInicio(evento.getFechaInicio())
                .fechaFin(evento.getFechaFin())
                .lugar(evento.getLugar())
                .gruposAfectados(evento.getGruposAfectados())
                .estado(evento.getEstado())
                .creadorId(creador != null ? creador.getId() : null)
                .creadorNombre(creador != null ? creador.getNombre() : null)
                .build();
    }

    private TipoEvento buscarTipoPorId(Long tipoId) {
        return tipoEventoRepository.findById(tipoId)
                .orElseThrow(() -> new IllegalArgumentException("No existe TipoEvento con id: " + tipoId));
    }

    private Usuario buscarUsuarioPorId(Long usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("No existe Usuario con id: " + usuarioId));
    }
}