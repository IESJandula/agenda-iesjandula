package agenda.init;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import agenda.model.TipoEvento;
import agenda.repository.TipoEventoRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TipoEventoInitializer implements CommandLineRunner {

    private final TipoEventoRepository tipoEventoRepository;

    @Override
    @Transactional
    public void run(String... args) {
        crearSiNoExiste("Académico", "#1f8ef1", "book", 1);
        crearSiNoExiste("Reunión", "#2dce89", "meeting_room", 2);
        crearSiNoExiste("Actividad", "#f5365c", "sports", 3);
        crearSiNoExiste("Excursión", "#fb6340", "nature", 4);
    }

    private void crearSiNoExiste(String nombre, String color, String icono, int prioridad) {
        if (!tipoEventoRepository.existsByNombreIgnoreCase(nombre)) {
            TipoEvento tipoEvento = TipoEvento.builder()
                    .nombre(nombre)
                    .color(color)
                    .icono(icono)
                    .prioridad(prioridad)
                    .activo(true)
                    .build();
            tipoEventoRepository.save(tipoEvento);
        }
    }
}
