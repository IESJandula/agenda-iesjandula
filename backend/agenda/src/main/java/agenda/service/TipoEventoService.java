package agenda.service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import agenda.dto.CrearTipoEventoRequest;
import agenda.dto.TipoEventoResponseDTO;
import agenda.exception.BusinessException;
import agenda.exception.DuplicateResourceException;
import agenda.exception.ResourceNotFoundException;
import agenda.model.TipoEvento;
import agenda.repository.TipoEventoRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TipoEventoService {

    private static final String TIPO_OTRO = "Otro";

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
                .protegido(false)
                .build();

        TipoEvento guardado = tipoEventoRepository.save(tipoEvento);
        return convertirAResponse(guardado);
    }

    @Transactional(readOnly = true)
            public List<TipoEventoResponseDTO> obtenerTiposPublicos() {
            return tipoEventoRepository.findAll()
                .stream()
                .filter(this::esTipoVisiblePublicamente)
                .sorted(Comparator.comparingInt(TipoEvento::getPrioridad)
                    .thenComparing(TipoEvento::getNombre, String.CASE_INSENSITIVE_ORDER))
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
            }

            @Transactional(readOnly = true)
    public List<TipoEventoResponseDTO> obtenerTodos() {
        return tipoEventoRepository.findAll()
                .stream()
                .sorted(Comparator.comparingInt(TipoEvento::getPrioridad)
                    .thenComparing(TipoEvento::getNombre, String.CASE_INSENSITIVE_ORDER))
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

        if (tipoEvento.isProtegido() && !tipoEvento.getNombre().equalsIgnoreCase(request.getNombre())) {
            throw new BusinessException("TIPO_EVENTO_PROTEGIDO",
                    "Los tipos oficiales no se pueden renombrar. Ajusta solo color, icono, prioridad o estado.");
        }

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

        if (tipoEvento.isProtegido()) {
            throw new BusinessException("TIPO_EVENTO_PROTEGIDO",
                    "Los tipos oficiales y la opción Otro no se pueden eliminar");
        }

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
        response.setProtegido(tipoEvento.isProtegido());
        return response;
    }

    @Transactional(readOnly = true)
    public List<TipoEventoResponseDTO> obtenerTiposAdmin() {
        return obtenerTodos();
    }

    private boolean esTipoVisiblePublicamente(TipoEvento tipoEvento) {
        if (tipoEvento == null || !tipoEvento.isActivo()) {
            return false;
        }

        return tipoEvento.isProtegido() || TIPO_OTRO.equalsIgnoreCase(tipoEvento.getNombre());
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