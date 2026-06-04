package agenda.controller;

import java.util.List;

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
import org.springframework.web.bind.annotation.RestController;

import agenda.dto.CrearTipoEventoRequest;
import agenda.dto.TipoEventoResponseDTO;
import agenda.service.TipoEventoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/tipos")
@RequiredArgsConstructor
public class TipoEventoController {

    private final TipoEventoService tipoEventoService;

    @GetMapping
    public ResponseEntity<List<TipoEventoResponseDTO>> listarPublicos() {
        return ResponseEntity.ok(tipoEventoService.obtenerTiposPublicos());
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TipoEventoResponseDTO>> listarAdmin() {
        return ResponseEntity.ok(tipoEventoService.obtenerTiposAdmin());
    }

    @PostMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TipoEventoResponseDTO> crearTipo(
            @Valid @RequestBody CrearTipoEventoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tipoEventoService.crearTipoEvento(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TipoEventoResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(tipoEventoService.obtenerPorId(id));
    }

    @PutMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TipoEventoResponseDTO> actualizarTipo(
            @PathVariable Long id,
            @Valid @RequestBody CrearTipoEventoRequest request) {
        return ResponseEntity.ok(tipoEventoService.actualizarTipoEvento(id, request));
    }

    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminarTipo(@PathVariable Long id) {
        tipoEventoService.eliminarTipoEvento(id);
        return ResponseEntity.noContent().build();
    }
}