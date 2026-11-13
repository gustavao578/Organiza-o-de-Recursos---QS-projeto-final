CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE tb_reservas
    ADD CONSTRAINT ex_tb_reservas_sem_sobreposicao_ativa
    EXCLUDE USING gist (
        recurso_id WITH =,
        tsrange(data_inicio, data_fim, '[)') WITH &&
    )
    WHERE (status IN ('PENDENTE', 'APROVADA', 'EM_USO'));
