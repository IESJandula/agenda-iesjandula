package agenda.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import agenda.dto.ActualizarEventoRequest;
import agenda.dto.CrearEventoRequest;
import agenda.dto.EventoResponseDTO;
import agenda.dto.ResponsableDTO;
import agenda.dto.RechazarEventoRequestDTO;
import agenda.dto.importacion.ImportarFestivoDTO;
import agenda.dto.importacion.ImportarFestivoErrorDTO;
import agenda.dto.importacion.ImportarFestivosConfirmarResponseDTO;
import agenda.dto.importacion.ImportarFestivosPreviewResponseDTO;
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
        return crearEventoConEstado(request, request.getEstado(), false, null);
    }

    @Transactional
    public Evento crearPropuestaEvento(CrearEventoRequest request) {
        return crearEventoConEstado(request, EstadoEvento.PENDIENTE, true, null);
    }

    @Transactional(readOnly = true)
    public ImportarFestivosPreviewResponseDTO previsualizarFestivos(MultipartFile archivo) {
        validarArchivoCsv(archivo);

        try {
            String contenido = new String(archivo.getBytes(), StandardCharsets.UTF_8);
            List<String> lineas = Arrays.asList(contenido.split("\\R"));

            if (lineas.isEmpty()) {
                throw new BusinessException("CSV_VACIO", "El archivo CSV está vacío");
            }

            validarCabecerasFestivos(lineas.get(0));

            List<ImportarFestivoDTO> eventos = new ArrayList<>();
            List<ImportarFestivoErrorDTO> errores = new ArrayList<>();
            List<ImportarFestivoErrorDTO> advertencias = new ArrayList<>();

            for (int indice = 1; indice < lineas.size(); indice++) {
                String linea = lineas.get(indice).trim();
                if (linea.isEmpty()) {
                    continue;
                }

                try {
                    ImportarFestivoDTO festivo = parsearLineaFestivo(linea, indice + 1);
                    if (esFestivoDuplicado(festivo)) {
                        advertencias.add(ImportarFestivoErrorDTO.builder()
                                .linea(indice + 1)
                                .mensaje("Este festivo ya existe y se omitirá al confirmar la importación")
                                .build());
                        continue;
                    }

                    eventos.add(festivo);
                } catch (BusinessException ex) {
                    errores.add(ImportarFestivoErrorDTO.builder()
                            .linea(indice + 1)
                            .mensaje(ex.getMessage())
                            .build());
                }
            }

            return ImportarFestivosPreviewResponseDTO.builder()
                    .eventos(eventos)
                    .errores(errores)
                    .advertencias(advertencias)
                    .validos(eventos.size())
                    .duplicados(advertencias.size())
                    .totalErrores(errores.size())
                    .build();
        } catch (IOException ex) {
            throw new BusinessException("CSV_LECTURA_ERROR", "No se pudo leer el archivo CSV");
        }
    }

    @Transactional
    public ImportarFestivosConfirmarResponseDTO confirmarImportacionFestivos(List<ImportarFestivoDTO> eventosImportados) {
        if (eventosImportados == null) {
            throw new BusinessException("IMPORTACION_VACIA", "No se enviaron festivos para importar");
        }

        TipoEvento tipoFestivo = obtenerTipoFestivo();
        Usuario creador = obtenerUsuarioAutenticado()
                .orElseThrow(() -> new BusinessException("USUARIO_AUTENTICADO_NO_ENCONTRADO",
                        "No se pudo identificar al usuario autenticado para registrar la importación"));

        List<EventoResponseDTO> importados = new ArrayList<>();
        int omitidosDuplicados = 0;
        List<ImportarFestivoErrorDTO> errores = new ArrayList<>();
        Set<String> clavesProcesadas = new HashSet<>();

        for (ImportarFestivoDTO festivo : eventosImportados) {
            Integer linea = festivo != null ? festivo.getLinea() : null;
            try {
                if (festivo == null) {
                    throw new BusinessException("FESTIVO_INVALIDO", "Se recibió un festivo inválido");
                }

                validarImportacionFestivo(festivo);

                String clave = claveFestivo(festivo);
                if (esFestivoDuplicado(festivo) || clavesProcesadas.contains(clave)) {
                    omitidosDuplicados++;
                } else {
                    clavesProcesadas.add(clave);
                    String tituloOriginal = festivo.getTitulo();
                    String tituloNormalizado = tituloOriginal == null ? "" : tituloOriginal.trim();

                    Evento evento = Evento.builder()
                            .tipo(tipoFestivo)
                            .creador(creador)
                            .titulo(tituloNormalizado)
                            .descripcion(festivo.getDescripcion())
                            .fechaInicio(festivo.getFechaInicio().atStartOfDay())
                            .fechaFin(festivo.getFechaFin().atTime(LocalTime.of(23, 59, 59)))
                            .lugar("Festivo")
                            .gruposAfectados("General")
                            .enlaceDocumento(null)
                            .numAsistentes(null)
                            .estado(EstadoEvento.CONFIRMADO)
                            .build();

                    Evento guardado = eventoRepository.save(evento);
                    importados.add(convertirAResponse(guardado));
                }
            } catch (BusinessException ex) {
                errores.add(ImportarFestivoErrorDTO.builder()
                        .linea(linea)
                        .mensaje(ex.getMessage())
                        .build());
            }
        }

        return ImportarFestivosConfirmarResponseDTO.builder()
                .eventos(importados)
                .importados(importados.size())
                .omitidosDuplicados(omitidosDuplicados)
                .omitidosPorDuplicado(omitidosDuplicados)
                .errores(errores)
                .build();
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
        if (request.getResponsablesIds() != null) {
            evento.setResponsables(validarYObtenerResponsables(request.getResponsablesIds()));
        }
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

    @Transactional
    public EventoResponseDTO aprobarEvento(Long id) {
        Evento evento = eventoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Evento", "id", id));

        evento.setEstado(EstadoEvento.CONFIRMADO);
        evento.setFechaAprobacion(LocalDateTime.now());
        evento.setAprobador(obtenerUsuarioAutenticado().orElse(evento.getAprobador()));

        return convertirAResponse(eventoRepository.save(evento));
    }

    @Transactional
    public EventoResponseDTO rechazarEvento(Long id, RechazarEventoRequestDTO request) {
        Evento evento = eventoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Evento", "id", id));

        evento.setEstado(EstadoEvento.RECHAZADO);
        evento.setMotivoRechazo(request.getMotivo().trim());
        evento.setFechaAprobacion(LocalDateTime.now());
        evento.setAprobador(obtenerUsuarioAutenticado().orElse(evento.getAprobador()));

        return convertirAResponse(eventoRepository.save(evento));
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
            .tipoPrioridad(tipo != null ? tipo.getPrioridad() : null)
                .titulo(evento.getTitulo())
                .descripcion(evento.getDescripcion())
                .fechaInicio(evento.getFechaInicio())
                .fechaFin(evento.getFechaFin())
                .lugar(evento.getLugar())
                .gruposAfectados(evento.getGruposAfectados())
                .estado(evento.getEstado())
                .creadorId(creador != null ? creador.getId() : null)
                .creadorNombre(creador != null ? creador.getNombre() : null)
                .motivoRechazo(evento.getMotivoRechazo())
                .responsables(convertirResponsablesAResponse(evento.getResponsables()))
                .build();
    }

    private List<ResponsableDTO> convertirResponsablesAResponse(List<Usuario> responsables) {
        if (responsables == null) {
            return new ArrayList<>();
        }

        return responsables.stream()
                .filter(Objects::nonNull)
                .map(usuario -> ResponsableDTO.builder()
                        .id(usuario.getId())
                        .nombre(usuario.getNombre())
                        .email(usuario.getEmail())
                        .build())
                .collect(Collectors.toList());
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

    private List<Usuario> validarYObtenerResponsables(List<Long> responsablesIds) {
        if (responsablesIds == null) {
            return new ArrayList<>();
        }

        if (responsablesIds.stream().anyMatch(Objects::isNull)) {
            throw new BusinessException("RESPONSABLES_INVALIDOS",
                    "La lista de responsables contiene identificadores nulos");
        }

        List<Long> idsUnicos = responsablesIds.stream().distinct().collect(Collectors.toList());
        List<Usuario> usuarios = usuarioRepository.findAllById(idsUnicos);
        Map<Long, Usuario> usuariosPorId = usuarios.stream()
                .collect(Collectors.toMap(Usuario::getId, usuario -> usuario));

        List<Long> idsNoEncontrados = idsUnicos.stream()
                .filter(id -> !usuariosPorId.containsKey(id))
                .collect(Collectors.toList());

        if (!idsNoEncontrados.isEmpty()) {
            String idsTexto = idsNoEncontrados.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));
            throw new BusinessException("RESPONSABLES_NO_ENCONTRADOS",
                    "No existen usuarios con los ids: " + idsTexto);
        }

        return idsUnicos.stream()
                .map(usuariosPorId::get)
                .collect(Collectors.toList());
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

    private Evento crearEventoConEstado(CrearEventoRequest request, EstadoEvento estado, boolean preferirUsuarioAutenticado, Long creadorIdFallback) {
        log.info("Creando evento: titulo={}, tipo={}, estado={}", request.getTitulo(), request.getTipoId(), estado);

        validarTitulo(request.getTitulo());

        TipoEvento tipo = validarYObtenerTipoEvento(request.getTipoId());
        Usuario creador = obtenerCreadorEvento(request, preferirUsuarioAutenticado, creadorIdFallback);
        validarRangoFechas(request.getFechaInicio(), request.getFechaFin());

        Evento evento = Evento.builder()
            .tipo(tipo)
            .creador(creador)
            .responsables(validarYObtenerResponsables(request.getResponsablesIds()))
            .titulo(request.getTitulo())
            .descripcion(request.getDescripcion())
            .fechaInicio(request.getFechaInicio())
            .fechaFin(request.getFechaFin())
            .lugar(request.getLugar())
            .gruposAfectados(request.getGruposAfectados())
            .enlaceDocumento(request.getEnlaceDocumento())
            .numAsistentes(request.getNumAsistentes())
            .estado(estado)
            .build();

        Evento eventoPersistido = eventoRepository.save(evento);
        log.info("Evento creado exitosamente: id={}, titulo={}", eventoPersistido.getId(), eventoPersistido.getTitulo());

        return eventoPersistido;
    }

    private Usuario obtenerCreadorEvento(CrearEventoRequest request, boolean preferirUsuarioAutenticado, Long creadorIdFallback) {
        if (preferirUsuarioAutenticado) {
            Optional<Usuario> usuarioAutenticado = obtenerUsuarioAutenticado();
            if (usuarioAutenticado.isPresent()) {
                return usuarioAutenticado.get();
            }
        }

        Long creadorId = request.getCreadorId() != null ? request.getCreadorId() : creadorIdFallback;
        if (creadorId == null) {
            throw new BusinessException("CREADOR_OBLIGATORIO", "No se pudo determinar el creador del evento");
        }

        return validarYObtenerUsuario(creadorId);
    }

    private Optional<Usuario> obtenerUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserDetails userDetails)) {
            return Optional.empty();
        }

        return usuarioRepository.findByEmail(userDetails.getUsername());
    }

    private void validarArchivoCsv(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new BusinessException("CSV_VACIO", "Debes subir un archivo CSV válido");
        }
    }

    private void validarCabecerasFestivos(String cabecera) {
        String[] columnas = parsearCsvLine(cabecera, 1);
        String[] esperadas = { "fecha_inicio", "fecha_fin", "titulo", "descripcion" };

        if (columnas.length != esperadas.length) {
            throw new BusinessException("CSV_COLUMNAS_INVALIDAS",
                    "El CSV debe contener exactamente las columnas: fecha_inicio,fecha_fin,titulo,descripcion");
        }

        for (int indice = 0; indice < esperadas.length; indice++) {
            if (!esperadas[indice].equalsIgnoreCase(columnas[indice].trim())) {
                throw new BusinessException("CSV_COLUMNAS_INVALIDAS",
                        "El CSV debe contener exactamente las columnas: fecha_inicio,fecha_fin,titulo,descripcion");
            }
        }
    }

    private ImportarFestivoDTO parsearLineaFestivo(String linea, int numeroLinea) {
        String[] columnas = parsearCsvLine(linea, numeroLinea);

        if (columnas.length != 4) {
            throw new BusinessException("CSV_FILA_INVALIDA",
                    String.format("La línea %d debe tener 4 columnas separadas por comas", numeroLinea));
        }

        LocalDate fechaInicio = parsearFecha(columnas[0].trim(), numeroLinea, "fecha_inicio");
        LocalDate fechaFin = parsearFecha(columnas[1].trim(), numeroLinea, "fecha_fin");
        String titulo = columnas[2].trim();
        String descripcion = columnas[3].trim();

        if (titulo.isEmpty()) {
            throw new BusinessException("CSV_TITULO_OBLIGATORIO",
                    String.format("La línea %d no contiene un título válido", numeroLinea));
        }

        if (fechaFin.isBefore(fechaInicio)) {
            throw new BusinessException("CSV_FECHAS_INVALIDAS",
                    String.format("La línea %d tiene una fecha_fin anterior a fecha_inicio", numeroLinea));
        }

        return ImportarFestivoDTO.builder()
                .linea(numeroLinea)
                .fechaInicio(fechaInicio)
                .fechaFin(fechaFin)
                .titulo(titulo)
                .descripcion(descripcion.isEmpty() ? null : descripcion)
                .build();
    }

    private String[] parsearCsvLine(String linea, int numeroLinea) {
        if (linea == null) {
            throw new BusinessException("CSV_FILA_INVALIDA",
                    String.format("La línea %d está vacía o es inválida", numeroLinea));
        }

        return linea.split(",", -1);
    }

    private LocalDate parsearFecha(String valor, int numeroLinea, String campo) {
        try {
            return LocalDate.parse(valor);
        } catch (Exception ex) {
            throw new BusinessException("CSV_FECHA_INVALIDA",
                    String.format("La línea %d tiene una fecha inválida en el campo %s", numeroLinea, campo));
        }
    }

    private TipoEvento obtenerTipoFestivo() {
        TipoEvento tipoFestivo = tipoEventoRepository.findByNombreIgnoreCase("Festivo")
                .orElseGet(() -> TipoEvento.builder()
                        .nombre("Festivo")
                        .color("#EF4444")
                        .icono("calendar")
                        .prioridad(1)
                        .activo(true)
                        .build());

        boolean necesitaNormalizacion = tipoFestivo.getId() == null
                || !"Festivo".equals(tipoFestivo.getNombre())
                || !"#EF4444".equalsIgnoreCase(tipoFestivo.getColor())
                || !"calendar".equalsIgnoreCase(tipoFestivo.getIcono())
                || tipoFestivo.getPrioridad() != 1
                || !tipoFestivo.isActivo();

        if (necesitaNormalizacion) {
            tipoFestivo.setNombre("Festivo");
            tipoFestivo.setColor("#EF4444");
            tipoFestivo.setIcono("calendar");
            tipoFestivo.setPrioridad(1);
            tipoFestivo.setActivo(true);
            tipoFestivo = tipoEventoRepository.save(tipoFestivo);
        }

        return tipoFestivo;
    }

    private boolean esFestivoDuplicado(ImportarFestivoDTO festivo) {
        return eventoRepository.existsFestivoDuplicado(
                festivo.getTitulo().trim(),
                festivo.getFechaInicio().atStartOfDay(),
                festivo.getFechaFin().atTime(LocalTime.of(23, 59, 59)),
                "Festivo");
    }

    private String claveFestivo(ImportarFestivoDTO festivo) {
        return String.join("|",
                festivo.getTitulo().trim().toLowerCase(),
                festivo.getFechaInicio().toString(),
                festivo.getFechaFin().toString());
    }

    private void validarImportacionFestivo(ImportarFestivoDTO festivo) {
        if (festivo == null) {
            throw new BusinessException("FESTIVO_INVALIDO", "Se recibió un festivo inválido");
        }

        if (festivo.getFechaInicio() == null || festivo.getFechaFin() == null) {
            throw new BusinessException("FESTIVO_INVALIDO", "Las fechas del festivo son obligatorias");
        }

        if (festivo.getTitulo() == null || festivo.getTitulo().trim().isEmpty()) {
            throw new BusinessException("FESTIVO_INVALIDO", "El título del festivo es obligatorio");
        }
    }

    /**
     * ==================== MÉTODOS DE CONSULTA ====================
     */
}