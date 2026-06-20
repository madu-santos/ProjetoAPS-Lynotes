-- Criação da tabela de usuários (administradores do painel) caso ela não exista
CREATE TABLE IF NOT EXISTS usuario (
    id VARCHAR(36) PRIMARY KEY,              -- Identificador único (UUID) no formato de texto
    nome VARCHAR(100) NOT NULL,             -- Nome completo do usuário
    email VARCHAR(100) UNIQUE NOT NULL,      -- Email de login (único)
    senha VARCHAR(100) NOT NULL             -- Senha criptografada (hash BCrypt)
);
 
-- Seed de usuário administrador padrão (senha: admin123)
INSERT INTO usuario (id, nome, email, senha)
VALUES ('c4b4d693-e18e-4f51-b844-3d9692482be2', 'Administrador', 'admin@email.com', '$2a$10$uNMxjjZPaBHQm.LCdShe7ujv3tAyy8Fn9px0u6XEUyfFW1/LjWPSa')
ON CONFLICT (email) DO NOTHING;

CREATE TABLE IF NOT EXISTS artista (
    id VARCHAR(36) PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    genero_musical VARCHAR(100),
    biografia TEXT
);

ALTER TABLE artista DROP CONSTRAINT IF EXISTS artista_nome_key;
