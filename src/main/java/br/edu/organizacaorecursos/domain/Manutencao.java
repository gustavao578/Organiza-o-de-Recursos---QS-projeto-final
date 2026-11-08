package br.edu.organizacaorecursos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_manutencoes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Manutencao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "recurso_id", nullable = false)
    @NotNull(message = "O recurso é obrigatório.")
    private Recurso recurso;

    @Column(nullable = false)
    @NotNull(message = "A data de início é obrigatória.")
    private LocalDateTime dataInicio;

    @Column(nullable = false)
    @NotNull(message = "A data de fim é obrigatória.")
    private LocalDateTime dataFim;

    @Column(nullable = false)
    @NotBlank(message = "A descrição é obrigatória.")
    private String descricao;
}
