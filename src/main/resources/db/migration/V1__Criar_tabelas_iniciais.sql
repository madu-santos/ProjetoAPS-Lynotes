-- V1__Criar_tabelas_iniciais.sql

CREATE TABLE IF NOT EXISTS usuario (
    id VARCHAR(36) PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    senha VARCHAR(100) NOT NULL,
    tipo VARCHAR(20) DEFAULT 'COMUM'
);

CREATE TABLE IF NOT EXISTS artista (
    id VARCHAR(36) PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    genero_musical VARCHAR(100),
    biografia TEXT,
    cpf VARCHAR(14) UNIQUE,
    data_nascimento DATE,
    cep VARCHAR(10),
    logradouro VARCHAR(200),
    numero VARCHAR(20),
    bairro VARCHAR(100),
    cidade VARCHAR(100),
    uf VARCHAR(2),
    status_verificacao VARCHAR(20) DEFAULT 'NAO_SOLICITADO',
    usuario_id VARCHAR(36)
);

CREATE TABLE IF NOT EXISTS album (
    id VARCHAR(36) PRIMARY KEY,
    titulo VARCHAR(255) NOT NULL,
    artista VARCHAR(255) NOT NULL,
    ano_lancamento INT NOT NULL,
    usuario_id VARCHAR(36)
);

CREATE TABLE IF NOT EXISTS musica (
    id VARCHAR(36) PRIMARY KEY,
    titulo VARCHAR(255) NOT NULL,
    genero VARCHAR(255),
    duracao_segundos INT,
    album_id VARCHAR(36),
    artista VARCHAR(255),
    resenha TEXT,
    nota INT,
    spotify_url VARCHAR(255),
    youtube_url VARCHAR(255),
    usuario_id VARCHAR(36),
    CONSTRAINT fk_album FOREIGN KEY (album_id) REFERENCES album(id) ON DELETE SET NULL,
    CONSTRAINT fk_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS playlist (
    id VARCHAR(36) PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    usuario_id VARCHAR(36) NOT NULL,
    oculta BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_playlist_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS playlist_item (
    id VARCHAR(36) PRIMARY KEY,
    playlist_id VARCHAR(36) NOT NULL,
    url VARCHAR(500) NOT NULL,
    video_id VARCHAR(50) NOT NULL,
    titulo VARCHAR(500) NOT NULL,
    ordem INT NOT NULL,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_playlist_item_playlist FOREIGN KEY (playlist_id) REFERENCES playlist(id) ON DELETE CASCADE
);

-- Inserir usuário administrador padrão (se não existir)
INSERT INTO usuario (id, nome, email, senha, tipo) 
VALUES ('c4b4d693-e18e-4f51-b844-3d9692482be2', 'Administrador', 'admin@email.com', '$2a$10$uNMxjjZPaBHQm.LCdShe7ujv3tAyy8Fn9px0u6XEUyfFW1/LjWPSa', 'ADMIN')
ON CONFLICT (email) DO NOTHING;
