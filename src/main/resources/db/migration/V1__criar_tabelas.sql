CREATE TABLE IF NOT EXISTS tb_usuarios (
    id UUID PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    matricula VARCHAR(255) NOT NULL UNIQUE,
    perfil VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS tb_recursos (
    id UUID PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    tipo VARCHAR(255) NOT NULL,
    restrito BOOLEAN NOT NULL,
    responsavel_id UUID
);

CREATE TABLE IF NOT EXISTS tb_reservas (
    id UUID PRIMARY KEY,
    recurso_id UUID NOT NULL REFERENCES tb_recursos (id),
    data_inicio TIMESTAMP NOT NULL,
    data_fim TIMESTAMP NOT NULL,
    status VARCHAR(255) NOT NULL,
    usuario_id UUID NOT NULL REFERENCES tb_usuarios (id)
);

CREATE TABLE IF NOT EXISTS tb_manutencoes (
    id UUID PRIMARY KEY,
    recurso_id UUID NOT NULL REFERENCES tb_recursos (id),
    data_inicio TIMESTAMP NOT NULL,
    data_fim TIMESTAMP NOT NULL,
    descricao VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS tb_reserva_auditorias (
    id UUID PRIMARY KEY,
    reserva_id UUID NOT NULL REFERENCES tb_reservas (id),
    usuario_id UUID NOT NULL,
    instante TIMESTAMP WITH TIME ZONE NOT NULL,
    estado_anterior VARCHAR(255) NOT NULL,
    estado_novo VARCHAR(255) NOT NULL,
    motivo VARCHAR(1000) NOT NULL
);

CREATE TABLE IF NOT EXISTS tb_conflitos_reserva (
    id UUID PRIMARY KEY,
    instante_tentativa TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_tb_reservas_recurso_periodo
    ON tb_reservas (recurso_id, data_inicio, data_fim);
CREATE INDEX IF NOT EXISTS idx_tb_reservas_usuario
    ON tb_reservas (usuario_id);
CREATE INDEX IF NOT EXISTS idx_tb_reservas_status
    ON tb_reservas (status);
CREATE INDEX IF NOT EXISTS idx_tb_manutencoes_recurso_periodo
    ON tb_manutencoes (recurso_id, data_inicio, data_fim);
CREATE INDEX IF NOT EXISTS idx_tb_reserva_auditorias_reserva_instante
    ON tb_reserva_auditorias (reserva_id, instante, id);
CREATE INDEX IF NOT EXISTS idx_tb_conflitos_reserva_instante
    ON tb_conflitos_reserva (instante_tentativa);
