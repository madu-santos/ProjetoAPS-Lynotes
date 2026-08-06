-- V10__Adicionar_youtube_ids_e_capa.sql

ALTER TABLE musica ADD COLUMN IF NOT EXISTS youtube_id VARCHAR(50);
ALTER TABLE musica ADD COLUMN IF NOT EXISTS capa_url VARCHAR(500);

ALTER TABLE album ADD COLUMN IF NOT EXISTS youtube_id VARCHAR(50);
ALTER TABLE album ADD COLUMN IF NOT EXISTS capa_url VARCHAR(500);

ALTER TABLE artista ADD COLUMN IF NOT EXISTS youtube_id VARCHAR(50);
ALTER TABLE artista ADD COLUMN IF NOT EXISTS capa_url VARCHAR(500);

CREATE INDEX IF NOT EXISTS idx_musica_youtube_id ON musica(youtube_id);
CREATE INDEX IF NOT EXISTS idx_album_youtube_id ON album(youtube_id);
CREATE INDEX IF NOT EXISTS idx_artista_youtube_id ON artista(youtube_id);
