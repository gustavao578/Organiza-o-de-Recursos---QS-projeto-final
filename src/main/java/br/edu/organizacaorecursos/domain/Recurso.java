package br.edu.organizacaorecursos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_recursos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Recurso {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    @NotBlank(message = "O nome do recurso é obrigatório.")
    private String nome;

    @Column(nullable = false)
    @NotBlank(message = "O tipo do recurso é obrigatório.")
    private String tipo;

    @Column(nullable = false)
    private boolean restrito;

    private UUID responsavelId;

    public Recurso(UUID id, String nome, String tipo) {
        this(id, nome, tipo, false, null);
    }
}
