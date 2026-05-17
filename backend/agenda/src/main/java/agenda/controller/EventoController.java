package agenda.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import agenda.dto.ActualizarEventoRequest;
import agenda.dto.CrearEventoRequest;
import agenda.dto.EventoResponseDTO;
import agenda.dto.importacion.ImportarFestivosConfirmarRequestDTO;
import agenda.dto.importacion.ImportarFestivosPreviewResponseDTO;
import agenda.enums.EstadoEvento;
import agenda.model.Evento;
import agenda.service.EventoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controlador REST para gestionar eventos.
 * 
 * Validaciones activas:
 * - @Valid activa automáticamente todas las validaciones del DTO CrearEventoRequest
 * - Los errores de validación se capturan en GlobalExceptionHandler
 * - Respuestas HTTP: 201 Created (éxito), 400 Bad Request (validación fallida)
 */
@RestController
@RequestMapping("/api/eventos")
@RequiredArgsConstructor
public class EventoController {

    private final EventoService eventoService;

    /**
     * Crea un nuevo evento con validaciones automáticas.
     * 
     * @Valid: Activa la validación de Jakarta Validation en CrearEventoRequest
     * 
     * @param request DTO con datos del evento (validado automáticamente)
     * @return 201 Created con el evento creado en caso de éxito
     *         400 Bad Request si falla la validación
     */
    @PostMapping
    public ResponseEntity<EventoResponseDTO> crearEvento(@Valid @RequestBody CrearEventoRequest request) {
        Evento creado = eventoService.crearEvento(request);
        EventoResponseDTO response = eventoService.convertirAResponse(creado);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Crea una propuesta de evento en estado PENDIENTE.
     *
     * Si existe usuario autenticado, el servicio usa ese usuario como creador.
     * Si no se puede resolver, se conserva el creadorId del DTO.
     */
    @PostMapping("/propuestas")
    public ResponseEntity<EventoResponseDTO> crearPropuesta(@Valid @RequestBody CrearEventoRequest request) {
        Evento creado = eventoService.crearPropuestaEvento(request);
        EventoResponseDTO response = eventoService.convertirAResponse(creado);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value = "/importar-festivos/preview", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ImportarFestivosPreviewResponseDTO> previsualizarFestivos(
            @RequestPart("archivo") MultipartFile archivo) {
        return ResponseEntity.ok(eventoService.previsualizarFestivos(archivo));
    }

    @PostMapping("/importar-festivos/confirmar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<EventoResponseDTO>> confirmarImportacionFestivos(
            @Valid @RequestBody ImportarFestivosConfirmarRequestDTO request) {
        return ResponseEntity.ok(eventoService.confirmarImportacionFestivos(request.getEventos()));
    }

    /**
     * Obtiene todos los eventos.
     * 
     * @return 200 OK con lista de eventos
     */
    @GetMapping
    public ResponseEntity<List<EventoResponseDTO>> obtenerTodos() {
        return ResponseEntity.ok(eventoService.obtenerTodos());
    }

    /**
     * Obtiene un evento por su ID.
     * 
     * @param id ID del evento
     * @return 200 OK si existe, 404 Not Found si no existe
     */
    @GetMapping("/{id}")
    public ResponseEntity<EventoResponseDTO> obtenerPorId(@PathVariable Long id) {
        return eventoService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Actualiza un evento con validaciones automáticas.
     * 
     * @Valid: Activa la validación de Jakarta Validation en ActualizarEventoRequest
     * 
     * @param id ID del evento a actualizar
     * @param request DTO con nuevos datos (validado automáticamente)
     * @return 200 OK con evento actualizado en caso de éxito
     *         400 Bad Request si falla la validación
     *         404 Not Found si el evento no existe
     */
    @PutMapping("/{id}")
    public ResponseEntity<EventoResponseDTO> actualizarEvento(@PathVariable Long id,
            @Valid @RequestBody ActualizarEventoRequest request) {
        EventoResponseDTO actualizado = eventoService.actualizarEvento(id, request);
        if (actualizado == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(actualizado);
    }

    /**
     * Elimina un evento.
     * 
     * @param id ID del evento a eliminar
     * @return 204 No Content si se eliminó correctamente
     *         404 Not Found si el evento no existe
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarEvento(@PathVariable Long id) {
        boolean eliminado = eventoService.eliminarEvento(id);
        if (!eliminado) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/aprobar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EventoResponseDTO> aprobarEvento(@PathVariable Long id) {
        return ResponseEntity.ok(eventoService.aprobarEvento(id));
    }

    @PutMapping("/{id}/rechazar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EventoResponseDTO> rechazarEvento(@PathVariable Long id) {
        return ResponseEntity.ok(eventoService.rechazarEvento(id));
    }

    /**
     * Filtra eventos por estado.
     * 
     * @param estado Estado del evento
     * @return 200 OK con eventos filtrados
     */
    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<EventoResponseDTO>> filtrarPorEstado(@PathVariable EstadoEvento estado) {
        return ResponseEntity.ok(eventoService.filtrarPorEstado(estado));
    }

    /**
     * Filtra eventos por múltiples criterios.
     * 
     * @param estado Estado del evento (opcional)
     * @param tipoId ID del tipo de evento (opcional)
     * @param creadorId ID del creador (opcional)
     * @param fechaInicio Fecha inicio (opcional)
     * @param fechaFin Fecha fin (opcional)
     * @return 200 OK con eventos que cumplan los criterios
     */
    @GetMapping("/filtro")
    public ResponseEntity<List<EventoResponseDTO>> filtrarEventos(
            @RequestParam(required = false) EstadoEvento estado,
            @RequestParam(required = false) Long tipoId,
            @RequestParam(required = false) Long creadorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin) {
        return ResponseEntity.ok(eventoService.filtrarEventos(estado, tipoId, creadorId, fechaInicio, fechaFin));
    }
}