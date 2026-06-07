-- Criação da tabela de usuários (administradores do painel) caso ela não exista
CREATE TABLE IF NOT EXISTS usuario (
    id VARCHAR(36) PRIMARY KEY,              -- Identificador único (UUID) no formato de texto
    nome VARCHAR(100) NOT NULL,             -- Nome completo do usuário
    email VARCHAR(100) UNIQUE NOT NULL,      -- Email de login (único)
    senha VARCHAR(100) NOT NULL             -- Senha criptografada (hash BCrypt)
);
