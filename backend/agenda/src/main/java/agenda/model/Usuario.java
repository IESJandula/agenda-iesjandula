package agenda.model;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import agenda.enums.Rol;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "usuarios")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String nombre;

    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaAlta;

    @JsonIgnore
    @ToString.Exclude
    @OneToMany(mappedBy = "creador")
    private List<Evento> eventosCreados;

    @JsonIgnore
    @ToString.Exclude
    @OneToMany(mappedBy = "aprobador")
    private List<Evento> eventosAprobados;

    @SuppressWarnings("unused")
    @PrePersist
    private void prePersist() {
        if (this.fechaAlta == null) {
            this.fechaAlta = LocalDateTime.now();
        }
    }
}