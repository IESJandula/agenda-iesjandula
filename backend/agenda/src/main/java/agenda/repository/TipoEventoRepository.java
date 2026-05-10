package agenda.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import agenda.model.TipoEvento;

public interface TipoEventoRepository extends JpaRepository<TipoEvento, Long> {

    boolean existsByNombreIgnoreCase(String nombre);
}