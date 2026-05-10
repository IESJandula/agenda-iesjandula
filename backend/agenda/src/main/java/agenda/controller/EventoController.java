package agenda.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import agenda.dto.CrearEventoRequest;
import agenda.dto.EventoResponseDTO;
import agenda.enums.EstadoEvento;
import agenda.model.Evento;
import agenda.service.EventoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/eventos")
@RequiredArgsConstructor
public class EventoController {

    private final EventoService eventoService;

    @PostMapping
    public ResponseEntity<EventoResponseDTO> crearEvento(@Valid @RequestBody CrearEventoRequest request) {
        Evento creado = eventoService.crearEvento(request);
        EventoResponseDTO response = eventoService.convertirAResponse(creado);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<EventoResponseDTO>> obtenerTodos() {
        return ResponseEntity.ok(eventoService.obtenerTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventoResponseDTO> obtenerPorId(@PathVariable Long id) {
        return eventoService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventoResponseDTO> actualizarEvento(@PathVariable Long id,
            @Valid @RequestBody CrearEventoRequest request) {
        EventoResponseDTO actualizado = eventoService.actualizarEvento(id, request);
        if (actualizado == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(actualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarEvento(@PathVariable Long id) {
        boolean eliminado = eventoService.eliminarEvento(id);
        if (!eliminado) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<EventoResponseDTO>> filtrarPorEstado(@PathVariable EstadoEvento estado) {
        return ResponseEntity.ok(eventoService.filtrarPorEstado(estado));
    }

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