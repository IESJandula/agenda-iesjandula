package agenda.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import agenda.model.TipoEvento;

public interface TipoEventoRepository extends JpaRepository<TipoEvento, Long> {

    boolean existsByNombreIgnoreCase(String nombre);

    Optional<TipoEvento> findByNombreIgnoreCase(String nombre);
}