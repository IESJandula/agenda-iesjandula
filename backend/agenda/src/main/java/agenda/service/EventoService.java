package agenda.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import agenda.dto.ActualizarEventoRequest;
import agenda.dto.CrearEventoRequest;
import agenda.dto.EventoResponseDTO;
import agenda.enums.EstadoEvento;
import agenda.exception.BusinessException;
import agenda.exception.ResourceNotFoundException;
import agenda.model.Evento;
import agenda.model.TipoEvento;
import agenda.model.Usuario;
import agenda.repository.EventoRepository;
import agenda.repository.TipoEventoRepository;
import agenda.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Servicio de eventos con validaciones de negocio profesionales.
 * 
 * Responsabilidades:
 * - Crear, actualizar y eliminar eventos
 * - Aplicar reglas de negocio
 * - Validar integridad referencial
 * - Convertir DTOs
 * 
 * Validaciones de negocio:
 * 1. TipoEvento debe existir y estar activo
 * 2. Usuario creador debe existir
 * 3. fechaFin > fechaInicio (si existe fechaFin)
 * 4. Título no vacío y válido
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventoService {

    private final EventoRepository eventoRepository;
    private final TipoEventoRepository tipoEventoRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * Crea un evento con validaciones de negocio.
     * 
     * Validaciones:
     * - Tipo de evento existe y está activo
     * - Usuario creador existe
     * - Rango de fechas válido
     * - Título válido
     * 
     * @param request Datos del evento a crear
     * @return Evento persistido
     * @throws ResourceNotFoundException Si TipoEvento o Usuario no existe
     * @throws BusinessException Si hay violación de reglas de negocio
     */
    @Transactional
    public Evento crearEvento(CrearEventoRequest request) {
        log.info("Creando evento: titulo={}, tipo={}, creador={}", 
            request.getTitulo(), request.getTipoId(), request.getCreadorId());
        
        // Validar que título no sea vacío/nulo
        validarTitulo(request.getTitulo());
        
        // Validar y obtener TipoEvento
        TipoEvento tipo = validarYObtenerTipoEvento(request.getTipoId());
        
        // Validar y obtener Usuario creador
        Usuario creador = validarYObtenerUsuario(request.getCreadorId());
        
        // Validar rango de fechas
        validarRangoFechas(request.getFechaInicio(), request.getFechaFin());
        
        log.debug("Validaciones exitosas para evento: {}", request.getTitulo());
        
        Evento evento = Evento.builder()
            .tipo(tipo)
            .creador(creador)
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

        Evento eventoPersistido = eventoRepository.save(evento);
        log.info("Evento creado exitosamente: id={}, titulo={}", 
            eventoPersistido.getId(), eventoPersistido.getTitulo());
        
        return eventoPersistido;
    }

    /**
     * Actualiza un evento con validaciones de negocio.
     * 
     * @param id ID del evento a actualizar
     * @param request Nuevos datos del evento
     * @return EventoResponseDTO actualizado
     * @throws ResourceNotFoundException Si el evento no existe
     * @throws BusinessException Si hay violación de reglas de negocio
     */
    @Transactional
    public EventoResponseDTO actualizarEvento(Long id, ActualizarEventoRequest request) {
        log.info("Actualizando evento: id={}, titulo={}", id, request.getTitulo());
        
        // Validar que evento existe
        Evento evento = eventoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Evento", "id", id));
        
        // Validar que título no sea vacío/nulo
        validarTitulo(request.getTitulo());
        
        // Validar y obtener TipoEvento
        TipoEvento tipo = validarYObtenerTipoEvento(request.getTipoId());
        
        // Validar y obtener Usuario creador
        Usuario creador = validarYObtenerUsuario(request.getCreadorId());
        
        // Validar rango de fechas
        validarRangoFechas(request.getFechaInicio(), request.getFechaFin());
        
        log.debug("Validaciones exitosas para actualizar evento: id={}", id);
        
        // Actualizar campos
        evento.setTipo(tipo);
        evento.setCreador(creador);
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
        log.info("Evento actualizado exitosamente: id={}", id);
        
        return convertirAResponse(actualizado);
    }

    /**
     * Elimina un evento.
     * 
     * @param id ID del evento a eliminar
     * @return true si se eliminó, false si no existe
     */
    @Transactional
    public boolean eliminarEvento(Long id) {
        log.info("Eliminando evento: id={}", id);
        
        if (!eventoRepository.existsById(id)) {
            log.warn("No se puede eliminar: evento no existe con id={}", id);
            return false;
        }

        eventoRepository.deleteById(id);
        log.info("Evento eliminado exitosamente: id={}", id);
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

    /**
     * ==================== VALIDACIONES DE NEGOCIO ====================
     */
    
    /**
     * Valida que el título no sea nulo ni vacío.
     * 
     * @param titulo Título a validar
     * @throws BusinessException Si el título es inválido
     */
    private void validarTitulo(String titulo) {
        if (titulo == null || titulo.trim().isEmpty()) {
            log.warn("Validación fallida: título vacío o nulo");
            throw new BusinessException("TITULO_VACIO", 
                "El título del evento es obligatorio y no puede estar vacío");
        }
        
        if (titulo.trim().length() < 3) {
            log.warn("Validación fallida: título muy corto: {}", titulo);
            throw new BusinessException("TITULO_CORTO", 
                "El título debe tener al menos 3 caracteres");
        }
        
        log.debug("Validación exitosa: título válido");
    }
    
    /**
     * Valida y obtiene un TipoEvento.
     * Verifica que exista y esté activo.
     * 
     * @param tipoId ID del tipo de evento
     * @return TipoEvento validado
     * @throws ResourceNotFoundException Si el tipo no existe
     * @throws BusinessException Si el tipo está inactivo
     */
    private TipoEvento validarYObtenerTipoEvento(Long tipoId) {
        TipoEvento tipo = tipoEventoRepository.findById(tipoId)
            .orElseThrow(() -> {
                log.warn("Validación fallida: TipoEvento no existe con id={}", tipoId);
                return new ResourceNotFoundException("TipoEvento", "id", tipoId);
            });
        
        if (!tipo.isActivo()) {
            log.warn("Validación fallida: TipoEvento inactivo: id={}, nombre={}", 
                tipo.getId(), tipo.getNombre());
            throw new BusinessException("TIPO_EVENTO_INACTIVO", 
                String.format("No se puede usar el tipo de evento '%s' porque está inactivo. " +
                    "Contacte al administrador para activarlo.", tipo.getNombre()));
        }
        
        log.debug("Validación exitosa: TipoEvento válido y activo: {}", tipo.getNombre());
        return tipo;
    }
    
    /**
     * Valida y obtiene un Usuario.
     * Verifica que el usuario existe para poder crear eventos.
     * 
     * @param usuarioId ID del usuario
     * @return Usuario validado
     * @throws ResourceNotFoundException Si el usuario no existe
     */
    private Usuario validarYObtenerUsuario(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> {
                log.warn("Validación fallida: Usuario no existe con id={}", usuarioId);
                return new ResourceNotFoundException("Usuario", "id", usuarioId);
            });
        
        log.debug("Validación exitosa: Usuario válido: {}", usuario.getNombre());
        return usuario;
    }
    
    /**
     * Valida el rango de fechas del evento.
     * Reglas:
     * - fechaInicio debe ser posterior a ahora (validado por @FutureOrPresent)
     * - Si fechaFin existe, debe ser posterior a fechaInicio
     * 
     * @param fechaInicio Fecha de inicio
     * @param fechaFin Fecha de fin (puede ser nula)
     * @throws BusinessException Si las fechas son inválidas
     */
    private void validarRangoFechas(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        // Si no hay fechaFin, es válido (evento sin hora de fin)
        if (fechaFin == null) {
            log.debug("Validación exitosa: evento sin fecha de fin");
            return;
        }
        
        // Validar que fechaFin > fechaInicio
        if (fechaFin.isBefore(fechaInicio) || fechaFin.isEqual(fechaInicio)) {
            log.warn("Validación fallida: fechaFin ({}) no es posterior a fechaInicio ({})", 
                fechaFin, fechaInicio);
            throw new BusinessException("RANGO_FECHAS_INVALIDO", 
                "La fecha de fin del evento debe ser posterior a la fecha de inicio");
        }
        
        log.debug("Validación exitosa: rango de fechas válido");
    }

    /**
     * ==================== MÉTODOS DE CONSULTA ====================
     */
}