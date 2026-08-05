-- Adiciona colunas para suportar compartilhamento de músicas, álbuns e artistas no feed
ALTER TABLE post ADD COLUMN tipo VARCHAR(30) DEFAULT 'TEXTO' NOT NULL;
ALTER TABLE post ADD COLUMN musica_id VARCHAR(36);
ALTER TABLE post ADD COLUMN album_id VARCHAR(36);
ALTER TABLE post ADD COLUMN artista_id VARCHAR(36);

-- Adiciona chaves estrangeiras com SET NULL para manter os posts se os itens forem deletados
ALTER TABLE post ADD CONSTRAINT fk_post_musica FOREIGN KEY (musica_id) REFERENCES musica(id) ON DELETE SET NULL;
ALTER TABLE post ADD CONSTRAINT fk_post_album FOREIGN KEY (album_id) REFERENCES album(id) ON DELETE SET NULL;
ALTER TABLE post ADD CONSTRAINT fk_post_artista FOREIGN KEY (artista_id) REFERENCES artista(id) ON DELETE SET NULL;

-- Cria tabela de seguidores para relacionamento social
CREATE TABLE seguidor (
    seguidor_id VARCHAR(36) NOT NULL,
    seguido_id VARCHAR(36) NOT NULL,
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (seguidor_id, seguido_id),
    CONSTRAINT fk_seguidor_seguidor FOREIGN KEY (seguidor_id) REFERENCES usuario(id) ON DELETE CASCADE,
    CONSTRAINT fk_seguidor_seguido FOREIGN KEY (seguido_id) REFERENCES usuario(id) ON DELETE CASCADE
);
