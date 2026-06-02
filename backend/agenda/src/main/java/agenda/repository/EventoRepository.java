package agenda.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import agenda.enums.EstadoEvento;
import agenda.model.Evento;

public interface EventoRepository extends JpaRepository<Evento, Long> {

    List<Evento> findByEstado(EstadoEvento estado);

    @Query("""
        SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END
        FROM Evento e
        WHERE LOWER(TRIM(e.titulo)) = LOWER(TRIM(:titulo))
          AND e.fechaInicio = :fechaInicio
          AND e.fechaFin = :fechaFin
          AND LOWER(TRIM(e.tipo.nombre)) = LOWER(TRIM(:tipoNombre))
        """)
    boolean existsFestivoDuplicado(
            @Param("titulo") String titulo,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin,
            @Param("tipoNombre") String tipoNombre);

        @Query("""
            SELECT e
            FROM Evento e
            WHERE (:estado IS NULL OR e.estado = :estado)
              AND (:tipoId IS NULL OR e.tipo.id = :tipoId)
              AND (:creadorId IS NULL OR e.creador.id = :creadorId)
              AND (:fechaInicio IS NULL OR e.fechaInicio >= :fechaInicio)
              AND (:fechaFin IS NULL OR e.fechaInicio <= :fechaFin)
            """)
        List<Evento> filtrarEventos(
            @Param("estado") EstadoEvento estado,
            @Param("tipoId") Long tipoId,
            @Param("creadorId") Long creadorId,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin);
}