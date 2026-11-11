package br.edu.organizacaorecursos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_conflitos_reserva")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConflitoReserva {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private LocalDateTime instanteTentativa;

    public ConflitoReserva(LocalDateTime instanteTentativa) {
        this.instanteTentativa = instanteTentativa;
    }
}
