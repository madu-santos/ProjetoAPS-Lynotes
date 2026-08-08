CREATE TABLE recuperacao_senha (
    email VARCHAR(100) PRIMARY KEY,
    codigo VARCHAR(10) NOT NULL,
    expiracao TIMESTAMP NOT NULL
);
