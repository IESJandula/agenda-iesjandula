package agenda.init;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import agenda.model.TipoEvento;
import agenda.repository.TipoEventoRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TipoEventoInitializer implements CommandLineRunner {

    private static final String TIPO_OTRO = "Otro";

    private static final List<TipoSemilla> TIPOS_OFICIALES = List.of(
            new TipoSemilla("Festivo", "#EF4444", "calendar", 1),
            new TipoSemilla("Claustro", "#F59E0B", "groups", 2),
            new TipoSemilla("Sesión de evaluación", "#8B5CF6", "task_alt", 2),
            new TipoSemilla("Actividad complementaria", "#10B981", "menu_book", 3),
            new TipoSemilla("Actividad extraescolar", "#06B6D4", "school", 3),
            new TipoSemilla("Erasmus", "#3B82F6", "flight_takeoff", 4),
            new TipoSemilla("Inicio periodo", "#64748B", "rocket_launch", 5),
            new TipoSemilla("Fin periodo", "#475569", "flag", 5),
            new TipoSemilla(TIPO_OTRO, "#9CA3AF", "help_outline", 99));

    private final TipoEventoRepository tipoEventoRepository;

    @Override
    @Transactional
    public void run(String... args) {
        TIPOS_OFICIALES.forEach(this::sincronizarTipoOficial);
    }

    private void sincronizarTipoOficial(TipoSemilla semilla) {
        TipoEvento tipoEvento = tipoEventoRepository.findByNombreIgnoreCase(semilla.nombre())
                .orElseGet(TipoEvento::new);

        boolean necesitaActualizacion = tipoEvento.getId() == null
                || !semilla.nombre().equals(tipoEvento.getNombre())
                || !semilla.color().equalsIgnoreCase(tipoEvento.getColor())
                || !semilla.icono().equalsIgnoreCase(tipoEvento.getIcono())
                || tipoEvento.getPrioridad() != semilla.prioridad()
                || !tipoEvento.isActivo()
                || !tipoEvento.isProtegido();

        if (necesitaActualizacion) {
            tipoEvento.setNombre(semilla.nombre());
            tipoEvento.setColor(semilla.color());
            tipoEvento.setIcono(semilla.icono());
            tipoEvento.setPrioridad(semilla.prioridad());
            tipoEvento.setActivo(true);
            tipoEvento.setProtegido(true);
            tipoEventoRepository.save(tipoEvento);
        }
    }

    private record TipoSemilla(String nombre, String color, String icono, int prioridad) {
    }
}
