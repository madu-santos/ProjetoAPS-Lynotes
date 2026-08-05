CREATE TABLE pedido_seguir (
    seguidor_id VARCHAR(36) NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    seguido_id VARCHAR(36) NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    data_criacao TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (seguidor_id, seguido_id)
);
