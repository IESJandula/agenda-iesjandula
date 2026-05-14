package agenda.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import agenda.dto.CrearTipoEventoRequest;
import agenda.dto.TipoEventoResponseDTO;
import agenda.exception.DuplicateResourceException;
import agenda.exception.ResourceNotFoundException;
import agenda.model.TipoEvento;
import agenda.repository.TipoEventoRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TipoEventoService {

    private final TipoEventoRepository tipoEventoRepository;

    @Transactional
    public TipoEventoResponseDTO crearTipoEvento(CrearTipoEventoRequest request) {
        validarNombreDuplicado(request.getNombre());

        TipoEvento tipoEvento = TipoEvento.builder()
                .nombre(request.getNombre())
                .color(request.getColor())
                .icono(request.getIcono())
                .prioridad(request.getPrioridad())
                .activo(request.getActivo())
                .build();

        TipoEvento guardado = tipoEventoRepository.save(tipoEvento);
        return convertirAResponse(guardado);
    }

    @Transactional(readOnly = true)
    public List<TipoEventoResponseDTO> obtenerTodos() {
        return tipoEventoRepository.findAll()
                .stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TipoEventoResponseDTO obtenerPorId(Long id) {
        return convertirAResponse(obtenerEntidadPorId(id));
    }

    @Transactional
    public TipoEventoResponseDTO actualizarTipoEvento(Long id, CrearTipoEventoRequest request) {
        TipoEvento tipoEvento = obtenerEntidadPorId(id);

        validarNombreDuplicadoParaActualizacion(request.getNombre(), tipoEvento.getNombre());

        tipoEvento.setNombre(request.getNombre());
        tipoEvento.setColor(request.getColor());
        tipoEvento.setIcono(request.getIcono());
        tipoEvento.setPrioridad(request.getPrioridad());
        tipoEvento.setActivo(request.getActivo());

        TipoEvento actualizado = tipoEventoRepository.save(tipoEvento);
        return convertirAResponse(actualizado);
    }

    @Transactional
    public void eliminarTipoEvento(Long id) {
        TipoEvento tipoEvento = obtenerEntidadPorId(id);
        tipoEventoRepository.delete(tipoEvento);
    }

    @Transactional(readOnly = true)
    public TipoEventoResponseDTO convertirAResponse(TipoEvento tipoEvento) {
        if (tipoEvento == null) {
            return null;
        }

        TipoEventoResponseDTO response = new TipoEventoResponseDTO();
        response.setId(tipoEvento.getId());
        response.setNombre(tipoEvento.getNombre());
        response.setColor(tipoEvento.getColor());
        response.setIcono(tipoEvento.getIcono());
        response.setPrioridad(tipoEvento.getPrioridad());
        response.setActivo(tipoEvento.isActivo());
        return response;
    }

    private TipoEvento obtenerEntidadPorId(Long id) {
        return tipoEventoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TipoEvento", "id", id));
    }

    private void validarNombreDuplicado(String nombre) {
        if (tipoEventoRepository.existsByNombreIgnoreCase(nombre)) {
            throw new DuplicateResourceException("TipoEvento", "nombre", nombre);
        }
    }

    private void validarNombreDuplicadoParaActualizacion(String nombreNuevo, String nombreActual) {
        if (!nombreNuevo.equalsIgnoreCase(nombreActual) && tipoEventoRepository.existsByNombreIgnoreCase(nombreNuevo)) {
            throw new DuplicateResourceException("TipoEvento", "nombre", nombreNuevo);
        }
    }
}